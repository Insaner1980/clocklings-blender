# Clocklingsin kertojaäänet

Tässä dokumentissa on kaikki, mitä kertojaäänten tekemiseen OpenAI:n puhemallilla tarvitaan: 14 lausetta seitsemällä kielellä, ääniohjeet, käsittely ja tarkistuslista.

Kielet: suomi (fi), englanti (en), ruotsi (sv), saksa (de), ranska (fr), espanja (es) ja Brasilian portugali (pt-BR). Tiedostoja tulee 14 × 7 = 98.

## Ääniohjeet (`instructions`)

Jokaiseen pyyntöön tulee perusohje, kieliohje ja lauseen sävyohje. Ohjeet on kirjoitettu englanniksi, koska malli noudattaa niitä siten parhaiten.

### Perusohje (kaikki lauseet)

```
You are the warm, friendly narrator of a clock-learning app for children aged 4 to 8.
Speak clearly and a little slower than normal conversation, with natural intonation.
Sound cheerful and kind, but calm and not exaggerated or cartoonish.
Pronounce every word fully. Leave no long pauses at the start or end.
```

### Kieliohjeet

- **fi**: `Speak standard Finnish (yleiskieli) with natural Finnish intonation and stress on the first syllable of each word. No English accent.`
- **en**: `Speak neutral British English.`
- **sv**: `Speak neutral standard Swedish as spoken in Sweden (rikssvenska).`
- **de**: `Speak standard German (Hochdeutsch).`
- **fr**: `Speak standard metropolitan French from France.`
- **es**: `Speak Castilian Spanish from Spain (es-ES), not Latin American Spanish.`
- **pt-BR**: `Speak Brazilian Portuguese (pt-BR), not European Portuguese.`

### Sävyohjeet

- **neutraali**: ei lisäohjetta, pelkkä perusohje.
- **opettava**: `Gentle and patient, like explaining something new to a small child.`
- **iloinen**: `Happy and warmly proud of the child.`
- **kannustava**: `Warm and encouraging, never disappointed.`
- **innostava**: `Energetic and encouraging, short and crisp.`
- **leikkisä**: `Playful and smiling, a little softer.`

## Lauseet

Tiedostonimessä on vain pieniä kirjaimia, numeroita ja alaviivoja.

### Tehtävänannot

#### 1. `voice_question_read`

Sävy: neutraali.

- fi: Mitä kello on?
- en: What time is it?
- sv: Vad är klockan?
- de: Wie spät ist es?
- fr: Quelle heure est-il ?
- es: ¿Qué hora es?
- pt-BR: Que horas são?

#### 2. `voice_question_set`

Sävy: neutraali.

- fi: Aseta kelloon tämä aika.
- en: Set the clock to this time.
- sv: Ställ klockan på den här tiden.
- de: Stelle die Uhr auf diese Zeit.
- fr: Règle l’horloge sur cette heure.
- es: Pon esta hora en el reloj.
- pt-BR: Ajuste o relógio para este horário.

### Viisarivihjeet

#### 3. `voice_hint_hour`

Sävy: opettava.

- fi: Lyhyt pinkki viisari näyttää tunnin.
- en: The short pink hand shows the hour.
- sv: Den korta rosa visaren visar timmen.
- de: Der kurze rosa Zeiger zeigt die Stunde.
- fr: La petite aiguille rose indique l’heure.
- es: La aguja corta y rosa marca la hora.
- pt-BR: O ponteiro pequeno e rosa mostra a hora.

#### 4. `voice_hint_minute`

Sävy: opettava.

- fi: Pitkä sininen viisari näyttää minuutit.
- en: The long blue hand shows the minutes.
- sv: Den långa blå visaren visar minuterna.
- de: Der lange blaue Zeiger zeigt die Minuten.
- fr: La grande aiguille bleue indique les minutes.
- es: La aguja larga y azul marca los minutos.
- pt-BR: O ponteiro grande e azul mostra os minutos.

### Palkitseminen

#### 5. `voice_result_3`

Sävy: iloinen.

- fi: Upeaa!
- en: Wonderful!
- sv: Underbart!
- de: Wunderbar!
- fr: Formidable !
- es: ¡Estupendo!
- pt-BR: Maravilha!

#### 6. `voice_result_2`

Sävy: iloinen.

- fi: Hyvin tehty!
- en: Well done!
- sv: Bra gjort!
- de: Gut gemacht!
- fr: Bravo !
- es: ¡Muy bien!
- pt-BR: Muito bem!

#### 7. `voice_result_1`

Sävy: kannustava.

- fi: Jatka tutkimista!
- en: Keep exploring!
- sv: Fortsätt utforska!
- de: Entdecke weiter!
- fr: Continue à explorer !
- es: ¡Sigue explorando!
- pt-BR: Continue explorando!

#### 8. `voice_result_reward`

Sävy: iloinen.

- fi: Sait palkinnon!
- en: You earned a reward!
- sv: Du fick en belöning!
- de: Du hast eine Belohnung bekommen!
- fr: Tu as gagné une récompense !
- es: ¡Has conseguido un premio!
- pt-BR: Você ganhou um prêmio!

### Tervehdykset

#### 9. `voice_greeting_morning`

Sävy: iloinen.

