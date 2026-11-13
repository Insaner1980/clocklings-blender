# Clocklings monetization

Implemented locally on 8 November 2026. This is not a release-readiness or policy-certification report. Production purchases remain unavailable until the correct public Play licensing key and product configuration are supplied.

## Approved model

One non-consumable **Clocklings Full Unlock** purchase permanently grants full access. The single product ID is centralized in `FullUnlockProduct.ID`: `clocklings_full_unlock`; no prior approved ID was found. The owner confirmed **EUR 6.99** on 8 November 2026; it has not been configured in Play Console. Country availability and regional prices still require review. The app displays the localized Play price. No subscriptions, ads, consumables, paid stars, separate character purchases, analytics, accounts or backend were added.

| Existing content | Access |
| --- | --- |
| Fundamentals and `viisarit`, `kierros`, `tasan` | Free; verified as exactly `Lessons.levelLessons[0]`. |
| Level index 0 practice | Free, repeatable whole-hour reading and setting. |
| Levels 1–4, their lessons and practice; minute challenge | Full version (indices are zero-based). |
| Explore / Free Clock, Play Together, optional `vrk` lesson, all clock appearances | Free. |
| Human and cat companions, no-companion mode | Free. |
| Other companions | Full version **and three total earned stars**. All become selectable together; three stars can be earned in the free level. |
| Human/cat items below | Free with existing earned-star requirements. |
| Remaining outfits/rewards | Full-version eligibility with existing earning requirements. |
| Language, accessibility, sound preferences and progress saving | Both versions. |

| Earned stars | Human free items | Cat free items |
| --- | --- | --- |
| 0 | `hair-letit`, `dress-perus`, `shoes-tossut` | `cat-neck-scarf` |
| 1 | `dress-pilkku` | `cat-head-bow` |
| 2 | `head-rusetti` | `cat-extra-yarn` |
| 3 | `shoes-punaiset` | `cat-neck-bell-collar` |

Purchase/restoration never awards stars, completes lessons or grants unearned outfits. No timers, quotas or repetition limits were introduced. Existing 0–15-star outfit requirements remain intact.

## Access and saved data

`AccessPolicy` is shared by navigation, lesson queues, starts and companion/item selection actions. Restored restricted destinations fall back to the learning/practice list. Automatic queues retain accessible lessons; next-level starts and replay are checked again. An active lesson is not replaced by a purchase screen when ownership changes. Results never trigger purchasing and show only rewards eligible under the current policy.

Progress, names, outfits and preferences are preserved. A restricted saved companion displays as the human; unavailable outfit items use the existing default/empty slot. These display-only fallbacks never overwrite saved choices merely because access changes, including opening appearance or changing clock preferences. Restoration makes the saved eligible choice visible again. No-companion mode remains available. An intentional new selection replaces its saved choice normally.

Separate Preferences DataStore `clocklings_purchase.preferences_pb` holds signed purchase JSON, signature and acknowledgement result. Learning reset never writes this file. Cached evidence is cryptographically revalidated; an ownership boolean alone is insufficient. Initial ownership loading finishes before restored navigation is evaluated. StateFlow updates access/UI without restarting.

## Local verification and recovery

The owner explicitly chose local signature verification and accepted its weaker tamper resistance and refund/acknowledgement limits. Google recommends server verification; this implementation is **not server verification**.

`LocalPurchaseVerifier` verifies Play's RSA PKCS#1 v1.5/SHA-1 purchase-data signature with this application's public Play licensing key, then requires the expected package, sole expected product, nonempty matching token, quantity one and explicit completed signed wire state (`purchaseState = 0`). Pending, cancelled, malformed, altered, wrong-app/product/key and inconsistent callback records cannot grant or acknowledge ownership. The raw signed state is checked because Billing's convenience accessor is permissive for missing/other states.

`BillingGateway` is the replaceable transport boundary. `PlayBillingGateway` handles queries, callbacks, errors, reconnection, launch and acknowledgement, never consumption. It selects one standard permanent Buy option and rejects rental, preorder, limited-quantity and promotional offers. Missing/ambiguous details or price disable purchase. Billing **9.1.0**, minimum API compatibility and guidance were checked against official documentation/Google Maven on 8 November. Unrelated dependencies/SDKs are unchanged; strict dependency verification stays enabled with necessary verified checksum metadata.

