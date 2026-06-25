# TestFlight Readiness

Date updated: 2026-06-25

## Current status

The owner confirmed that Codemagic `ios-simulator-unsigned` passed for the
Phase 12 release-readiness foundation on 2026-06-25. The repository now
contains a manual Codemagic `ios-testflight-release` workflow that can build a
signed iOS IPA and upload it to App Store Connect after the owner completes the
secure Apple and Codemagic setup below.

This does not approve a production App Store release. TestFlight is the next
required release-validation gate before any production approval.

## Repository controls

- The workflow is manual-only: `triggering.events` is empty.
- The workflow fails if `BETTAMIND_IOS_BUNDLE_ID` is missing or still uses the
  `dev.bettamind.placeholder` identifier.
- App Store signing files are fetched through Codemagic `ios_signing` with
  `distribution_type: app_store`.
- The IPA is uploaded through the Codemagic App Store Connect integration named
  `bettamind-app-store-connect`.
- `submit_to_testflight` is false, so the workflow uploads the build but does
  not automatically submit it for external beta review.
- `submit_to_app_store` is false.
- The checked-in iOS `Info.plist` uses Xcode version build settings, and the
  workflow stamps concrete TestFlight metadata from Codemagic `BUILD_NUMBER`;
  marketing version defaults to `0.1.0` and can be overridden with
  `BETTAMIND_IOS_MARKETING_VERSION`.

## Owner setup required

Complete these outside Git. Do not commit signing files, certificates,
provisioning profiles, API keys, `.p8` files or exported archives.

1. Enroll or confirm Apple Developer Program membership for the publishing
   entity.
2. Create the final owner-controlled iOS bundle identifier in Apple Developer
   and App Store Connect.
3. Create the App Store Connect app record for Bettamind before automated
   publishing.
4. In Codemagic Team integrations, add the Apple Developer Portal integration
   using a dedicated App Store Connect API key named
   `bettamind-app-store-connect`.
5. In Codemagic code signing identities, provide or fetch the Apple
   Distribution certificate and App Store provisioning profile for the final
   bundle identifier.
6. Create a Codemagic variable group named `bettamind-testflight` with:
   - `BETTAMIND_IOS_BUNDLE_ID`: final App Store bundle identifier.
   - `BETTAMIND_IOS_MARKETING_VERSION`: optional, for example `0.1.0`.
7. Start `ios-testflight-release` manually against the pushed release-candidate
   commit.
8. After upload processing completes in App Store Connect, add internal testers
   first and run the TestFlight smoke checklist below.

## TestFlight smoke checklist

- Install the build from TestFlight on at least one owner-controlled physical
  iPhone.
- Confirm first launch does not require an account.
- Confirm core offline paths open and remain useful without backend or AI.
- Confirm privacy-lock and encrypted local-storage paths still work.
- Confirm safety-support flows do not claim help was contacted unless the user
  completes the action.
- Confirm reminders and notification previews are neutral and optional.
- Confirm no placeholder bundle ID, placeholder app name or placeholder support
  URL appears in the installed build or App Store Connect metadata.
- Record build number, device model, iOS version, tester, smoke result and
  blockers in the release evidence log before widening testing.

## Reference

- Codemagic iOS signing:
  https://docs.codemagic.io/yaml-code-signing/signing-ios/
- Codemagic App Store Connect publishing:
  https://docs.codemagic.io/yaml-publishing/app-store-connect/
- Apple TestFlight overview:
  https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview
