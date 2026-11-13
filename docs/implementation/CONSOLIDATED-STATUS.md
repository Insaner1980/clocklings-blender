# Clocklings — consolidated implementation and verification status

Updated: 8 November 2026.

This is the single current summary of implemented functionality, the Android/Google audit follow-ups, adaptive-layout work, actual validation and remaining limits. It supersedes older workload counts, desktop-scope deferrals, rejected short-window screenshots and outstanding-cleanup statements in the historical reports. Those reports remain evidence, not separate current task lists.

The 6 November consolidation reviewed retained evidence and current file identities without rerunning validation. The PR #13 follow-up records the 7 November checks; the focused verification below adds current-checkout JVM and API 26 evidence on 8 November. Historical test results retain their original version and environment boundaries.

## Current position

- The approved monetization model and local purchase-signature verification are implemented on local branch `codex/full-unlock`, with the validation below. Production purchases remain safely unavailable until the correct public Play licensing key and Console/license-test configuration are supplied.
- The scoped application corrections and adaptive implementation are complete and have the validation described below. The no-companion copy defect found during focused local verification has now been corrected in all seven languages; see the scoped correction below.
- Supported product scope includes small and large Android phones, tablets, Chromebooks and Googlebook devices. The later owner instruction supersedes the audit's earlier desktop/laptop deferral. Selected product scope is not a claim of verified compatibility on every device/runtime.
- The owner made a Pixel 9 available for the focused local verification below. Earlier unavailable-hardware statements remain historical; Chromebook/Googlebook and broader physical-device coverage are still unverified.
- Additional emulator-based checks can be considered separately. An old physical Android phone is not inherently required for API 26 validation: API 26 tests already ran on a dedicated emulator before the adaptive changes.
- Publication, signing, licensing/provenance and privacy/store work remain deliberately deferred. This document does not declare release readiness.
- The audit/adaptive work was committed and pushed as `49e1c16` on `codex/adaptive-layouts`. The owner opened [PR #13](https://github.com/Insaner1980/Clocklings/pull/13); its review follow-up is recorded below.

## PR #14 review follow-up, 8 November

Verified CodeRabbit's two comments and Greptile's one comment on `d20e7a0` against the implementation. The monetization fixture now saves and restores the entire previous purchase state after each test, including a failing test body. The Together resize test explicitly checks compact controls at 1000 × 360 dp, their absence at 1000 × 600 dp, and their return after resizing and Activity recreation. The earlier adaptive-layout APK hash below is now labeled as a historical checkpoint. Application code and resources are unchanged.

Validation: debug app/test APK assembly, `ktlintCheck`, `detekt`, secrets checks and `git diff --check` passed. On the dedicated API 36 `ClocklingsReview20261030` emulator, **15 instrumented tests passed**: the three monetization tests at 411 dp / 100%, 320 dp / 100%, 411 dp / 200% and 320 dp / 200%; the Together resize/recreation test at both font scales; and the full-access level/challenge test immediately after the monetization tests in the same instrumentation process. This last sequence verifies that the free-access fixture no longer leaves paid content locked for subsequent tests.

Initial emulator runs ended in host QEMU crashes (Windows exception `0xc0000005`), without completed test results. The successful runs used the host GPU with Vulkan disabled; no project or AVD configuration file was changed. The task-owned emulator was stopped afterwards. The full JVM suite, human TalkBack review, live Play purchases and owner-operated `lc`/`sc` were not rerun. The local-only execution log is `reports/pr14-review/device-validation.log`.

## Dependency-warning follow-up, 8 November

The owner asked to address the four Lint dependency-version warnings where necessary. Reviewed the actual dependency declarations and official release notes before changing the existing `codex/full-unlock` checkout:

- Updated only the shared Guava constraint from `33.7.2-jre` to `33.8.0-jre`. The [latest stable release](https://github.com/google/guava/releases/tag/v33.8.0) strengthens `HashBiMap` and `LinkedHashMultimap.ValueSet` against hash flooding (reported there as forthcoming CVE-2026-118042). The existing constraint covers build tools and Robolectric; project sources have no direct Guava imports or `ForwardingMap` overrides. The documented `StandardValues` final-method compatibility caveat is therefore assessed through the existing build/test consumers rather than a source workaround.
- Kept Gradle `9.8.2`. [AGP 9.5 compatibility](https://developer.android.com/build/releases/agp-9-5-0-release-notes) requires at least Gradle `9.6.0` and JDK 17; the current toolchain meets that requirement. The available `9.9.0` is an optional toolchain update, not required to correct an observed failure here.
- Kept both Navigation 3 artifacts at `1.2.0`. The [1.2.1 release note](https://developer.android.com/jetpack/androidx/releases/navigation3#1.2.1) describes lowering the library's compileSdk requirement from 37 to 36. This app already uses compileSdk 37, so that change does not require a runtime-library update for this task. Neither of these three optional-version warnings was suppressed.

The first dependency-inspection attempt failed before project configuration completed because another process held the shared Gradle build cache (`Owner PID: 30772`). The accompanying missing-compileSdk message followed the failed Kotlin DSL configuration, although the declaration is present. Retrying with `--no-build-cache` succeeded without changing that declaration, disabling checks, deleting shared caches or stopping the other process. `debugUnitTestRuntimeClasspath` selects Guava `33.8.0-jre` with its Java-8-compatible Android variant.

The first build/test attempt then correctly rejected the new `guava-33.8.0-android.jar` because its checksum was not yet in dependency-verification metadata. Its downloaded SHA-256 matched [Maven Central's published checksum](https://repo.maven.apache.org/maven2/com/google/guava/guava/33.8.0-android/guava-33.8.0-android.jar.sha256): `c88b3bd93382316b00a387b629d2cc2620119e16c46554b07fa14e9aa07f990e`. Added only that exact artifact/version checksum, preserving earlier metadata, trusted keys, metadata verification and signature verification. There is no Gradle dependency lockfile to update. The subsequent `debugRuntimeClasspath` inspection found no `com.google.guava:guava` dependency in the application runtime.

`assembleDebug` and `lintDebug` passed with the new version. Lint has **zero errors and three remaining optional-update warnings**, one for Gradle and two for Navigation 3; the Guava warning is gone. Existing test-API deprecation, test-manifest merge and native-symbol stripping messages were not treated as reasons for unrelated migrations.

Final validation: **456 JVM/Robolectric tests in 83 suites passed, zero failures/errors/skips**. The final invocation completed `BUILD SUCCESSFUL` with dependency verification enabled and 59 executed tasks:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:dependencyInsight --dependency com.google.guava:guava --configuration debugRuntimeClasspath --no-build-cache --no-daemon --console=plain
```

Current debug APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `015E943ADD4F6BD7A33E5AEB83D5CBCE57BD80A8D75EB8C99DBD9A42A6865DF9`. All eight retained files in `reports/full-unlock/focused-verification/`, including the test-signed release APK, remain hash-identical. Earlier artifact hashes below retain their checkpoint meaning. All Java processes identified as belonging to this follow-up, including their observed children, exited; none remained at handoff. No device tests, signing changes, Play Console access, purchases, commit or push occurred. TalkBack listening and real Play purchase testing remain deferred.

**New follow-up cleanup limitation:** automatic approval review rejected removal of this follow-up's two disposable failure-report directories with `blocked by policy`. No deletion or alternate removal mechanism was attempted afterwards. Read-only checks verified their exact resolved paths under this workspace, their parents and all descendants, with zero reparse points. These are new leftovers (481,512 and 267,263 bytes), separate from the owner's already deleted `.scratch/focused-verification` and `.apk.idsig` sidecar, which remain absent. Only the following new failure reports may be removed; successful test/Lint reports and retained verification evidence are preserved:

```powershell
Remove-Item -LiteralPath 'C:\Dev\Clocklings\build\reports\dependency-verification\at-1794148066308' -Recurse -Force
Remove-Item -LiteralPath 'C:\Dev\Clocklings\build\reports\configuration-cache\9fvl1qzm9kia0cyb76vcatksn\5b23ptk0uyuj0xrj0celll547' -Recurse -Force
```

## No-companion wording correction, 8 November

The owner authorized a focused correction after the verification finding below. Inspected the current resource usage and `AccessPolicy`: `AppearanceScreen` displays `no_companion_hint` for the NONE preview in both entitlement states, while lesson/game access is controlled separately. All seven translations contained the equivalent misleading promise. Only this string in each `personalization.xml` was changed; no production behavior, free/full boundaries, purchase prompts, dependencies or EUR 6.99 price changed. Existing uncommitted work on `codex/full-unlock` was preserved.

| Language | Corrected description |
| --- | --- |
| English | Practise telling the time without a companion. |
| Finnish | Harjoittele kellonaikoja ilman kaveria. |
| Swedish | Öva på klockan utan en kompis. |
| German | Übe die Uhrzeit ohne Figur. |
| French | Apprends à lire l’heure sans compagnon. |
| Spain Spanish | Practica las horas sin acompañante. |
| Brazilian Portuguese | Pratique as horas sem companheiro. |

Validation actually run:

- `tools/check-localization.ps1` passed for all seven languages: resource keys, types, plurals, arrays and argument signatures. Each replacement is shorter than its previous wording. The secrets-before-read checks and scoped `git diff --check` also passed.
- **9 existing JVM/Robolectric tests passed, zero failures/errors/skips:** `AccessPolicyTest` (3), `AccessRewardsTest` (4), `LocalAppFlowTest.companionColorsAndOutfitsPersistAcrossRecreation` (1), and `ScreenshotReviewUiTest.companionRetryKeepsThePreviewAfterSwitchingTabsAcrossDisplaySizes` (1). The latter covered 411/320 dp at 100%/200% font scale, including 320 dp / 200%. It verifies existing companion retry/preview behavior; this is not new seven-language visual or device evidence for the hint. No existing assertion depended on the old wording, so no tests or behavioral assertions were changed.
- `:app:assembleDebug` and `:app:lintDebug` passed. Lint reported no errors and four dependency-update warnings (Gradle, two Navigation 3 entries and Guava), outside this text-only correction. No dependency update was made.

The targeted Gradle invocation, run only from this repository:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'fi.clocklings.domain.AccessPolicyTest' --tests 'fi.clocklings.domain.AccessRewardsTest' --tests 'fi.clocklings.ui.LocalAppFlowTest.companionColorsAndOutfitsPersistAcrossRecreation' --tests 'fi.clocklings.ui.ScreenshotReviewUiTest.companionRetryKeepsThePreviewAfterSwitchingTabsAcrossDisplaySizes' :app:assembleDebug :app:lintDebug --no-daemon --console=plain
```

Updated debug deliverable: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `4B715D364BD039C515F5585B943462843567B36698F618553F63308C5BED6B95`. Older debug hashes below describe their original verification checkpoints. The retained test-signed release APK and all eight files in `reports/full-unlock/focused-verification/` were hash-checked unchanged; that release APK still contains the pre-correction wording and was not rebuilt or re-signed.

No full unit/device matrix, Pixel/emulator testing, new screenshots, owner-operated `lc`/`sc`, Play Console access, signing setup, purchases, commits, pushes or publication occurred. TalkBack listening and real Play purchase testing remain explicitly **NOT_RUN / DEFERRED**. The earlier audit is not reopened. Current cleanup status and the preserved historical rejection are recorded below.

The Gradle invocation completed successfully. Its remaining single-use daemon was identified by PID, parent, start time and command, then stopped; follow-up inspection found no task-owned Java processes or their children. No disposable scratch directory, helper, server or emulator was created. Standard debug/test/lint outputs remain in the existing build directories; unrelated processes and retained verification evidence were preserved.

## Focused Pixel and optimized-build verification, 8 November

This later local-only task supersedes the earlier proposed Console next step. **All Play Console setup and live transactions are deferred by the owner.** No app creation, form saving, declarations, products, accounts, upload, publication or production signing were attempted or requested. The confirmed price remains **EUR 6.99**. The 456-test campaign, API 26 matrix and owner-operated `lc`/`sc` were not repeated.

Branch `codex/full-unlock`, base `b09e3be612d3e9f65a470e946f50c666d45de0d2`, with the existing uncommitted implementation. Initial-file SHA-256 comparisons found **zero changes to existing workspace files before documentation updates**. Production code, resources, build configuration and tests were not edited. The only maintained-file changes in this task are this document and `MONETIZATION.md`.

### Pixel 9: normal debug application

The owner explicitly authorized this physical-device exception to the default emulator-only rule. The already connected Pixel 9 ran Android 17/API 37, Finnish UI, 1080 × 2424 px, 420 dpi and its existing font scale **1.15**. Accessibility and Google-account settings were left under owner control. Existing package and new APK were both `fi.clocklings`, versionCode 1/versionName 1.0, with the same Android Debug certificate SHA-256 `f6ed10fcf19c9aa5f6a31a53358cc245f487fe5b1765e7028080e020aebb7d05`. `adb -s <Pixel> install -r app/build/outputs/apk/debug/app-debug.apk` succeeded. No uninstall, data clearing, downgrade or phone reset was used. The learning DataStore SHA-256 was identical immediately before and after installation, before first launch: `b6deefcfde9424f4e6520c547e648900c04ea229f0c58910397b19f9ff44f322`.

All interactions used the normal application and production purchase service through ADB touch/key input and inspected UI, not the instrumented fake-entitlement application. The existing `MonetizationDeviceTest` fixture resets progress and was therefore deliberately not run on the Pixel.

| Scoped check | Actual result |
| --- | --- |
| Complete free first learning level | PASS: replayed all explanation pages and answered the final questions of Two hands, One trip around and Whole hours. Each completed normally. |
| Basic practice | PASS: completed all ten alternating whole-hour reading/setting tasks, including dragging the hour hand, with three stars. No purchase prompt appeared on the result screen. The Pixel already had three stars and completed first-level lessons; this is repeat-play evidence, not fresh reward-threshold evidence. |
| Companions and outfits | PASS within the three-star state: human, cat and no-companion choices worked; paid companions stayed disabled. Inspected the six free human and four free cat items, alongside locked paid items and their earning requirements. Cat bow unequip/re-equip and human red-shoes selection worked; cat bow and human white shoes were restored, ending on the initially displayed human companion. One misleading no-companion message is recorded below. |
| Explore / Play Together / navigation | PASS: moved the Explore hand from 3:00 to 6:00 and selected a random time; completed Together handover, reveal and turn swap. Back navigation returned normally. |
| Adult gate and real IME | PASS: field focus and numeric Gboard appeared with controls reachable. Gboard's on-screen Done rejected `0000`; Cancel returned to wardrobe and dismissed the gate. Reopening generated a new code; the correct reversed code submitted using Gboard Done opened the parent screen. This is real IME interaction, not merely a Compose semantics assertion. |
| Unconfigured Purchase / Restore | PASS: purchase was visibly unavailable and disabled, with no fallback price or purchase dialog. Tapping the disabled purchase, Restore purchases and Retry left the safe unavailable state; no restored/owned success or paid access appeared. |
| Background/resume and cold relaunch | PASS: Home followed by `am start -W` resumed the parent screen. Scoped `am force-stop fi.clocklings` followed by normal `am start -W -n fi.clocklings/.MainActivity` cold-launched successfully. All three free lesson completions and three stars remained visible. The post-interaction learning DataStore hash `eadcf5cc41c5f37b322ac6ed4e8e0c005eed424b794e420fd8b72c5115bb599b` was unchanged across these lifecycle checks. This does not claim byte-for-byte equality across the intervening intentional companion/outfit interactions or arbitrary power-loss recovery. |
| Crash inspection | No `Process: fi.clocklings` entry in the retained Pixel crash buffer. This is bounded log evidence, not a full ANR/performance audit. |
| Owner-assisted TalkBack listening | NOT_RUN / DEFERRED by the owner on 8 November: the owner cannot perform the listening check now and will do it later. The proposed scope remains a restricted lesson, the adult gate and unavailable purchase/restore information. No new listening evidence was obtained, and automated semantics/IME results are not counted as listening evidence. |

No UI implementation changed. The existing 320 dp, 200% font-scale and combined API 26 evidence was retained as historical coverage. Those configurations were **not rerun** or applied to the owner's phone; the new physical-device evidence is only its existing configuration above.

### Optimized release: compilation and runtime are separate results

Sequential commands from this repository, each completed before the next Gradle invocation:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
.\gradlew.bat :app:assembleRelease --no-daemon --console=plain
```

Both passed. Release ran `minifyReleaseWithR8`, `optimizeReleaseResources` and `lintVitalRelease` with the existing `isMinifyEnabled = true` / `isShrinkResources = true` configuration. No R8 missing-class/optimization failure was reported; the log retained the generic Gradle 10 deprecation notice. No keep rule or shrinking setting was changed. Production signing stayed unconfigured; the standard release output is unsigned. A separate output was signed locally using the existing **Android debug key**, without changing Gradle signing configuration.

**PASS — actual optimized runtime:** dedicated project-local `ClocklingsOptimized36`, `emulator-5592`, Android 16/API 36 x86_64, image `BE2A.250530.026.D1/13818094`, 1080 × 2424 px/420 dpi/font 1.0, English. Successful run used host GPU, Vulkan disabled, 2048 MB RAM and two cores. Installed the separately test-signed release with `adb -s emulator-5592 install --no-incremental reports/full-unlock/focused-verification/app-release-local-test.apk`. Verified the installed APK hash against that file and absence of `DEBUGGABLE` in package flags.

The actual release application cold-started, completed Two hands, followed the normal practice lesson-introduction route and its visible Skip and play action, then completed ten reading/setting tasks from zero stars. The result awarded three stars and offered only the free polka-dot dress, bow and red shoes. Paid next levels remained disabled. The parent gate opened using native Tab/Enter after the correct code; disabled Purchase, Restore and Retry retained the unavailable state with no false success. These were ordinary UI actions, with no entitlement fixture, test licensing key or unlock bypass.

**Environment limitations:** the first emulator process ended after its initial launch receipt without an application crash trace; that attempt is not counted as completed runtime coverage. On the restarted emulator, an Android **System UI** ANR dialog appeared during startup. Choosing Wait allowed the subsequent complete journey above. The final emulator crash buffer was empty, but runtime logs included media-codec/system-resource and timing warnings. These observations do not establish a Clocklings optimization defect or performance/audio quality; no source workaround was made. Unrelated running `medium_phone` and `KnitToolsAdaptiveAudit` emulators were not used or stopped.

**Packaged configuration:** `apkanalyzer manifest debuggable` returned `false`; application class is `fi.clocklings.ClocklingsApplication`, backup remains disabled, minSdk 26/targetSdk 37, and Billing/Internet/network-state permissions remain present. `aapt2 dump resources` confirms `play_billing_public_key = UNCONFIGURED`. R8 mapping confirms optimized application/Billing code. Defined-class inspection found no `ClocklingsTest*`, `DeviceTestPurchases`, JUnit or AndroidX Test classes. Neither compilation nor this sideloaded runtime proves Google Play transactions, acknowledgement, account reconciliation, refund handling or reinstall restoration.

| Artifact | SHA-256 |
| --- | --- |
| Normal debug APK, unchanged | `69A87E17176EB2234D225AE5959394462D0138F50AACB18A58AEB86EC4C26FB9` |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | `CC715CC86DF6564408F984BBFFAEF7B2DD085071BC44484B30F5DE34B95CEEAD` |
| `reports/full-unlock/focused-verification/app-release-local-test.apk` | `3141ED4B35045064A468B14448ADFB445D77977FC91A842CB7ADF979761CB0B8` |

The test-signed artifact is for local verification only and was never installed on the Pixel. Build receipts and five inspected screenshots are retained in `reports/full-unlock/focused-verification/` (Git-ignored local evidence).

### Reproduced defect, subsequently corrected

**Historical finding: misleading no-companion access message.** With Billing unconfigured/free access, My Companion's no-companion option (`Pelkkä kello`) said “Kaikki oppitunnit ja pelit ovat käytettävissäsi”, while later learning/practice levels remained restricted. `AppearanceScreen.kt:173` renders `no_companion_hint` for the NONE preview in both free and full access. All seven translations promised all lessons/games. Choosing no companion worked, but the message misrepresented commercial access. Pixel reproduction and screenshot: `reports/full-unlock/focused-verification/pixel-no-companion-copy.png`. No access bypass was observed. The verification task did not authorize a production correction; the owner subsequently authorized the scoped wording correction recorded above.

**Historical cleanup rejection:** the dedicated emulator was stopped with its explicitly selected ADB serial. Follow-up process inspection found none of the task's emulator/QEMU/console/crash-handler PIDs and no new Java process compared with the entry snapshot. Automatic approval review rejected the bounded disposable-artifact removal with `blocked by policy`; no deletion occurred during that task and no alternate deletion mechanism was used. At that checkpoint, the owned scratch directory contained 5,670,389,003 bytes, including the disposable AVD, pulled old Pixel APK, helper, intermediate images and duplicate logs; the incremental-install signature sidecar also remained. The exact target, parents and descendants had **zero reparse points**. Shared emulator crash storage and unrelated files/processes were left untouched.

**Current cleanup status, 8 November:** the owner reports manually deleting `C:\Dev\Clocklings\.scratch\focused-verification` and `C:\Dev\Clocklings\reports\full-unlock\focused-verification\app-release-local-test.apk.idsig`. Read-only existence checks confirmed both are absent. They are no longer outstanding cleanup; no deletion or bypass of the earlier rejection was performed by this correction task. The retained test-signed release APK, two build receipts and five screenshots are present with unchanged SHA-256 hashes. The APK and screenshots remain evidence for the earlier, pre-correction build.

The owner explicitly deferred TalkBack listening until later. The focused local verification is closed; the copy defect and cleanup were subsequently resolved as recorded here. No further device testing was performed after the owner's deferral. TalkBack listening and real Play purchase testing remain NOT_RUN / DEFERRED.

## Monetization implementation and verification, 8 November

**Live setup continuation, 8 November:** Finnvek's Play Console contains no Clocklings application; the owner confirmed it has not been created. The unsaved Create app form reports `fi.clocklings` available, but no application was submitted and no declarations were accepted. The owner confirmed **EUR 6.99**; country availability, regional pricing and product translations remain unconfigured. The public key remains `UNCONFIGURED`, and `clocklings_full_unlock`/its Buy option do not yet exist for Clocklings. Console has no explicit license-tester email list; Google's publishing-account exception is documented, but the Pixel 9 account/test instruments remain unverified. Scoped Pixel 9 Billing tests are now explicitly authorized after the prerequisites; no device installation or transaction has run. Official guidance permits license-tester debug sideloading, so a new upload is not assumed necessary. App creation and other Console changes still require separate permission. See [MONETIZATION.md](MONETIZATION.md#live-setup-inspection-8-november-2026) for the prepared first step and validation plan. Existing local/simulated and emulator evidence below was not rerun. The previously blocked `.scratch/full-unlock` directory is now absent; this continuation did not delete it or bypass the earlier rejection. No temporary background process was started.

Base revision: `b09e3be612d3e9f65a470e946f50c666d45de0d2`, local branch `codex/full-unlock`. Existing work in `PROJECT.md`, `AdaptiveWindowDeviceTest.kt`, this document and `.codex-remote-attachments/` was preserved. No commit, push, PR, upload, production signing or Play Console change was made. This is a locally validated implementation, not publication readiness.

**Implemented approved content:** free `viisarit`/`kierros`/`tasan` lessons (verified `Lessons.levelLessons[0]`), repeatable whole-hour read/set practice, Explore, Play Together, optional `vrk`, all clock appearances, HUMAN/CAT and their ten existing 0–3-star items, progress saving and general preferences. Other companions require full ownership and three total earned stars; other outfits retain their existing thresholds. Full access covers all five levels and all practice/challenge activities. No-companion mode remains free. Purchase never awards stars, lesson completions or unearned rewards.

The shared policy protects restored/alternate navigation, automatic lesson queues, direct starts, replay and item/companion selection. Ownership updates do not interrupt an active lesson with a purchase screen. Restricted saved choices use display-only fallbacks; progress, names, outfits and preferences are not deleted or unnecessarily overwritten. Restoration updates eligibility immediately without restarting.

**Implemented local verification:** the owner explicitly accepted local Play signature verification without a backend/accounts/analytics. Billing 9.1.0 queries and updates feed a replaceable gateway and ownership controller. RSA signature, expected app/product/token/quantity and explicit completed state are checked before durable ownership and acknowledgement. Separate purchase DataStore survives learning reset. Pending, cancellation, invalid/duplicate/already-owned transactions, restoration, acknowledgement retries/restart recovery, offline cached access, query failures and confirmed ownership loss are handled. A fresh ownership query reconciles callbacks; a valid old signature alone does not prove absence of revocation. No consumption or recurring expiry is used. See [MONETIZATION.md](MONETIZATION.md) for the exact approved mapping, persistence/fallback behavior, local-verification limits, permissions/data flows and owner setup.

**Configuration blocker:** `play_billing_public_key` in `app/src/main/res/values/billing.xml` is deliberately `UNCONFIGURED`. The correct public licensing key for `fi.clocklings` is unavailable; purchase launch is safely disabled with no bypass. The single product is `clocklings_full_unlock`, named **Clocklings Full Unlock**. Actual product/Buy-option configuration, regional prices/availability and the license-test environment remain unverified. The owner confirmed EUR 6.99 during the live setup continuation; displayed purchase prices must come from Play.

**Parent area:** product information, purchase and Restore purchases are behind a cancellable digit-order adult gate, separate from clock exercises. The gate is a usability boundary, not legal age verification. All new copy is localized into seven languages. Price-unavailable/retry states are explicit. The UI explains access versus earned rewards and purchase restoration versus local learning data. No lesson/result paywalls, urgency, subscriptions or additional paid products were introduced.

**PASS — final local validation:**

- `:app:testDebugUnitTest --no-daemon`: **456 tests in 83 classes**, zero failures/errors/skips. This includes 47 cases beyond the historical 409-test baseline, with deterministic signed test receipts/fake Billing for pending, cancelled, invalid, duplicate, restored/already-owned, acknowledgement retry/restart, failed writes/reads, offline ownership, successful ownership loss/account changes, learning reset, saved choices and access paths.
- `:app:assembleDebug :app:assembleDebugAndroidTest --no-daemon`: both final debug APKs built successfully.
- `:app:ktlintCheck`, `:app:detekt`, `:app:lintDebug`, `:app:stabilityDump`, then `:app:stabilityCheck`: passed in separate sequential `--no-daemon` invocations. Updated debug/release stability baselines were reviewed for the changed composable signatures. Lint has zero errors and the same four unrelated dependency-update warnings (Gradle, Navigation 3 runtime/UI and Guava); no unrelated versions were upgraded.
- `tools/check-localization.ps1` and its `-SelfTest`: passed. The public licensing key has an explicit non-UI translation exception, tested alongside the existing brand exception. Secrets scans and scoped diff checks passed. Owner-operated `lc`/`sc` were not run.

**PASS — dedicated API 26 runtime:** `ClocklingsBilling26`, `emulator-5592`, x86_64 Android 8.0 `OSR1.180418.026/6741039`. Three `MonetizationDeviceTest` cases passed in every configuration below (**15 UI executions**): free/restricted practice, adult cancellation and keyboard Tab/Enter entry, unavailable purchasing, restore without false success, live entitlement updates, companion three-star eligibility, saved-choice fallback/recovery and earned-but-paid outfit restrictions. `ReceiptStorageDeviceTest` also passed once at normal size: real Android RSA verification and private DataStore write/reopen/acknowledgement/clear with an ephemeral test key (**one crypto/disk test**).

| Configuration | Result |
| --- | --- |
| 1080 × 2400 px, 480 dpi, 360 dp width, font 1.0 | PASS: 3 UI + 1 crypto/disk |
| 1080 × 2400 px, 540 dpi, 320 dp width, font 1.0 | PASS: 3 UI |
| 1080 × 2400 px, 480 dpi, font 2.0 | PASS: 3 UI |
| 1080 × 2400 px, 540 dpi, 320 dp width, font 2.0 | PASS: 3 UI |
| 1000 × 360 px, 160 dpi, font 1.0; `am get-config` reports 952 × 336 dp | PASS: 3 UI |

The final APK also launched normally with the production purchase service and missing key, outside the fake-entitlement instrumentation application. Native Tab/Enter opened the parent area, where purchase stayed unavailable. Normal-size parent and 320 dp/200% gate, parent controls and restricted wardrobe screenshots were visually inspected; scrolling keeps controls reachable. Five final screenshots, five successful instrumentation receipts, test counts and APK identities are retained locally in `reports/full-unlock/`. Automated semantics/keyboard/IME evidence does not constitute a human TalkBack listening review or physical-hardware testing.

Initial failures were resolved without weakening checks: the earlier basic-field fix replaced an incompatible outlined-field typography path; three existing companion tests needed earned-star fixtures after the approved rule; complexity findings required splitting small UI helpers. The keyboard test initially asserted focus on the button's inner semantic node while its outer keyboard target held focus. Native input confirmed correct behavior; the final test verifies Tab/Enter activation, and speculative key handling was removed. A combined stability dump/check invocation exposed the plugin's implicit task dependency; separate final dump/check runs passed. The final recorded checks supersede intermediate failures and the earlier 424-test access-only result.

**Final APK SHA-256:** `app/build/outputs/apk/debug/app-debug.apk` = `69A87E17176EB2234D225AE5959394462D0138F50AACB18A58AEB86EC4C26FB9`; `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` = `CFC608F909DF157497B35D9FB39092F6ECBEFCAD86E24034B91B22AE735DE944`.

**BLOCKED / NOT_RUN:** live Play purchase, reinstall restoration, acknowledgement timing, pending payment completion, refund/revoke and real Play-account changes require the correct public key, active product/Buy option/final price, and authorized license testers/distribution. Simulated transactions and ephemeral-key device signatures are not Google Play transaction evidence. No real-money purchase, remote CI or publication/policy certification is claimed. No physical device, unrelated emulator, Chromebook or Googlebook was used; no such equipment is required from the owner.

**Historical cleanup rejection, now resolved:** the owned emulator/QEMU/console/crash-handler processes and temporary Gradle processes were verified stopped. Automatic approval review rejected both the bounded cleanup operation and the narrower literal-path command with `blocked by policy`. At that checkpoint, the task-created AVD, temporary files and intermediate logs remained in `C:\Dev\Clocklings\.scratch\full-unlock`. The resolved target, its workspace/parent and descendants had no junction/symlink redirection. The later read-only checks above, including this wording correction, confirmed that directory is absent. The retained APKs and verification evidence are outside it; the earlier rejection is historical, not outstanding cleanup.

The unrelated `emulator-5580` and all physical devices were left untouched. Localization self-test fixtures were removed by their own teardown.

## PR #13 review follow-up, 7 November

CodeRabbit and Greptile's 17 inline findings at `49e1c16` were checked against the implementation. Eleven prompted corrections: API-compatible IME/cutout reads, preservation of emulator rotation/display overrides, safe fixture-parent logging, non-masking split-screen teardown, optional page-size expectations, rejected SoundPool sample handling, removal of a diagnostic print, independent startup-loading coverage, a one-way restored-scroll inset readiness latch, and the toolchain/local-evidence clarifications in this document. Focused regressions cover rejected audio samples and retained scaffold content during later inset changes.

Six findings did not warrant changes:

- `ActivityRetentionDeviceTest`: all six `ActivityScenario.onActivity` calls must complete; unavailable activities or failed recreation throw rather than silently skipping a reference. See the [ActivityScenario contract](https://developer.android.com/reference/androidx/test/core/app/ActivityScenario).
- `AdaptiveWindowDeviceTest`: explicit expected layout thresholds provide an independent test oracle; production thresholds were not changed.
- `AuditInputMethodService`: Android creates the IME window before `onCreateInputView`; the potentially absent root insets are already checked. See the [framework lifecycle implementation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/inputmethodservice/InputMethodService.java).
- `AuditSpeechService`: synthesis requests run serially on the synthesis thread, and the callback blocks until completion. The claimed concurrent pending-latch replacement does not occur under the [TextToSpeechService contract](https://developer.android.com/reference/android/speech/tts/TextToSpeechService).
- `Sfx`: the local `loaded` name is a naming preference without a behavior defect.
- `resumedActivities`: the inspected lifecycle and audio-setting callers run on the main thread, including the delayed lifecycle coroutine; no cross-thread access was found.

Validation with AGP 9.5.1: 20 targeted JVM cases passed (`SfxFailureTest`, `SpeechTest`, `StartupStateTest` on API 26/33, and `ScaffoldInsetsTest`). Debug app/test APK builds, ktlint, Detekt, Android lint, and debug/release stability checks passed. Eight instrumented cases passed on the dedicated API 36 `ClocklingsReview20261030` emulator: native-library loading with an unspecified page-size expectation, multi-window audio ownership, live resizing/rotation/recreation, four real-IME runs at 411/320 dp and 100%/200% font scale (each covering English, Finnish, Swedish and German), and native composition/replacement/commit/cancellation. The fixture-IME case initially timed out before the keyboard appeared; enabling hardware-keyboard IME display and reselecting the fixture made the unchanged test pass. Emulator settings were restored afterwards.

The final review patch used targeted validation, not another complete 407-case JVM run. API 26 instrumented execution, 16 KB runtime execution, physical devices, and an online policy review were not repeated in this follow-up.

## Focused current-checkout verification, 8 November

Tested revision: `b09e3be612d3e9f65a470e946f50c666d45de0d2`, branch `codex/adaptive-layouts`. Entry working tree: modified `PROJECT.md` and untracked `.codex-remote-attachments/`; both preserved. Production source/resources and build configuration matched HEAD throughout. This task changes only this summary and one wait in `AdaptiveWindowDeviceTest`: actual full-window pixel dimensions replace a configuration-dp comparison that fails when API 26 excludes navigation bars. State/layout assertions were preserved.

**PASS — local checks:** `:app:testDebugUnitTest --rerun --no-daemon` executed **409 tests in 75 classes, zero failures/errors/skips**. `:app:assembleDebug :app:assembleDebugAndroidTest --no-daemon` passed (current outputs were up-to-date). After the test-only adjustment, `:app:assembleDebugAndroidTest :app:ktlintAndroidTestSourceSetCheck --no-daemon` passed. Gradle invocations were sequential, using JDK 21.0.12 and the existing toolchain.

**Runtime:** dedicated disposable `ClocklingsFocused26`, `emulator-5592`, Android 8.0/API 26, x86_64, image `OSR1.180418.026/6741039`, host graphics, Vulkan disabled. In-process page size was **4096 bytes** and both packaged native libraries loaded. The pre-existing `medium_phone`/API 36 emulator was not used or changed.

| Configuration | Actual result |
| --- | --- |
| Normal phone: 1080 × 2400 px, 480 dpi, 360 dp wide, font 1.0 | **PASS, 6 executions:** screen-family journey; real IME editing; all seven language choices plus Device language persisted across recreation without changing learning data; native libraries; lesson quiz/completion; full practice/results/wardrobe/recreation. |
| 320 dp: same pixels, 540 dpi, font 1.0 | **PASS, 2:** screen-family journey and real IME editing. |
| 200% text: same pixels, 480 dpi, font 2.0 | **PASS, 2:** screen-family journey and real IME editing. |
| 320 dp + 200%: same pixels, 540 dpi, font 2.0 | **PASS, 7:** screen-family journey, real IME editing, name draft/selection recreation, startup read fallback/recovery, locale-read error/Retry, native composition/replacement/commit/cancellation with saved-name recreation, and exact Settings scroll restoration. |
| Short landscape: 1000 × 360 px, 160 dpi, font 1.0; API 26 configuration 952 × 336 dp | **PASS, 1:** screen-family journey with compact arrangement and full-control bounds assertions. |
| Live resize/rotation/recreation: 160 dpi, font 1.0 | **PASS, 1:** 800 × 900, 1200 × 600, 900 × 360 and 320 × 800 px, then rotation and explicit Activity recreation; chosen Explore time and navigation retained. |

These are **19 successful method/configuration executions across retained runs**, not one initially clean suite. The screen-family journey visits Home, Explore, learning, practice, challenge, Play Together, companions, clock/custom styles, wardrobe, Settings and the language picker. Each real-IME run covers English, Finnish, Swedish and German with installed Gboard `6.2.34.154174348-preload-x86_64` (`25623411`), visible keyboard/inset assertions, cursor edits and persisted names. The final composition/scroll pair explicitly selected the existing test APK's native IME/TTS fixtures; silent fixture speech is not listening evidence.

**Initial FAIL/BLOCKED observations, preserved:** the first normal screen-family attempt timed out opening Settings after Back navigation; the unchanged isolated replay and subsequent size cases passed, so its cause remains unproven. The first short-window attempt was **BLOCKED** by the image's Nexus Launcher `NullPointerException` dialog; closing that guest system app allowed an unchanged replay to pass. The first resize test **FAIL** exposed the configuration-dp/full-window mismatch described above; a Google Search crash dialog was also observed. After the scoped test correction and closing those guest system apps, resize/rotation/recreation passed. No reproducible Clocklings application defect was established; no production repair was made.

**APK identity (SHA-256):** application `fi.clocklings`, version 1 / `1.0`, minSdk 26, targetSdk 37; `app/build/outputs/apk/debug/app-debug.apk`: `B6E8E99D667DAFCB622FEA1CD2262781EB5B08F6ECFAB971783DFE41CD0433D0`, unchanged throughout all runtime runs. Initial test APK: `1BA90C7204694CD43982FA4E20275F1281BDE094977831E3C7BB5941C1F55326`; final test APK after the wait correction: `CE4C338CF040B9ADB6AD35996F8B01613DAF4877A235A2A557154A11D80A7B37`, at `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Installed APK hashes matched the corresponding local artifacts. Only the final rotation and composition/scroll runs used the corrected test APK.

**NOT_RUN:** physical devices (including unavailable Pixel 9), actual Chromebook/Googlebook runtimes, TalkBack listening, other API/16 KB reruns, owner-operated `lc`/`sc`, broader static/security audits, remote CI, uploads and publication work. Android emulator resizing is not Chromebook/Googlebook compatibility evidence.

Local-only receipts: [`reports/focused-api26/`](../../reports/focused-api26/) contains the command logs, per-run results, configuration/APK identities and original failure evidence. The dedicated emulator and all tracked temporary Gradle/helper processes stopped; its owned `.scratch` AVD/temp directory was deleted after path/reparse-point checks. Both final APKs remain at the paths above. No commit, push or PR was made.

## Implemented application

| Area | Implemented behavior |
| --- | --- |
| Identity and architecture | Independent Clocklings application (`fi.clocklings`) with its own launcher/splash branding, Kotlin, Jetpack Compose, Navigation 3, manual AppServices and Preferences DataStore. The original private application's source and data remain separate. |
| Learning | Eight standard lessons, optional 24-hour lesson, demonstrations, quizzes and a learning path connected to the existing five practice levels. Completion/progress and retry behavior are implemented. |
| Practice and challenge | Read/set clock practice, feedback, results, best stars and the optional one-minute challenge with an absolute monotonic deadline, best score and replay/retry actions. |
| Explore and Play Together | Interactive clock exploration; set, ask, reveal and swap turns in Play Together. Hidden answers stay hidden from accessibility until revelation. |
| Personalization | Nine companions, no-companion mode, companion names/colors/outfits, recorded companion voices, eight clock appearances including constrained custom styling, and local saved choices. Stars unlock thresholds rather than being spent. Bunny, car and unicorn were completed after the earlier pilot. |
| Language | English, Finnish, shared Swedish, German, French, Spain Spanish and Brazilian Portuguese throughout content, controls and accessibility text. Device `pt` and `pt-PT` deliberately use Brazilian content. Language-specific clock mathematics/wording, half-hour conventions and 24-hour explanations have independent fixtures. |
| Audio | Recorded effects and on-demand speech using an eligible installed offline voice in the content language. Brazilian speech requires a Brazilian voice. Learning works without an eligible voice; absent effects are silent rather than queued for later playback. |
| Settings and data | Language, sound/speech, motion and existing learning/display preferences; progress reset; local persistence; visible failure/retry paths. No new account, analytics, backend or data-transfer feature was added by this work. |
| Accessibility and input | Accessible names/states, answer feedback, visible keyboard focus, clock arrow-key controls, pointer interactions, enlarged-text layouts, IME/inset handling and corrected edit-history routing. Human and automated evidence have different scopes, recorded below. |

The current toolchain uses AGP 9.5.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Material 3 1.4.0, Navigation 3 1.2.0, Lifecycle 2.11.0 and DataStore 1.3.0; minSdk 26, target/compileSdk 37. The historical audit/adaptive checks below used AGP 9.5.0. PR #12 updated AGP separately on 7 November; the subsequent 407-test suite, debug app/test builds, ktlint, Detekt, lint and stability checks passed with 9.5.1 before commit `49e1c16`. Adaptive work introduced no dependency upgrade, SDK change, navigation redesign, text-field migration or new experimental API. Earlier audit code already had a narrowly scoped BringIntoViewSpec opt-in; this is not presented as a new adaptive migration.

## Application corrections completed during the audit

| Area | Completed correction or verified contract |
| --- | --- |
| Startup and storage reads | Added visible loading while initial data is pending. Locale-read failure and Retry respect system insets. Initial I/O fallback does not overwrite stored progress; later failures retain known data and recovery remains possible. |
| Failed edits and retries | Failed companion-name drafts retain the error and Retry through restoration. Name, color and item operations keep their own failure state; an unrelated save cannot clear or retry the wrong operation. Preview/tab-switch and item-slot retry regressions were corrected in preceding reviews. |
| Pending saves and reset | Lesson completion praise waits for successful persistence while correct-answer feedback remains immediate. After a destructive reset is submitted, dismissal/cancellation is blocked until completion rather than implying the committed operation can be cancelled. Pending-write exit, failure, reopen and retry behavior was exercised. |
| Persistence boundaries | Real owned DataStore files were reopened and deliberately corrupted for recovery tests. A staged write was interrupted after serialization and before fsync/rename; old-value recovery and later successful commits were checked. These observations are not a universal power-loss guarantee. |
| Activity and process state | Audio ownership counts resumed Activities, so pausing one does not mute another resumed instance. Navigation/task/draft/selection/scroll state is retained across the tested resize, rotation and Activity recreation cases. Actual process-death observations follow the separately accepted unfinished-session restart policy; they do not promise seamless durable resume of every unfinished session. |
| Keyboard and focus | Shared controls and interactive clocks show visible focus. Clock arrows use existing nudge mathematics. Focus-driven scrolling keeps focused labels out of fades/system bars. Modal Cancel, Back and Escape and complete task journeys were verified. |
| Semantics | Correct/Incorrect answer state and a polite live region are exposed after feedback. Name-field errors are associated with the field. Inactive reserved Settings speech text is excluded from semantics. |
| Scroll and initial insets | Restored Settings scroll no longer clamps against a temporary zero-inset viewport or a shorter speech-availability message. Exact offset restoration is tested; no arbitrary delay or relaxed tolerance was substituted. |
| Audio and animation resources | Unsuccessful AudioTrack loads release resources; failed recorded-clip loads do not prevent later clips. Expired confetti stops unnecessary catch-up work. Finite speech bursts and native stall/background cancellation/recovery were tested. |
| Bounded work | Challenge answer-layout cache retains at most 16 configuration keys. Consecutive I/O retries back off from 1 to 30 seconds and reset after successful reading, reducing repeated reads/logging. |
| Large graphics | Oversized clock-style previews and compact Home artwork were constrained after rendered failures were reproduced. Later adaptive work extends the layout treatment across existing screens. |
| Gboard history | Exact Ctrl+Z / Ctrl+Shift+Z routing now reaches Compose's normal focused-view dispatch before the IME, preserving two undo and two redo operations. No custom undo store, production reflection or text-field API migration was added. |
| Test infrastructure | Replaced a fixed practice-progression sleep with an indexed-task wait. Controlled native IME/TTS fixtures observe current input requests and actual lifecycle events. The test receiver registration that blocked lint now uses `ContextCompat.registerReceiver` with the explicit exported flag needed by the separate test APK. |
| Localization lint | Investigated 27 plural-category warnings and two German repeated-word warnings. Proven unreachable count categories and grammatical false positives received element-specific documented exceptions with regression coverage; global rules were not disabled. Current lint's four warnings concern dependency updates, not those old translation warnings. |
| Dependency and notice evidence | Reconciled resolved dependencies, maintainer/source/advisory facts and 123 license-metadata rows; checked packaged font notices, corrected missing Protobuf attribution and reconciled Okio evidence. This does not establish owner artwork/audio/code rights or final-distribution licensing. Earlier DeepSec dependency/test corrections and their checks remain recorded in STATUS.md; no fresh vulnerability scan or remote-alert status is claimed here. |

## Adaptive implementation and final visual correction

Layout decisions use actual available width, height and font scale, including portrait, landscape, narrow, wide, short and resized windows. The Activity is resizable and no longer portrait-locked; touchscreen hardware is optional. Content/clock widths are bounded, and instructions remain readable rather than stretching indefinitely.

Home pairs its scene with its task menu when space permits. Explore, lessons, practice, challenge, Play Together and personalization pair visuals with controls where useful. Lists, results and settings retain their appropriate sequential layout. Narrow windows and enlarged text reflow and scroll. No navigation rail or unnecessary multi-pane navigation was added.

The owner correctly rejected the first polished short-window lesson/challenge/together images: visible primary buttons did not prove that playback/setup controls were unobscured. The final correction uses a dedicated compact arrangement below 480 dp height when width and font scale permit it:

- The clock uses the left column's height instead of being squeezed above a full-window footer.
- Instructions occupy the right pane; a separate action row reserves its own height and cannot cover them.
- Compact lesson playback controls sit beside Previous/Next. Compact Play Together places Random time beside Pass to partner and uses the existing instruction text without the decorative companion.
- Normal-height/narrow layouts retain the existing scaffold and tested challenge clock sizing, animation and reduced-motion behavior.
- Independent composition slots and explicit action-focus restoration preserve button identity, the selected primary action and scroll across layout thresholds. Play Together retains focus through phase changes.

The existing palette, theme tokens, seven languages, touch targets and Gboard correction were preserved. No decorative colored edge stripes or middle-dot separators were added. At enlarged text sizes, long instructions may require scrolling; all text is not promised to fit simultaneously.

## Validation actually performed

### Evidence rules

The original discovery mapped **877 checks across 60 sections**. Mapping is coverage of inspection, not 877 passing tests. Subsequent execution counts are snapshots from different versions and sometimes repeated configurations, so they must not be added into a single current test total. Superseded failures remain recorded. Automated semantics are not human TalkBack listening, and resized Android windows are not Chromebook/Googlebook runtime evidence.

### Build, local tests and static analysis

| Checkpoint | Actual result and version boundary |
| --- | --- |
| Earlier application/localization | Foundation, companions, artwork, settings, localization, persistence and review regressions were validated in their dated implementation checkpoints. Seven-language resource parity and checker self-tests passed. Historical gallery coverage includes new companions and their wardrobes; earlier pilot “still to do” notes were superseded. |
| Audit local corrections | Full JVM suites progressed through 395 and then 401 passing tests. Localization lint verification covered Android resource selection on API 26, 33 and 37. These are retained historical results, not a rerun of the final adaptive version. |
| Audit completion | 31 focused JVM tests passed, plus debug/test builds, ktlint, Detekt and both Compose stability checks. |
| Final input correction | Full JVM suite: **402 passed**, zero failures/errors/skips. Builds, ktlint and Detekt passed. |
| Initial adaptive implementation | Full JVM suite: **404 passed**, followed by two focused saved-state tests. Builds, ktlint, Detekt and stability checks passed. Lint subsequently passed after the receiver correction. |
| Adaptive polish intermediate version | Full JVM suite: **406 passed**. That invocation still failed style checks, which were subsequently corrected; later focused runs of 23 and then 12 tests and static checks passed. It is not described as a wholly successful initial Gradle invocation. |
| Final compact-layout version | **24 focused JVM tests passed**, zero failures/errors/skips: ChallengeInteractionTest 8, AdaptiveContentTest 4, ClockTaskScaffoldTest 1, StartupInsetsTest 1 and ScreenshotReviewUiTest 10. Debug app/test builds, ktlint, Detekt, lint and debug/release stability checks passed. |
| Current lint | **0 errors, 4 existing dependency-update warnings**. No upgrade was introduced merely to silence them. |
| Baselines and assertions | Compose stability signatures were inspected before accepting scoped baseline updates. Screenshot baselines were not regenerated to accept failures. Final visibility assertions remain strict; the action-focus test checks the second of two enabled buttons, node identity and saved scroll offset across layout thresholds. |

The compact-layout handoff used the focused regression coverage above; the subsequent complete current-checkout JVM result is recorded in the 8 November section. Owner-run `lc`/`sc` and fresh CI were not run for either this verification or the final audit/adaptive handoff. Earlier CI or security-check results do not establish a current remote result.

### Actual Android runtime coverage retained from the audit

| Environment / subject | Executed evidence | Limit |
| --- | --- | --- |
| API 26 and API 32 dedicated x86_64 emulators | Minimum and final legacy-locale endpoints; native composition/history, startup read fallback/recovery, language changes, Settings scroll/recreation, native libraries and TTS stall/cancellation/recovery. Normal, 320 dp, 200% text and combined cases ran. | Predates adaptive changes. API 27–31 were not individually executed. An old physical phone was not required. |
| API 36 x86_64 with actual 16 KB pages | Both guest and in-process page-size checks returned 16384; both packaged native libraries loaded. Runtime/input matrix and later corrected-input regression passed. | Emulator execution, not ARM64 hardware or final signed/optimized distribution. |
| Runtime/provider continuation | **56 distinct successful method/configuration observations across retained runs**, followed by explicitly identified final-fixture groups. | Not one clean 56-test invocation; earlier fixture and observation failures remain separate. |
| Final corrected-input APK | Eight successful Gboard/composing-history observations across normal/320 dp/200%/combined; four regression passes on API32 and four on actual 16 KB API36. | Earlier APK than the final adaptive build. Gboard version and hashes are pinned in the historical audit. |
| API 36 core/state/error work | Real owned storage corruption/reopen and staged-commit interruption, 40 pending-write/exit observations, process-death policy, draft/error restoration, two simultaneously resumed Activities and real split-screen audio ownership. Final audit completion recorded **36/36** selected observations. | Scoped failure contracts and observed configurations, not universal durability or every lifecycle event. |
| API 37 phone evidence | Historical startup, locale, name-field/inset and provider observations on the identified phone. | Limited earlier debug APK and observed configuration; no current equipment access is assumed. |
| Retention/performance | Repeated navigation/recreation, Activity collection controls and bounded memory trends; earlier debug graphics/frame observations. | A ComposeTestRule retention diagnostic failed and was isolated with an independently passing ActivityScenario control. No general no-leak guarantee or release benchmark is claimed. |

### Adaptive window, input and state coverage

All adaptive device work used the dedicated **ClocklingsReview20261030 / emulator-5590, API 36**. Android key/mouse events were injected through actual platform paths. This was not a physical keyboard/trackpad test. Host QEMU crashes and interrupted invocations were not counted as passes.

| Window / text size | Completed coverage |
| --- | --- |
| 320 x 800 dp / 100% | Complete screen-family journey and narrow layout checks. |
| 420 x 900 dp / 200% | Complete screen-family journey with enlarged text. |
| 320 x 640 dp / 200% | Combined narrow/enlarged-text journey, strict action visibility and real IME editing. Earlier adaptive runs also covered 320 x 800 / 200%. |
| 1000 x 360 dp / 100% | Final screen-family journey; lesson, challenge and Together introductions in all seven languages; raw unclipped clickable-control bounds and clock bounds of at least 200 dp. |
| 1000 x 360 dp / 200% | Screen-family journey rerun after the final compact lesson playback-row adjustment. Final screenshot shows playback/navigation visible. |
| 1000 x 900 dp / 100% | Tablet-sized screen-family journey and state/input coverage. |
| 1280 x 720 dp / 100% | Laptop-sized screen-family journey. |
| Live transitions | Multiple widths/heights, portrait/landscape, 360/480 dp height threshold crossings, rotation and Activity recreation; active lesson/practice, selected time, drafts/selections and scroll retained in the relevant tests. Real Android split-screen was separately exercised. |
| Keyboard and pointer | Complete lesson/practice/challenge/personalization/Together journeys across the adaptive stages; final compact action focus and phase changes; native mouse click, drag and wheel scrolling. |
| IME/insets | Real multilingual editing, cursor/selection/paste, field visibility, IME-driven layout transitions, three-button/cutout evidence and retained Gboard history checks. The final compact correction did not change text-field routing. |

Initial adaptive work recorded 47 successful selected executions; the subsequent polish recorded 20. The final compact report records its own matrix, five state/pointer tests, three keyboard journeys and a combined-size real IME test. These are overlapping observations, not counts of distinct features.

After the **last** compact lesson playback change, the final APK passed four checks at 1000 x 360 / 100%: the complete screen-family journey, all seven languages for the three compact introductions, lesson/practice threshold/recreation state and the whole keyboard-only lesson. The 1000 x 360 / 200% screen-family journey passed again. Unaffected phone/tablet/laptop and other-flow evidence was retained from immediately before that compact-only adjustment, rather than falsely attributed to a new execution.

### Human audio/accessibility and language evidence

The audit records an earlier owner-assisted Finnish TalkBack session covering Home, Settings/Language reading order and state, lesson incorrect/correct feedback, challenge timer behavior, Explore alternative clock actions, Together, wardrobe/name controls, error announcement/Retry and retained draft. Modal focus evidence was recorded separately. Those observations used the earlier installed APK; they do **not** verify the final compact pane arrangement's spoken order.

The same historical audit records disconnected/offline listening to effects and eligible Finnish, English, Swedish, German, French and Brazilian Portuguese voices, including speech stopping on backgrounding. Spanish had no eligible installed voice and was not counted as a spoken pass. Controlled native TTS stall tests additionally verified `started -> stopped -> released -> completed`; silent fixture output is not human listening evidence.

Automated language checks cover all seven resource sets, clock-language boundary fixtures, lesson answers, formatting/plurals and layout. They are not professional native-speaker certification. An additional linguistic review is optional quality evidence, not an invented mandatory release gate.

## Current visuals and deliverable

The final compact evidence directory contains **15 screenshots**. The nine final English/Finnish/German lesson/challenge/Together images and the enlarged-text lesson were visually inspected after the last build; the remaining five phone/tablet/laptop captures retain applicable unchanged-layout evidence.

| View | Current evidence |
| --- | --- |
| Finnish short lesson | [fi-lesson.png](../../reports/adaptive-layouts/compact/screenshots/fi-lesson.png) |
| Finnish short challenge | [fi-challenge.png](../../reports/adaptive-layouts/compact/screenshots/fi-challenge.png) |
| Finnish short Play Together | [fi-together.png](../../reports/adaptive-layouts/compact/screenshots/fi-together.png) |
| Short window, 200% text | [short200-lesson.png](../../reports/adaptive-layouts/compact/screenshots/short200-lesson.png) |
| 320 dp / 100%, 200%, combined | [320 dp](../../reports/adaptive-layouts/compact/screenshots/phone320-lesson.png), [200%](../../reports/adaptive-layouts/compact/screenshots/phone200-lesson.png), [320 dp + 200%](../../reports/adaptive-layouts/compact/screenshots/phone320200-lesson.png) |
| Tablet and laptop windows | [tablet](../../reports/adaptive-layouts/compact/screenshots/tablet-lesson.png), [laptop](../../reports/adaptive-layouts/compact/screenshots/laptop-lesson.png) |

The rejected `polish/screenshots/short-lesson.png`, `short-challenge.png` and `short-together.png` are historical failure evidence, not accepted final layouts.

Adaptive-layout checkpoint APK (8 November, before full unlock): `C:\Dev\Clocklings\app\build\outputs\apk\debug\app-debug.apk`.

SHA-256: `B6E8E99D667DAFCB622FEA1CD2262781EB5B08F6ECFAB971783DFE41CD0433D0` (verified on 8 November against the installed API 26 artifact).

This historical hash identifies the adaptive-layout verification build, not the current file at that path. The current full-unlock debug APK is recorded in [Dependency-warning follow-up](#dependency-warning-follow-up-8-november). Neither is a signed publication deliverable.

## Reconciled audit follow-ups

The input audit ended at **14 RESOLVED / 0 OPEN / 0 BLOCKED / 5 DEFERRED**. The later adaptive scope decision changes how the window row is described below; repeating that old aggregate as today's complete backlog would be misleading.

| Original follow-up | Current disposition |
| --- | --- |
| FU01 — artifact/release | Debug and historical unsigned optimized package evidence completed. Final signing, distributed artifact identity and optimized distribution runtime remain deferred with publication. |
| FU03 — runtime verification | Original scoped minimum/legacy/16 KB gaps resolved. Later adaptive validation and its explicit unavailable evidence are recorded above. |
| FU04 — Console/store | Deferred owner/account/listing/audience/distribution decisions. No current Console or remote track claim. |
| FU05 — official sources/dependencies | Dated source/dependency/advisory reconciliation completed. Recheck applicable requirements when actual publication is authorized; historical research is not perpetual compliance evidence. |
| FU06 — audio | Scoped installed-voice/effect/offline/background observations completed. Missing Spanish voice and provider/device limits remain explicit. |
| FU-A02-STATE | Startup/read fallback/recovery and relevant legacy/current runtime checks resolved. |
| FU-A02-WRITES | Identified pending-write, failure, interruption, exit/reopen and retry contracts resolved. |
| FU-A05-LICENSE | Local dependency/notice corrections completed. Owner provenance/grants and final-distribution obligations remain deferred. |
| FU-A08-ASYNC | Scoped resource, cancellation, Activity retention and controlled native-provider stall/recovery criteria resolved. |
| FU-A09-RESTORE | Accepted restart policy and scoped process/Activity/draft/scroll/multi-resume criteria resolved. Adaptive state preservation additionally verified. |
| FU-A11-FOCUS | Scoped control/journey/visible-focus/modal criteria resolved; compact action-focus regressions added. |
| FU-A12-INSETS | Original legacy/current phone/IME criteria resolved; adaptive resizing/IME checks added. Desktop caption-runtime evidence remains unavailable. |
| FU-A13-WINDOWS | Phones/tablets/laptops are selected scope and adaptive implementation/emulator validation are completed. Actual Chromebook/Googlebook runtime evidence is unavailable. Specialized fold-hinge/external-display behavior is unselected, not an automatic unfinished feature. |
| FU-A14-INPUT | Current scoped application input requirements resolved, including verified Gboard undo/redo routing. Original Ka/null event attribution is historically unknown, with no reproduced current composition defect. No repeat broad input audit is required. |
| FU-A15-SEMANTICS | Original finite semantics/human scenarios resolved. Human review of the final adaptive arrangement has not been performed and is unavailable under the current owner constraint. |
| FU-A20-CACHE | Bounded retained configuration cache resolved and tested. |
| FU-A31-DISCLOSURE | Local data/provider inventory completed; owner/contact/privacy/retention/audience/store reconciliation remains deferred. |
| FU-A33-RETRY | Measured retry/log-rate issue resolved and regression tested. |
| FU-A41-SYNC | Fixed-sleep progression replaced and the full instrumented journey verified. |

The earlier discovery-only actions FU-A01.02-01, FU02 and FU07 are also complete. The original Ka/null traces cannot be reconstructed from missing historical events. Source recovery and passing replays do not turn that unknown into a proven cause; equally, the unknown is not a current application bug or new owner task.

## What may still be done

### Unavailable evidence, accepted as a current limitation

| Item | Current treatment |
| --- | --- |
| Actual Chromebook and Googlebook runtimes | No suitable equipment/runtime was available. These are distinct targets; neither is certified by phone-AVD window resizing. No equipment acquisition is requested. |
| Desktop caption controls and physical trackpad gestures | Not observed on actual target equipment. Android injected mouse/wheel and safe-inset evidence remains valid within its scope. |
| Human TalkBack review of the final adaptive layouts | Unavailable under the owner's current constraint. Historical human checks and current automated semantics/focus tests remain evidence; neither is relabeled as a new listening session. |
| Latest adaptive APK on minimum-API physical hardware | Not available and not required as a new owner task. The current API 26 emulator rerun is recorded above; it does not establish physical-device evidence. |

There is no requirement to buy, borrow or obtain these devices to complete the authorized implementation. These limitations prevent unsupported compatibility/accessibility claims; they do not by themselves identify a source defect.

### Optional additional checks that do not inherently require owning those devices

- The optional focused **latest-APK API 26 emulator** rerun was completed on 8 November, within the scope and evidence limits above. This does not reopen the older full audit or other API matrices.
- An **emulator-hosted accessibility session** could be assessed if a usable TalkBack setup and observation method are available. That setup was not verified in this consolidation, and automated accessibility nodes alone would still not prove spoken behavior.
- A suitable dedicated desktop emulator, if available and separately used, could add caption/window evidence. It would remain emulator evidence, not physical Chromebook/Googlebook/trackpad certification.
- Owner-run `lc`/`sc` and later branch CI may provide current check reports. None is claimed for this final snapshot. The four dependency-update lint warnings do not authorize unrelated upgrades.

These are options for more evidence, not a mandatory new queue or permission to repeat completed matrices. No unresolved product decision is needed to close the current implementation work.

### Deliberately deferred until publication work is requested

1. Production signing and final distribution artifact/certificate identity, optimized distributed runtime and release validation.
2. Owner artwork, audio, font and imported-code provenance/grants, plus final packaged license/NOTICE reconciliation.
3. Publishing identity/contact, privacy/provider/retention/deletion wording and required public/in-app/store disclosures.
4. Play Console/account/track/listing/audience/country/commercial/distribution declarations and current applicable publication requirements.

Old proposals such as additional profiles, everyday timers/routines/widgets, adult controls or specialized fold/external-display features are not automatically approved implementation debt. They require a separate product decision if pursued. No curriculum mapping, Teacher Approved work or partial/MVP publication was added.

## Cleanup and preservation

The 6 November consolidation checked **25 exact previously named cleanup paths**. All were absent: seven audit temporary directories; `reports/adaptive-layouts/.scratch`; the nine polish disposable files; the seven compact disposable files; and `app/build/compact-runtime`. Their old blocked-deletion instructions are historical and do not represent current outstanding cleanup. That task verified absence; it did not perform or claim the earlier deletions.

At that checkpoint, the final APK and 15 compact screenshots remained present. Prior task receipts recorded shutdown of owned emulators/helpers/temporary Gradle processes. The 6 November documentation task started no emulator, server or watcher and created no disposable scratch artifacts. The separate 8 November verification and its completed cleanup are recorded above. No unrelated files, application data, processes, commits or Git history were changed.

## Evidence references

This document is sufficient for the current status and remaining-work decisions. The following records retain detailed commands, logs, historical failures and chronology:

Evidence under `reports/`, including screenshots, logs and APKs, is local-only and Git-ignored. Those relative links require the original working directory and do not resolve in a fresh GitHub checkout. They are historical local evidence, not downloadable repository artifacts.

| Record | Purpose |
| --- | --- |
| [Implementation history](STATUS.md) | Foundation, product decisions, localization, companions, branding, review fixes and dated validation. Older headings/branch names remain historical. |
| [Android/Google audit](../../reports/android-google-audit-discovery.md) | 877-item discovery, corrective work, runtime/provider/physical observations, dependency/notice research and final input closeout. Read final dispositions rather than early OPEN counts. |
| [Adaptive implementation](../../reports/adaptive-layouts/REPORT.md) | Whole-application scope, official runtime-source links, initial 404-test and 47-execution evidence. |
| [Adaptive polish history](../../reports/adaptive-layouts/polish/REPORT.md) | Intermediate 406-test suite, phase-focus fix and retained input/state evidence. Its three rejected short-window captures are superseded. |
| [Final compact correction](../../reports/adaptive-layouts/compact/REPORT.md) | Final 24-test result, actual window matrix, strict bounds/focus assertions and current screenshots. Its cleanup section is historical; the paths were rechecked absent above. |
| Final compact receipts | `reports/adaptive-layouts/compact/validation.log`, `jvm-results.json`, `stability.log`, `lesson-final.log`, `short200.log`, and the named matrix/input logs. |

Official Chromebook/Googlebook/desktop guidance was checked during adaptive implementation and linked in its report. It describes distinct runtime/input/window requirements, not equivalent runtimes or certification. Those sources were not fetched again for the consolidation or this focused verification.
