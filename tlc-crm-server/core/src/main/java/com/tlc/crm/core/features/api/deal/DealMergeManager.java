package com.tlc.crm.core.features.api.deal;

import com.tlc.commons.util.TypeChecker;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.sql.resource.MDODROPDOWNFIELDS;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.core.api.handler.ModificationHandler;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.ParticipantManager;
import com.tlc.crm.core.api.deal.manager.ProductDetailsHolderManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.action.service.EmailSummaryActionService;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.core.features.api.merge.EntityMergeManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;

import com.tlc.i18n.I18nKey;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

/**
 * Manages the merge deal related DB actions
 *
 * @author pandiaraj
 */
public class DealMergeManager extends EntityMergeManager {

    private final DealManager dealManager;
    private final SourceActivityManager sourceActivityManager;
    private final ParticipantManager participantManager;
    private final FieldService fieldService;
    private final MonetaryManager monetaryManager;
    private final ProductDetailsHolderManager productDetailsHolderManager;
    private final CommonTimelineManager commonTimelineManager;
    private final SourceTagManager sourceTagManager;
    private final ModificationHandler modificationHandler;
    /**
     * <p>
     * Initializes all required manager and service instances used across deal merge operations.
     * </p>
     */
    public DealMergeManager() {
        dealManager = DealManager.getInstance();
        sourceActivityManager = SourceActivityManager.getInstance();
        participantManager = ParticipantManager.getInstance();
        fieldService = FieldService.getSecureInstance();
        monetaryManager = MonetaryManager.getInstance();
        productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        sourceTagManager = SourceTagManager.getInstance();
        modificationHandler = ModificationHandler.getInstance();
    }

    private static class Instance {
        private static final DealMergeManager INSTANCE = new DealMergeManager();
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @param orgId Represents the organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Gets the {@link DealMergeManager} Instance
     *
     * @return {@link DealMergeManager}
     */
    public static DealMergeManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the deal preview details based on the primary and secondary id
     *
     * @param orgId       Represents the organization id
     * @param primaryId   Represents the primary id
     * @param secondaryId Represents the secondary id
     * @return The {@link Deal} preview details
     */
    public Deal getDealPreviewDetails(final Long orgId, final Long primaryId, final Long secondaryId) {
        final Table dealTable = dealManager.getTable();
        final Set<Long> dealIds = Set.of(primaryId, secondaryId);
        final DataContainer dataContainer = getDataContainer(orgId, dealIds);
        final Deal primaryDeal = getPreviewDetails(primaryId, dealTable, dataContainer);
        final Deal secondaryDeal = getPreviewDetails(secondaryId, dealTable, dataContainer);

        mergeDealsForPreview(primaryDeal, secondaryDeal);
        loadDealUsers(primaryDeal);

        return primaryDeal;
    }

    /**
     * <p>
     * Gets the {@link Deal} details
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param dealId Represents the deal id
     * @return The {@link Deal} details
     */
    public Deal get(final Long orgId, final Long dealId) {
        final Table dealTable = dealManager.getTable();
        final DataContainer dataContainer = getDataContainer(orgId, List.of(dealId));
        final Deal deal = getPreviewDetails(dealId, dealTable, dataContainer);

        loadDealUsers(deal);

        return deal;
    }

    /**
     * Loads deal users in a single fetch and stores user ids in the entity for downstream converters.
     */
    private void loadDealUsers(final Deal deal) {
        final CrmUser createdBy = deal.getCreatedBy();
        final CrmUser updatedBy = deal.getUpdatedBy();
        final CrmUser salesOwner = deal.getSalesOwner();
        final Collection<Long> userIds = collectUserIds(createdBy.id(), updatedBy.id(), salesOwner.id());
        userIds.add(deal.getOwnerId());

        final ValueHolder valueHolder = deal.getValueHolder();

        if (Objects.nonNull(valueHolder)) {
            userIds.addAll(valueHolder.getUserValues().stream().map(CrmUser::id).collect(Collectors.toSet()));
        }

        deal.setUserIds(userIds);
        final Map<Long, CrmUser> usersById = getUsersById(deal.orgId(), userIds);

        deal.setCreatedBy(usersById.get(createdBy.id()));
        deal.setUpdatedBy(usersById.get(updatedBy.id()));
        deal.setSalesOwner(usersById.get(salesOwner.id()));
    }

