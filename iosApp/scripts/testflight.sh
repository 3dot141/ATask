#!/bin/bash
# Archives the release app on this Mac and uploads it to TestFlight, without GitHub Actions.
#
# Upload auth prefers an App Store Connect API key over the Apple account signed in to Xcode
# (Settings > Accounts), whose session expires every few weeks: the key is read from
# ~/Run/Env/apple/appstore-connect.env (ASC_KEY_PATH/ASC_KEY_ID/ASC_ISSUER_ID) when it exists,
# or from those variables if already exported. Without a key it falls back to the Xcode account
# with DEVELOPMENT_TEAM from Configuration/Local.xcconfig. The version is version.properties;
# BUILD_ATTEMPT (default 0) uploads the same version again. --no-upload stops after the archive.
#
# The optimized Kotlin link takes about 10 minutes and needs a Mac with 16 GB of memory.
set -euo pipefail
cd "$(dirname "$0")/.."

upload=1
for arg in "$@"; do
  case $arg in
    --no-upload) upload=0 ;;
    *) echo "usage: $0 [--no-upload]" >&2; exit 2 ;;
  esac
done

team=${APPLE_TEAM_ID:-$(sed -n 's/^[[:space:]]*DEVELOPMENT_TEAM[[:space:]]*=[[:space:]]*//p' \
  Configuration/Local.xcconfig 2>/dev/null | tr -d '[:space:]')}
if [ -z "$team" ]; then
  echo "No signing team: put DEVELOPMENT_TEAM = <team id> in iosApp/Configuration/Local.xcconfig" >&2
  exit 1
fi

# The App Store Connect key from ~/Run/Env unless the caller exported one.
if [ -z "${ASC_KEY_PATH:-}" ] && [ -f "$HOME/Run/Env/apple/appstore-connect.env" ]; then
  # set -a exports what the env file sets, so the python step below sees the variables.
  set -a
  . "$HOME/Run/Env/apple/appstore-connect.env"
  set +a
fi

auth=()
if [ -n "${ASC_KEY_PATH:-}" ]; then
  if [ ! -f "${ASC_KEY_PATH/#\~/$HOME}" ]; then
    echo "warning: ASC_KEY_PATH not found ($ASC_KEY_PATH); falling back to the Xcode account" >&2
  else
    auth=(-authenticationKeyPath "${ASC_KEY_PATH/#\~/$HOME}"
          -authenticationKeyID "${ASC_KEY_ID:?ASC_KEY_ID is needed with ASC_KEY_PATH}"
          -authenticationKeyIssuerID "${ASC_ISSUER_ID:?ASC_ISSUER_ID is needed with ASC_KEY_PATH}")
    echo "Uploading with the App Store Connect API key ${ASC_KEY_ID}"
  fi
fi

memory_gb=$(( $(sysctl -n hw.memsize) / 1073741824 ))
if [ "$memory_gb" -lt 16 ]; then
  echo "warning: this Mac has $memory_gb GB of memory; the optimized Kotlin link needs about 10 GB" >&2
fi

scripts/generate.sh
version=$(sed -n 's/^MARKETING_VERSION = //p' Configuration/Version.xcconfig)
build=$(sed -n 's/^CURRENT_PROJECT_VERSION = //p' Configuration/Version.xcconfig)
out=build/testflight/$version-$build
archive=$out/ATask.xcarchive
echo "ATask $version ($build), team $team -> $archive"

rm -rf "$out"
xcodebuild archive \
  -project ATask.xcodeproj -scheme ATask -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath "$archive" \
  -allowProvisioningUpdates ${auth[@]+"${auth[@]}"} \
  DEVELOPMENT_TEAM="$team"

if [ "$upload" -eq 0 ]; then
  echo "Archived (not uploaded): $archive"
  exit 0
fi

# ExportOptions.plist has destination upload: the export sends the build to App Store Connect.
xcodebuild -exportArchive \
  -archivePath "$archive" \
  -exportPath "$out/export" \
  -exportOptionsPlist ExportOptions.plist \
  -allowProvisioningUpdates ${auth[@]+"${auth[@]}"}

