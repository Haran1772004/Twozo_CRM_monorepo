package com.tlc.crm.entity.listeners;

import com.tlc.crm.entity.cache.OptionCacheImpl;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.cleanup.OrphanReferenceCleanupService;
import com.tlc.crm.entity.service.dropdown.DropdownOptionReferenceService;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * A listener that will listen the creation/update/delete actions of {@link Option}
 *
 * @author Kishore J
 * @version 1.0
 */
public class OptionListener implements CompleteRowListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptionListener.class);

    private final OptionCacheImpl optionCacheImpl;
    private final OptionManager optionManager;
    private final OrphanReferenceCleanupService cleanupService;
    private final DropdownOptionReferenceService dropdownOptionReferenceService;
    private final FieldService fieldService;

    private OptionListener() {
        optionCacheImpl = OptionCacheImpl.getInstance();
        optionManager = OptionManager.getInstance();
        cleanupService = OrphanReferenceCleanupService.getInstance();
        dropdownOptionReferenceService = DropdownOptionReferenceService.getInstance();
        fieldService = FieldService.getInstance();
    }

    private static final class Instance {
        private static final OptionListener INSTANCE = new OptionListener();
    }

    /**
     * <p>
     * Gets the {@link OptionListener} instance
     * </p>
     *
     * @return The {@link OptionListener}
     */
    public static OptionListener getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of {@link Option} rows
     */
    @Override
    public void addRows(final Collection<Row> rows) {
        addOptionsToCache(rows);
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of {@link Option} rows
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {
        if (Objects.nonNull(rows) && !rows.isEmpty()) {

            for (final Row row : rows) {
                final Option option = optionManager.convertRowToModel(row);

                optionCacheImpl.removeOption(option);
                triggerOrphanCleanup(row.getOrgId(), option);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param rows Which contains the collection of {@link Option} rows
     */
    @Override
    public void updateRows(final Collection<Row> rows) {
        addOptionsToCache(rows);
    }

    /**
     * <p>
     * Adds a collection {@link Option} rows data into the cache
     * </p>
     *
     * @param rows Which contains the collection of {@link Option} rows
     */
    private void addOptionsToCache(final Collection<Row> rows) {
        if (Objects.nonNull(rows) && !rows.isEmpty()) {

            for (final Row row : rows) {
                optionCacheImpl.addOption(optionManager.convertRowToModel(row));
            }
        }
    }

    /**
     * <p>
     * Handles side effects when an option row is deleted: mutable dropdown reassignment or orphan cleanup
     * on persistent custom fields.
     * </p>
     *
     * @param orgId  Organization id.
     * @param option Deleted {@link Option}.
     */
    private void triggerOrphanCleanup(final Long orgId, final Option option) {
        if (Objects.isNull(orgId) || Objects.isNull(option) || Objects.isNull(option.id())) {
            return;
        }

        try {
            final Long fieldId = option.getFieldId();

            if (Objects.nonNull(fieldId)) {
                LOGGER.debug("Triggering option deletion handling: orgId={}, optionId={}, fieldId={}", orgId, option.id(), fieldId);
                final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);

                if (dropdownOptionReferenceService.isReassignRequired(fieldInfo)) {
                    LOGGER.debug("Skipping post-delete reassignment for mutable dropdown fieldId={}; handled in field update", fieldId);
                } else {
                    cleanupService.cleanupOptionReferences(orgId, option.id(), fieldId);
                }
            }
        } catch (final Exception exception) {
            LOGGER.error("Error triggering option deletion handling: optionId={}, error={}", option.id(), exception.getMessage(), exception);
        }
    }
}
