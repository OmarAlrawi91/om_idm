#!/usr/bin/env bash
# Local build on Linux/macOS: installs Android SDK + Gradle 9.3.1, then builds the APK.
set -euo pipefail
command -v java >/dev/null || { echo "Install JDK 17+ first"; exit 1; }
ROOT="$(cd "$(dirname "$0")" && pwd)"; TOOLS="$ROOT/.tools"; mkdir -p "$TOOLS"
export ANDROID_HOME="$TOOLS/android-sdk"
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  OS=linux; [ "$(uname)" = "Darwin" ] && OS=mac
  curl -L -o "$TOOLS/cmdtools.zip" "https://dl.google.com/android/repository/commandlinetools-${OS}-11076708_latest.zip"
  unzip -q "$TOOLS/cmdtools.zip" -d "$ANDROID_HOME/cmdline-tools" && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null || true
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-36" "build-tools;36.0.0" "platform-tools"
echo "sdk.dir=$ANDROID_HOME" > "$ROOT/local.properties"
if [ ! -d "$TOOLS/gradle-9.3.1" ]; then
  curl -L -o "$TOOLS/gradle.zip" https://services.gradle.org/distributions/gradle-9.3.1-bin.zip
  unzip -q "$TOOLS/gradle.zip" -d "$TOOLS"
fi
cd "$ROOT" && "$TOOLS/gradle-9.3.1/bin/gradle" :app:assembleDebug
echo "APK: $ROOT/app/build/outputs/apk/debug/"
