# Clocklings language review

Updated implementation review, 25 October 2026, retaining the independent WP01 contract seed and baseline inventory. English, Finnish and one shared Swedish content set are implemented; Spanish/French stay unexposed until their later implementation/review. This record distinguishes source evidence, editorial choices and executed checks. It does not establish native-speaker review, actual voice pronunciation or a TalkBack audit. Current build/device evidence and its limits are recorded in [STATUS: Localization and Settings — 25 October 2026](../implementation/STATUS.md#localization-and-settings--25-october-2026).

## Inventory and ownership

Read-only baseline: branch `codex/localization-settings`; only the owner's untracked implementation plan preceded this work. Toolchain declarations: AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Navigation 3 1.2.0, DataStore 1.3.0, Gradle 9.8.2, Java 17, minSdk 26 and compile/targetSdk 37. No toolchain change is part of WP01. The AGP explanatory comment in the version catalog is stale relative to its actual version declaration.

| Baseline file | Strings | Plurals | Arrays | Review group |
| --- | ---: | ---: | ---: | --- |
| `values/strings.xml` | 286 | 8 | 1 (7 praise entries) | Navigation, Home, lessons, practice, Together, hints, accessibility, result/challenge, human wardrobe |
| `values/personalization.xml` | 41 | 0 | 0 | Six companions, none, clock appearances/customization and selection states |
| `values/companion_items.xml` | 80 | 0 | 0 | Five non-human companions' 16 items each; match artwork, not IDs |
| Total | 407 | 8 | 1 | 416 resource keys; 415 require each translation |

The current total is **438 base resource keys and 437 translated keys in each of Finnish and Swedish**: the baseline above plus 25 Settings keys and two contextual noon/midnight narration keys, minus five obsolete Home keys removed in the 25 October lint followup (`mode_label`, `sound_on`, `sound_off`, `reset`, `reset_confirm`). The sole allowed non-translatable resource is `app_name` = `Clocklings`. Persistence IDs, routes, companion/user names and lesson IDs are not translated. Color/theme XML and the `values-v31` theme contain no text resources. Every baseline text key appears in the appendix, with call-site context, argument signatures and plural branches. This is an inventory, not a duplicate translation catalog.

Current language-sensitive sources and review obligations:

| Source/output | Meaning and review obligation |
| --- | --- |
| `domain/TimeLanguage.kt`, `EnglishTime.kt`, `FinnishTime.kt`, `SwedishTime.kt` | The integrated language service selects analog phrases, numbers, digital display, numeric narration, contextual day labels/adverbs and greeting categories. Pure arithmetic stays separate from wording. |
| `ui/locale/AppLocale.kt` | The integrated provider resolves the effective language and supplies matching resources and `LocalAppLanguage`; Settings chooses the explicit language or device default. |
| `ui/Texts.kt` | Resource/Time adapters, full spoken sentence, numeric/clock labels, inflected hints, complete praise + answer. Resources and formatter must use the same effective language. |
| `ui/lesson/LessonCatalog.kt` | Nine stable IDs and numeric quiz targets below. Number bubbles/ticks stay numeric; :30 reference highlight is 3 for English and 4 for Finnish/Swedish at 3:30. The physical-position page retains both numbers. |
| `domain/Hints.kt` | `nearHalf` covers :21–:39. Its copy must say near the halfway point, not claim exactly :30. Geometry and accepted numeric answers stay unchanged. |
| `ui/clock/` and `audio/Speech.kt` | Canvas labels receive localized text; accessibility/narration cannot reveal Together's hidden answer. Installed offline voice metadata does not prove pronunciation. |

## Selected glossary and conventions

| Concept | English | Finnish | Swedish |
| --- | --- | --- | --- |
| Settings / language | Settings / Language | Asetukset / Kieli | Inställningar / Språk |
| Follow device | Device language | Laitteen kieli | Enhetens språk |
| Hour / minute hand | hour hand / minute hand | tuntiviisari / minuuttiviisari | timvisare / minutvisare |
| Face / clockwise | clock face / clockwise | kellotaulu / myötäpäivään | urtavla / medurs |
| Half-hour duration | half an hour | puoli tuntia | en halvtimme |
| Quarter-hour duration | a quarter of an hour | vartti | en kvart |
| Sound / Back / Cancel | Sound effects / Back / Cancel | Äänitehosteet / Takaisin / Peruuta | Ljudeffekter / Tillbaka / Avbryt |

English keeps elapsed-hour `past` before :30, `half past` at :30 and next-hour `to` after it, with quarter specials. Finnish retains its partitive five-minute seed (`viittä`, `kymmentä`, `varttia`, `kahtakymmentä`) and ordinary `yksi minuutti` / `kaksi minuuttia`. Finnish and Swedish name the next hour at half past; :21–:29 count toward half, :31–:39 away from half. Swedish uses `en minut` but hour `ett`; compounds `tjugoett`, not a context-free translation of one. These rules are output style, not a claim that accepted alternatives are wrong distractors.

Numeric display is deliberately application-specific: English 3:07 / 03:07, Finnish 3.07 / 3.07, Swedish 3.07 / 03.07 (12/24-hour modes respectively). Midnight in 24-hour mode is EN 00:00, FI 0.00, SV 00.00. Keep minute padding in all three. FI/EN preserve existing style; SV adopts the plan's padded 24-hour style. This does not claim that padded Swedish hours are required in prose.

Numeric narration is distinct from analog phrases. EN retains `fifteen thirty`, `three oh seven`, `zero zero zero`; FI retains `viisitoista kolmekymmentä`, `kolme nolla seitsemän`, `nolla nolla nolla`; SV implements the selected editorial readings `femton trettio`, `tre noll sju`, `noll noll noll`. Leading-zero minutes use `noll`, not English `oh`. FI/SV triple zero is an explicit retained/chosen digit reading, not a claim of ideal conversational midnight speech. The explicit contextual sentence below is selected separately at exact midnight/noon in 24-hour narration.

| Context | English | Finnish | Swedish |
| --- | --- | --- | --- |
| Analog-only 12:00 phrase | twelve o'clock | tasan kaksitoista | tolv |
| 00:00 standalone context | midnight | keskiyö | midnatt |
| 12:00 standalone context | noon | keskipäivä | middag (context label only; explicit phrase preferred in narration) |
| 00:00 complete sentence | It is midnight. | On keskiyö. | Det är midnatt. |
| 12:00 complete sentence | It is noon. | On keskipäivä. | Klockan är tolv på dagen. |

Avoid mechanically wrapping every contextual label in `Kello on %s` / `Klockan är %s`. In ordinary FI sentences, `Kello on puoli neljä iltapäivällä.` is appropriate, whereas `puoli neljältä` answers when something happens and must not replace the clock reading. For `hint_hands_right`, quote the centre label or provide a complete sentence rather than inserting a nominative day noun in an inflected slot. Swedish noon `tolv på dagen` explicitly distinguishes it from meal `middag`; this is a narrow editorial clarification of the plan's clarity review, not a regional split.

## Source checks and limits

Sources accessed 25 October 2026:

- [Kielitoimiston ohjepankki: clock variants](https://kielitoimistonohjepankki.fi/ohje/ajanilmaukset-kellonajat-viisi-vaille-tai-vailla-viitta-vaille-tai-vailla/) supports both nominative and partitive minute amounts with yli/vaille and alternative hour cases. Retaining `viittä yli` is justified; alternate accepted variants must not be false answers.
- [Kielitoimiston ohjepankki: notation and inflection](https://kielitoimistonohjepankki.fi/ohje/ajanilmaukset-kellonajat-merkinta-ja-taivutus/) supports Finnish dot notation and context-dependent forms. This is evidence for reviewing complete sentences, not proof of every generated exact-minute expression.
- [SPSM, Lärarhandledning till Klockan 2, printed page 6](https://webbutiken.spsm.se/globalassets/egenproduktion/filer/lararhandledning-till-klockan-2.pdf) directly supports Swedish :25/:35 half-hour-relative forms and accepts whole-hour alternatives. It does not attest all uncommon :21/:29/:31/:39 sentences.
- [University of Gothenburg NCM, Eva Norén, slide 14](https://ncm.gu.se/media/nywebb/matematikutvecklare/forelasare/eva_n061201ppt.pdf) also contrasts Swedish half-relative five-minute forms with English elapsed-hour expressions. No curriculum requirement is imported.
- [Isof Frågelådan: midnight notation](https://frageladan.isof.se/faqs/25134) permits different midnight representations by context, including padded digital forms. It supports the numeric choice, not the complete narration grammar.
- [Svensk ordbok 2009: middag](https://spraakbanken.gu.se/resurser/data/karp/so-2009-webbversion/middag.html) records midday and meal senses; [midnatt](https://spraakbanken.gu.se/resurser/data/karp/so-2009-webbversion/midnatt.html) identifies the day boundary. These are authoritative archived dictionary entries hosted by Gothenburg, not a claim of access to a newer dictionary edition.

Uncommon Swedish `nio minuter i halv fyra`, `en minut i halv fyra`, `en minut över halv fyra`, `nio minuter över halv fyra` are implemented compositional editorial choices and remain explicit review targets. The time arithmetic is independently checkable; the located authoritative sources do not directly validate their naturalness for children. Do not label that group “source checked” merely because :25/:35 is attested. Full UI context, speech pronunciation and remaining linguistic uncertainty must be reported at handoff.

## Independent clock fixtures

Authored from numeric meaning before the new language service. These examples are expected labels without an introductory sentence, not snapshots generated from production code. Swedish uncommon half-relative rows carry the source limit above. Preserve exact minute values rather than rounding. Five-minute forms omit minute units; non-five-minute forms include them.

| Time | English | Finnish | Swedish |
| --- | --- | --- | --- |
| 3:00 | three o'clock | tasan kolme | tre |
| 3:01 | one minute past three | yksi minuutti yli kolme | en minut över tre |
| 3:02 | two minutes past three | kaksi minuuttia yli kolme | två minuter över tre |
| 3:05 | five past three | viittä yli kolme | fem över tre |
| 3:10 | ten past three | kymmentä yli kolme | tio över tre |
| 3:14 | fourteen minutes past three | neljätoista minuuttia yli kolme | fjorton minuter över tre |
| 3:15 | quarter past three | varttia yli kolme | kvart över tre |
| 3:16 | sixteen minutes past three | kuusitoista minuuttia yli kolme | sexton minuter över tre |
| 3:19 | nineteen minutes past three | yhdeksäntoista minuuttia yli kolme | nitton minuter över tre |
| 3:20 | twenty past three | kahtakymmentä yli kolme | tjugo över tre |
| 3:21 | twenty-one minutes past three | yhdeksän minuuttia vaille puoli neljä | nio minuter i halv fyra |
| 3:24 | twenty-four minutes past three | kuusi minuuttia vaille puoli neljä | sex minuter i halv fyra |
| 3:25 | twenty-five past three | viittä vaille puoli neljä | fem i halv fyra |
| 3:26 | twenty-six minutes past three | neljä minuuttia vaille puoli neljä | fyra minuter i halv fyra |
| 3:29 | twenty-nine minutes past three | yksi minuutti vaille puoli neljä | en minut i halv fyra |
| 3:30 | half past three | puoli neljä | halv fyra |
| 3:31 | twenty-nine minutes to four | yksi minuutti yli puoli neljä | en minut över halv fyra |
| 3:34 | twenty-six minutes to four | neljä minuuttia yli puoli neljä | fyra minuter över halv fyra |
| 3:35 | twenty-five to four | viittä yli puoli neljä | fem över halv fyra |
| 3:36 | twenty-four minutes to four | kuusi minuuttia yli puoli neljä | sex minuter över halv fyra |
| 3:39 | twenty-one minutes to four | yhdeksän minuuttia yli puoli neljä | nio minuter över halv fyra |
| 3:40 | twenty to four | kahtakymmentä vaille neljä | tjugo i fyra |
| 3:41 | nineteen minutes to four | yhdeksäntoista minuuttia vaille neljä | nitton minuter i fyra |
| 3:44 | sixteen minutes to four | kuusitoista minuuttia vaille neljä | sexton minuter i fyra |
| 3:45 | quarter to four | varttia vaille neljä | kvart i fyra |
| 3:46 | fourteen minutes to four | neljätoista minuuttia vaille neljä | fjorton minuter i fyra |
| 3:50 | ten to four | kymmentä vaille neljä | tio i fyra |
| 3:55 | five to four | viittä vaille neljä | fem i fyra |
| 3:58 | two minutes to four | kaksi minuuttia vaille neljä | två minuter i fyra |
| 3:59 | one minute to four | yksi minuutti vaille neljä | en minut i fyra |
| 11:30 | half past eleven | puoli kaksitoista | halv tolv |
| 12:30 | half past twelve | puoli yksi | halv ett |
| 12:45 | quarter to one | varttia vaille yksi | kvart i ett |
| 23:59 (analog) | one minute to twelve | yksi minuutti vaille kaksitoista | en minut i tolv |

For 03:30 and 15:30 the same hands/analog words are intentional. In 24-hour mode use distinct numbers and context (night versus afternoon); 23:59 → 00:00 wraps to the next day without inventing a date feature. Day/greeting fixtures include the minute before, at and after 00:00, 05:00, 06:00, 10:00, 12:00, 18:00, 22:00 and 24:00. Sun is true from 06:00 through 17:59 only. FI day labels split aamu/aamupäivä at 10:00; EN retains morning; SV implements the selected morgon/förmiddag split at 10:00. Exactly midnight/noon override ordinary labels. These categories are app teaching choices, not claims about sunrise or universal cultural day boundaries.

Concrete expected `dayPart.name` fixtures, written independently from the new service. Each cell is the value one minute before / at / one minute after the stated boundary; 24:00 is normalized to 00:00. Swedish worker confirmed these selected ranges before integration.

| Boundary | English names | Finnish names | Swedish names | Sun before/at/after |
| --- | --- | --- | --- | --- |
| 00:00 | night / midnight / night | yö / keskiyö / yö | natt / midnatt / natt | false / false / false |
| 05:00 | night / morning / morning | yö / aamu / aamu | natt / morgon / morgon | false / false / false |
| 06:00 | morning / morning / morning | aamu / aamu / aamu | morgon / morgon / morgon | false / true / true |
| 10:00 | morning / morning / morning | aamu / aamupäivä / aamupäivä | morgon / förmiddag / förmiddag | true / true / true |
| 12:00 | morning / noon / afternoon | aamupäivä / keskipäivä / iltapäivä | förmiddag / middag / eftermiddag | true / true / true |
| 18:00 | afternoon / evening / evening | iltapäivä / ilta / ilta | eftermiddag / kväll / kväll | true / false / false |
| 22:00 | evening / night / night | ilta / yö / yö | kväll / natt / natt | false / false / false |
| 24:00 | night / midnight / night | yö / keskiyö / yö | natt / midnatt / natt | false / false / false |

`greeting` expectations are language-neutral category IDs with different localized greeting copy. Before/at/after each boundary: 00:00 NIGHT/NIGHT/NIGHT; 05:00 NIGHT/MORNING/MORNING; 06:00 MORNING/MORNING/MORNING; 10:00 MORNING/DAY/DAY; 12:00 DAY/DAY/DAY; 18:00 DAY/EVENING/EVENING; 22:00 EVENING/NIGHT/NIGHT; 24:00 NIGHT/NIGHT/NIGHT. Thus `noon` and `midnight` day labels do not create new greeting enum cases.

## Independent lesson quiz answers

Each first authored option (`c1`) is currently marked correct before shuffle. Preserve semantic correctness rather than relying on that implementation order. Distinct numeric time options must remain distinct after translation; valid alternate readings of the correct time must not be distractors.

| Stable lesson ID | Numeric target | Intended correct meaning | English / Finnish / Swedish correct label |
| --- | --- | --- | --- |
| `viisarit` | 240 = 04:00 | identify hour hand, not read time | The short pink hand / Lyhyt pinkki viisari / Den korta rosa visaren |
| `kierros` | 180 = 03:00 | duration of a complete minute-hand revolution | One hour / Yksi tunti / En timme |
| `tasan` | 540 = 09:00 | whole hour | Nine o'clock / Tasan yhdeksän / Nio |
| `puoli` | 330 = 05:30 | half hour; FI/SV next hour is six | Half past five / Puoli kuusi / Halv sex |
| `vartti` | 405 = 06:45 | quarter to next hour seven | Quarter to seven / Varttia vaille seitsemän / Kvart i sju |
| `ylivaille` | 250 = 04:10 | ten after four | Ten past four / Kymmentä yli neljä / Tio över fyra |
| `viisi` | 520 = 08:40 | twenty until nine | Twenty to nine / Kahtakymmentä vaille yhdeksän / Tjugo i nio |
| `minuutit` | 187 = 03:07 | exactly seven minutes after three | Seven minutes past three / Seitsemän minuuttia yli kolme / Sju minuter över tre |
| `vrk` | 1140 = 19:00 | seven in evening, not morning | Seven in the evening: 19:00 / Seitsemän illalla: 19.00 / Sju på kvällen: 19.00 |

Demo consistency targets: puoli 210 = 3:30, highlight reference 3 EN / 4 FI/SV; subsequent 450 = 7:30 refers to 7 EN / 8 FI/SV. Five-minute demo 205 = 3:25 and 215 = 3:35 both name next hour four in FI/SV. `vrk` 180 versus 900 is 03:00/15:00; Finnish keeps 3.00/15.00, Swedish 03.00/15.00. Demo narration, highlights, translated explanation and final answer must agree on these literal numbers.

## Review state and checks

Historical WP01 checkpoint: the baseline inventory and independent fixtures preceded FI/SV resource authoring. The expected 830 missing-key diagnostics below describe that historical checkpoint; current resource completeness passes.

Run `pwsh -NoProfile -File tools/check-localization.ps1` for resource parity and `-SelfTest` for isolated negative fixtures. The checker reads text in every XML file in `values`, `values-fi`, `values-sv`; it validates duplicates, types, missing/extra keys, plural branches including `other`, array lengths, positional argument conversion and multiplicity. Reordering arguments is allowed. New non-translatable keys require an explicit reviewed exception. No placeholder exceptions currently exist. It is not a grammar, artwork, Kotlin-literal or Android rendering checker, and does not infer correctness from fallback resolution. Configuration-qualified text, if introduced later, needs its own inventory/parity review; current extra qualifiers contain themes only.

WP01 actual checks: valid isolated fixture passed; all eight mutations were detected (missing key, argument type, repeated argument count, type mismatch, missing `other`, array length, duplicate key, and unreviewed translation exclusion). Production baseline returned 830 missing-key issues, precisely 415 per language. No Gradle/emulator/physical-device check was started by WP01; parent runs integration validation. `lc` and `sc` remain owner-run. Later rendered checks must include 320 dp, 200% font and their combination, localized semantic trees, hidden-answer states and actual narration limits.

WP01 review followup: malformed additional formatter tokens also fail, including a valid `%1$s` followed by invalid `%2$q`. Self-test now has nine failing mutations plus the valid fixture; all ten PASS messages were observed in both PowerShell 7 and Windows PowerShell 5.1. Concrete day/greeting fixtures appear above.

WP05 authoring/source-check update, 25 October 2026: all four text XML groups now exist in FI and SV. The 27 added keys below bring the total to 443 base keys and 442 required translations per language. Direct resource parity passes; saved evidence: `reports/localization/resource-completeness.log` (local-only, ignored by Git). All nine authored correct answers match the independent table in every language and all authored option sets are visibly distinct. Numeric quiz targets remain unchanged. All nine lessons were reviewed against page/demo numeric targets; FI/SV half-hour explanations and :25/:35 labels refer to the next hour; past/to lessons use examples rather than claiming every minute of a side uses that preposition. Hint sentence fragments were read with their actual wrappers; the correct-hand hint quotes the centre label context without requiring inflection of the inserted day noun. Near-half copy now covers the actual :21–:39 range in all languages. Item labels were compared to the domain IDs and the five `*Items.kt` draw-function mappings, including coin pouch/heart display/bone tag distinctions. This is source review, not rendered artwork inspection or native-speaker verification.

Autonyms English/Suomi/Svenska intentionally stay identical across all locales but each has an authored entry; they are not omitted through fallback. Numeric patterns (`digital_24`, `home_star_total`, 12/24 mode labels) intentionally preserve the same formatting syntax. User-entered companion names remain untouched. `app_name` remains the sole non-translatable exception.

FI/SV content groups are authored and source reviewed. AppLocale and TimeLanguage are integrated into resource/time adapters and Settings. Compiled-resource resolution, language switching, layout at 320 dp/200% and their combination, and semantic-tree checks have separate evidence in [the current STATUS result section](../implementation/STATUS.md#localization-and-settings--25-october-2026); this source record does not mark all device runs complete. Actual offline voice pronunciation, native-speaker proof and a full TalkBack audit are not established here. The specific Swedish uncommon exact-minute source limits above remain visible, even when formatter tests pass. No language is marked release ready by this document alone.

Small contextual exceptions to the inventory: `settings_guides_description` now names minute numbers, highlighted clock halves and 24-hour numbers in all three languages, so it does not promise text labels on each half. Finnish `settings_speech_description` is “Puhe alkaa vain, kun napautat Kuuntele.”; removing the button compound avoids the observed word break at 320 dp/200% without changing on-demand behavior. Swedish `settings_reset_body` uses “val av utseende” instead of the long compound “utseendeinställningar”; reset scope is unchanged. The two `speak_*_24` entries each take only the numeric reading `%1$s` and provide their own complete midnight/noon sentence, while ordinary `speak_time_24` retains both clock-phrase and numeric-reading arguments. See STATUS for rerun evidence after these copy adjustments.

### Added resource inventory

| Key | File / state | Arguments | Teaching |
| --- | --- | --- | --- |
| speak_midnight_24 | strings; contextual narration at exact midnight | 1:s | yes |
| speak_noon_24 | strings; contextual narration at exact noon | 1:s | yes |
| settings_title | settings; Home entry / screen title | none | no |
| language_title | settings; language chooser title | none | no |
| language_device | settings; follow-device option | none | no |
| language_english | settings; English autonym | none | no |
| language_finnish | settings; Finnish autonym | none | no |
| language_swedish | settings; Swedish autonym | none | no |
| settings_language_description | settings; language change scope | none | no |
| settings_clock_display | settings; 12/24 selector | none | yes |
| settings_clock_example | settings; dynamic time examples | 1:s,2:s | yes |
| settings_guides_title | settings; shared help toggle | none | yes |
| settings_guides_description | settings; minute numbers, highlighted halves and 24-hour overlays | none | yes |
| settings_sound_title | settings; sound toggle | none | no |
| settings_sound_description | settings; effects scope | none | no |
| settings_speech_title | settings; existing speech capability | none | no |
| settings_speech_available | settings; installed offline voice available | none | no |
| settings_speech_unavailable | settings; offline voice unavailable | none | no |
| settings_speech_description | settings; on-demand Listen behavior | none | no |
| settings_progress_title | settings; learning progress group | none | no |
| settings_reset_title | settings; reset confirmation title | none | no |
| settings_reset_body | settings; exact data reset scope and retained settings | none | no |
| settings_reset_action | settings; destructive confirm action | none | no |
| settings_cancel | settings; cancellation | none | no |
| settings_save_error | settings; failed persisted change | none | no |
| language_save_error | settings; failed language change | none | no |
| settings_retry | settings; retry action | none | no |

## Baseline resource appendix

Argument notation is `position:type`, repeated per occurrence; plural branches and array indices are explicit. “Teaching” marks direct clock/number/hint content; other copy still needs meaning review. Context names are actual referencing Kotlin files (resource-only labels may be resolved through shared adapters).

### companion_items.xml

| Key | Type/branches; arguments | Screen/state call sites | Teaching |
| --- | --- | --- | --- |
| item_cat_neck_scarf | string: none | Texts.kt | no |
| item_cat_head_bow | string: none | Texts.kt | no |
| item_cat_extra_yarn | string: none | Texts.kt | no |
| item_cat_neck_bell_collar | string: none | Texts.kt | no |
| item_cat_head_bobble_hat | string: none | Texts.kt | no |
| item_cat_extra_glasses | string: none | Texts.kt | no |
| item_cat_neck_bow_tie | string: none | Texts.kt | no |
| item_cat_head_sailor_hat | string: none | Texts.kt | no |
| item_cat_extra_fish_backpack | string: none | Texts.kt | no |
| item_cat_neck_ruffle | string: none | Texts.kt | no |
| item_cat_head_flower_crown | string: none | Texts.kt | no |
| item_cat_neck_pearls | string: none | Texts.kt | no |
| item_cat_extra_wand | string: none | Texts.kt | no |
| item_cat_neck_cape | string: none | Texts.kt | no |
| item_cat_head_crown | string: none | Texts.kt | no |
| item_cat_extra_wings | string: none | Texts.kt | no |
| item_dog_neck_bandana | string: none | Texts.kt | no |
| item_dog_head_cap | string: none | Texts.kt | no |
| item_dog_extra_ball | string: none | Texts.kt | no |
| item_dog_neck_bone_collar | string: none | Texts.kt | no |
| item_dog_head_sun_hat | string: none | Texts.kt | no |
| item_dog_extra_frisbee | string: none | Texts.kt | no |
| item_dog_neck_bow_tie | string: none | Texts.kt | no |
| item_dog_head_party_hat | string: none | Texts.kt | no |
| item_dog_extra_boots | string: none | Texts.kt | no |
| item_dog_neck_lei | string: none | Texts.kt | no |
| item_dog_head_aviator | string: none | Texts.kt | no |
| item_dog_extra_backpack | string: none | Texts.kt | no |
| item_dog_neck_medal | string: none | Texts.kt | no |
| item_dog_head_helmet | string: none | Texts.kt | no |
| item_dog_extra_raincoat | string: none | Texts.kt | no |
| item_dog_neck_cape | string: none | Texts.kt | no |
| item_robot_head_antenna | string: none | Texts.kt | no |
| item_robot_neck_bow_tie | string: none | Texts.kt | no |
| item_robot_extra_wrench | string: none | Texts.kt | no |
| item_robot_head_propeller | string: none | Texts.kt | no |
| item_robot_neck_tie | string: none | Texts.kt | no |
| item_robot_extra_heart_eyes | string: none | Texts.kt | no |
| item_robot_head_headphones | string: none | Texts.kt | no |
| item_robot_neck_spring | string: none | Texts.kt | no |
| item_robot_extra_star_eyes | string: none | Texts.kt | no |
| item_robot_head_chef_hat | string: none | Texts.kt | no |
| item_robot_neck_lights | string: none | Texts.kt | no |
| item_robot_extra_pet | string: none | Texts.kt | no |
| item_robot_head_star_antennas | string: none | Texts.kt | no |
| item_robot_neck_medal | string: none | Texts.kt | no |
| item_robot_head_astronaut | string: none | Texts.kt | no |
| item_robot_extra_jetpack | string: none | Texts.kt | no |
| item_dragon_neck_pendant | string: none | Texts.kt | no |
| item_dragon_head_flower_crown | string: none | Texts.kt | no |
| item_dragon_extra_coins | string: none | Texts.kt | no |
| item_dragon_neck_bow | string: none | Texts.kt | no |
| item_dragon_head_wizard_hat | string: none | Texts.kt | no |
| item_dragon_extra_book | string: none | Texts.kt | no |
| item_dragon_neck_scarf | string: none | Texts.kt | no |
| item_dragon_head_tiara | string: none | Texts.kt | no |
| item_dragon_extra_shield | string: none | Texts.kt | no |
| item_dragon_neck_chain | string: none | Texts.kt | no |
| item_dragon_head_knight_helmet | string: none | Texts.kt | no |
| item_dragon_extra_star_cloud | string: none | Texts.kt | no |
| item_dragon_neck_sparkles | string: none | Texts.kt | no |
| item_dragon_neck_cape | string: none | Texts.kt | no |
| item_dragon_head_crown | string: none | Texts.kt | no |
| item_dragon_extra_wings | string: none | Texts.kt | no |
| item_dinosaur_extra_backpack | string: none | Texts.kt | no |
| item_dinosaur_head_flower | string: none | Texts.kt | no |
| item_dinosaur_extra_bucket | string: none | Texts.kt | no |
| item_dinosaur_neck_scarf | string: none | Texts.kt | no |
| item_dinosaur_head_cap | string: none | Texts.kt | no |
| item_dinosaur_extra_balloon | string: none | Texts.kt | no |
| item_dinosaur_neck_bow_tie | string: none | Texts.kt | no |
| item_dinosaur_head_party_hat | string: none | Texts.kt | no |
| item_dinosaur_extra_umbrella | string: none | Texts.kt | no |
| item_dinosaur_neck_lei | string: none | Texts.kt | no |
| item_dinosaur_head_hard_hat | string: none | Texts.kt | no |
| item_dinosaur_extra_kite | string: none | Texts.kt | no |
| item_dinosaur_neck_binoculars | string: none | Texts.kt | no |
| item_dinosaur_neck_medal | string: none | Texts.kt | no |
| item_dinosaur_head_explorer_hat | string: none | Texts.kt | no |
| item_dinosaur_extra_skateboard | string: none | Texts.kt | no |

### personalization.xml

| Key | Type/branches; arguments | Screen/state call sites | Teaching |
| --- | --- | --- | --- |
| appearance_title | string: none | AppearanceScreen.kt | no |
| companions_title | string: none | AppearanceScreen.kt | no |
| clock_styles_title | string: none | AppearanceScreen.kt, HomeScreen.kt | yes |
| appearance_selected | string: none | AppearanceScreen.kt, WardrobeScreen.kt | no |
| companion_choice_hint | string: none | AppearanceScreen.kt | no |
| companion_human | string: none | AppearanceNames.kt | no |
| companion_cat | string: none | AppearanceNames.kt | no |
| companion_dog | string: none | AppearanceNames.kt | no |
| companion_robot | string: none | AppearanceNames.kt | no |
| companion_dragon | string: none | AppearanceNames.kt | no |
| companion_dinosaur | string: none | AppearanceNames.kt | no |
| companion_none | string: none | AppearanceNames.kt | no |
| no_companion_hint | string: none | AppearanceScreen.kt | no |
| companion_color | string: none | WardrobeScreen.kt | no |
| color_original | string: none | AppearanceNames.kt | no |
| color_blue | string: none | AppearanceNames.kt, AppearanceScreen.kt | no |
| color_green | string: none | AppearanceNames.kt, AppearanceScreen.kt | no |
| color_purple | string: none | AppearanceNames.kt, AppearanceScreen.kt | no |
| color_orange | string: none | AppearanceNames.kt | no |
| color_coral | string: none | AppearanceScreen.kt | no |
| clock_choice_hint | string: none | AppearanceScreen.kt | yes |
| theme_classic | string: none | AppearanceNames.kt | no |
| theme_rainbow | string: none | AppearanceNames.kt | no |
| theme_space | string: none | AppearanceNames.kt | no |
| theme_forest | string: none | AppearanceNames.kt | no |
| theme_ocean | string: none | AppearanceNames.kt | no |
| theme_garden | string: none | AppearanceNames.kt | no |
| theme_workshop | string: none | AppearanceNames.kt | no |
| theme_custom | string: none | AppearanceNames.kt | no |
| custom_clock_hint | string: none | AppearanceScreen.kt | yes |
| clock_color | string: none | AppearanceScreen.kt | yes |
| clock_frame | string: none | AppearanceScreen.kt | yes |
| frame_round | string: none | AppearanceScreen.kt | no |
| frame_scalloped | string: none | AppearanceScreen.kt | no |
| frame_blocks | string: none | AppearanceScreen.kt | no |
| clock_numerals | string: none | AppearanceScreen.kt | yes |
| numerals_rounded | string: none | AppearanceScreen.kt | no |
| numerals_plain | string: none | AppearanceScreen.kt | no |
| clock_hands | string: none | AppearanceScreen.kt | yes |
| hands_round | string: none | AppearanceScreen.kt | no |
| hands_pointer | string: none | AppearanceScreen.kt | no |
| home_companion_choices | string: none | HomeScreen.kt | no |

### strings.xml

| Key | Type/branches; arguments | Screen/state call sites | Teaching |
| --- | --- | --- | --- |
| app_name | string: none | HomeScreen.kt, IntroScreen.kt | no |
| back | string: none | AppearanceScreen.kt, LessonScreens.kt, LevelsScreen.kt, Panels.kt, WardrobeScreen.kt | no |
| help_on | string: none | Panels.kt | no |
| help_off | string: none | Panels.kt | no |
| listen | string: none | AskScreen.kt, FreeClockScreen.kt, LessonScreens.kt | no |
| random_time | string: none | AskScreen.kt, FreeClockScreen.kt | no |
| next | string: none | GameScreen.kt, LessonScreens.kt | no |
| done | string: none | GameScreen.kt, LessonScreens.kt | no |
| play | string: none | LevelsScreen.kt | no |
| clock_is | string: none | FreeClockScreen.kt | yes |
| question_read | string: none | AskScreen.kt, ChallengeScreen.kt, GameScreen.kt | yes |
| digital_24 | string: 1:s | Texts.kt | yes |
| speak_time | string: 1:s | Texts.kt | yes |
| speak_time_24 | string: 1:s,2:s | Texts.kt | yes |
| praise | string-array; 0: none; 1: none; 2: none; 3: none; 4: none; 5: none; 6: none | Texts.kt | no |
| stars_count | plurals; one: 1:d; other: 1:d | LevelsScreen.kt | no |
| minutes_count | plurals; one: 1:d; other: 1:d | LessonCatalog.kt | yes |
| clock_description | string: 1:s | ClockFace.kt | yes |
| clock_question_description | string: none | ClockFace.kt | yes |
| clock_add | plurals; one: 1:d; other: 1:d | ClockFace.kt | yes |
| clock_subtract | plurals; one: 1:d; other: 1:d | ClockFace.kt | yes |
| clock_zone_yli | string: none | ClockFace.kt | yes |
| clock_zone_vaille | string: none | ClockFace.kt | yes |
| intro_greeting_morning | string: none | Texts.kt | no |
| intro_greeting_day | string: none | Texts.kt | no |
| intro_greeting_evening | string: none | Texts.kt | no |
| intro_greeting_night | string: none | Texts.kt | no |
| intro_description | string: 1:s,2:s | IntroScreen.kt | no |
| intro_skip | string: none | IntroScreen.kt | no |
| tagline | string: none | HomeScreen.kt | no |
| now_eyebrow | string: none | HomeScreen.kt | no |
| doll_default_name | string: none | HomeScreen.kt | no |
| home_star_total | string: 1:d,2:d | HomeScreen.kt | no |
| home_doll_hint | string: none | HomeScreen.kt | no |
| menu_koulu | string: none | HomeScreen.kt | no |
| menu_koulu_sub | string: none | HomeScreen.kt | no |
| menu_vapaa | string: none | HomeScreen.kt | no |
| menu_vapaa_sub | string: none | HomeScreen.kt | no |
| menu_pelaa | string: none | HomeScreen.kt | no |
| menu_pelaa_sub | string: none | HomeScreen.kt | no |
| menu_kysy | string: none | HomeScreen.kt | no |
| menu_kysy_sub | string: none | HomeScreen.kt | no |
| menu_nukke | string: none | HomeScreen.kt | no |
| mode_label | string: none | HomeScreen.kt | no |
| mode_12 | string: none | HomeScreen.kt | no |
| mode_24 | string: none | HomeScreen.kt | no |
| sound_on | string: none | HomeScreen.kt | no |
| sound_off | string: none | HomeScreen.kt | no |
| reset | string: none | HomeScreen.kt | no |
| reset_confirm | string: none | HomeScreen.kt | no |
| free_title | string: none | FreeClockScreen.kt | no |
| legend_hour | string: none | FreeClockScreen.kt | no |
| legend_minute | string: none | FreeClockScreen.kt | no |
| legend_drag | string: none | FreeClockScreen.kt | no |
| levels_title | string: none | LevelsScreen.kt | no |
| level_eyebrow | string: 1:d | LevelsScreen.kt | no |
| level_example | string: 1:s | LevelsScreen.kt | yes |
| watch_lesson | string: none | LevelsScreen.kt | no |
| level_tasatunnit | string: none | Texts.kt | no |
| level_puolet | string: none | Texts.kt | no |
| level_vartit | string: none | Texts.kt | no |
| level_viisi | string: none | Texts.kt | no |
| level_minuutit | string: none | Texts.kt | no |
| challenge_eyebrow | string: none | LevelsScreen.kt | no |
| challenge_title | string: none | ChallengeScreen.kt, LevelsScreen.kt | no |
| challenge_desc | string: none | ChallengeScreen.kt, LevelsScreen.kt | no |
| record | string: 1:d | LevelsScreen.kt | no |
| question_set | string: none | GameScreen.kt | yes |
| praise_with_time | string: 1:s,2:s | GameScreen.kt | no |
| reveal_read | string: 1:s | GameScreen.kt | yes |
| reveal_set | string: 1:s | GameScreen.kt | yes |
| hint_hands_right | string: 1:s,2:s | Texts.kt | yes |
| hint_sun | string: none | Texts.kt | yes |
| hint_moon | string: none | Texts.kt | yes |
| hint_read_hour | string: 1:s | Texts.kt | yes |
| hint_read_hour_half | string: none | Texts.kt | yes |
| hint_read_minute | string: 1:s | Texts.kt | yes |
| hint_read_minute_also_hour | string: none | Texts.kt | yes |
| hint_set_minute | string: 1:s | Texts.kt | yes |
| hint_set_hour | string: 1:s | Texts.kt | yes |
| hint_set_hour_turn | string: none | Texts.kt | yes |
| hour_pos_exact | string: 1:d | Texts.kt | yes |
| hour_pos_passed | string: 1:d | Texts.kt | yes |
| hour_pos_between | string: 1:d,2:d | Texts.kt | yes |
| hour_pos_almost | string: 1:d | Texts.kt | yes |
| minute_pos_top | string: none | Texts.kt | yes |
| minute_pos_bottom | string: none | Texts.kt | yes |
| minute_pos_quarter_past | string: none | Texts.kt | yes |
| minute_pos_quarter_to | string: none | Texts.kt | yes |
| minute_pos_number | string: 1:d,2:d | Texts.kt | yes |
| minute_pos_ticks | string: 1:d | Texts.kt | yes |
| result_title_3 | string: none | ResultScreen.kt | no |
| result_title_2 | string: none | ResultScreen.kt | no |
| result_title_1 | string: none | ResultScreen.kt | no |
| result_text | plurals; one: 1:s,2:d; other: 1:s,2:d | ResultScreen.kt | no |
| result_previous_best | plurals; one: 1:d; other: 1:d | ResultScreen.kt | no |
| result_new_item | string: none | ResultScreen.kt | no |
| result_new_items | string: none | ResultScreen.kt | no |
| result_play_more | string: none | ResultScreen.kt | no |
| play_again | string: none | ResultScreen.kt | no |
| next_level | string: none | ResultScreen.kt | no |
| dress_doll | string: none | AppearanceScreen.kt, ResultScreen.kt | no |
| result_stars_description | plurals; one: 1:d; other: 1:d | ResultScreen.kt | no |
| challenge_score | string: 1:d | ChallengeScreen.kt | no |
| challenge_intro | string: none | ChallengeScreen.kt | no |
| challenge_best | string: 1:d | ChallengeScreen.kt | no |
| challenge_no_best | string: none | ChallengeScreen.kt | no |
| challenge_go | string: none | ChallengeScreen.kt | no |
| challenge_over | string: none | ChallengeScreen.kt | no |
| challenge_new_best | string: none; New best! / Uusi ennätys! / Nytt rekord! | ChallengeScreen.kt, visible record headline above the score | no |
| challenge_new_record | string: 1:d | ChallengeScreen.kt | no |
| challenge_result | string: 1:d,2:d | ChallengeScreen.kt | no |
| ask_title | string: none | AskScreen.kt | no |
| ask_big_eyebrow | string: 1:d | AskScreen.kt | no |
| ask_big_question | string: none | AskScreen.kt | no |
| ask_big_text | string: none | AskScreen.kt | no |
| ask_go | string: none | AskScreen.kt | no |
| ask_small_eyebrow | string: 1:d | AskScreen.kt | no |
| ask_small_text | string: none | AskScreen.kt | no |
| ask_reveal | string: none | AskScreen.kt | no |
| ask_new | string: none | AskScreen.kt | no |
| doll_title | string: none | WardrobeScreen.kt | no |
| doll_name_label | string: none | WardrobeScreen.kt | no |
| doll_name_placeholder | string: none | WardrobeScreen.kt | no |
| doll_info | plurals; one: 1:d; other: 1:d | WardrobeScreen.kt | no |
| doll_all_open | string: none | WardrobeScreen.kt | no |
| doll_optional_hint | string: none | WardrobeScreen.kt | no |
| doll_optional_hint_partial | string: none | WardrobeScreen.kt | no |
| doll_item_on | string: none | WardrobeScreen.kt | no |
| doll_item_locked | string: 1:d | WardrobeScreen.kt | no |
| slot_hair | string: none | Texts.kt | no |
| slot_dress | string: none | Texts.kt | no |
| slot_shoes | string: none | Texts.kt | no |
| slot_head | string: none | Texts.kt | no |
| slot_neck | string: none | Texts.kt | no |
| slot_extra | string: none | Texts.kt | no |
| item_hair_letit | string: none | Texts.kt | no |
| item_dress_perus | string: none | Texts.kt | no |
| item_shoes_tossut | string: none | Texts.kt | no |
| item_dress_pilkku | string: none | Texts.kt | no |
| item_head_rusetti | string: none | Texts.kt | no |
| item_shoes_punaiset | string: none | Texts.kt | no |
| item_hair_pitkat | string: none | Texts.kt | no |
| item_dress_tahti | string: none | Texts.kt | no |
| item_head_kukat | string: none | Texts.kt | no |
| item_extra_laukku | string: none | Texts.kt | no |
| item_shoes_saappaat | string: none | Texts.kt | no |
| item_hair_kiharat | string: none | Texts.kt | no |
| item_dress_prinsessa | string: none | Texts.kt | no |
| item_head_kruunu | string: none | Texts.kt | no |
| item_extra_siivet | string: none | Texts.kt | no |
| item_shoes_kimalle | string: none | Texts.kt | no |
| item_hair_sateenkaari | string: none | Texts.kt | no |
| item_dress_sateenkaari | string: none | Texts.kt | no |
| koulu_title | string: none | LessonScreens.kt | no |
| koulu_intro | string: none | LessonScreens.kt | no |
| lesson_done | string: none | LessonScreens.kt | no |
| path_current | string: none; screen reader state of the current stop on the Learn and Play paths (en "Up next", fi "Vuorossa", sv "På tur"; added in screenshot review 6, round 3) | PathNode.kt | no |
| lesson_skip | string: none | LessonScreens.kt | no |
| lesson_replay | string: none | LessonScreens.kt | no |
| lesson_prev | string: none | LessonScreens.kt | no |
| lesson_try | string: none | LessonScreens.kt | no |
| lesson_next_lesson | string: none | LessonScreens.kt | no |
| lesson_start_game | string: none | LessonScreens.kt | no |
| lesson_practise | string: none | LessonScreens.kt | no |
| lesson_learned | string: 1:s,2:s | LessonScreens.kt | no |
| say_quarter | string: 1:d | LessonCatalog.kt | yes |
| say_number_minutes | string: 1:d,2:d | LessonCatalog.kt | yes |
| l_viisarit_title | string: none | LessonCatalog.kt | yes |
| l_viisarit_desc | string: none | LessonCatalog.kt | yes |
| l_viisarit_p1a | string: none | LessonCatalog.kt | yes |
| l_viisarit_p1b | string: none | LessonCatalog.kt | yes |
| l_viisarit_p2a | string: none | LessonCatalog.kt | yes |
| l_viisarit_p3a | string: none | LessonCatalog.kt | yes |
| l_viisarit_p3b | string: none | LessonCatalog.kt | yes |
| l_viisarit_q | string: none | LessonCatalog.kt | yes |
| l_viisarit_c1 | string: none | LessonCatalog.kt | yes |
| l_viisarit_c2 | string: none | LessonCatalog.kt | yes |
| l_viisarit_hint | string: none | LessonCatalog.kt | yes |
| l_kierros_title | string: none | LessonCatalog.kt | yes |
| l_kierros_desc | string: none | LessonCatalog.kt | yes |
| l_kierros_p1a | string: none | LessonCatalog.kt | yes |
| l_kierros_p1b | string: none | LessonCatalog.kt | yes |
| l_kierros_say_start | string: none | LessonCatalog.kt | yes |
| l_kierros_say_go | string: none | LessonCatalog.kt | yes |
| l_kierros_say_end | string: none | LessonCatalog.kt | yes |
| l_kierros_p2a | string: none | LessonCatalog.kt | yes |
| l_kierros_p2b | string: none | LessonCatalog.kt | yes |
| l_kierros_say_moved | string: none | LessonCatalog.kt | yes |
| l_kierros_p3a | string: none | LessonCatalog.kt | yes |
| l_kierros_p3b | string: none | LessonCatalog.kt | yes |
| l_kierros_say_hour | string: none | LessonCatalog.kt | yes |
| l_kierros_q | string: none | LessonCatalog.kt | yes |
| l_kierros_c1 | string: none | LessonCatalog.kt | yes |
| l_kierros_c2 | string: none | LessonCatalog.kt | yes |
| l_kierros_c3 | string: none | LessonCatalog.kt | yes |
| l_kierros_hint | string: none | LessonCatalog.kt | yes |
| l_tasan_title | string: none | LessonCatalog.kt | yes |
| l_tasan_desc | string: none | LessonCatalog.kt | yes |
| l_tasan_p1a | string: none | LessonCatalog.kt | yes |
| l_tasan_p2a | string: none | LessonCatalog.kt | yes |
| l_tasan_p2b | string: none | LessonCatalog.kt | yes |
| l_tasan_p3a | string: none | LessonCatalog.kt | yes |
| l_tasan_p3b | string: none | LessonCatalog.kt | yes |
| l_tasan_q | string: none | LessonCatalog.kt | yes |
| l_tasan_c1 | string: none | LessonCatalog.kt | yes |
| l_tasan_c2 | string: none | LessonCatalog.kt | yes |
| l_tasan_c3 | string: none | LessonCatalog.kt | yes |
| l_tasan_hint | string: none | LessonCatalog.kt | yes |
| l_puoli_title | string: none | LessonCatalog.kt | yes |
| l_puoli_desc | string: none | LessonCatalog.kt | yes |
| l_puoli_p1a | string: none | LessonCatalog.kt | yes |
| l_puoli_p1b | string: none | LessonCatalog.kt | yes |
| l_puoli_say_half | string: none | LessonCatalog.kt | yes |
| l_puoli_p2a | string: none | LessonCatalog.kt | yes |
| l_puoli_p2b | string: none | LessonCatalog.kt | yes |
| l_puoli_p3a | string: none | LessonCatalog.kt | yes |
| l_puoli_p3b | string: none | LessonCatalog.kt | yes |
| l_puoli_say_digits | string: none | LessonCatalog.kt | yes |
| l_puoli_p4a | string: none | LessonCatalog.kt | yes |
| l_puoli_p4b | string: none | LessonCatalog.kt | yes |
| l_puoli_q | string: none | LessonCatalog.kt | yes |
| l_puoli_c1 | string: none | LessonCatalog.kt | yes |
| l_puoli_c2 | string: none | LessonCatalog.kt | yes |
| l_puoli_c3 | string: none | LessonCatalog.kt | yes |
| l_puoli_hint | string: none | LessonCatalog.kt | yes |
| l_vartti_title | string: none | LessonCatalog.kt | yes |
| l_vartti_desc | string: none | LessonCatalog.kt | yes |
| l_vartti_p1a | string: none | LessonCatalog.kt | yes |
| l_vartti_p1b | string: none | LessonCatalog.kt | yes |
| l_vartti_say_four | string: none | LessonCatalog.kt | yes |
| l_vartti_p2a | string: none | LessonCatalog.kt | yes |
| l_vartti_p2b | string: none | LessonCatalog.kt | yes |
| l_vartti_p3a | string: none | LessonCatalog.kt | yes |
| l_vartti_p3b | string: none | LessonCatalog.kt | yes |
| l_vartti_q | string: none | LessonCatalog.kt | yes |
| l_vartti_c1 | string: none | LessonCatalog.kt | yes |
| l_vartti_c2 | string: none | LessonCatalog.kt | yes |
| l_vartti_c3 | string: none | LessonCatalog.kt | yes |
| l_vartti_hint | string: none | LessonCatalog.kt | yes |
| l_ylivaille_title | string: none | LessonCatalog.kt | yes |
| l_ylivaille_desc | string: none | LessonCatalog.kt | yes |
| l_ylivaille_p1a | string: none | LessonCatalog.kt | yes |
| l_ylivaille_p1b | string: none | LessonCatalog.kt | yes |
| l_ylivaille_p2a | string: none | LessonCatalog.kt | yes |
| l_ylivaille_p2b | string: none | LessonCatalog.kt | yes |
| l_ylivaille_p3a | string: none | LessonCatalog.kt | yes |
| l_ylivaille_p3b | string: none | LessonCatalog.kt | yes |
| l_ylivaille_q | string: none | LessonCatalog.kt | yes |
| l_ylivaille_c1 | string: none | LessonCatalog.kt | yes |
| l_ylivaille_c2 | string: none | LessonCatalog.kt | yes |
| l_ylivaille_c3 | string: none | LessonCatalog.kt | yes |
| l_ylivaille_hint | string: none | LessonCatalog.kt | yes |
| l_viisi_title | string: none | LessonCatalog.kt | yes |
| l_viisi_desc | string: none | LessonCatalog.kt | yes |
| l_viisi_p1a | string: none | LessonCatalog.kt | yes |
| l_viisi_p1b | string: none | LessonCatalog.kt | yes |
| l_viisi_p2a | string: none | LessonCatalog.kt | yes |
| l_viisi_p2b | string: none | LessonCatalog.kt | yes |
| l_viisi_p3a | string: none | LessonCatalog.kt | yes |
| l_viisi_p3b | string: none | LessonCatalog.kt | yes |
| l_viisi_q | string: none | LessonCatalog.kt | yes |
| l_viisi_c1 | string: none | LessonCatalog.kt | yes |
| l_viisi_c2 | string: none | LessonCatalog.kt | yes |
| l_viisi_c3 | string: none | LessonCatalog.kt | yes |
| l_viisi_hint | string: none | LessonCatalog.kt | yes |
| l_minuutit_title | string: none | LessonCatalog.kt | yes |
| l_minuutit_desc | string: none | LessonCatalog.kt | yes |
| l_minuutit_p1a | string: none | LessonCatalog.kt | yes |
| l_minuutit_p1b | string: none | LessonCatalog.kt | yes |
| l_minuutit_p2a | string: none | LessonCatalog.kt | yes |
| l_minuutit_p2b | string: none | LessonCatalog.kt | yes |
| l_minuutit_q | string: none | LessonCatalog.kt | yes |
| l_minuutit_c1 | string: none | LessonCatalog.kt | yes |
| l_minuutit_c2 | string: none | LessonCatalog.kt | yes |
| l_minuutit_c3 | string: none | LessonCatalog.kt | yes |
| l_minuutit_hint | string: none | LessonCatalog.kt | yes |
| l_vrk_title | string: none | LessonCatalog.kt | yes |
| l_vrk_desc | string: none | LessonCatalog.kt | yes |
| l_vrk_p1a | string: none | LessonCatalog.kt | yes |
| l_vrk_p1b | string: none | LessonCatalog.kt | yes |
| l_vrk_say_first | string: none | LessonCatalog.kt | yes |
| l_vrk_say_second | string: none | LessonCatalog.kt | yes |
| l_vrk_say_day | string: none | LessonCatalog.kt | yes |
| l_vrk_p2a | string: none | LessonCatalog.kt | yes |
| l_vrk_p2b | string: none | LessonCatalog.kt | yes |
| l_vrk_say_night | string: none | LessonCatalog.kt | yes |
| l_vrk_say_sum | string: none | LessonCatalog.kt | yes |
| l_vrk_p3a | string: none | LessonCatalog.kt | yes |
| l_vrk_p3b | string: none | LessonCatalog.kt | yes |
| l_vrk_say_afternoon | string: none | LessonCatalog.kt | yes |
| l_vrk_q | string: none | LessonCatalog.kt | yes |
| l_vrk_c1 | string: none | LessonCatalog.kt | yes |
| l_vrk_c2 | string: none | LessonCatalog.kt | yes |
| l_vrk_c3 | string: none | LessonCatalog.kt | yes |
| l_vrk_hint | string: none | LessonCatalog.kt | yes |
