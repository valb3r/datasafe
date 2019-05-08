package de.adorsys.datasafe.business.impl.directory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import de.adorsys.datasafe.business.api.profile.operations.ProfileRegistrationService;
import de.adorsys.datasafe.business.api.profile.operations.ProfileRetrievalService;
import de.adorsys.datasafe.business.api.resource.ResourceResolver;
import de.adorsys.datasafe.business.api.types.UserID;
import de.adorsys.datasafe.business.api.types.UserPrivateProfile;
import de.adorsys.datasafe.business.api.types.UserPublicProfile;
import de.adorsys.datasafe.business.impl.profile.operations.DFSBasedProfileStorageImpl;
import de.adorsys.datasafe.business.impl.profile.operations.DefaultUserProfileCache;
import de.adorsys.datasafe.business.impl.profile.operations.UserProfileCache;
import de.adorsys.datasafe.business.impl.profile.resource.ResourceResolverImpl;

import javax.inject.Singleton;

/**
 * This module is responsible for providing user profiles - his inbox, private storage, etc. locations.
 */
@Module
public abstract class DefaultProfileModule {

    @Provides
    @Singleton
    static UserProfileCache userProfileCache() {
        Cache<UserID, UserPublicProfile> publicProfileCache = CacheBuilder.newBuilder()
                .initialCapacity(1000)
                .build();
        Cache<UserID, UserPrivateProfile> privateProfileCache = CacheBuilder.newBuilder()
                .initialCapacity(1000)
                .build();

        return new DefaultUserProfileCache(publicProfileCache.asMap(), privateProfileCache.asMap());
    }

    @Binds
    abstract ProfileRetrievalService profileService(DFSBasedProfileStorageImpl impl);

    @Binds
    abstract ProfileRegistrationService creationService(DFSBasedProfileStorageImpl impl);

    @Binds
    abstract ResourceResolver resourceResolver(ResourceResolverImpl impl);
}