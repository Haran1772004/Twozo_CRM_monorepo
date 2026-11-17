package com.tlc.crm.core.action.export;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.export.action.DataExporter;
import com.tlc.crm.common.export.model.ExportProviderType;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;

import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.core.field.marketinglist.MarketingListFields;

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
 * Exports the marketing list contact information.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@DataExporter(name = "marketingListContact")
public class MarketingListContactExportAction extends ContactExportAction {

    private final MarketingListContactManager marketingListContactManager;

    public MarketingListContactExportAction() {
        marketingListContactManager = MarketingListContactManager.getInstance();
    }

    /**
     * <p>
     * Gets the export sql {@link EntityListInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                 Represents an organization id
     * @param userId                Represents the user id
     * @param request               Represents the request {@link JsonObject}
     * @param provider              Represents the {@link Provider}
     * @param joinLoader            Represents the {@link JoinLoader} to obtain JoinClause for the dependent
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return export sql {@link EntityListInterpreter}
     */
    @Override
    public EntityListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                          final Provider provider, final JoinLoader joinLoader,
                                                          final I18nResolutionAdapter i18NResolutionAdapter) {
        final DataExportSQLListInterpreter dataExportSQLListInterpreter = new DataExportSQLListInterpreter(userId, orgId,
                provider, joinLoader, i18NResolutionAdapter, ExportProviderType.get(request.getString(ExportFields.EXPORT_PROVIDER_TYPE)),
                maxFieldSelectionLimit, maxFilterCriteriaLimit, getFieldNames(orgId, userId, request, provider), true);
        final Long marketingListId = request.getLong(MarketingListFields.MARKETING_LIST_ID);
        final boolean isAllSelected = request.optBoolean(ExportFields.IS_ALL_SELECTED);
        final JsonArray excludedIdsArray = request.optJsonArray(ExportFields.EXCLUDED_IDS);
        final Collection<Long> excludedIds = new HashSet<>();

        if (Objects.nonNull(excludedIdsArray)) {
            excludedIdsArray.forEach(excludedId -> excludedIds.add((Long) excludedId));
        }

        if (isAllSelected) {
            final Collection<Long> contactIds = marketingListContactManager.getContactIds(orgId, marketingListId, excludedIds, null);

            dataExportSQLListInterpreter.applyPkCriteria(contactIds, false);
        } else {
            final JsonArray entityIdsArray = request.getJsonArray(ExportFields.IDS);
            final Collection<Long> entityIds = new HashSet<>();

            entityIdsArray.forEach(id -> entityIds.add((Long) id));
            dataExportSQLListInterpreter.applyPkCriteria(entityIds, false);
        }

        return dataExportSQLListInterpreter;
    }

    /**
     * {@inheritDoc}
     *
     * @param config the field configuration settings
     * @return {@code true} if the configuration is enabled for marketing contact list, otherwise {@code false}
     */
    @Override
    protected boolean isEnabled(final FieldConfig.Config config) {
        return config.isEnabledForMarketingList();
    }
}