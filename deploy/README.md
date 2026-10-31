# Getting it out

One folder per destination; inside, numbered folders for the steps in order.

| Folder | What |
|---|---|
| [`website/`](website) | The website on GitHub Pages: `2-site/assemble.sh` builds it from `site/`, and `.github/workflows/pages.yml` publishes it on every push to `main` that touches it. |

Google Play, F-Droid and signed GitHub releases come with the first release,
following HubidubiTask's `deploy/` steps.