- Launch grants nothing. A completed callback must verify, then triggers a new ownership query; an old signed callback alone cannot restore revoked access.
- A successful query's completed expected purchase must verify before durable storage/access. Pending grants nothing; updates, foreground entry and Restore purchases recheck it. Cancellation preserves earlier ownership. Already-owned responses trigger restoration.
- Verified evidence is durably stored **before** acknowledgement. Acknowledgement is idempotent by token and its result is persisted. Failure leaves verified access and a retry status. Duplicate callbacks/queries never grant rewards or repeat recorded acknowledgement.
- Network, connection, product and query failures preserve verified ownership. Invalid/ambiguous expected-product records are failures, not confirmed loss. A successful ownership query without a completed expected purchase removes the entitlement, including an account that no longer owns it. Learning data is untouched.
- Transient failures retry after 2, 10 and 60 seconds while foregrounded. Foreground entry, Retry and Restore start another attempt. Backgrounding closes the connection. Relaunch revalidates the stored receipt and queries Play to recover unfinished acknowledgement. No background service or recurring entitlement expiry is added.

**Limits:** a signature proves receipt origin, not that it has never been refunded. Refund/revocation/account changes are detected only when Play successfully reports its ownership view, which Play caching may delay. There is no independent account identity check, real-time developer notification or server API query. Offline verified access has no time limit and can remain until successful reconciliation. A modified/rooted client can bypass local code/storage. Failed local writes are retried; if revoked evidence cannot be cleared before process death, the old cache can be usable offline after restart. Failed cache reads cannot establish ownership and are retried without deleting the file.

Acknowledgement recovery requires the app to run/connect within Google's acknowledgement window (normally three days after PURCHASED, shorter for license tests), otherwise Play can refund/revoke. The cache is not proof against that outcome. A crash after Play acknowledges but before local persistence is recovered by querying Play; known acknowledgement avoids repeated calls.

## Parent area

Product information, purchase and Restore purchases sit behind an accessible reversed-digit adult gate reached intentionally from settings/content/appearance. It is a usability boundary, not legal age verification, and uses no clock exercise. Editable-field semantics, keyboard/IME submission, scalable text and cancellation are supported; restored parent destinations require the gate again. No lesson/result purchase screens, repeated modal prompts, urgency or countdowns were added.

Resources cover English, Finnish, shared Swedish, German, French, Spain Spanish and Brazilian Portuguese. Copy explains one-time access, separately earned rewards and that restoring purchases does not recover learning data stored only on another installation.

## Permissions and data flows

The merged manifest adds `com.android.vending.BILLING`, `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` through Billing/transitive dependencies. No location permission or application analytics pipeline was added. New code exchanges product information, purchase records/tokens/signatures and acknowledgements with Google Play. It sends no learning progress, names, outfits or preferences to a new service. Billing's Play services/data-transport dependencies are not a separately introduced analytics product.

Receipts/tokens stay in private local DataStore and are not logged. Existing disabled backup/extraction rules remain unchanged. Reinstall restoration queries the relevant Play account; local learning-data restoration is not promised. The licensing key is public and may be packaged; service-account credentials, private keys and server secrets must never be packaged. Ephemeral signing keys/fake entitlements exist only in tests/test APK; production has no bypass.

## Exact remaining owner setup and live tests

**Deferred by the owner in the later local-verification task on 8 November:** the following is a future prerequisite record, not the current next action. Do not create the Play application, save the prepared form, accept declarations, configure products or request permission for these steps during local verification. EUR 6.99 remains unchanged.

