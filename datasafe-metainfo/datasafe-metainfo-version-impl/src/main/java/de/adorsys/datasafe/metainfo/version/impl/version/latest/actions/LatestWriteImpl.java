package de.adorsys.datasafe.metainfo.version.impl.version.latest.actions;

import de.adorsys.datasafe.encrypiton.api.types.UserIDAuth;
import de.adorsys.datasafe.metainfo.version.api.actions.VersionedWrite;
import de.adorsys.datasafe.metainfo.version.api.version.EncryptedLatestLinkService;
import de.adorsys.datasafe.metainfo.version.api.version.VersionEncoderDecoder;
import de.adorsys.datasafe.metainfo.version.impl.version.types.LatestDFSVersion;
import de.adorsys.datasafe.privatestore.api.actions.EncryptedResourceResolver;
import de.adorsys.datasafe.privatestore.api.actions.WriteToPrivate;
import de.adorsys.datasafe.types.api.actions.WriteRequest;
import de.adorsys.datasafe.types.api.resource.AbsoluteLocation;
import de.adorsys.datasafe.types.api.resource.BasePrivateResource;
import de.adorsys.datasafe.types.api.resource.PrivateResource;
import de.adorsys.datasafe.types.api.resource.Uri;
import de.adorsys.datasafe.types.api.utils.Log;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Default versioned resource writer that resolves latest resource link location using
 * {@link EncryptedLatestLinkService}, writes versioned with {@link VersionEncoderDecoder} and encrypted blob into
 * privatespace using {@link WriteToPrivate} then updates latest link content, so that it points to written blob.
 * Link content is the resource that is relative to user privatespace.
 * Relativization against privatespace root of written blob is done by {@link EncryptedResourceResolver}.
 * @implNote Writes only to versioned resources - can't be used with ordinary one
 * @param <V> version tag
 */
public class LatestWriteImpl<V extends LatestDFSVersion> implements VersionedWrite<V> {

    @Getter
    private final V strategy;

    private final VersionEncoderDecoder encoder;
    private final EncryptedResourceResolver encryptedResourceResolver;
    private final WriteToPrivate writeToPrivate;
    private final EncryptedLatestLinkService latestVersionLinkLocator;

    @Inject
    public LatestWriteImpl(V strategy, VersionEncoderDecoder encoder, EncryptedResourceResolver encryptedResourceResolver,
                           WriteToPrivate writeToPrivate, EncryptedLatestLinkService latestVersionLinkLocator) {
        this.strategy = strategy;
        this.encoder = encoder;
        this.encryptedResourceResolver = encryptedResourceResolver;
        this.writeToPrivate = writeToPrivate;
        this.latestVersionLinkLocator = latestVersionLinkLocator;
    }

    @Override
    public OutputStream write(WriteRequest<UserIDAuth, PrivateResource> request) {
        AbsoluteLocation<PrivateResource> latestSnapshotLink =
                latestVersionLinkLocator.resolveLatestLinkLocation(
                        request.getOwner(), request.getLocation()
                );

        Uri decryptedPath = encoder.newVersion(request.getLocation().location()).getPathWithVersion();

        PrivateResource resourceRelativeToPrivate = encryptPath(
                request.getOwner(),
                decryptedPath,
                request.getLocation()
        );

        return new VersionCommittingStream(
                writeToPrivate.write(
                        request.toBuilder().location(BasePrivateResource.forPrivate(decryptedPath)).build()
                ),
                writeToPrivate,
                request.toBuilder().location(latestSnapshotLink.getResource()).build(),
                resourceRelativeToPrivate
        );
    }

    private PrivateResource encryptPath(UserIDAuth auth, Uri uri, PrivateResource base) {
        AbsoluteLocation<PrivateResource> resource = encryptedResourceResolver.encryptAndResolvePath(
                auth,
                base.resolve(uri, new Uri(""))
        );

        return BasePrivateResource.forPrivate(resource.getResource().encryptedPath());
    }

    @Slf4j
    @RequiredArgsConstructor
    private static final class VersionCommittingStream extends OutputStream {

        private final OutputStream streamToWrite;
        private final WriteToPrivate writeToPrivate;
        private final WriteRequest<UserIDAuth, PrivateResource> request;
        private final PrivateResource writtenResource;

        @Override
        public void write(int b) throws IOException {
            streamToWrite.write(b);
        }

        @Override
        public void write(byte[] bytes, int off, int len) throws IOException {
            streamToWrite.write(bytes, off, len);
        }

        @Override
        @SneakyThrows
        public void close() {
            super.close();
            streamToWrite.close();

            log.debug("Committing file {} with blob {}", Log.secure(request.getLocation()), Log.secure(writtenResource));
            try (OutputStream os = writeToPrivate.write(request)) {
                os.write(writtenResource.location().toASCIIString().getBytes());
            }
        }
    }
}
