# Clock-learning app for Google Play: product and implementation plan

**Author:** Claude (Opus 5.5), written for comparison with an independent plan by Codex.
**Date:** 2026-10-20
**Status:** Superseded historical comparison draft. See [STATUS.md](implementation/STATUS.md) for current owner decisions and [CONSOLIDATED-STATUS.md](implementation/CONSOLIDATED-STATUS.md) for implementation and verification. Earlier English-first release and original Git-history copying proposals below are not current instructions.
**Working title:** "the store app". The final name is an open decision (section 15).

---

## How to read this document

- **Proposed status** labels on features:
  - **IN**: proposed for the launch release.
  - **OUT**: proposed to be left out of the product entirely.
  - **INVESTIGATE**: cannot be decided responsibly yet; the section names what must be learned and how.
  
  These are Claude's proposals. The owner decides. An idea appearing in both plans does not mean it has been chosen.
- **MUST / SHOULD / MAY** are used in the RFC sense inside requirements.
- **Evidence:** factual claims that were checked against a source are marked with a source reference in section 17. Claims that are reasoned assumptions are marked *(assumption)*. Section 16 lists both groups explicitly.
- **Release model:** the app launches once, complete. There is no "minimum version, more later" plan in this document. Work is done in stages before release; the public release happens only when every feature with status IN meets its definition of done (section 13). Additional languages are added after the English release, each one only when it meets the language readiness bar (section 8.6).

---

## Table of contents

1. Purpose, audience, and principles
2. Decisions already made
3. Relationship to the existing Sateenkaarikello app
4. Planning process and stage exit criteria
5. User journeys (draft)
6. Feature catalogue
7. English time-telling specification
8. Localisation beyond English
9. Learning model: skills, misconceptions, hints, sessions, rewards
10. Prototypes and experiments
11. Technical plan (provisional)
12. Google Play release plan
13. Launch readiness criteria
14. Risks and mitigations
15. Open decisions for the owner
16. Evidence status: verified facts and assumptions
17. Sources

---

## 1. Purpose, audience, and principles

### 1.1 Purpose

A native Android app that teaches children to read and set an analogue clock, understand how hours and minutes relate, connect analogue, digital, and spoken time, and begin to use time in everyday situations (what time something happens, how long until something happens).

It grows out of Sateenkaarikello, a Finnish app made for one child, and keeps its strongest qualities: a warm and personal tone, gentle feedback, animated explanations, an interactive clock that responds well to dragging, and a companion the child dresses and names.

### 1.2 Audience

- **Primary learner:** children aged about 6 to 8 who are learning to tell time at school or at home.
- **Secondary learners:** 5-year-olds who cannot yet read fluently (supported by optional listening, section 6.7), and 9-year-olds who still need practice with minutes, a.m./p.m., 24-hour time, and elapsed time.
- **Adults:** parents, grandparents, and older siblings who install the app, adjust a few settings, and sometimes play together. Teachers may recommend it but the app is not designed as a classroom tool (see section 12.6 for why this matters).

### 1.3 Product principles

These principles constrain every feature. A feature that conflicts with them is changed or dropped.

1. **Legibility of the clock comes first.** Themes, companions, and effects never make the hands, numbers, or centre of the clock harder to read. Decoration goes around the clock, not into the parts being learned.
2. **Appearance and learning support are independent.** A child's choice of theme or companion never changes task difficulty. Learning aids (minute numbers, sectors, highlights) are separate settings that work in every theme.
3. **The child never loses anything by being away.** No companion gets sad or hungry, no streak breaks, nothing decays. Coming back is always a happy event.
4. **Mistakes are part of learning.** Feedback after a mistake is specific, kind, and gives the child another real attempt before showing an answer.
5. **Every task works in silence.** Sound and speech are optional aids. No task requires hearing.
6. **The real clock is the goal.** The child should be able to read ordinary clocks without the app's colours and helpers. A plain wall-clock face is part of the learning path, not just a theme.
7. **No ads, no tracking, no accounts, no network.** The app keeps the original's zero-permission design unless a specific feature proves it needs more and the owner accepts the trade-off. Any such exception is listed in section 11.9.
8. **Natural stopping points.** Sessions end at a clear point after success, with a calm choice to continue, explore, or stop.
9. **Fair to all children.** Characters and clothes are not split by gender. Diverse skin tones, hair types, glasses, and mobility aids are available from the start.
10. **Launch complete.** Every feature included in the release is finished, tested, and reviewed. Nothing is shipped as a placeholder.

---

## 2. Decisions already made

| # | Decision | Source |
| --- | --- | --- |
| D1 | The store app is a new, separate app. The existing Sateenkaarikello stays exactly as it is for the owner's niece. | Owner |
| D2 | English is the first language. Other languages follow after the English release. | Owner |
| D3 | The release contains every planned feature. No "first version, rest later" plan. | Owner |
| D4 | "Kysy siskolta" (Ask your sister) is not carried over in its family-specific form. A neutral shared-play mode replaces it (details are open, section 6.4). | Owner, agreed by both AIs |
| D5 | Children choose a clock face appearance and a companion. | Owner |
| D6 | Speaking the time aloud (TTS) is optional, never required. Microphone-based answers are not built. | Agreed in discussion |
| D7 | There is no budget for paid translators or paid native-speaker reviewers. | Owner |
| D8 | A language is released only when it meets a readiness bar agreed in advance (section 8.6). A human reviewer is welcome but not required. | Agreed in discussion |
| D9 | Planning order: journeys, then candidate features with IN/OUT/INVESTIGATE, then prototypes, then locking release content and readiness criteria, then the technical design. | Agreed in discussion |
| D10 | The technical structure is derived from decided use cases, not designed first. Section 11 is therefore provisional. | Agreed in discussion |
| D11 | The owner's niece may give feedback on prototypes voluntarily. She is not a required tester and her feedback is not a release condition. | Agreed in discussion |

---

## 3. Relationship to the existing Sateenkaarikello app

### 3.1 Separation

- The store app lives in a **new Git repository** created from the current repository's history (so useful history and tooling come along), with its own remote.
- It gets its own `applicationId`, app name, launcher icon, signing key, and Play listing. Because the `applicationId` differs, both apps can be installed on the same device.
- The existing repository receives no store-app changes. This document is stored in the existing repository only temporarily and should move to the new repository when it is created.

### 3.2 What the existing app already provides (inventory, verified by reading the code on 2026-10-20)

| Area | Existing implementation | Reuse outlook |
| --- | --- | --- |
| Clock maths | `domain/ClockTime.kt`: angles, snapping, minute and hour drag, hand picking, animation duration | Reuse almost unchanged |
| Interactive clock | `ui/clock/ClockFace.kt`: Canvas 400x400, drag, highlight, accessibility actions, `paintRingFrom`, `onSettle` | Reuse interaction; rendering split into theme layers |
| Levels | `domain/Levels.kt`: 5 levels (step 60/30/15/5/1), 10 tasks alternating READ and SET, 3 choices with misconception-based distractors, star rules | Reuse generation ideas; levels redefined per curriculum (section 9.2) |
| Hints | `domain/Hints.kt`: typed hints by hour position (exact, just passed, between, almost) and minute position | Generalise into the misconception model (section 9.3) |
| Lessons | `domain/Lessons.kt` demo DSL (Wait, Move, Highlight, Under, Over, Say) and `ui/lesson/LessonCatalog.kt` with 9 lessons | Reuse DSL; rewrite all lesson content for English |
| Time language | `domain/FinnishTime.kt` | Replace with a per-language phrasing module (section 11.4) |
| Wardrobe and doll | `domain/Wardrobe.kt` (18 items, 5 slots, unlocked by total stars), `ui/doll/` | Reuse the system ideas; drawing is per companion (section 6.5) |
| Progress and settings | `domain/Progress.kt`, `data/ProgressRepository.kt` (Preferences DataStore) | Extend to profiles and skill records |
| Audio | `audio/ToneSynth.kt`, `Sfx.kt`, `Speech.kt` | Reuse SFX; rewrite Speech (section 6.7) |
| Celebrations | `ui/components/Confetti.kt`, `ConfettiMath.kt` | Reuse |
| Intro | `ui/intro/` | Rework for companion and theme |
| Free clock, challenge, ask | `ui/freeclock/`, `ui/challenge/`, `ui/ask/` | Free clock reused; challenge becomes optional mode; ask becomes Play Together |
| Quality tooling | ktlint, detekt with compose rules, Android lint, Compose stability config, semgrep and OWASP scripts | Carry over |
| Screenshot test | `GalleryTest` has two tests only (doll in every item, clock variants). It does not cover screens, menus, lessons, or languages. | Storage approach reusable; full screen and language coverage must be built new |

