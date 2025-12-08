#!/bin/bash
# Build script that ensures Java 17 is used
# This fixes compatibility issues with Lombok and newer Java versions

export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home

echo "Building with Java 17..."
echo "JAVA_HOME: $JAVA_HOME"

./mvnw clean package -Dmaven.test.skip=true "$@"

