#!/bin/bash

NATIVE_IMAGE_CMD=$(echo "$JAVA_HOME/jre/bin/native-image.cmd" | sed 's!^/c/!C:\\!g' | sed 's!/!\\!g')
echo "Starting native-image from $NATIVE_IMAGE"

echo "Check 3"
cmd.exe /C "$NATIVE_IMAGE_CMD"

echo "Check 4"
cmd.exe /C "$NATIVE_IMAGE_CMD" -jar target\datasafe-cli-pkg.jar -H:+ReportUnsupportedElementsAtRuntime -H:Name=native-image-test --no-server --no-fallback --enable-all-security-services --features=de.adorsys.datasafe.cli.hacks.GraalCompileFixNpeOnMissingServiceTypeInKnownProviders --initialize-at-build-time=org.apache.http.conn.routing.HttpRoute --initialize-at-build-time=org.apache.http.conn.HttpClientConnectionManager --initialize-at-build-time=org.apache.http.HttpClientConnection --initialize-at-build-time=org.apache.http.conn.ConnectionRequest --initialize-at-build-time=org.apache.http.protocol.HttpContext --initialize-at-build-time=org.apache.http.pool.ConnPoolControl --initialize-at-run-time=org.bouncycastle.crypto.prng.SP800SecureRandom --initialize-at-run-time='org.bouncycastle.jcajce.provider.drbg.DRBG$Default' --initialize-at-run-time='org.bouncycastle.jcajce.provider.drbg.DRBG$NonceAndIV'
