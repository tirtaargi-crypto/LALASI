#!/bin/sh
# builds an unsigned Lalasi.ipa that sidestore or altstore signs and installs on the phone
set -e
cd "$(dirname "$0")/.."

CONFIG="${1:-Release}"
rm -rf build/Payload build/Lalasi.ipa

xcodebuild -project Lalasi.xcodeproj -target Lalasi -configuration "$CONFIG" -sdk iphoneos \
    SYMROOT="$PWD/build" CODE_SIGNING_ALLOWED=NO -quiet build

mkdir -p build/Payload
cp -R "build/$CONFIG-iphoneos/Lalasi.app" build/Payload/
(cd build && zip -qry Lalasi.ipa Payload)
echo "built $PWD/build/Lalasi.ipa"