- fi: Hyvää huomenta!
- en: Good morning!
- sv: God morgon!
- de: Guten Morgen!
- fr: Bonjour !
- es: ¡Buenos días!
- pt-BR: Bom dia!

#### 10. `voice_greeting_day`

Sävy: iloinen.

- fi: Hei!
- en: Hello!
- sv: Hej!
- de: Hallo!
- fr: Salut !
- es: ¡Hola!
- pt-BR: Olá!

#### 11. `voice_greeting_evening`

Sävy: iloinen.

- fi: Hyvää iltaa!
- en: Good evening!
- sv: God kväll!
- de: Guten Abend!
- fr: Bonsoir !
- es: ¡Buenas tardes!
- pt-BR: Boa noite!

#### 12. `voice_greeting_night`

Sävy: leikkisä.

- fi: Hei, yökukkuja!
- en: Hello, night owl!
- sv: Hej, nattuggla!
- de: Hallo, Nachteule!
- fr: Salut, oiseau de nuit !
- es: ¡Hola, búho nocturno!
- pt-BR: Olá, corujinha!

### Minuuttihaaste

#### 13. `voice_challenge_go`

Sävy: innostava.

- fi: Aloita!
- en: Start!
- sv: Starta!
- de: Los!
- fr: C’est parti !
- es: ¡Empezar!
- pt-BR: Começar!

#### 14. `voice_challenge_over`

Sävy: innostava.

- fi: Aika loppui!
- en: Time is up!
- sv: Tiden är ute!
- de: Die Zeit ist um!
- fr: Le temps est écoulé !
- es: ¡Se acabó el tiempo!
- pt-BR: O tempo acabou!

## Tekeminen

### Malli ja ääni

- Malli: `gpt-4o-mini-tts` (Speech API). **Malli suljetaan 6.1.2027.** Tee kaikki 98 tiedostoa samassa erässä ennen sitä, koska myöhemmin uudella mallilla tehdyt lisäykset kuulostaisivat erilaisilta.
- Valitse yksi valmisääni koekuuntelemalla ja käytä sitä kaikissa kielissä ja lauseissa.
- Pyydä vastaus WAV-muodossa ja pakkaa se itse alla olevalla tavalla.
- Kirjaa jokaisesta tiedostosta teksti, kieli, malli, ääni, koko `instructions`-teksti ja päivämäärä. Säilytä alkuperäiset WAV-tiedostot.

### Käsittely ffmpegillä

Lopputulos: OGG Vorbis, mono, 24 kHz, 48 kbit/s. Äänekkyys on −20 LUFS, ja se säädetään pelkällä vahvistuksella ilman kompressoria tai limitteriä.

1. Leikkaa hiljaisuus alusta ja lopusta (noin 50 ms jää jäljelle):

   ```bash
   ffmpeg -i in.wav -af "silenceremove=start_periods=1:start_threshold=-50dB:start_silence=0.05,areverse,silenceremove=start_periods=1:start_threshold=-50dB:start_silence=0.05,areverse" trimmed.wav
   ```

2. Mittaa äänekkyys (lue lopusta `I:`-arvo LUFS-yksikössä):

   ```bash
   ffmpeg -i trimmed.wav -af ebur128=peak=true -f null -
   ```

3. Vahvistus on `-20 - mitattu arvo` dB. Jos mitattu arvo on esimerkiksi −16,5, vahvistus on −3,5 dB. Pakkaa:

   ```bash
   ffmpeg -i trimmed.wav -af "volume=-3.5dB" -ac 1 -ar 24000 -c:a libvorbis -b:a 48k voice_question_read.ogg
   ```

4. Tarkista lopputulos: äänekkyys −20 ±0,5 LUFS ja huippu (true peak) enintään −1 dBTP.

### Kansiorakenne

Jokaisella kielellä on oma kansionsa, ja tiedostonimet ovat kaikissa samat:

```
fi/voice_question_read.ogg
en/voice_question_read.ogg
sv/voice_question_read.ogg
de/voice_question_read.ogg
fr/voice_question_read.ogg
es/voice_question_read.ogg
pt-BR/voice_question_read.ogg
```

## Kuuntelulista

Kuuntele jokainen tiedosto. Kiinnitä erityistä huomiota näihin:

- **fi**: äänteiden pituudet ja painotus ("yökukkuja", "viisari", "minuutit", "Hyvää huomenta").
- **en**: brittiläinen ääntämys, ei amerikkalainen.
- **sv**: riikinruotsalainen aksentti, ei suomenruotsalainen. Tarkista "visaren", "klockan" ja "nattuggla".
- **de**: "Zeiger", "Nachteule".
- **fr**: "l’horloge", "aiguille" ja sanojen luonteva sidonta.
- **es**: kastilialainen ääntämys. Esimerkiksi sanan "Empezar" z lausutaan θ-äänteenä eikä s-äänteenä.
- **pt-BR**: brasilialainen ääntämys sanoissa "relógio", "ponteiro" ja "corujinha", ei portugalilainen.
- **Kaikki kielet**: alussa ei ylimääräistä hengitystä tai taukoa, loppu ei katkea, ja äänekkyys on sama kuin muissa tiedostoissa.

Natiivipuhujan tarkistus on suositeltava ainakin ruotsille, ranskalle, espanjalle ja portugalille.
