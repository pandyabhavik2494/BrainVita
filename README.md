# BrainVita

The classic 33-hole peg solitaire, built for the Galaxy Z Fold 8 (Android 11 and later). Jump pegs
over each other to remove them; finish with one peg in the centre.

- One game at a time. Close the app and come back: the game carries on where you left it, until you
  choose Restart.
- Lays itself out for both screens: a single column on the cover screen, board beside the controls on
  the inner screen.
- Undo any move. No accounts, no network, no ads, nothing leaves the phone.

## Layout

- `core/`: the rules, in plain Kotlin with unit tests: the board, legal jumps, win and stuck
  detection, undo, and the saved-game format.
- `app/`: the Android app: Compose UI (cover and inner screen layouts), the board drawn on a canvas,
  DataStore for the saved game.
- `app/src/androidTest/`: a screen test that plays a jump, undoes it, and checks the game resumes
  after the app is closed, saving screenshots as it goes.
- `.github/workflows/android.yml` and `ci/`: the build (below).

## How a build happens

Every push to `main` (or a manual run of the workflow):

1. Runs the unit tests and builds the app.
2. Signs the APK with the release key and publishes it as a GitHub release, `build-N`, with
   `BrainVita.apk` attached.
3. Starts two emulators at the Fold's cover-screen size (1080×2520, large text) and inner-screen size
   (1968×2184), installs the APK, opens it, runs the screen test, and publishes the screenshots as
   `build-N-cover-screen` and `build-N-inner-screen` releases.

## Install on the phone

1. On the phone, open the repository's Releases page and download `BrainVita.apk` from the latest
   `build-N` release.
2. Open the download; allow installs from your browser if Android asks.
3. Every build is signed with the same release key, so a new build installs straight over the old one
   and keeps the saved game.

## The signing key

The release key is not in the repository. It is stored as GitHub Actions secrets
(`BRAINVITA_KEYSTORE_B64`, `BRAINVITA_KEYSTORE_PASSWORD`, `BRAINVITA_KEY_ALIAS`,
`BRAINVITA_KEY_PASSWORD`) under Settings → Secrets and variables → Actions, and the owner keeps a
backup of the keystore. If the secrets are missing, the workflow warns and signs with a throwaway
debug key; such a build has to be uninstalled before a release-signed build will install.

## Build it yourself

Open the folder in Android Studio and run the `app` configuration. Local builds sign with the debug
key. To run the unit tests from a terminal: `./gradlew :core:test`.
