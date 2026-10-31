# HubidubiCash

An app with one purpose: to help you save money. It shows where your money
went and whether you spent less than the week before, keeps your regular
income and bills in one table you tick off as you pay, and adds your rent and
subscriptions to your spending by itself. On **Android, macOS and Linux**.

No account, no cloud, no server of ours. The app has no internet permission
at all: your spending stays on the device you typed it on, and leaves it only
as a backup file you make, sealed with your password.

A sibling of [HubidubiTask](https://github.com/HubidubiPlanner/HubidubiTask),
in its look.

## What it does

| Tab | What is on it |
|---|---|
| Statistics | Where it opens: what went out in the last day, week, month or year, kind by kind, as bars or a circle, and how it compares with the time before |
| Spendings | Every spending in the dates you choose, by day, 50 at a time |
| Regular | Your regular income and spending; a regular spending is added to Spendings on its day each month |
| Bills | One month as a table: each regular income and bill on its day with a tick, the life costs still to come, and the total. When all is ticked, the next month shows |
| Settings | The period and chart for Statistics, life costs a month, currency, language, theme; Export and Import |

The + on each tab asks what kind of spending it was, then the amount. On
Android, home-screen widgets do the same from the home screen.

Website: <https://hubidubiplanner.github.io/hubidubicash>. How to use it:
[`docs/MANUAL.md`](docs/MANUAL.md).

## Build from source

The Gradle build is in `src/apps`. It needs JDK 17 and, for the phone app, an
Android SDK.

```sh
cd src/apps
./gradlew :shared:desktopTest :desktop:test   # tests
./gradlew :android:assembleDebug              # phone app
./gradlew :desktop:run                        # computer app
./gradlew :desktop:packageDmg                 # Mac app (on a Mac)
./gradlew :desktop:packageDeb                 # Linux package (on Linux)
```

More in [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Where things are

| Folder | What is in it |
|---|---|
| [`src/apps/`](src/apps) | The app: one Gradle build. `shared/` holds everything both apps use, the screens included; `android/` the phone app and its widgets; `desktop/` the macOS and Linux app. |
| [`site/`](site) | The website. |
| [`deploy/`](deploy) | Getting it out: the website now, the stores with the first release. |
| [`docs/`](docs) | The manual and the changelog. |

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Report security problems privately,
as [`SECURITY.md`](SECURITY.md) says.

## Licence

Copyright (C) 2026 HubidubiPlanner.

HubidubiCash is free software: you can redistribute it and modify it under
the terms of the GNU General Public License, version 3 or (at your option)
any later version. See [`LICENSE`](LICENSE). The IBM Plex Sans fonts it ships
are under the SIL Open Font License 1.1.
