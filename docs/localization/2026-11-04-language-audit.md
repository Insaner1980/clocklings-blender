# Kaikkien seitsemän kielen tarkistus, 4.11.2026

Tarkistus koskee paikallista Clocklings-sovellusta haarassa `codex/localization-settings`, lähtörevisiona `890d6f8390dd840d97c3a85729b635bc62b4238f`. Käyttäjä valtuutti myös löydettyjen virheiden korjaamisen. Muutoksia ei ole commitoitu tai julkaistu.

## Kattavuus

Tarkistin englannin, suomen, yhteisen ruotsin, saksan, ranskan, Espanjan espanjan ja Brasilian portugalin. Nykyinen kielimäärä varmistettiin `AppLanguage`-määrityksestä ja resurssihakemistoista. Vanhemmassa kolmen kielen suunnitelmassa oleva määrä ei rajannut tätä tarkistusta.

- Kaikki 28 tekstiresurssitiedostoa: `strings.xml`, `settings.xml`, `personalization.xml` ja `companion_items.xml` jokaiselta kieleltä.
- Yhteensä 3 487 tekstiarvoa: englannissa 499, muissa kielissä 498 kussakin. Laskenta sisältää monikkomuotojen ja taulukoiden yksittäiset arvot sekä englannin kääntämättömän sovellusnimen. Resurssiavaimia on yhteensä 3 382.
- Ohjeet, palaute, virheilmoitukset, asetukset, saavutettavuuskuvaukset, hahmot, kaikki asusteiden nimet sekä yhdeksän oppitunnin tekstit ja vastausvaihtoehdot kaikilla kielillä.
- Kaikki seitsemän kellonaikamuodostinta, kokonaiset kellonaikalauseet, vihjeet, vuorokaudenajat, numeerinen puhe sekä opetustekstien suhde oppituntien numeerisiin esimerkkeihin.
- Ajettujen muodostimien 420 analogista ilmausta (03.00–03.59 seitsemällä kielellä) sekä 182 raja-aikatapausta, joista kustakin tarkistettiin vuorokaudenaikaan sidottu ilmaus ja numeerinen lukutapa. Ensimmäisen tulostuksen merkistöhäiriö korjattiin UTF-8-tulostuksella ennen aksentillisten muotojen arviointia.

Arvioinnissa painottuivat oikea merkitys, kielioppi, lapselle ymmärrettävät ohjeet, termien johdonmukaisuus, lempeä palaute, lukumäärät 0/1/useita sekä opetustekstin ja kellon näyttämän ajan vastaavuus. Pelkkä avainten vastaavuus tai testien läpäisy ei ollut kielellinen hyväksyntä.

## Korjaukset

Muutin 19 resurssitekstiä kuudessa kielessä. Englannista ei löytynyt tässä tarkistuksessa varmaa merkitys- tai kielioppivirhettä, joka olisi edellyttänyt muutosta.

