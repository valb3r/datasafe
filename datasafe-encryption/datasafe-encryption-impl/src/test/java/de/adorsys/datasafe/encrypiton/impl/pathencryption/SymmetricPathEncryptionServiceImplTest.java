package de.adorsys.datasafe.encrypiton.impl.pathencryption;

import de.adorsys.datasafe.encrypiton.api.keystore.KeyStoreService;
import de.adorsys.datasafe.encrypiton.api.types.keystore.*;
import de.adorsys.datasafe.encrypiton.impl.keystore.KeyStoreServiceImpl;
import de.adorsys.datasafe.types.api.resource.Uri;
import de.adorsys.datasafe.types.api.shared.BaseMockitoTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.net.URISyntaxException;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class SymmetricPathEncryptionServiceImplTest extends BaseMockitoTest {

    private SymmetricPathEncryptionServiceImpl bucketPathEncryptionService = new SymmetricPathEncryptionServiceImpl(
            new DefaultPathEncryption(new DefaultPathDigestConfig())
    );

    private KeyStoreService keyStoreService = new KeyStoreServiceImpl();
    private ReadKeyPassword readKeyPassword = new ReadKeyPassword("readkeypassword");
    private ReadStorePassword readStorePassword = new ReadStorePassword("readstorepassword");
    private KeyStoreAuth keyStoreAuth = new KeyStoreAuth(readStorePassword, readKeyPassword);
    private KeyStoreCreationConfig config = new KeyStoreCreationConfig(0, 1);
    private KeyStore keyStore = keyStoreService.createKeyStore(keyStoreAuth, KeyStoreType.DEFAULT, config);
    private KeyStoreAccess keyStoreAccess = new KeyStoreAccess(keyStore, keyStoreAuth);

    @Test
    void testSuccessEncryptDecryptPath() throws URISyntaxException {
        String testPath = "path/to/file";

        log.info("Test path: {}", testPath);

        Uri testURI = new Uri(testPath);

        SecretKeySpec secretKey = keyStoreService.getSecretKey(keyStoreAccess, KeyStoreCreationConfig.PATH_KEY_ID);
        Uri encrypted = bucketPathEncryptionService.encrypt(secretKey, testURI);
        Uri decrypted = bucketPathEncryptionService.decrypt(secretKey, encrypted);

        log.debug("Encrypted path: {}", encrypted);

        assertEquals(testPath, decrypted.toASCIIString());
    }

    @Test
    void testFailEncryptPathWithWrongKeyID() throws URISyntaxException {
        String testPath = "path/to/file/";

        log.info("Test path: {}", testPath);

        Uri testURI = new Uri(testPath);

        SecretKeySpec secretKey = keyStoreService.getSecretKey(keyStoreAccess, new KeyID("Invalid key"));
        // secret keys is null, because during key obtain was used incorrect KeyID,
        // so bucketPathEncryptionService#encrypt throw BaseException(was handled NullPointerException)
        assertThrows(IllegalArgumentException.class, () -> bucketPathEncryptionService.encrypt(secretKey, testURI));
    }

    @Test
    void testFailEncryptPathWithBrokenEncryptedPath() {
        SecretKeySpec secretKey = keyStoreService.getSecretKey(keyStoreAccess, KeyStoreCreationConfig.PATH_KEY_ID);

        assertThrows(BadPaddingException.class,
                () -> bucketPathEncryptionService.decrypt(secretKey,
                        new Uri("bRQiW8qLNPEy5tO7shfV0w==/k0HooCVlmhHkQFw8mc_BROKEN_PATH")));
    }

    @Test
    void testFailEncryptPathWithTextPath() {
        SecretKeySpec secretKey = keyStoreService.getSecretKey(keyStoreAccess, KeyStoreCreationConfig.PATH_KEY_ID);
        assertThrows(IllegalBlockSizeException.class,
                () -> bucketPathEncryptionService.decrypt(secretKey,
                        new Uri("/simple/text/path/")));
    }
}
