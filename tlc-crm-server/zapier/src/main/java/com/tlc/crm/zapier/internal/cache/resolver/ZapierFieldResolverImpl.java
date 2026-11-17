package com.tlc.crm.zapier.internal.cache.resolver;

import com.tlc.crm.zapier.api.manager.ZapierFieldManager;
import com.tlc.crm.zapier.api.models.ZapierField;
import com.tlc.crm.zapier.internal.cache.ZapierFieldCache;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The resolver responsible for retrieving and storing the details of {@link ZapierField} from/to cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ZapierFieldResolverImpl implements ZapierFieldResolver {

    private final ZapierFieldCache zapierFieldCache;
    private final ReentrantReadWriteLock lock;
    private final ZapierFieldManager zapierFieldManager;

    private ZapierFieldResolverImpl() {
        this.zapierFieldCache = ZapierFieldCache.getInstance();
        this.lock = new ReentrantReadWriteLock();
        this.zapierFieldManager = ZapierFieldManager.getInstance();
    }

    private static class Instance {
        private static final ZapierFieldResolverImpl INSTANCE = new ZapierFieldResolverImpl();
    }

    /**
     * Gets the {@link ZapierFieldResolverImpl} instance.
     *
     * @return {@link ZapierFieldResolverImpl}.
     */
    public static ZapierFieldResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id The action ID for which the fields are to be retrieved.
     */
    @Override
    public Collection<ZapierField> getFieldsByActionId(final Short id) {
        final Collection<ZapierField> zapierFields = zapierFieldCache.getFieldsByAction(id);

        if (Objects.isNull(zapierFields) || zapierFields.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Collection<ZapierField> fields = zapierFieldManager.getByActionId(id);

                zapierFieldCache.setFields(id, fields);
                return fields;
            } finally {
                writeLock.unlock();
            }
        }

        return zapierFields;
    }

}
