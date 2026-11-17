# Clocklings usage analytics

Implemented 15 November 2026. This is the Blender application's integration, not the original Clocklings or KnitTools integration.

## Installed services

- [Finnish dashboard](https://eu.posthog.com/project/351609/dashboard/1118246) in the separate Clocklings organization, project 351609, PostHog EU, free plan.
- Receiver: `https://clocklings-analytics.emmahota.workers.dev/v1/metrics`.
- Cloudflare Worker and D1 database: `clocklings-analytics`. D1 was created with `--jurisdiction eu`; the deployed database reports EU jurisdiction and EEUR region.
- The PostHog ingestion token is a Worker secret named `POSTHOG_PROJECT_TOKEN`. It is not in the Android app or repository. No personal API token is needed by the app.
- Ordinary release builds include the receiver origin. Debug and local measurement builds disable analytics by default. An explicit `-PanalyticsEndpoint=https://...` overrides the origin for a deliberate integration test; `-PanalyticsEndpoint=` disables it.

No server needs to run on the owner's computer. Cloudflare runs the receiver and scheduled export. No paid plan was enabled.

## What is measured

The app records only allowlisted counters after the parent-area sharing switch is enabled. It starts disabled. The existing parent puzzle protects the setting; it is not verified parental consent or identity verification.

| Area | Counters |
| --- | --- |
| Each practice level | Started, completed, reading/set-clock tasks shown, first-attempt success, success after a hint, solution shown |
| Each lesson | Started, completed, wrong/correct answers, replay |
| Minute challenge | Started, completed, correct/wrong answers, time expired |
| Navigation | Visits to Home, Lessons, Levels, Free clock, Play together, Wardrobe, Personalize, Settings, Parent, Challenge and Result |

Game indexes 0–4 mean whole hours, half hours, quarters, five-minute intervals and individual minutes. Lesson indexes 0–8 map to `viisarit`, `kierros`, `tasan`, `puoli`, `vartti`, `ylivaille`, `viisi`, `minuutit`, `vrk`. Exported `exercise`, `outcome` and `screen` properties provide fixed Finnish labels.

`read_hint` and `set_hint` mean a correct answer after an earlier error/hint. They do not count every displayed hint. Starts and completions describe activity, not matched individual funnels. Navigation counts actual transitions; opening/restoring the initial home destination is not counted as a visit. Exact target clock times and answer values are not uploaded.

## Data flow and limitations

1. Android accumulates integer counters for UTC Monday-to-Sunday weeks in a separate Preferences DataStore. Each counter is capped at 1,000 per local week. Existing manifest and extraction rules exclude the data from cloud backup and device transfer.
2. Android JobScheduler attempts a network-constrained upload daily. Android controls timing, so this is not a guaranteed daily delivery. Only completed weeks are sent; four completed weeks are retained for retry.
3. One random UUID identifies each weekly batch for retry deduplication. It is not reused in later weeks or sent to PostHog. Opt-out cancels active upload, cancels the job and clears queued local data. Counts already accepted by the server cannot be individually withdrawn from a combined total.
4. The receiver accepts only the fixed metric names, positive integers up to 1,000, valid closed weeks, and a maximum 48 KiB JSON body. It atomically deduplicates receipts and adds counters. It stores only receipt ID/week and combined metric totals, not individual batch contents.
5. The hourly cron finalizes a week after its four-week late-upload window closes. Each metric is exported only when at least ten positive batches contributed. Below-threshold cells and expired receipts are deleted. This is a threshold on batches, not verified distinct children or people.
6. PostHog receives `clocklings_weekly_metric` with week, metric key, scope, fixed display labels, total and contributor count. Its timestamp is the UTC Monday at the beginning of the measured week. All rows share one service identity; person processing and geolocation are disabled. A deterministic event UUID makes retries deduplicable. Accepted exports are removed from D1. Provider-managed backups and service/security logs have their own retention.

Expect the first reportable week about four weeks after that week ends, subject to enough contributors and delivery. The dashboard is empty until then. Missing rows are unavailable or suppressed, not zero. Do not calculate exact success/abandonment rates by subtracting independently suppressed cells. Counts are opt-in, capped and can be lost on uninstall, storage failure, long offline periods or force-stop. The public anonymous receiver cannot authenticate unique installations and can receive fabricated valid reports.

No name, email, profile name, age, location, advertising ID, Android ID, stable device ID, session recording, screenshot, free text, individual event timeline or automatic crash contents is collected by this integration. This deliberately rules out individual retention, user journeys, user counts and session replays. It is broad activity measurement, not every PostHog feature.

Cloudflare necessarily processes the connection IP and normal network metadata. The Worker does not copy them into D1 or forward them to PostHog; Worker observability, request logs and traces are disabled in configuration. This does **not** establish that Cloudflare has no provider/security logs, nor that the complete pipeline never processes personal data. The seven-language app disclosure explicitly names the IP processing and the two providers.

## Publication information

The changed data flow must be reflected in the app's published privacy notice and Play Console declarations. The implementation supplies optional app-interaction analytics and network transfer, so do not declare simply that no data ever leaves the device. Whether any category qualifies as ephemeral processing or a service-provider sharing exception depends on the actual provider arrangement. The numeric parent gate is not a legal consent certification.

Authoritative publishing references: [Google Play Data safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) and [Google Play Families policy](https://support.google.com/googleplay/android-developer/answer/9893335?hl=en-gb). This work does not certify legal compliance or submit Play Console forms. Release signing remains unconfigured.

## Maintenance

Run `node --test analytics-worker/test/index.test.mjs` for receiver/export tests. Use Wrangler with `--config analytics-worker/wrangler.jsonc` for deployment and `d1 migrations apply clocklings-analytics --remote` for migrations. Preserve the existing secret when deploying. The endpoint's `GET /health` returns 204 but only verifies receiver availability, not scheduled PostHog delivery.

`UsageUploadDeviceTest` is an opt-in live transport check. Pass instrumentation argument `clocklingsAnalyticsSmoke=true` on a dedicated emulator. It sends the known synthetic receipt `8ab1565c-787a-427b-bd12-9b5bd06c8368` twice with `game.99.start=1` for the previous UTC week. Verify a single contribution in D1 and remove only those exact fixture rows afterward. Never enable this test against player data casually.

## Verification

Synthetic PostHog events are labeled `clocklings_pipeline_verification` or metric `verification.synthetic_pipeline` and are excluded by every production dashboard query. Two synthetic events remain in PostHog as delivery evidence; the receiver's test rows and receipts were removed and their absence was checked.

- Final receiver suite: 4 Node/SQLite tests passed, including input bounds, transaction rollback, duplicate delivery, suppression, redirects, provider errors, quota responses, stable export UUIDs and fixed labels.
- Live HTTPS receiver: both duplicate requests returned 204 and D1 held one contribution.
- Actual Cloudflare cron exported the synthetic cell with total 123 and 10 contributors. PostHog readback confirmed those values, the shared service identity, person processing disabled, geolocation disabled and no `$ip`. The live run exposed and fixed Cloudflare's unsupported `redirect: error`; export now uses `manual` and rejects redirects. The normal hourly cron was restored and deployed.
- `UsageUploadDeviceTest` passed on the dedicated `ClocklingsReview20261030` emulator at `emulator-5582`: the app's real HTTPS transport accepted both sends, and remote D1 confirmed total 1, contributors 1 and one receipt. The emulator was stopped and its two processes were confirmed absent.
- The initial focused JVM run passed 60 tests. Parent controls were checked in all seven languages at 360/100%, 320/100%, 360/200% and 320/200%. These are Robolectric UI checks, not rendered-device or human TalkBack checks.
- Resource parity passed for all seven languages. Generated BuildConfig values confirm the production receiver for release and an empty endpoint for ordinary debug.
- All eight saved dashboard queries executed without errors and returned no real-player data, as expected before publication. The dashboard was also opened and verified in the browser. Computer-use state was reset; no task-owned capture helper remained.

The final Gradle run succeeded: `:app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:ktlintCheck :app:detekt :app:lintDebug` with `--no-daemon --continue`. JUnit XML confirms 528 tests in 105 suites, zero failures, errors or skipped tests. The final minified unsigned release APK is `app/build/outputs/apk/release/app-release-unsigned.apk`; it is not a signed Play publication artifact. The standard debug APK is `app/build/outputs/apk/debug/app-debug.apk` and has reporting disabled by default.

All identified task-owned Gradle/test, emulator, tail and diagnostic processes were confirmed stopped. Tool policy rejected removal of the task-created `.wrangler/` and `analytics-worker/.wrangler/` CLI caches. Their paths and contents were checked to stay inside the workspace with no reparse points; they are ignored by Git and remain available for bounded manual deletion.

Physical-device testing, human TalkBack/native-speaker review, actual Play publication and the owner-run `lc`/`sc` were not performed. No commit or push was made.
