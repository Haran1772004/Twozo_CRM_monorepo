package com.tlc.crm.entity.listeners;

import com.tlc.crm.entity.cache.field.FieldCache;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.role.permission.processor.FieldPermissionProcessor;

import com.tlc.crm.entity.service.FieldService;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * A listener that will listens the create/update/delete actions of {@link FieldConfig}
 * </p>
 *
 * @author Sundar
 * @version 1.0
 */
public class CrmFieldConfigListener implements CompleteRowListener {

    private final FieldCache fieldCache;
    private final FieldCache secureFieldCache;
    private final FieldService fieldService;
    private final CrmFieldsManager crmFieldsManager;
    private final FieldPermissionProcessor fieldPermissionProcessor;

    private final static class InstanceHolder {
        private static final CrmFieldConfigListener INSTANCE = new CrmFieldConfigListener();
    }

    public static CrmFieldConfigListener getInstance() {
        return CrmFieldConfigListener.InstanceHolder.INSTANCE;
    }

    private CrmFieldConfigListener() {
        fieldCache = FieldCache.getInstance();
        secureFieldCache = FieldCache.getSecureInstance();
        fieldService = FieldService.getInstance();
        crmFieldsManager = CrmFieldsManager.getFieldsManager();
        fieldPermissionProcessor = FieldPermissionProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRows(final Collection<Row> newRows) {
        addFieldConfigsToCache(newRows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRows(final Collection<Row> updatedRows) {
        addFieldConfigsToCache(updatedRows);
    }

    /**
     * <p>
     * Removes each row from the field config cache
     * </p>
     */
    @Override
    public void deleteRows(final Collection<Row> deletedRows) {

        if (Objects.nonNull(deletedRows) && !deletedRows.isEmpty()) {
            deletedRows.stream().findAny().map(Row::getOrgId).ifPresent(orgId -> {
                fieldCache.invalidateCache(orgId);
                secureFieldCache.invalidateCache(orgId);
            });
        }
    }

    /**
     * <p>
     * Converts each {@link Row} into {@link FieldConfig} and add it to the cache
     * </p>
     *
     * @param rows Field config rows
     */
    private void addFieldConfigsToCache(final Collection<Row> rows) {

        if (Objects.nonNull(rows) && !rows.isEmpty()) {
            final Collection<Long> fieldIds = new HashSet<>();
            Long orgId = null;

            for (final Row row : rows) {
                final FieldConfig fieldConfig = crmFieldsManager.convertRowIntoFieldConfig(row);

                fieldIds.add(fieldConfig.getFieldId());

                if (Objects.isNull(orgId)) {
                    orgId = row.getOrgId();
                }
            }
            fieldCache.invalidateCache(orgId);
            secureFieldCache.invalidateCache(orgId);
            fieldPermissionProcessor.upsert(false, orgId, fieldService.getFieldsById(orgId, fieldIds).values());
        }
    }

}
