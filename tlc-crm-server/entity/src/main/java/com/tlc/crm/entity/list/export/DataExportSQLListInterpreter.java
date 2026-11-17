package com.tlc.crm.entity.list.export;

import com.tlc.crm.common.export.model.ExportProviderType;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.SQLListInterpreter;

import com.tlc.crm.common.list.model.ConfigHolder;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * <p>
 * Provides the specialized functionality for exporting the data. It serves as a bridge between
 * the SQL-based data manipulation and the exportation process, ensuring seamless integration
 * and efficient data handling.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see SQLListInterpreter
 */
public final class DataExportSQLListInterpreter extends EntitySQLListInterpreter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataExportSQLListInterpreter.class);

    private final ExportProviderType exportProviderType;
    private final Set<String> fieldNames;
    private final boolean isMarketingListExport;
    private final boolean isSegmentContactListExport;

    public DataExportSQLListInterpreter(final Long userId, final Long orgId, final Provider provider, final JoinLoader joinLoader,
                                        final I18nResolutionAdapter i18NResolutionAdapter, final ExportProviderType exportProviderType,
                                        final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit, final Set<String> fieldNames,
                                        final boolean isMarketingListExport) {
        this(userId, orgId, provider, joinLoader, i18NResolutionAdapter, exportProviderType, maxFieldSelectionLimit,
                maxFilterCriteriaLimit, fieldNames, isMarketingListExport, false);
    }

    public DataExportSQLListInterpreter(final Long userId, final Long orgId, final Provider provider, final JoinLoader joinLoader,
                                        final I18nResolutionAdapter i18NResolutionAdapter, final ExportProviderType exportProviderType,
                                        final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit, final Set<String> fieldNames,
                                        final boolean isMarketingListExport, final boolean isSegmentContactListExport) {
        super(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);

        this.exportProviderType = exportProviderType;
        this.fieldNames = fieldNames;
        this.isMarketingListExport = isMarketingListExport;
        this.isSegmentContactListExport = isSegmentContactListExport;
    }

    public Set<String> getFieldNames() {
        return Objects.nonNull(fieldNames) ? fieldNames : Collections.emptySet();
    }

    /**
     * <p>
     * Applies the pk criteria for the given {@link Collection} of entity ids.
     * </p>
     *
     * @param ids        Represents the {@link Collection} of entity ids
     * @param isExcluded A flag indicating whether the IDs are excluded or not.
     */
    public void applyPkCriteria(final Collection<Long> ids, final boolean isExcluded) {
        final Column pkColumn = super.getPrimaryTable().getPKColumn();
        final Criteria criteria = isExcluded ? Criteria.notIn(pkColumn, ids) : Criteria.in(pkColumn, ids);

        super.getDerivedQuery().setWhereClause(new WhereClause(criteria));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultSelect() {

        if (Objects.equals(ExportProviderType.DETAIL, exportProviderType)) {
            final Collection<FieldConfig> fields = listStructure.getFields();

            for (final FieldConfig fieldConfig : fields) {

                if (Objects.nonNull(fieldConfig)) {
                    if (fieldConfig.isDynamicField()) {
                        dynamicFields.add(fieldConfig.getName());
                    } else {
                        loadSelectColumnAndJoins(fieldConfig, selectQuery);
                    }
                }
            }
        } else {
            final long defaultSelectTime = System.currentTimeMillis();
            final Collection<ConfiguredField> defaultConfiguredFields = getDefaultConfiguredFields();
            final List<ConfigHolder> configHolders = getConfiguredFieldsInternal();

            for (final ConfiguredField defaultField : defaultConfiguredFields) {
                defaultField.setEnabled(true);
                selectClause(defaultField);

                configHolders.removeIf(holder -> holder.fieldConfig().getName().contains(defaultField.fieldName()));
            }

            for (final ConfigHolder configHolder : configHolders) {
                final FieldConfig fieldConfig = configHolder.fieldConfig();
                final FieldConfig.Config config = fieldConfig.getConfig();

                final boolean isFieldEnabled = Objects.nonNull(config) && (isMarketingListExport ? config.isEnabledForMarketingList()
                        : isSegmentContactListExport ? config.isEnabledForSegmentContactList() || config.isMandatoryField()
                        : config.isEnabledForWeb());

                if (fieldNames.contains(fieldConfig.getName()) || isFieldEnabled) {
                    if (fieldConfig.isDynamicField()) {
                        dynamicFields.add(fieldConfig.getName());
                    } else {
                        loadSelectColumnAndJoins(fieldConfig, selectQuery);
                    }
                }
            }
            LOGGER.debug("Data export - [{}]: Default select process completed in {}ms, Org ID: {}, User ID: {}", provider, System.currentTimeMillis() - defaultSelectTime, orgId, userId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultLimit() {
    }
}