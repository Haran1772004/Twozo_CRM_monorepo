package com.tlc.crm.workflow.cache.resolver.internal;

import com.tlc.crm.workflow.api.manager.StepOptionManager;
import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.cache.StepOptionCache;
import com.tlc.crm.workflow.cache.resolver.StepOptionResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * The resolver responsible for retrieving and storing details of the {@link StepOption} from/to the cache
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class StepOptionResolverImpl implements StepOptionResolver {

    private final StepOptionManager stepOptionManager;
    private final StepOptionCache stepOptionCache;
    private final ReentrantReadWriteLock lock;

    private StepOptionResolverImpl() {
        this.stepOptionCache = StepOptionCache.getInstance();
        this.stepOptionManager = StepOptionManager.getInstance();
        this.lock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final StepOptionResolverImpl INSTANCE = new StepOptionResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link StepOptionResolverImpl}
     * </p>
     *
     * @return The {@link StepOptionResolverImpl}
     */
    public static StepOptionResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the {@link StepOption} id
     * @return {@link StepOption}\\
     *
     */
    @Override
    public StepOption get(final Long id) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final StepOption stepOption;

        readLock.lock();

        try {
            stepOption = stepOptionCache.getStepOption(id);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(stepOption)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final StepOption workflowStepOption = stepOptionManager.getStepOption(id);

                stepOptionCache.setStepOption(workflowStepOption);

                return workflowStepOption;
            } finally {
                writeLock.unlock();
            }
        }

        return stepOption;
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Represents the {@link Collection} of {@link StepOption} id
     * @return return true if it is present, otherwise it will return false
     */
    @Override
    public boolean exists(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return false;
        } else {

            for (final Long id : ids) {

                if (Objects.isNull(get(id))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Represents the {@link Set} of step option id
     * @return {@link Collection} of {@link StepOption}
     */
    public Collection<StepOption> getStepOptions(final Set<Long> ids) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Collection<StepOption> stepOptions;

        readLock.lock();

        try {
            stepOptions = stepOptionCache.getStepOptions(ids);
        } finally {
            readLock.unlock();
        }

        if (stepOptions.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();
            try {
                final Collection<StepOption> workflowStepOptions = stepOptionManager.getStepOptions(ids);

                stepOptionCache.setStepOptions(workflowStepOptions);

                return workflowStepOptions;
            } finally {
                writeLock.unlock();
            }
        }

        return stepOptions;
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasNames {@link Collection} of alias name
     * @return {@link Map} of {@link StepOption}
     */
    @Override
    public Map<String, StepOption> getStepOptionsByAliasName(final Collection<String> aliasNames) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Map<String, StepOption> stepOptions;

        readLock.lock();

        try {
            stepOptions = stepOptionCache.getStepOptionsByAliasName(aliasNames);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(stepOptions) || stepOptions.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Map<String, StepOption> stepOptionsByAliasName = stepOptionManager.getStepOptionsByAliasName(aliasNames);

                stepOptionCache.setStepOptions(stepOptionsByAliasName.values());

                return stepOptionsByAliasName;
            } finally {
                writeLock.unlock();
            }
        }

        return stepOptions;
    }
}
