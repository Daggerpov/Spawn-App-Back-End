#!/bin/bash
# Build script that ensures Java 17 is used
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw "$@"