1. Obtain the **public licensing RSA key for package `fi.clocklings`** from that application's Play Console monetization/licensing setup (Base64 public key). Check the application identity; this is not an upload/app-signing certificate or the original private app's key. Replace `UNCONFIGURED` in `app/src/main/res/values/billing.xml` (`play_billing_public_key`). The deliberately invalid placeholder prevents purchase launch/verification; production has no substitute test key.
2. Create one **one-time product** with ID `clocklings_full_unlock` and name **Clocklings Full Unlock**. Configure one standard permanent **Buy** option, seven product translations and intended country availability. No rentals, preorder/discount offers, consumables or additional products.
3. Apply the owner-confirmed **EUR 6.99** price only after separate authorization to change Console settings. Review country availability and localized prices/taxes before activating the product/Buy option for testing. The app must never use a fallback price.
4. Establish the package identity in Play Console. Google's Billing test guide permits sideloaded debug builds for license testers with a matching package; do not require a new build upload solely for this test path. If the actual Console setup requires an uploaded artifact, explain the specific blocker and obtain separate authorization before uploading or configuring signing. The owner explicitly authorized scoped tests on their identified Pixel 9, after configuration prerequisites; a dedicated Play-capable emulator may be used for destructive scenarios. The earlier API 26 Google APIs emulator is not a licensed Play purchase environment.
5. Check **Manage account → Play apps settings → Monetization settings → License testing**. Google's current help says the publishing account is automatically a license tester; add another account only if needed and separately authorized. Keep sign-in under the owner's control and verify the actual purchase-dialog account and explicit test payment method. If using a test track, also grant access and complete opt-in. Track membership alone does not make transactions free.
6. Confirm that Play identifies a **test purchase/test payment method** before transacting. Exercise approved, declined/cancelled, pending→approved/cancelled, duplicate/already-owned, reinstall restore, interrupted acknowledgement/restart, offline access, query failure, refund/revoke and account-change cases. Record ownership restoration separately from local learning data.

The correct key, active product/option, configured regional prices/availability and verified license-test environment are missing/unverified and block live Play evidence. No real-money purchase, saved Console change, backend, paid service, upload, signing configuration or publication occurred.

## Live setup inspection, 8 November 2026

Read-only inspection used branch `codex/full-unlock`, base revision `b09e3be612d3e9f65a470e946f50c666d45de0d2`, preserving all existing uncommitted work. Sonar secrets checks passed before reading the inspected project instructions, status, configuration and Billing source. This continuation changes documentation only; no application repair or new automated/device test run was needed.

| Prerequisite | Observed status |
| --- | --- |
| Play application | Finnvek's app list contained only KnitTools. The owner also confirmed that Clocklings has not been created. |
| Package identity | The unsaved Create app form reported **Package name available** for `fi.clocklings`. This does not reserve the name or establish a Play application. |
| Public licensing key | `app/src/main/res/values/billing.xml` still contains `UNCONFIGURED`. No Clocklings key is available before its Console application exists. The existing verifier/configuration gate remains unchanged. |
| Product and Buy option | Not created for Clocklings. Intended single product: `clocklings_full_unlock`, standard permanent Buy, no additional offers or quantities. |
| Price and availability | EUR 6.99 approved by the owner in this continuation; no Console price applied. Countries, converted regional prices and product translations remain unconfigured. Required product languages: English, Finnish, shared Swedish, German, French, Spain Spanish and Brazilian Portuguese. |
| License testing | The current Console page showed Email lists selected, no configured email list, and `RESPOND_NORMALLY`. The publishing-account exception is documented by Google, but the Pixel 9's actual billing account and test instruments have not been inspected. |

**Historical prepared action, not authorized or submitted, now deferred:** create a draft application on Finnvek with name **Clocklings: Learn to Tell Time**, package `fi.clocklings`, default language **English (United States), en-US**, type **App**, and **Free** download. These values were entered in the unsaved form during the earlier inspection. Neither Developer Program Policies nor US export-law declarations were accepted; **Create app was not pressed**. No app, product, licensing key, signing setup or release was created. The form was left open at that earlier checkpoint; its current browser state was not revisited in the local-verification task. The historical screenshot is retained at `reports/full-unlock/play-console-app-draft.png`.

**Next local validation after the correct public key is available:** integrate only the intended resource, validate the RSA public-key encoding, run the existing `LocalPurchaseVerifierTest` and `LocalPurchasesTest` plus `:app:assembleDebug --no-daemon`. Before an authorized data-preserving Pixel installation, verify the device model/serial, APK package/version/hash/signing certificate and compatibility with the installed package. Stop if uninstall, data clearing or incompatible-signature replacement would be required. No UI change is planned; the existing 320 dp, 200% and combined emulator evidence remains historical evidence, not a new Pixel result.

**Live scenarios: all NOT_RUN in this continuation.** Cancellation, declined payment and pending cancellation should precede the first successful non-consumable purchase. Pending approval and immediate access, restoration, already-owned handling, offline verified ownership and transient-query recovery await the prerequisites. A second successful purchase may require a separately authorized refund/revocation. Duplicate callbacks and interrupted acknowledgement must remain simulated evidence unless safely reproduced against Play; no fault-injection bypass will be added merely to claim live coverage. Pixel 9 tests, actual Play test transactions, refund/revocation and destructive emulator restoration have not run. No real-money payment is authorized.