    /**
     * Gets the primary preview details based on deal primary or secondary id
     *
     * @param dealId        Represents the deal id
     * @param dealTable     Represents the deal table
     * @param dataContainer Holds the updated information of user defined and system field values
     * @return The primary {@link Deal} preview details
     */
    private Deal getPreviewDetails(final Long dealId, final Table dealTable, final DataContainer dataContainer) {
        final Row dealRow = dataContainer.getRow(dealTable, new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));
        final Deal deal = dealManager.convertRowToModel(dealRow, dataContainer);

        setDealSummaryInfo(deal);

        return deal;
    }

    /**
     * Merges the deals for preview details
     *
     * @param primaryDeal   An entity that holds the attributes of primary deal
     * @param secondaryDeal An entity that holds the attributes of secondary deal
     */
    private void mergeDealsForPreview(final Deal primaryDeal, final Deal secondaryDeal) {
        final SummaryInfo primaryDealSummaryInfo = primaryDeal.getSummaryInfo();
        final ActivitySummary primaryActivitySummary = primaryDealSummaryInfo.getActivitySummary();
        final SummaryInfo secondaryDealSummaryInfo = secondaryDeal.getSummaryInfo();
        final ActivitySummary secondaryDealSummaryInfoActivitySummary = secondaryDealSummaryInfo.getActivitySummary();
        final Collection<Tag> primaryTags = primaryDealSummaryInfo.getTags();
        final Collection<Tag> secondaryTags = secondaryDealSummaryInfo.getTags();
        final EmailSummary primaryEmailSummary = primaryDealSummaryInfo.getEmailSummary();
        final EmailSummary secondaryEmailSummary = secondaryDealSummaryInfo.getEmailSummary();
        primaryDeal.setSource(preferPrimaryValue(primaryDeal.getSource(), secondaryDeal.getSource()));
        primaryDeal.setDescription(preferPrimaryValue(primaryDeal.getDescription(), secondaryDeal.getDescription()));
        primaryDeal.setType(preferPrimaryValue(primaryDeal.getType(), secondaryDeal.getType()));
        primaryDeal.setPaymentStatus(preferPrimaryValue(primaryDeal.getPaymentStatus(), secondaryDeal.getPaymentStatus()));
        primaryDeal.setCompany(preferPrimaryValue(primaryDeal.getCompany(), secondaryDeal.getCompany()));
        primaryActivitySummary.setNextActivityDate(preferPrimaryValue(primaryActivitySummary.getNextActivityDate(),
                secondaryDealSummaryInfoActivitySummary.getNextActivityDate()));
        primaryActivitySummary.setLastActivityDate(preferPrimaryValue(primaryActivitySummary.getLastActivityDate(),
                secondaryDealSummaryInfoActivitySummary.getLastActivityDate()));
        primaryActivitySummary.setLastActivityType(preferPrimaryValue(primaryActivitySummary.getLastActivityType(),
                secondaryDealSummaryInfoActivitySummary.getLastActivityType()));
        primaryDeal.setExpectedCloseDate(preferPrimaryValue(primaryDeal.getExpectedCloseDate(), secondaryDeal.getExpectedCloseDate()));
        primaryDeal.setRecentNote(preferPrimaryValue(primaryDeal.getRecentNote(), secondaryDeal.getRecentNote()));
        primaryDeal.setWeightedRevenue(preferPrimaryValue(primaryDeal.getWeightedRevenue(), secondaryDeal.getWeightedRevenue()));
        primaryDeal.setParticipants(mergeParticipantsForPreview(primaryDeal, secondaryDeal));
        primaryDealSummaryInfo.setTags(mergeUniqueByKey(primaryTags, secondaryTags, Tag::id));
        primaryEmailSummary.setLastEmailReceived(preferPrimaryValue(primaryEmailSummary.getLastEmailReceived(),
                secondaryEmailSummary.getLastEmailReceived()));
        primaryEmailSummary.setLastEmailSent(preferPrimaryValue(primaryEmailSummary.getLastEmailSent(),
                secondaryEmailSummary.getLastEmailSent()));
        primaryActivitySummary.setPendingActivities(sumNullableLong(primaryActivitySummary.getPendingActivities(),
                secondaryDealSummaryInfoActivitySummary.getPendingActivities()));
        primaryActivitySummary.setUpcomingActivities(sumNullableLong(primaryActivitySummary.getUpcomingActivities(),
                secondaryDealSummaryInfoActivitySummary.getUpcomingActivities()));
        primaryActivitySummary.setDoneActivities(sumNullableLong(primaryActivitySummary.getDoneActivities(),
                secondaryDealSummaryInfoActivitySummary.getDoneActivities()));
        primaryActivitySummary.setTotalActivities(primaryActivitySummary.getDoneActivities()
                + primaryActivitySummary.getPendingActivities());
        primaryDeal.setTotalProductCount(sumNullableLong(primaryDeal.getTotalProductCount(), secondaryDeal.getTotalProductCount()));
        primaryEmailSummary.setEmailMessageCount(sumNullableLong(primaryEmailSummary.getEmailMessageCount(), secondaryEmailSummary.getEmailMessageCount()));
        primaryDealSummaryInfo.setEmailSummary(primaryEmailSummary);
        primaryDealSummaryInfo.setActivitySummary(primaryActivitySummary);
        primaryDeal.setSummaryInfo(primaryDealSummaryInfo);
        mergeCustomFields(primaryDeal, secondaryDeal);
        primaryDeal.setDescription(preferPrimaryValue(primaryDeal.getDescription(), secondaryDeal.getDescription()));
        primaryDeal.setSource(preferPrimaryValue(primaryDeal.getSource(), secondaryDeal.getSource()));
        primaryDeal.setType(preferPrimaryValue(primaryDeal.getType(), secondaryDeal.getType()));
        primaryDeal.setPaymentStatus(preferPrimaryValue(primaryDeal.getPaymentStatus(), secondaryDeal.getPaymentStatus()));
        primaryDeal.setTerritory(preferPrimaryValue(primaryDeal.getTerritory(), secondaryDeal.getTerritory()));
        primaryDeal.setForecastCategory(preferPrimaryValue(primaryDeal.getForecastCategory(), secondaryDeal.getForecastCategory()));
        primaryDeal.setDealValue((primaryDeal.getDealValue() == null || primaryDeal.getDealValue().getValue().compareTo(BigDecimal.ZERO) == 0) ?
                secondaryDeal.getDealValue() : primaryDeal.getDealValue());
        primaryDeal.setExpectedDealValue((primaryDeal.getExpectedDealValue() == null || primaryDeal.getExpectedDealValue().getValue().compareTo(BigDecimal.ZERO) == 0) ?
                secondaryDeal.getExpectedDealValue() : primaryDeal.getExpectedDealValue());
        primaryDeal.setDealValueInBaseCurrency((primaryDeal.getDealValueInBaseCurrency() == null || primaryDeal.getDealValueInBaseCurrency().getValue().compareTo(BigDecimal.ZERO) == 0) ?
                secondaryDeal.getDealValueInBaseCurrency() : primaryDeal.getDealValueInBaseCurrency());
        primaryDeal.setWeightedRevenue((primaryDeal.getWeightedRevenue() == null || primaryDeal.getWeightedRevenue().getValue().compareTo(BigDecimal.ZERO) == 0) ?
                secondaryDeal.getWeightedRevenue() : primaryDeal.getWeightedRevenue());
    }

