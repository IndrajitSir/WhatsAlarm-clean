#!/bin/bash

set -e

echo "Installing Java 17..."
sudo apt update
sudo apt install -y openjdk-17-jdk unzip wget

# SDK paths
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# Create directories
mkdir -p $ANDROID_HOME/cmdline-tools
cd $HOME

echo "Downloading Android command line tools..."
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip

# Clean old install
rm -rf $ANDROID_HOME/cmdline-tools/latest
rm -rf cmdline-tools

# Extract properly
unzip cmdline-tools.zip
mkdir -p $ANDROID_HOME/cmdline-tools/latest
mv cmdline-tools/* $ANDROID_HOME/cmdline-tools/latest/
rm -rf cmdline-tools cmdline-tools.zip

# Verify sdkmanager
if [[ ! -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
    echo "❌ ERROR: sdkmanager not found"
    exit 1
fi

# Install SDK components
echo "Installing SDK components..."
yes | sdkmanager \
"platform-tools" \
"platforms;android-35" \
"build-tools;35.0.0"

# Accept licenses
yes | sdkmanager --licenses

# Persist environment variables
echo 'export ANDROID_HOME=$HOME/android-sdk' >> ~/.bashrc
echo 'export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH' >> ~/.bashrc
source ~/.bashrc

echo "✅ Android SDK setup complete!"
sdkmanager --version