### 3.3 Note about the niece's sideloaded APK

Google is introducing developer verification for apps installed outside Google Play on certified Android devices: first in Brazil, Indonesia, Singapore, and Thailand from 30 September 2026, and globally from 2027 according to current reporting. A free limited-distribution account for hobbyists allows sharing with up to 20 devices without government ID. Before the requirement reaches Finland, the owner should register the existing app this way so it stays installable and updatable. This is independent of the store app but should not be forgotten. *(Dates come from news reporting; confirm with Google's developer documentation before acting.)*

---

## 4. Planning process and stage exit criteria

The stages are sequential in principle, but later findings may send work back to an earlier stage by the owner's decision.

| Stage | Output | Exit criterion |
| --- | --- | --- |
| S1 Journeys | Section 5 completed and accepted by the owner | The owner recognises each journey as the experience they want; open choices inside the journeys are listed |
| S2 Candidate features | Section 6 with each feature marked IN, OUT, or INVESTIGATE by the owner | Every feature has a status; every INVESTIGATE item has a concrete question and a planned prototype |
| S3 Prototypes | Results for section 10 experiments | Each INVESTIGATE item is resolved to IN or OUT with a written reason |
| S4 Release content lock | Final feature list, per-feature definitions of done, English specification signed off | No feature has unresolved design questions that affect what the child does or learns |
| S5 Technical design | Section 11 rewritten as a confirmed design | Every IN feature maps to modules, data, and tests |
| S6 Build | Implementation in the new repository | All IN features meet their definition of done |
| S7 Release preparation | Section 12 items complete | Section 13 launch criteria all satisfied |

Changes after S4 are allowed when building reveals that a design does not serve the child. Each such change is a recorded owner decision.

---

## 5. User journeys (draft)

Journeys describe whole sessions. They exist to show how features fit together and where they compete for the child's attention. Reading ability is a thread in every child journey rather than a separate journey.

### J1 Adult first start (short shared opening)

**Who:** a parent installs the app on their phone or a family tablet and opens it with the child nearby.

1. The app opens to a short animated welcome (clock spins to the current time, companion silhouettes wave). Tap skips it.
2. One adult screen, shown once, with sensible defaults already selected:
   - Clock words: **UK** or **US** (preselected from device locale; explained with one example each: "quarter past three" and "quarter after three").
   - 12-hour or 24-hour clock (preselected from region; 24-hour is offered as a later learning step, not the starting mode, for UK; hidden by default for US).
   - "Read instructions aloud" toggle (shown only if a suitable voice exists, section 6.7).
   - A link "More settings later in the grown-up area".
   - One button: "Hand over to your child".
3. No parental gate on this screen (nothing can be bought or opened externally here). The grown-up area later is behind a gate (section 6.8).
4. The child's part begins (J2 step 1).

**Must be true:** the adult understands within one minute what the app does, that it has no ads or purchases (if that is the business model chosen), and how to change settings later.

**Open choices:** whether profiles are created here or later; whether to ask the child's age or school year (proposed: no; the app adapts by skill, not age).

### J2 A child learning from the start

**Who:** a 6-year-old who knows numbers to 12, maybe reads short words.

1. **Choose a companion.** Six large picture cards (section 6.5). Choosing is immediate; every base companion is available. A "Just the clock" option exists. The companion greets the child (animation; optional speech).
2. **Optional naming.** The companion asks for a name. Skip is equally prominent.
3. **Choose a clock face.** Four to six theme previews showing the same time. The plain wall clock is among them but is not the default for beginners.
4. **First lesson: "Two hands".** Animated demo: short hand, long hand, numbers going round. The companion points. Each explanation page has a listen button (if speech is on). One quiz question.
5. **First practice journey** of 3 tasks (o'clock only). Correct answers: celebration and a star flying to the progress dots. One wrong answer: hint ladder step 1 (section 9.4).
6. **Natural end.** "That was today's trip!" Choices: "Another trip", "Play with the clock", "Dress up", "Stop for now". The companion waves.
7. On the next visit the companion is simply happy to see the child. The next lesson ("Half past") is suggested, not forced. Any level and lesson remains open (the original's no-lock decision is kept).

**Pre-reader thread:** every instruction has an icon and an optional listen button; tasks use pictures and the clock, not paragraphs; answer choices in READ tasks show the clock time as digital time plus words, and the words can be listened to.

**What can go wrong:** the companion steals attention from the clock (prototype P3); the theme choice overwhelms (limit to 4 to 6 visible); the child cannot tell which hand is which in a theme (prototype P1).

### J3 A child who already reads o'clock and half past

**Who:** a 7-year-old practising quarters and five-minute intervals.

1. Home shows the companion, today's suggested activity, and the skill map ("I can: o'clock, half past. Practising: quarter to.").
2. The child picks **Clock Repair Shop** (section 6.3, G1): broken clocks arrive with a ticket "Quarter to four". The child fixes the hands. A mistake where the hour hand sits exactly on 4 triggers the misconception hint "At quarter to four, the short hand has not quite reached 4 yet."
3. Practice adapts: if quarter-to items keep failing, the next journey includes more of them, mixed with already mastered items so success stays frequent (section 9.5).
4. A new sticker for "Quarter past and quarter to" appears in the album when the skill is mastered.
5. The child tries the plain wall-clock face in "Same time, different clocks" (G8) to transfer the skill.
6. Optional **Minute Challenge** (60-second mode) is available but not suggested by default.

### J4 Playing together

**Who:** a child and an adult or sibling, one device.

1. From home: **Play Together**. Each player taps a companion token to show who they are (no names required).
2. Mode choice with three picture cards (section 6.4): "Ask and answer", "Make a task card", "Teach me".
3. Example, "Make a task card": the child picks a character, an event ("Picnic"), and sets a time on the clock out of the other player's sight. The card shows "The dragon's picnic starts at...". The adult answers aloud or sets their own clock. Reveal compares the two clocks side by side.
4. Turns swap with one button. No score by default; an optional "keep score" is off.
5. "Teach me": the adult sets the clock and gives an answer that may be deliberately wrong; the child judges with "Right" or "Not quite" and then shows the correct time.
6. Ending: a shared celebration, then back to home.

### J5 Everyday time (INVESTIGATE)

**Who:** a parent who wants the child to see how long 10 minutes is before leaving the house.

1. Parent opens the **Visual Timer** (section 6.6) from home or the grown-up area.
2. Sets 10 minutes; the child's chosen face shows a coloured sector that shrinks as time passes; the companion sits beside it.
3. The screen stays on while the timer runs; when it ends, a gentle sound and animation play.
4. Optional: "What time will it be when the timer ends?" mini-question for the child.

**Open questions:** whether the timer must keep running when the screen turns off (this affects permissions, section 11.9), whether it lives in the child area or the adult area, and whether routines and a home-screen widget belong in the product at all. These are why J5 and its features are INVESTIGATE.

---

## 6. Feature catalogue

Each feature entry contains: what the child does, the learning purpose, help behaviour, connections, definition of done (DoD), proposed status, and open questions. Short entries are used where the detail is obvious.

### 6.1 Learning core

#### L1 Clock School (lessons) — proposed **IN**

- **Child does:** watches short animated explanations (2 to 4 pages), replays them, answers one quiz question.
- **Learns:** the conceptual steps of each skill (section 9.1).
- **Help:** replay button, optional listen, companion points at the relevant part.
- **Connections:** each skill in the skill map has one lesson; practice suggests the lesson after repeated misconceptions.
- **Content:** fully rewritten for English. Proposed lesson list (English UK; US differences noted in section 7):
  1. Two hands
  2. The long hand goes round (one turn of the long hand is one hour; the short hand moves too)
  3. O'clock
  4. Half past
  5. Quarter past and quarter to
  6. Past and to (right half and left half of the clock)
  7. Counting in fives
  8. Minutes between the numbers
  9. Morning, afternoon, evening; a.m. and p.m.
  10. Noon and midnight
  11. The 24-hour clock (UK path; optional in US path)
  12. How long? (elapsed time with a sector)
  13. Roman numerals (UK Year 3 content; optional)
- **DoD:** every lesson has final English text reviewed against section 7, works with listen on and off, works at 200% font scale on a 320 dp wide screen, and has a quiz with exactly one correct answer.

#### L2 Free Clock — proposed **IN**

- Child drags either hand; time is shown in words (current variant), digital form, and optionally spoken. "Random time" button. Hour hand moves with the minute hand (gearing, see G11).
- Uses the chosen face and help layers.
- **Addition:** button-based adjustment (+/- minute, +/- 5 minutes, +/- hour) as an accessible alternative to dragging (section 6.9).

#### L3 Practice journeys (levels) — proposed **IN**

- Replaces "Pelaa" levels. A journey is 3, 5, or 10 tasks (child picks; default 5).
- Task types: READ (choose among 3), SET (drag hands), and the game-mode task types from section 6.3 where they fit.
- Levels map to skills (section 9.2) and remain freely selectable.
- Stars per journey stay (0 to 1 mistakes: 3 stars, 2 to 3: 2 stars, more: 1 star) *(proposal: keep the existing rule; review in P2)*.

#### L4 Skill map — proposed **IN**

- Visual map of skills: "I can", "Practising", "Not started". No comparison with other children, no percentages for the child. The adult area shows a slightly more detailed view (section 6.8).

#### L5 Adaptive practice and review — proposed **IN**

- Task selection weights skills by recent success and by detected misconceptions. Mastered skills reappear occasionally for review. Rules are local and transparent (section 9.5).

#### L6 Minute Challenge (60-second mode) — proposed **IN as optional side mode**

- Kept from the original, but not on the main learning path and not suggested automatically. Shows personal best only.

### 6.2 Help and feedback (cross-cutting) — proposed **IN**

- **Hint ladder** after a wrong answer (section 9.4): attention, demonstration, guided solving. After each hint, the child gets a real new attempt.
- **Misconception-specific hints** (section 9.3).
- **Help layers** that the child or adult turns on and off independently of theme: minute numbers around the edge, past/to halves, quarter sectors, tick emphasis, hand labels.
- **Calm mode:** removes background motion and extra decoration; keeps the clock and the task.

### 6.3 Game modes

Each is a variation of READ or SET tasks with a small story frame, which keeps the engine small while the experience varies *(assumption about effort; confirm in S5)*.

| ID | Name | Child does | Learns | Proposed status |
| --- | --- | --- | --- | --- |
| G1 | Clock Repair Shop | Fixes clocks whose hands are wrong for the ticket time | Hand roles; hour-hand position between numbers | **IN** |
| G2 | Silly Robot | The robot companion answers wrongly; the child finds and fixes the mistake | Recognising common misconceptions | **IN** |
| G3 | The Train Leaves! | Sets the clock to the time on a digital ticket | Digital to analogue | **IN** |
| G4 | Clock Pairs | Matches cards: analogue, digital, words | Three representations of the same time | **IN** |
| G5 | Before or After? | Orders 3 to 4 events by time | Comparing times; a.m./p.m. | **IN** |
| G6 | How Long Until? | Moves time forward by an interval or reads the interval between two clocks | Elapsed time | **IN** |
| G7 | One Hand Hidden | Sees only the hour hand and estimates the time | The hour hand alone carries information | **INVESTIGATE** (may be too hard; P2) |
| G8 | Same Time, Different Clocks | Finds the same time on different faces including the plain wall clock | Transfer to real clocks | **IN** |
| G9 | Clock Puzzle | Places numbers 1 to 12 on an empty face | Structure of the dial | **IN** (early skill) |
| G10 | Which Hand Is Which? | Short drills identifying and moving the right hand | The most basic confusion | **IN** (folded into L1/G1 if it feels repetitive) |
| G11 | Gear View | Turns the minute hand and watches the hour hand creep; optional see-through gears | One minute-hand turn moves the hour hand one number | **IN** inside Free Clock and lesson 2 |
| G12 | Unrolled Clock | The rim of 60 minutes unrolls into a straight line and rolls back | A full turn is 60 minutes; links to number lines | **INVESTIGATE** (comprehension, P4) |
| G13 | Can This Clock Be Right? | Judges impossible clocks (minute hand at 6, hour hand exactly on a number) | Hour-hand position must match minutes | **IN** (part of G2) |
| G14 | Same Clock, Two Moments | The companion eats breakfast at 7:00 and hears a bedtime story at 7:00; both appear on a day timeline | a.m./p.m., day parts | **IN** |
| G15 | A Day with My Companion (story) | A chain of G1, G3, G5, G6 tasks forms the companion's day | Time in context | **INVESTIGATE** (scope; P6 shows if a chain feels like a story or a chore) |
| G16 | Guess a Minute | Child presses when they think one minute has passed | Feel for duration; not scored as clock skill | **INVESTIGATE** |

**Explicitly not a game mode:** microphone answers (OUT, D6).

### 6.4 Play Together — proposed **IN** (modes below)

Replaces "Kysy siskolta". Works with any second person. The app is never required to be used with an adult; Play Together is one optional area (this matters for Teacher Approved eligibility, section 12.6).

| Mode | Description | Proposed status |
| --- | --- | --- |
| PT1 Ask and Answer | The original flow, generalised: one sets the clock privately, the other answers aloud, reveal shows the answer; one-tap turn swap | **IN** |
| PT2 Make a Task Card | Child picks a character, event, and time; the other player solves it; reveal compares | **IN** |
| PT3 Teach Me | The adult gives an answer (sometimes deliberately wrong); the child judges and corrects | **IN** |
| PT4 Shared Trip | Both players' correct answers move the same trip forward; no winner | **INVESTIGATE** (overlap with PT1 plus a progress bar) |
| PT5 Split-screen race (tablet) | Two clocks, one target, first to set it correctly | **INVESTIGATE** (competition may not suit adult and child; layout cost) |

- **Scoring:** off by default; optional tally.
- **DoD:** each IN mode tested with an adult and child pair in prototype P6; works on phone and tablet; no text requires the child to read quickly.

### 6.5 Companions — proposed **IN** (count is an open decision)

#### 6.5.1 Candidate companions

| Companion | Customisation | Personality moment | Notes |
| --- | --- | --- | --- |
| Human friend (heir of the doll) | Skin tone, hair style and colour, clothes, glasses, hearing aid, wheelchair option | Waves and dances | The original doll's clothes and animations are a starting point |
| Cat or dog | Fur colours and patterns, scarf, collar, hat | Tilts head when thinking | Quadruped: different body, different clothing anchors |
| Dragon | Colour, horns, wings, accessories | Puffs a small cloud of stars | Growth stages (egg to young dragon) as a selectable look |
| Robot | Shell colour, antennae, face-screen expressions | Face lights up with an idea | Natural host for G2 Silly Robot |
| Dinosaur | Colours, patterns, backpack | Happy stomp | |
| Imaginary creature | Ears, eyes, tail, patterns | The child builds a silly friend | Highest combinatorial freedom; highest art risk |

#### 6.5.2 Rules

- All base companions available immediately; unlocks are extra items and looks.
- A **"Just the clock"** option hides the companion in tasks.
- Companions can be renamed and switched at any time; progress belongs to the child profile, not the companion.
- Companions never express suffering, hunger, or disappointment about absence (principle 3).
- **Growth stages** (dragon, possibly others): reaching a stage unlocks it; the child can choose any reached stage as the current look.
- **Presence options:** "lively", "calm", "almost silent" control animation amount and speech frequency.
- **Companion space:** a small place (room, forest clearing, spaceship, garden) decorated with earned items. Proposed **IN** if art budget allows; otherwise the sticker album carries the reward role. *(Decided after P3 and art estimate.)*

#### 6.5.3 What is shared and what is not

Shared across companions (one system): selection, naming, persistence, reward unlocking, reaction triggers (greeting, success, thinking, waving goodbye), placement in screens, accessibility descriptions.

Not shared (per companion): drawing, body proportions, clothing anchor points, item artwork, motion details. The design of each companion is done first; shared code is extracted afterwards (D10).

#### 6.5.4 Art production (major open risk)

The original doll is hand-authored vector paths from the web version. Six companions with clothing sets need substantially more art. Options:

1. Hand-authored vector paths (as now): consistent, fully owned, slow.
2. Open-licensed asset packs (for example CC0): fast, but style consistency and child-appropriateness must be checked item by item.
3. AI-generated artwork: fast to explore; licensing terms, style consistency, and vector conversion must be checked; Google Play's policies on AI-generated content must be followed.
4. Commissioned art: excluded by budget (D7).

**Proposal:** decide companion count only after producing one complete companion (base body plus 6 items plus 4 reactions) to measure real effort (prototype P3).

### 6.6 Everyday time tools — proposed **INVESTIGATE**

| ID | Feature | Child benefit | Key design questions |
| --- | --- | --- | --- |
| E1 | Visual Timer | Sees how long 5, 10, 15 minutes is | Must it run with the screen off? If yes, completion needs a notification or alarm, which needs permissions on newer Android (section 11.9). If no, keep the screen on while visible and end in-app. Who sets it: child or adult? |
| E2 | Routines | Sees what comes next today and at what time | Real time versus practice time; editing in the adult area; no reminders without notifications |
| E3 | Home-screen widget | Sees an analogue clock in their own theme every day | Android 12+ (API 31) supports custom dial and hand images in the widget `AnalogClock` through RemoteViews; behaviour must be tested on devices; API 26 to 30 need a fallback or the widget is offered only on API 31+ |

These are judged by child benefit, not by whether they keep the app on the phone. If included, each needs its own full design and DoD before S4.

### 6.7 Sound and speech — proposed **IN** (optional by design)

- **SFX:** kept (tone synthesis, tick, success, win). Separate toggles for effects and speech.
- **TTS listening:** optional listen buttons for instructions, lesson text, time phrases, and answer choices. Optional "read instructions automatically" for pre-readers (default off; decided in P7).
- **Voice requirements** the implementation MUST check:
  - Exact locale match (en-GB voice for UK variant, en-US for US). If only another English region is available, speak with it only if the owner accepts this (open decision), otherwise hide listening.
  - Offline voice: `Voice.isNetworkConnectionRequired()` false, unless the owner accepts network voices. Because the app has no INTERNET permission, a network voice would be fetched by the TTS engine, not by the app; this still breaks the "works offline" promise and must be detected and handled.
  - Spoken text MAY differ from displayed text (for example "a.m." spoken as "A M").
- **No microphone, no speech recognition** (D6).

### 6.8 Grown-up area — proposed **IN**

- Behind a **parental gate** (for example, "hold these two buttons for 3 seconds" or a simple arithmetic problem written in words). The gate protects external links (privacy policy, feedback e-mail), purchases if any, profile deletion, and learning-affecting settings.
- Contents:
  - Clock words: UK or US; 12-hour or 24-hour; speech settings; motion; sound.
  - Profiles: create, rename, delete (local only; proposed maximum 4).
  - Skill summary per child: mastered, practising, frequent misconceptions in plain language, with one suggestion for a real-life activity ("Ask your child to show you where the short hand is at half past four").
  - Reset progress (two-step confirmation, like the original).
  - Feedback: opens the e-mail app with a prefilled subject (no network permission needed).
  - Privacy policy link (opens browser).
  - Licences (fonts and any open-licensed art).

### 6.9 Accessibility — proposed **IN** (all items)

- Hands distinguishable by length, width, and shape, not only colour; colour-vision-safe palettes checked for each theme.
- Button alternative to dragging (L2, all SET tasks).
- TalkBack: every clock state has a spoken description; hands have accessibility actions (existing `ClockFace` supports actions; extend).
- Layout verified at 320 dp width with 200% font scale, and on tablets.
- Reduced motion respected (existing `rememberReduceMotion` behaviour kept: no confetti, no flying star, no companion motion).
- Calm mode (section 6.2).
- Left-handed friendly placement of main controls *(INVESTIGATE: check if any control placement is hand-dependent)*.

### 6.10 Profiles — proposed **IN**

Local profiles on the device, no accounts. Each has its own companion, face, settings for help layers, progress, skills, stickers, and decorations. Global settings: sound, speech, UK/US, 12/24 h default. One profile is created automatically; more can be added in the grown-up area.

### 6.11 Explicitly OUT

| Feature | Reason |
| --- | --- |
| Ads of any kind | Principle 7; Families policy complexity |
| Analytics and crash reporting SDKs | Principle 7; data safety |
| Accounts, cloud sync | Principle 7 |
| Subscriptions | Unsuitable for a finished learning product *(owner decision on monetisation still open)* |
| Social features, chat | Child safety; Teacher Approved excludes social features |
| Daily streaks, decay, sad companions | Principle 3 |
| Microphone answers | D6 |
| Camera, AR | Permissions; Families AR warning requirements; not needed |
| Classroom/teacher mode as a main feature | Teacher Approved excludes apps built mainly for classroom use *(see 12.6)* |

---

## 7. English time-telling specification

This section is the normative source for English phrases. Every rule must be checked against at least two independent, reliable sources before S4 (section 8.6 applies to English as well).

### 7.1 Variants

| Item | UK variant | US variant |
| --- | --- | --- |
| Default for device locale | en-GB, en-IE, en-AU, en-NZ, en-ZA, en-IN *(confirm per region)* | en-US, en-CA *(confirm)* |
| Spelling in UI | colour, favourite | color, favorite |
| :15 phrase | quarter past three | quarter after three (also accepted: quarter past) |
| :45 phrase | quarter to four | quarter to four (also accepted: quarter of / quarter till four, regional) |
| Primary reading style | "past/to" phrases | digital reading ("three forty-five") taught alongside "past/to" *(confirm against US teaching materials)* |
| 24-hour clock | Part of the path (Year 3 and 4) | Off by default; optional |
| a.m./p.m. written | a.m., p.m. *(confirm school convention)* | a.m., p.m. or AM, PM *(choose one; confirm)* |
| Noon and midnight | midday, midnight (noon accepted) | noon, midnight |
| Digital separator | 3:45 *(confirm; some UK materials use 3.45)* | 3:45 |

### 7.2 Phrase rules (UK, analogue reading)

Let `h` be the hour (1 to 12) and `m` the minute (0 to 59). "next" is the following hour (12 is followed by 1).

| Minutes | Phrase pattern | Example (h = 3) |
| --- | --- | --- |
| 0 | `<h> o'clock` | three o'clock |
| 5, 10, 20, 25 | `<m> past <h>` | twenty-five past three |
| 15 | `quarter past <h>` | quarter past three |
| 30 | `half past <h>` | half past three |
| 45 | `quarter to <next>` | quarter to four |
| 35, 40, 50, 55 | `<60 - m> to <next>` | twenty-five to four |
| 1 | `one minute past <h>` | one minute past three |
| 2 to 29, not multiple of 5 | `<m> minutes past <h>` | seven minutes past three |
| 31 to 59, not multiple of 5 | `<60 - m> minutes to <next>` | twenty-three minutes to four |
| 59 | `one minute to <next>` | one minute to four |

**Switch point:** "past" is used up to and including 30; "to" from 31. *(This is the most error-prone rule across languages; it must be confirmed by sources for English UK and US.)*

**The word "minutes":** required when the minute count is not a multiple of five; omitted for multiples of five. Singular "minute" for 1.

**Not taught:** colloquial "half three" (UK speech for 3:30). Lesson L1-4 SHOULD warn parents in the grown-up help that "half three" means half past three in UK speech, because the same phrase means 2:30 in several other languages *(assumption to verify; it is a known source of confusion for language learners)*.

### 7.3 Digital reading rules

| Time | Spoken digital form |
| --- | --- |
| 3:00 | three o'clock |
| 3:05 | three oh five |
| 3:30 | three thirty |
| 3:45 | three forty-five |
| 12:00 | twelve o'clock; noon or midday when a.m./p.m. context is active |
| 0:00 (24 h) | midnight; "zero hundred hours" is not taught *(assumption: confirm UK school usage of "hundred hours")* |
| 15:30 (24 h) | fifteen thirty |

### 7.4 Canonical form and accepted forms

Every time has:

- one **canonical display phrase** for the selected variant (shown in tasks and answer choices),
- zero or more **accepted alternatives** (shown in lessons as "also said as", used when the child or adult compares answers in Play Together),
- one **spoken form** (for TTS; may differ in punctuation and abbreviations).

The app never parses free text, so "accepted" means "presented as equally correct" in teaching and reveal screens.

### 7.5 Day parts (UK and US)

| Range | Word |
| --- | --- |
| 0:00 | midnight |
| 0:01 to 11:59 | in the morning (a.m.) *(night hours 0 to 5 may be "at night"; confirm what schools teach)* |
| 12:00 | midday or noon |
| 12:01 to 17:59 | in the afternoon (p.m.) |
| 18:00 to 23:59 | in the evening (p.m.); "at night" from about 21:00 *(confirm)* |

### 7.6 Curriculum alignment (for skill order and store description)

England, National Curriculum mathematics (statutory statements, paraphrased; verify wording before quoting):

| Year (age) | Time content |
| --- | --- |
| Year 1 (5 to 6) | Tell the time to the hour and half past the hour, draw hands to show these times; sequence events; days, weeks, months |
| Year 2 (6 to 7) | Tell and write the time to five minutes, including quarter past and quarter to; draw hands; know minutes in an hour and hours in a day; compare and sequence intervals |
| Year 3 (7 to 8) | Tell and write the time from an analogue clock including Roman numerals I to XII, and 12-hour and 24-hour clocks; read time to the nearest minute; a.m., p.m., morning, afternoon, noon, midnight; compare durations |
| Year 4 (8 to 9) | Convert between analogue and digital 12-hour and 24-hour clocks |

United States, Common Core State Standards for Mathematics:

| Grade | Standard | Content |
| --- | --- | --- |
| 1 | 1.MD.B.3 | Tell and write time in hours and half-hours using analog and digital clocks |
| 2 | 2.MD.C.7 | Tell and write time from analog and digital clocks to the nearest five minutes, using a.m. and p.m. |
| 3 | 3.MD.A.1 | Tell and write time to the nearest minute; measure and solve problems with time intervals in minutes |

Store text MAY say the skills follow these stages only after the final content is checked against the official texts. It MUST NOT claim endorsement.

### 7.7 Required English test data

- A generated table of all 1,440 minutes for each variant, 12-hour and 24-hour, with canonical, accepted, and spoken forms.
- A **golden set** of examples copied from cited sources (each test names its source), covering every row of 7.2 and 7.3, the switch points (29, 30, 31; 34, 35; 44, 45, 46; 59 to 0), noon, midnight, and 12 o'clock wrap-around (12:45 is "quarter to one").
- Rule-consistency tests over all minutes (for example "to" phrases always name the next hour; "minutes" appears exactly when `m % 5 != 0`).

---

## 8. Localisation beyond English

### 8.1 Principle

Time phrasing is grammar, not translation. Each language gets its own phrase rules, its own lesson order where the concepts differ, and its own checks. UI strings are translated as usual.

### 8.2 Known differences to research (a checklist, not a specification)

Each row is a research prompt. Every language is specified separately even when rows look similar here (Swedish, Norwegian, and Danish, for example, are separate specifications).

| Language | 3:30 | Points to research |
| --- | --- | --- |
| German | halb vier (refers to the next hour) | "fünf vor halb vier" (3:25) and "fünf nach halb vier" (3:35); regional "viertel vier" (3:15) and "dreiviertel vier" (3:45); choose the taught form |
| Dutch | half vier | Half hour is the reference from about :20 to :40 ("tien voor half vier" = 3:20); confirm exact range |
| Swedish | halv fyra | "fem i halv fyra" (3:25), "fem över halv fyra" (3:35), "kvart över", "kvart i" |
| Norwegian, Danish | halv fire | Separate specifications; confirm differences from Swedish |
| French | trois heures et demie | "moins le quart", "moins dix"; midi, minuit; 24-hour common in speech |
| Spanish | las tres y media | "es la una" (singular) vs "son las dos"; Spain "menos cuarto" vs Latin American "un cuarto para las cuatro" |
| Portuguese | três e meia | Portugal vs Brazil ("quinze para as quatro") |
| Italian | le tre e mezza | "è l'una", mezzogiorno, mezzanotte, "meno un quarto" |
| Polish | wpół do czwartej | Ordinal hours with case endings |
| Russian, Ukrainian | половина четвёртого | Ordinal genitive hours; "без четверти четыре" |
| Czech | půl čtvrté | "čtvrt na čtyři" (3:15), "tři čtvrtě na čtyři" (3:45) |
| Hungarian | fél négy | "negyed négy" (3:15), "háromnegyed négy" (3:45) |
| Hindi | saadhe teen | "sawa teen" (3:15), "paune chaar" (3:45); special words for 1:30 "dedh" and 2:30 "dhai" |
| Japanese, Chinese | 3時半, 三点半 | 午前/午後; Chinese "一刻" for a quarter, "差五分四点" |
| Arabic, Hebrew | (research) | Right-to-left UI; the clock MUST NOT be mirrored (hands always move clockwise); Eastern Arabic numerals on the dial as an option |

### 8.3 Non-phrase localisation work

- All UI strings, lessons, hints, companion lines, adult help, store listing.
- Plural and case rules (Android plurals, ICU MessageFormat where needed).
- Digital time format per locale (platform formatting; JVM and Android ICU data may differ, so tests must run on device as well).
- Fonts: Fredoka and Nunito cover Latin; scripts such as Cyrillic, Arabic, Hebrew, Devanagari, and CJK need checked font coverage or additional fonts with compatible licences *(coverage per font must be verified)*.
- Right-to-left layout with a non-mirrored clock.
- TTS voice availability per locale (section 6.7 checks).
- Per-app language selection (Android 13+ per-app language preferences with a generated locale configuration).

### 8.4 Who does the language work (no budget, D7)

1. **Drafting:** one AI (Claude or Codex) writes the phrase rules, tests, and translations, with sources cited for every rule.
2. **Independent review:** the other AI checks the draft against the cited sources and the context of use, with the explicit task of finding errors, not judging fluency.
3. **Automated checks:** rule-consistency tests over all minutes, golden examples from sources, missing-string checks, layout checks.
4. **Owner review:** overall behaviour, consistency, and whether the text fits the screen and the task. The owner is not expected to check foreign grammar.
5. **Optional human reviewer:** a competent speaker who knows how the language is taught to children (a native speaker is not automatically better than a teacher who learned the language later). Found through acquaintances, Finnish mother-tongue teachers, language exchange, or language communities. Their help adds confidence; it is not a condition.

Two AIs agreeing is not proof. Independence comes from the sources (section 8.6 bar 1), because both AIs may share training-data errors.

### 8.5 In-context review

- Screenshots of every screen and state in every language are reviewed (requires new screenshot infrastructure; the existing `GalleryTest` does not cover screens).
- Behaviour that a still image cannot show is checked by tests and walkthroughs: the hint matches the child's mistake, feedback appears in the right situation, the displayed time matches the task, and speech says the right thing.

### 8.6 Language readiness bar (agreed in advance)

A language is released only when all of the following hold:

1. **Rules sourced:** every phrase rule and every switch point (for example English past/to at 30/31; German "vor/nach halb" range; Dutch half-hour reference range) is confirmed by at least two independent, reliable sources, preferably children's teaching material from that country. Sources must describe the regional variety being taught. Golden examples from the sources are in the tests. Not every individual minute needs two sources; all minutes are checked programmatically against the sourced rules.
2. **Context checked:** every string has been reviewed in its screen context (screenshot) and every behaviour-dependent text by test or walkthrough.
3. **No open teaching issues:** the uncertainty log has no unresolved item that affects what the child is taught. Cosmetic wording doubts in menus may remain if logged. Resolution must rest on evidence, not on two AIs agreeing.

Two kinds of uncertainty are distinguished:

- **Known open problem:** "we do not know whether this taught phrase is correct". Blocks release.
- **General residual risk:** the content is checked, but no method guarantees zero errors. Accepted, as in any software.

### 8.7 Language order

Chosen by verifiability, not market size:

1. English (UK and US), owner reads English.
2. Swedish: the owner has school Swedish; Finland-Swedish reviewers may be available; "halv fyra" logic matches Finnish.
3. German: same "half of the next hour" logic; large market.
4. Then languages where sources are strong and a reviewer may be found (Spanish, French, Dutch), each judged against the bar individually.
5. Languages with heavier grammar (Polish, Russian, Czech) only when sources and review support them.

---

## 9. Learning model

### 9.1 Skills

| ID | Skill | UK year | US grade |
| --- | --- | --- | --- |
| S1 | Dial structure: numbers 1 to 12 clockwise | Y1 | G1 |
| S2 | Hand roles: short hour hand, long minute hand | Y1 | G1 |
| S3 | O'clock | Y1 | G1 |
| S4 | Half past | Y1 | G1 |
| S5 | Quarter past, quarter to | Y2 | G2 *(US: "quarter after/to" taught? confirm)* |
| S6 | Five-minute intervals, past and to | Y2 | G2 |
| S7 | Hour-hand position between numbers | Y2 | G2 |
| S8 | Nearest minute | Y3 | G3 |
| S9 | a.m., p.m., day parts, noon, midnight | Y3 | G2 |
| S10 | 24-hour clock | Y3 to Y4 | optional |
| S11 | Analogue and digital conversion | Y2 to Y4 | G1 to G3 |
| S12 | Elapsed time in minutes | Y3 | G3 |
| S13 | Roman numerals on clocks | Y3 | optional |

### 9.2 Levels

Levels are curated mixes of skills, freely selectable:

| Level | Content | Minute distribution (proposal) |
| --- | --- | --- |
| 1 O'clock | S2, S3 | always 0 |
| 2 Half past | S3, S4 | 70% :30 |
| 3 Quarters | S5 | 65% :15/:45, rest :00/:30 |
| 4 Fives | S6, S7 | 75% non-quarter fives |
| 5 Minutes | S8 | 75% non-multiples of five |
| 6 Day parts | S9 (12-hour with a.m./p.m.) | mixed |
| 7 24-hour (UK; optional US) | S10 | mixed |
| 8 How long? | S12 | intervals of 5 to 60 minutes |

### 9.3 Misconception model

Language-neutral misconception types, detected from the child's wrong answer:

| ID | Misconception | Detection | Hint focus |
| --- | --- | --- | --- |
| M1 | Hands swapped | Answer equals the swapped interpretation | Highlight short hand, name it |
| M2 | Hour rounded up after half past (2:50 read as 3:50) | Minutes right, hour one too high, m >= 30 | "The short hand has not reached 3 yet" |
| M3 | Hour hand placed exactly on a number when minutes are not zero (SET) | Minute right, hour hand on number | Gearing reminder (G11 animation) |
| M4 | Minute hand read as the number it points to (pointing at 4 read as 4 minutes) | Answer minutes = number | Count in fives with bubbles |
| M5 | Past and to confused (20 past read as 20 to) | Minutes mirrored | Show right half "past", left half "to" |
| M6 | Wrong hour in "to" phrases (twenty to four read as twenty to three) | Hour one too low with "to" | "To" counts towards the next hour |
| M7 | a.m./p.m. confusion | Same hands, other half of day | Sun and moon, day timeline |
| M8 | Tick counting off by one | Minutes differ by 1 | Count ticks from the number |
| M9 | Other / unknown | None of the above | Generic hint by hand (existing behaviour) |

Distractors in READ tasks are generated from M1, M2, M4, M5, M6, M7 (extends the existing `optionsFor`).

### 9.4 Hint ladder

After a wrong answer:

1. **Attention:** highlight the relevant hand or area, one short sentence.
2. **Demonstration:** animate a partial step (for example, minute hand sweeping in fives).
3. **Solve together:** guided steps; the child performs the final move or selection.

After steps 1 and 2 the child gets a real new attempt. Step 3 ends with the child producing the correct answer. There is no passive "here is the answer" reveal in practice tasks *(change from the original, which reveals after the second mistake; validate in P2)*. Play Together keeps an explicit reveal because there the reveal is the point.

### 9.5 Adaptive selection

- Per skill, keep the last 10 results (correct first try, correct after hint, incorrect) and misconception counts.
- **Mastered:** at least 8 of the last 10 correct on first try, across at least 2 different days *(thresholds to be tuned in P2)*.
- Journey composition: about 60% current skill, 25% weakest recent misconception, 15% review of mastered skills *(proposal)*.
- After a difficult item, the next item is easier (a known success).
- Never show the child percentages or comparisons.

### 9.6 Sessions and endings

- Journey length 3, 5, or 10 tasks; default 5.
- Ending screen after the last task: celebration, sticker if earned, and calm choices.
- No time pressure except in the optional Minute Challenge.

### 9.7 Rewards

| Reward | Earned by | Spent? | Can be lost? |
| --- | --- | --- | --- |
| Stars per level (best 0 to 3) | Journeys | No | No |
| Stickers | Lessons completed, skills mastered, effort milestones (for example "practised on three different days", no consecutive-day requirement) | No | No |
| Companion items and looks | Total stars and stickers | No | No |
| Companion space decorations | Stickers | No | No |
| Growth stages | Skill milestones | No | No; any reached stage selectable |

Rewards recognise effort as well as accuracy. Nothing is purchasable inside the child area.

---

## 10. Prototypes and experiments

Prototypes are small and disposable: images, clickable mock-ups, or single screens. They are not the start of the production architecture. Participants are voluntary; sessions are short and fun; the child can stop at any time; a parent is present; nothing is recorded except the facilitator's notes.

| ID | Question | Artefact | Signal | Feeds |
| --- | --- | --- | --- | --- |
| P1 | Can children tell the hands apart in each candidate theme? | Static images of 6 themes at 6 times | Correct reading rate similar to the plain face | Theme list, hand style rules |
| P2 | Does the hint ladder feel like help, and are mastery thresholds sensible? | Interactive single screen with hints | Child continues after hints; no frustration signs | 9.4, 9.5 |
| P3 | Does the companion help or distract? What does one complete companion cost to produce? | One companion with items and reactions | Attention stays on the clock during tasks; measured art hours | Companion count, companion space |
| P4 | Is the unrolled-clock animation understandable? | Animation prototype | Child can explain "a full turn is 60 minutes" afterwards | G12 |
| P5 | Is gear view understandable and interesting? | Free-clock prototype | Child predicts hour-hand position | G11 |
| P6 | Do Play Together modes work with adult and child? Does a story chain feel like a story? | Paper or clickable flow | Both players engaged; no confusion about turns | PT1 to PT5, G15 |
| P7 | How much listening does a pre-reader need? Automatic or on demand? | Lesson prototype with TTS | Child completes without adult reading aloud | 6.7 |
| P8 | Does the adult understand the app and settings in one minute? | First-start mock-up | Adult can explain UK/US and where settings live | J1 |
| P9 | Visual timer: screen-on only versus background | Simple timer screen | Parents' expectations | E1 |

The owner's niece may join P1 to P7 voluntarily; she knows the original, so her feedback shows what to keep. At least one child who has never seen the original app should take part where possible, to show whether a new user understands it.

---

## 11. Technical plan (provisional)

This section will be rewritten in stage S5 from decided use cases. It records the current expectations so that both plans can be compared.

### 11.1 Repository and project

- New repository from current history (`git clone`, new remote, then rename).
- New package and `applicationId` (placeholder `com.<owner>.<appname>`).
- Keep: Kotlin, Jetpack Compose, Navigation 3, DataStore, kotlinx.serialization, ktlint, detekt with compose rules, Android lint, Compose stability configuration, semgrep and OWASP scripts, Dependabot with a one-week delay.
- Versions: re-verify all at project start (the existing project uses AGP 9.4.1, Gradle 9.8.1, Kotlin 2.4.21, Compose BOM 2026.09.00, `compileSdk`/`targetSdk` 37, `minSdk` 26 as of 2026-10-14).
- Release format: Android App Bundle with Play App Signing (the original's APK and manual keystore remain for the niece's app only).

### 11.2 Module outline (to be confirmed)

```
domain/
  time/        ClockTime (reused), DayPart, period handling
  language/    TimeLanguage interface + per-variant implementations (en-GB, en-US, later others)
  curriculum/  skills, levels, lesson definitions per variant
  learning/    misconception classifier, hint ladder state machine, adaptive selection, mastery
  rewards/     stars, stickers, unlock rules
  companion/   companion kinds, slots per kind, item catalogue, reaction triggers
data/
  profiles, settings, progress, skill records (persistence)
audio/
  sfx (reused), speech (rewritten with voice checks)
ui/
  theme/       design tokens (as now)
  clock/       interaction (reused) + face themes + help layers
  companion/   per-companion renderers + shared host
  screens...   home, lessons, practice, game modes, play together, free clock, grown-up area
```

`domain` stays free of Android imports and is unit-tested on the JVM, as in the original.

### 11.3 Clock rendering

- `ClockFace` keeps drag, snapping, animation, highlight, accessibility.
- Rendering is split into layers: background decoration (theme), dial (theme, within legibility rules), help layers (independent), marks (lessons), hands (theme style within legibility rules), overlays.
- **Legibility rules (enforced by tests where possible):** minimum contrast between hands and dial; hour hand at most about 65% of minute hand length *(tune in P1)*; numbers never replaced by pictures; centre cap always visible.

### 11.4 Language module

- Interface sketch (to be confirmed by S4 answers): for a time and variant, return canonical display phrase, accepted alternatives, spoken form, digital string, and day-part phrase; plus lesson-specific phrase pieces (for example "past" and "to" labels for help layers).
- Each implementation cites its sources in code comments next to the rules.
- Digital formatting: `java.time` with locale on JVM for tests and the platform formatter on device; tests must run on device too because ICU data can differ.

### 11.5 Persistence

- Profiles and per-skill histories may outgrow Preferences DataStore. Candidates: typed DataStore with kotlinx.serialization, or Proto DataStore. Room is not expected to be needed. *(Decide in S5.)*
- Corrupted data falls back to defaults (as now) without crashing.

### 11.6 Speech

- Select a voice by exact locale; check `isNetworkConnectionRequired`; expose availability per variant; hide listening when no suitable voice exists.
- Rate slightly slower than default (original uses 0.9).

### 11.7 Testing strategy

| Layer | What | Where |
| --- | --- | --- |
| Domain unit tests | All 1,440 minutes per variant and mode; golden source examples; switch points; misconception classifier; hint ladder; adaptive selection; reward rules | JVM |
| ViewModel tests | Game modes, Play Together flows, lessons, using fakes (as now: `FakeServices`, in-memory store) | JVM |
| Compose UI tests | Navigation, task flows, accessibility semantics | Emulators API 26 and latest |
| Screenshot tests | Every screen and key state, every theme, every language, light conditions, 320 dp at 200% font, tablet | New infrastructure |
| Device checks | TTS voices offline/online, widget if IN, performance on a low-end device | Real devices (Pixel, Samsung) |
| Release checks | R8 release build, no permissions in merged manifest (or only accepted ones), size | CI or local script |

### 11.8 Performance

- Idle animations use frame-time APIs that let tests reach idle (as now).
- Companion animation reads pose at draw time to avoid recomposition per frame (as now).
- Target smooth dragging on a low-end device from the API 26 era *(set a measurable target in S5)*.

### 11.9 Permissions

| Feature | Possible permission | Plan |
| --- | --- | --- |
| Core app | None | Keep none |
| TTS | None (`<queries>` entry only) | Keep |
| Visual timer running in background with completion alert | Notifications (API 33+), possibly exact alarms | Avoid: screen-on, in-app timer unless the owner accepts a permission |
| Routines reminders | Notifications | Avoid; routines display only |
| Widget | None | OK |
| One-time purchase (if chosen) | Billing permission via Play Billing Library | Only if the paid-unlock model is chosen; data safety implications must be checked |

---

## 12. Google Play release plan

### 12.1 Account and testing requirement

- Developer account type decides testing obligations. Personal accounts created after 13 November 2023 must run a closed test with at least 12 testers for 14 continuous days before production access. Organisation accounts are exempt. The owner's account type must be checked.
- Even if not required, a closed test with families from English-speaking countries is planned for quality reasons.

### 12.2 Target API

From 31 August 2026, new apps and updates must target Android 16 (API 36) or higher. The current project targets 37, which satisfies this; re-check at release.

### 12.3 Families policy

- Target audience declared in Play Console (proposed age groups: 5 and under is not a target; 6 to 8 primary; 9 to 12 secondary *(confirm the exact Play age bands at release)*).
- Child-appropriate content; no SDKs that are not approved for child-directed services; no ads.
- A **privacy policy** that accurately reflects data handling is required. Proposed hosting: a simple static page (for example GitHub Pages) owned by the developer.
- Data safety form: filled from the final implementation. If the final app collects and shares nothing, it declares that; this is not decided in advance (if Play Billing is used, its declarations must be checked).
- Content rating questionnaire (IARC).

### 12.4 Store listing

- Name (open), short and full description in English, feature graphic, icon, phone and tablet screenshots (produced from the screenshot test infrastructure plus marketing framing), optional short video.
- Claims allowed: no ads, no tracking, works offline (if speech fallback is handled), skills follow the UK and US school stages (only after verification, no endorsement claims).

### 12.5 Monetisation (open decision)

| Model | Pros | Cons |
| --- | --- | --- |
| Free | Widest reach; simplest | No income |
| Paid upfront | No in-app purchases; zero-permission kept; clear to parents | Fewer installs; refund window |
| Free with one-time unlock behind parental gate | Try before buying | Play Billing integration; gate design; more review scrutiny |

No subscriptions, no consumables, no purchasable companions in the child area.

### 12.6 Teacher Approved

Google's Teacher Approved programme rates eligible apps and can badge them and show them on the Kids tab. According to Google's eligibility page, apps that comply with the Families policies are eligible, but apps meant to be used with an adult, apps built mainly for classroom use, apps needing a physical toy or another app, language-learning apps, and apps with social features are not supported. Consequences for this plan:

- The app must be fully usable by a child alone; Play Together is optional.
- No teacher or classroom mode as a primary feature.
- Later language versions must not be presented as language-learning apps.

Eligibility and criteria must be re-checked before release.

### 12.7 Release checklist (summary)

1. Account type confirmed; closed test completed if required.
2. Target API meets the current requirement.
3. Privacy policy published.
4. Data safety form matches the final build.
5. Target audience and content rating complete.
6. Store listing assets ready and reviewed for claims.
7. Release AAB built with R8, tested on API 26 and latest, Pixel and Samsung.
8. Section 13 criteria satisfied.

---

## 13. Launch readiness criteria

The English release happens only when all hold:

1. Every feature with status IN meets its definition of done.
2. English specification (section 7) passes the language readiness bar (section 8.6), for UK and US if both are IN.
3. All automated tests pass; screenshot review of every screen in every variant and theme is complete.
4. Accessibility checks pass (section 6.9).
5. No known open teaching issue in the uncertainty log.
6. Prototype findings are incorporated or explicitly declined by the owner.
7. Play release checklist (12.7) complete.
8. The app works fully offline (except a TTS voice the owner has accepted as network-based, if any) and requests no permissions beyond those accepted in 11.9.

---

## 14. Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Art production for many companions and themes | Release delayed for months | Measure real cost with one companion (P3) before fixing counts |
| "Everything at launch" scope grows | Never released | Feature lock at S4; changes after S4 need an explicit owner decision |
| Wrong English phrase taught (UK vs US mix) | Loss of trust; Teacher Approved rejection | Section 7 rules with sources; golden tests; variant setting |
| Theme hurts legibility | Learning harmed | Principle 1; P1; legibility tests |
| Permission creep (timer, purchases) | Breaks privacy promise; more review | Section 11.9 table; owner decision per permission |
| TTS voice missing or online-only | Pre-readers unsupported on some devices | Silent-first design; voice checks; clear adult info |
| Play policy changes | Release blocked | Re-check policies at S7 |
| Single developer workload | Burnout, quality drops | Stages with exit criteria; AI-assisted review; realistic timeline |
| Companion distracts from learning | Weaker learning | P3; "Just the clock"; calm presence |
| Niece's sideloaded app stops installing | Personal impact | Limited-distribution registration before global rollout |

---

## 15. Open decisions for the owner

1. App name (check Play Store and trademark conflicts before choosing).
2. Monetisation model (12.5).
3. UK and US both at launch, or one variant? (Proposal: both, since phrase differences are small and both are needed for English markets.)
4. Number of companions at launch (after P3).
5. Companion space: IN or OUT (after P3).
6. Game modes: confirm IN list (6.3), resolve INVESTIGATE items.
7. Play Together modes (6.4).
8. Everyday tools E1 to E3 (6.6).
9. Art approach (6.5.4).
10. Accept a non-exact regional TTS voice or hide listening? Accept network voices?
11. Profile limit (proposal 4).
12. Practice reveal behaviour: full hint ladder without passive reveal (proposal) or keep the original reveal after two mistakes.
13. Which countries to release in first (English-speaking markets for the English release).
14. Whether the repository is ever made open source (would enable free community translation platforms; not required).

---

## 16. Evidence status

### 16.1 Verified (sources in section 17)

- Families policy: target audience declaration, child-appropriate content, approved SDKs only, neutral age screen for mixed audiences, privacy policy required.
- Closed testing requirement: 12 testers for 14 days for personal accounts created after 13 November 2023 (third-party sources and Codex's check of Google's help page).
- Target API 36 required from 31 August 2026.
- Teacher Approved eligibility and exclusions (Google's pages).
- `AnalogClock` gained `setDial`, `setHourHand`, `setMinuteHand` (Icon) in API 31 and is supported for RemoteViews use.
- `Voice.isNetworkConnectionRequired` exists; `setLanguage` may fall back to a nearby locale.
- Existing code facts in section 3.2 (read on 2026-10-20).
- Competitors with talking-clock features exist (at least one Play listing verified by Codex; others from store listings).

### 16.2 Assumptions (to be verified or tested)

- Misconception list (9.3) reflects the most common errors: based on teaching experience reflected in the original app's distractors, not on a cited study.
- Story frames over shared task engines keep effort manageable.
- Mastery thresholds and journey mix percentages.
- Hand length ratio for legibility.
- UK school conventions for a.m./p.m. notation, digital separator, "at night" boundaries; US teaching of "quarter after/of/till".
- Developer verification dates (news reporting).
- Font coverage for non-Latin scripts.
- Any claim of uniqueness relative to competitors (no systematic competitor analysis has been done; one should be done before writing store text).

---

## 17. Sources

- Google Play Families policy: https://support.google.com/googleplay/android-developer/answer/9893335
- Google Play Families policy (related pages): https://support.google.com/googleplay/android-developer/answer/9867159 and https://support.google.com/googleplay/android-developer/answer/17122218
- Google Play testing requirements for new personal accounts: https://support.google.com/googleplay/android-developer/answer/14151465
- Third-party summaries of the closed-testing rule: https://www.testerscommunity.com/blog/google-play-closed-testing-requirements-2026 and https://www.choicely.com/blog/google-play-12-tester-rule
- Target API level requirements: https://developer.android.com/google/play/requirements/target-sdk
- Teacher Approved: https://support.google.com/googleplay/android-developer/answer/10075896 and https://play.google.com/console/about/programs/teacherapproved/
- Android developer verification reporting: https://www.androidauthority.com/android-developer-verification-rollout-sideloading-flow-3653395 and https://www.androidauthority.com/android-sideloading-changes-timeline-3679204/
- AnalogClock API 31 changes: https://developer.android.com/sdk/api_diff/31/changes/android.widget.AnalogClock and https://developer.android.com/reference/android/widget/AnalogClock
- TextToSpeech and Voice: https://developer.android.com/reference/android/speech/tts/TextToSpeech and https://developer.android.com/reference/android/speech/tts/Voice
- Glance (widgets in Compose style): https://developer.android.com/develop/ui/compose/glance
- England National Curriculum, mathematics programmes of study: https://www.gov.uk/government/publications/national-curriculum-in-england-mathematics-programmes-of-study
- Common Core State Standards, Measurement and Data: https://www.thecorestandards.org/Math/Content/1/MD/ , https://www.thecorestandards.org/Math/Content/2/MD/ , https://www.thecorestandards.org/Math/Content/3/MD/
- British Council LearnEnglish Kids, telling the time worksheet (cited by Codex): https://learnenglishkids.britishcouncil.org/sites/kids/files/attachment/how-to-tell-the-time-worksheet.pdf
- Goethe-Institut A2 word list (cited by Codex): https://www.goethe.de/pro/relaunch/prf/hr/Goethe-Zertifikat_A2_Wortliste.pdf
- Competitor listing with talking clock: https://play.google.com/store/apps/details?id=com.paid.thelearningapps.talkingclock