echo "Uploaded ATask $version ($build). It shows in TestFlight once App Store Connect has processed it."

# The beta groups pick builds by hand (hasAccessToAllBuilds=false): a processed build is invisible
# to testers until it is added to a group. Assign the new build to every group of the app with the
# ASC API key, waiting for processing to finish first. A failure here only warns: the upload is
# done, and the build can be added in App Store Connect by hand.
if [ -n "${ASC_KEY_PATH:-}" ] && python3 -c 'import cryptography' 2>/dev/null; then
  echo "Assigning build $build to the app's beta groups..."
  if ! python3 - "$archive" "$build" <<'PY'
import base64, glob, json, os, plistlib, sys, time, urllib.error, urllib.request
from cryptography.hazmat.primitives.serialization import load_pem_private_key
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric.utils import decode_dss_signature

archive, build_no = sys.argv[1], sys.argv[2]
key = load_pem_private_key(open(os.path.expanduser(os.environ['ASC_KEY_PATH']), 'rb').read(), None)
b64u = lambda d: base64.urlsafe_b64encode(d).rstrip(b'=')
h = b64u(json.dumps({'alg': 'ES256', 'kid': os.environ['ASC_KEY_ID']}).encode())
p = b64u(json.dumps({'iss': os.environ['ASC_ISSUER_ID'], 'exp': int(time.time()) + 600,
                     'aud': 'appstoreconnect-v1'}).encode())
r, s = decode_dss_signature(key.sign(h + b'.' + p, ec.ECDSA(hashes.SHA256())))
jwt = (h + b'.' + p + b'.' + b64u(r.to_bytes(32, 'big') + s.to_bytes(32, 'big'))).decode()

def asc(path, data=None):
    headers = {'Authorization': 'Bearer ' + jwt}
    if data is not None:
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request('https://api.appstoreconnect.apple.com' + path,
                                 data=data, headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            body = resp.read()
            return resp.status, json.loads(body) if body else None
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or b'{}')

# The app comes from the archive's bundle id, not from ASC_APP_ID (which belongs to another app).
app_bundle = plistlib.load(open(glob.glob(archive + '/Products/Applications/*.app/Info.plist')[0], 'rb'))['CFBundleIdentifier']
st, apps = asc('/v1/apps?filter[bundleId]=' + app_bundle + '&limit=1')
if st != 200 or not apps.get('data'):
    sys.exit('no app found for bundle id ' + app_bundle)
app_id = apps['data'][0]['id']
print('app: %s (%s)' % (app_bundle, app_id))

# Wait for the build record to appear and reach a state a group can be given (about 10 min max).
build_id, state = None, None
for _ in range(20):
    st, b = asc('/v1/builds?filter[app]=' + app_id + '&filter[version]=' + build_no + '&limit=1')
    if st == 200 and b.get('data'):
        build_id = b['data'][0]['id']
        state = b['data'][0]['attributes'].get('processingState')
        if state != 'PROCESSING' and state != 'UPLOADING':
            break
    print('  build %s is %s, waiting...' % (build_no, state or 'not listed yet'))
    time.sleep(30)
if not build_id:
    sys.exit('build ' + build_no + ' never appeared')
print('build %s is %s' % (build_no, state))

st, groups = asc('/v1/betaGroups?filter[app]=' + app_id + '&limit=50')
if st != 200 or not groups.get('data'):
    sys.exit('no beta groups to assign to')
for g in groups['data']:
    name = g['attributes'].get('name', g['id'])
    body = json.dumps({'data': [{'type': 'builds', 'id': build_id}]}).encode()
    st, _ = asc('/v1/betaGroups/' + g['id'] + '/relationships/builds', body)
    print('  %s: %s' % (name, 'assigned' if st in (200, 204) else 'HTTP %d (add it by hand)' % st))
PY
  then
    echo "warning: build assignment failed; add build $build to its groups in App Store Connect" >&2
  fi
else
  echo "note: no ASC key (or cryptography module) — add build $build to its groups by hand" >&2
fi
