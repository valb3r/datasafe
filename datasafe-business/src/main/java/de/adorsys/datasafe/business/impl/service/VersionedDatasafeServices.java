package de.adorsys.datasafe.business.impl.service;

import dagger.BindsInstance;
import dagger.Component;
import de.adorsys.datasafe.business.impl.cmsencryption.DefaultCMSEncryptionModule;
import de.adorsys.datasafe.business.impl.directory.DefaultCredentialsModule;
import de.adorsys.datasafe.business.impl.directory.DefaultProfileModule;
import de.adorsys.datasafe.business.impl.document.DefaultDocumentModule;
import de.adorsys.datasafe.business.impl.inbox.actions.DefaultInboxActionsModule;
import de.adorsys.datasafe.business.impl.keystore.DefaultKeyStoreModule;
import de.adorsys.datasafe.business.impl.pathencryption.DefaultPathEncryptionModule;
import de.adorsys.datasafe.business.impl.privatestore.actions.DefaultPrivateActionsModule;
import de.adorsys.datasafe.business.impl.privatestore.actions.DefaultVersionedPrivateActionsModule;
import de.adorsys.datasafe.business.impl.storage.DefaultStorageModule;
import de.adorsys.datasafe.directory.api.config.DFSConfig;
import de.adorsys.datasafe.directory.impl.profile.operations.DFSBasedProfileStorageImpl;
import de.adorsys.datasafe.inbox.impl.InboxServiceImpl;
import de.adorsys.datasafe.metainfo.version.impl.version.latest.DefaultVersionInfoServiceImpl;
import de.adorsys.datasafe.metainfo.version.impl.version.latest.LatestPrivateSpaceImpl;
import de.adorsys.datasafe.metainfo.version.impl.version.types.LatestDFSVersion;
import de.adorsys.datasafe.privatestore.impl.PrivateSpaceServiceImpl;
import de.adorsys.datasafe.storage.api.StorageService;

import javax.inject.Singleton;

/**
 * This is Datasafe services that always work with latest file version using `software` versioning.
 */
@Singleton
@Component(modules = {
        DefaultCredentialsModule.class,
        DefaultKeyStoreModule.class,
        DefaultDocumentModule.class,
        DefaultCMSEncryptionModule.class,
        DefaultPathEncryptionModule.class,
        DefaultInboxActionsModule.class,
        DefaultPrivateActionsModule.class,
        DefaultVersionedPrivateActionsModule.class,
        DefaultProfileModule.class,
        DefaultStorageModule.class
})
public interface VersionedDatasafeServices {

    /**
     * @return Provides version information for a given resource (shows only versioned resources)
     */
    DefaultVersionInfoServiceImpl versionInfo();

    /**
     * @return Filtered view of user's private space, that shows only latest files (works only with versioned resources)
     */
    LatestPrivateSpaceImpl<LatestDFSVersion> latestPrivate();

    /**
     * @return Raw view of private user space (shows everything - all versioned and not versioned)
     */
    PrivateSpaceServiceImpl privateService();

    /**
     * @return Inbox service that provides capability to share data between users
     */
    InboxServiceImpl inboxService();

    /**
     * @return User-profile service that provides information necessary for locating his data.
     */
    DFSBasedProfileStorageImpl userProfile();

    /**
     * Binds DFS connection (for example filesystem, minio) and system storage and access
     */
    @Component.Builder
    interface Builder {

        /**
         * Binds (configures) system root uri - where user profiles will be located and system
         * access to open (but not to read key) keystore.
         */
        @BindsInstance
        Builder config(DFSConfig config);

        /**
         * Binds (configures) all storage operations - not necessary to call {@code storageList} after.
         */
        @BindsInstance
        Builder storage(StorageService storageService);

        /**
         * @return Software-versioned Datasafe services.
         */
        VersionedDatasafeServices build();
    }
}
