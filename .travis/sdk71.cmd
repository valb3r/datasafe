call "C:\Program Files\Microsoft SDKs\Windows\v7.1\Bin\SetEnv.cmd"

call %1 ^
    -jar datasafe-cli/target/datasafe-cli-pkg.jar ^
    -H:+ReportUnsupportedElementsAtRuntime ^
    -H:Name=native-image-test ^
    --no-fallback ^
    --enable-all-security-services ^
    --features=de.adorsys.datasafe.cli.hacks.GraalCompileFixNpeOnMissingServiceTypeInKnownProviders ^
    --initialize-at-build-time=org.apache.http.conn.routing.HttpRoute ^
    --initialize-at-build-time=org.apache.http.conn.HttpClientConnectionManager ^
    --initialize-at-build-time=org.apache.http.HttpClientConnection ^
    --initialize-at-build-time=org.apache.http.conn.ConnectionRequest ^
    --initialize-at-build-time=org.apache.http.protocol.HttpContext ^
    --initialize-at-build-time=org.apache.http.pool.ConnPoolControl ^
    --initialize-at-run-time=org.bouncycastle.crypto.prng.SP800SecureRandom ^
    --initialize-at-run-time='org.bouncycastle.jcajce.provider.drbg.DRBG$Default' ^
    --initialize-at-run-time='org.bouncycastle.jcajce.provider.drbg.DRBG$NonceAndIV'
