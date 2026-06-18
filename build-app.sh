#!/bin/bash
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk
cd "/data/data/com.termux/files/home/agent app"
./gradlew assembleDebug --no-daemon
