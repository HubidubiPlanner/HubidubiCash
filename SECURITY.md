# Security

## Reporting a vulnerability

Report it privately, through GitHub's private vulnerability reporting:

<https://github.com/HubidubiPlanner/hubidubicash/security/advisories/new>

Never in a public issue, a pull request or a discussion. There is no email
address for reports; GitHub keeps the report visible only to you and the
maintainers until a fix is out.

Please include what is affected (backups, what is stored on a device, the
widgets), the version and the system (Android, macOS or Linux), and how to
reproduce it.

## Supported versions

Only the latest release gets fixes.

## What the design promises

- The app never goes online. The phone app's finished manifest has no
  permissions at all, and the build fails if one appears; the computer app
  opens no connections.
- Everything stays on the device it was typed on: on Android in the app's
  private storage, out of Android's cloud backup; on a computer in the app's
  folder in the user's account.
- A backup is sealed with the person's password: AES-256-GCM in 1 MiB
  chunks, each bound to the file's header and to its place, with a key from
  PBKDF2-HMAC-SHA256 over 600 000 rounds and a fresh salt. A file changed,
  reordered or cut short does not open, not even in part. Without the
  password nobody can open it, us included.
