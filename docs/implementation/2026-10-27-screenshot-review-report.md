# Kuvakaappauskorjausten toteutus — 27.10.2026

Toteutus perustuu [hyväksyttyyn suunnitelmaan](../superpowers/plans/2026-10-27-screenshot-review-fixes.md). Työ tehtiin `codex/localization-settings`-haarassa olemassa olevien muutosten päälle. Ei commitia, pushia tai PR:ää. Suunnitelmasta poistetut havainnot jätettiin pois.

## Toteutetut kohdat

| Kohta | Muutos |
| --- | --- |
| A1 | Numerokehän säde 110 ja fontti 39; tuntiviisarin pituus 86. Minuuttiviisarin pituus säilyi 150:nä. Sisemmän 24 tunnin kehän säde 82 ja fontti 12 erottavat pienet luvut suurista numeroista. Numeron hehkun säde 27 säilyi. |
| A2 | Molempien viisarien ympärillä on kellotaulun värinen reunus. Viisarien peittävyys ja piirtojärjestys säilyivät. |
| A3 | Tuntiviisari on teksteissä pinkki kaikilla kolmella kielellä, myös vihjeissä ja vastausvaihtoehdoissa. |
| A4 | Viiden minuutin oppitunti siirtää viisarin kerrottavaan numeroon ennen selitettä. Uudelleentoisto aloittaa ajasta 3.00. |
| B1 | Yhdessä-pelin satunnaisaika ja Anna parille ovat päällekkäiset, täysleveät painikkeet. Jaettu painiketeksti käyttää kokonaisia sanoja säilyttävää komponenttia. |
| B2 | Oppitunnin navigointi on vieritysalueen ulkopuolella ja huomioi järjestelmän alapalkin. Painikkeet ovat yhtä leveät; ahtaassa tilassa ne pinoutuvat. |
| B3 | Kuuntelu ja uudelleentoisto käyttävät samankokoisia kuvakepainikkeita. Myös yhdessä-pelin paljastetun vastauksen kuuntelu päivitettiin. Erillisiä Kuuntele-tekstipainikkeita ei jätetty. |
| B4 | Puhekuplan häntä pysyy kuplan reunassa myös yhden rivin selitteissä. Kumppani ja kupla kohdistuvat yläreunasta. |
| B5 | Otsikkopalkin tausta peittää sen alle vierivän sisällön. Polun pisteet kiertävät tekstien ja kirjapainikkeiden alueen. |
| B6 | Vastauksen digitaalinen aika on loppureunassa. Ahtaassa tilassa suurella fontilla se on sanojen alapuolella; muuten samalla rivillä. |
| B7 | Vaatekaapin ruudukon kaikille nimille mitataan yhteinen fonttikoko. Jos pisin sana ei mahdu vähimmäiskoolla, ruudukko vaihtuu yhteen sarakkeeseen. Tulosnäkymän erillisen vaatelaatan aiempi tekstisovitus säilyi. |
| B8 | Numeroiden ja yksiköiden välissä on sitova välilyönti kaikilla kolmella kielellä. |
| B9 | Etusivun hahmo ja kello ovat lähempänä otsikkoa; pilvet ja alhaalla olevat valinnat säilyivät. Pelin, haasteen ja yhdessä-pelin kello saa suuremman osuuden näkymän korkeudesta. |
| C1 | Opi on jaettu viiteen lukuun, joissa on tason värit ja harjoituslinkit. Oppituntien numerointi ja suorituslogiikka säilyivät. 24 tunnin oppitunti on oma viimeinen luku. `levelOf` on testattu kaikilla yhdeksällä tunnisteella ja tuntemattomalla tunnisteella. |
| C2, 2.3 | Pelaa-näkymän otsikko vastaa etusivua. Tasojen nimet vastaavat saman aiheen oppitunteja englanniksi, suomeksi ja ruotsiksi. |
| C3 | Käytössä oleva apupainike on keltainen ja sen lamppu täytetty. Valintatila ja tilaa kuvaava saavutettavuusteksti säilyivät. |
| C4 | Selitteiden pisteet ja mittayksiköiden numeromuoto yhtenäistettiin. Kirjoitussäännöt lisättiin resurssien kommentteihin. |
| C5 | Minuuttikuplien numerot käyttävät minuuttimerkintöjen fonttiperhettä, painoa ja väriä. Ympyrän säde 18 säilyi. |
| C6 | Anna parille -painikkeessa on puhelimen ojentamista kuvaava kuvake. Etusivun yhdessä-pelillä on kahden ihmisen kuvake. Ohjeet päivitettiin kolmella kielellä. |
| D1 | Lukitut asut näyttävät nykyiset/tarvittavat tähdet. Näkymä kertoo, etteivät tähdet kulu, ja saavutettavuuskuvaus sisältää molemmat määrät. |
| D2 | Etusivun kumppanin vieressä on vaateripustin. Se avaa olemassa olevan kumppaninäkymän. Painiketta ei näytetä ilman kumppania. |
| E1 | Kytkimen pois-tila käyttää vaalealla pinnalla tummaa reunusta ja nuppia. `InkSoft`/valkoinen-kontrasti on 5,34:1; käytöstä poistetuilla kytkimillä on alennettu peittävyys. |
| E2 | Apumerkintöjen kuvaus on jaettu kahteen lauseeseen. Sisäkehän todelliset luvut ovat 13–23 ja 0, joten teksti kuvaa niitä. |
| E3 | Ääneen lukemisen tieto on äänikortin alapuolella. Teksti kertoo saatavuuden ja kaiutinkuvakkeen käytön. Vanhat käyttämättömät resurssit poistettiin. |