    /**
     * Sets the {@link SummaryInfo} details into {@link Deal}
     *
     * @param deal An entity that holds the attributes of deal
     */
    private void setDealSummaryInfo(final Deal deal) {
        final SummaryInfo summaryInfo = new SummaryInfo();
        final Long orgId = deal.orgId();
        final Long id = deal.id();
        final String entityType = Deal.ENTITY_TYPE;

        summaryInfo.setActivitySummary(sourceActivityManager.getActivitySummary(orgId, id, entityType));
        summaryInfo.setWebFormName(dealManager.getWebFormName(orgId, deal.id()));
        summaryInfo.setTags(sourceTagManager.getMappedTags(orgId, id, entityType));
        summaryInfo.setEmailSummary(EmailSummaryActionService.getInstance().get(orgId, id, Source.DEAL));

        deal.setSummaryInfo(summaryInfo);
    }

    /**
     * Merges the primary and secondary deal
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param primaryId   Represents the primary id
     * @param secondaryId Represents the secondary id
     * @return Response of the Merged deals
     */
    public Map<String, Object> mergeDeals(final Long orgId, final long userId, final Long primaryId, final Long secondaryId) {
        final Table dealTable = dealManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Set<Long> dealIds = Set.of(primaryId, secondaryId);
        final Column dealPkColumn = dealTable.getPKColumn();
        final Map<String, Object> response = new HashMap<>();
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final DataContainer targetContainer = DataContainer.create();
        final Column detailHolderDealIdColumn = productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID);
        final DataContainer dataContainer = getDataContainer(orgId, dealIds);
        final Row primaryDealRow = dataContainer.getRow(dealTable, new WhereClause(Criteria.eq(dealPkColumn, primaryId)));
        final Row secondaryDealRow = dataContainer.getRow(dealTable, new WhereClause(Criteria.eq(dealPkColumn, secondaryId)));
        final Long secondaryDealMonetaryId = secondaryDealRow.get(MDODEAL.MONETARY_ID);
        final Set<String> columnNames = dealTable.getTableDefinition().getColumns();

