package com.tlc.crm.zapier.internal.service;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.zapier.api.models.ZapierField;
import com.tlc.crm.zapier.internal.cache.resolver.ZapierFieldResolver;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import java.util.Collection;
import java.util.Objects;

/**
 * Handling operations related to ZapierField objects.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ZapierFieldServiceImpl implements ZapierFieldService {

    private final ZapierFieldResolver zapierFieldResolver;

    private ZapierFieldServiceImpl() {
        this.zapierFieldResolver = ZapierFieldResolver.getInstance();
    }

    private static class Instance {
        private static final ZapierFieldServiceImpl INSTANCE = new ZapierFieldServiceImpl();
    }

    /**
     * Gets the {@link ZapierFieldServiceImpl} instance.
     *
     * @return {@link ZapierFieldServiceImpl}.
     */
    public static ZapierFieldServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id The action ID for which the fields are to be retrieved.
     * @return The collection of {@link ZapierField}.
     */
    @Override
    public Collection<ZapierField> getFieldsByActionId(final Short id) {
        final Collection<ZapierField> fields = zapierFieldResolver.getFieldsByActionId(id);

        if (Objects.isNull(fields) || fields.isEmpty()) {
            throw ErrorCode.get(ZapierErrorCodes.ACTION_FIELDS_NOT_FOUND, "i18n.crm.zapier.invalid.action.field");
        }

        return fields;
    }
}
