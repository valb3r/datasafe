package de.adorsys.datasafe.metainfo.version.impl.version.latest;

import de.adorsys.datasafe.encrypiton.api.types.UserIDAuth;
import de.adorsys.datasafe.metainfo.version.api.version.EncryptedLatestLinkService;
import de.adorsys.datasafe.metainfo.version.api.version.VersionEncoderDecoder;
import de.adorsys.datasafe.metainfo.version.api.version.VersionInfoService;
import de.adorsys.datasafe.metainfo.version.impl.version.types.DFSVersion;
import de.adorsys.datasafe.privatestore.api.actions.ListPrivate;
import de.adorsys.datasafe.types.api.actions.ListRequest;
import de.adorsys.datasafe.types.api.resource.*;

import javax.inject.Inject;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Default implementation of version information service that determines latest resource using
 * {@link EncryptedLatestLinkService} and reads all associated blobs with versions using {@link ListPrivate}
 * within privatespace. Then it decrypts associated blobs into version and path using {@link VersionEncoderDecoder}
 */
public class DefaultVersionInfoServiceImpl implements VersionInfoService<DFSVersion> {

    private final VersionEncoderDecoder encoder;
    private final ListPrivate listPrivate;
    private final EncryptedLatestLinkService latestVersionLinkLocator;

    @Inject
    public DefaultVersionInfoServiceImpl(VersionEncoderDecoder encoder, ListPrivate listPrivate,
                                         EncryptedLatestLinkService latestVersionLinkLocator) {
        this.encoder = encoder;
        this.listPrivate = listPrivate;
        this.latestVersionLinkLocator = latestVersionLinkLocator;
    }

    @Override
    public Stream<Versioned<AbsoluteLocation<ResolvedResource>, PrivateResource, DFSVersion>> versionsOf(
            ListRequest<UserIDAuth, PrivateResource> request) {

        return listPrivate.list(request).map(this::parseVersion).filter(Objects::nonNull);
    }

    @Override
    public Stream<Versioned<AbsoluteLocation<ResolvedResource>, ResolvedResource, DFSVersion>> listJoinedWithLatest(
            ListRequest<UserIDAuth, PrivateResource> request) {
        return versionsOf(request).map(it -> resolveLatest(request, it));
    }

    private Versioned<AbsoluteLocation<ResolvedResource>, ResolvedResource, DFSVersion> resolveLatest(
            ListRequest<UserIDAuth, PrivateResource> request,
            Versioned<AbsoluteLocation<ResolvedResource>, PrivateResource, DFSVersion> versioned) {
        AbsoluteLocation<PrivateResource> latestLink = latestVersionLinkLocator
                .resolveLatestLinkLocation(request.getOwner(), versioned.stripVersion());

        // TODO: This can be cached - latest links for resource version.
        AbsoluteLocation<ResolvedResource> resolved = listPrivate
                .list(request.toBuilder().location(latestLink.getResource()).build())
                .findFirst().orElse(null);

        // no latest found - it is possibly outdated, user needs to validate timestamp.
        if (null == resolved) {
            return new BaseVersionedPath<>(
                    versioned.getVersion(),
                    versioned.absolute(),
                    null
            );
        }

        AbsoluteLocation<PrivateResource> latestObject =
                latestVersionLinkLocator.readLinkAndDecrypt(request.getOwner(), latestLink);
        return new BaseVersionedPath<>(
                versioned.getVersion(),
                versioned.absolute(),
                resolved.getResource().withResource(latestObject.getResource())
        );
    }

    private Versioned<AbsoluteLocation<ResolvedResource>, PrivateResource, DFSVersion> parseVersion(
            AbsoluteLocation<ResolvedResource> resource) {
        VersionedUri versionedUri = encoder.decodeVersion(
                resource.getResource().asPrivate().decryptedPath()
        ).orElse(null);

        if (null == versionedUri) {
            return null;
        }

        return new BaseVersionedPath<>(
                new DFSVersion(versionedUri.getVersion()),
                resource,
                BasePrivateResource.forPrivate(versionedUri.getPathWithoutVersion())
        );
    }
}