        for (final String columnName : columnNames) {
            final Object primaryValue = primaryDealRow.get(columnName);
            final Object secondaryValue = secondaryDealRow.get(columnName);

            if (Objects.nonNull(secondaryValue)) {
                if (Objects.isNull(primaryValue) || (primaryValue instanceof Number
                        && NumericalConstant.ZERO == ((Number) primaryValue).doubleValue())) {
                    primaryDealRow.set(columnName, secondaryValue);
                }
            }
        }

        targetContainer.updateRow(primaryDealRow);
        final Long primaryMonetaryId = primaryDealRow.get(MDODEAL.MONETARY_ID);
        final Row secondaryMonetaryRow = dataContainer.getRow(monetaryTable, secondaryDealMonetaryId);
        boolean isPrimaryDealValueChanged = false;

        if (Objects.nonNull(primaryMonetaryId) && Objects.nonNull(secondaryDealMonetaryId)
                && !primaryMonetaryId.equals(secondaryDealMonetaryId)) {
            final Row primaryMonetaryRow = dataContainer.getRow(monetaryTable, primaryMonetaryId);

            if (Objects.nonNull(primaryMonetaryRow) && Objects.nonNull(secondaryMonetaryRow)) {
                for (final String columnName : monetaryTable.getTableDefinition().getColumns()) {
                    final Object secondaryValue = secondaryMonetaryRow.get(columnName);
                    final Object primaryValue = primaryMonetaryRow.get(columnName);

                    if (Objects.nonNull(secondaryValue) && (Objects.isNull(primaryValue)
                            || (TypeChecker.isNumber(primaryValue) && NumericalConstant.ZERO == NumberUtils.parseNumber(primaryValue).doubleValue()))) {
                        primaryMonetaryRow.set(columnName, secondaryValue);
                        isPrimaryDealValueChanged = true;
                    }
                }
                targetContainer.updateRow(primaryMonetaryRow);
            }
        }
        final Deal primaryDeal = new Deal();

        primaryDeal.setId(primaryId);
        primaryDeal.setOrgId(orgId);
        loadStandardMergeRowsIntoContainer(orgId, Source.DEAL, primaryId, List.of(secondaryId), targetContainer, List.of(), List.of());
        loadDealCompanyMergeIntoContainer(orgId, primaryId, secondaryId, targetContainer);
        loadParticipantMergeIntoContainer(orgId, primaryId, secondaryId, targetContainer);
        loadDealDropdownFieldsIntoContainer(orgId, primaryId, secondaryId, dataContainer, targetContainer);
        loadCustomFieldRowsIntoContainer(primaryDeal, List.of(secondaryId), dataContainer, targetContainer, Deal.ENTITY_TYPE);
        loadDealTimeline(orgId, userId, primaryId, secondaryId, dataContainer, targetContainer);
        loadDealDefaultOptionFields(primaryId, secondaryId, dataContainer, targetContainer);
        loadDealRow(primaryId, secondaryId, dataContainer, targetContainer);

