#!/bin/bash

NATIVE_IMAGE_CMD=$(echo "$JAVA_HOME/bin/native-image.cmd" | sed 's!^/c/!C:\\!g' | sed 's!/!\\!g')
echo "Starting native-image from $NATIVE_IMAGE"

cmd //E:ON //V:ON //T:0E //K .travis\sdk71.cmd "$NATIVE_IMAGE_CMD"
