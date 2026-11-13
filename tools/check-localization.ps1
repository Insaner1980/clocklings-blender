# Checks authored resources directly, so Android fallback cannot hide missing translations.
[CmdletBinding()]
param(
    [string]$ResourceRoot,
    [switch]$SelfTest
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if (-not $ResourceRoot) { $ResourceRoot = Join-Path $PSScriptRoot '../app/src/main/res' }

function Get-FormatSignature([System.Xml.XmlElement]$Node) {
    if ($Node.GetAttribute('formatted') -eq 'false') { return '' }
    # Java Formatter conversions; %% and %n consume no argument. Require positional arguments.
    $pattern = '%(?:(?<index>\d+)\$)?[-#+ 0,(<]*\d*(?:\.\d+)?(?<type>[tT][HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc]|[bBhHsScCdoxXeEfgGaA%n])'
    if ([regex]::Replace($Node.InnerText, $pattern, '').Contains('%')) {
        throw "Malformed formatter token in $($Node.InnerText)"
    }
    $tokens = [System.Collections.Generic.List[string]]::new()
    foreach ($match in [regex]::Matches($Node.InnerText, $pattern)) {
        $type = $match.Groups['type'].Value
        if ($type -in @('%', 'n')) { continue }
        if (-not $match.Groups['index'].Success -or $match.Value.Contains('<')) {
            throw "Non-positional placeholder in $($Node.OuterXml)"
        }
        $tokens.Add("$($match.Groups['index'].Value):$type")
    }
    return (@($tokens | Sort-Object) -join ',')
}

function Read-TextResources([string]$Directory, [System.Collections.Generic.List[string]]$Problems) {
    $result = @{}
    if (-not (Test-Path -LiteralPath $Directory -PathType Container)) { return $result }
    foreach ($file in Get-ChildItem -LiteralPath $Directory -Filter '*.xml' -File | Sort-Object Name) {
        $xml = [System.Xml.XmlDocument]::new()
        $xml.XmlResolver = $null
        $xml.Load($file.FullName)
        foreach ($node in $xml.DocumentElement.ChildNodes) {
            if ($node.NodeType -ne 'Element') { continue }
            $type = $node.LocalName
            if ($type -eq 'item' -and $node.GetAttribute('type') -eq 'string') { $type = 'string' }
            if ($type -notin @('string', 'plurals', 'string-array')) { continue }
            $key = $node.GetAttribute('name')
            if ([string]::IsNullOrWhiteSpace($key)) { throw "Unnamed text resource in $($file.FullName)" }
            if ($result.ContainsKey($key)) {
                $Problems.Add("DUPLICATE $Directory/$key")
                continue
            }
            $items = @($node)
            if ($type -ne 'string') { $items = @($node.SelectNodes('item')) }
            if ($items.Count -eq 0) { $Problems.Add("EMPTY $Directory/$key") }
            $signatures = @{}
            for ($index = 0; $index -lt $items.Count; $index++) {
                $item = $items[$index]
                $part = if ($type -eq 'plurals') { $item.GetAttribute('quantity') } else { "$index" }
                if ($type -eq 'plurals' -and $part -notin @('zero', 'one', 'two', 'few', 'many', 'other')) {
                    $Problems.Add("PLURAL_QUANTITY $Directory/$key/$part")
                }
                if ($signatures.ContainsKey($part)) { $Problems.Add("DUPLICATE_ITEM $Directory/$key/$part") }
                try { $signatures[$part] = Get-FormatSignature $item }
                catch { $Problems.Add("FORMAT $Directory/$key/$part : $($_.Exception.Message)"); $signatures[$part] = '' }
                if ([string]::IsNullOrWhiteSpace($item.InnerText)) { $Problems.Add("EMPTY $Directory/$key/$part") }
            }
            if ($type -eq 'plurals' -and -not $signatures.ContainsKey('other')) {
                $Problems.Add("PLURAL_OTHER $Directory/$key")
            }
            $result[$key] = [PSCustomObject]@{
                Type = $type
                Signatures = $signatures
                Count = $items.Count
                NonTranslatable = $node.GetAttribute('translatable') -eq 'false'
            }
        }
    }
    return $result
}

function Test-ResourceParity([string]$Root) {
    $problems = [System.Collections.Generic.List[string]]::new()
    $base = Read-TextResources (Join-Path $Root 'values') $problems
    if ($base.Count -eq 0) { throw "No base text resources in $Root/values" }
    # Brand and the app-specific public RSA key are not translated UI copy.
    # Keep this explicit: new strings still require translations by default.
    $exceptions = @('app_name', 'play_billing_public_key')
    foreach ($key in $base.Keys) {
        if ($base[$key].NonTranslatable -and $key -notin $exceptions) {
            $problems.Add("UNREVIEWED_EXCEPTION values/$key")
        }
    }
    foreach ($language in @('fi', 'sv', 'de', 'fr', 'es', 'pt-rBR')) {
        $translated = Read-TextResources (Join-Path $Root "values-$language") $problems
        foreach ($key in $base.Keys | Sort-Object) {
            $source = $base[$key]
            if ($source.NonTranslatable -and $key -in $exceptions) {
                if ($translated.ContainsKey($key)) { $problems.Add("TRANSLATED_EXCEPTION $language/$key") }
                continue
            }
            if (-not $translated.ContainsKey($key)) { $problems.Add("MISSING $language/$key"); continue }
            $target = $translated[$key]
            if ($target.NonTranslatable) { $problems.Add("TRANSLATABLE $language/$key") }
            if ($source.Type -ne $target.Type) { $problems.Add("TYPE $language/$key"); continue }
            if ($source.Type -eq 'string-array' -and $source.Count -ne $target.Count) {
                $problems.Add("ARRAY_COUNT $language/$key ($($source.Count) != $($target.Count))")
            }
            # All authored languages include one/other. Extra valid branches are allowed,
            # but must accept the same arguments as the source other branch.
            if ($source.Type -eq 'plurals') {
                foreach ($part in $source.Signatures.Keys) {
                    if (-not $target.Signatures.ContainsKey($part)) { $problems.Add("PLURAL_BRANCH $language/$key/$part") }
                }
            }
            foreach ($part in $target.Signatures.Keys) {
                $sourcePart = if ($source.Type -eq 'plurals' -and -not $source.Signatures.ContainsKey($part)) { 'other' } else { $part }
                if ($source.Signatures.ContainsKey($sourcePart) -and $source.Signatures[$sourcePart] -cne $target.Signatures[$part]) {
                    $problems.Add("PLACEHOLDER $language/$key/$part ($($source.Signatures[$sourcePart]) != $($target.Signatures[$part]))")
                }
            }
        }
        foreach ($key in $translated.Keys) {
            if (-not $base.ContainsKey($key)) { $problems.Add("EXTRA $language/$key") }
        }
    }
    return $problems.ToArray()
}

function Invoke-SelfTest {
    $fixture = Join-Path $PSScriptRoot ('.localization-fixture-' + [guid]::NewGuid().ToString('N'))
    try {
        foreach ($folder in @('values', 'values-fi', 'values-sv', 'values-de', 'values-fr', 'values-es', 'values-pt-rBR')) {
            $null = New-Item -ItemType Directory -Path (Join-Path $fixture $folder) -Force
        }
        $valid = @'
<resources>
  <string name="label">Time %1$s: %2$d</string>
  <string name="repeat">%1$s %1$s %%</string>
  <string name="required">Required</string>
  <plurals name="count"><item quantity="one">%1$d minute</item><item quantity="other">%1$d minutes</item></plurals>
  <string-array name="praise"><item>Good</item><item>%1$s</item></string-array>
</resources>
'@
        foreach ($folder in @('values', 'values-fi', 'values-sv', 'values-de', 'values-fr', 'values-es', 'values-pt-rBR')) {
            Set-Content -LiteralPath (Join-Path $fixture "$folder/strings.xml") -Value $valid -Encoding utf8
        }
        $basePath = Join-Path $fixture 'values/strings.xml'
        $baseText = $valid.Replace('<resources>', '<resources><string name="app_name" translatable="false">Clocklings</string><string name="play_billing_public_key" translatable="false">UNCONFIGURED</string>')
        Set-Content -LiteralPath $basePath -Value $baseText -Encoding utf8
        $errors = @(Test-ResourceParity $fixture)
        if ($errors.Count) { throw "Valid fixture failed: $($errors -join '; ')" }
        $cases = @(
            @{ Name = 'missing key'; Text = $valid.Replace('<string name="required">Required</string>', ''); Error = 'MISSING fi/required' },
            @{ Name = 'argument type'; Text = $valid.Replace('Time %1$s: %2$d', 'Time %1$s: %2$s'); Error = 'PLACEHOLDER fi/label/' },
            @{ Name = 'argument multiplicity'; Text = $valid.Replace('%1$s %1$s %%', '%1$s %%'); Error = 'PLACEHOLDER fi/repeat/' },
            @{ Name = 'malformed extra argument'; Text = $valid.Replace('%1$s %1$s %%', '%1$s %1$s %% %2$q'); Error = 'FORMAT' },
            @{ Name = 'resource type'; Text = $valid.Replace('<string name="required">Required</string>', '<string-array name="required"><item>Required</item></string-array>'); Error = 'TYPE fi/required' },
            @{ Name = 'plural other'; Text = $valid.Replace('<item quantity="other">%1$d minutes</item>', ''); Error = 'PLURAL_OTHER' },
            @{ Name = 'array length'; Text = $valid.Replace('<item>Good</item>', ''); Error = 'ARRAY_COUNT fi/praise' },
            @{ Name = 'duplicate key'; Text = $valid.Replace('</resources>', '<string name="label">Duplicate</string></resources>'); Error = 'DUPLICATE' },
            @{ Name = 'unreviewed exception'; Text = $valid.Replace('<string name="required">', '<string name="required" translatable="false">'); Error = 'TRANSLATABLE fi/required' }
        )
        foreach ($case in $cases) {
            Set-Content -LiteralPath (Join-Path $fixture 'values-fi/strings.xml') -Value $case.Text -Encoding utf8
            $errors = @(Test-ResourceParity $fixture)
            if (-not @($errors | Where-Object { $_.StartsWith($case.Error) }).Count) {
                throw "Fixture did not detect $($case.Name): $($errors -join '; ')"
            }
            Write-Output "PASS self-test: $($case.Name)"
        }
        Write-Output 'PASS self-test: valid translations and explicit brand/public-key exceptions'
    } finally {
        # Only remove this invocation's verified child directory.
        $toolsRoot = [System.IO.Path]::GetFullPath($PSScriptRoot) + [System.IO.Path]::DirectorySeparatorChar
        $fixturePath = [System.IO.Path]::GetFullPath($fixture)
        if (-not $fixturePath.StartsWith($toolsRoot, [System.StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe fixture cleanup path' }
        if (Test-Path -LiteralPath $fixturePath) { Remove-Item -LiteralPath $fixturePath -Recurse -Force }
    }
}

if ($SelfTest) { Invoke-SelfTest; exit 0 }
$problems = @(Test-ResourceParity $ResourceRoot)
if ($problems.Count) {
    $problems | Write-Output
    Write-Output "FAIL: $($problems.Count) localization issue(s)."
    exit 1
}
Write-Output 'PASS: All seven languages have matching resource keys, types, plurals, arrays and argument signatures.'
