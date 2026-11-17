package com.tlc.crm.entity.listeners;

import com.tlc.crm.common.models.Action;

import com.tlc.crm.entity.builder.FieldInfoBuilder;
import com.tlc.crm.entity.cache.field.FieldCache;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.Field;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.role.permission.processor.FieldPermissionProcessor;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A listener that will listens the create/update/delete actions of {@link Field}
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class CrmFieldListener implements CompleteRowListener {

    private final FieldCache fieldCache;
    private final FieldCache secureFieldCache;
    private final FieldInfoBuilder fieldInfoBuilder;
    private final CrmFieldsManager crmFieldsManager;
    private final FieldPermissionProcessor fieldPermissionProcessor;

    private final static class InstanceHolder {
        private static final CrmFieldListener INSTANCE = new CrmFieldListener();
    }

    public static CrmFieldListener getInstance() {
        return CrmFieldListener.InstanceHolder.INSTANCE;
    }

    private CrmFieldListener() {
        fieldCache = FieldCache.getInstance();
        secureFieldCache = FieldCache.getSecureInstance();
        fieldInfoBuilder = FieldInfoBuilder.getInstance();
        crmFieldsManager = CrmFieldsManager.getSecureFieldsManager();
        fieldPermissionProcessor = FieldPermissionProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRows(final Collection<Row> newRows) {
        loadFields(newRows, Action.CREATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRows(final Collection<Row> updatedRows) {
        loadFields(updatedRows, Action.UPDATE);
    }

    /**
     * <p>
     * Removes each row from the field cache
     * </p>
     */
    @Override
    public void deleteRows(final Collection<Row> deletedRows) {

        if (Objects.nonNull(deletedRows) && !deletedRows.isEmpty()) {
            final Collection<FieldInfo> fields = new ArrayList<>();
            Long orgId = null;

            for (final Row deletedRow : deletedRows) {
                final Field field = crmFieldsManager.convertRowToModel(deletedRow);

                field.setFieldConfig(crmFieldsManager.getFieldConfig(field.orgId(), field.id()));
                final FieldInfo fieldInfo = fieldInfoBuilder.build(field);

                fields.add(fieldInfo);

                if (Objects.isNull(orgId)) {
                    orgId = deletedRow.getOrgId();
                }
            }
            fieldCache.invalidateCache(orgId);
            secureFieldCache.invalidateCache(orgId);
            fieldPermissionProcessor.delete(orgId, fields);
        }
    }

    /**
     * <p>
     * Converts each {@link Row} into {@link Field} and add it to the cache
     * </p>
     *
     * @param rows Field rows
     */
    private void loadFields(final Collection<Row> rows, final Action action) {

        if (Objects.nonNull(rows) && !rows.isEmpty()) {
            final Collection<FieldInfo> fields = new ArrayList<>();
            Long orgId = null;

            for (final Row row : rows) {
                final Field field = crmFieldsManager.convertRowToModel(row);

                field.setFieldConfig(crmFieldsManager.getFieldConfig(field.orgId(), field.id()));
                final FieldInfo fieldInfo = fieldInfoBuilder.build(field);

                fields.add(fieldInfo);

                if (Objects.isNull(orgId)) {
                    orgId = row.getOrgId();
                }
            }
            fieldCache.invalidateCache(orgId);
            secureFieldCache.invalidateCache(orgId);

            if (Action.CREATE == action) {
                fieldPermissionProcessor.upsert(false, true, orgId, fields);
            }
        }
    }
}
