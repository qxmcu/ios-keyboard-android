#!/usr/bin/env bash
# ==============================================================================
# Automated Build Script for iOS Keyboard Clone (Android)
# Builds both Debug and Release APKs out-of-the-box
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================================"
echo "  iOS Keyboard for Android - Automated Build Script"
echo "========================================================"

chmod +x ./gradlew

BUILD_TYPE="${1:-all}"

case "$BUILD_TYPE" in
    debug)
        echo "--> Building Debug APK..."
        ./gradlew assembleDebug --stacktrace
        echo ""
        echo "SUCCESS: Debug APK built at:"
        echo "  $SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        echo "--> Building Release APK..."
        ./gradlew assembleRelease --stacktrace
        echo ""
        echo "SUCCESS: Release APK built at:"
        echo "  $SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"
        ;;
    all|*)
        echo "--> Building both Debug and Release APKs..."
        ./gradlew assembleDebug assembleRelease --stacktrace
        echo ""
        echo "SUCCESS: APKs built successfully at:"
        echo "  Debug:   $SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk"
        echo "  Release: $SCRIPT_DIR/app/build/outputs/apk/release/app-release.apk"
        ;;
esac

echo ""
echo "To install via adb, run:"
echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo "========================================================"
