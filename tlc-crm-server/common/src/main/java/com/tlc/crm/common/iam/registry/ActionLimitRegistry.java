package com.tlc.crm.common.iam.registry;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.remote.listener.ExpiryListener;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.commons.util.StringUtil;

import com.tlc.crm.common.cache.RemoteCache;
import com.tlc.crm.common.iam.api.manager.ActionLimitManager;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.security.permission.model.ActionLimitDetails;
import com.tlc.crm.security.permission.model.LimitActionType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

/**
 * <p>
 * Registry to holds the remaining available limits to perform the action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class ActionLimitRegistry implements RemoteCache, SecureUserTaskProcessor {

    private final Cache<String, Map<LimitActionType, Long>> limitationsCache;
    private final Cache<String, Map<LimitActionType, Long>> currentlyUsedCountCache;
    private final ActionLimitManager actionLimitManager;
    private final LockStore<String> lockStore;

    private ActionLimitRegistry() {
        actionLimitManager = ActionLimitManager.getInstance();
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<String, Map<LimitActionType, Long>> currentlyUsedCountCacheConfig = new CacheConfig<>("currentlyUsedCountCache");
        final CacheConfig<String, Map<LimitActionType, Long>> limitationsCacheConfig = new CacheConfig<>("limitationsCache");
        final BiConsumer<Long, ActionLimitDetails> remainingCountUpdater = (remainingActionCount, actionLimitDetails) -> {
            actionLimitDetails.setRemainingActionCount(remainingActionCount);
            actionLimitManager.store(actionLimitDetails);
        };
        final BiConsumer<Long, ActionLimitDetails> currentCountUpdater = (currentlyUsedCount, actionLimitDetails) -> {
            actionLimitDetails.setCurrentlyUsedCount(currentlyUsedCount);
            actionLimitManager.updateCurrentlyUsedLimit(actionLimitDetails.orgId(), List.of(actionLimitDetails));
        };

        currentlyUsedCountCacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(2));
        limitationsCacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(1));
        currentlyUsedCountCacheConfig.addListener(buildExpiryListener(currentCountUpdater));
        limitationsCacheConfig.addListener(buildExpiryListener(remainingCountUpdater));

        this.lockStore = new LockStore<>(10);
        this.limitationsCache = cacheService.createOrOpenCache(limitationsCacheConfig);
        this.currentlyUsedCountCache = cacheService.createOrOpenCache(currentlyUsedCountCacheConfig);
    }

    private static class Instance {
        private static final ActionLimitRegistry INSTANCE = new ActionLimitRegistry();
    }

    public static ActionLimitRegistry getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
    }

    /**
     * <p>
     * Stores the remaining limit count of the given {@link LimitActionType}.
     * </p>
     *
     * @param orgId           Represents an organisation id.
     * @param userId          Represents an user id.
     * @param permittedLimit  Represents the permitted limit count of the action.
     * @param usedCount       Represents the used count of the action.
     * @param limitActionType Represents the type of the action.
     */
    public void setRemainingLimitCount(final long orgId, final long userId, final Long permittedLimit, final long usedCount,
                                       final LimitActionType limitActionType) {
        setRemainingLimitCount(orgId, userId, permittedLimit, usedCount, 0, limitActionType);
    }

    /**
     * <p>
     * Stores the remaining limit count of the given {@link LimitActionType}.
     * </p>
     *
     * @param orgId             Represents an organisation id.
     * @param userId            Represents an user id.
     * @param permittedLimit    Represents the permitted limit count of the action.
     * @param usedCount         Represents the used count of the action.
     * @param currentUsageValue Represents the current usage value.
     * @param limitActionType   Represents the type of the action.
     */
    public void setRemainingLimitCount(final long orgId, final long userId, final Long permittedLimit, final long usedCount,
                                       final int currentUsageValue, final LimitActionType limitActionType) {
        if (Objects.nonNull(permittedLimit) && (usedCount <= permittedLimit)) {
            final long remainingActionCount = permittedLimit - usedCount;

            decrementLimit(orgId, userId, remainingActionCount, currentUsageValue, limitActionType);
        } else {
            setRemainingLimitCount(orgId, userId, 0L, 0, 0, limitActionType);
        }
    }

    /**
     * <p>
     * Decrements the remaining limit count of the given {@link LimitActionType}.
     * </p>
     *
     * @param orgId             Represents an organisation id.
     * @param userId            Represents an user id.
     * @param remainingCount    Represents the remaining count of the action.
     * @param currentUsageValue Represents the current usage value.
     * @param limitActionType   Represents the type of the action.
     */
    public void decrementLimit(final long orgId, final long userId, final long remainingCount, final int currentUsageValue,
                               final LimitActionType limitActionType) {
        final String orgUserId = combine(orgId, userId);
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(orgUserId).writeLock();

        writeLock.lock();

        try {
            final Map<LimitActionType, Long> remainingActionLimit = limitationsCache.computeIfAbsent(orgUserId, orgUser -> new HashMap<>());
            final long remainingLimitCount = remainingActionLimit.getOrDefault(limitActionType, remainingCount) - currentUsageValue;
            final long actionLimit = Math.max(remainingLimitCount, 0);

            remainingActionLimit.put(limitActionType, actionLimit);
            limitationsCache.put(orgUserId, remainingActionLimit);
        } finally {
            writeLock.unlock();
        }

        if (0 < currentUsageValue) {
            updateCurrentUsageCount(orgId, userId, currentUsageValue, limitActionType);
        }
    }

    /**
     * <p>
     * Fetches the remaining limit count of the given {@link LimitActionType}.
     * </p>
     *
     * @param orgId           Represents an organisation id.
     * @param userId          Represents an user id.
     * @param limitActionType Represents the type of the action.
     */
    public Optional<Long> getRemainingLimit(final long orgId, final long userId, final LimitActionType limitActionType) {
        final String orgUserId = combine(orgId, userId);
        final ReentrantReadWriteLock.ReadLock readLock = lockStore.getLock(orgUserId).readLock();

        readLock.lock();

        try {
            final Optional<Map<LimitActionType, Long>> remainingCountByType = Optional.ofNullable(limitationsCache.get(orgUserId));

            return remainingCountByType.map(remainingCounts -> remainingCounts.get(limitActionType));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * <p>
     * Fetches the currently used action limit count.
     * </p>
     *
     * @param orgId           Represents the organisation id.
     * @param userId          Represents the user id.
     * @param limitActionType Represents the type of the limit permission.
     * @return Currently used action count.
     */
    public long getCurrentlyUsedLimit(final long orgId, final long userId, final LimitActionType limitActionType) {
        final String orgUserId = combine(orgId, userId);
        final ReentrantReadWriteLock.ReadLock readLock = lockStore.getLock(orgUserId).readLock();
        final Optional<Long> currentlyUsedCount;

        readLock.lock();

        try {
            final Optional<Map<LimitActionType, Long>> currentlyUsedCountByType = Optional.ofNullable(currentlyUsedCountCache.get(orgUserId));
            currentlyUsedCount = currentlyUsedCountByType.map(currentlyUsedCounts -> currentlyUsedCounts.get(limitActionType));
        } finally {
            readLock.unlock();
        }

        return currentlyUsedCount.orElseGet(() -> {
            final ActionLimitDetails actionLimitDetails = new ActionLimitDetails(orgId, userId, limitActionType);
            final long currentlyUsedLimit = actionLimitManager.getCurrentlyUsedLimit(actionLimitDetails);

            updateCurrentlyUsedLimit(orgId, userId, currentlyUsedLimit, limitActionType);
            return currentlyUsedLimit;
        });
    }

    /**
     * <p>
     * Clears the remaining limit count of the given {@link LimitActionType}.
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param actionLimitDetails Holds the details of the action limit count.
     */
    public void clear(final long orgId, final Collection<ActionLimitDetails> actionLimitDetails) {
        final String orgIdAsString = String.valueOf(orgId);
        final Map<String, Map<LimitActionType, Long>> remainingActionCounts = new HashMap<>();
        final Map<String, Map<LimitActionType, Long>> currentlyUsedActionCounts = new HashMap<>();
        final Map<String, Collection<LimitActionType>> limitActionTypes = new HashMap<>();
        final ReentrantReadWriteLock.ReadLock readLock = lockStore.getLock(orgIdAsString).readLock();

        readLock.lock();

        try {
            for (final ActionLimitDetails actionLimitDetail : actionLimitDetails) {
                final String orgUserId = combine(actionLimitDetail.orgId(), actionLimitDetail.getUserId());

                limitActionTypes.computeIfAbsent(orgUserId, userId -> new HashSet<>()).add(actionLimitDetail.getLimitActionType());
            }
            final Set<String> orgUserIds = limitActionTypes.keySet();

            Optional.ofNullable(limitationsCache.getAll(orgUserIds)).ifPresent(remainingActionCounts::putAll);
            Optional.ofNullable(currentlyUsedCountCache.getAll(orgUserIds)).ifPresent(currentlyUsedActionCounts::putAll);
        } finally {
            readLock.unlock();
        }
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(orgIdAsString).writeLock();

        writeLock.lock();

        try {
            for (final Map.Entry<String, Collection<LimitActionType>> limitActionTypeEntry : limitActionTypes.entrySet()) {
                final String orgUserId = limitActionTypeEntry.getKey();
                final Map<LimitActionType, Long> remainingCountsByAction = remainingActionCounts.getOrDefault(orgUserId, new HashMap<>());
                final Map<LimitActionType, Long> currentlyUsedCountsByAction = currentlyUsedActionCounts.getOrDefault(orgUserId, new HashMap<>());

                limitActionTypeEntry.getValue().forEach(limitActionType -> {
                    remainingCountsByAction.remove(limitActionType);
                    currentlyUsedCountsByAction.remove(limitActionType);
                });
                limitationsCache.put(orgUserId, remainingCountsByAction);
                currentlyUsedCountCache.put(orgUserId, currentlyUsedCountsByAction);
            }
        } finally {
            writeLock.unlock();
        }
        actionLimitManager.updateRemainingActionLimit(orgId, actionLimitDetails);
        actionLimitManager.updateCurrentlyUsedLimit(orgId, actionLimitDetails);
    }

    /**
     * <p>
     * Updates the current usage action limit count.
     * </p>
     *
     * @param orgId             Represents the organisation id.
     * @param userId            Represents the user id.
     * @param currentUsageValue Represents the current usage value.
     * @param limitActionType   Represents the type of the limit permission.
     */
    private void updateCurrentUsageCount(final long orgId, final long userId, final long currentUsageValue, final LimitActionType limitActionType) {
        final long usedActionCount = getCurrentlyUsedLimit(orgId, userId, limitActionType);

        updateCurrentlyUsedLimit(orgId, userId, usedActionCount + currentUsageValue, limitActionType);
    }

    /**
     * <p>
     * Updates the currently used action limit count.
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param userId             Represents the user id.
     * @param currentlyUsedCount Represents the current usage value.
     * @param limitActionType    Represents the type of the limit permission.
     */
    private void updateCurrentlyUsedLimit(final long orgId, final long userId, final long currentlyUsedCount, final LimitActionType limitActionType) {
        final String orgUserId = combine(orgId, userId);
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(orgUserId).writeLock();

        writeLock.lock();

        try {
            final Map<LimitActionType, Long> currentlyUsedCountByType = currentlyUsedCountCache.computeIfAbsent(orgUserId, orgUser -> new HashMap<>());

            currentlyUsedCountByType.put(limitActionType, currentlyUsedCount);
            currentlyUsedCountCache.put(orgUserId, currentlyUsedCountByType);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * <p>
     * Builds the expiry listener of action counts cache to store the action counts into DB.
     * </p>
     *
     * @param countUpdater Represents the updater of the count.
     * @return {@link ExpiryListener}.
     */
    private ExpiryListener<String, Map<LimitActionType, Long>> buildExpiryListener(final BiConsumer<Long, ActionLimitDetails> countUpdater) {
        return (orgUserId, limitActionTypes) -> {
            if (Objects.nonNull(limitActionTypes)) {
                final String[] orgAndUserId = orgUserId.split(StringUtil.Delimiter.UNDER_SCORE.getDelimiter());
                final long orgId = Long.parseLong(orgAndUserId[0]);
                final long userId = Long.parseLong(orgAndUserId[1]);

                secureProcess(orgId, userId, () ->
                        limitActionTypes.forEach((limitActionType, actionCounts) -> {
                            final ActionLimitDetails actionLimitDetails = new ActionLimitDetails(orgId, userId, limitActionType);

                            countUpdater.accept(actionCounts, actionLimitDetails);
                        }));
            }
        };
    }

    /**
     * <p>
     * Combines the organisation id and the user id.
     * </p>
     *
     * @param orgId  Represents an organisation id.
     * @param userId Represents an user id.
     * @return Combined org and user ids.
     */
    private String combine(final long orgId, final long userId) {
        return StringUtil.joinNonNullValues(StringUtil.Delimiter.UNDER_SCORE, orgId, userId).orElseGet(() -> "");
    }
}