## Toteutustarkennukset

- `DemoStep.Move` käynnistää animaation odottamatta sitä. A4:ssä siirron jälkeen odotetaan siksi 600 ms ennen selitettä, minkä jälkeen tulee suunnitelman 500 ms tauko. Ensimmäinen sivu kestää noin 13,6 sekuntia aiemman noin 10 sekunnin sijaan; selite vastaa nyt saavutettua asentoa. Toinen sivu säilyi.
- B7:n yhden sarakkeen varatila estää pitkien nimien katkeamisen suurilla fonteilla. Laitekuvissa löytynyt normaalikoon rajatapaus (“Kukkakaulanauha”) toistettiin epäonnistuvalla testillä ja korjattiin mittaamalla fontti puolen pisteen askelin todellista pikselileveyttä vasten. Testi tarkistaa myös yhteisen koon kunkin ruudukon sisällä.
- B6:n alkuperäinen saman rivin asettelu katkaisi pitkän englanninkielisen kellonajan keskeltä sanaa 320 dp:n leveydellä ja 200 %:n tekstikoolla. Epäonnistuva regressiotesti varmisti havainnon. Tässä ahtaassa tilassa digitaalinen aika siirtyy sanojen alle, mutta säilyy oikeassa reunassa. Viimeinen laitetesti läpäisi kaikki kolme kieltä.
- C1:n uuden pelilinkin ”Aloita pelaaminen!” katkeaminen 320 dp / 200 % -koossa toistettiin olemassa olevassa lukujen navigointitestissä. Linkin teksti vaihdettiin yhteiseen `WordSafeText`-komponenttiin.
- D2 käyttää olemassa olevaa kumppanireittiä; siitä valitaan asu. Uutta navigointireittiä ei lisätty.
- Päätökset luvuista, tasojen yhteisestä ulkoasusta, tähtien merkityksestä ja pinkistä päätoimintoväristä on kirjattu [STATUS.md](STATUS.md)-tiedostoon.

## Varmistus

Lähde- ja JVM-varmistus:

- Viimeinen koko JVM-ajo läpäisi **217 testiä** ilman virheitä tai ohituksia (`chapter-complete.log`), myös C1-linkin viimeisen korjauksen jälkeen. Mukana ovat uudet viisarin ajoituksen, tasokytkentöjen, navigoinnin näkyvyyden, vastausrivien kohdistuksen ja kokonaisten sanojen, lukujen pelilinkkien ja vaatenimien testit.
- Debug-sovellus ja instrumentointitestien APK rakennettiin. `ktlintCheck` läpäisi (`chapter-complete.log`).
- Debug- ja release-vakausvertailut päivitettiin tarkoituksellisten parametrimuutosten jälkeen. Uusissa ja muuttuneissa parametreissa ei ole `UNSTABLE`-merkintöjä; tarkistussääntöjä ei muutettu. Molemmat vakaustarkistukset läpäisivät myös viimeisen tuotantokoodimuutoksen jälkeen (`chapter-complete.log`).
- `tools/check-localization.ps1` läpäisi: avaimet, resurssityypit, monikot, taulukot ja muotoiluargumentit täsmäävät kolmella kielellä.
- Riippumaton koodikatselmointi ei löytänyt vahvistettua korjausta vaativaa havaintoa. Se tehtiin ennen viimeisiä kuvista löytyneitä sanakatkojen tarkennuksia; niiden näyttö perustuu regressiotesteihin ja laitekuviin.
- Omistajan `lc`- ja `sc`-tarkistuksia ei ajettu.

