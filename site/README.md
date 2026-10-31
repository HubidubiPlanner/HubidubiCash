# HubidubiCash website

Plain static HTML and CSS: no build step, no framework, no third-party
requests. The look is HubidubiTask's site, stylesheet and fonts as they are.
Publish it with [`deploy/website`](../deploy/website).

```
index.html      Home: saving money first, then what the app does, taps, privacy
download.html   Android, Mac and Linux; building from source
privacy.html    The privacy policy (the URL Google Play asks for)
style.css       HubidubiTask's Ivory theme (light and dark), IBM Plex Sans only
fonts/          The app's own fonts (OFL, see fonts/LICENSE.txt)
assets/         Icon, link preview image, and screens/: the app, light and dark
```

Preview: `python3 -m http.server -d site 8000`, then http://localhost:8000.

The pages follow the system's light or dark setting; screenshots switch with
it. `https://hubidubicash.example` and `https://contact.hubidubicash.example`
are placeholders that `deploy/website/2-site/assemble.sh` replaces with
`SITE_URL` and `CONTACT_URL` from its `site.env`.

When the app changes, retake the screenshots: in `src/apps`, run
`REVIEW_SHOTS=<folder> ./gradlew :shared:desktopTest --tests '*ScreensTest*'`,
then convert `site-*.png` to `assets/screens/*.jpg`.
