#!/bin/sh
# The website as it will be published, in out/public.
#
#   sh deploy/website/2-site/assemble.sh
#
# Copies site/, puts in the address and contact link from site.env, and
# refuses to finish when a page links to a file that is not there.
set -eu
here=$(cd "$(dirname "$0")" && pwd)
repo=$(cd "$here/../../.." && pwd)
. "$here/site.env"
site_url=${SITE_URL%/}
public="$here/out/public"

rm -rf "$here/out"
mkdir -p "$public"
cp -R "$repo/site/." "$public/"
rm -f "$public/README.md"

python3 - "$public" "$site_url" "${CONTACT_URL%/}" <<'PY'
import glob, os, re, sys
public, site_url, contact_url = sys.argv[1:]
problems = []
for page in glob.glob(os.path.join(public, "*.html")):
    s = open(page, encoding="utf-8").read()
    s = s.replace("https://contact.hubidubicash.example", contact_url).replace("https://hubidubicash.example", site_url)
    open(page, "w", encoding="utf-8").write(s)
    name = os.path.basename(page)
    # Every local link and image must be there; a ?query is not part of the name.
    for ref in re.findall(r'(?:href|src|srcset)="([^"#:?]+)(?:\?[^"#]*)?(?:#[^"]*)?"', s):
        if ref and not os.path.exists(os.path.join(public, ref)):
            problems.append("%s links to %s, which is not there" % (name, ref))
for p in problems:
    print("!! " + p, file=sys.stderr)
sys.exit(1 if problems else 0)
PY

case "$site_url $CONTACT_URL" in
  *hubidubicash.example*) echo "!! site.env still has the placeholder address or contact; set them before publishing" >&2 ;;
esac
echo "==> $public ($(du -sh "$public" | cut -f1)), for $site_url"
echo "    preview: python3 -m http.server -d $public 8000"
