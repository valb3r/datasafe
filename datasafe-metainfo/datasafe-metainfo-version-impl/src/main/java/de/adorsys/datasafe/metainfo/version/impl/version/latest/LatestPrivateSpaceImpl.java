package de.adorsys.datasafe.metainfo.version.impl.version.latest;

import de.adorsys.datasafe.encrypiton.api.types.UserIDAuth;
import de.adorsys.datasafe.metainfo.version.api.actions.VersionedList;
import de.adorsys.datasafe.metainfo.version.api.actions.VersionedRead;
import de.adorsys.datasafe.metainfo.version.api.actions.VersionedRemove;
import de.adorsys.datasafe.metainfo.version.api.actions.VersionedWrite;
import de.adorsys.datasafe.metainfo.version.api.version.VersionedPrivateSpaceService;
import de.adorsys.datasafe.metainfo.version.impl.version.types.LatestDFSVersion;
import de.adorsys.datasafe.types.api.actions.ListRequest;
import de.adorsys.datasafe.types.api.actions.ReadRequest;
import de.adorsys.datasafe.types.api.actions.RemoveRequest;
import de.adorsys.datasafe.types.api.actions.WriteRequest;
import de.adorsys.datasafe.types.api.resource.*;
import lombok.Getter;

import javax.inject.Inject;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

/**
 * Privatespace where each operation will be applied to latest file version.
 * @implNote Operations on  non-versioned resources are not supported. Ideally, do not mix versioned and
 * non-versioned resources in same privatespace.
 * @param <V> version tag
 */
public class LatestPrivateSpaceImpl<V extends LatestDFSVersion> implements VersionedPrivateSpaceService<V> {

    @Getter
    private final V strategy;

    private final VersionedList<V> listService;
    private final VersionedRead<V> readService;
    private final VersionedRemove<V> removeService;
    private final VersionedWrite<V> writeService;

    @Inject
    public LatestPrivateSpaceImpl(V strategy, VersionedList<V> listService, VersionedRead<V> readService,
                                  VersionedRemove<V> removeService, VersionedWrite<V> writeService) {
        this.strategy = strategy;
        this.listService = listService;
        this.readService = readService;
        this.removeService = removeService;
        this.writeService = writeService;
    }

    // Delegate didn't work
    @Override
    public Stream<AbsoluteLocation<ResolvedResource>> list(ListRequest<UserIDAuth, PrivateResource> request) {
        return listService.list(request);
    }

    // Delegate didn't work
    @Override
    public Stream<Versioned<AbsoluteLocation<PrivateResource>, ResolvedResource, Version>> listWithDetails(
            ListRequest<UserIDAuth, PrivateResource> request) {
        return listService.listVersioned(request);
    }

    // Delegate didn't work
    @Override
    public InputStream read(ReadRequest<UserIDAuth, PrivateResource> request) {
        return readService.read(request);
    }

    // Delegate didn't work
    @Override
    public void remove(RemoveRequest<UserIDAuth, PrivateResource> request) {
        removeService.remove(request);
    }

    // Delegate didn't work
    @Override
    public OutputStream write(WriteRequest<UserIDAuth, PrivateResource> request) {
        return writeService.write(request);
    }
}
