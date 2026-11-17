package com.tlc.crm.core.action.export;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.export.action.DataExporter;
import com.tlc.crm.common.export.model.ExportProviderType;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.core.segment.manager.SegmentContactManager;
import com.tlc.crm.entity.export.fields.ExportFields;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.entity.list.export.DataExportSQLListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Exports contacts for a segment using the same columns as {@link com.tlc.crm.core.action.segment.SegmentContactListAction}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@DataExporter(name = "segmentContact")
public class SegmentContactExportAction extends ContactExportAction {

    private final SegmentContactManager segmentContactManager;

    /**
     * <p>
     * Creates the segment contact export action and initializes the manager
     * required to resolve the contacts of a segment.
     * </p>
     */
    public SegmentContactExportAction() {
        segmentContactManager = SegmentContactManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link EntityListInterpreter} used to export the contacts of a segment. Restricts the export
     * to the contacts of the segment when all contacts are selected, excluding the excluded ids. Otherwise,
     * restricts the export to the selected contact ids, excluding the excluded ids.
     * </p>
     *
     * @param orgId                Represents the organization id.
     * @param userId               Represents the user id.
     * @param request              Represents the export request JSON.
     * @param provider             Represents the {@link Provider}.
     * @param joinLoader           Represents the {@link JoinLoader}.
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return The {@link EntityListInterpreter} with the primary key criteria applied.
     */
    @Override
    public EntityListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                          final Provider provider, final JoinLoader joinLoader,
                                                          final I18nResolutionAdapter i18NResolutionAdapter) {
        final DataExportSQLListInterpreter dataExportSQLListInterpreter = new DataExportSQLListInterpreter(userId, orgId,
                provider, joinLoader, i18NResolutionAdapter, ExportProviderType.get(request.getString(ExportFields.EXPORT_PROVIDER_TYPE)),
                maxFieldSelectionLimit, maxFilterCriteriaLimit, getFieldNames(orgId, userId, request, provider), false, true);
        final Long segmentId = request.getLong(SegmentFields.SEGMENT_ID.getName());
        final boolean isAllSelected = request.optBoolean(ExportFields.IS_ALL_SELECTED);
        final JsonArray excludedIdsArray = request.optJsonArray(ExportFields.EXCLUDED_IDS);
        final Collection<Long> excludedIds = new HashSet<>();

        if (Objects.nonNull(excludedIdsArray)) {
            excludedIdsArray.forEach(excludedId -> excludedIds.add((Long) excludedId));
        }

        if (isAllSelected) {
            final Collection<Long> contactIds = segmentContactManager.getContactIds(orgId, segmentId, excludedIds, null);

            dataExportSQLListInterpreter.applyPkCriteria(contactIds, false);
        } else {
            final JsonArray entityIdsArray = request.getJsonArray(ExportFields.IDS);
            final Collection<Long> entityIds = new HashSet<>();

            entityIdsArray.forEach(id -> entityIds.add((Long) id));
            dataExportSQLListInterpreter.applyPkCriteria(entityIds, false);

            if (!excludedIds.isEmpty()) {
                dataExportSQLListInterpreter.applyPkCriteria(excludedIds, true);
            }
        }

        return dataExportSQLListInterpreter;
    }

    /**
     * <p>
     * Checks whether the field is enabled for the segment contact export. A field is enabled
     * if it is mandatory or enabled for the segment contact list.
     * </p>
     *
     * @param config Represents the {@link FieldConfig.Config} of the field.
     * @return {@code true} if the field is enabled for the segment contact export.
     */
    @Override
    protected boolean isEnabled(final FieldConfig.Config config) {
        return config.isMandatoryField() || config.isEnabledForSegmentContactList();
    }

    /**
     * <p>
     * Gets the list type of the segment contact export.
     * </p>
     *
     * @return The {@link ListType#SEGMENT_CONTACT_LIST}.
     */
    @Override
    protected ListType getListType() {
        return ListType.SEGMENT_CONTACT_LIST;
    }
}