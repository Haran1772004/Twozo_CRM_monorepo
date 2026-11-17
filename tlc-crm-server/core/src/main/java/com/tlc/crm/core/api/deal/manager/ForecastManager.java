package com.tlc.crm.core.api.deal.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.api.deal.model.ForecastSummary;
import com.tlc.crm.core.api.deal.model.ForecastView;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.sql.resource.OMDOFORECASTVIEW;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.entity.sql.resource.OMQFFORECASTCATEGORY;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.LimitClause;

import com.tlc.validator.TlcModel;
import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.deal.ForecastFields;
import com.tlc.crm.core.internal.deal.status.ForecastErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;

import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.models.EntityManagerHolder;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.sql.resource.OMETDATERANGE;
import com.tlc.crm.entity.sql.resource.OMETENTITY;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.OrderByClause.OrderType;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *     Manages the {@link Deal} forecast related actions.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public class ForecastManager {

    private static final String TOTAL_VALUE = "TOTAL_VALUE";
    private static final String DEAL_COUNT = "DEAL_COUNT";
    private static final String STATE = "STATE";

    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final FieldService fieldService;
    private final MonetaryManager monetaryManager;
    private final EntityManagerHolder entityManagerHolder;
    private final SourceActivityManager sourceActivityManager;
    private final CompanyManager companyManager;
    private final FilterCriteriaHandler filterCriteriaHandler;
    private final ParticipantManager participantManager;
    private final BaseCurrencyService baseCurrencyService;
    private final PipelineStageManager pipelineStageManager;
    private final ForecastCategoryManager forecastCategoryManager;

    private ForecastManager() {
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        fieldService = FieldService.getSecureInstance();
        monetaryManager = MonetaryManager.getInstance();
        entityManagerHolder = EntityManagerHolder.getInstance();
        sourceActivityManager = SourceActivityManager.getInstance();
        companyManager = CompanyManager.getInstance();
        filterCriteriaHandler = FilterCriteriaHandler.getInstance();
        participantManager = ParticipantManager.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        forecastCategoryManager = ForecastCategoryManager.getInstance();
    }

    private static class Instance {
        private static final ForecastManager INSTANCE = new ForecastManager();
    }

    /**
     * <p>
     *     Gets the {@link ForecastManager} instance.
     * </p>
     *
     * @return The instance of {@link ForecastManager}.
     */
    public static ForecastManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     *     Returns the {@link OrgDataStore}.
     * </p>
     *
     * @param orgId Refers the organization id.
     * @return The {@link OrgDataStore}.
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     *     Returns the {@link Table}.
     * </p>
     *
     * @return The Table of {@code OMDOFORECASTVIEW}.
     */
    private Table getForecastViewTable() {
        return Table.get(OMDOFORECASTVIEW.TABLE);
    }

    /**
     * <p>
     * Fetches the deals between the start and end date in the pipeline ids
     * and pipeline stage ids provided and sort the deals by deal state.
     * </p>
     *
     * @param orgId              Refers the organization id.
     * @param pipelineIds        Which contains the pipeline ids.
     * @param pipelineStagesIds  Which contains the pipeline stages ids.
     * @param fieldId            Represents the field id
     * @param interval           The {@link Interval} of start and end date.
     * @param arrangeBy          Refers the field by which the deals are sorted.
     * @param start              Represents the start value
     * @param limit              Represents the limit value
     * @param filterCriteriaList Represents the {@link Collection} of {@link FilterCriteria}
     * @return The {@link Collection} of deals.
     */
    public final Map<Deal, Collection<Activity>> getDealsByPipelineAndStage(final Long orgId, final Set<Long> pipelineIds,
                                                                            final Set<Long> pipelineStagesIds, final Long fieldId,
                                                                            final Interval<Long, Long> interval, final String arrangeBy,
                                                                            final int start, final int limit,
                                                                            final Collection<FilterCriteria> filterCriteriaList) {
        final Table dealTable = dealManager.getTable();
        final Table contactTable = contactManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table dealCompanyTable = dealManager.getDealCompanyTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final SelectQuery innerQuery = SelectQuery.get(dealTable);
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column companyPkColumn = companyTable.getPKColumn();
        final Column stateColumn = dealTable.getColumn(MDODEAL.STATE);
        final Long startDate = interval.start();
        final Long endDate = interval.end();
        final Map<Deal, Collection<Activity>> dealMap = new LinkedHashMap<>();
        final OrderType ascType = OrderType.ASCENDING;
        final OrderType orderType = Objects.equals(arrangeBy.toLowerCase(), ForecastFields.OPEN) ? ascType : OrderType.DESCENDING;
        final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);
        final StoreType storeType = fieldInfo.storeType();

        innerQuery.addSelectClause(dealPkColumn);
        innerQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineIds))
                .and(Criteria.in(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pipelineStagesIds))
                .and(Criteria.eq(dealTable.getColumn(MDODEAL.STATUS), Deal.Status.ACTIVE.getId())));
        loadJoinsAndSelectClause(selectQuery, contactTable, dealTable, monetaryTable);
        final Consumer<Column> orderByAppender = (column) -> selectQuery.addOrderByClause(List.of(
                new OrderByClause(stateColumn, orderType), new OrderByClause(column, ascType)));
        final WhereClause whereClause = new WhereClause(Criteria.in(dealPkColumn, innerQuery))
                .and(Criteria.notEq(stateColumn, Deal.State.LOST.getId()));

        if (fieldInfo.isPersistent()) {
            final Column fieldColumn = fieldInfo.column();

            selectQuery.addJoinClause(new JoinClause(dealPkColumn, fieldInfo.table()
                    .getColumn(OMETENTITY.ENTITY_ID)));

            switch (storeType) {
                case DATE -> {
                    selectQuery.setWhereClause(whereClause.and(Criteria.between(fieldColumn, startDate, endDate)));
                    orderByAppender.accept(fieldColumn);
                }
                case DATE_RANGE -> {
                    if (entityManagerHolder.getManager(storeType) instanceof
                            final AbstractConfigManager<? extends TlcModel> abstractConfigManager) {
                        final Table relatedTable = abstractConfigManager.getTable();
                        final Column dateRangeColumn = relatedTable.getColumn(OMETDATERANGE.TO);

                        selectQuery.addJoinClause(new JoinClause(fieldColumn, relatedTable.getPKColumn()));
                        selectQuery.setWhereClause(whereClause.and(new WhereClause(Criteria.between(dateRangeColumn,
                                startDate, endDate))));
                        orderByAppender.accept(dateRangeColumn);
                    }
                }
                default -> throw ErrorCode.get(ForecastErrorCodes.INVALID_FORECAST_FIELD_STORE_TYPE);
            }
        } else {
            final Consumer<Column> criteriaColumnAppender = (column) -> {
                selectQuery.setWhereClause(whereClause.and(Criteria.between(column, startDate, endDate)));
                orderByAppender.accept(column);
            };

            switch (fieldInfo.aliasName()) {
                case "dealCreated" -> criteriaColumnAppender.accept(dealTable.getColumn(MDODEAL.CREATED_TIME));
                case "expectedCloseDate" -> {
                    final Column expectedClosedDateColumn = dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE);
                    final WhereClause closedTimeCriteria = new WhereClause(Criteria.eq(stateColumn, Deal.State.WON.id()))
                            .and(Criteria.between(dealTable.getColumn(MDODEAL.CLOSED_TIME), startDate, endDate));
                    final WhereClause expectedCloseDateCriteria = new WhereClause(Criteria.eq(stateColumn, Deal.State.OPEN.id()))
                            .and(Criteria.between(expectedClosedDateColumn, startDate, endDate));

                    selectQuery.setWhereClause(whereClause.and(closedTimeCriteria.or(expectedCloseDateCriteria)));
                    orderByAppender.accept(expectedClosedDateColumn);
                }
            }
        }
        final String companyKey = DealFields.COMPANY;
        final FieldInfo companyFieldInfo = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey).get(companyKey);
        final Table companyDerivedTable = companyManager.getCompanyFieldTable(companyFieldInfo, dealCompanyTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Table forecastCategoryTable = forecastCategoryManager.getTable();

        selectQuery.addJoinClause(List.of(new JoinClause(dealPkColumn, companyDerivedTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(companyDerivedTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyPkColumn, leftJoin),
                new JoinClause(dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID), forecastCategoryTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(List.of(companyPkColumn, companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.ORG_ID),
                forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.NAME)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addSelectClause(companyDerivedTable);

        if (Objects.nonNull(filterCriteriaList) && !filterCriteriaList.isEmpty()) {
            filterCriteriaHandler.applyCriteria(orgId, Provider.DEAL_LIST, selectQuery, filterCriteriaList);
        }
        final DataContainer entityContainer = orgDataStore(orgId).get(selectQuery);

        if (!entityContainer.isEmpty()) {
            final Table dealActivityTable = sourceActivityManager.getDealActivityTable();
            final Table activityTable = sourceActivityManager.getTable();
            final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
            final Column activityDealIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID);
            final Column isDoneColumn = activityTable.getColumn(OMACACTIVITY.IS_DONE);
            final Collection<Row> dealRows = entityContainer.getRows(dealTable).toList();
            final SelectQuery activitySelectQuery = SelectQuery.get(activityTable);

            activitySelectQuery.addJoinClause(new JoinClause(activityTable.getPKColumn(), dealActivityIdColumn));
            activitySelectQuery.addSelectClause(List.of(activityDealIdColumn, dealActivityIdColumn,
                    activityTable.getColumn(OMACACTIVITY.TITLE), activityTable.getColumn(OMACACTIVITY.ORG_ID),
                    isDoneColumn, activityTable.getColumn(OMACACTIVITY.START_TIME), activityTable.getColumn(OMACACTIVITY.END_TIME),
                    activityTable.getColumn(OMACACTIVITY.CREATOR_ID)));
            activitySelectQuery.setWhereClause(new WhereClause(Criteria.in(activityDealIdColumn,
                    dealRows.stream().map(Row::getPKValue).collect(Collectors.toUnmodifiableSet())))
                    .and(Criteria.eq(isDoneColumn, false)));

            final DataContainer activityDataContainer = orgDataStore(orgId).get(activitySelectQuery);

            final Collection<Long> contactIds = dealRows.stream().map(dealRow -> (Long) dealRow.get(MDODEAL.CONTACT_ID)).filter(Objects::nonNull).collect(Collectors.toSet());
            final DataContainer contactMediaContainer = buildContactMediaContainer(orgId, contactIds);

            dealRows.forEach(dealRow -> {
                final Deal deal = convertRowToPartialModel(dealRow, entityContainer, monetaryTable, companyDerivedTable, contactMediaContainer);

                dealMap.put(deal, dealManager.getRelatedActivities(deal.id(), activityDataContainer));
            });
        }

        return dealMap;
    }

    /**
     * <p>
     *     Loads the select and join clauses into the {@link SelectQuery}.
     * </p>
     *
     * @param selectQuery   Refers the selectQuery to fetch deal data.
     * @param dealTable     Refers the deal table.
     * @param monetaryTable Refers the monetary table.
     */
    private void loadJoinsAndSelectClause(final SelectQuery selectQuery, final Table contactTable, final Table dealTable,
                                          final Table monetaryTable) {
        final Column monetaryId = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column contactId = dealTable.getColumn(MDODEAL.CONTACT_ID);
        final Table companyTable = companyManager.getTable();
        final Table participantTable = ParticipantManager.getInstance().getTable();
        final Column participantDealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
        final Column participantContactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
        final Table pipelineTable = Table.get(MDOPIPELINE.TABLE);
        final Column pipelineIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_ID);

        selectQuery.addJoinClause(List.of(new JoinClause(monetaryId, monetaryTable.getPKColumn()),
                new JoinClause(dealPkColumn, participantDealIdColumn),
                new JoinClause(participantContactIdColumn, contactPkColumn),
                new JoinClause(pipelineIdColumn, pipelineTable.getPKColumn())));
        selectQuery.addSelectClause(List.of(dealPkColumn, dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID),
                dealTable.getColumn(MDODEAL.ORG_ID), monetaryId, contactId, dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE),
                dealTable.getColumn(MDODEAL.STATE), contactPkColumn, contactTable.getColumn(MCOCONTACT.ORG_ID),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE),
                monetaryTable.getColumn(OMETMONETARY.VALUE), monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID),
                monetaryTable.getColumn(OMETMONETARY.ORG_ID), companyTable.getPKColumn(), companyTable.getColumn(MCOCOMPANY.NAME),
                participantDealIdColumn, participantContactIdColumn, pipelineIdColumn, dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID),
                pipelineTable.getColumn(MDOPIPELINE.ROTTEN_DAYS), dealTable.getColumn(MDODEAL.MODIFIED_TIME)));
    }

    /**
     * <p>
     *     Builds the contact media container which contains the contact email and phone details.
     * </p>
     *
     * @param orgId      Refers the organization id.
     * @param contactIds Refers the contact ids whose media details to be fetched.
     * @return The {@link DataContainer} which contains the contact media details.
     */
    private DataContainer buildContactMediaContainer(final Long orgId, final Collection<Long> contactIds) {
        final Table contactTable = contactManager.getTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final Table emailTable = Table.get(OMETEMAIL.TABLE);
        final Table phonesTable = Table.get(OMETPHONE.TABLE);
        final Column contactPkColumn = contactTable.getPKColumn();
        final SelectQuery mediaQuery = SelectQuery.get(contactTable);

        mediaQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                new JoinClause(contactPkColumn, contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), JoinClause.JoinType.LEFT),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn(), JoinClause.JoinType.LEFT),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phonesTable.getPKColumn(), JoinClause.JoinType.LEFT)));
        mediaQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, contactIds)));
        mediaQuery.addSelectClause(List.of(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID),
                contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID),
                contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID)));
        mediaQuery.addSelectClause(emailTable, phonesTable);

        return orgDataStore(orgId).get(mediaQuery);
    }

    /**
     * <p>
     *     Converts the {@link Row} into {@link Deal}.
     * </p>
     *
     * @param dealRow               Row which contains the deal details.
     * @param dataContainer         A container that holds the deals' information.
     * @param monetaryTable         Refers the monetary table.
     * @param companyDerivedTable   Refers the company derived table
     * @param contactMediaContainer Refers the contact media container which contains the contact email and phone details.
     * @return {@link Deal}.
     */
    private Deal convertRowToPartialModel(final Row dealRow, final DataContainer dataContainer, final Table monetaryTable,
                                          final Table companyDerivedTable, final DataContainer contactMediaContainer) {
        final Deal deal = new Deal();
        final Long dealId = dealRow.getPKValue();
        final Table companyTable = companyManager.getTable();
        final Table forecastCategoryTable = forecastCategoryManager.getTable();
        final Row monetaryRow = dataContainer.getRow(monetaryTable, (Long) dealRow.get(MDODEAL.MONETARY_ID));
        final Row contactRow = dataContainer.getRow(contactManager.getTable(), (Long) dealRow.get(MDODEAL.CONTACT_ID));
        final Row dealCompanyRow = dataContainer.getRow(companyDerivedTable,
                new WhereClause(Criteria.eq(companyDerivedTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealId)));
        final Row companyRow = Objects.isNull(dealCompanyRow) ? null : dataContainer.getRow(companyTable,
                new WhereClause(Criteria.eq(companyTable.getPKColumn(), dealCompanyRow.get(MDOCOMPANYFIELDS.COMPANY_ID))));
        final Integer stateId = dealRow.get(MDODEAL.STATE);
        final Integer lostStateId = Deal.State.LOST.getId();
        final Table contactTable = contactManager.getTable();
        final Table participantTable = participantManager.getTable();
        final Table pipelineTable = Table.get(MDOPIPELINE.TABLE);
        final Row pipeLineRow = dataContainer.getRow(pipelineTable);
        final Long forecastCategoryId = dealRow.get(MDODEAL.FORECAST_CATEGORY_ID);
        final Long orgId = dealRow.getOrgId();

        if (Objects.nonNull(forecastCategoryId)) {
            final Row forecastCategoryRow = dataContainer.getRow(forecastCategoryTable, forecastCategoryId);
            final ForecastCategory forecastCategory = new ForecastCategory();

            forecastCategory.setId(forecastCategoryRow.getPKValue());
            forecastCategory.setName(forecastCategoryRow.get(OMQFFORECASTCATEGORY.NAME));
            deal.setForecastCategory(forecastCategory);
        }

        if (!lostStateId.equals(stateId)) {
            deal.setState(Deal.State.getStateById(stateId));
        }
        deal.setId(dealId);
        deal.setOrgId(orgId);
        deal.setTitle(dealRow.get(MDODEAL.TITLE));
        deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
        final Pipeline pipeline = new Pipeline();
        final PipelineStage pipelineStage = new PipelineStage();
        final Long pipelineId = dealRow.get(MDODEAL.PIPELINE_ID);
        final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);

        pipelineStage.setPipelineId(pipelineId);
        pipelineStage.setId(pipelineStageId);
        pipelineStage.setName(pipelineStageManager.getPipelineStageName(orgId, pipelineStageId));
        deal.setPipelineStage(pipelineStage);
        pipeline.setId(pipelineId);
        pipeline.setRottenDays(pipeLineRow.get(MDOPIPELINE.ROTTEN_DAYS));
        deal.setPipeline(pipeline);
        deal.setModifiedTime(dealRow.get(MDODEAL.MODIFIED_TIME));

        if (Objects.nonNull(contactRow)) {
            final Contact contact = contactManager.convertRowToModel(orgId, contactRow, true, dataContainer);
            final Long contactId = contact.id();
            final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                    ContactFields.PHONES, ContactFields.EMAILS);
            final Long phoneFieldId = fields.get(ContactFields.PHONES).id();
            final Long emailFieldId = fields.get(ContactFields.EMAILS).id();

            contactManager.buildPhones(contactId, contact, contactRow, phoneFieldId, contactMediaContainer);
            contactManager.buildEmails(contactId, contact, contactRow, emailFieldId, contactMediaContainer);
            deal.setPrimaryContact(contact);
        }

        if (Objects.nonNull(companyRow)) {
            deal.setCompany(companyManager.convertRowToModel(companyRow, true, dataContainer));
        }

        if (Objects.nonNull(monetaryRow)) {
            final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);

            deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));
        }

        final Stream<Row> participantRows = dataContainer.getRows(participantTable,
                new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId)));
        final Collection<Long> contactIds = participantRows.filter(Objects::nonNull).map(participantRow ->
                (Long) participantRow.get(MDOPARTICIPANT.CONTACT_ID)).collect(Collectors.toList());
        final Stream<Row> contactRows = dataContainer.getRows(contactTable,
                new WhereClause(Criteria.in(contactTable.getPKColumn(), contactIds)));
        final Collection<Participant> participants = new HashSet<>();

        contactRows.forEach(row -> {
            final Contact contact = contactManager.convertRowToPartialModel(row);
            final Participant participant = new Participant();

            participant.setContact(contact);
            participants.add(participant);
        });
        deal.setParticipants(participants);

        return deal;
    }

    /**
     * Retrieves a {@link ForecastSummary} for the specified organization and parameters.
     * <p>
     * This method aggregates or computes forecast data based on the provided field,
     * pipeline, stage, time interval, and filtering criteria. The returned
     * {@link ForecastSummary} encapsulates the computed forecast metrics.
     * </p>
     *
     * @param orgId              The unique identifier of the organization.
     * @param fieldId            The field id.
     * @param pipelineIds        The set of pipeline IDs.
     * @param pipelineStageIds   The set of pipeline stage IDs.
     * @param interval           An {@link Interval} representing the start and end values (e.g., a time range) for which the forecast is computed.
     * @param filterCriteriaList A collection of {@link FilterCriteria}.
     * @return A {@link ForecastSummary} containing the computed forecast metrics.
     */
    public ForecastSummary getSummary(final long orgId, final Long fieldId, final Collection<Long> pipelineIds,
                                      final Collection<Long> pipelineStageIds, final Interval<Long, Long> interval, final Collection<FilterCriteria> filterCriteriaList) {
        final Table dealTable = dealManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Column currencyIdColumn = monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID);
        final Column dealStateColumn = dealTable.getColumn(MDODEAL.STATE);
        final SelectQuery summarySelectQuery = SelectQuery.get(dealTable);

        summarySelectQuery.addSelectClause(List.of(currencyIdColumn, monetaryTable.getColumn(OMETMONETARY.VALUE, TOTAL_VALUE).sum(),
                dealTable.getColumn(MDODEAL.ID, DEAL_COUNT).countWithDistinct(), dealStateColumn));
        summarySelectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryTable.getPKColumn()));
        summarySelectQuery.addGroupByClause(new GroupByClause(currencyIdColumn));
        summarySelectQuery.addGroupByClause(new GroupByClause(dealStateColumn));
        applyQueryConstraints(orgId, fieldId, pipelineIds, pipelineStageIds, summarySelectQuery, interval, filterCriteriaList);
        final List<Map<String, Object>> dataContainer = orgDataStore(orgId).getData(summarySelectQuery);

        BigDecimal totalWonDealsValue = BigDecimal.ZERO;
        BigDecimal totalOpenDealsValue = BigDecimal.ZERO;
        long totalCount = 0;

        for (final Map<String, Object> row : dataContainer) {
            totalCount += (long) row.get(DEAL_COUNT);
            final short currencyId = (short) row.get(OMETMONETARY.CURRENCY_ID);
            final BigDecimal value = toBigDecimal(row.get(TOTAL_VALUE));
            final BigDecimal monetaryValue = baseCurrencyService.formatCalibratedValue(orgId, value, Currency.getCurrencyById(currencyId));

            switch (Deal.State.getStateById((int) row.get(STATE))) {
                case OPEN -> totalOpenDealsValue = totalOpenDealsValue.add(monetaryValue);
                case WON -> totalWonDealsValue = totalWonDealsValue.add(monetaryValue);
            }
        }

        return new ForecastSummary(totalCount, totalOpenDealsValue, totalWonDealsValue);
    }

    /**
     * Retrieves the total count of records matching the specified filters and criteria.
     *
     * @param orgId              The unique identifier of the organization.
     * @param pipelineIds        The set of pipeline IDs.
     * @param pipelineStageIds   The set of pipeline stage IDs.
     * @param fieldId            Refers the field id.
     * @param interval           An {@link Interval} representing the start and end values.
     * @param filterCriteriaList A collection of {@link FilterCriteria}.
     * @return The total count of records matching the specified filters and criteria.
     */
    public long getTotalCount(final Long orgId, final Set<Long> pipelineIds, final Set<Long> pipelineStageIds,
                              final Long fieldId, final Interval<Long, Long> interval, final Collection<FilterCriteria> filterCriteriaList) {
        final Table dealTable = dealManager.getTable();
        final SelectQuery countQuery = SelectQuery.get(dealTable);

        countQuery.addSelectClause(dealTable.getColumn(MDODEAL.ID, DEAL_COUNT).countWithDistinct());
        applyQueryConstraints(orgId, fieldId, pipelineIds, pipelineStageIds, countQuery, interval, filterCriteriaList);
        final List<Map<String, Object>> dataContainer = orgDataStore(orgId).getData(countQuery);

        return dataContainer.isEmpty() ? 0L : (long) dataContainer.get(0).get(DEAL_COUNT);
    }

    /**
     * Applies query constraints to the given {@link SelectQuery}. This method augments the provided {@link SelectQuery} by adding WHERE clauses,
     * joins, or other restrictions so that only records matching the specified parameters are included when the query is executed.
     *
     * @param orgId              The unique identifier of the organization.
     * @param fieldId            The identifier of the field.
     * @param pipelineIds        The collection of pipeline IDs.
     * @param pipelineStageIds   The collection of pipeline stage IDs.
     * @param selectQuery        The {@link SelectQuery} to which constraints will be applied.
     * @param interval           An {@link Interval} representing the start and end values.
     * @param filterCriteriaList A collection of {@link FilterCriteria} specifying additional filtering rules to apply to the query.
     */
    private void applyQueryConstraints(final long orgId, final long fieldId, final Collection<Long> pipelineIds,
                                       final Collection<Long> pipelineStageIds, final SelectQuery selectQuery,
                                       final Interval<Long, Long> interval, final Collection<FilterCriteria> filterCriteriaList) {
        final Table dealTable = dealManager.getTable();
        final Column dealPkColumn = dealTable.getPKColumn();
        final SelectQuery innerQuery = SelectQuery.get(dealTable);
        final Column stateColumn = dealTable.getColumn(MDODEAL.STATE);

        innerQuery.addSelectClause(dealPkColumn);
        innerQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineIds))
                .and(Criteria.in(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pipelineStageIds))
                .and(Criteria.eq(dealTable.getColumn(MDODEAL.STATUS), Deal.Status.ACTIVE.getId())));

        final WhereClause whereClause = new WhereClause(Criteria.in(dealPkColumn, innerQuery))
                .and(Criteria.notEq(stateColumn, Deal.State.LOST.getId()));
        final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);
        final StoreType storeType = fieldInfo.storeType();

        if (fieldInfo.isPersistent()) {
            final Column fieldColumn = fieldInfo.column();

            selectQuery.addJoinClause(new JoinClause(dealPkColumn, fieldInfo.table().getColumn(OMETENTITY.ENTITY_ID)));

            switch (storeType) {
                case DATE ->
                        selectQuery.setWhereClause(whereClause.and(Criteria.between(fieldColumn, interval.start(), interval.end())));
                case DATE_RANGE -> {
                    final Table relatedTable = ((AbstractConfigManager<?>) entityManagerHolder.getManager(storeType)).getTable();

                    selectQuery.addJoinClause(new JoinClause(fieldColumn, relatedTable.getPKColumn()));
                    selectQuery.setWhereClause(whereClause.and(new WhereClause(Criteria.between(relatedTable.getColumn(OMETDATERANGE.TO), interval.start(), interval.end()))));
                }
                default -> throw ErrorCode.get(ForecastErrorCodes.INVALID_FORECAST_FIELD_STORE_TYPE);
            }
        } else {
            switch (fieldInfo.aliasName()) {
                case "dealCreated" -> selectQuery.setWhereClause(whereClause.and(Criteria.between(dealTable.getColumn(MDODEAL.CREATED_TIME), interval.start(), interval.end())));
                case "expectedCloseDate" -> {
                    final Long startDate = interval.start();
                    final Long endDate = interval.end();
                    final WhereClause closedTimeCriteria = new WhereClause(Criteria.eq(stateColumn, Deal.State.WON.id()))
                            .and(Criteria.between(dealTable.getColumn(MDODEAL.CLOSED_TIME), startDate, endDate));
                    final WhereClause expectedCloseDateCriteria = new WhereClause(Criteria.eq(stateColumn, Deal.State.OPEN.id()))
                            .and(Criteria.between(dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), startDate, endDate));

                    selectQuery.setWhereClause(whereClause.and(closedTimeCriteria.or(expectedCloseDateCriteria)));
                }
            }
        }

        if (Objects.nonNull(filterCriteriaList) && !filterCriteriaList.isEmpty()) {
            filterCriteriaHandler.applyCriteria(orgId, Provider.DEAL_LIST, selectQuery, filterCriteriaList);
        }
    }

    /**
     * Retrieves the forecast view details for the specified organization and user.
     *
     * @param orgId  The unique identifier of the organization.
     * @param userId The unique identifier of the user.
     * @return An {@link Optional} containing the {@link ForecastView} for the given organization and user,
     */
    public Optional<ForecastView> getViewDetails(final long orgId, final long userId) {
        final Table table = getForecastViewTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(OMDOFORECASTVIEW.USER_ID), userId)));

        return Optional.ofNullable(dataContainer.getRow(table)).map(row -> {
            final ForecastView forecastView = new ForecastView(row.getOrgId(), row.get(OMDOFORECASTVIEW.USER_ID));

            forecastView.setId(row.getPKValue());
            forecastView.setConfig(row.get(OMDOFORECASTVIEW.CONFIG));

            return forecastView;
        });
    }

    /**
     * Updates the details of an {@link ForecastView}.
     *
     * @param forecastView The {@link ForecastView} containing updated details to be saved.
     */
    public void updateViewDetails(final ForecastView forecastView) {
        final Table table = getForecastViewTable();
        final DataContainer dataContainer = DataContainer.create();
        final Long orgId = forecastView.orgId();
        final Row existingRow = orgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMDOFORECASTVIEW.USER_ID), forecastView.getUserId()))).getRow(table);
        final Row row = Objects.isNull(existingRow) ? new Row(table) : existingRow;

        if (row.isNewRow()) {
            row.setOrgId(orgId);
        }
        row.set(OMDOFORECASTVIEW.USER_ID, forecastView.getUserId());
        row.set(OMDOFORECASTVIEW.CONFIG, forecastView.getConfig());

        dataContainer.updateRow(row);
        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * <p>
     * Converts a numeric column value (returned as {@link Long}, {@link Double}
     * or {@link BigDecimal} depending on the underlying aggregate) into a
     * {@link BigDecimal} without precision loss.
     * </p>
     *
     * @param rawValue Represents the raw column value.
     * @return The {@link BigDecimal} representation, or {@link BigDecimal#ZERO} when {@code null}.
     */
    private static BigDecimal toBigDecimal(final Object rawValue) {
        if (Objects.isNull(rawValue)) {
            return BigDecimal.ZERO;
        }

        if (rawValue instanceof BigDecimal bigDecimalValue) {
            return bigDecimalValue;
        }
        return new BigDecimal(rawValue.toString());
    }
}