| Kieli ja avaimet | Havainto ja tehty korjaus |
| --- | --- |
| FI: `challenge_desc` | Korjasin verbin vaatiman muodon ja täsmensin kohteen: `Kuinka monta ehdit vastata minuutissa?` → `Kuinka moneen kysymykseen ehdit vastata minuutissa?` |
| FI: `challenge_new_record`, `challenge_result` | Arvo 1 tuotti virheellisen muodon `1 kertaa`. Tulos ilmaistaan nyt lukumäärästä riippumattomalla otsikolla `Oikeita vastauksia: %1$d.` |
| SV: `challenge_desc` | Selvensin kysymysten määrän ja minuutin aikarajan: `Hur många frågor hinner du besvara på en minut?` |
| SV: `challenge_new_record`, `challenge_result` | Arvo 1 tuotti muodon `1 gånger`. Uusi rakenne on `Rätta svar: %1$d.` |
| ES: `challenge_best`, `challenge_new_record`, `challenge_result` | Arvo 1 tuotti muodon `1 aciertos`. Ennätys on nyt `Mejor resultado: %1$d`; tuloslauseissa käytetään rakennetta `Aciertos: %1$d.` |
| DE: `l_viisarit_p3b` | Korvasin kömpelön suunnan nimeämisen suoralla lauseella `Die Zeiger bewegen sich im Uhrzeigersinn.` |
| DE: `l_ylivaille_p1a`, `l_ylivaille_p2a`, `l_ylivaille_p3a`, `l_ylivaille_p3b` | Vanha yleissääntö yhdisti `nach`-ilmauksen aina jo ohitettuun tuntiin. Se oli ristiriidassa sovelluksen ilmauksen `fünf nach halb vier` kanssa (03.35). Sidoin täydestä tunnista laskemisen konkreettisiin esimerkkeihin ja lisäsin puoleen tuntiin suhteutettavan poikkeuksen. |
| FR: `item_dog_neck_bone_collar` | `en os` kertoi luumateriaalista. Kuvan luunmuotoista merkkiä kuvaa nyt `en forme d’os`. |
| FR: `item_unicorn_head_horn_rings` | `Anneaux de corne` saattoi tarkoittaa sarvimateriaalista tehtyjä renkaita. `Anneaux pour la corne` kertoo niiden käyttötarkoituksen. |
| ES: `item_dog_neck_bone_collar` | `de hueso` vaihtui muotoa tarkoittavaan ilmaukseen `en forma de hueso`. |
| PT-BR: `item_dog_head_sun_hat`, `item_bunny_head_sun_hat` | `Chapéu de sol` voi Brasiliassa tarkoittaa sateen- tai päivänvarjoa. Kuvien olkihattujen täsmällinen nimi on nyt `Chapéu de palha`. |

Lukumääräkorjaukset käyttävät jo sovelluksessa esiintyvää otsikko–lukumäärä-rakennetta. Ne toimivat myös nollalla ja suuremmilla arvoilla muuttamatta pelin logiikkaa tai resurssien argumentteja. Kellonaikojen laskentaa tai alueellisia kielivalintoja ei muutettu.

Lisäsin yhden oikean vastauksen resurssitulosteita tarkistavan regression `ExpandedLocaleContentTest`-testiin ja päivitin muuttuneen suomenkielisen tulostekstin `ActiveLocaleStateTest`-testissä. Laajensin olemassa olevan `ClockSurfaceStateTest`-asettelutestin myös muutettuihin viisari- ja yli/vaille-oppituntisivuihin.

## Kielikohtaiset johtopäätökset

| Kieli | Tarkistuksen tulos ja tarkkuuden raja |
| --- | --- |
| EN | `past/to`, `half past`, yksikkö/monikko ja vuorokaudenajan liittäminen tarkistettu. Britti- ja amerikanenglannin kirjoitusasut sekoittuvat; kyse on toimituksellisesta yhtenäisyydestä, ei yksittäisten sanojen virheellisyydestä. |
| FI | Rektiovirhe ja lukumäärätaivutus korjattu. Vaille/vailla- ja lukusanan sijamuotovaihtelua ei muutettu virheeksi, koska hyväksyttyjä vaihtoehtoja on useita. |
| SV | Haasteen ohjetta selvennetty ja yksikkö korjattu. Tavalliset puoleen tuntiin suhteutetut viiden minuutin ilmaukset ovat lähteen tukemia. Harvinaisempien minuuttikohtaisten muotojen, kuten `nio minuter i halv fyra`, pedagoginen luontevuus jää arviointirajaksi. |
| DE | Opetuksen ristiriita korjattu. `halb` viittaa seuraavaan tuntiin; 25 ja 35 minuutin ilmaukset tarkistettu. Muut kuin viiden minuutin ajat ilmaistaan nykyisessä toteutuksessa numeerisesti; tätä tyyliä ei muutettu. |
| FR | Kellonaikojen `une heure/heures`, `et quart`, `et demie`, `midi/minuit` ja `moins` tarkistettu. Kahden esineen merkitys korjattu. Välimerkkien edellä käytettävien sitovien välilyöntien johdonmukaisuus on mahdollinen myöhempi typografinen parannus, ei tässä todettu toiminnallinen virhe. |
| ES-ES | `Es la` / `Son las`, y/menos, tuntiin viittaavat lukusanat ja vuorokaudenajat tarkistettu. Kolme lukumäärätekstiä ja luunmuotoisen merkin nimi korjattu. |
| PT-BR | `É` / `São`, `uma/duas`, `meio-dia/meia-noite`, minuuttien yksikkö ja 24 tunnin lukutapa tarkistettu. Kaksi hatunnimeä korjattu. Muutamat tuntiosoittimen jo ohittamaa tuntia kuvaavat lauseet ovat raskaita mutta ymmärrettäviä; niitä ei muutettu epävarman tyylipreferenssin perusteella. |