Paikalliset lokit ovat `.superpowers/sdd/2026-10-27-screenshot-review-fixes/`-hakemistossa. Kuvien ja koontikuvien hakemisto on `reports/screenshot-review-20261027/`.

### Laitetestaus

Käytettiin vain tätä työtä varten käynnistettyä AVD:tä `ClocklingsFoundation20261020`, `emulator-5590`, API 37. Näyttö käytti kolmen painikkeen järjestelmänavigointia. Fyysistä laitetta ja toista käynnissä ollutta emulaattoria ei käytetty.

Suunnitelman kahdeksan testiluokan lisäksi ajettiin `LocalizationContentTest` ja `ActiveLocaleStateTest`: **kaikille 49 erilliselle testille on läpäisevä tulos**. Ensimmäinen yhteisajo oli 41/49; kahdeksan epäonnistumista liittyi vanhoihin otsikko-/lukituskuvausodotuksiin, uuden vaateripustimen aiheuttamaan monitulkintaiseen testivalitsimeen sekä kahden kielitestitapauksen aikakatkaisuun ja yhteisen ajon tilaan. Päivitetty `AppFlowTest` läpäisi 6/6 englanniksi asetetulla sovelluskohtaisella kielellä; neljä kieli-/aktiivitilan tapausta läpäisi erillisissä uusinnoissa. Tämä ei ole väite yhdestä puhtaasta 49 testin yhteisajosta. Lokit: `device/required.txt`, `device/appflow-final.txt` ja nimettyjen metodien lokit.

| Näkymä | Kuvien kielet | Viimeinen varsinainen matriisiajo |
| --- | --- | --- |
| 411 dp, 100 % (1080 × 2400 px, 420 dpi) | en, fi, sv | 3/3 läpäisi |
| 320 dp, 100 % (1080 × 2400 px, 540 dpi) | en, fi, sv | 3/3 läpäisi |
| 411 dp, 200 % | en, fi, sv | 3/3 läpäisi |
| 320 dp, 200 % | en, fi, sv | 3/3 läpäisi B6-korjauksen jälkeen |
| 360 × 640 dp, 100 % (1080 × 1920 px, 480 dpi) | en, fi, sv | 3/3 läpäisi |

Peruskoossa ajettiin kolme uutta galleria-/toistotestiä. Muissa soluissa ajettiin kuvagalleria sekä kaksi `ClockSurfaceStateTest`-testiä, jotka tarkistavat vastausten ja oppitunnin ohjainten luettavuuden ja piilotetun vastauksen kielenvaihdossa. Matriisin viimeinen 320 dp / 200 % uusinta käyttää lopullista vastausasettelua. Muiden solujen yhteisajot edeltävät viimeisiä vastaus- ja lukulinkkitekstin tarkennuksia; Opi-polun kuvat uusittiin erikseen viimeisellä sovellusversiolla. Lokit: `device/411-100.txt`, `320-100.txt`, `411-200.txt`, `320-200.txt`, `360-100.txt`.

Todellinen oppitunnin ViewModel saavutti ajan 3.05 ennen ensimmäistä numerokohtaista selitettä. Uudelleentoisto palautti ajan 3.00:aan ja tyhjensi selitteen; seuraava tarkistettu asento oli 3.10. Pelkkää lavastettua kuvakaappaustilaa ei laskettu animaation todisteeksi.

Kellosta tallennettiin molemmat viisarimallit 12 viiden minuutin asennossa sekä 10.50 ja 11.55, lisäksi teräväkärkinen malli kaikissa 60 minuuttiasennossa ja 24 tunnin sisäkehä. Numeropikselien takaisinmittauksessa pienin väli säteeseen 145 oli noin **13,14 kelloyksikköä**, yli vaaditun neljän. Tarkistetuissa kuvissa ei löytynyt terävän minuuttikärjen tai tuntiviisarin rungon peittämää ison numeron pikseliä. Tämä on kuvapohjainen mittaus näistä tiloista, ei yleinen todistus jokaisesta koosta ja teemasta. Sisäkehän viimeinen 82/12-säätö tarkistettiin erillisessä 1/1 läpäisseessä kellogalleriassa (`device/clock-final.txt`).

