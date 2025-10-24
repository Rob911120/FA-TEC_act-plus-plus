#!/bin/bash

# Build script for AR Measurement App
# This script builds the APK file for installation on Android devices

echo "================================================"
echo "AR Measurement App - Build Script"
echo "================================================"
echo ""

# Check if we're in the right directory
if [ ! -f "settings.gradle" ]; then
    echo "Error: Must be run from the ar-measurement-app directory"
    exit 1
fi

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
    echo "Warning: ANDROID_HOME not set"
    echo "Make sure Android SDK is installed"
    echo ""
fi

# Generate gradle wrapper if it doesn't exist
if [ ! -f "gradlew" ]; then
    echo "Generating Gradle wrapper..."
    gradle wrapper --gradle-version 8.1
    chmod +x gradlew
    echo ""
fi

# Build the APK
echo "Building APK..."
echo ""
./gradlew assembleDebug

# Check if build succeeded
if [ $? -eq 0 ]; then
    echo ""
    echo "================================================"
    echo "Build SUCCESS!"
    echo "================================================"
    echo ""
    echo "APK Location:"
    echo "  app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "To install on connected device:"
    echo "  ./gradlew installDebug"
    echo ""
    echo "Or manually install:"
    echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
else
    echo ""
    echo "================================================"
    echo "Build FAILED"
    echo "================================================"
    echo ""
    echo "Common issues:"
    echo "  1. Android SDK not installed"
    echo "  2. ANDROID_HOME environment variable not set"
    echo "  3. Internet connection required for first build"
    echo ""
    echo "Try opening the project in Android Studio instead"
    exit 1
fi