**Cleanup recheck:** `C:\Dev\Clocklings\.scratch\full-unlock` is absent. The workspace and `.scratch` parent are ordinary directories without reparse points. No cleanup command was run or earlier rejection bypassed; removal by this continuation is not claimed. The existing debug APK and `reports/full-unlock` evidence still exist. No Gradle, emulator, server or other temporary background process was started.

Official instructions rechecked: [Billing license tests and sideloading](https://developer.android.com/google/play/billing/test), [license-test configuration](https://support.google.com/googleplay/android-developer/answer/6062777?hl=en), [public licensing key](https://support.google.com/googleplay/android-developer/answer/186113?hl=en), and [one-time products and Buy options](https://support.google.com/googleplay/android-developer/answer/16430488?hl=en). The generic Console help also describes a published test-track path; this does not make track membership equivalent to license testing or establish that a new debug build must be uploaded. Actual Console/package/product prerequisites must be checked before choosing distribution. This inspection does not establish publication readiness.

## Validation

The later [focused Pixel and optimized-build verification](CONSOLIDATED-STATUS.md#focused-pixel-and-optimized-build-verification-8-november) used the unchanged normal application with `UNCONFIGURED` Billing. Debug and minified/resource-shrunk release builds passed sequential `--no-daemon` invocations; the 456-test campaign and API 26 matrix were not repeated.

The owner-authorized Pixel 9 update passed package/version/certificate compatibility checks and preserved the learning file across installation. All three free lessons and a ten-task practice replay passed, along with free companion/outfit selection, Explore, Together, real Gboard adult-gate rejection/cancellation/success, unavailable Purchase/Restore/Retry, background/resume and a cold relaunch retaining completions/stars. The Pixel already had three stars. A dedicated API 36 emulator running the separately debug-key-signed optimized APK supplied fresh zero-to-three-star reward evidence, a completed lesson/practice journey and parent/unavailable-purchase runtime evidence. That artifact was not installed on the Pixel, and production signing remains unconfigured. The report records emulator startup/System UI limitations separately from application behavior.

The no-companion message's misleading promise of all lessons/games was subsequently corrected in all seven languages on 8 November. It now describes practising without a companion, with no purchase prompt or change to the approved free/full boundaries or EUR 6.99 price. See the [scoped correction and validation](CONSOLIDATED-STATUS.md#no-companion-wording-correction-8-november). Read-only checks confirmed the owner-deleted `.scratch/focused-verification` directory and `app-release-local-test.apk.idsig` sidecar are absent; the retained test-signed release APK and required verification evidence are unchanged. The earlier cleanup policy rejection remains historical evidence, not outstanding cleanup.

TalkBack listening remains NOT_RUN / DEFERRED by the owner, with no new listening evidence; automated UI/IME observations are not listening evidence. Real Play purchase testing, all Console changes, signing setup, uploads and publication remain deferred. No Pixel or emulator testing was performed for the wording correction.

The earlier 409-test/API 26 baseline and intermediate 424-test access-only result are historical. Final checks are recorded in [CONSOLIDATED-STATUS.md](CONSOLIDATED-STATUS.md#monetization-implementation-and-verification-8-november). Fake-Billing tests, device crypto/DataStore/UI checks and live Play transactions are separate evidence. No live transaction or publication/policy certification is claimed. Automated semantics/IME checks are not a human TalkBack listening review.

## Official references checked

- [Billing releases](https://developer.android.com/google/play/billing/release-notes) and [supported versions](https://developer.android.com/google/play/billing/deprecation-faq)
- [Integration and lifecycle](https://developer.android.com/google/play/billing/integrate)
- [Verification/acknowledgement guidance](https://developer.android.com/google/play/billing/security)
- [Purchase API](https://developer.android.com/reference/com/android/billingclient/api/Purchase)
- [Public licensing key](https://support.google.com/googleplay/android-developer/answer/186113?hl=en)
- [One-time product options](https://developer.android.com/google/play/billing/one-time-product-multi-purchase-options-offers)
- [License testers and testing](https://developer.android.com/google/play/billing/test)
