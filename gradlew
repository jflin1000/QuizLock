#!/bin/bash
export JAVA_HOME=${JAVA_HOME:-/workspace/tools/jdk-17.0.21+9}
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME=/workspace/tools/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
exec /workspace/tools/gradle-8.7/bin/gradle "$@"
