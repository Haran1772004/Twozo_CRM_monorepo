package com.tlc.iam.server.resolver.internal;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.iam.common.models.User;
import com.tlc.iam.server.cache.UserCache;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.iam.server.resolver.UserResolver;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides the implementation for the {@link UserResolver}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class UserResolverImpl implements UserResolver {

    private static final byte STRIPES_COUNT = 4;

    private final UserManager userManager;
    private final UserCache userCache;
    private final LockStore<Long> lockStore;

    private UserResolverImpl() {
        userManager = UserManager.getInstance();
        userCache = UserCache.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
    }

    private static final class Instance {
        private static final UserResolverImpl USER_RESOLVER = new UserResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link  UserResolverImpl}
     * </p>
     *
     * @return the instance of {@link UserResolverImpl}
     */
    public static UserResolverImpl getInstance() {
        return Instance.USER_RESOLVER;
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId  Represents the iam organization id
     * @param iamUserId Represents the iam user id
     * @return the {@link User}
     */
    @Override
    public User get(final Long iamOrgId, final Long iamUserId) {
        final ReentrantReadWriteLock lock = lockStore.getLock(iamOrgId);
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final User cachedUser = userCache.get(iamUserId);

            if (Objects.nonNull(cachedUser)) {
                return cachedUser;
            }
        } finally {
            readLock.unlock();
        }
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            final User user = userManager.getUser(iamUserId, iamOrgId);

            userCache.add(user);

            return user;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param user Represents the {@link User}
     */
    @Override
    public void add(final User user) {
        userCache.add(user);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamUserId Represents the iam user id
     */
    @Override
    public void remove(final Long iamUserId) {
        userCache.remove(iamUserId);
    }
}
