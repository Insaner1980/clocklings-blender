# Clocklings: German, French, Spanish and Brazilian Portuguese language research

Date: 4 November 2026

Status: Research and proposed editorial baseline. The four languages are not implemented by this task. The explanations in this document are in English; target-language examples retain their original spelling.

## 1. Purpose and evidence

Prepare the linguistic basis for adding German, French, Spanish and Brazilian Portuguese to a children's clock-learning app. Correctness includes grammar, the time a phrase actually denotes, natural usage, and agreement between explanations, clock highlights, answer options and speech.

Three evidence levels apply throughout:

- **Source-supported rule:** a rule or construction supported by the linked language reference or teaching source.
- **Proposal:** a consistent choice for Clocklings among valid alternatives. A source supporting a construction does not establish that this exact product choice is uniquely correct.
- **Verification still needed:** final translation review, implementation tests, rendered layouts and actual pronunciation. None is claimed complete here.

Examples below are independently authored applications of the cited rules, not output copied from a future formatter. They are candidate fixtures, not a native-speaker certification. Sources were consulted on 4 November 2026. Access limitations are recorded in section 12.

The owner's new request expands the earlier three-language scope. Older planning documents that restrict the release to English, Finnish and Swedish do not override that request. This research does not introduce curriculum mapping, a paid-review dependency, or a partial language release.

## 2. Locale and style proposals

| Language | Proposed content baseline | Important distinction | Decision status |
| --- | --- | --- | --- |
| German | Standard German, Germany-oriented teaching examples | Regional clock expressions remain valid; choose one consistent set for the app. | Proposed; not a separate Austria/Switzerland translation. |
| French | Standard French, France-oriented wording | Prefer `moins le quart` in the app; do not mix regional alternatives indiscriminately. | Proposed; no separate Canadian French content set. |
| Spanish | Spanish as used in Spain, `es-ES`; proposed teaching style uses `y` / `menos` | Many American varieties commonly use minutes + `para` + upcoming hour. Neither is an incorrect answer when it denotes the target time. | **Confirmed by the owner on 4 November 2026:** Spain Spanish. |
| Brazilian Portuguese | Brazilian Portuguese, `pt-BR` | Preserve the Brazilian identity in content and voice selection. | Brazil was explicitly requested. |

