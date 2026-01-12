#!/bin/bash

# ---------------------------
# Android SDK Setup Script
# ---------------------------

# Set SDK path
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# Create directories
mkdir -p $ANDROID_HOME/cmdline-tools

# Download latest command line tools for Linux
echo "Downloading Android command line tools..."
cd $HOME
wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O cmdline-tools.zip

# Remove any broken 'latest' folder
rm -rf $ANDROID_HOME/cmdline-tools/latest

# Unzip and move to proper location
unzip -q cmdline-tools.zip
mv cmdline-tools $ANDROID_HOME/cmdline-tools/latest
rm cmdline-tools.zip

# Verify sdkmanager exists
if [[ ! -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
    echo "ERROR: sdkmanager not found! Check the extraction."
    exit 1
fi

# Update PATH for current shell
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# Install required SDK components
echo "Installing platform-tools, Android 34 SDK, and build-tools 34.0.0..."
yes | sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Accept all licenses
echo "Accepting licenses..."
yes | sdkmanager --licenses

# Final check
echo "Android SDK setup complete!"
sdkmanager --version
