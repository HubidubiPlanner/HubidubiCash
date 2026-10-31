# Contributing

HubidubiCash is one Gradle build in `src/apps`: `shared/` everything both
apps use (the model, the money maths, the database, the translations and
every screen), `android/` the phone app and its widgets, `desktop/` the macOS
and Linux app.

## Build and test

You need JDK 17 and an Android SDK (`ANDROID_HOME`, or `sdk.dir` in
`src/apps/local.properties`).

```sh
cd src/apps
./gradlew :shared:desktopTest :desktop:test :android:assembleDebug :android:checkDebugPermissions
```

This is what to run before you open a pull request; CI runs it on every
push. Without an Android SDK, `-Phubidubicash.desktopOnly=true` leaves the
phone app out. The computer app starts with `./gradlew :desktop:run`.

`REVIEW_SHOTS=<folder>` with the `ScreensTest` saves every tab as a picture:
the website's screenshots come from there.

## Sign off your commits

Every commit in a pull request needs a `Signed-off-by:` line with your name
(or the name you publish under) and an email:

```sh
git commit -s
```

By signing off you certify the Developer Certificate of Origin
(<https://developercertificate.org>): you wrote the change, or have the right
to submit it under the project's licence.

## House rules

- **The app never goes online.** No network permission, no library that
  phones home. `:android:checkDebugPermissions` (and `check`) fails the build
  if the finished manifest asks for one.
- **Every text in the UI goes through `t("…")`**, with its Polish, German,
  French and Spanish translations in
  `src/apps/shared/src/commonMain/kotlin/com/hubidubicash/core/i18n/`
  (`src/apps/tools/i18n_add.py` puts new ones in their place). `I18nTest`
  checks that they are complete, and `ScreensTest` that every tab's label
  fits five across a 360dp phone in every language.
- **No hint or helper text in the UI.** No placeholder explanations, no lines
  that tell people how to use a screen.
- **The look is HubidubiTask's.** Its six themes, IBM Plex Sans, square
  corners, borders instead of shadows, the + at the top right. The screens
  are written once, in `shared`, for the phone and the computer alike.
- **Amounts are whole numbers of hundredths** (`Long`), never floating point.
- **Logic is plain Kotlin that takes `today` as a parameter**, so the tests
  can say which day it is.
- **Kind colours** are checked for colour-blind readers around the whole
  circle and at 3:1 on every theme's cards (`KindColorsTest`). Text never
  wears them.
- **Backups stay sealed.** `Sealed.kt` is HubidubiTask's, unchanged:
  AES-256-GCM, PBKDF2-HMAC-SHA256 with 600 000 rounds, a password of at least
  12 characters.

## Pull requests

- One topic per pull request, with tests for what changes.
- Bugs and ideas go to the issues:
  <https://github.com/HubidubiPlanner/hubidubicash/issues>.
- Security problems never go to a public issue; see [SECURITY.md](SECURITY.md).

## Licence

By contributing you agree that your contribution is licensed under the GNU
General Public License, version 3 or later, as the rest of HubidubiCash is.
