# Clocklings Blender version

## Scope and authority

- This is the independent Blender artwork experiment. Work only in `C:\Dev\Clocklings-Blender-version`.
- `C:\Dev\Clocklings` is read-only source material. Preserve its files, Git state and application data.
- Launcher name: **Clocklings Blender**. Application ID: `fi.clocklings.blender`; release signing is unconfigured.
- `C:\Dev\Sateenkaarikello-Android` is read-only source material. Never change its files, Git state, settings, signing material, or installed application data.
- App name: **Clocklings**. Google Play title: **Clocklings: Learn to Tell Time**.
- Read `docs/implementation/STATUS.md` for current decisions and validation evidence, and `docs/superpowers/plans/2026-10-20-public-clock-learning-app.md` for the product proposal.
- The owner's instructions override the planning documents. Proposed features and quantities are not approved decisions.
- Release languages: English, Finnish, shared Swedish, German, French, Spain Spanish (`es-ES`) and Brazilian Portuguese (`pt-BR`; device `pt` and `pt-PT` also select Brazilian content). Complete all agreed features and all seven languages before publication. No MVP release, curriculum mapping, or Teacher Approved work.
- Ask about consequential unresolved product choices before implementing dependent behavior. Decide ordinary technical details independently.

## Implementation

- Reuse the copied Kotlin, Jetpack Compose, Navigation 3, manual AppServices, Preferences DataStore, and pure domain/test code.
- Preserve the existing toolchain unless an authorized change requires otherwise. Do not introduce unrelated dependencies or migrations.
- Use the smallest sufficient change and relevant validation. Preserve existing user work.
- Keep new identifiers, filenames, and code comments in English. Put UI copy in Android resources and keep clock mathematics separate from language-specific wording.
- Use existing centralized theme tokens. No decorative colored edge stripes or middle-dot separators.
- Inherited Finnish code, artwork, and reward behavior are a reuse baseline, not approval of the final public feature inventory.

## Validation and local safety

- Run Gradle only from this repository. Use `--no-daemon` for temporary validation.
- Never use the original application's signing properties or keys. Release signing remains unconfigured until separately authorized.
- Run `:app:testDebugUnitTest` and `:app:assembleDebug` for the initial independent application.
- For changed UI, include 320 dp width, 200% font scale, and their combination. Report actual device evidence separately from source inspection.
- Use an explicitly identified dedicated test emulator. Do not install, reset, or test on a physical device or an unrelated running emulator.
- The owner runs `lc` and `sc`. Do not claim those checks passed without their reports.
- Track and stop temporary processes started for this task. Never stop unrelated processes.
- Do not copy secrets, signing keys, local machine settings, build output, caches, private reports, or the original Git history.

## Git and reporting

- Work on a `codex/` feature branch.
- Do not commit, push, create a remote repository, or create/merge a pull request without a separate explicit request. The owner creates pull requests manually.
- Report concisely in Finnish: implemented work, checks actually run, and unresolved decisions or verification gaps.

## Offline security scans

- A successful coordinating-agent `sonar analyze secrets` check satisfies the secrets-before-read requirement for the exact unchanged files it covered. Provide the checked scope, Git revision, and successful result to offline workers; they may read those files without rerunning Sonar or accessing the system keychain.
- Files outside that successful check, or changed afterwards, still require a successful secrets check before reading. Never pass credentials to workers.
