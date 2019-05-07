package de.adorsys.datasafe.business.impl.e2e;

import com.google.common.io.ByteStreams;
import de.adorsys.datasafe.business.api.types.CreateUserPrivateProfile;
import de.adorsys.datasafe.business.api.types.CreateUserPublicProfile;
import de.adorsys.datasafe.business.api.types.UserID;
import de.adorsys.datasafe.business.api.types.UserIDAuth;
import de.adorsys.datasafe.business.api.types.action.ListRequest;
import de.adorsys.datasafe.business.api.types.action.ReadRequest;
import de.adorsys.datasafe.business.api.types.action.WriteRequest;
import de.adorsys.datasafe.business.api.types.keystore.ReadKeyPassword;
import de.adorsys.datasafe.business.api.types.resource.*;
import de.adorsys.datasafe.business.api.types.utils.LogHelper;
import de.adorsys.datasafe.business.impl.service.DefaultDatasafeServices;
import de.adorsys.datasafe.shared.BaseMockitoTest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseE2ETest extends BaseMockitoTest {

    protected static final String PRIVATE_COMPONENT = "private";
    protected static final String PRIVATE_FILES_COMPONENT = PRIVATE_COMPONENT + "/files";
    protected static final String INBOX_COMPONENT = "inbox";

    protected DefaultDatasafeServices services;

    protected UserIDAuth john;
    protected UserIDAuth jane;

    @SneakyThrows
    protected void writeDataToPrivate(UserIDAuth auth, String path, String data) {
        OutputStream stream = services.privateService().write(WriteRequest.forDefaultPrivate(auth, path));
        stream.write(data.getBytes());
        stream.close();
        log.info("File {} of user {} saved to {}", LogHelper.encryptIdNeeded(data),
                LogHelper.encryptIdNeeded(auth.getUserID()), LogHelper.encryptIdNeeded(path));
    }

    protected AbsoluteResourceLocation<PrivateResource> getFirstFileInPrivate(UserIDAuth inboxOwner) {
        List<AbsoluteResourceLocation<PrivateResource>> files = services.privateService().list(
                ListRequest.forDefaultPrivate(inboxOwner, "./")
        ).collect(Collectors.toList());

        log.info("{} has {} in PRIVATE", LogHelper.encryptIdNeeded(inboxOwner.getUserID().getValue()), LogHelper.encryptIdNeeded(files));
        return files.get(0);
    }

    @SneakyThrows
    protected String readPrivateUsingPrivateKey(UserIDAuth user, PrivateResource location) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream dataStream = services.privateService().read(ReadRequest.forPrivate(user, location));

        ByteStreams.copy(dataStream, outputStream);
        String data = new String(outputStream.toByteArray());
        log.info("{} has {} in PRIVATE", LogHelper.encryptIdNeeded(user.getUserID().getValue()), LogHelper.encryptIdNeeded(data));

        return data;
    }

    @SneakyThrows
    protected String readInboxUsingPrivateKey(UserIDAuth user, PrivateResource location) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream dataStream = services.inboxService().read(ReadRequest.forPrivate(user, location));

        ByteStreams.copy(dataStream, outputStream);
        String data = new String(outputStream.toByteArray());
        log.info("{} has {} in INBOX", LogHelper.encryptIdNeeded(user.getUserID().getValue()), LogHelper.encryptIdNeeded(data));

        return data;
    }

    protected AbsoluteResourceLocation<PrivateResource>  getFirstFileInInbox(UserIDAuth inboxOwner) {
        List<AbsoluteResourceLocation<PrivateResource>> files = services.inboxService().list(
               ListRequest.forDefaultPrivate(inboxOwner, "./")
        ).collect(Collectors.toList());

        log.info("{} has {} in INBOX", LogHelper.encryptIdNeeded(inboxOwner.getUserID().getValue()), LogHelper.encryptIdNeeded(files));
        return files.get(0);
    }

    protected void registerJohnAndJane(URI rootLocation) {
        john = registerUser("john", rootLocation);
        jane = registerUser("jane", rootLocation);
    }

    @SneakyThrows
    protected void sendToInbox(UserID to, String filename, String data) {
        OutputStream stream = services.inboxService().write(WriteRequest.forDefaultPublic(to, "./" + filename));
        stream.write(data.getBytes());
        stream.close();
        log.info("File {} sent to INBOX of user {}", LogHelper.encryptIdNeeded(filename), LogHelper.encryptIdNeeded(to));
    }

    protected UserIDAuth registerUser(String userName, URI rootLocation) {
        UserIDAuth auth = new UserIDAuth(new UserID(userName), new ReadKeyPassword("secure-password " + userName));

        rootLocation = rootLocation.resolve(userName + "/");

        URI keyStoreUri = rootLocation.resolve("./" + PRIVATE_COMPONENT + "/keystore");
        log.info("User's keystore location: {}", LogHelper.encryptIdNeeded(keyStoreUri));
        URI inboxUri = rootLocation.resolve("./" + INBOX_COMPONENT + "/");
        log.info("User's inbox location: {}", LogHelper.encryptIdNeeded(inboxUri));

        services.userProfile().registerPublic(CreateUserPublicProfile.builder()
                .id(auth.getUserID())
                .inbox(access(inboxUri))
                .publicKeys(access(keyStoreUri))
                .build()
        );

        URI filesUri = rootLocation.resolve("./" + PRIVATE_FILES_COMPONENT + "/");
        log.info("User's files location: {}", LogHelper.encryptIdNeeded(filesUri));

        services.userProfile().registerPrivate(CreateUserPrivateProfile.builder()
                .id(auth)
                .privateStorage(accessPrivate(filesUri))
                .keystore(accessPrivate(keyStoreUri))
                .inboxWithWriteAccess(accessPrivate(inboxUri))
                .build()
        );

        log.info("Created user: {}", LogHelper.encryptIdNeeded(userName));
        return auth;
    }

    protected void removeUser(UserIDAuth userIDAuth) {
        services.userProfile().deregister(userIDAuth);
        log.info("User deleted: {}", LogHelper.encryptIdNeeded(userIDAuth.getUserID().getValue()));
    }

    private AbsoluteResourceLocation<PublicResource> access(URI path) {
        return new AbsoluteResourceLocation<>(new DefaultPublicResource(path));
    }

    private AbsoluteResourceLocation<PrivateResource> accessPrivate(URI path) {
        return new AbsoluteResourceLocation<>(new DefaultPrivateResource(path, URI.create(""), URI.create("")));
    }
}