         if (isPrimaryDealValueChanged) {
             final Row productDetailHolderRow = dataContainer.getRow(productDetailsHolderTable, new WhereClause(Criteria.eq(detailHolderDealIdColumn, secondaryId)));

             if (Objects.nonNull(productDetailHolderRow)) {
                 productDetailHolderRow.set(OMDOPRODUCTDETAILSHOLDER.DEAL_ID, primaryId);
                 targetContainer.updateRow(productDetailHolderRow);
             }
         }

        dataContainer.append(targetContainer);
        final Deal mergedDeal = dealManager.convertRowToModel(primaryDealRow, dataContainer);

        targetContainer.deleteRow(secondaryDealRow);
        modificationHandler.loadModificationDetailsIntoContainer(orgId, userId, List.of(primaryId), Source.DEAL, targetContainer);

        if (Objects.nonNull(secondaryMonetaryRow)) {
            targetContainer.deleteRow(secondaryMonetaryRow);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
        response.put(DealFields.MERGED_RECORDS, secondaryId);
        response.put(DealFields.DEAL, mergedDeal);

        return response;
    }

    /**
     * <p>
     * Loads merge query for deal company mapping only when primary does not already have a company.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param primaryId       Represents the primary deal id
     * @param secondaryId     Represents the secondary deal id
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadDealCompanyMergeIntoContainer(final Long orgId, final Long primaryId, final Long secondaryId,
                                                   final DataContainer targetContainer) {
        final Table dealCompanyTable = dealManager.getDealCompanyTable();
        final Column entityIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID);
        final Column fieldIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID);
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, DealFields.COMPANY);
        final Long dealCompanyFieldId = fields.get(DealFields.COMPANY).id();

        final WhereClause primaryWhereClause = new WhereClause(Criteria.eq(entityIdColumn, primaryId))
                .and(Criteria.eq(fieldIdColumn, dealCompanyFieldId));

        if (orgDataStore(orgId).get(dealCompanyTable, primaryWhereClause).isEmpty()) {
            final WhereClause secondaryWhereClause = new WhereClause(Criteria.eq(entityIdColumn, secondaryId))
                    .and(Criteria.eq(fieldIdColumn, dealCompanyFieldId));

            orgDataStore(orgId).get(dealCompanyTable, secondaryWhereClause).getRows(dealCompanyTable).forEach(row -> {
                row.set(entityIdColumn, primaryId);
                targetContainer.updateRow(row);
            });
        }
    }

    /**
     * <p>
     * Loads dedupe-safe merge queries for deal participants.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param primaryId       Represents the primary deal id
     * @param secondaryId     Represents the secondary deal id
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadParticipantMergeIntoContainer(final long orgId, final Long primaryId, final Long secondaryId,
                                                   final DataContainer targetContainer) {
        super.loadDeduplicatedMappingRowsIntoContainer(orgId, primaryId, List.of(secondaryId), MDOPARTICIPANT.DEAL_ID,
                MDOPARTICIPANT.CONTACT_ID, participantManager.getTable(), targetContainer);
    }


    /**
     * <p>
     * Gets the deal data container
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param dealIds Contains the collection of deal ids
     * @return The {@link DataContainer}
     */
    private DataContainer getDataContainer(final Long orgId, final Collection<Long> dealIds) {
        final Table dealTable = dealManager.getTable();
        final Optional<SelectQuery> optionalSelectQuery = dealManager.buildDealBaseQuery(dealTable, dealIds);

        if (optionalSelectQuery.isEmpty()) {
            return DataContainer.create();
        }
        final SelectQuery selectQuery = optionalSelectQuery.get();
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailsTable = productDetailsHolderManager.getProductDetailTable();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(dealTable.getPKColumn(), productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), leftJoin),
                new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), leftJoin)));
        selectQuery.addSelectClause(productDetailsHolderTable, productDetailsTable);

        return dealManager.fetchDealDataContainer(false, orgId, dealIds, dealTable, selectQuery);
    }

    /**
     * <p>
     * Loads the deal related timelines into the target {@link DataContainer}.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param dealId          Represents the primary deal id
     * @param secondaryId     Represents the secondary/duplicate deal id
     * @param dataContainer   Holds the information of existing deal attributes
     * @param targetContainer Holds the updated information of deal attributes
     */
    private void loadDealTimeline(final long orgId, final long userId, final Long dealId, final Long secondaryId,
                                  final DataContainer dataContainer, final DataContainer targetContainer) {
        final Table dealTable = dealManager.getTable();
        final Row primaryRow = dataContainer.getRow(dealTable, dealId);

        if (Objects.nonNull(primaryRow)) {
            final Row secondaryRow = dataContainer.getRow(dealTable, secondaryId);

            if (Objects.nonNull(secondaryRow)) {
                final String title = secondaryRow.get(MDODEAL.TITLE);

                loadMergeTimelineIntoContainer(orgId, userId, Timeline.Category.DEAL, List.of(secondaryId), dealId,
                        new FieldTimeline(List.of(title), (String) primaryRow.get(MDODEAL.TITLE), new I18nKey("i18n.deal.fields.title")),
                        dataContainer, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Loads deal's drop down fields into {@link DataContainer}.
     * </p>
     *
     * @param orgId           Represents an org id.
     * @param primaryId       Primary deal id.
     * @param secondaryId     Secondary deal id.
     * @param dataContainer   {@link DataContainer}.
     * @param targetContainer Target {@link DataContainer}.
     */
    private void loadDealDropdownFieldsIntoContainer(final Long orgId, final Long primaryId, final Long secondaryId,
                                                     final DataContainer dataContainer, final DataContainer targetContainer) {
        final Collection<FieldInfo> dropdownFields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, List.of(
                DealFields.TYPE, DealFields.PAYMENT_STATUS, DealFields.WON_REASON,
                DealFields.LOST_REASON, DealFields.DEAL_SOURCE, DealFields.FORECAST_CATEGORY)).values();

        for (final FieldInfo fieldInfo : dropdownFields) {
            loadDropdownFieldRowsIntoContainer(orgId, fieldInfo, true, primaryId, List.of(secondaryId), dataContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the primary deal's default option field rows ({@code dealSource}, {@code type},
     * {@code paymentStatus}) based on {@code mergeDealsForPreview} conditions — only when the
     * primary has no row for that field and the secondary does.
     * </p>
     *
     * @param primaryId       Represents the primary deal id
     * @param secondaryId     Represents the secondary deal id
     * @param dataContainer   Holds the existing deal row data
     * @param targetContainer Holds the rows to be committed
     */
    private void loadDealDefaultOptionFields(final Long primaryId, final Long secondaryId,
                                             final DataContainer dataContainer, final DataContainer targetContainer) {
        final Map<String, FieldInfo> fieldInfoMap = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE,
                DealFields.DEAL_SOURCE, DealFields.TYPE, DealFields.PAYMENT_STATUS);
        final Table fieldValueTable = Table.get(MDODROPDOWNFIELDS.TABLE);

        for (final FieldInfo fieldInfo : fieldInfoMap.values()) {
            final Long fieldId = fieldInfo.id();
            final Column entityIdColumn = fieldValueTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID);
            final Column fieldIdColumn = fieldValueTable.getColumn(MDODROPDOWNFIELDS.FIELD_ID);
            final Criteria fieldCriteria = Criteria.eq(fieldIdColumn, fieldId);
            final Row primaryRow = dataContainer.getRow(fieldValueTable,
                    new WhereClause(Criteria.eq(entityIdColumn, primaryId)).and(fieldCriteria));

            if (Objects.isNull(primaryRow)) {
                final Row secondaryRow = dataContainer.getRow(fieldValueTable,
                        new WhereClause(Criteria.eq(entityIdColumn, secondaryId)).and(fieldCriteria));

                if (Objects.nonNull(secondaryRow)) {
                    secondaryRow.set(MDODROPDOWNFIELDS.ENTITY_ID, primaryId);
                    targetContainer.updateRow(secondaryRow);
                }
            }
        }
    }

    /**
     * <p>
     * Loads the primary deal table row with secondary deal values based on
     * {@code mergeDealsForPreview} conditions — only when the primary value is null
     * and the secondary value is non-null.
     * </p>
     * <p>Fields handled: {@code EXPECTED_CLOSE_DATE}, {@code RECENT_NOTE}, {@code DESCRIPTION}</p>
     *
     * @param primaryId       Represents the primary deal id
     * @param secondaryId     Represents the secondary deal id
     * @param dataContainer   Holds the existing deal row data
     * @param targetContainer Holds the rows to be committed
     */
    private void loadDealRow(final Long primaryId, final Long secondaryId, final DataContainer dataContainer,
                             final DataContainer targetContainer) {
        final Table dealTable = dealManager.getTable();
        final Row primaryRow = dataContainer.getRow(dealTable, primaryId);
        final Row secondaryRow = dataContainer.getRow(dealTable, secondaryId);

        if (Objects.nonNull(primaryRow) && Objects.nonNull(secondaryRow)) {
            primaryRow.set(MDODEAL.EXPECTED_CLOSE_DATE, preferPrimaryValue(primaryRow.get(MDODEAL.EXPECTED_CLOSE_DATE),
                    secondaryRow.get(MDODEAL.EXPECTED_CLOSE_DATE)));
            primaryRow.set(MDODEAL.RECENT_NOTE, preferPrimaryValue(primaryRow.get(MDODEAL.RECENT_NOTE),
                    secondaryRow.get(MDODEAL.RECENT_NOTE)));
            primaryRow.set(MDODEAL.DESCRIPTION, preferPrimaryValue(primaryRow.get(MDODEAL.DESCRIPTION),
                    secondaryRow.get(MDODEAL.DESCRIPTION)));

            targetContainer.updateRow(primaryRow);
        }
    }

    /**
     * Merges related contacts (participants) from both deals for the preview, folding in the
     * secondary deal's Primary Contact as a Related Contact, and excluding whichever contact
     * ends up as the merged deal's Primary Contact so it is never shown twice.
     *
     * @param primaryDeal   the primary deal (its Primary Contact wins after merge)
     * @param secondaryDeal the secondary deal
     * @return the merged, deduplicated collection of {@link Participant}s
     */
    private Collection<Participant> mergeParticipantsForPreview(final Deal primaryDeal, final Deal secondaryDeal) {
        final Contact mergedPrimaryContact = primaryDeal.getPrimaryContact();
        final Long mergedPrimaryContactId = Objects.nonNull(mergedPrimaryContact) ? mergedPrimaryContact.id() : null;
        final Map<Long, Participant> merged = new LinkedHashMap<>();

        Stream.concat(primaryDeal.getParticipants().stream(), secondaryDeal.getParticipants().stream())
                .forEach(participant -> merged.putIfAbsent(participant.getContact().id(), participant));

        final Contact secondaryPrimaryContact = secondaryDeal.getPrimaryContact();

        if (Objects.nonNull(secondaryPrimaryContact) && !secondaryPrimaryContact.id().equals(mergedPrimaryContactId)) {
            final Participant secondaryPrimaryAsParticipant = new Participant();

            secondaryPrimaryAsParticipant.setContactId(secondaryPrimaryContact.id());
            secondaryPrimaryAsParticipant.setContact(secondaryPrimaryContact);
            secondaryPrimaryAsParticipant.setDealId(primaryDeal.id());
            secondaryPrimaryAsParticipant.setOrgId(primaryDeal.orgId());
            merged.putIfAbsent(secondaryPrimaryContact.id(), secondaryPrimaryAsParticipant);
        }

        if (Objects.nonNull(mergedPrimaryContactId)) {
            merged.remove(mergedPrimaryContactId);
        }

        return merged.isEmpty() ? null : merged.values();
    }
}