Suomen ilmauksen `ylimääräiset pienet merkit` yksinkertaistaminen ja englannin kirjoitusasun alueellinen yhtenäistäminen ovat mahdollisia toimituksellisia valintoja. Myös EN/FI/SV-kielten keskiyön numerolukutavan luontevuus kannattaa erottaa sen teknisestä yksiselitteisyydestä. Näitä ei kirjattu todetuiksi kielioppivirheiksi.

## Lähteet ja niiden käyttö

Lähteet tukevat rajattuja kielioppia ja ajanilmausta koskevia päätelmiä. Ne eivät ole koko sovelluksen ulkopuolinen kielentarkastus.

- [British Council: How to tell the time](https://learnenglishkids.britishcouncil.org/how-tell-time?page=0): englannin lastenopetuksen tunti-, puoli-, vartti- ja past/to-ilmaukset.
- [Kotus: Ajanilmaukset](https://kielitoimistonohjepankki.fi/ohje/ajanilmaukset-kellonajat-viisi-vaille-tai-vailla-viitta-vaille-tai-vailla/): suomen hyväksytyt vaille/vailla- ja sijamuotovaihtoehdot.
- [SPSM: Lärarhandledning till Klockan 2, s. 6](https://webbutiken.spsm.se/globalassets/egenproduktion/filer/lararhandledning-till-klockan-2.pdf): ruotsin tavalliset viisi vaille/yli puoli -ilmaukset ja vaihtoehtoiset ilmaisut. Lähde ei varmista jokaista sovelluksen minuuttikohtaista muotoa.
- [Goethe-Institut: Die gesprochene Uhrzeit, s. 8](https://www.goethe.de/resources/files/pdf270/didaktische-materialien-zu-gfdz-v1.pdf): saksan puoleen tuntiin suhteutetut ilmaukset. Tarkistuksessa saatiin hakupalvelun indeksoima sivukatkelma; PDF:n suora avaus ei onnistunut.
- [TV5MONDE: Dire l’heure](https://apprendre.tv5monde.com/fr/aides/grammaire-dire-lheure): ranskan kellonaikarakenteet ja 12/24 tunnin ilmaisutavat.
- [Académie française: demi](https://www.dictionnaire-academie.fr/article/A8D0746): midi/minuit-ilmausten demi-muoto; kyseessä on sanakirjan kahdeksannen painoksen artikkeli.
- [RAE: hora](https://www.rae.es/dpd/hora): espanjan kellonaikojen esittäminen ja vuorokaudenajan täsmennykset.
- [Goiânian opetusviranomainen: Quantas horas são?](https://sme.goiania.go.gov.br/conexaoescola/ensino_fundamental/quantas-horas-sao/): Brasilian portugalin lasten kellonaikaopetuksen vertailuaineisto.
- [Michaelis: chapéu](https://michaelis.uol.com.br/moderno-portugues/busca/portugues-brasileiro/chap%C3%A9u): Brasilian portugalin hatunnimien merkityseron tarkistus.

## Varmennus

| Tarkistus | Tulos |
| --- | --- |
| `tools/check-localization.ps1` korjausten jälkeen | PASS: seitsemän kielen avaimet, resurssityypit, monikkomuodot, taulukot ja argumentit vastaavat toisiaan. |
| Rajattu `:app:testDebugUnitTest` korjausten jälkeen | PASS: 89 testiä, 13 testiluokkaa, 0 epäonnistumista, 0 virhettä, 0 ohitettua. |
| `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:ktlintCheck` | PASS korjausten jälkeen. |
| `LocalizationContentTest` emulaattorissa korjausten jälkeen | Kaikki 7 testimetodia PASS, mukaan lukien 7 × 9 oppitunnin riippumattomat vastausesimerkit. Koko yhdistetty ajo keskeytyi myöhemmin emulaattorin kaatumiseen. |
| `LanguageSettingsTest.settingsLanguageAndResetLayoutsAreReadableInAllLanguages` | PASS kaikilla 7 kielellä jokaisessa alla olevassa neljässä näyttöasetuksessa. |
| `ClockSurfaceStateTest.exactMinuteChoicesAndHalfHourLessonControlsRemainReadableInEveryLanguage` | PASS jatkoajossa: kaikki 7 kieltä jokaisessa neljässä näyttöasetuksessa. Alkuperäinen emulaattorikaatuminen kierrettiin `-gpu host` -valinnalla. |
| `ActiveLocaleStateTest.activeChallengeEndsAtItsOriginalDeadlineAfterLocaleRecreation` | PASS jatkoajossa 360 dp / 100 %: kielenvaihto säilytti alkuperäisen aikarajan, vastausvaihtoehdot ja oppimistiedot sekä näytti päivitetyn suomenkielisen tulostekstin. |
| `git diff --check` | PASS lopullisille lähdekoodimuutoksille. |

Emulaattorin onnistunut asetusten, kielivalinnan, nollauksen ja etusivulle palaamisen testimatriisi:

| Leveys | Fontti | Kielet | Tulos |
| --- | --- | --- | --- |
| 360 dp | 100 % | EN, FI, SV, DE, FR, ES-ES, PT-BR | PASS |
| 320 dp | 100 % | EN, FI, SV, DE, FR, ES-ES, PT-BR | PASS |
| 360 dp | 200 % | EN, FI, SV, DE, FR, ES-ES, PT-BR | PASS |
| 320 dp | 200 % | EN, FI, SV, DE, FR, ES-ES, PT-BR | PASS |

Näytön fyysinen koko oli 1080 × 2400 pikseliä; tiheys 480 tai 540 dpi tuotti vastaavasti 360 tai 320 dp:n leveyden. Lisäksi tarkastin 320 dp / 200 % -kuvista jokaisen seitsemän kielen etusivun sekä englanninkielisen kielivalinnan. Näkyvät kellonaikatekstit säilyivät kokonaisina. Kielilistan ja etusivun alempi sisältö vaatii tässä koossa vierittämistä. Korjattujen asustenimien visuaalista asettelua ei tässä ajossa varmennettu.

JVM-valinta: `fi.clocklings.domain.*TimeTest`, `TimeLanguageTest`, `HintsTest`, `ExpandedLocaleContentTest`, `LessonCatalogTest` sekä `ChallengeInteractionTest`-luokan aloituspainikkeen ja tulosnäkymän asettelutestit. Haasteen tulosnäkymää tarkistetaan kaikilla seitsemällä kielellä myös 320 dp / 200 % -yhdistelmällä. Ajot käyttivät JDK 21.0.12:ta ja `--no-daemon`-valintaa.

Emulaattori on tähän työhön käynnistetty `ClocklingsReview20261030`, `emulator-5586`, API 36. Ensimmäinen ajo ehti ilmoittaa seitsemän sisältötestiä ja yhden kaikkien kielten asetustestin hyväksytyiksi, mutta emulaattori sulkeutui ennen viimeisen asettelutestin tulosta. Korjausten jälkeinen uusinta ohjelmistopohjaisella grafiikalla ja `-read-only`-valinnalla pysähtyi samaan kohtaan: seitsemän sisältötestiä ja asetustesti PASS, `ClockSurfaceStateTest` kesken, sen jälkeinen `ActiveLocaleStateTest` ei käynnistynyt. Windowsin tapahtumaloki vahvisti molemmilla kerroilla `qemu-system-x86_64-headless.exe`-prosessin kaatumisen poikkeuskoodilla `0xc0000005`. Kokonaisia ajoja ei lasketa hyväksytyiksi, eikä emulaattoriprosessin kaatumista kirjata sovelluksen kielivirheeksi. Näyttö oli näissä ajoissa 360 dp leveä ja fontti 100 %.

### Emulaattoriajon jatko 4.11.2026

Käyttäjän pyytämä jatkoajo sai molemmat kesken jääneet testit loppuun. Projektin aiemmassa `STATUS.md`-testiraportissa kuvattu `-gpu host` -kiertotapa toimi myös tässä ajossa. Käytössä oli Android Emulator 37.2.13.0, sama erillinen API 36 -AVD ja käynnistysvalinnat `-port 5586 -no-window -no-audio -no-snapshot -read-only -gpu host -memory 2048`. `host` on Androidin [dokumentoitu grafiikkavaihtoehto](https://developer.android.com/studio/run/emulator-acceleration#command-line-gpu). Onnistuminen tällä asetuksella ei yksin todista aiemman kaatumisen tarkkaa juurisyytä.

Sovelluskoodia, tekstejä tai testien vaatimuksia ei muutettu jatkoajoa varten. Käytin aiemmin rakennettuja, korjaukset sisältäviä debug- ja testipaketteja. Sovelluspaketin SHA-256 on `59ED58F221D0877526E1E4BC8171686F2B2926A24D5FF240992D7CD6DD052D8C` ja testipaketin `4898F1758E2C698740F0F7E024B4584B88D630B40961E6F881FCA956A1394434`.

| Jatkoajo | Kattavuus | Tulos | Testiaika |
| --- | --- | --- | --- |
| Oppitunnit ja vastausvaihtoehdot, 360 dp / 100 % | Kaikki 7 kieltä | PASS | 34,271 s |
| Oppitunnit ja vastausvaihtoehdot, 320 dp / 100 % | Kaikki 7 kieltä | PASS | 32,799 s |
| Oppitunnit ja vastausvaihtoehdot, 360 dp / 200 % | Kaikki 7 kieltä | PASS | 32,535 s |
| Oppitunnit ja vastausvaihtoehdot, 320 dp / 200 % | Kaikki 7 kieltä | PASS | 34,716 s |
| Minuuttihaasteen aikaraja ja kielenvaihto, 360 dp / 100 % | EN → FI | PASS | 67,528 s |

Jokainen ajo päättyi tulokseen `OK (1 test)`: yhteensä viisi hyväksyttyä ajoa ilman epäonnistumisia tai ohituksia. Oppituntitesti tarkistaa jokaisella kielellä kolme tarkkaa kellonaikavastausta ja kahdeksan oppituntitilaa, mukaan lukien korjatut saksan opetustekstit. Tarkistukset kattavat tekstien korkeuden ja rivileveyden, katkaisumerkit, sanojen säilymisen kokonaisina sekä edellinen/seuraava-toimintojen mahtumisen ja saavutettavuuden vierittämällä. Nämä ovat emulaattorissa renderöidyn käyttöliittymän automaattisia tarkistuksia; jatkoajossa ei tehty uutta kuvakaappausten visuaalista tarkastusta.

## Mitä tämä ei todista

Tämä on perusteellinen tekoälyavusteinen tekstien ja toteutuksen tarkistus. Se ei takaa kaikkien ilmausten täydellistä luontevuutta jokaiselle lapselle tai kielialueelle. Natiivipuhujan arviota, lasten käyttäjätestiä, oikeiden puhesynteesien kuuntelua, TalkBack-kuuntelua tai fyysistä laitetestiä ei tehty. Puhuttavaksi muodostettujen tekstien tarkistaminen ei varmista moottorin ääntämystä. `lc`- ja `sc`-tarkistuksia ei ajettu.

Resurssien täydellinen tekstikattavuus ei tarkoita kaikkien ruutujen kaikkien tilojen visuaalista testausta. Emulaattorin tarkat testatut näyttöasetukset ja näkymät on rajattu yllä olevassa varmennusosassa.
