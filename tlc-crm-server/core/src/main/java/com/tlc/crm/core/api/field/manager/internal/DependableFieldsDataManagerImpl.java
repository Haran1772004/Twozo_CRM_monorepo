package com.tlc.crm.core.api.field.manager.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.field.manager.DependableFieldsDataManager;
import com.tlc.crm.core.fields.api.model.DependableFieldsData;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONDROPDOWNFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDODROPDOWNFIELDS;
import com.tlc.crm.entity.EntityLoader;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Manages the database actions for dependable fields
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class DependableFieldsDataManagerImpl implements DependableFieldsDataManager {

    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final EntityLoader entityLoader;
    private final DateTimeUtil dateTimeUtil;
    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final OptionService optionService;
    private final ForecastCategoryManager forecastCategoryManager;
    private final DateUtil dateUtil;

    private DependableFieldsDataManagerImpl() {
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        entityLoader = EntityLoader.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
        pipelineManager = PipelineManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        optionService = OptionService.getInstance();
        forecastCategoryManager =  ForecastCategoryManager.getInstance();
        dateUtil = DateUtil.getInstance();
    }

    private static final class Instance {
        private static final DependableFieldsDataManagerImpl DEPENDABLE_FIELDS_DATA_MANAGER = new DependableFieldsDataManagerImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link DependableFieldsDataManagerImpl}
     * </p>
     *
     * @return {@link DependableFieldsDataManagerImpl}
     */
    public static DependableFieldsDataManagerImpl getInstance() {
        return Instance.DEPENDABLE_FIELDS_DATA_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            Represents the organization id
     * @param entityId         Represents the entity id
     * @param source           Represents the source
     * @param dependableFields Represents the collection of dependable fields
     * @return {@link DependableFieldsData}
     */
    @Override
    public DependableFieldsData getDependableFieldsData(final Long orgId, final Long entityId, final Source source,
                                                        final Collection<FieldInfo> dependableFields) {
        final Table entityTable = switch (source) {
            case CONTACT -> contactManager.getTable();
            case DEAL -> dealManager.getTable();
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        };
        final DataContainer dataContainer = entityLoader.getFieldsDataContainer(orgId, dependableFields, entityTable,
                new WhereClause(Criteria.eq(entityTable.getPKColumn(), entityId)));
        final DependableFieldsData dependableFieldsData = new DependableFieldsData(entityId, source);

        switch (source) {
            case CONTACT -> loadContactDependableFieldsData(orgId, entityId, entityTable, dependableFieldsData, dataContainer, dependableFields);
            case DEAL -> loadDealDependableFieldsData(orgId, entityId, dependableFieldsData, dataContainer, dependableFields);
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
        }

        return dependableFieldsData;
    }

    /**
     * <p>
     * Loads the contact dependable field values into {@link DependableFieldsData}
     * </p>
     *
     * @param contactId            Represents the contact id
     * @param contactTable         Represents the contact table
     * @param dependableFieldsData Represents the {@link DependableFieldsData}
     * @param dataContainer        Represents the data container which contains the field values
     * @param dependableFields     Represents the collection of dependable fields
     */
    private void loadContactDependableFieldsData(final Long orgId, final Long contactId, final Table contactTable, final DependableFieldsData dependableFieldsData,
                                                 final DataContainer dataContainer, final Collection<FieldInfo> dependableFields) {
        final Supplier<Row> contactRowSupplier = () -> dataContainer.getRow(contactTable, contactId);

        for (final FieldInfo fieldInfo : dependableFields) {
            final Table valueTable = fieldInfo.table();

            switch (fieldInfo.aliasName()) {
                case "subscriptionStatus", "unsubscribeReason", "contactLostReason" -> {
                    final WhereClause whereClause = new WhereClause(Criteria.eq(valueTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), contactId))
                            .and(Criteria.eq(valueTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), fieldInfo.id()));
                    final Row valueRow = dataContainer.getRow(valueTable, whereClause);

                    if (Objects.nonNull(valueRow)) {
                        final Long optionId = valueRow.get(MCONDROPDOWNFIELDS.OPTION_ID);
                        final Option option = optionService.get(orgId, fieldInfo.id(), optionId);

                        if (Objects.nonNull(option)) {
                            dependableFieldsData.add(fieldInfo, Pair.of(optionId, option.getOption()));
                        }
                    }
                }
                case "subscriptionTypes" -> {
                    final WhereClause whereClause = new WhereClause(Criteria.eq(valueTable.getColumn(MCONDROPDOWNFIELDS.ENTITY_ID), contactId))
                            .and(Criteria.eq(valueTable.getColumn(MCONDROPDOWNFIELDS.FIELD_ID), fieldInfo.id()));
                    final Collection<Pair<Long, Object>> values = new ArrayList<>();

                    dataContainer.getRows(valueTable, whereClause).forEach(row -> {
                        final Long optionId = row.get(MCONDROPDOWNFIELDS.OPTION_ID);
                        final Option option = optionService.get(orgId, fieldInfo.id(), optionId);

                        if (Objects.nonNull(option)) {
                            values.add(Pair.of(optionId, option.getOption()));
                        }
                    });

                    if (!values.isEmpty()) {
                        dependableFieldsData.add(fieldInfo, values);
                    }
                }
                case "otherUnsubscribeReason" -> {
                    final Row valueRow = contactRowSupplier.get();

                    if (Objects.nonNull(valueRow)) {
                        dependableFieldsData.add(fieldInfo, valueRow.get(MCOCONTACT.OTHER_UNSUBSCRIBE_REASON));
                    }
                }
                case "lifeCycleStage" -> {
                    final Row valueRow = contactRowSupplier.get();

                    if (Objects.nonNull(valueRow)) {
                        final Long lifecycleStageId = valueRow.get(MCOCONTACT.LIFE_CYCLE_STAGE_ID);
                        final String lifeCycleStageName = lifeCycleStageManager.getLifeCycleStageName(orgId, lifecycleStageId);

                        if (Objects.nonNull(lifeCycleStageName)) {
                            dependableFieldsData.add(fieldInfo, Pair.of(lifecycleStageId, lifeCycleStageName));
                        }
                    }
                }
                case "lifeCycleStatus" -> {
                    final Row valueRow = contactRowSupplier.get();

                    if (Objects.nonNull(valueRow)) {
                        final Long lifecycleStatusId = valueRow.get(MCOCONTACT.LIFE_CYCLE_STATUS_ID);
                        final String lifeCycleStatusName = lifeCycleStatusManager.getLifeCycleStatusName(orgId, lifecycleStatusId);

                        if (Objects.nonNull(lifeCycleStatusName)) {
                            dependableFieldsData.add(fieldInfo, Pair.of(lifecycleStatusId, lifeCycleStatusName));
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Loads the deal dependable field values into {@link DependableFieldsData}
     * </p>
     *
     * @param dealId               Represents the deal id
     * @param dependableFieldsData Represents the {@link DependableFieldsData}
     * @param dataContainer        Represents the data container which contains the field values
     * @param dependableFields     Represents the collection of dependable fields
     */
    private void loadDealDependableFieldsData(final Long orgId, final Long dealId, final DependableFieldsData dependableFieldsData,
                                              final DataContainer dataContainer, final Collection<FieldInfo> dependableFields) {
        final Row dealRow = dataContainer.getRow(dealManager.getTable(), dealId);

        if (Objects.nonNull(dealRow)) {

            for (final FieldInfo fieldInfo : dependableFields) {
                final Table valueTable = fieldInfo.table();

                switch (fieldInfo.aliasName()) {
                    case "pipeline" -> {
                        final Long pipelineId = dealRow.get(MDODEAL.PIPELINE_ID);

                        if (Objects.nonNull(pipelineId)) {
                            final Pipeline pipeline = pipelineManager.getPipeline(orgId, pipelineId);

                            if (Objects.nonNull(pipeline)) {
                                dependableFieldsData.add(fieldInfo, Pair.of(pipelineId, pipeline.getName()));
                            }
                        }
                    }
                    case "pipelineStage" -> {
                        final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);

                        if (Objects.nonNull(pipelineStageId)) {
                            final String pipelineStageName = pipelineStageManager.getPipelineStageName(orgId, pipelineStageId);

                            if (Objects.nonNull(pipelineStageName)) {
                                dependableFieldsData.add(fieldInfo, Pair.of(pipelineStageId, pipelineStageName));
                            }
                        }
                    }
                    case "dealClosedOn" ->
                            dependableFieldsData.add(fieldInfo, dateTimeUtil.getTimeStamp(dealRow.get(MDODEAL.CLOSED_TIME)));
                    case "wonReason", "lostReason" -> {
                        final WhereClause whereClause = new WhereClause(Criteria.eq(valueTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), dealId))
                                .and(Criteria.eq(valueTable.getColumn(MDODROPDOWNFIELDS.FIELD_ID), fieldInfo.id()));
                        final Row valueRow = dataContainer.getRow(valueTable, whereClause);

                        if (Objects.nonNull(valueRow)) {
                            final Long optionId = valueRow.get(MDODROPDOWNFIELDS.OPTION_ID);
                            final Option option = optionService.get(orgId, fieldInfo.id(), optionId);

                            dependableFieldsData.add(fieldInfo, Pair.of(optionId, option.getOption()));
                        }
                    }
                    case "forecastCategory" -> {
                        final Long forecastCategoryId = dealRow.get(MDODEAL.FORECAST_CATEGORY_ID);

                        if (Objects.nonNull(forecastCategoryId)) {
                            dependableFieldsData.add(fieldInfo, Pair.of(forecastCategoryId, forecastCategoryManager.get(orgId, forecastCategoryId).getName()));
                        }
                    }
                    case "expectedCloseDate" -> dependableFieldsData.add(fieldInfo, dateUtil.getTimeStamp(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE)));
                }
            }
        }
    }
}