Kuvat kattavat pyydetyt etusivun, Opi-polun alun/lopun, tasot, Tutki 3.45/10.50, yhdessä-pelin, 12.10-lukukysymyksen, kolme oppituntia, kissan asut ja asetukset. Mukana on myös koiran ja robotin asuja. `screenshots/localization/` sisältää PNG-kuvat ja `contacts/` koontikuvat. Tiedostonimen alku kertoo leveyden, fonttiasteikon ja kielen. Galleria käyttää hallittuja Compose-tiloja; esimerkiksi asetusten valittu kieli voi olla English samalla, kun esitettävät tekstit ovat suomeksi. Varsinainen kielenvaihto testattiin erillisillä toimintatesteillä.

Kuvien tallennuksessa havaittiin kaappausongelma: `UiAutomation.takeScreenshot()` palautti joissakin näkymävaihdoissa edellisen ruudun, ja pitkän yhteisen kieliajon myöhemmissä kuvissa puuttui piirroksia. Kuvat uusittiin erillisissä kieliajoissa. Viimeiset Opi- ja englannin/ruotsin vaatekaappikuvat tallennettiin suoraan emulaattorin Compose-juuren renderöinnistä, ilman järjestelmäpalkkeja: **25/25 kohdennettua kuvausajoa läpäisi** kaikissa viidessä näyttöasetuksessa (`device/*-learn-final.txt`, `device/*-wardrobe-final.txt`). Viimeisen kuvaustestiversion rakennus ja Android-testien muotoilutarkistus läpäisivät (`capture-root-verified.log`). Oppituntien navigoinnin sijainti suhteessa järjestelmäpalkkiin arvioitiin alkuperäisistä koko näytön kuvista ja ohjainten laitetesteistä.

### Rajat ja avoimet havainnot

- 360 × 640 dp:n normaalitekstillä kaikki neljä etusivun toimintoa näkyvät. 320 dp:n leveydellä ja suurilla fonteilla vierittäminen on edelleen tarpeen. Yhdessä-pelin kortin painikkeet ovat saavutettavissa vierittämällä; koko kello ja kortti eivät aina näy samanaikaisesti.
- 24 tunnin englanninkielisessä kellossa `afternoon` osuu `past`/`to`-apusanojen alueelle. Näiden tekstien sijoittelu oli sama jo ennen tehtävää; sitä ei muutettu numerokehän korjauksen yhteydessä. Tämä on avoin erillinen visuaalinen havainto.
- 320 dp / 200 % -poluissa osa pitkistä vanhoista nimistä voi yhä katketa sanan keskeltä, esimerkiksi `Five-minute steps`, `vuorokaudessa` ja `Minututmaningen`. Suunnitelman vastaus-, painike- ja vaatenimien korjaukset eivät muuttaneet polkuotsikoiden aiempaa fontin pienennysrajaa. Uusi C1-pelilinkin toimintoteksti korjattiin erikseen.
- Kytkimien roolit ja tilanvaihto sekä apupainikkeen saavutettavuuskuvaukset tarkistettiin testeistä/lähteestä. TalkBackin puhetta, äänenlaatua tai fyysistä laitetta ei tarkistettu tässä tehtävässä.
- Aikaisemmissa JVM-ajoissa esiintyi Windowsin tilapäisten DataStore-tiedostojen virheitä. Uuden vaatekaappitestin oikea sovellussäiliö korvattiin olemassa olevalla `FakeServices`-testifixtuurilla; tuotannon tallennuslogiikkaa ei muutettu. Viimeinen koko 217 testin ajo läpäisi.

### Lopetus

Tallessa on 766 PNG-kuvaa ja 51 koontikuvaa. Viimeiset 320 dp / 200 % Opi- ja vaatekaappikuvat tarkastettiin; uusi pelilinkin teksti pysyy kokonaisina sanoina. Kaikkien 45 vaatekaapin alkunäkymän tausta vastaa oikeaa näkymää myös aiemmin virheellisesti tallentuneissa kuvissa.

Testiemulaattorin näyttö palautettiin kokoon 1080 × 2400, tiheyteen 420 dpi, fonttiasteikkoon 1,0 ja eleohjaukseen. Tehtävän emulaattori suljettiin: `emulator-5590` poistui laitelistasta, ja käynnistyksen PID 39744 sekä tehtävän väliaikaiset ajot olivat päättyneet. Muita prosesseja tai laitteita ei pysäytetty. `git diff --check` läpäisi. Työ jäi commitoimatta `codex/localization-settings`-haaraan.
