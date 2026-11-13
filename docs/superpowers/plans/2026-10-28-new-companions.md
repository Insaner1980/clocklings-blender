# New Companions Implementation Plan

**Goal:** Add the bunny, the car and the unicorn as companions with their own wardrobes, in the existing vinyl style.

**Spec:** `docs/superpowers/specs/2026-10-28-new-companions-design.md`

## Constraints

- Work only in `C:\Dev\Clocklings`. No commits, pushes or pull requests without a separate request.
- No new dependencies or raster artwork. Colors only from `PersonalizationColors` and `ArtTones`. Never allocate a `Brush`, `Path` or `Stroke` inside a draw call.
- UI copy in English, Finnish and Swedish resources. No `·`, `•` or ` | ` separators.
- Gradle only from the repository root with `--no-daemon`. Device checks only on `emulator-5590`.

## Tasks

### Task 1: Per-kind slots and new kinds

- `Slot`: add `ROOF`, `FRONT`, `WHEELS`, `MODEL` (all optional). `CompanionKind.slots`: car gets these four.
- `CompanionKind`: add `BUNNY` and `CAR` before `NONE` (unicorn in Task 4).
- `AnimalRig`: crops become a map from slot to rect; the existing head/neck constructor stays.
- `ArtPart.WHEELS` for hubcap items.
- Wire every exhaustive `when` over kinds and slots: `rigFor`, `itemArtOf`, `companionName`, `companionVoice`, `slotName`, `PersonalizationColors.companion`.
- Strings: companion and slot names in en, fi, sv.
- Tests: `CompanionItemsTest` covers the new kinds and the car's slots.

### Task 2: Car (pilot)

- `CarRig.kt` and `CarItems.kt` with all 16 items; catalog in `CompanionItems.kt`; item names in three languages.
- Coverage test and gallery helper.

### Task 3: Bunny (pilot)

- `BunnyRig.kt` and `BunnyItems.kt` with all 16 items; catalog; item names in three languages.
- Coverage test and gallery helper.

**Checkpoint:** unit tests and debug build pass, rendered previews and emulator screenshots of both companions and their wardrobes go to the owner. Stop for approval.

### Task 4: Unicorn

- Catalog approved with the owner, `UnicornRig.kt`, `UnicornItems.kt`, names, tests.

### Task 5: Finish

- Screenshots at 320 dp, 200% font and both; `STATUS.md`; stability baselines; memory update.
