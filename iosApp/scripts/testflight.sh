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
  . "$HOME/Run/Env/apple/appstore-connect.env"
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