The German regional variation is documented by the [Atlas zur deutschen Alltagssprache](https://www.atlas-alltagssprache.de/uhrzeit/). Spanish alternatives are documented in [RAE/ASALE, *hora*, sections 2.1 and 2.1.1](https://www.rae.es/dpd/hora).

Proposed child-facing address: German `du`, French `tu`, Spanish `tú`, Brazilian Portuguese `você`, often omitted when the sentence is clear without it. Use short direct instructions, consistent verb forms and encouraging feedback. Avoid unnecessary gendered praise and formal administrative language. These are editorial proposals, not grammatical requirements.

## 3. Working glossary

Preferred terms for translation consistency. Introduce each technical term beside a visible feature, then use it consistently. The short/long distinction describes Clocklings' actual hands; do not make colour the only identifying cue.

| Concept | German | French | Spanish | Brazilian Portuguese |
| --- | --- | --- | --- | --- |
| Clock | Uhr | horloge | reloj | relógio |
| Clock face | Zifferblatt | cadran | esfera del reloj | mostrador do relógio |
| Hand | Zeiger | aiguille | aguja | ponteiro |
| Hour hand | Stundenzeiger | aiguille des heures | aguja de las horas | ponteiro das horas |
| Minute hand | Minutenzeiger | aiguille des minutes | aguja de los minutos | ponteiro dos minutos |
| Short hand | kurzer Zeiger | petite aiguille | aguja corta | ponteiro pequeno |
| Long hand | langer Zeiger | grande aiguille | aguja larga | ponteiro grande |
| Clockwise | im Uhrzeigersinn | dans le sens des aiguilles d'une montre | en el sentido de las agujas del reloj | no sentido horário |
| One hour, duration | eine Stunde | une heure | una hora | uma hora |
| One minute | eine Minute | une minute | un minuto | um minuto |
| Two minutes | zwei Minuten | deux minutes | dos minutos | dois minutos |
| Half an hour, duration | eine halbe Stunde | une demi-heure | media hora | meia hora |
| A quarter of an hour, duration | eine Viertelstunde | un quart d'heure | un cuarto de hora | um quarto de hora |
| Noon, standalone | Mittag | midi | mediodía | meio-dia |
| Midnight, standalone | Mitternacht | minuit | medianoche | meia-noite |
| What time is it? | Wie spät ist es? | Quelle heure est-il ? | ¿Qué hora es? | Que horas são? |

Terminology anchors: [Ravensburger's clock-learning instructions, PDF page 4](https://product-files.ravensburger.cloud/manuals/664135.pdf#page=4), [Duden: Stundenzeiger](https://www.duden.de/rechtschreibung/Stundenzeiger), [Larousse Junior: aiguille](https://www.larousse.fr/dictionnaires/junior/aiguille/479), and [Goiânia education department: analogue clock](https://sme.goiania.go.gov.br/conexaoescola/ensino_fundamental/matematica-relogio-analogico/). Spanish `manecilla` is also a legitimate term, documented by [El Colegio de México](https://dem.colmex.mx/ver/manecilla); the proposed set uses `aguja` consistently. The glossary is a preferred vocabulary list, not a claim that every entry was individually approved by a native reviewer.

Keep clock time separate from elapsed duration. German particularly needs `Uhr` for clock readings and `Stunde` for an hour of duration. A quarter-hour duration can be taught with the fraction word even when the preferred clock reading uses the number fifteen.

## 4. German rules and proposed output

**Source-supported:** `halb` names the upcoming hour. At 03:30, the reference is four. Common five-minute readings can also refer to the half-hour. Goethe's [project-time worksheet, worksheet 7, printed page 8](https://www.goethe.de/resources/files/pdf270/didaktische-materialien-zu-gfdz-v1.pdf) illustrates :25, :30 and :35 this way. Only its indexed extract was accessible; see section 12.

**Proposal for five-minute teaching:**

| Minute | Construction | Reference hour |
| --- | --- | --- |
| :00 | hour + `Uhr` | Current |
| :05, :10, :20 | minute number + `nach` + hour | Current |
| :15 | `Viertel nach` + hour | Current |
| :25 | `fünf vor halb` + hour | Upcoming |
| :30 | `halb` + hour | Upcoming |
| :35 | `fünf nach halb` + hour | Upcoming |
| :40, :50, :55 | remaining minutes + `vor` + hour | Upcoming |
| :45 | `Viertel vor` + hour | Upcoming |

At other minutes, propose the exact hour/minute reading: `drei Uhr sieben`, for example. This avoids inventing a rule that children must express every unusual minute relative to a half-hour. Goethe's [language-game materials, PDF pages 64–65](https://www.goethe.de/resources/files/pdf347/handreichungen_spielsammlung-v1.pdf#page=64) contrast ordinary relative expressions with precise numerical readings. Never use an approximation such as `kurz vor` as the exact answer to a minute-precision exercise.

The choice of `zwanzig nach` / `zwanzig vor` at :20/:40 is editorial. The [Atlas](https://www.atlas-alltagssprache.de/uhrzeit/) documents half-hour-relative regional alternatives. Do not use a valid regional reading as a wrong answer.

**Grammar:** use `ein Uhr` but standalone hour `eins`, including `halb eins`. Do not apply a global replacement of every `eins` with `ein`; the surrounding construction matters. [Duden's explanation](https://www.duden.de/sprachwissen/sprachratgeber/Ein-eins-unbestimmter-Artikel-oder-Zahlwort) supports this distinction. Preserve noun capitals and compounds such as `fünfundzwanzig`; keep `ß` in `dreißig` for the proposed Germany-oriented spelling.

For full sentences use `Es ist …`. For literal 24-hour numerical narration use the actual hour, for example `fünfzehn Uhr dreißig`, not a half-hour phrase built from a 24-hour number.

## 5. French rules and proposed output

**Source-supported:** clock sentences use `Il est …`; one hour uses `une heure`, other positive hours use plural `heures`. Ordinary minutes follow the hour directly. Quarter and half-hour constructions use `et quart` and `et demie`; the proposed quarter-to construction is `moins le quart`. [TV5MONDE's time lesson](https://apprendre.tv5monde.com/fr/aides/grammaire-dire-lheure) also distinguishes conversational 12-hour forms from literal 24-hour readings.

**Proposal:** use the common quarter/half forms and five-minute `moins` expressions in 12-hour teaching. For other exact minutes, use the actual hour followed by minutes. For minute one, retaining the word `minute` makes the unit explicit. This is a teaching choice, not a requirement to say the unit at every minute.

French `heure` and `minute` are feminine. In particular, exact-minute numbers ending in one need attention: `vingt et une`, `trente et une`, `quarante et une`, `cinquante et une`. The [GRAC reference, section 74a, PDF page 134](https://grec.methodal.net/IMG/pdf/archive_grac_2025-3.pdf#page=134) explicitly discusses feminine minute readings. Do not reuse a masculine number list without context.

Write `demie` after `heure(s)` but `demi` after `midi` or `minuit`; it does not become `demies` after plural hours. See the [Canadian language portal's time guidance](https://nos-langues.canada.ca/fr/cles-de-la-redaction/heure-ecriture-de-lheure) and [Académie française: minuit](https://www.dictionnaire-academie.fr/article/A9M2278). Choose one accepted number-spelling convention consistently; the examples here retain traditional spacing around `et`, with forms such as `vingt-cinq` otherwise hyphenated. GRAC's all-hyphen spelling reflects a different accepted convention.

For numerical 24-hour narration use `quinze heures trente`. Do not build the lesson's standard numerical reading by attaching `et demie` or `moins` to a 24-hour hour number. A conversational phrase alongside a 24-hour display is a separate output with explicit day context.

At 03:30 the lesson highlights three when explaining the named hour. Physical hand-position explanations still show the hand between three and four.

## 6. Spanish rules and proposed output

**Source-supported:** use `Es la una …` for one and its fractions; use `Son las …` for the other hour readings. With a `menos` expression, this depends on the hour being named: 12:55 becomes `Es la una menos cinco.` The question selected for the app is `¿Qué hora es?` [FundéuRAE](https://www.fundeu.es/consulta/que-hora-es-o-que-horas-son-200/) explains the question and agreement; [RAE/ASALE, section 5.2](https://www.rae.es/dpd/hora) also covers zero hours.

**Confirmed locale:** the owner selected Spanish as used in Spain (`es-ES`) on 4 November 2026.

**Proposed reading policy for that locale:** five-minute readings use the current hour with `y` through :30, then the upcoming hour with `menos`. Special fractions are `y cuarto`, `y media`, `menos cuarto`. For non-five-minute precision, propose the actual hour and numerical minutes. Standard variants must not become distractors merely because the app teaches a different style.

The [RAE/ASALE reference, section 2.1.1](https://www.rae.es/dpd/hora), documents `para` constructions in much of Spanish-speaking America. For example, a quarter to four can be expressed using `un cuarto para las cuatro`. These remain valid regional alternatives; the selected Clocklings baseline is Spain Spanish.

Hour and minute numbers have different agreement contexts: `veintiuna horas` refers to feminine hours; `veintiún minutos` refers to masculine minutes with the noun present. Bare minute numbers use forms such as `veintiuno`. [RAE/ASALE, *uno*, sections 2.2–2.3](https://www.rae.es/dpd/uno) explains gender agreement and shortening before a noun. Apply those rules to fixtures; do not replace all occurrences of `uno` globally. Preserve accents in `dieciséis`, `veintidós`, `veintitrés` and `veintiséis`.

Clock time and appointment time need separate sentence frames: `Es la una` answers the current time; `a la una` answers when an event happens. Do not prepend one fixed verb to every generated phrase. At 03:30 the explanatory hour highlight is three.

## 7. Brazilian Portuguese rules and proposed output

**Source-supported:** hours use feminine forms such as `uma` and `duas`; minutes use masculine `um` and `dois`. Full current-time sentences use singular `É uma hora` and plural `São duas horas`. [Mango Languages' Brazilian Portuguese lesson](https://www.mangolanguages.com/resources/learn/grammar/brazilian-portuguese/how-to-tell-time-in-brazilian-portuguese) explains this distinction and constructions with `para`.

**Proposal:** use `e quinze` for :15 and `e meia` for :30. Teach `um quarto de hora` as a duration/fraction, without requiring a literal quarter-word construction as the ordinary :15 reading. At :35/:40/:45/:50/:55, teach the minutes remaining with `para`; at other exact minutes, use the actual hour plus `e` and the minutes. The exact-minute examples retain units for clarity.

The target after `para` is construction-sensitive: plural hours use `para as`, one can use `para a uma`, noon uses `para o meio-dia`, and midnight uses `para a meia-noite`. The one-hour construction is supported by the indexed extract of [IFSul's Portuguese course, module 1, booklet 1](https://idiomas.ifsul.edu.br/conteudo/portugues/modulo_01/pdf/cadernos/caderno_1/POR_M1_C1_imp_aluno.pdf); its complete PDF was not accessible through the browser tool. Treat the selected article usage as a focused contextual review target, not as a statement that every alternative is wrong.

For a full “minutes remaining” explanation, `Falta um minuto …` and `Faltam dois minutos …` make agreement depend on the remaining minutes. Do not apply the `É`/`São` rule for a direct hour reading to every possible phrase shape.

The half-hour word is feminine `meia`, including after noon and midnight. Preserve hyphens in `meio-dia` and `meia-noite`. The [Brazilian Senate's style guidance](https://www12.senado.leg.br/manualdecomunicacao/estilos/hora) supports these points; its relevant indexed text was accessible, but the page itself timed out. [Goiânia's teaching material](https://sme.goiania.go.gov.br/conexaoescola/ensino_fundamental/quantas-horas-sao/) independently explains the half-hour agreement.

Use Brazilian spellings such as `dezesseis`, `dezessete` and `dezenove`. Retain accents in words such as `três`, `relógio` and `manhã`. At 03:30 the explanatory hour highlight is three. An unavailable Brazilian offline voice should not silently become a European Portuguese voice; preserving silent operation is the proposed fallback.

## 8. Candidate clock fixtures

### 8.1 Clock-face readings without day context

These are bare answer labels, without a sentence-opening verb or final punctuation. The display uses `h:mm` here only to identify the mathematical time. The same clock face can represent morning or afternoon. In particular, bare 12:00 does not establish noon or midnight.

Five-minute rows exercise the proposed conversational style. Other rows deliberately use precise numerical readings. This mixture is an explicit editorial proposal for the new languages, not a change to existing English/Finnish/Swedish output. Sources in sections 4–7 support the constructions; the table is a set of authored test candidates, not a claim that every complete string appears verbatim in a source.

| Time | German | French | Spanish, Spain (`es-ES`) | Brazilian Portuguese |
| --- | --- | --- | --- | --- |
| 3:00 | drei Uhr | trois heures | las tres | três horas |
| 3:01 | drei Uhr eins | trois heures une minute | las tres y un minuto | três horas e um minuto |
| 3:05 | fünf nach drei | trois heures cinq | las tres y cinco | três e cinco |
| 3:07 | drei Uhr sieben | trois heures sept | las tres y siete | três horas e sete minutos |
| 3:15 | Viertel nach drei | trois heures et quart | las tres y cuarto | três e quinze |
| 3:20 | zwanzig nach drei | trois heures vingt | las tres y veinte | três e vinte |
| 3:21 | drei Uhr einundzwanzig | trois heures vingt et une | las tres y veintiuno | três horas e vinte e um minutos |
| 3:25 | fünf vor halb vier | trois heures vingt-cinq | las tres y veinticinco | três e vinte e cinco |
| 3:29 | drei Uhr neunundzwanzig | trois heures vingt-neuf | las tres y veintinueve | três horas e vinte e nove minutos |
| 3:30 | halb vier | trois heures et demie | las tres y media | três e meia |
| 3:31 | drei Uhr einunddreißig | trois heures trente et une | las tres y treinta y uno | três horas e trinta e um minutos |
| 3:35 | fünf nach halb vier | quatre heures moins vingt-cinq | las cuatro menos veinticinco | vinte e cinco para as quatro |
| 3:40 | zwanzig vor vier | quatre heures moins vingt | las cuatro menos veinte | vinte para as quatro |
| 3:41 | drei Uhr einundvierzig | trois heures quarante et une | las tres y cuarenta y uno | três horas e quarenta e um minutos |
| 3:45 | Viertel vor vier | quatre heures moins le quart | las cuatro menos cuarto | quinze para as quatro |
| 3:51 | drei Uhr einundfünfzig | trois heures cinquante et une | las tres y cincuenta y uno | três horas e cinquenta e um minutos |
| 3:59 | drei Uhr neunundfünfzig | trois heures cinquante-neuf | las tres y cincuenta y nueve | três horas e cinquenta e nove minutos |
| 12:00 | zwölf Uhr | douze heures | las doce | doze horas |
| 12:30 | halb eins | douze heures et demie | las doce y media | doze e meia |

Before these become exact-string test expectations, review them independently of the implementation. Pay particular attention to the deliberate switch between relative five-minute and direct exact-minute readings, and to whether the exact-minute exercise should also explain an equivalent “minutes remaining” reading. Do not generate expected answers with the formatter under test.

### 8.2 Full-sentence and agreement checks

| Language / context | Candidate sentence | What it checks |
| --- | --- | --- |
| German 01:00 | Es ist ein Uhr. | `ein` before `Uhr`. |
| German 12:30, no day context | Es ist halb eins. | Upcoming one; standalone `eins`. |
| French 01:00 | Il est une heure. | Singular feminine hour. |
| French 00:30, explicit midnight context | Il est minuit et demi. | Masculine `demi`. |
| French 12:30, explicit noon context | Il est midi et demi. | Same exception at noon. |
| Spanish 12:55 | Es la una menos cinco. | Verb/article follow the named upcoming hour. |
| Spanish 02:00 | Son las dos. | Plural hour sentence. |
| Brazilian Portuguese 01:01 | É uma hora e um minuto. | Feminine hour; masculine minute; singular verb. |
| Brazilian Portuguese 02:02 | São duas horas e dois minutos. | Gender and plural agreement. |
| Brazilian Portuguese 12:30, explicit noon context | É meio-dia e meia. | Singular verb; feminine half-hour. |
| Brazilian Portuguese one minute until four | Falta um minuto para as quatro. | `Falta` agrees with one remaining minute. |
| Brazilian Portuguese two minutes until four | Faltam dois minutos para as quatro. | `Faltam` agrees with two remaining minutes. |

The noon/midnight French distinction is also supported by [Larousse's *heure* usage notes](https://www.larousse.fr/dictionnaires/francais/heure/39843/locution?q=toute-). Do not infer these contextual names from hand positions alone.

### 8.3 Contextual and numerical outputs

| Meaning | German | French | Spanish | Brazilian Portuguese |
| --- | --- | --- | --- | --- |
| Explicit 00:00 label | Mitternacht | minuit | medianoche | meia-noite |
| Explicit 12:00 label | Mittag | midi | mediodía | meio-dia |
| 15:30, conversational label with context | halb vier nachmittags | trois heures et demie de l'après-midi | las tres y media de la tarde | três e meia da tarde |
| 15:30, literal numerical label | fünfzehn Uhr dreißig | quinze heures trente | las quince horas y treinta minutos | quinze horas e trinta minutos |

Propose a compact `h:mm` digital display in 12-hour mode and `HH:mm` in 24-hour mode for the four new languages, with padded minutes and 00:00 at the start of a day. This is an app-display convention, not a universal rule for prose. French prose may use `15 h 30`, Brazilian Portuguese `15h30`, and German `15:30 Uhr`. Spanish punctuation and display options are described by [FundéuRAE](https://www.fundeu.es/recomendacion/horas-grafia/); French distinctions between prose and technical displays are described by the [Canadian language portal](https://nos-langues.canada.ca/fr/cles-de-la-redaction/heure-ecriture-de-lheure).

Numerical narration should read the time naturally rather than spell each padded zero. Exact 00:00/12:00 need complete sentences appropriate to each language. Freeze the separate zero-hour numerical narration in implementation fixtures after a dedicated contextual review; this report does not approve a universal “zero zero zero” reading.

Day-part labels and phrase suffixes are separate grammatical objects. Examples include German `Morgen` / `morgens`, French `matin` / `du matin`, Spanish `mañana` / `de la mañana`, and Portuguese `manhã` / `da manhã`. French early-morning clock expressions can use `du matin` while the scene is still dark. Portuguese `de manhã` and `da manhã` have different sentence uses; [Mango's explanation](https://www.mangolanguages.com/resources/learn/grammar/brazilian-portuguese/how-to-tell-time-in-brazilian-portuguese) distinguishes them.

Do not present the app's fixed morning/evening boundaries as universal cultural or astronomical facts. Preserve the teaching sun/moon behavior unless a change is separately justified. Review phrase suffixes around noon, midnight and a named upcoming hour: for example, contextual French 11:55 can be `midi moins cinq`, without mechanically appending `du matin` to it.

## 9. Lesson adaptation

Inspect the complete scene: text, visible time, hand positions, highlighted number, caption, speech, quiz choices and feedback. Translate the teaching intent, not the English sentence structure.

| Existing lesson / resources | Required adaptation |
| --- | --- |
| `viisarit`, `l_viisarit_*` | Introduce the hand names with short/long cues and the actual current artwork. The current English resources identify a short pink hand, not the older review's red hand. |
| `kierros`, `l_kierros_*` | Distinguish elapsed one-hour duration from an hour on the clock. Check all minute-count plurals. |
| `tasan`, `l_tasan_*` | Use natural whole-hour readings and correct one-hour sentence agreement. |
| `puoli`, `l_puoli_*` | At 03:30, explain/highlight four for German; three for French, Spanish and Brazilian Portuguese. Keep the physical-position explanation between three and four. |
| `vartti`, `l_vartti_*` | Keep the 15-minute fraction concept. Brazilian Portuguese can teach the duration `um quarto de hora` while reading the clock with `quinze`. |
| `ylivaille`, `l_ylivaille_*` | Adapt the actual `nach/vor`, `moins`, `y/menos` or `para` constructions. Do not label these all as literal translations of English “past/to.” |
| `viisi`, `l_viisi_*` | German :25/:35 reference the next half-hour in the proposed style. Ensure captions and explanation match. Other languages retain their selected whole-hour constructions. |
| `minuutit`, `l_minuutit_*` | The current quiz is exactly 03:07. Use an exact reading, never rounded or approximate wording. Explain the direct reading if previous lessons used relative forms. |
| `vrk`, `l_vrk_*` | Distinguish 03:00 from 15:00, explain the two revolutions, and handle noon/midnight explicitly. Check the current 19:00 quiz in context. |

Draft short explanations for the 03:30 scene, illustrating the proposed register:

| Language | Draft explanation |
| --- | --- |
| German | Der kurze Zeiger steht zwischen 3 und 4. Es ist halb vier. Bis vier fehlt noch eine halbe Stunde. |
| French | La petite aiguille est à mi-chemin entre 3 et 4. Il est trois heures et demie. |
| Spanish | La aguja corta está a mitad de camino entre el 3 y el 4. Son las tres y media. |
| Brazilian Portuguese | O ponteiro pequeno está no meio do caminho entre o 3 e o 4. São três e meia. |

These drafts demonstrate the hour-reference difference. They are not a replacement for reviewing all pages, hints and answer choices together. Preserve the existing numeric targets, answer identities and saved lesson IDs. A valid alternative wording of the same time must never serve as an incorrect distractor.

## 10. Verified implementation implications

Read-only source inspection used branch `codex/localization-settings`, HEAD `51415f1b278123ca8aa53f77704f8b75487eacec`, plus the existing uncommitted working-tree changes. This is not a claim that HEAD alone contains the inspected state. Existing work was preserved. Workspace files were scanned successfully for secrets before reading.

| Inspected source | Finding and scoped future work |
| --- | --- |
| `app/src/main/java/fi/clocklings/domain/TimeLanguage.kt` | Already separates analog words, numerical words, display, day parts and contextual words. Add language-specific implementations using the existing approach. Shared arithmetic does not imply shared grammar. |
| `app/src/main/java/fi/clocklings/domain/AppLanguage.kt` | Only `en`, `fi`, `sv` exist. `fromTag` currently discards the region before matching. Adding an enum entry tagged `pt-BR` alone would not work with that comparison. Preserve the requested Brazilian identity and specify device-language fallback behavior. |
| `app/src/main/res/xml/locales_config.xml` | Only the existing three locales are advertised. Expose new locales when their complete implementation is ready. |
| `app/src/main/java/fi/clocklings/ui/Texts.kt` | `spokenTime` wraps generated phrases using resource sentence templates. Spanish and Portuguese need construction-aware agreement; one fixed translated prefix cannot handle every form. Prefer a small explicit language-specific solution. |
| `app/src/main/java/fi/clocklings/ui/lesson/LessonCatalog.kt` | `all(language)` currently selects the current-hour half lesson only for English and next-hour references for every other language. Extend this explicitly: German joins Finnish/Swedish; French/Spanish/Brazilian Portuguese join English for the :30 reference. |
| `app/src/main/java/fi/clocklings/audio/Speech.kt` | Offline selection filters by language and prefers an exact locale, but can fall back across regions. Under the proposed Brazilian voice policy, `pt-PT` must not silently satisfy `pt-BR`. Preserve optional offline speech and silent learning. |
| `tools/check-localization.ps1` | The existing checker is configured for the three current resource sets. Include the new resource sets and appropriate plural/placeholder checks; do not treat English fallback as a completed translation. |
| `app/src/test/java/fi/clocklings/domain/TimeLanguageTest.kt` | Already checks complete-day coverage, analog/day-context separation and distinct answer choices. Extend with independently reviewed expected phrases and language-specific boundaries. Coverage alone cannot prove grammar. |

No dependency upgrade, toolchain change, navigation redesign, migration or generic grammar framework is needed merely to prepare this language expansion. Existing English, Finnish and Swedish behavior should remain unchanged.

All text-resource files belong in the later translation inventory, including companion and appearance resources, arrays, plurals, accessibility descriptions and error messages. The historical counts in `language-review.md` are not a freshly verified count of this working tree.

## 11. Implementation and verification plan

1. Use the confirmed Spain Spanish (`es-ES`) baseline and confirm the proposed German/French editorial scope. Record decisions without implying that other regional expressions are wrong.
2. Review the glossary and candidate fixtures, especially exact-minute readings and full sentence agreement. Keep independently authored expectations separate from formatter output.
3. Implement one language at a time: wording, complete resources, lesson references, locale handling and speech. Finishing a language internally does not authorize a partial public release.
4. Run focused domain and resource checks, then the project's relevant JVM/build checks. Preserve existing language behavior. Do not infer linguistic correctness from a successful build.
5. Validate rendered flows and actual speech separately. Record exactly which configurations and voices were checked.

Required focused language cases:

- Every minute in the 1,440-minute day; both 12-hour and 24-hour modes; hour wraparound; distinct answer options and round-trip numeric meaning where a separately authored oracle is available.
- :00, :01, :14/:15/:16, :20/:21, :24/:25/:26, :29/:30/:31, :34/:35/:36, :39/:40/:41, :44/:45/:46 and :59, especially across 11/12/1 and 23/0/1.
- The 720 analog readings must not infer morning, evening, noon or midnight. Contextual readings must distinguish the two halves of the day.
- German `ein`/`eins`; French feminine minute compounds and `demi`/`demie`; Spanish `es`/`son`, `la`/`las` and hour/minute agreement; Portuguese `é`/`são`, `falta`/`faltam`, `uma`/`um`, `duas`/`dois`, and the target after `para`.
- 21:21 and 22:22 for hour/minute agreement; exact 00:00/12:00 and adjacent minutes for contextual wording. Check all day-part transitions without assuming every language uses the existing English boundaries or greetings naturally.
- All nine lesson quizzes and their explanations, including highlights at 03:30, the 03:25/03:35 demonstrations, and 03:00 versus 15:00. Compare answers by meaning as well as visible string uniqueness.
- Locale selection, device-default resolution, restart/recreation and language switching during an activity; language resources, generated wording and speech must agree. Test `pt-BR`, `pt-PT`, bare `pt` and unsupported device locales according to the chosen fallback policy.

Rendered checks must include normal sizing, **320 dp width**, **200% font scale**, and **320 dp with 200% font scale** for every new language. Exercise long answers, lesson pages, dialogs, hints, errors and accessibility labels. Use an explicitly identified dedicated emulator. Do not install or test on a physical device or unrelated emulator without separate authorization.

Listen to the actual eligible offline voices: text tests do not establish pronunciation, liaison, stress, pacing or correct number reading. Check TalkBack output and hidden-answer states separately from visible text. Missing voice data must leave learning usable without speech. `lc` and `sc` remain owner-run.

A qualified native-language review of children's teaching copy would provide additional assurance, particularly for regional naturalness and speech. It is recommended, not asserted to have happened or made a paid prerequisite for starting. Without that review, describe the release evidence accurately as source-supported and tested; do not certify flawless native usage.

## 12. Remaining choices, source limits and completed work

Resolved on 4 November 2026: the owner selected Spanish as used in Spain (`es-ES`). The Spanish region is no longer an open decision.

### Choices before dependent implementation

| Item | Recommendation / remaining decision |
| --- | --- |
| Exact-minute style | Adopt the proposed direct numerical readings outside five-minute points; explicitly explain equivalent readings where needed. Confirm this editorial behavior before freezing fixtures. |
| German/French regional scope | Use the proposed Germany/France-oriented baseline with one content set each. |
| Portuguese fallback | Keep manual selection explicitly Brazilian. Decide whether a device set to bare `pt` or `pt-PT` automatically receives Brazilian content or the existing unsupported-language fallback. Do not confuse that content policy with permission to use a European voice. |
| Zero-hour narration and day periods | Review complete sentences and boundaries in each language before implementation fixtures are treated as final. Numeric display alone does not settle spoken wording. |

### Source limitations

- Goethe's project-time worksheet and IFSul's course booklet exceeded the browser tool's document-size limit. Their cited rules are based on visible indexed excerpts, not a complete PDF inspection. Other German material was opened successfully.
- The Brazilian Senate page timed out on direct retrieval. Its indexed text supplied the cited noon/midnight guidance, cross-checked against the accessible Goiânia material.
- Teaching pages sometimes overstate a convention. In particular, Mango's recommendation to say fifteen in Brazilian clock readings is used as a style recommendation here, not proof that the phrase `um quarto` is impossible. Its unrelated examples and English translations are not adopted wholesale.
- Sources attest rules and selected usages. They do not certify every composed fixture, every regional preference, the final UI translation, or the pronunciation of an installed Android voice.

### Completed in this task

- Inspected the relevant current source and existing localization documentation without changing application code.
- Researched the four languages and recorded a glossary, clock-language rules, example fixtures, regional choices, lesson adaptations and scoped validation plan.
- Produced this single Markdown deliverable. No resource translations or new language formatters were implemented.
- No Gradle build, unit/instrumentation test, emulator session, listening test, native-speaker review, `lc` or `sc` was run for this research task.
