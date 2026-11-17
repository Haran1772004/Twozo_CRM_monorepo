package com.tlc.crm.core.api.deal.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.deal.handler.DynamicDealFieldEvaluatorHandler;
import com.tlc.crm.core.action.service.EmailSummaryActionService;
import com.tlc.crm.core.api.insights.service.EntityInsightsService;
import com.tlc.crm.core.api.insights.service.EntityInsightsToSummaryInfoMapper;
import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.entity.followers.records.UserFollowerUpdateRecords;
import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;
import com.tlc.crm.core.api.deal.model.DealKanbanSortFields;
import com.tlc.crm.core.features.api.followers.source.model.SourceFollowers;
import com.tlc.crm.core.field.fieldAudit.resolver.DealFieldAuditResolver;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.core.sql.resource.MWFCONTACTWEBFORM;
import com.tlc.crm.core.sql.resource.OMDOFOLLOWERS;
import com.tlc.crm.core.sql.resource.OMPOVARIATION;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.DefaultForecastCategory;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.common.list.sort.models.SortCriteria;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.common.list.sort.models.SortConfig;
import com.tlc.crm.entity.sql.resource.MTLFIELDVALUETIMELINE;
import com.tlc.crm.entity.sql.resource.OMQFFORECASTCATEGORY;
import com.tlc.crm.entity.sql.resource.OMTLTIMELINE;
import com.tlc.crm.entity.timeline.field.TimelineActionFields;
import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.manager.FieldTrackingConfigManager;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;

import com.tlc.crm.entity.timeline.field.TimelineFields;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.sql.common.api.dml.Criteria;

import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.field.deal.PipelineStageFields;
import com.tlc.crm.core.audit.AuditLogger;
import com.tlc.crm.entity.audit.log.model.AuditLogData;
import com.tlc.crm.entity.audit.log.model.AuditSource;
import com.tlc.crm.entity.dropdown.DealSourceStaticOption;
import com.tlc.crm.entity.sql.resource.MTLCOMPANYTIMELINE;

import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Deal.State;
import com.tlc.crm.core.api.deal.model.DealProgress;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.deal.model.StageProgress;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCOMADDRESSFIELDS;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;
import com.tlc.crm.core.sql.resource.MCONPHONEFIELDS;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDODROPDOWNFIELDS;
import com.tlc.crm.core.sql.resource.MDOPARTICIPANT;
import com.tlc.crm.core.sql.resource.MDOPIPELINE;
import com.tlc.crm.core.sql.resource.MDOPIPELINESTAGE;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.core.sql.resource.MDOSTAGEPROGRESS;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.core.sql.resource.MWFDEALWEBFORM;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;

import com.tlc.crm.entity.DefaultFieldsDataLoader;
import com.tlc.crm.entity.EntityLoader;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditAttribute;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.handler.EntityHandler;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.internal.DataLoaderValues;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.sql.resource.MTLCONTACTTIMELINE;
import com.tlc.crm.entity.sql.resource.MTLDEALTIMELINE;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.entity.sql.resource.OMACACTIVITYTYPE;
import com.tlc.crm.entity.sql.resource.OMETOPTION;
import com.tlc.crm.entity.timeline.api.CommonTimelineManager;
import com.tlc.crm.entity.timeline.model.FieldTimeline;
import com.tlc.crm.entity.timeline.model.Timeline;
import com.tlc.crm.entity.timeline.model.TimelineType;

import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.crm.fields.sql.resource.OMETPHONE;

import com.tlc.crm.webform.api.manager.WebFormManager;
import com.tlc.crm.webform.sql.resource.OMWFWEBFORM;

import com.tlc.i18n.I18nKey;

import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.OrderByClause.OrderType;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.HavingClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.LinkedHashSet;

import static com.tlc.crm.fields.models.StoreType.DROPDOWN;
import static com.tlc.crm.fields.models.StoreType.SINGLE_STRING;
import static com.tlc.sql.common.api.dml.JoinClause.JoinType.LEFT;

/**
 * Manages the {@link Deal} related actions
 *
 * @author Sundar
 * @version 1.0
 */
public class DealManager extends EntityConfigManager<Deal> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DealManager.class);
    private static final String TOTAL_VALUE = "TOTAL_VALUE";
    private static final String DEAL_COUNT = "DEAL_COUNT";
    private static final I18nKey PIPELINE_STAGE_KEY = new I18nKey("i18n.pipeline.stage.name");
    private static final I18nKey PIPELINE_KEY = new I18nKey("i18n.deal.fields.pipeline.name");
    private static final String PRODUCT_NAME = Source.PRODUCT.getName();

    private final EntityLoader entityLoader;
    private final EntityHandler entityHandler;
    private final PipelineManager pipelineManager;
    private final MonetaryManager monetaryManager;
    private final EmailManager emailManager;
    private final PhoneManager phoneManager;
    private final OptionManager optionManager;
    private final ProductDetailsHolderManager productDetailsHolderManager;
    private final CommonTimelineManager commonTimelineManager;
    private final PipelineStageManager pipelineStageManager;
    private final ProductManager productManager;
    private final DefaultFieldsDataLoader defaultFieldsDataLoader;
    private final UserService userService;
    private final ActivityTypeManager activityTypeManager;
    private final SourceActivityManager sourceActivityManager;
    private final WebFormManager webFormManager;
    private final OptionService optionService;
    private final BaseCurrencyService baseCurrencyService;
    private final SourceTagManager sourceTagManager;
    private final FilterCriteriaHandler filterCriteriaHandler;
    private final AuditLogger auditLogger;
    private final DateTimeUtil dateTimeUtil;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final SourceSettingsService sourceSettingsService;
    private final SourceFollowersManager sourceFollowersManager;
    private final DynamicDealFieldEvaluatorHandler dynamicDealFieldEvaluatorHandler;
    private final ForecastCategoryManager forecastCategoryManager;
    private final AddressManager addressManager;
    private final TerritoryManager territoryManager;
    private final FieldAuditHandler fieldAuditHandler;
    private final FieldTrackingConfigManager fieldTrackingConfigManager;
    private final DealFieldAuditResolver fieldAuditResolver;
    private final EntityInsightsToSummaryInfoMapper entityInsightsToSummaryInfoMapper;
    private final EntityInsightsService entityInsightsService;

    private static class Instance {
        private static final DealManager INSTANCE = new DealManager();
    }

    private DealManager() {
        entityLoader = EntityLoader.getInstance();
        entityHandler = EntityHandler.getInstance();
        pipelineManager = PipelineManager.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        optionManager = OptionManager.getInstance();
        emailManager = EmailManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        productDetailsHolderManager = ProductDetailsHolderManager.getInstance();
        commonTimelineManager = CommonTimelineManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        productManager = ProductManager.getInstance();
        defaultFieldsDataLoader = DefaultFieldsDataLoader.getInstance();
        userService = UserService.getInstance();
        activityTypeManager = ActivityTypeManager.getInstance();
        sourceActivityManager = SourceActivityManager.getInstance();
        webFormManager = WebFormManager.getInstance();
        optionService = OptionService.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
        sourceTagManager = SourceTagManager.getInstance();
        filterCriteriaHandler = FilterCriteriaHandler.getInstance();
        auditLogger = AuditLogger.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        sourceSettingsService = SourceSettingsService.getInstance();
        sourceFollowersManager = SourceFollowersManager.getInstance();
        dynamicDealFieldEvaluatorHandler = DynamicDealFieldEvaluatorHandler.getInstance();
        forecastCategoryManager = ForecastCategoryManager.getInstance();
        addressManager = AddressManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
        fieldAuditHandler = FieldAuditHandler.getInstance();
        fieldTrackingConfigManager = FieldTrackingConfigManager.getInstance();
        fieldAuditResolver = DealFieldAuditResolver.getInstance();
        entityInsightsToSummaryInfoMapper = EntityInsightsToSummaryInfoMapper.getInstance();
        entityInsightsService = EntityInsightsService.getInstance();
    }

    /**
     * Gets the {@link DealManager} Instance
     *
     * @return the {@link DealManager}
     */
    public static DealManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of deal
     */
    @Override
    public Table getTable() {
        return Table.get(MDODEAL.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return Deal follower table.
     */
    @Override
    protected Table getFollowersTable() {
        return Table.get(OMDOFOLLOWERS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of deal participant
     */
    public Table getParticipantTable() {
        return Table.get(MDOPARTICIPANT.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Table} of deal participant
     */
    public Table getContactTable() {
        return Table.get(MCOCONTACT.TABLE);
    }

    /**
     * Gets the deal stage progress table
     *
     * @return {@link Table}
     */
    private Table getStageProgressTable() {
        return Table.get(MDOSTAGEPROGRESS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link EntityHandler}
     */
    @Override
    protected EntityHandler getEntityHandler() {
        return entityHandler;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Column}
     */
    @Override
    protected Optional<Column> getTerritoryColumn() {
        return Optional.ofNullable(getTable().getColumn(MDODEAL.TERRITORY_ID));
    }

    /**
     * <p>
     * Fetches the {@link Deal} that has one of the id value mentioned in the parameter
     * </p>
     *
     * @param orgId Organization id
     * @param ids   Deals id
     * @return Collection of deals
     */
    @Override
    public Collection<Deal> get(final Long orgId, final Collection<Long> ids) {
        final Table dealTable = getTable();
        final Optional<SelectQuery> optionalSelectQuery = buildDealBaseQuery(dealTable, ids);

        if (optionalSelectQuery.isEmpty()) {
            return Collections.emptyList();
        }
        final SelectQuery selectQuery = optionalSelectQuery.get();
        DataContainer dataContainer = null;
        final boolean isProductEnabled = sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME);

        if (isProductEnabled) {
            final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
            final Table productDetailsTable = productDetailsHolderManager.getProductDetailTable();
            final Table productTable = productManager.getTable();
            final Table variationTable = VariationManager.getInstance().getTable();
            final JoinClause.JoinType leftJoin = LEFT;

            selectQuery.addJoinClause(List.of(new JoinClause(dealTable.getPKColumn(), productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), leftJoin),
                    new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), leftJoin),
                    new JoinClause(productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), productTable.getPKColumn(), leftJoin),
                    new JoinClause(productDetailsTable.getColumn(MDOPRODUCTDETAIL.VARIATION_ID), variationTable.getPKColumn(), leftJoin)));
            selectQuery.addSelectClause(productDetailsHolderTable, productDetailsTable);
            selectQuery.addSelectClause(List.of(productTable.getColumn(MPOPRODUCT.NAME), variationTable.getColumn(OMPOVARIATION.NAME)));
            dataContainer = orgDataStore(orgId).get(selectQuery);
        }
        final DataContainer entityContainer = fetchDealDataContainer(false, orgId, ids, dealTable, isProductEnabled ? buildDealBaseQuery(dealTable, ids).get() : selectQuery);

        if (Objects.nonNull(dataContainer)) {
            entityContainer.append(dataContainer);
        }

        if (entityContainer.size(dealTable) > 0) {
            return entityContainer.getRows(dealTable).map(dealRow -> convertRowToModel(dealRow, entityContainer)).toList();
        } else {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
    }

    /**
     * <p>
     * Fetches the {@link Deal} that has the id value mentioned in the parameter
     * </p>
     *
     * @param orgId           Represents the organisation ID
     * @param id              Represents the contact ID
     * @param templateFields  Fields values which are needed
     * @return {@link Deal} linked with the id
     */
    public Deal getDealWithTemplateFields(final Long orgId, final Long id, final Collection<FieldInfo> templateFields) {
        final Table dealTable = getTable();
        final Optional<SelectQuery> optionalSelectQuery = buildDealBaseQuery(dealTable, List.of(id));

        if (optionalSelectQuery.isPresent()) {
            final SelectQuery selectQuery = optionalSelectQuery.get();
            final DataContainer entityContainer = getEntityContainer(orgId, List.of(id), selectQuery, templateFields);

            if (entityContainer.size(dealTable) > 0) {
                return entityContainer.getRows(dealTable).map(dealRow -> convertRowToModel(dealRow, entityContainer)).findFirst().orElse(null);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization ID.
     * @param userId   Represents the user ID.
     * @param entityId Represents the entity ID.
     * @return A flag that indicates whether the user is the follower.
     */
    @Override
    public boolean isUserFollowingEntity(final long orgId, final long userId, final long entityId) {
        return sourceFollowersManager.isFollowerOfEntity(orgId, userId, entityId, Source.DEAL);
    }

    /**
     * <p>
     * Using the entity IDs and user ID, checks if the user is the follower of the entities
     * </p>
     *
     * @param orgId     Represents the organisation id
     * @param userId    Represents the user id
     * @param entityIds Represents the entity ids
     * @return The map containing the key as entity id and value is true if the user is the follower, false otherwise
     */
    @Override
    public Map<Long, Boolean> isUserFollowingEntities(final long orgId, final long userId, final Collection<Long> entityIds) {
        return sourceFollowersManager.isUserFollowingEntities(orgId, userId, entityIds, Source.DEAL);
    }

    /**
     * Fetches a {@link Deal} populated with basic details required for quotation creation.
     *
     * <p>The following data is loaded:</p>
     * <ul>
     *   <li>Deal name, sales owner</li>
     *   <li>Deal monetary value (currency and amount)</li>
     *   <li>Deal products (line items with name, quantity, price, discount, amount)</li>
     *   <li>Deal contacts (primary contact and participants with email and profile picture)</li>
     *   <li>Deal company with address</li>
     * </ul>
     *
     * @param orgId the organisation ID
     * @param id    the deal ID
     * @return a {@link Deal} populated with basic details
     */
    public Deal getDealWithBasicDetails(final long orgId, final long id) {
        final Table dealTable = getTable();
        final Table participantTable = getParticipantTable();
        final Table contactTable = getContactTable();
        final Table emailTable = emailManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final Table companyAddressTable = companyManager.getCompanyAddressTable();
        final Table addressTable = addressManager.getTable();
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailsTable = productDetailsHolderManager.getProductDetailTable();
        final Table productTable = productManager.getTable();
        final JoinClause.JoinType leftJoin = LEFT;
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column participantDealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
        final Column participantContactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
        final Column primaryEmailColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL);
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(dealTable);
        selectQuery.addSelectClause(monetaryTable);
        selectQuery.addSelectClause(List.of(contactPkColumn, contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME), contactTable.getColumn(MCOCONTACT.ORG_ID),
                primaryEmailColumn, participantDealIdColumn, participantContactIdColumn));
        selectQuery.addSelectClause(List.of(emailTable.getPKColumn(),
                emailTable.getColumn(OMETEMAIL.VALUE)));

        selectQuery.addJoinClause(List.of(
                new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()),
                new JoinClause(dealPkColumn, participantDealIdColumn),
                new JoinClause(participantContactIdColumn, contactPkColumn),
                new JoinClause(primaryEmailColumn, emailTable.getPKColumn(), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPkColumn, id)));
        selectQuery.addOrderByClause(new OrderByClause(participantTable.getPKColumn()));

        final DataContainer dataContainer = orgDataStore.get(selectQuery);

        final String companyKey = DealFields.COMPANY;
        final Map<String, FieldInfo> dealFields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);

        if (dealFields.containsKey(companyKey)) {
            final FieldInfo companyFieldInfo = dealFields.get(companyKey);
            final Table companyDerivedTable = companyManager.getCompanyFieldTable(companyFieldInfo, dealCompanyTable);
            final SelectQuery companySelectQuery = SelectQuery.get(dealTable);

            companySelectQuery.addSelectClause(companyDerivedTable);
            companySelectQuery.addSelectClause(companyTable);
            companySelectQuery.addJoinClause(new JoinClause(dealPkColumn, companyDerivedTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), leftJoin));
            companySelectQuery.addJoinClause(new JoinClause(companyDerivedTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), leftJoin));
            companySelectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPkColumn, id)));

            final String addressKey = CompanyFields.ADDRESS;
            final Map<String, FieldInfo> companyFields = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, addressKey);

            if (companyFields.containsKey(addressKey)) {
                companySelectQuery.addSelectClause(companyAddressTable);
                companySelectQuery.addSelectClause(addressTable);
                companySelectQuery.addJoinClause(new JoinClause(companyTable.getPKColumn(), companyAddressTable.getColumn(MCOMADDRESSFIELDS.ENTITY_ID), leftJoin));
                companySelectQuery.addJoinClause(new JoinClause(companyAddressTable.getColumn(MCOMADDRESSFIELDS.ADDRESS_ID), addressTable.getPKColumn(), leftJoin));
            }
            dataContainer.append(orgDataStore.get(companySelectQuery));
        }

        if (sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME)) {
            final SelectQuery productSelectQuery = SelectQuery.get(dealTable);

            productSelectQuery.addSelectClause(productDetailsHolderTable, productDetailsTable);
            productSelectQuery.addSelectClause(productTable.getColumn(MPOPRODUCT.NAME));
            productSelectQuery.addJoinClause(List.of(
                    new JoinClause(dealPkColumn, productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), leftJoin),
                    new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), leftJoin),
                    new JoinClause(productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), productTable.getPKColumn(), leftJoin)));

            dataContainer.append(orgDataStore.get(productSelectQuery));
        }

        if (dataContainer.isEmpty()) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }

        final Row dealRow = dataContainer.getRow(dealTable);
        final Deal deal = new Deal();

        deal.setId(dealRow.getPKValue());
        deal.setOrgId(orgId);
        deal.setTitle(dealRow.get(MDODEAL.TITLE));

        final Long salesOwnerId = dealRow.get(MDODEAL.OWNER_ID);

        if (Objects.nonNull(salesOwnerId)) {
            deal.setSalesOwner(userService.getUser(orgId, salesOwnerId));
        }

        final Row monetaryRow = dataContainer.getRow(monetaryTable, (Long) dealRow.get(MDODEAL.MONETARY_ID));

        if (Objects.nonNull(monetaryRow)) {
            final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);

            deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));

            if (sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME)) {
                deal.setProductDetailsHolder(getProductDetailsHolder(dealRow.getPKValue(), dataContainer));
            }
        }

        final Long primaryContactId = dealRow.get(MDODEAL.CONTACT_ID);

        loadPrimaryContact(dataContainer, primaryContactId, deal);
        loadDealParticipants(dataContainer, deal);

        final Row dealCompanyRow = dataContainer.getRow(dealCompanyTable,
                new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealRow.getPKValue())));

        if (Objects.nonNull(dealCompanyRow)) {
            final Row companyRow = dataContainer.getRow(companyTable,
                    new WhereClause(Criteria.eq(companyTable.getPKColumn(), dealCompanyRow.get(MDOCOMPANYFIELDS.COMPANY_ID))));

            if (Objects.nonNull(companyRow)) {
                deal.setCompany(companyManager.convertRowToModel(companyRow, true, dataContainer));
            }
        }

        return deal;
    }

    /**
     * Sets the {@link SummaryInfo} of a deal.
     *
     * @param orgId Refers the organization id.
     * @param deal  Refers the {@link Deal} for which the summary details is to be set.
     */
    public final void loadSummaryDetails(final Long orgId, final Deal deal) {
        final Long dealId = deal.id();
        final SummaryInfo summaryInfo = new SummaryInfo();

        if (Objects.nonNull(dealId)) {
            final String entityType = Deal.ENTITY_TYPE;

            summaryInfo.setEmailSummary(EmailSummaryActionService.getInstance().get(orgId, dealId, Source.DEAL));
            summaryInfo.setActivitySummary(sourceActivityManager.getActivitySummary(orgId, dealId, entityType));
            summaryInfo.setWebFormName(getWebFormName(orgId, dealId));
            summaryInfo.setTags(sourceTagManager.getMappedTags(orgId, dealId, entityType));
        }
        deal.setSummaryInfo(summaryInfo);
        loadSummaryPreviewDetails(orgId, deal, false);
    }

    /**
     * <p>
     * Loads the summary info
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param deal  Represents the deal whose summary info will be set.
     */
    public final void loadSummaryInfo(final Long orgId, final Deal deal) {
        final Long dealId = deal.id();
        final SummaryInfo summaryInfo;

        if (Objects.nonNull(dealId)) {
            summaryInfo = entityInsightsToSummaryInfoMapper.buildSummaryInfo(orgId, dealId, Source.DEAL);
            summaryInfo.setTags(sourceTagManager.getMappedTags(orgId, dealId, Deal.ENTITY_TYPE));
            summaryInfo.setWebFormName(getWebFormName(orgId, dealId));
        } else {
            summaryInfo = new SummaryInfo();
        }
        deal.setSummaryInfo(summaryInfo);
        loadSummaryPreviewDetails(orgId, deal, false);
    }

    /**
     * Fetches the {@link DataContainer} that contains the deal data
     *
     * @param orgId   Represents the organisation ID
     * @param dealIds Which contains the collection of deal IDs
     * @return {@link DataContainer} that contains the deal data
     */
    public DataContainer fetchDealContainer(final Long orgId, final Collection<Long> dealIds) {
        final Table dealTable = getTable();
        final Optional<SelectQuery> optionalSelectQuery = buildDealBaseQuery(dealTable, dealIds);

        if (optionalSelectQuery.isEmpty()) {
            return DataContainer.create();
        }
        return fetchDealDataContainer(true, orgId, dealIds, dealTable, optionalSelectQuery.get());
    }

    /**
     * <p>
     * Fetches the {@link DataContainer} that contains the deal data with only the base details
     * </p>
     *
     * @param orgId   Represents the organisation ID
     * @param dealIds Represents the collection of deal IDs
     * @return the {@link DataContainer} that contains the deal data
     */
    public DataContainer fetchPartialDealContainer(final Long orgId, final Collection<Long> dealIds) {
        final Table dealTable = getTable();
        final Optional<SelectQuery> optionalSelectQuery = buildDealBaseQuery(dealTable, dealIds);

        if (optionalSelectQuery.isEmpty()) {
            return DataContainer.create();
        }

        return orgDataStore(orgId).get(optionalSelectQuery.get());
    }

    /**
     * <p>
     * Builds the deal base query
     * </p>
     *
     * @param dealTable Represents the deal table
     * @param dealIds   Represents the collection of deal ids
     * @return The optional {@link SelectQuery}
     */
    public Optional<SelectQuery> buildDealBaseQuery(final Table dealTable, final Collection<Long> dealIds) {
        if (isEntityIdCollectionEmpty(dealIds)) {
            return Optional.empty();
        }
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(dealTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getPKColumn(), dealIds)));

        return Optional.of(selectQuery);
    }

    /**
     * Fetches the related entities of deal table
     *
     * @param isUpdateEvent Represents the update event
     * @param orgId         Represents the organisation id
     * @param dealIds       Represents the collection of deal ids
     * @param dealTable     Represents the deal table
     * @param selectQuery   To be loaded with the select and join clauses
     * @return {@link DataContainer} which contains the deal related records
     */
    public DataContainer fetchDealDataContainer(final boolean isUpdateEvent, final Long orgId, final Collection<Long> dealIds,
                                                final Table dealTable, final SelectQuery selectQuery) {
        final Map<Table, Collection<Column>> requiredColumns = new HashMap<>();
        final Table participantTable = ParticipantManager.getInstance().getTable();
        final Table contactTable = ContactManager.getInstance().getTable();
        final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);
        final Collection<FieldInfo> dealFields = secureFieldService.getFieldsBySource(orgId, Deal.ENTITY_TYPE);
        final Map<Table, JoinClause.JoinType> joins = new HashMap<>();
        final JoinClause.JoinType innerJoin = JoinClause.JoinType.INNER;
        final Column participantDealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column dealContactColumn = dealTable.getColumn(MDODEAL.CONTACT_ID);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column territoryPkColumn = territoryTable.getPKColumn();
        final Table forecastCategoryTable = forecastCategoryManager.getTable();

        selectQuery.addJoinClause(List.of(new JoinClause(dealPkColumn, participantDealIdColumn),
                new JoinClause(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactPkColumn),
                new JoinClause(dealTable.getColumn(MDODEAL.TERRITORY_ID), territoryPkColumn, LEFT),
                new JoinClause(dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID), forecastCategoryTable.getPKColumn(), LEFT)));
        selectQuery.addSelectClause(List.of(dealContactColumn, participantDealIdColumn,
                participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactPkColumn,
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.ORG_ID), territoryPkColumn, territoryTable.getColumn(OMCRMTERRITORY.NAME),
                dealTable.getColumn(MDODEAL.STATUS), forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.NAME)));
        SourceTagManager.getInstance().loadJoinsAndSelectClause(Source.DEAL, dealTable, selectQuery);

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        joins.put(monetaryManager.getTable(), innerJoin);
        joins.put(pipelineManager.getTable(), innerJoin);
        joins.put(pipelineStageManager.getTable(), innerJoin);

        dataContainer.append(getEntityContainer(orgId, buildDealBaseQuery(dealTable, dealIds).get(), dealIds, dealFields, requiredColumns, joins));

        if (isUpdateEvent) {
            dataContainer.append(getDealTimelineDataContainer(orgId, dealPkColumn, dealIds));
        }

        return dataContainer;
    }

    /**
     * Fetches the web form name related to the deal
     *
     * @param orgId  Represents an organisation id
     * @param dealId Represents the deal id.
     * @return name of the web form.
     */
    public String getWebFormName(final Long orgId, final Long dealId) {
        final Table dealTable = getTable();
        final Table dealWebFormTable = Table.get(MWFDEALWEBFORM.TABLE);
        final Table webFormTable = webFormManager.getTable();
        final Column dealPKColumn = dealTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(dealPKColumn, dealWebFormTable.getColumn(MWFDEALWEBFORM.DEAL_ID)),
                new JoinClause(dealWebFormTable.getColumn(MWFDEALWEBFORM.WEB_FORM_ID), webFormTable.getPKColumn())));
        selectQuery.addSelectClause(webFormTable.getColumn(OMWFWEBFORM.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPKColumn, dealId)));

        final Row row = orgDataStore(orgId).get(selectQuery).getRow(webFormTable);

        return Objects.isNull(row) ? null : row.get(OMWFWEBFORM.NAME);
    }

    /**
     * <p>
     * Gets Deal Ids based on the Pipeline & PipelineStage id provided
     * </p>
     *
     * @param orgId           Represents an organization's id
     * @param pipelineId      Represents {@link Pipeline} id.
     * @param pipelineStageId Represents {@link PipelineStage} id.
     * @return Deal ids.
     */
    public Collection<Long> getDealIds(final Long orgId, final Long pipelineId, final Long pipelineStageId) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(dealTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineId)).
                and(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pipelineStageId))));
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(dealTable);

        return rows.map(Row::getPKValue).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets Deal & Pipeline Ids based on the Pipeline id provided
     * </p>
     *
     * @param orgId      Represents an organization's id
     * @param pipelineId Represents pipeline id.
     * @return Deal ids.
     */
    public Map<Long, Long> getDealAndPipelineStageIds(final Long orgId, final Long pipelineId) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column pipelineStageColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column dealPkColumn = dealTable.getPKColumn();

        selectQuery.addSelectClause(List.of(dealPkColumn, pipelineStageColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineId)));
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(dealTable);
        final Map<Long, Long> dealAndPipelineStageIdsMap = new HashMap<>();

        rows.forEach(row -> dealAndPipelineStageIdsMap.put(row.get(dealPkColumn), row.get(pipelineStageColumn)));

        return dealAndPipelineStageIdsMap;
    }

    /**
     * Builds the additional tables into the {@link SelectQuery} to join and select the columns
     *
     * @param dealPKColumn Represents the deal PK column
     * @param dealIds      The collection of deal ids
     */
    private DataContainer getDealTimelineDataContainer(final Long orgId, final Column dealPKColumn, final Collection<Long> dealIds) {
        final Table dealTimelineTable = commonTimelineManager.getDealTimelineTable();
        final Table timelineTable = commonTimelineManager.getTable();
        final Table dealTable = getTable();
        final Column timelineIdColumn = timelineTable.getPKColumn();
        final Table contactTimelineTable = commonTimelineManager.getContactTimelineTable();
        final Table fieldvalueTimelineTable = commonTimelineManager.getFieldValueTimelineTable();
        final JoinClause.JoinType leftJoin = LEFT;
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(dealTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getPKColumn(), dealIds)));
        selectQuery.addJoinClause(List.of(new JoinClause(dealPKColumn, dealTimelineTable.getColumn(MTLDEALTIMELINE.DEAL_ID)),
                new JoinClause(dealTimelineTable.getColumn(MTLDEALTIMELINE.TIMELINE_ID), timelineIdColumn),
                new JoinClause(timelineIdColumn, contactTimelineTable.getColumn(MTLCONTACTTIMELINE.TIMELINE_ID), leftJoin),
                new JoinClause(timelineIdColumn, fieldvalueTimelineTable.getColumn(MTLFIELDVALUETIMELINE.TIMELINE_ID), leftJoin)));

        selectQuery.addSelectClause(List.of(timelineIdColumn, timelineTable.getColumn(OMTLTIMELINE.TYPE)));
        selectQuery.addSelectClause(dealTimelineTable, contactTimelineTable, fieldvalueTimelineTable);

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * Gets the deal progress by given deal id
     *
     * @param orgId  Represents the organisation id
     * @param dealId Represents the deal id
     * @return The {@link DealProgress}
     */
    public DealProgress getDealProgress(final Long orgId, final Long dealId) {
        final Table dealTable = getTable();
        final Table pipelineTable = pipelineManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table dealStageProgressTable = getStageProgressTable();
        final SelectQuery selectQuery = SelectQuery.get(dealStageProgressTable);
        final SelectQuery pipelineSelectQuery = SelectQuery.get(dealTable);
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column pipelineStagePkColumn = pipelineStageTable.getPKColumn();
        final Column pipelinePkColumn = pipelineTable.getPKColumn();
        final Column stageProgressPipelineColumn = dealStageProgressTable.getColumn(MDOSTAGEPROGRESS.PIPELINE_ID);
        final Column stageProgressDealColumn = dealStageProgressTable.getColumn(MDOSTAGEPROGRESS.DEAL_ID);
        final List<JoinClause> joinClauses = List.of(new JoinClause(stageProgressPipelineColumn, pipelinePkColumn),
                new JoinClause(pipelinePkColumn, pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID)),
                new JoinClause(stageProgressDealColumn, dealPkColumn));

        pipelineSelectQuery.addSelectClause(dealTable.getColumn(MDODEAL.PIPELINE_ID));
        pipelineSelectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPkColumn, dealId)));
        selectQuery.addJoinClause(joinClauses);
        selectQuery.addSelectClause(List.of(dealPkColumn, dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE),
                dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), dealTable.getColumn(MDODEAL.STATE), pipelineTable.getColumn(MDOPIPELINE.NAME),
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), pipelineStagePkColumn, pipelinePkColumn,
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.PROBABILITY), pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER)));
        selectQuery.addSelectClause(dealStageProgressTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(stageProgressDealColumn, dealId)).and(Criteria.eq(stageProgressPipelineColumn, pipelineSelectQuery)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row pipelineRow = dataContainer.getRow(pipelineTable);
        final Row dealRow = dataContainer.getRow(dealTable);
        final Collection<Row> stageProgressRows = dataContainer.getRows(dealStageProgressTable).toList();
        final Collection<Row> pipelineStageRows = dataContainer.getRows(pipelineStageTable).toList();
        final DealProgress dealProgress = new DealProgress();

        if (Objects.nonNull(dealRow)) {
            dealProgress.setDealId(dealRow.getPKValue());
            dealProgress.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
            setDealStageProgress(pipelineStageRows, stageProgressRows, dealProgress);

            if (Objects.nonNull(pipelineRow)) {
                dealProgress.setPipelineName(pipelineRow.get(MDOPIPELINE.NAME));
                dealProgress.setPipelineId(pipelineRow.get(MDOPIPELINE.ID));
            }
        }

        return dealProgress;
    }

    /**
     * <p>
     * Fetches the {@link Deal} for the given id.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param id     Deal id.
     * @param fields Required fields.
     * @return {@link Deal} holds the details of the contact.
     */
    public Deal get(final Long orgId, final Long id, final Collection<FieldInfo> fields) {
        final Table table = getTable();
        final DataContainer dataContainer = getEntityContainer(orgId, List.of(id), fields);
        final Row row = dataContainer.getRow(table);

        return Objects.isNull(row) ? null : convertRowToModel(row, dataContainer);
    }

    /**
     * Gets the collection of non-participants by deal id
     *
     * @param orgId    Represents organization id
     * @param dealId   Represents the deal id
     * @param limit    Which holds the limit configuration.
     * @param criteria Which configure the criteria.
     * @return The collection of non {@link Participant}
     */
    public Collection<Participant> getNonParticipantsByDealId(final Long orgId, final Long dealId, final DropdownLimit limit,
                                                              final DropdownCriteria criteria) {
        final ContactManager contactManager = ContactManager.getInstance();
        final Collection<Participant> participants = new ArrayList<>();
        final Table participantTable = ParticipantManager.getInstance().getTable();
        final Table contactTable = contactManager.getTable();
        final Table dealTable = getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final SelectQuery participantSelectQuery = SelectQuery.get(participantTable);
        final SelectQuery contactSelectQuery = SelectQuery.get(contactTable);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column participantDealColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
        final Column participantContactColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column primaryEmailColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL);
        final Column primaryPhoneColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE);
        final Column companyPkColumn = companyTable.getPKColumn();
        final Column companyEntityIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID);
        final JoinClause.JoinType leftJoin = LEFT;

        participantSelectQuery.addJoinClause(List.of(new JoinClause(participantDealColumn, dealTable.getPKColumn()),
                new JoinClause(participantContactColumn, contactPkColumn)));
        participantSelectQuery.addSelectClause(participantContactColumn);
        participantSelectQuery.setWhereClause(new WhereClause(Criteria.eq(participantDealColumn, dealId)));
        contactSelectQuery.addSelectClause(List.of(contactPkColumn, firstNameColumn, lastNameColumn, primaryEmailColumn, primaryPhoneColumn));
        contactSelectQuery.addSelectClause(List.of(emailTable.getPKColumn(), emailTable.getColumn(OMETEMAIL.VALUE)));
        contactSelectQuery.addSelectClause(List.of(phoneTable.getPKColumn(), phoneTable.getColumn(OMETPHONE.NUMBER)));
        contactSelectQuery.addSelectClause(List.of(companyPkColumn, companyTable.getColumn(MCOCOMPANY.NAME)));
        contactSelectQuery.addSelectClause(contactCompanyTable);
        contactSelectQuery.addJoinClause(List.of(
                new JoinClause(primaryEmailColumn, emailTable.getPKColumn(), leftJoin),
                new JoinClause(primaryPhoneColumn, phoneTable.getPKColumn(), leftJoin),
                new JoinClause(contactPkColumn, companyEntityIdColumn, leftJoin),
                new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyPkColumn, leftJoin)));
        contactSelectQuery.setWhereClause(new WhereClause(Criteria.notIn(contactPkColumn, participantSelectQuery)).
                and(new WhereClause(criteria.build(firstNameColumn)).or(criteria.build(lastNameColumn))));
        contactSelectQuery.addOrderByClause(new OrderByClause(contactPkColumn));
        contactSelectQuery.setLimitClause(limit.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(contactSelectQuery);

        if (!dataContainer.isEmpty()) {
            dataContainer.getRows(contactTable).forEach(row -> {
                final Participant participant = new Participant();
                final Contact contact = contactManager.convertRowToPartialModel(row);

                contact.setOrgId(orgId);
                final Optional<Long> primaryEmailId = Optional.ofNullable(row.get(MCOCONTACT.PRIMARY_EMAIL));
                final Optional<Long> primaryPhoneId = Optional.ofNullable(row.get(MCOCONTACT.PRIMARY_PHONE));
                final Optional<Row> emailRow = primaryEmailId.map(id -> dataContainer.getRow(emailTable, id));
                final Optional<Row> phoneRow = primaryPhoneId.map(id -> dataContainer.getRow(phoneTable, id));
                final Optional<Row> contactCompanyRow = Optional.ofNullable(dataContainer.getRow(contactCompanyTable,
                        new WhereClause(Criteria.eq(companyEntityIdColumn, row.getPKValue()))));
                final Optional<Row> companyRow = contactCompanyRow.map(contactCompanyRowValue ->
                        dataContainer.getRow(companyTable, (Long) contactCompanyRowValue.get(MCONCOMPANYFIELDS.COMPANY_ID)));

                emailRow.ifPresent(email -> contact.setPrimaryEmail(emailManager.convertRowToModel(email)));
                phoneRow.ifPresent(phone -> contact.setPrimaryPhone(phoneManager.convertRowToModel(phone)));
                companyRow.ifPresent(company -> contact.setCompany(companyManager.convertRowToPartialModel(company)));

                participant.setContact(contact);
                participants.add(participant);
            });
        }

        return participants;
    }

    /**
     * Gets the total count of non-participants by deal id
     *
     * @param orgId    Represents organization id
     * @param dealId   Represents the deal id
     * @param criteria Which configure the criteria.
     * @return The total count of non {@link Participant}
     */
    public long getNonParticipantsCountByDealId(final Long orgId, final Long dealId, final DropdownCriteria criteria) {
        final Table participantTable = ParticipantManager.getInstance().getTable();
        final Table contactTable = contactManager.getTable();
        final SelectQuery participantSelectQuery = SelectQuery.get(participantTable);
        final CountQuery nonParticipantCountQuery = CountQuery.get(contactTable);

        participantSelectQuery.addSelectClause(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID));
        participantSelectQuery.setWhereClause(new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId)));

        nonParticipantCountQuery.setWhereClause(new WhereClause(Criteria.notIn(contactTable.getPKColumn(), participantSelectQuery))
                .and(new WhereClause(criteria.build(contactTable.getColumn(MCOCONTACT.FIRST_NAME)))
                        .or(criteria.build(contactTable.getColumn(MCOCONTACT.LAST_NAME)))));

        return orgDataStore(orgId).get(nonParticipantCountQuery);
    }

    /**
     * Sets the deal stage progress into {@link DealProgress}
     *
     * @param pipelineStageRows Represents the collection of pipeline stage rows
     * @param stageProgressRows Represents the collection of stage progress rows
     * @param dealProgress      An entity that holds the deal stage progress attributes
     */
    private void setDealStageProgress(final Collection<Row> pipelineStageRows, final Collection<Row> stageProgressRows,
                                      final DealProgress dealProgress) {
        final Map<Long, List<Row>> rowsByStage = stageProgressRows.stream().collect(Collectors.groupingBy(stageProgressRow ->
                stageProgressRow.get(MDOSTAGEPROGRESS.PIPELINE_STAGE_ID)));

        for (final Row pipleineStageRow : pipelineStageRows) {
            final Long stageId = pipleineStageRow.getPKValue();
            final List<Row> stageRowList = rowsByStage.get(stageId);
            final PipelineStage pipelineStage = new PipelineStage();

            pipelineStage.setId(stageId);
            pipelineStage.setName(pipleineStageRow.get(MDOPIPELINESTAGE.NAME));
            pipelineStage.setProbability(pipleineStageRow.get(MDOPIPELINESTAGE.PROBABILITY));
            pipelineStage.setOrder(pipleineStageRow.get(MDOPIPELINESTAGE.ORDER));

            if (Objects.nonNull(stageRowList)) {

                for (final Row row : stageRowList) {
                    final StageProgress stageProgress = new StageProgress();

                    stageProgress.setStartTime(row.get(MDOSTAGEPROGRESS.START_TIME));
                    stageProgress.setEndTime(row.get(MDOSTAGEPROGRESS.END_TIME));
                    stageProgress.setPipelineStage(pipelineStage);
                    dealProgress.addStageProgress(stageProgress);
                }
            } else {
                final StageProgress stageProgress = new StageProgress();

                stageProgress.setPipelineStage(pipelineStage);
                dealProgress.addStageProgress(stageProgress);
            }
        }
    }

    /**
     * Fetch the deal dynamic option values for the given deal ids
     *
     * @param orgId   Represents the organisation id
     * @param dealIds {@link Collection} of deal ids
     * @return {@link DataContainer} which contains the deal dynamic option related records
     */
    private DataContainer fetchDealDynamicOptionDataContainer(final Long orgId, final Collection<Long> dealIds) {
        final Table dealTable = getTable();
        final JoinClause.JoinType leftJoin = LEFT;
        final SelectQuery optionSelectQuery = SelectQuery.get(dealTable);
        final Table dynamicOptionTable = optionManager.getTable();
        final Table dealDynamicOptionTable = getDealDynamicOptionTable();

        optionSelectQuery.addJoinClause(List.of(new JoinClause(dealTable.getPKColumn(), dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.OPTION_ID), dynamicOptionTable.getPKColumn(), leftJoin)));
        optionSelectQuery.addSelectClause(dynamicOptionTable, dealTable);
        optionSelectQuery.setWhereClause(new WhereClause(Criteria.in(dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), dealIds)));

        return orgDataStore(orgId).get(optionSelectQuery);
    }

    /**
     * <p>
     * Fetches the user defined entity of an organization along with system entity
     * </p>
     *
     * @param orgId Organization id
     * @return Collection of entity
     */
    private Collection<FieldInfo> getDealFields(final Long orgId) {
        return secureFieldService.getFieldsBySource(orgId, Deal.ENTITY_TYPE);
    }

    /**
     * <p>
     * Saves the attributes of {@link Deal} into the database
     * </p>
     *
     * @param deal An entity that holds the deal attributes which have to be saved into database
     */
    @Override
    public Deal create(Deal deal) {
        final Long orgId = deal.orgId();
        final DataContainer targetContainer = DataContainer.create();

        loadDealIntoContainer(true, deal, targetContainer);
        sourceFollowersManager.loadFollowerIfExists(orgId, deal.getSalesOwner(), deal.id(), Source.DEAL, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
        auditLogger.log(orgId, List.of(new AuditLogData(deal.getCreatedBy().id(), deal.id(), deal, AuditSource.DEAL, AuditAction.ADDED)));

        return deal;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organisation ID
     * @param deals which contains the collection of {@link Deal}
     */
    @Override
    public final void delete(final Long orgId, final Collection<Deal> deals) {
        if (Objects.nonNull(deals) && !deals.isEmpty()) {
            final Map<Integer, Collection<FieldInfo>> groupedFields = fieldService.groupAllFieldsByType(orgId, Deal.ENTITY_TYPE);
            final Table dealTable = getTable();
            final Set<Long> entityIds = new HashSet<>();
            final Collection<ValueHolder> valueHolders = new ArrayList<>();
            final Set<Long> monetaryIds = new HashSet<>();
            final AuditSource auditSource = AuditSource.DEAL;
            final AuditAction auditAction = AuditAction.DELETED;
            final Collection<AuditLogData> auditLogDataList = new ArrayList<>();

            for (final Deal deal : deals) {
                entityIds.add(deal.id());
                valueHolders.add(deal.getValueHolder());
                monetaryIds.add(deal.getDealValue().id());
                auditLogDataList.add(new AuditLogData(deal.getUpdatedBy().id(), deal.id(), deal, auditSource, auditAction));
            }
            final List<DeleteQuery> deleteQueries = entityHandler.buildEntityDeleteQueries(dealTable, groupedFields,
                    new WhereClause(Criteria.in(dealTable.getPKColumn(), entityIds)), valueHolders);

            if (!monetaryIds.isEmpty()) {
                final Table monetaryTable = monetaryManager.getTable();

                deleteQueries.add(entityHandler.buildEntityDeleteQuery(monetaryTable, new WhereClause(Criteria
                        .in(monetaryTable.getPKColumn(), monetaryIds))));
            }
            entityInsightsService.enqueueMappedEntitiesForDeletedDeals(orgId, entityIds);

            orgDataStore(orgId).delete(deleteQueries);

            if (!auditLogDataList.isEmpty()) {
                auditLogger.log(orgId, auditLogDataList);
            }
        }
    }

    /**
     * <p>
     * Updates the given deals
     * </p>
     *
     * @param orgId  Organization id
     * @param models Collection of deals that have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(Long orgId, Collection<Deal> models) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Table dealTable = getTable();
        final Collection<FieldInfo> fields = getDealFields(orgId);
        final Collection<Long> ids = models.stream().map(Deal::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Optional<SelectQuery> optionalSelectQuery = buildDealBaseQuery(dealTable, ids);

        if (optionalSelectQuery.isEmpty()) {
            return processOutcome;
        }
        final SelectQuery selectQuery = optionalSelectQuery.get();
        buildDealProgressSelectQuery(dealTable, selectQuery, JoinClause.JoinType.INNER);
        buildProductDetailsHolderSelectQuery(dealTable, selectQuery);

        final DataContainer entityContainer = fetchDealDataContainer(true, orgId, ids, dealTable, selectQuery);
        final AuditAction auditAction = AuditAction.UPDATED;
        final AuditSource auditSource = AuditSource.DEAL;
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final Map<String, FieldInfo> defaultFields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);

        models.forEach(model -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadDealIntoContainer(false, fields, model, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                final Long id = model.id();

                processOutcome.addProcessedRecord(model.id());
                loadCompleteDealInfo(orgId, model, defaultFields, targetContainer);
                final Row oldDealRow = entityContainer.getRow(dealTable, id);
                final Deal oldDeal = Objects.isNull(oldDealRow) ? null : convertRowToModel(oldDealRow, entityContainer);

                auditLogDataList.add(new AuditLogData(model.getUpdatedBy().id(), model.id(), oldDeal, model, auditSource, auditAction));
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Deal update failed Org ID : {} ID : {} reason : {}", orgId, model.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, model.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        if (!auditLogDataList.isEmpty()) {
            auditLogger.log(orgId, auditLogDataList);
        }

        return processOutcome;
    }

    /**
     * Builds the product details holder select query
     *
     * @param dealTable   Represents the deal table
     * @param selectQuery Represents the deal select query
     */
    private void buildProductDetailsHolderSelectQuery(final Table dealTable, final SelectQuery selectQuery) {
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailTable = productDetailsHolderManager.getProductDetailTable();
        final Column dealIdColumn = productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID);

        selectQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), dealIdColumn, LEFT));
        selectQuery.addJoinClause(new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), LEFT));
        selectQuery.addSelectClause(List.of(dealIdColumn, productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.TAX_INCLUSIVE),
                productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.CURRENCY_ID)));
        selectQuery.addSelectClause(productDetailTable);
    }

    /**
     * <p>
     * Loads the complete deal info into the {@link Deal}.
     * </p>
     * <h4>
     * (FYI - example: When mapping a company to a deal, if the company is older, only the entry in the
     * MDOCompanyFields table is available. However, we require an entry in the MCOCompany table).
     * </h4>
     *
     * @param deal            Holds the details of the deal.
     * @param targetContainer Holds the updated information of user defined and system field values.
     */
    private void loadCompleteDealInfo(final Long orgId, final Deal deal, final Map<String, FieldInfo> fields,
                                      final DataContainer targetContainer) {
        final CompanyManager companyManager = CompanyManager.getInstance();
        final Table dealTable = getTable();
        final Table contactTable = getContactTable();
        final Table pipelineTable = pipelineManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final Collection<Long> dealIds = new HashSet<>();
        final Collection<Long> unavailableContactIds = new HashSet<>();
        final Collection<Long> unavailableCompanyIds = new HashSet<>();
        final Collection<Long> unavailablePipelineIds = new HashSet<>();
        final Collection<Long> unavailablePipelineStageIds = new HashSet<>();

        loadEntityIds(deal, dealIds, unavailableContactIds, unavailableCompanyIds, unavailablePipelineIds,
                unavailablePipelineStageIds, fields, targetContainer);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final boolean isContactNeeded = !unavailableContactIds.isEmpty();
        final boolean isCompanyNeeded = !unavailableCompanyIds.isEmpty();
        final boolean isPipelineNeeded = !unavailablePipelineIds.isEmpty();
        final boolean isPipelineStageNeeded = !unavailablePipelineStageIds.isEmpty();

        if (isContactNeeded) {
            final SelectQuery contactQuery = SelectQuery.get(contactTable);
            final Column contactPkColumn = contactTable.getPKColumn();
            final Table participantTable = getParticipantTable();

            contactQuery.setWhereClause(new WhereClause(Criteria.in(contactPkColumn, unavailableContactIds)));
            contactQuery.addSelectClause(List.of(contactPkColumn, contactTable.getColumn(MCOCONTACT.ORG_ID),
                    contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)));
            final Table contactDerivedTable = DerivedTable.get(contactTable.getName(), contactTable.getAlias(), contactQuery);
            final Column pkColumn = contactDerivedTable.getPKColumn();

            selectQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), participantTable.getColumn(MDOPARTICIPANT.DEAL_ID)));
            selectQuery.addJoinClause(new JoinClause(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), pkColumn));
            selectQuery.addSelectClause(List.of(pkColumn, contactDerivedTable.getColumn(MCOCONTACT.ORG_ID),
                    contactDerivedTable.getColumn(MCOCONTACT.FIRST_NAME), contactDerivedTable.getColumn(MCOCONTACT.LAST_NAME)));
        }

        if (isPipelineNeeded) {
            final SelectQuery pipelineQuery = SelectQuery.get(pipelineTable);
            final Column pipelinePkColumn = pipelineTable.getPKColumn();

            pipelineQuery.setWhereClause(new WhereClause(Criteria.in(pipelinePkColumn, unavailablePipelineIds)));
            pipelineQuery.addSelectClause(List.of(pipelinePkColumn, pipelineTable.getColumn(MDOPIPELINE.ORG_ID),
                    pipelineTable.getColumn(MDOPIPELINE.NAME)));
            final Table pipelineDerivedTable = DerivedTable.get(pipelineTable.getName(), pipelineTable.getAlias(), pipelineQuery);
            final Column pkColumn = pipelineDerivedTable.getPKColumn();

            selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.PIPELINE_ID), pkColumn));
            selectQuery.addSelectClause(List.of(pkColumn, pipelineDerivedTable.getColumn(MDOPIPELINE.ORG_ID),
                    pipelineDerivedTable.getColumn(MDOPIPELINE.NAME)));
        }

        if (isPipelineStageNeeded) {
            final SelectQuery pipelineStageQuery = SelectQuery.get(pipelineStageTable);
            final Column pipelineStagePkColumn = pipelineStageTable.getPKColumn();

            pipelineStageQuery.setWhereClause(new WhereClause(Criteria.in(pipelineStagePkColumn, unavailablePipelineStageIds)));
            pipelineStageQuery.addSelectClause(List.of(pipelineStagePkColumn, pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORG_ID),
                    pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
            final Table pipelineStageDerivedTable = DerivedTable.get(pipelineStageTable.getName(), pipelineStageTable.getAlias(), pipelineStageQuery);
            final Column pkColumn = pipelineStageDerivedTable.getPKColumn();

            selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pkColumn));
            selectQuery.addSelectClause(List.of(pkColumn, pipelineStageDerivedTable.getColumn(MDOPIPELINESTAGE.ORG_ID),
                    pipelineStageDerivedTable.getColumn(MDOPIPELINESTAGE.NAME)));
        }

        if (isCompanyNeeded) {
            final SelectQuery companyQuery = SelectQuery.get(companyTable);
            final Column companyPkColumn = companyTable.getPKColumn();
            final JoinClause.JoinType leftJoin = LEFT;

            companyQuery.addSelectClause(List.of(companyPkColumn, companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.ORG_ID)));
            companyQuery.setWhereClause(new WhereClause(Criteria.in(companyPkColumn, unavailableCompanyIds)));
            final Table companyDerivedTable = DerivedTable.get(companyTable.getName(), companyTable.getAlias(), companyQuery);
            final Column pkColumn = companyDerivedTable.getPKColumn();

            selectQuery.addSelectClause(List.of(pkColumn, companyDerivedTable.getColumn(MCOCOMPANY.NAME),
                    companyDerivedTable.getColumn(MCOCOMPANY.ORG_ID)));
            selectQuery.addJoinClause(List.of(
                    new JoinClause(dealTable.getPKColumn(), dealCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), leftJoin),
                    new JoinClause(dealCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), pkColumn, leftJoin)));
        }

        if (!selectQuery.getJoinClause().isEmpty()) {
            selectQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getPKColumn(), dealIds)));
            final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

            if (isContactNeeded) {
                final Row contactRow = dataContainer.getRow(contactTable);

                if (Objects.nonNull(contactRow)) {
                    deal.setPrimaryContact(ContactManager.getInstance().convertRowToPartialModel(contactRow));
                }
            }

            if (isPipelineNeeded) {
                final Row pipelineRow = dataContainer.getRow(pipelineTable);

                if (Objects.nonNull(pipelineRow)) {
                    deal.setPipeline(pipelineManager.convertRowToModel(pipelineRow, null, true));
                }
            }

            if (isPipelineStageNeeded) {
                final Row pipelineStageRow = dataContainer.getRow(pipelineStageTable);

                if (Objects.nonNull(pipelineStageRow)) {
                    deal.setPipelineStage(pipelineStageManager.convertRowToModel(pipelineStageRow));
                }
            }

            if (isCompanyNeeded) {
                final Row companyRow = dataContainer.getRow(companyTable);

                if (Objects.nonNull(companyRow)) {
                    deal.setCompany(companyManager.convertRowToPartialModel(companyRow));
                }
            }
        }
    }

    /**
     * <p>
     * Loads the unavailable entity ids.
     * </p>
     *
     * @param deal                        the deal whose related entities are to be validated
     * @param unavailableContactIds       Holds the unavailable contact ids.
     * @param unavailableCompanyIds       Holds the unavailable company ids.
     * @param unavailablePipelineIds      Holds the unavailable pipeline ids.
     * @param unavailablePipelineStageIds Holds the unavailable pipelineStage ids.
     * @param fields                      Holds the fields of the deal.
     * @param targetContainer             Holds the updated information of user defined and system field values.
     */
    private void loadEntityIds(final Deal deal, final Collection<Long> dealIds, final Collection<Long> unavailableContactIds,
                               final Collection<Long> unavailableCompanyIds, final Collection<Long> unavailablePipelineIds,
                               final Collection<Long> unavailablePipelineStageIds, final Map<String, FieldInfo> fields,
                               final DataContainer targetContainer) {
        final Table contactTable = getContactTable();
        final Table participantTable = getParticipantTable();
        final Table companyTable = CompanyManager.getInstance().getTable();
        final Table pipelineTable = pipelineManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final Collection<Long> contactIds = new HashSet<>();
        final Long companyId = Optional.ofNullable(deal.getCompany()).map(Company::id).orElseGet(() -> null);
        final Collection<Participant> participants = deal.getParticipants();

        if (Objects.nonNull(participants)) {
            participants.forEach(participant -> {
                final Contact participantContact = participant.getContact();

                if (Objects.nonNull(participantContact)) {
                    final Long participantContactId = participantContact.id();

                    if (Objects.nonNull(participantContactId)) {
                        contactIds.add(participantContactId);
                    }
                }
            });
        }
        contactIds.add(deal.getPrimaryContact().id());
        targetContainer.getRows(getTable()).forEach(dealRow -> {
            final Long pkValue = dealRow.getPKValue();
            final Long companyFieldId = fields.get(DealFields.COMPANY).id();
            final WhereClause contactCriteria = new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), pkValue))
                    .and(Criteria.in(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactIds));
            final WhereClause companyCriteria = new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), companyFieldId))
                    .and(Criteria.eq(dealCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), pkValue))
                    .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyId));

            dealIds.add(pkValue);

            if (targetContainer.getRows(participantTable, new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), deal.getPrimaryContact().id()))).findAny().isEmpty()) {
                unavailableContactIds.addAll(contactIds);
            } else {
                loadEntityIds(MDOPARTICIPANT.CONTACT_ID, participantTable, contactTable, contactCriteria, targetContainer, unavailableContactIds);
            }
            loadEntityIds(dealRow.get(MDODEAL.PIPELINE_ID), pipelineTable, targetContainer, unavailablePipelineIds);
            loadEntityIds(dealRow.get(MDODEAL.PIPELINE_STAGE_ID), pipelineStageTable, targetContainer, unavailablePipelineStageIds);

            if (0 == targetContainer.size(dealCompanyTable)) {
                if (Objects.nonNull(companyId)) {
                    unavailableCompanyIds.add(companyId);
                }
            } else {
                loadEntityIds(MDOCOMPANYFIELDS.COMPANY_ID, dealCompanyTable, companyTable, companyCriteria, targetContainer, unavailableCompanyIds);
            }
        });
    }

    /**
     * <p>
     * Loads the unavailable entity ids.
     * </p>
     *
     * @param entityId             Represents the entity id.
     * @param entityTable          Represents the entity table.
     * @param targetContainer      Holds the updated information of user defined and system field values.
     * @param unavailableEntityIds Holds the unavailable entity ids.
     */
    private void loadEntityIds(final Long entityId, final Table entityTable, final DataContainer targetContainer,
                               final Collection<Long> unavailableEntityIds) {
        if (Objects.nonNull(entityId)) {
            final Row entityRow = targetContainer.getRow(entityTable, entityId);

            if (Objects.isNull(entityRow)) {
                unavailableEntityIds.add(entityId);
            }
        }
    }

    /**
     * <p>
     * Loads the unavailable entity ids.
     * </p>
     *
     * @param entityIdColumnName   Represents the column name of the entity id(COMPANY_ID, PHONE_ID, EMAIL_ID).
     * @param mappingTable         Represents the mapping table of the entity.
     * @param entityTable          Represents the entity table.
     * @param entityCriteria       Represents the criteria of the entity.
     * @param targetContainer      Holds the updated information of user defined and system field values.
     * @param unavailableEntityIds Holds the unavailable entity ids.
     */
    private void loadEntityIds(final String entityIdColumnName, final Table mappingTable, final Table entityTable,
                               final WhereClause entityCriteria, final DataContainer targetContainer,
                               final Collection<Long> unavailableEntityIds) {
        final Stream<Row> mappingRows = targetContainer.getRows(mappingTable, entityCriteria);

        mappingRows.forEach(mappingRow -> {
            final Long entityId = mappingRow.get(entityIdColumnName);
            final Row entityRow = targetContainer.getRow(entityTable, entityId);

            if (Objects.isNull(entityRow)) {
                unavailableEntityIds.add(entityId);
            }
        });
    }

    /**
     * Builds the deal stage and state progress select query
     *
     * @param dealTable   Represents the deal table
     * @param selectQuery Represents the deal select query
     * @param joinType    Represents the type of the joins needed.
     */
    private void buildDealProgressSelectQuery(final Table dealTable, final SelectQuery selectQuery, final JoinClause.JoinType joinType) {
        final Column dealPKColumn = dealTable.getPKColumn();
        final Table dealStageProgressTable = getStageProgressTable();

        selectQuery.addJoinClause(List.of(new JoinClause(dealPKColumn, dealStageProgressTable.getColumn(MDOSTAGEPROGRESS.DEAL_ID), joinType)));
        selectQuery.addSelectClause(dealStageProgressTable);
    }

    /**
     * <p>
     * Loads the deal details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param deal            An entity that holds the deal attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadDealIntoContainer(final boolean isCreateEvent, final Deal deal, final DataContainer targetContainer) {
        final Long orgId = deal.orgId();
        final Collection<FieldInfo> fields = getDealFields(orgId);
        final DataContainer entityContainer = isCreateEvent ? null
                : entityLoader.getEntityContainerByIds(orgId, getTable(), fields, List.of(deal.id()));

        loadDealIntoContainer(isCreateEvent, fields, deal, entityContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the deal details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param fields          Deal entity that have to be loaded into the container
     * @param deal            An entity that holds the deal attributes
     * @param sourceContainer Holds the user defined and system field values of existing deal
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    public void loadDealIntoContainer(final boolean isCreateEvent, final Collection<FieldInfo> fields, final Deal deal,
                                      final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table dealTable = getTable();
        final Long dealId = deal.id();
        final Map<String, String> oldDealValues = new HashMap<>();
        final Long currentTime = Instant.now().toEpochMilli();
        final Row dealRow = isCreateEvent ? new Row(dealTable)
                : Optional.ofNullable(sourceContainer.getRow(dealTable, dealId)).map(Row::new).orElse(null);
        final CrmUser createdBy = deal.getCreatedBy();
        final long orgId = deal.orgId();
        final SummaryInfo summaryInfo = deal.getSummaryInfo();
        final SourceTagManager sourceTagManager = SourceTagManager.getInstance();
        final Source source = Source.DEAL;
        final DateTime createdTime = deal.getCreatedTime();

        if (!isCreateEvent && Objects.isNull(dealRow)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }

        if (!isCreateEvent) {
            final DealValue dealValue = deal.getDealValue();

            if (Objects.nonNull(dealValue)) {

                if (Objects.isNull(deal.getProductDetailsHolder()) && productDetailsHolderManager.exists(orgId, dealId)) {
                    final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
                    final Row productDetailsHolderRow = sourceContainer.getRow(productDetailsHolderTable,
                            new WhereClause(Criteria.eq(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), dealId)));

                    if (Objects.nonNull(productDetailsHolderRow)) {
                        targetContainer.deleteRow(productDetailsHolderRow);
                    }
                }
                dealValue.setOrgId(orgId);
                dealValue.setId(dealRow.get(MDODEAL.MONETARY_ID));
                final SourceFollowers sourceFollowers = new SourceFollowers();

                sourceFollowers.setOrgId(orgId);
                sourceFollowers.setSource(Source.DEAL);
                sourceFollowers.setEntityId(dealId);
                sourceFollowersManager.removeSourceFollower(sourceFollowers, deal.getOwnerId(), targetContainer);
            }

            loadOldDealValues(deal, oldDealValues, dealRow, sourceContainer);

            if (deal.hasSalesOwner() && !Objects.equals(dealRow.get(MDODEAL.OWNER_ID), deal.getSalesOwner().id())) {
                dealRow.set(MDODEAL.LAST_ASSIGNED_AT, currentTime);
            }
        }

        if (dealRow.isNewRow()) {
            dealRow.setOrgId(orgId);
            deal.setId(dealRow.getPKValue());
            deal.setEntityState(Entity.State.NEW);
            dealRow.set(MDODEAL.CREATED_TIME, Objects.isNull(createdTime) ? currentTime : createdTime);
            dealRow.set(MDODEAL.STAGE_MODIFIED_TIME, currentTime);
            dealRow.set(MDODEAL.CREATED_BY, createdBy.id());
            dealRow.set(MDODEAL.LAST_ASSIGNED_AT, currentTime);
        } else {
            deal.setEntityState(Entity.State.EXISTING);

            if (Objects.nonNull(createdTime)) {
                dealRow.setIfModified(MDODEAL.CREATED_TIME, createdTime);
            }
        }

        if (!isCreateEvent) {
            loadFieldAudit(deal, sourceContainer, targetContainer);
        }
        dealRow.set(MDODEAL.MODIFIED_TIME, currentTime);
        loadDealRowIntoContainer(isCreateEvent, deal, dealRow, sourceContainer, targetContainer);
        loadDealTimeline(deal, isCreateEvent, oldDealValues, dealRow, sourceContainer, targetContainer);
        defaultFieldsDataLoader.loadData(new DataLoaderValues<>(isCreateEvent, dealTable, deal, fields,
                sourceContainer, targetContainer));
        entityLoader.buildEntity(isCreateEvent, deal, fields, dealTable, sourceContainer, targetContainer);

        final Collection<Tag> tags = Objects.nonNull(summaryInfo) ? summaryInfo.getTags() : Collections.emptyList();

        if (!tags.isEmpty()) {
            sourceTagManager.mapTagsWithSource(isCreateEvent, orgId, dealRow.getPKValue(), tags, Source.DEAL,
                    sourceContainer, targetContainer);
        }

        if (!isCreateEvent) {
            sourceTagManager.deleteMappedTags(dealId, tags, source, sourceContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the deal field edit history into the target container
     * </p>
     *
     * @param deal            An entity that holds the deal attributes
     * @param targetContainer Holds the updated information of user defined and system field values
     * @param sourceContainer Holds the existing deal sub-entity data
     */
    private void loadFieldAudit(final Deal deal, final DataContainer sourceContainer, final DataContainer targetContainer) {
        try {
            fieldAuditResolver.loadFieldAudit(deal, sourceContainer.getRow(getTable(), deal.id()), sourceContainer, targetContainer);
        } catch (Exception exception) {
            LOGGER.error("Field edit history : Error while loading history for deal id: {}", deal.id(), exception);
        }
    }

    /**
     * <p>
     * Loads the old values of the deal fields.
     * </p>
     *
     * @param oldDealRow    Represents the row of a deal table
     * @param deal          An entity that holds the deal attributes
     * @param oldDealValues Holds the existing values of deal fields
     * @param dataContainer Holds the user defined and system field values of existing deal
     */
    private void loadOldDealValues(final Deal deal, final Map<String, String> oldDealValues, final Row  oldDealRow,
                                   final DataContainer dataContainer) {
        final Table dealDropdownTable = Table.get(MDODROPDOWNFIELDS.TABLE);
        final Table dealCompanyTable = getDealCompanyTable();

        final Long dealPkValue = oldDealRow.getPKValue();
        final Long orgId = oldDealRow.getOrgId();
        final Date oldExpectedCloseDate = oldDealRow.get(MDODEAL.EXPECTED_CLOSE_DATE);
        final DateTime oldDealCreatedTime = oldDealRow.get(MDODEAL.CREATED_TIME);
        final Integer oldDealState = oldDealRow.get(MDODEAL.STATE);
        final Long oldContactId = oldDealRow.get(MDODEAL.CONTACT_ID);
        final Long oldPipelineStageId = oldDealRow.get(MDODEAL.PIPELINE_STAGE_ID);
        final String oldDealTitle = oldDealRow.get(MDODEAL.TITLE);
        final CrmUser salesOwner = deal.getSalesOwner();
        final Long oldSalesOwnerId = oldDealRow.get(MDODEAL.OWNER_ID);
        final Long oldPipelineId = oldDealRow.get(MDODEAL.PIPELINE_ID);
        final String lostReasonKey = DealFields.LOST_REASON;
        final String companyKey = DealFields.COMPANY;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, lostReasonKey, companyKey);
        final Long lostReasonFieldId = fields.get(lostReasonKey).id();
        final FieldInfo companyField = fields.get(companyKey);
        final WhereClause dealCompanyCriteria = new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealPkValue));

        final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable(), (Long) oldDealRow.get(MDODEAL.MONETARY_ID));
        final Row lostReasonRow = dataContainer.getRow(dealDropdownTable, new WhereClause(Criteria
                .eq(dealDropdownTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), dealPkValue))
                .and(Criteria.eq(dealDropdownTable.getColumn(MDODROPDOWNFIELDS.FIELD_ID), lostReasonFieldId)));
        final Row companyDealRow = dataContainer.getRow(dealCompanyTable, Objects.nonNull(companyField)
                ? dealCompanyCriteria.and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyField.id()))
                : dealCompanyCriteria);

        final Contact contact = deal.getPrimaryContact();
        final Company company = deal.getCompany();
        final PipelineStage pipelineStage = deal.getPipelineStage();
        final Pipeline pipeline = deal.getPipeline();

        if (Objects.nonNull(oldDealTitle)) {
            oldDealValues.put(DealFields.TITLE, oldDealTitle);
        }

        if (Objects.nonNull(oldDealState)) {
            oldDealValues.put(DealFields.STATUS, State.getStateById(oldDealState).getName());
        }

        if (Objects.nonNull(lostReasonRow)) {
            final Option option = optionService.get(orgId, lostReasonFieldId, lostReasonRow.get(MDODROPDOWNFIELDS.OPTION_ID));

            oldDealValues.put(DealFields.LOST_REASON, option.getOption());
        }

        if (Objects.nonNull(oldExpectedCloseDate)) {
            oldDealValues.put(DealFields.EXPECTED_CLOSE_DATE, String.valueOf(oldExpectedCloseDate.getTimeStamp()));
        }

        if (Objects.nonNull(oldDealCreatedTime)) {
            oldDealValues.put(DealFields.DEAL_CREATED, String.valueOf(oldDealCreatedTime.getTimeStamp()));
        }

        if (Objects.nonNull(pipelineStage) && !Objects.equals(oldPipelineStageId, pipelineStage.id())) {
            final String oldPipelineStageName = pipelineStageManager.getPipelineStageName(orgId, oldPipelineStageId);

            oldDealValues.put(DealFields.PIPELINE_STAGE, oldPipelineStageName);
        }

        if (Objects.nonNull(pipeline) && !Objects.equals(oldPipelineId, pipeline.id())) {
            oldDealValues.put(DealFields.PIPELINE, pipelineManager.partialGet(orgId, oldPipelineId).getName());
        }

        if (Objects.nonNull(contact) && !Objects.equals(oldContactId, contact.id())) {
            oldDealValues.put(DealFields.PRIMARY_CONTACT, ContactManager.getInstance().getContactFullName(orgId, oldDealRow.get(MDODEAL.CONTACT_ID)));
        }

        if (Objects.nonNull(companyDealRow)) {
            final Long companyId = companyDealRow.get(MDOCOMPANYFIELDS.COMPANY_ID);

            if (Objects.isNull(company)) {
                oldDealValues.put(DealFields.COMPANY, companyManager.getCompanyName(orgId, companyId));
            } else if (!Objects.equals(companyId, company.id())) {
                oldDealValues.put(DealFields.COMPANY, companyManager.getCompanyName(orgId, companyId));
            }
        }

        if (Objects.nonNull(monetaryRow)) {
            final Currency currency = Currency.getCurrencyById(monetaryRow.get(OMETMONETARY.CURRENCY_ID));

            oldDealValues.put(DealFields.DEAL_VALUE, Monetary.getValueWithSymbol(currency.getSymbol(),
                    monetaryRow.get(OMETMONETARY.VALUE)));
        }

        if (Objects.nonNull(salesOwner) && !Objects.equals(salesOwner.id(), oldSalesOwnerId)) {
            oldDealValues.put(DealFields.SALES_OWNER, Objects.isNull(oldSalesOwnerId) ? null : String.valueOf(oldSalesOwnerId));
        }
        final Long oldForecastCategoryId = oldDealRow.get(MDODEAL.FORECAST_CATEGORY_ID);
        final ForecastCategory forecastCategory = deal.getForecastCategory();
        final String forecastCategoryKey = DealFields.FORECAST_CATEGORY;

        if (Objects.nonNull(forecastCategory)) {
            if (Objects.isNull(oldForecastCategoryId)) {
                oldDealValues.put(forecastCategoryKey, null);
            } else {
                if (!Objects.equals(oldForecastCategoryId, forecastCategory.id())) {
                    final String oldForecastCategoryName = forecastCategoryManager.getName(orgId, oldForecastCategoryId).orElse(null);

                    oldDealValues.put(forecastCategoryKey, oldForecastCategoryName);
                }
            }
        } else if (Objects.nonNull(oldForecastCategoryId)) {
            final String oldForecastCategoryName = forecastCategoryManager.getName(orgId, oldForecastCategoryId).orElse(null);

            oldDealValues.put(forecastCategoryKey, oldForecastCategoryName);
        }
    }

    /**
     * <p>
     * Puts the non null value into the map
     * </p>
     *
     * @param map   Represents the map to which the key and value has to be put
     * @param key   Represents the key to be put in the map
     * @param value Represents the value to be put in the map
     */
    private static void putIfNonNull(final Map<String, String> map, final String key, final Object value) {
        if (Objects.nonNull(value)) {
            map.putIfAbsent(key, String.valueOf(value));
        }
    }

    /**
     * <p>
     * Loads the sub entities of deal table into the data container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param deal            An entity that holds the deal attributes
     * @param row             It represents the row of a deal table
     * @param sourceContainer Holds the user defined and system field values of existing deal
     * @param dataContainer   Holds the updated information of user defined and system field values
     */
    private void loadDealRowIntoContainer(final boolean isCreateEvent, final Deal deal, final Row row,
                                          final DataContainer sourceContainer, final DataContainer dataContainer) {
        final ContactManager contactManager = ContactManager.getInstance();
        final CompanyManager companyManager = CompanyManager.getInstance();
        final Pipeline pipeline = deal.getPipeline();
        final PipelineStage stage = deal.getPipelineStage();
        final Contact dealContact = deal.getPrimaryContact();
        final Collection<Participant> participants = deal.getParticipants();
        final Company company = deal.getCompany();
        final DealValue dealValue = deal.getDealValue();
        final Territory territory = deal.getTerritory();
        final CrmUser salesOwner = deal.getSalesOwner();
        final CrmUser updatedBy = deal.getUpdatedBy();
        final Long currentTime = Instant.now().toEpochMilli();
        final Long dealId = deal.id();
        final Table dealTable = getTable();
        final ForecastCategory forecastCategory = deal.getForecastCategory();

        if (Objects.nonNull(dealContact)) {

            if (Objects.isNull(dealContact.id())) {
                contactManager.loadContactIntoContainer(true, dealContact, dataContainer);
            }
            row.set(MDODEAL.CONTACT_ID, dealContact.id());
        }

        if (Objects.nonNull(participants) && !participants.isEmpty()) {

            for (final Participant participant : participants) {
                final Contact contact = participant.getContact();

                if (Objects.isNull(contact.id())) {
                    contactManager.loadContactIntoContainer(true, contact, dataContainer);
                }
            }
        }

        if (Objects.nonNull(company) && Objects.isNull(company.id())) {
            companyManager.loadCompanyIntoContainer(true, company, dataContainer);
        }

        if (!isCreateEvent) {
            final Stream<Row> dealRows = sourceContainer.getRows(dealTable);

            dealRows.filter(Objects::nonNull).forEach(dealRow -> {
                final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);

                if (Objects.nonNull(pipelineStageId) && !Objects.equals(pipelineStageId, stage.id())) {
                    row.set(MDODEAL.STAGE_MODIFIED_TIME, currentTime);
                }
            });
        }
        loadDealStageProgressIntoContainer(isCreateEvent, deal, dealTable, sourceContainer, dataContainer);
        row.set(MDODEAL.MODIFIED_TIME, currentTime);
        row.set(MDODEAL.PIPELINE_ID, pipeline.id());
        row.set(MDODEAL.PIPELINE_STAGE_ID, stage.id());
        row.set(MDODEAL.OWNER_ID, salesOwner.id());
        row.set(MDODEAL.UPDATED_BY, updatedBy.id());
        row.set(MDODEAL.TITLE, deal.getTitle());
        row.set(MDODEAL.EXPECTED_CLOSE_DATE, deal.getExpectedCloseDate());
        row.set(MDODEAL.DESCRIPTION, deal.getDescription());
        row.set(MDODEAL.CLOSED_TIME, deal.getClosedTime());
        row.set(MDODEAL.STATE, deal.getState().getId());
        row.set(MDODEAL.FORECAST_CATEGORY_ID, Objects.isNull(forecastCategory) ? null : forecastCategory.id());
        row.set(MDODEAL.TERRITORY_ID, Objects.nonNull(territory) ? territory.id() : null);
        row.set(MDODEAL.RECENT_NOTE, deal.getRecentNote());
        row.set(MDODEAL.STATUS, deal.isArchived() ? Deal.Status.ARCHIVED.getId() : Deal.Status.ACTIVE.getId());

        dataContainer.updateRow(row);
        loadDealCompanyIntoContainer(isCreateEvent, sourceContainer, row.getPKValue(),
                Objects.nonNull(company) ? company.id() : null, dataContainer);
        loadParticipants(isCreateEvent, deal, sourceContainer, dataContainer);

        if (Objects.nonNull(dealValue)) {

            if (sourceSettingsService.isSourceEnabled(deal.orgId(), PRODUCT_NAME)) {
                final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

                if (Objects.nonNull(productDetailsHolder)) {
                    productDetailsHolder.setDealId(dealId);
                    loadProductDetailsIntoContainer(productDetailsHolder, dataContainer);
                }
            }
            row.set(MDODEAL.MONETARY_ID, dealValue.id());
        }

        if (!isCreateEvent) {
            deleteUnmappedParticipants(deal, sourceContainer, dataContainer);
            deleteUnmappedCompany(dealId, company, sourceContainer, dataContainer);
        }
    }

    /**
     * <p>
     * Loads the product details holder into the container
     * </p>
     *
     * @param productDetailsHolder An entity that holds the product details holder attributes
     * @param dataContainer        Holds the updated information of user defined and system field values
     */
    private void loadProductDetailsIntoContainer(final ProductDetailsHolder productDetailsHolder, final DataContainer dataContainer) {
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailTable = productDetailsHolderManager.getProductDetailTable();
        final SelectQuery selectQuery = SelectQuery.get(productDetailsHolderTable);

        selectQuery.addJoinClause(new JoinClause(productDetailsHolderTable.getPKColumn(),
                productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID)));
        selectQuery.addSelectClause(productDetailTable, productDetailsHolderTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), productDetailsHolder.getDealId())));
        final DataContainer sourceDataContainer = orgDataStore(productDetailsHolder.orgId()).get(selectQuery);

        productDetailsHolderManager.loadProductDetailsHolderIntoContainer(sourceDataContainer.isEmpty(), productDetailsHolder, sourceDataContainer, dataContainer);
    }

    /**
     * <p>
     * Deletes the unmapped company details of the deal in data container
     * </p>
     *
     * @param dealId          Represents the id of the {@link Deal}.
     * @param company         Represents the entity of {@link Company}
     * @param sourceContainer Holds the user defined and system field values of existing contact
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void deleteUnmappedCompany(final Long dealId, final Company company, final DataContainer sourceContainer,
                                       final DataContainer targetContainer) {

        if (Objects.isNull(company)) {
            final String companyKey = DealFields.COMPANY;
            final Map<String, FieldInfo> dealFields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);

            if (dealFields.containsKey(companyKey)) {
                final Table dealCompanyTable = getDealCompanyTable();
                final Row dealCompanyRow = sourceContainer.getRow(dealCompanyTable, new WhereClause
                        (Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealId)).and(Criteria
                        .eq(dealCompanyTable.getColumn(MCONCOMPANYFIELDS.FIELD_ID), dealFields.get(companyKey).id())));

                if (Objects.nonNull(dealCompanyRow)) {
                    targetContainer.deleteRow(dealCompanyRow);
                }
            }
        }
    }

    /**
     * Deletes unmapped participants of the deal in the container
     *
     * @param deal            An entity that holds the deal details attributes
     * @param sourceContainer Holds the user defined and system field values of existing deal
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void deleteUnmappedParticipants(final Deal deal, final DataContainer sourceContainer,
                                            final DataContainer targetContainer) {
        final Table partcipantTable = ParticipantManager.getInstance().getTable();
        final Collection<Contact> contactList = new ArrayList<>();
        final Collection<Participant> participants = deal.getParticipants();

        if (Objects.nonNull(participants) && !participants.isEmpty()) {

            for (final Participant participant : deal.getParticipants()) {
                contactList.add(participant.getContact());
            }
        }
        contactList.add(deal.getPrimaryContact());
        final WhereClause whereClause = new WhereClause(Criteria.eq(partcipantTable.getColumn(MDOPARTICIPANT.DEAL_ID),
                deal.id())).and(Criteria.notIn(partcipantTable.getColumn(MDOPARTICIPANT.CONTACT_ID),
                contactList.stream().map(Contact::id).collect(Collectors.toUnmodifiableSet())));
        final Stream<Row> participantRows = sourceContainer.getRows(partcipantTable, whereClause);

        participantRows.forEach(targetContainer::deleteRow);
    }

    /**
     * Loads the deal progress stages into the container
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param deal            An entity that holds the deal attributes
     * @param dealTable       Represents the deal table
     * @param dataContainer   Holds the user defined and system field values of existing deal
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadDealStageProgressIntoContainer(final boolean isCreateEvent, final Deal deal, final Table dealTable,
                                                    final DataContainer dataContainer, final DataContainer targetContainer) {
        final Long dealId = deal.id();
        final PipelineStage pipelineStage = deal.getPipelineStage();
        final Long pipelineStageId = Objects.nonNull(pipelineStage) ? pipelineStage.id() :
                dataContainer.getRow(dealTable, dealId).get(MDODEAL.PIPELINE_STAGE_ID);
        final boolean isStageUpdated = !isCreateEvent && updateIfValidStageProgress(dealId, pipelineStageId, deal.getState().getId(), dealTable,
                dataContainer, targetContainer);

        if (!isStageUpdated) {
            final Row row = new Row(getStageProgressTable());

            if (Objects.isNull(pipelineStage)) {
                final Long orgId = deal.orgId();
                final Long pipelineId = getPipelineId(orgId, dealId);

                row.set(MDOSTAGEPROGRESS.PIPELINE_ID, pipelineId);
                row.set(MDOSTAGEPROGRESS.PIPELINE_STAGE_ID, pipelineStageManager.getPipelineStageId(orgId, deal.getState(), dealId));
            } else {
                row.set(MDOSTAGEPROGRESS.PIPELINE_STAGE_ID, pipelineStageId);
                row.set(MDOSTAGEPROGRESS.PIPELINE_ID, deal.getPipeline().id());
            }
            row.set(MDOSTAGEPROGRESS.DEAL_ID, dealId);
            row.set(MDOSTAGEPROGRESS.START_TIME, Instant.now().toEpochMilli());

            targetContainer.updateRow(row);
        }
    }

    /**
     * Updates the progress of a deal stage and returns whether the stage was updated or not
     *
     * @param dealId          Represents the deal id
     * @param pipelineStageId Represents the pipeline stage id
     * @param stateId         Represents the state id
     * @param dealTable       Represents the deal table
     * @param dataContainer   Holds the user defined and system field values of existing deal
     * @param targetContainer Holds the updated information of user defined and system field values
     * @return true if the stage is Exists
     */
    private boolean updateIfValidStageProgress(final Long dealId, final Long pipelineStageId, final int stateId,
                                               final Table dealTable, final DataContainer dataContainer,
                                               final DataContainer targetContainer) {
        final Row dealRow = dataContainer.getRow(dealTable, new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));
        final Long pipelineStage = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);
        final Integer state = dealRow.get(MDODEAL.STATE);
        final boolean isStateEqual = state.equals(stateId);

        if (!pipelineStage.equals(pipelineStageId) || !isStateEqual) {
            final Table stageProgressTable = getStageProgressTable();
            final Stream<Row> stageProgressRows = dataContainer.getRows(stageProgressTable, new WhereClause(Criteria.
                    eq(stageProgressTable.getColumn(MDOSTAGEPROGRESS.DEAL_ID), dealId)));
            final Row stageRow = stageProgressRows.filter(row -> (Objects.isNull(row.get(MDOSTAGEPROGRESS.END_TIME)))).
                    findFirst().orElse(null);

            if (Objects.nonNull(stageRow)) {
                stageRow.set(MDOSTAGEPROGRESS.END_TIME, Instant.now().toEpochMilli());
                targetContainer.updateRow(stageRow);

                return false;
            }
        }

        return true;
    }

    /**
     * Loads the deal and company mapping into the data container
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param sourceContainer Holds the user defined and system field values of existing deal
     * @param dealId          Represents the Deal ID
     * @param companyId       Represents the Company ID
     * @param dataContainer   Holds the updated information of user defined and system field values
     */
    private void loadDealCompanyIntoContainer(final boolean isCreateEvent, final DataContainer sourceContainer,
                                              final Long dealId, final Long companyId, final DataContainer dataContainer) {
        if (Objects.nonNull(dealId) && Objects.nonNull(companyId)) {
            final String companyKey = DealFields.COMPANY;
            final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);

            if (fields.containsKey(companyKey)) {
                final Table dealCompanyTable = getDealCompanyTable();
                final Row dealCompanyRow;
                final Long dealCompanyFieldId = fields.get(companyKey).id();

                if (isCreateEvent) {
                    dealCompanyRow = new Row(dealCompanyTable);
                } else {
                    final Row existingDealCompanyRow = sourceContainer.getRow(dealCompanyTable,
                            new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealId)).
                                    and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), dealCompanyFieldId)));
                    dealCompanyRow = Objects.isNull(existingDealCompanyRow) ? new Row(dealCompanyTable) : new Row(existingDealCompanyRow);
                }
                dealCompanyRow.set(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyId);

                if (dealCompanyRow.isNewRow()) {
                    dealCompanyRow.set(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealId);
                    dealCompanyRow.set(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), dealCompanyFieldId);
                }
                dataContainer.updateRow(dealCompanyRow);
            }
        }
    }

    /**
     * Gets the deal dynamic option table
     *
     * @return {@link Table}
     */
    private Table getDealDynamicOptionTable() {
        return Table.get(MDODROPDOWNFIELDS.TABLE);
    }

    /**
     * Gets the deal and company mapping table
     *
     * @return {@link Table}
     */
    public Table getDealCompanyTable() {
        return Table.get(MDOCOMPANYFIELDS.TABLE);
    }

    /**
     * Loads the deal participants row into the data container
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param deal            An entity that holds the deal attributes
     * @param sourceContainer Holds the user defined and system field values of existing deal
     * @param dataContainer   to be loaded with the participant data
     */
    private void loadParticipants(final boolean isCreateEvent, final Deal deal, final DataContainer sourceContainer,
                                  final DataContainer dataContainer) {
        final ParticipantManager participantManager = ParticipantManager.getInstance();
        final Table participantTable = participantManager.getTable();
        final Collection<Contact> contactList = new ArrayList<>();
        final Long dealId = deal.id();
        final Long orgId = deal.orgId();
        final Collection<Participant> participants = deal.getParticipants();

        if (Objects.nonNull(participants) && !participants.isEmpty()) {

            for (final Participant participant : deal.getParticipants()) {
                contactList.add(participant.getContact());
            }
        }
        contactList.add(deal.getPrimaryContact());

        for (final Contact contact : contactList) {
            final Long contactId = contact.id();
            final Row participantRow = isCreateEvent ? null : sourceContainer.getRow(participantTable, new WhereClause(
                    Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId))
                    .and(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactId)));

            if (Objects.isNull(participantRow)) {
                final Participant participant = new Participant();

                participant.setOrgId(orgId);
                participant.setDealId(dealId);
                participant.setContactId(contactId);

                participantManager.loadParticipantIntoContainer(true, false, participant, dataContainer);
            }
        }
    }

    /**
     * <p>
     * Converts row into {@link Deal}
     * </p>
     *
     * @param dealRow       Deal row to be converted
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link Deal}
     */
    public Deal convertRowToModel(final Row dealRow, final DataContainer dataContainer) {
        final Long orgId = dealRow.getOrgId();
        final Long dealId = dealRow.getPKValue();
        long processStartTime = System.currentTimeMillis();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);
        LOGGER.debug("Deal row conversion: fields fetched in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);

        final Deal deal = new Deal();

        final Long salesOwnerId = dealRow.get(MDODEAL.OWNER_ID);
        final Long createdById = dealRow.get(MDODEAL.CREATED_BY);
        final Long updatedById = dealRow.get(MDODEAL.UPDATED_BY);
        final Long forecastCategoryId = dealRow.get(MDODEAL.FORECAST_CATEGORY_ID);
        final Collection<Long> userIds = new HashSet<>();

        final Table dealCompanyTable = getDealCompanyTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table dynamicOptionTable = optionManager.getTable();
        final Table dealDynamicOptionTable = getDealDynamicOptionTable();
        final Table pipeLineTable = pipelineManager.getTable();
        final Table territoryTable = Table.get(OMCRMTERRITORY.TABLE);

        final Row territoryRow = dataContainer.getRow(territoryTable, new WhereClause(Criteria
                .eq(territoryTable.getPKColumn(), dealRow.get(MDODEAL.TERRITORY_ID))));
        final Row dealCompanyRow = dataContainer.getRow(dealCompanyTable, new WhereClause(Criteria
                .eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealId)).and(Criteria.
                eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fields.get(DealFields.COMPANY).id())));
        final Row monetaryRow = dataContainer.getRow(monetaryTable, (Long) dealRow.get(MDODEAL.MONETARY_ID));
        final Row pipelineRow = dataContainer.getRow(pipeLineTable, (Long) dealRow.get(MDODEAL.PIPELINE_ID));
        final DateTime closedTime = dealRow.get(MDODEAL.CLOSED_TIME);
        final Collection<Row> dynamicOptionRows = dataContainer.getRows(dealDynamicOptionTable,
                new WhereClause(Criteria.eq(dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), dealId))).toList();

        if (!dynamicOptionRows.isEmpty()) {
            final Long typeFieldId = fields.get(DealFields.TYPE).id();
            final Long paymentFieldId = fields.get(DealFields.PAYMENT_STATUS).id();
            final Long lostReasonFieldId = fields.get(DealFields.LOST_REASON).id();
            final Long wonReasonFieldId = fields.get(DealFields.WON_REASON).id();
            final Long sourceFieldId = fields.get(DealFields.DEAL_SOURCE).id();
            final Column dynamicOptionIdColumn = dynamicOptionTable.getPKColumn();
            processStartTime = System.currentTimeMillis();

            for (final Row dynamicOptionRow : dynamicOptionRows) {
                final Row optionRow = dataContainer.getRow(dynamicOptionTable, new WhereClause(Criteria
                        .eq(dynamicOptionIdColumn, dynamicOptionRow.get(MDODROPDOWNFIELDS.OPTION_ID))));

                if (Objects.nonNull(optionRow)) {
                    final Long fieldId = optionRow.get(OMETOPTION.FIELD_ID);
                    final Option option = optionManager.convertRowToModel(optionRow);

                    if (Objects.equals(typeFieldId, fieldId)) {
                        deal.setType(option);
                    } else if (Objects.equals(paymentFieldId, fieldId)) {
                        deal.setPaymentStatus(option);
                    } else if (Objects.equals(lostReasonFieldId, fieldId)) {
                        deal.setLostReason(option);
                    } else if (Objects.equals(wonReasonFieldId, fieldId)) {
                        deal.setWonReason(option);
                    } else if (Objects.equals(sourceFieldId, fieldId)) {
                        deal.setSource(option);
                    }
                }
            }
            LOGGER.debug("Deal row conversion: Dynamic options converted in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }

        if (Objects.nonNull(forecastCategoryId)) {
            final Table forecastCategoryTable = forecastCategoryManager.getTable();
            final Row forecastCategoryRow = dataContainer.getRow(forecastCategoryTable, forecastCategoryId);

            if (Objects.nonNull(forecastCategoryRow)) {
                final ForecastCategory forecastCategory = new ForecastCategory();

                forecastCategory.setId(forecastCategoryId);
                forecastCategory.setName(forecastCategoryRow.get(OMQFFORECASTCATEGORY.NAME));
                deal.setForecastCategory(forecastCategory);
            }
        }

        if (Objects.nonNull(pipelineRow)) {
            processStartTime = System.currentTimeMillis();
            deal.setPipeline(pipelineManager.convertRowToModel(pipelineRow, dataContainer, false));
            final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);
            final Collection<PipelineStage> pipelineStages = deal.getPipeline().getPipelineStages();

            if (Objects.nonNull(pipelineStages)) {
                pipelineStages.stream().filter(pipelineStage -> Objects.equals(pipelineStageId, pipelineStage.id()))
                        .findFirst().ifPresent(deal::setPipelineStage);
            }
            LOGGER.debug("Deal row conversion: Dynamic options converted in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }

        if (Objects.nonNull(territoryRow)) {
            processStartTime = System.currentTimeMillis();
            final Territory territory = new Territory();

            territory.setId(territoryRow.getPKValue());
            territory.setName(territoryRow.get(OMCRMTERRITORY.NAME));
            deal.setTerritory(territory);
            LOGGER.debug("Deal row conversion: Territory row converted in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }

        if (Objects.nonNull(salesOwnerId)) {
            processStartTime = System.currentTimeMillis();
            deal.setSalesOwner(userService.getUser(orgId, salesOwnerId));
            userIds.add(salesOwnerId);
            LOGGER.debug("Deal row conversion: Sales owner converted in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }

        if (Objects.nonNull(createdById)) {
            processStartTime = System.currentTimeMillis();
            deal.setCreatedBy(userService.getUser(orgId, createdById));
            userIds.add(createdById);
            LOGGER.debug("Deal row conversion: Created by converted in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }

        if (Objects.nonNull(updatedById)) {
            processStartTime = System.currentTimeMillis();
            deal.setUpdatedBy(userService.getUser(orgId, updatedById));
            userIds.add(updatedById);
            LOGGER.debug("Deal row conversion: Updated by converted in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }
        deal.setId(dealId);
        deal.setOrgId(orgId);
        deal.setTitle(dealRow.get(MDODEAL.TITLE));

        if (Objects.isNull(dealCompanyRow)) {
            deal.setCompany(null);
        } else {
            processStartTime = System.currentTimeMillis();
            deal.setCompany(convertRowToCompany(orgId, dealCompanyRow, dataContainer));
            LOGGER.debug("Deal row conversion: Company row converted in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }
        deal.setOwnerId(dealRow.get(MDODEAL.OWNER_ID));
        deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
        deal.setModifiedTime(dealRow.get(MDODEAL.MODIFIED_TIME));
        deal.setCreatedTime(dealRow.get(MDODEAL.CREATED_TIME));
        deal.setStageUpdatedAt(dealRow.get(MDODEAL.STAGE_MODIFIED_TIME));
        deal.setClosedTime(closedTime);
        deal.setDescription(dealRow.get(MDODEAL.DESCRIPTION));
        deal.setCampaign(dealRow.get(MDODEAL.CAMPAIGN));
        deal.setLastAssignedAt(dealRow.get(MDODEAL.LAST_ASSIGNED_AT));
        deal.setRecentNote(dealRow.get(MDODEAL.RECENT_NOTE));
        deal.setStatus(dealRow.get(MDODEAL.STATUS));
        setTotalProduct(deal, dataContainer);
        final DateTime modifiedTime = dealRow.get(MDODEAL.MODIFIED_TIME);

        if (Objects.nonNull(modifiedTime)) {
            deal.setModifiedTime(dealRow.get(MDODEAL.MODIFIED_TIME));
        }
        final DateTime createdTime = dealRow.get(MDODEAL.CREATED_TIME);

        if (Objects.nonNull(createdTime)) {
            deal.setCreatedTime(createdTime);
        }
        final PipelineStage pipelineStage = deal.getPipelineStage();

        if (Objects.nonNull(pipelineStage)) {
            deal.setStageProbability(pipelineStage.getProbability());
        }

        if (Objects.nonNull(monetaryRow)) {
            processStartTime = System.currentTimeMillis();
            final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);
            final DealValue dealValue = new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue());
            LOGGER.debug("Deal row conversion: Monetary row converted in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);

            if (sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME)) {
                processStartTime = System.currentTimeMillis();
                deal.setProductDetailsHolder(getProductDetailsHolder(dealId, dataContainer));
                LOGGER.debug("Deal row conversion: Product details set in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
            }
            deal.setDealValue(dealValue);
        }
        processStartTime = System.currentTimeMillis();
        final Collection<String> fieldsToBeEvaluated = Set.of(DealFields.AGE, DealFields.EXPECTED_DEAL_VALUE,
                DealFields.WEIGHTED_VALUE, DealFields.ROTTING_DAYS, DealFields.PRODUCT_AMOUNT,
                DealFields.DEAL_VALUE_IN_BASE_CURRENCY);
        LOGGER.debug("Deal row conversion: Dynamic fields fetched in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);

        for (final String fieldName : fieldsToBeEvaluated) {
            processStartTime = System.currentTimeMillis();
            final FieldInfo fieldInfo = fields.get(fieldName);
            final String fieldAliasName = fieldInfo.aliasName();

            dynamicDealFieldEvaluatorHandler.computeFieldValue(fieldAliasName, deal);
            LOGGER.debug("Deal row conversion: {} value computed in {}ms. Org ID: {}, Deal ID: {}", fieldAliasName, System.currentTimeMillis() - processStartTime, orgId, dealId);
        }
        final Integer state = dealRow.get(MDODEAL.STATE);

        if (Objects.nonNull(state)) {
            deal.setState(State.getStateById(state));
        }
        final Long primaryContactId = dealRow.get(MDODEAL.CONTACT_ID);

        processStartTime = System.currentTimeMillis();
        loadPrimaryContact(dataContainer, primaryContactId, deal);
        LOGGER.debug("Deal row conversion: Primary contact loaded in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);

        processStartTime = System.currentTimeMillis();
        loadDealParticipants(dataContainer, deal);
        LOGGER.debug("Deal row conversion: Deal participants are loaded in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);

        updateDealWonLostTime(deal);
        processStartTime = System.currentTimeMillis();
        entityLoader.setEntityValues(deal, getTable(), dataContainer);
        LOGGER.debug("Deal row conversion: Entity values (custom field values) are set in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        final ValueHolder valueHolder = deal.getValueHolder();

        if (Objects.nonNull(valueHolder)) {
            processStartTime = System.currentTimeMillis();
            userIds.addAll(valueHolder.getUserValues().stream().map(CrmUser::id).collect(Collectors.toSet()));
            LOGGER.debug("Deal row conversion: user values are collected in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }
        deal.setUserIds(userIds);
        processStartTime = System.currentTimeMillis();
        final Collection<Tag> tags = SourceTagManager.getInstance().buildTags(dealId, Source.DEAL, dataContainer);
        LOGGER.debug("Deal row conversion: Source tags are built in {}ms. Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);

        if (Objects.nonNull(tags) && !tags.isEmpty()) {
            final SummaryInfo summaryInfo = new SummaryInfo();

            summaryInfo.setTags(tags);
            deal.setSummaryInfo(summaryInfo);
        }

        return deal;
    }

    /**
     * <p>
     * Load the participants of the deal
     * </p>
     *
     * @param dataContainer Represents the {@link DataContainer}
     * @param deal          Represents the {@link Deal}
     */
    private void loadDealParticipants(final DataContainer dataContainer, final Deal deal) {
        final Contact primaryContact = deal.getPrimaryContact();

        if (Objects.nonNull(primaryContact)) {
            final Long primaryContactId = primaryContact.id();
            final Table contactTable = contactManager.getTable();
            final Table participantTable = Table.get(MDOPARTICIPANT.TABLE);

            final Stream<Row> participantRows = dataContainer.getRows(participantTable,
                    new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), deal.id())));
            final Collection<Long> contactIds = participantRows.filter(Objects::nonNull).map(participantRow ->
                    (Long) participantRow.get(MDOPARTICIPANT.CONTACT_ID)).collect(Collectors.toSet());

            if (!contactIds.isEmpty()) {
                final Stream<Row> contactRows = dataContainer.getRows(contactTable,
                        new WhereClause(Criteria.in(contactTable.getPKColumn(), contactIds)));
                final Collection<Participant> participants = new LinkedHashSet<>();
                final Table emailTable = emailManager.getTable();
                final Table phoneTable = phoneManager.getTable();

                contactRows.forEach(contactRow -> {
                    if (!Objects.equals(primaryContactId, contactRow.getPKValue())) {
                        final Participant participant = new Participant();
                        final Contact contact = contactManager.convertRowToPartialModel(contactRow);
                        final Row emailRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailTable.getPKColumn(),
                                contactRow.get(MCOCONTACT.PRIMARY_EMAIL))));
                        final Row phoneRow = dataContainer.getRow(phoneTable, new WhereClause(Criteria.eq(phoneTable.getPKColumn(),
                                contactRow.get(MCOCONTACT.PRIMARY_PHONE))));

                        if (Objects.nonNull(phoneRow)) {
                            contact.setPrimaryPhone(phoneManager.convertRowToModel(phoneRow));
                        }

                        if (Objects.nonNull(emailRow)) {
                            contact.setPrimaryEmail(emailManager.convertRowToModel(emailRow));
                        }

                        participant.setContact(contact);
                        participants.add(participant);
                    }
                });
                deal.setParticipants(participants);
            }
        }
    }

    /**
     * <p>
     * Load the primary contact into the deal
     * </p>
     *
     * @param dataContainer    Represents the {@link DataContainer}
     * @param primaryContactId Represents the deal {@link Row}
     * @param deal             Represents the {@link Deal}
     */
    private void loadPrimaryContact(final DataContainer dataContainer, final Long primaryContactId, final Deal deal) {
        final Table contactTable = contactManager.getTable();
        final Row primaryContactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria
                .eq(contactTable.getPKColumn(), primaryContactId)));

        if (Objects.nonNull(primaryContactRow)) {
            final Contact contact = contactManager.convertRowToPartialModel(primaryContactRow);
            final Table emailTable = emailManager.getTable();
            final Table phoneTable = phoneManager.getTable();
            final Row emailRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailTable.getPKColumn(),
                    primaryContactRow.get(MCOCONTACT.PRIMARY_EMAIL))));
            final Row phoneRow = dataContainer.getRow(phoneTable, new WhereClause(Criteria.eq(phoneTable.getPKColumn(),
                    primaryContactRow.get(MCOCONTACT.PRIMARY_PHONE))));

            if (Objects.nonNull(phoneRow)) {
                contact.setPrimaryPhone(phoneManager.convertRowToModel(phoneRow));
            }

            if (Objects.nonNull(emailRow)) {
                contact.setPrimaryEmail(emailManager.convertRowToModel(emailRow));
            }
            deal.setPrimaryContact(contact);
        }
    }

    /**
     * Converts the row into {@link Company}
     *
     * @param orgId         Refers the organization id.
     * @param row           Holds the attribute values of {@link Company}
     * @param dataContainer A container that holds the user defined field values and system values
     * @return {@link Company}
     */
    private Company convertRowToCompany(final Long orgId, final Row row, final DataContainer dataContainer) {
        final Table companyTable = CompanyManager.getInstance().getTable();
        final Optional<Row> optionalCompanyRow = Optional.ofNullable(dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(),
                row.get(MCONCOMPANYFIELDS.COMPANY_ID)))));

        return optionalCompanyRow.map(companyRow -> {
            final Company company = new Company();

            company.setId(companyRow.getPKValue());
            company.setOrgId(orgId);
            company.setName(companyRow.get(MCOCOMPANY.NAME));

            return company;
        }).orElseGet(() -> null);
    }

    /**
     * <p>
     * Gets the product details holder by deal
     * </p>
     *
     * @param dealId It represents deal id
     * @return {@link ProductDetailsHolder}
     */
    private ProductDetailsHolder getProductDetailsHolder(final Long dealId, final DataContainer dataContainer) {
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Row row = dataContainer.getRow(productDetailsHolderTable, new WhereClause(Criteria.eq(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID),
                dealId)));

        return Objects.nonNull(row) ? productDetailsHolderManager.convertRowToModel(row, dataContainer) : null;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId organisation ID
     * @param ids   represents the collection of deal IDs
     * @return the {@link Deal} with partial data
     */
    @Override
    public Collection<Deal> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(MDODEAL.TITLE));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

        return orgDataStore(orgId).get(selectQuery).getRows(table).map(this::convertRowToPartialModel).toList();
    }

    /**
     * Gets the total deal count by pipeline and pipeline stage
     *
     * @param orgId           organisation ID
     * @param pipelineId      pipeline ID
     * @param pipelineStageId pipeline stage ID
     * @return the {@link Collection} of deals by pipeline and it's stage
     */
    public long getDealsCountByPipelineAndStage(final Long orgId, final Long pipelineId, final Long pipelineStageId) {
        final Table dealTable = getTable();
        final CountQuery countQuery = CountQuery.get(dealTable);

        countQuery.setWhereClause(buildDealsByPipelineAndStageCriteria(dealTable, pipelineId, pipelineStageId));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Gets the {@link WhereClause} to filter the deals by pipeline and pipeline stage IDs
     *
     * @param dealTable       Represents the deal table
     * @param pipelineId      Represents the pipeline ID
     * @param pipelineStageId Represents the pipeline stage ID
     * @return {@link WhereClause} which contains the {@link Criteria} to filter deals
     */
    private WhereClause buildDealsByPipelineAndStageCriteria(final Table dealTable, final Long pipelineId,
                                                             final Long pipelineStageId) {
        final WhereClause condition = new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineId));

        return Objects.isNull(pipelineStageId) ? condition
                : condition.and(Criteria.eq(dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pipelineStageId));
    }

    /**
     * Fetches the total deal value by pipeline and pipeline stage
     *
     * @param orgId           Represents the organisation ID
     * @param pipelineId      Represents the pipeline ID
     * @param pipelineStageId Represents the pipeline stage ID
     * @return the total deal value of given pipeline and pipeline stage
     */
    public BigDecimal getDealsValueByPipelineAndStage(final Long orgId, final Long pipelineId, final Long pipelineStageId) {
        final Table dealTable = getTable();
        final Table monetaryTable = monetaryManager.getTable();

        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(List.of(monetaryTable.getColumn(OMETMONETARY.VALUE), monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID)));
        selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryTable.getPKColumn()));
        selectQuery.setWhereClause(buildDealsByPipelineAndStageCriteria(dealTable, pipelineId, pipelineStageId));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> monetaryRows = dataContainer.getRows(monetaryTable);

        final AtomicReference<BigDecimal> totalValue = new AtomicReference<>(BigDecimal.ZERO);

        if (Objects.nonNull(monetaryRows)) {
            final ReadableExchangeRateContainer readableExchangeRateContainer = baseCurrencyService.getExchangeRate(orgId);
            final Currency defaultCurrency = baseCurrencyService.getDefaultCurrency(orgId);

            monetaryRows.forEach(row -> {
                final Currency currency = Currency.getCurrencyById(row.get(OMETMONETARY.CURRENCY_ID));
                final BigDecimal dealValue = BigDecimal.valueOf((Double) row.get(OMETMONETARY.VALUE));
                final BigDecimal updatedValue;

                if (!Objects.equals(defaultCurrency, currency) && Objects.nonNull(readableExchangeRateContainer)) {
                    updatedValue = readableExchangeRateContainer.getExchangeRate(currency).multiply(dealValue);
                } else {
                    updatedValue = dealValue;
                }

                totalValue.getAndUpdate(updatedValue::add);
            });
        }

        return totalValue.get();
    }

    /**
     * Gets the deals with its linked activities by pipeline and pipeline stage
     *
     * @param pipelineStage      Represents the {@link PipelineStage}
     * @param needSummary        Flag that represents the summary of pipeline stages is need or not
     * @param stageJson          Represents the {@link JsonObject} with deals related to the pipeline stage
     * @param start              Represents the start value
     * @param limit              Represents the limit value
     * @param filterCriteriaList Which contains the {@link Collection} of {@link FilterCriteria}
     * @return the {@link Collection} of deals and its associated activities by pipeline and it's stage
     */
    public Map<Deal, Collection<Activity>> getDealsAndLinkedActivitiesByPipelineAndStage(final PipelineStage pipelineStage,
                                                                                         final boolean needSummary,
                                                                                         final JsonObject stageJson,
                                                                                         final int start, final int limit,
                                                                                         final Collection<FilterCriteria> filterCriteriaList,
                                                                                         final SortConfig sortConfig) {
        final CompanyManager companyManager = CompanyManager.getInstance();
        final Table dealTable = getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table contactTable = getContactTable();
        final Table pipelineTable = pipelineManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table participantTable = getParticipantTable();
        final Table dealActivityTable = sourceActivityManager.getDealActivityTable();
        final Table activityTypeTable = activityTypeManager.getTable();
        final Table activityTable = sourceActivityManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final JoinClause.JoinType leftJoin = LEFT;
        final Column dealPkColumn = dealTable.getPKColumn();
        final Table companyTable = companyManager.getTable();
        final Column activityTypeIdColumn = activityTable.getColumn(OMACACTIVITY.TYPE_ID);
        final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
        final Column activityDealIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID);
        final Column isDoneColumn = activityTable.getColumn(OMACACTIVITY.IS_DONE);
        final Column contactIdColumn = dealTable.getColumn(MDODEAL.CONTACT_ID);
        final Column contactPKColumn = contactTable.getPKColumn();
        final Column pipelineIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_ID);
        final Column pipelineStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column participantDealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column createdTimeColumn = dealTable.getColumn(MDODEAL.CREATED_TIME);
        final Column primaryEmailColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL);
        final Column primaryPhoneColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Long orgId = pipelineStage.orgId();
        final Long pipelineId = pipelineStage.getPipelineId();
        final Long pipelineStageId = pipelineStage.id();
        final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);
        final Column contactOrgIdColumn = contactTable.getColumn(MCOCONTACT.ORG_ID);
        final Table userTable = Table.get(OMCRMUSER.TABLE);
        final Table forecastCategoryTable = forecastCategoryManager.getTable();

        selectQuery.addJoinClause(List.of(new JoinClause(contactIdColumn, contactPKColumn),
                new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()),
                new JoinClause(pipelineIdColumn, pipelineTable.getPKColumn()),
                new JoinClause(pipelineStageIdColumn, pipelineStageTable.getPKColumn()),
                new JoinClause(primaryEmailColumn, emailTable.getPKColumn(), leftJoin),
                new JoinClause(primaryPhoneColumn, phoneTable.getPKColumn(), leftJoin),
                new JoinClause(dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID), forecastCategoryTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(List.of(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID),
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.PROBABILITY), pipelineStageTable.getColumn(MDOPIPELINESTAGE.IS_DEFAULT),
                companyTable.getColumn(MCOCOMPANY.NAME), companyTable.getColumn(MCOCOMPANY.ORG_ID),
                pipelineTable.getColumn(MDOPIPELINE.ROTTEN_DAYS), firstNameColumn,
                lastNameColumn, contactTable.getPKColumn(), contactOrgIdColumn,
                primaryEmailColumn, emailTable.getColumn(OMETEMAIL.TYPE), emailTable.getColumn(OMETEMAIL.VALUE),
                primaryPhoneColumn, phoneTable.getColumn(OMETPHONE.NUMBER), phoneTable.getColumn(OMETPHONE.COUNTRY_ID),
                forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.NAME)));
        final String companyKey = DealFields.COMPANY;
        final Map<String, FieldInfo> dealFields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);

        if (dealFields.containsKey(companyKey)) {
            final FieldInfo companyFieldInfo = dealFields.get(companyKey);
            final Table companyDerivedTable = companyManager.getCompanyFieldTable(companyFieldInfo, dealCompanyTable);

            selectQuery.addSelectClause(companyDerivedTable);
            selectQuery.addJoinClause(new JoinClause(dealPkColumn, companyDerivedTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), leftJoin));
            selectQuery.addJoinClause(new JoinClause(companyDerivedTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), leftJoin));
        }
        selectQuery.addSelectClause(dealTable, monetaryTable);
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final WhereClause pipelineStageCriteria = new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.ORG_ID), orgId))
                .and(Criteria.eq(pipelineIdColumn, pipelineId)).and(Criteria.eq(pipelineStageIdColumn, pipelineStageId))
                .and(Criteria.eq(dealTable.getColumn(MDODEAL.STATUS), Deal.Status.ACTIVE.getId()));

        if (Objects.nonNull(filterCriteriaList) && !filterCriteriaList.isEmpty()) {
            filterCriteriaHandler.applyCriteria(orgId, Provider.DEAL_LIST, selectQuery, filterCriteriaList);
        }
        final WhereClause whereClause = selectQuery.getWhereClause();

        selectQuery.setWhereClause(Objects.isNull(whereClause) ? pipelineStageCriteria : WhereClauseUtil.connect(whereClause, pipelineStageCriteria, Connector.AND));
        final Optional<SortCriteria> optionalSortCriteria = sortConfig.getSortCriteriaList().stream().findFirst();

        if (optionalSortCriteria.isPresent()) {
            final SortCriteria sortCriteria = optionalSortCriteria.get();
            final OrderType orderType = SortType.ASCENDING == sortCriteria.getSortType() ? OrderType.ASCENDING : OrderType.DESCENDING;
            final DealKanbanSortFields sortField = DealKanbanSortFields.getById(sortCriteria.getFieldId().intValue());

            switch (sortField) {
                case CONTACT_NAME -> selectQuery.addOrderByClause(new OrderByClause(firstNameColumn, orderType));
                case COMPANY_NAME ->
                        selectQuery.addOrderByClause(new OrderByClause(companyTable.getColumn(MCOCOMPANY.NAME), orderType));
                case DEAL_TITLE ->
                        selectQuery.addOrderByClause(new OrderByClause(dealTable.getColumn(MDODEAL.TITLE), orderType));
                case DEAL_VALUE ->
                        selectQuery.addOrderByClause(new OrderByClause(monetaryTable.getColumn(OMETMONETARY.VALUE), orderType));
                case DEAL_CLOSED_ON ->
                        selectQuery.addOrderByClause(new OrderByClause(dealTable.getColumn(MDODEAL.CLOSED_TIME), orderType));
                case UPDATED_AT ->
                        selectQuery.addOrderByClause(new OrderByClause(dealTable.getColumn(MDODEAL.MODIFIED_TIME), orderType));
                case CREATED_BY, UPDATED_BY, SALES_OWNER ->
                        selectQuery.addOrderByClause(new OrderByClause(userTable.getColumn(OMCRMUSER.NAME), orderType));
                default -> selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn, orderType));
            }

            final Optional<String> optionalUserSortFieldColumn = switch (sortField) {
                case CREATED_BY -> Optional.of(MDODEAL.CREATED_BY);
                case UPDATED_BY -> Optional.of(MDODEAL.UPDATED_BY);
                case SALES_OWNER -> Optional.of(MDODEAL.OWNER_ID);
                default -> Optional.empty();
            };

            optionalUserSortFieldColumn.ifPresent(column -> {
                selectQuery.addSelectClause(userTable.getColumn(OMCRMUSER.NAME));
                selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(column), userTable.getPKColumn()));
            });
        }
        final Currency defaultCurrency = baseCurrencyService.getDefaultCurrency(orgId);
        final BiConsumer<Long, BigDecimal> stageSummaryHandler = (totalDeal, totalDealValue) -> {
            final JsonObject stageSummary = Json.object();
            final JsonObject currencyJson = Json.object();

            stageSummary.put(PipelineStageFields.TOTAL_DEALS.getName(), totalDeal);
            stageSummary.put(PipelineStageFields.TOTAL_VALUE.getName(), baseCurrencyService.applyPrecision(orgId, totalDealValue));
            stageSummary.put(PipelineStageFields.PROBABLE_VALUE.getName(), baseCurrencyService.applyPrecision(orgId,
                    totalDealValue.multiply(BigDecimal.valueOf(pipelineStage.getProbability() / 100))));
            currencyJson.put(PipelineStageFields.SYMBOL.getName(), defaultCurrency.getSymbol());
            currencyJson.put(PipelineStageFields.CODE.getName(), defaultCurrency.getCode());
            stageSummary.put(PipelineStageFields.CURRENCY.getName(), currencyJson);
            stageJson.put(PipelineStageFields.SUMMARY.getName(), stageSummary);
        };
        final long time = System.currentTimeMillis();
        final SelectQuery summarySelectQuery = SelectQuery.get(dealTable);
        final Column currencyIdColumn = monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID);

        summarySelectQuery.addSelectClause(List.of(currencyIdColumn, monetaryTable.getColumn(OMETMONETARY.VALUE, TOTAL_VALUE).sum(),
                dealTable.getColumn(MDODEAL.ID, DEAL_COUNT).countWithDistinct()));
        summarySelectQuery.addJoinClause(selectQuery.getJoinClause());
        summarySelectQuery.setWhereClause(selectQuery.getWhereClause());
        summarySelectQuery.addGroupByClause(new GroupByClause(currencyIdColumn));

        final List<Map<String, Object>> container = orgDataStore(orgId).getData(summarySelectQuery);

        LOGGER.debug("Deal Kanban: Time taken to fetch the summary query took {}ms, Org ID: {}, Stage ID: {}", System.currentTimeMillis() - time, orgId, pipelineStageId);
        final short defaultCurrencyId = defaultCurrency.getId();
        final long summaryConversionTime = System.currentTimeMillis();
        BigDecimal totalValues = BigDecimal.ZERO;
        long totalCount = 0;

        for (final Map<String, Object> row : container) {
            final short currencyId = (short) row.get(OMETMONETARY.CURRENCY_ID);
            final BigDecimal bigDecimalValue = Optional.ofNullable((Number) row.get(TOTAL_VALUE))
                    .map(value -> BigDecimal.valueOf(value.doubleValue()))
                    .orElseGet(() -> BigDecimal.ZERO);
            totalCount += (Long) row.get(DEAL_COUNT);

            if (!Objects.equals(defaultCurrencyId, currencyId)) {
                totalValues = totalValues.add(bigDecimalValue.divide(baseCurrencyService.getExchangeRate(defaultCurrency,
                        Currency.getCurrencyById(currencyId)), NumericalConstant.FIVE, RoundingMode.HALF_EVEN));
            } else {
                totalValues = totalValues.add(bigDecimalValue);
            }
        }
        LOGGER.debug("Deal Kanban: Summary conversion of deals took {}ms, Org ID: {}, Stage ID: {}", System.currentTimeMillis() - summaryConversionTime, orgId, pipelineStageId);

        if (needSummary) {
            stageSummaryHandler.accept(totalCount, totalValues);
        }
        stageJson.put(PipelineStageFields.TOTAL_RECORDS.getName(), totalCount);
        stageJson.put(PipelineStageFields.HAS_MORE.getName(), (start + limit) <= totalCount);

        if (0 >= totalCount || start > totalCount) {
            return Collections.emptyMap();
        } else {
            final long dealQueryTime = System.currentTimeMillis();
            final DataContainer entityContainer = orgDataStore(orgId).get(selectQuery);

            LOGGER.debug("Deal Kanban: Time taken to fetch the deal query took {}ms, Org ID: {}, Stage ID: {}", System.currentTimeMillis() - dealQueryTime, orgId, pipelineStageId);
            final Collection<Row> dealRows = entityContainer.getRows(dealTable).toList();

            if (dealRows.isEmpty()) {

                if (needSummary) {
                    stageSummaryHandler.accept(0L, BigDecimal.ZERO);
                }

                return Collections.emptyMap();
            }
            final Map<Deal, Collection<Activity>> dealMap = new LinkedHashMap<>();
            final SelectQuery activitySelectQuery = SelectQuery.get(activityTable);
            final Collection<Long> dealIds = dealRows.stream().map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
            activitySelectQuery.addJoinClause(List.of(
                    new JoinClause(activityTable.getPKColumn(), dealActivityIdColumn),
                    new JoinClause(activityTypeIdColumn, activityTypeTable.getPKColumn())));
            activitySelectQuery.addSelectClause(List.of(activityDealIdColumn, dealActivityIdColumn,
                    activityTable.getColumn(OMACACTIVITY.TITLE), activityTable.getColumn(OMACACTIVITY.ORG_ID),
                    isDoneColumn, activityTypeIdColumn, activityTypeTable.getColumn(OMACACTIVITYTYPE.NAME),
                    activityTypeTable.getColumn(OMACACTIVITYTYPE.ICON_ID), activityTypeTable.getColumn(OMACACTIVITYTYPE.IS_ACTIVE),
                    activityTable.getColumn(OMACACTIVITY.START_TIME), activityTable.getColumn(OMACACTIVITY.END_TIME), activityTable.getColumn(OMACACTIVITY.CREATOR_ID)));
            activitySelectQuery.setWhereClause(new WhereClause(Criteria.in(activityDealIdColumn, dealIds)).and(Criteria.eq(isDoneColumn, false)));
            optionalSortCriteria.ifPresent(sortCriteria -> {

                if (DealKanbanSortFields.LAST_ACTIVITY_DATE.equals(DealKanbanSortFields.getById(sortCriteria.getFieldId().intValue()))) {
                    final OrderType orderType = SortType.ASCENDING == sortCriteria.getSortType() ? OrderType.ASCENDING : OrderType.DESCENDING;

                    activitySelectQuery.addOrderByClause(new OrderByClause(activityTable.getColumn(OMACACTIVITY.COMPLETED_TIME), orderType));
                }
            });
            final long activityQueryTime = System.currentTimeMillis();
            final DataContainer activityDataContainer = orgDataStore(orgId).get(activitySelectQuery);

            LOGGER.debug("Deal Kanban: Time taken to fetch the activity query took {}ms, Org ID: {}, Stage ID: {}", System.currentTimeMillis() - activityQueryTime, orgId, pipelineStageId);
            final SelectQuery dealParticipantSelectQuery = SelectQuery.get(participantTable);
            final Column participanContactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);

            dealParticipantSelectQuery.addSelectClause(List.of(firstNameColumn, lastNameColumn, participantDealIdColumn, participanContactIdColumn, contactOrgIdColumn));
            dealParticipantSelectQuery.addJoinClause(List.of(new JoinClause(participantDealIdColumn, dealPkColumn),
                    new JoinClause(participanContactIdColumn, contactPKColumn)));
            dealParticipantSelectQuery.setWhereClause(new WhereClause(Criteria.in(dealPkColumn, dealIds)));

            final long participantQueryTime = System.currentTimeMillis();
            final DataContainer dealParticipantDataContainer = orgDataStore(orgId).get(dealParticipantSelectQuery);

            LOGGER.debug("Deal Kanban: Time taken to fetch the participant query took {}ms, Org ID: {}, Stage ID: {}", System.currentTimeMillis() - participantQueryTime, orgId, pipelineStageId);
            final long conversionTime = System.currentTimeMillis();

            for (final Row dealRow : dealRows) {
                final long convertTime = System.currentTimeMillis();
                final Deal deal = convertRowForKanban(dealRow, entityContainer);

                LOGGER.debug("Deal Kanban: Time taken to convert a deal took {}ms, Org ID: {}, Stage ID: {}", System.currentTimeMillis() - convertTime, orgId, pipelineStageId);

                loadDealParticipants(dealParticipantDataContainer, deal);
                dealMap.put(deal, getRelatedActivities(deal.id(), activityDataContainer));
            }

            LOGGER.debug("Deal Kanban: Time taken to complete the conversion of rows to deals in {}ms, Org ID: {}, Stage ID: {}", System.currentTimeMillis() - conversionTime, orgId, pipelineStageId);
            return dealMap;
        }
    }

    /**
     * <p>
     * Gets activities by deal.
     * </p>
     *
     * @param orgId An organization's id.
     * @param id    Deal's id.
     * @return {@link Collection<Activity>}.
     */
    public Collection<Activity> getActivitiesByDeal(final Long orgId, final Long id) {
        final Table dealActivityTable = sourceActivityManager.getDealActivityTable();
        final Table activityTable = sourceActivityManager.getTable();
        final Column dealActivityIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.ACTIVITY_ID);
        final Column activityDealIdColumn = dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID);
        final Column isDoneColumn = activityTable.getColumn(OMACACTIVITY.IS_DONE);

        final SelectQuery activitySelectQuery = SelectQuery.get(activityTable);
        activitySelectQuery.addJoinClause(new JoinClause(activityTable.getPKColumn(), dealActivityIdColumn));
        activitySelectQuery.addSelectClause(List.of(activityDealIdColumn, dealActivityIdColumn,
                activityTable.getColumn(OMACACTIVITY.TITLE), activityTable.getColumn(OMACACTIVITY.ORG_ID),
                isDoneColumn, activityTable.getColumn(OMACACTIVITY.START_TIME), activityTable.getColumn(OMACACTIVITY.END_TIME),
                activityTable.getColumn(OMACACTIVITY.CREATOR_ID)));
        activitySelectQuery.setWhereClause(new WhereClause(Criteria.eq(activityDealIdColumn, id)).and(Criteria.eq(isDoneColumn, false)));
        final DataContainer dataContainer = orgDataStore(orgId).get(activitySelectQuery);

        return orgDataStore(orgId).get(activitySelectQuery).getRows(activityTable).map(activityRow ->
                sourceActivityManager.convertRowToActivityHolder(activityRow, dataContainer).getActivity()).toList();
    }

    /**
     * Converts a database row into a {@code Deal} object for Kanban view.
     *
     * @param dealRow       the row data representing a deal from the result set
     * @param dataContainer contextual container holding additional metadata or references
     * @return a Deal object structured for use in Kanban displays
     */
    private Deal convertRowForKanban(final Row dealRow, final DataContainer dataContainer) {
        final String companyKey = DealFields.COMPANY;
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);
        final Deal deal = new Deal();
        final Long orgId = dealRow.getOrgId();
        final Long dealId = dealRow.getPKValue();
        final Table forecastCategoryTable = forecastCategoryManager.getTable();

        final Table monetaryTable = monetaryManager.getTable();
        final Table pipeLineTable = pipelineManager.getTable();
        final Row monetaryRow = dataContainer.getRow(monetaryTable, (Long) dealRow.get(MDODEAL.MONETARY_ID));
        final Row pipelineRow = dataContainer.getRow(pipeLineTable, (Long) dealRow.get(MDODEAL.PIPELINE_ID));
        final DateTime closedTime = dealRow.get(MDODEAL.CLOSED_TIME);
        final Long primaryContactId = dealRow.get(MDODEAL.CONTACT_ID);
        long processStartTime = System.currentTimeMillis();

        if (Objects.nonNull(pipelineRow)) {
            deal.setPipeline(pipelineManager.convertRowToModel(pipelineRow, dataContainer, false));
            final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);
            final Collection<PipelineStage> pipelineStages = deal.getPipeline().getPipelineStages();

            if (Objects.nonNull(pipelineStages)) {
                pipelineStages.stream().filter(pipelineStage -> Objects.equals(pipelineStageId, pipelineStage.id()))
                        .findFirst().ifPresent(deal::setPipelineStage);
            }
            LOGGER.debug("Deal Kanban: Model conversion of pipeline and stage took {}ms, Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }
        deal.setId(dealId);
        deal.setOrgId(orgId);
        deal.setTitle(dealRow.get(MDODEAL.TITLE));
        deal.setOwnerId(dealRow.get(MDODEAL.OWNER_ID));
        deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
        deal.setModifiedTime(dealRow.get(MDODEAL.MODIFIED_TIME));
        deal.setCreatedTime(dealRow.get(MDODEAL.CREATED_TIME));
        deal.setStageUpdatedAt(dealRow.get(MDODEAL.STAGE_MODIFIED_TIME));
        deal.setClosedTime(closedTime);
        deal.setLastAssignedAt(dealRow.get(MDODEAL.LAST_ASSIGNED_AT));

        final DateTime modifiedTime = dealRow.get(MDODEAL.MODIFIED_TIME);
        final DateTime createdTime = dealRow.get(MDODEAL.CREATED_TIME);
        final Integer state = dealRow.get(MDODEAL.STATE);

        if (Objects.nonNull(modifiedTime)) {
            deal.setModifiedTime(dealRow.get(MDODEAL.MODIFIED_TIME));
        }

        if (Objects.nonNull(createdTime)) {
            deal.setCreatedTime(createdTime);
        }

        if (fields.containsKey(companyKey)) {
            final Table dealCompanyTable = getDealCompanyTable();
            final Row dealCompanyRow = dataContainer.getRow(dealCompanyTable,
                    new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealId))
                            .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fields.get(companyKey).id())));

            deal.setCompany(Objects.isNull(dealCompanyRow) ? null : convertRowToCompany(orgId, dealCompanyRow, dataContainer));
        }
        deal.setStageProbability(deal.getPipelineStage().getProbability());
        processStartTime = System.currentTimeMillis();

        if (Objects.nonNull(monetaryRow)) {
            final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);
            final DealValue dealValue = new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue());

            if (sourceSettingsService.isSourceEnabled(orgId, PRODUCT_NAME)) {
                deal.setProductDetailsHolder(getProductDetailsHolder(dealId, dataContainer));
            }
            deal.setDealValue(dealValue);
            LOGGER.debug("Deal Kanban: Model conversion of monetary took {}ms, Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);
        }
        final Long forecastCategoryId = dealRow.get(MDODEAL.FORECAST_CATEGORY_ID);

        if (Objects.nonNull(forecastCategoryId)) {
            final Row forecastCategoryRow = dataContainer.getRow(forecastCategoryTable, forecastCategoryId);
            final ForecastCategory forecastCategory = new ForecastCategory();

            forecastCategory.setId(forecastCategoryRow.getPKValue());
            forecastCategory.setName(forecastCategoryRow.get(OMQFFORECASTCATEGORY.NAME));
            deal.setForecastCategory(forecastCategory);
        }
        if (Objects.nonNull(state)) {
            deal.setState(State.getStateById(state));
        }
        processStartTime = System.currentTimeMillis();

        loadPrimaryContact(dataContainer, primaryContactId, deal);
        LOGGER.debug("Deal Kanban: Model conversion of contact took {}ms, Org ID: {}, Deal ID: {}", System.currentTimeMillis() - processStartTime, orgId, dealId);

        updateDealWonLostTime(deal);

        return deal;
    }

    /**
     * Updates the deal's won or lost time using the stage updated time.
     *
     * @param deal   The deal to update
     */
    private void updateDealWonLostTime(final Deal deal) {
        final State dealState = deal.getState();
        final DateTime stageUpdatedAt = deal.getStageUpdatedAt();

        if (State.WON == dealState) {
            deal.setWonTime(stageUpdatedAt);
        } else if (State.LOST == dealState) {
            deal.setLostTime(stageUpdatedAt);
        }
    }

    /**
     * <p>
     * Gets the activities related to the deal.
     * </p>
     *
     * @param dealId        Represents the id of the {@link Deal}
     * @param dataContainer Represents the entity of {@link DataContainer}
     * @return the collection of {@link Activity} related to the deal.
     */
    public Collection<Activity> getRelatedActivities(final Long dealId, final DataContainer dataContainer) {
        final Table dealActivityTable = sourceActivityManager.getDealActivityTable();
        final Table activityTable = sourceActivityManager.getTable();

        final Stream<Row> dealActivityRows = dataContainer.getRows(dealActivityTable, new WhereClause(Criteria
                .eq(dealActivityTable.getColumn(MACDEALACTIVITY.DEAL_ID), dealId)));
        final Stream<Row> activityRows = dataContainer.getRows(activityTable, new WhereClause(Criteria
                .in(activityTable.getPKColumn(), dealActivityRows.map(dealActivityRow ->
                        dealActivityRow.get(MACDEALACTIVITY.ACTIVITY_ID)).collect(Collectors.toUnmodifiableSet()))));

        return activityRows.map(activityRow ->
                sourceActivityManager.convertRowToActivityHolder(activityRow, dataContainer).getActivity()).toList();
    }

    /**
     * Fetches the {@link Deal} for the dropdown actions.
     *
     * @param orgId            Represents an organization id.
     * @param dropdownLimit    Represents the {@link DropdownLimit}
     * @param dropdownCriteria Represents the {@link DropdownCriteria}
     * @param skipArchived     Specifies whether to include the archived deal
     * @return {@link Collection} of deals.
     */
    public Collection<Deal> getDeals(final long orgId, final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria,
                                     final boolean skipArchived) {
        final DataContainer dataContainer = orgDataStore(orgId).get(getSelectQuery(dropdownLimit, dropdownCriteria, skipArchived));

        return dataContainer.getRows(getTable()).map(row -> convertRowToPartialModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * Gets the {@link SelectQuery} with the provided criteria and limit value
     * </p>
     *
     * @param limit           Represents the {@link DropdownLimit}
     * @param criteria        Represents the {@link Criteria}
     * @param skipArchived    Specifies whether to skip the archived deals
     * @return {@link SelectQuery}
     */
    private SelectQuery getSelectQuery(final DropdownLimit limit, final DropdownCriteria criteria, final boolean skipArchived) {
        final Table dealTable = getTable();
        final Table stageTable = pipelineStageManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Column dealTitleColumn = dealTable.getColumn(MDODEAL.TITLE);
        final Column stageNameColumn = stageTable.getColumn(MDOPIPELINESTAGE.NAME);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column dealMonetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column dealStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Table contactTable = getContactTable();
        final Column contactIdColumn = dealTable.getColumn(MDODEAL.CONTACT_ID);

        selectQuery.addSelectClause(List.of(dealTable.getPKColumn(), dealTitleColumn, stageNameColumn,
                dealMonetaryIdColumn, dealStageIdColumn, contactIdColumn, contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)));
        selectQuery.addSelectClause(monetaryTable);
        selectQuery.addJoinClause(List.of(new JoinClause(dealStageIdColumn, stageTable.getPKColumn()),
                new JoinClause(dealMonetaryIdColumn, monetaryTable.getPKColumn(), LEFT),
                new JoinClause(contactIdColumn, contactTable.getPKColumn(), LEFT)));
        selectQuery.setLimitClause(limit.getLimitClause());
        selectQuery.setWhereClause(new WhereClause(criteria.build(dealTitleColumn)).or(criteria.build(stageNameColumn)));

        if (skipArchived) {
            selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.STATUS), Deal.Status.ACTIVE.getId())), Connector.AND));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link Deal}
     * </p>
     *
     * @param row           Represents the {@link Row}
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link Deal}
     */
    private Deal convertRowToPartialModel(final Row row, final DataContainer dataContainer) {
        final Table dealCompanyTable = getDealCompanyTable();
        final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable(), (Long) row.get(MDODEAL.MONETARY_ID));
        final Row pipelineStageRow = dataContainer.getRow(pipelineStageManager.getTable(), (Long) row.get(MDODEAL.PIPELINE_STAGE_ID));
        final Row contactRow = dataContainer.getRow(getContactTable(), (Long) row.get(MDODEAL.CONTACT_ID));
        final Row dealCompanyRow = dataContainer.getRow(dealCompanyTable, new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), row.getPKValue())));
        final Deal deal = convertRowToPartialModel(row);

        if (Objects.nonNull(monetaryRow)) {
            deal.setDealValue(new DealValue(monetaryManager.convertRowToModel(monetaryRow)));
        }

        if (Objects.nonNull(pipelineStageRow)) {
            deal.setPipelineStage(pipelineStageManager.convertRowToModel(pipelineStageRow));
        }

        if (Objects.nonNull(contactRow)) {
            deal.setPrimaryContact(contactManager.convertRowToPartialModel(contactRow));
        }

        if (Objects.nonNull(dealCompanyRow)) {
            final Row companyRow = dataContainer.getRow(companyManager.getTable(), (Long) dealCompanyRow.get(MDOCOMPANYFIELDS.COMPANY_ID));

            if (Objects.nonNull(companyRow)) {
                deal.setCompany(companyManager.convertRowToPartialModel(companyRow));
            }
        }

        return deal;
    }

    /**
     * Gets the related contact and company details by deal id.
     *
     * @param orgId  It represents the organization id
     * @param dealId It represents the deal id
     * @return {@link Collection<Deal>}
     */
    public Deal getRelatedEntitiesByDealId(final Long orgId, final Long dealId) {
        final CompanyManager companyManager = CompanyManager.getInstance();
        final Table dealTable = getTable();
        final Table contactTable = ContactManager.getInstance().getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column dealPkColumn = dealTable.getPKColumn();
        final Table dealCompanyTable = getDealCompanyTable();
        final Column contactPKColumn = contactTable.getPKColumn();
        final Table companyTable = companyManager.getTable();
        final Column contactIdColumn = dealTable.getColumn(MDODEAL.CONTACT_ID);
        final Column companyIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID);
        final JoinClause.JoinType leftJoin = LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(contactIdColumn, contactPKColumn),
                new JoinClause(dealTable.getPKColumn(), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(companyIdColumn, companyTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(List.of(contactIdColumn, contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME), companyIdColumn, dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID),
                companyTable.getColumn(MCOCOMPANY.NAME), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPkColumn, dealId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Deal deal = new Deal();

        if (dataContainer.isEmpty()) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        } else {
            final Row dealRow = dataContainer.getRow(dealTable);
            final Long contactId = dealRow.get(MDODEAL.CONTACT_ID);
            final Row contactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactPKColumn, contactId)));

            if (Objects.nonNull(contactRow)) {
                final Contact contact = new Contact();

                contact.setId(contactId);
                contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));

                deal.setPrimaryContact(contact);
            }
            final String companyKey = DealFields.COMPANY;
            final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);

            if (fields.containsKey(companyKey)) {
                final Row dealCompanyRow = dataContainer.getRow(dealCompanyTable,
                        new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealRow.getPKValue())).
                                and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fields.get(companyKey).id())));

                if (Objects.nonNull(dealCompanyRow)) {
                    final Row companyRow = dataContainer.getRow(companyTable, new WhereClause(Criteria.eq(companyTable.getPKColumn(),
                            dealCompanyRow.get(MDOCOMPANYFIELDS.COMPANY_ID))));

                    if (Objects.nonNull(companyRow)) {
                        deal.setCompany(companyManager.convertRowToPartialModel(companyRow));
                    }
                }
            }
        }

        return deal;
    }

    /**
     * Returns the total number of deals associated with a given contact, filtered by the provided criteria.
     *
     * @param orgId            the organization ID for data scoping
     * @param contactId        the ID of the contact whose deals should be counted
     * @param dropdownCriteria search/filter criteria to apply on deal titles
     * @return total number of matching deals for the specified contact
     */
    public long getTotalCountByContact(final long orgId, final Long contactId, final DropdownCriteria dropdownCriteria) {
        final Table dealTable = getTable();
        final Table participantTable = getParticipantTable();
        final CountQuery countQuery = CountQuery.get(dealTable);
        final Criteria criteria = dropdownCriteria.build(dealTable.getColumn(MDODEAL.TITLE));
        final WhereClause whereClause = Objects.isNull(contactId) ? new WhereClause(criteria)
                : new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactId)).and(criteria);

        countQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), participantTable.getColumn(MDOPARTICIPANT.DEAL_ID)));
        countQuery.setWhereClause(whereClause);
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Retrieves a collection of {@link Deal} objects associated with a given contact,
     * filtered and paginated based on dropdown parameters.
     *
     * @param orgId            the organization ID for data scoping
     * @param contactId        the ID of the contact to fetch deals
     * @param dropdownLimit    limit and offset information for pagination
     * @param dropdownCriteria criteria for filtering deals (e.g., by title)
     * @return a collection of deals matching the contact and filter criteria
     */
    public Collection<Deal> getDealsByContact(final long orgId, final Long contactId, final DropdownLimit dropdownLimit,
                                              final DropdownCriteria dropdownCriteria) {
        final Table dealTable = getTable();
        final Table participantTable = getParticipantTable();
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column titleColumn = dealTable.getColumn(MDODEAL.TITLE);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Criteria criteria = dropdownCriteria.build(titleColumn);
        final WhereClause whereClause = Objects.isNull(contactId) ? new WhereClause(criteria)
                : new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactId)).and(criteria);

        selectQuery.addSelectClause(titleColumn);
        selectQuery.addJoinClause(new JoinClause(dealPkColumn, participantTable.getColumn(MDOPARTICIPANT.DEAL_ID)));
        selectQuery.setWhereClause(whereClause);
        selectQuery.addOrderByClause(new OrderByClause(dealPkColumn));
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(dealTable).map(this::convertRowToPartialModel).toList();
    }

    /**
     * Returns the total number of deals associated with a given company, filtered by the provided criteria.
     *
     * @param orgId            the organization ID for data scoping
     * @param companyId        the ID of the company whose deals should be counted
     * @param dropdownCriteria search/filter criteria to apply on deal titles
     * @return total number of matching deals for the specified company
     */
    public long getTotalCountByCompany(final long orgId, final Long companyId, final DropdownCriteria dropdownCriteria) {
        final Table dealTable = getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final CountQuery countQuery = CountQuery.get(dealTable);
        final Criteria criteria = dropdownCriteria.build(dealTable.getColumn(MDODEAL.TITLE));

        if (Objects.isNull(companyId)) {
            countQuery.setWhereClause(new WhereClause(criteria));
        } else {
            final FieldInfo fieldInfo = fieldService.getDefaultFieldByAliasName(Source.DEAL, DealFields.COMPANY).orElse(null);

            if (Objects.nonNull(fieldInfo)) {
                countQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID)));
                countQuery.setWhereClause(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyId))
                        .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fieldInfo.id()))
                        .and(criteria));
            } else {
                countQuery.setWhereClause(new WhereClause(criteria));
            }
        }
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Retrieves a collection of {@link Deal} objects associated with a given company,
     * filtered and paginated based on dropdown parameters.
     *
     * @param orgId            the organization ID for data scoping
     * @param companyId        the ID of the company to fetch deals
     * @param dropdownLimit    limit and offset information for pagination
     * @param dropdownCriteria criteria for filtering deals (e.g., by title)
     * @return a collection of deals matching the company and filter criteria
     */
    public Collection<Deal> getDealsByCompany(final long orgId, final Long companyId, final DropdownLimit dropdownLimit,
                                              final DropdownCriteria dropdownCriteria) {
        final Table dealTable = getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column titleColumn = dealTable.getColumn(MDODEAL.TITLE);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Criteria criteria = dropdownCriteria.build(titleColumn);

        selectQuery.addSelectClause(titleColumn);

        if (Objects.nonNull(companyId)) {
            final FieldInfo fieldInfo = fieldService.getDefaultFieldByAliasName(Source.DEAL, DealFields.COMPANY).orElse(null);

            if (Objects.nonNull(fieldInfo)) {
                selectQuery.addJoinClause(new JoinClause(dealPkColumn, dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID)));
                selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyId))
                        .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fieldInfo.id()))
                        .and(criteria));
            } else {
                selectQuery.setWhereClause(new WhereClause(criteria));
            }
        } else {
            selectQuery.setWhereClause(new WhereClause(criteria));
        }
        selectQuery.addOrderByClause(new OrderByClause(dealPkColumn));
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(dealTable).map(this::convertRowToPartialModel).toList();
    }

    /**
     * Returns the total number of deals, delegating to the appropriate source-specific method
     * based on the given {@link Source} and entity ID.
     *
     * <p>If {@code source} and {@code entityId} are both non-null, the count is scoped to the
     * matching contact or company. Otherwise, falls back to the general count.</p>
     *
     * @param orgId            the organization ID for data scoping
     * @param entityId         the ID of the source entity (contact or company)
     * @param source           the {@link Source} used to determine the filter type (contact or company)
     * @param dropdownCriteria search/filter criteria to apply on deal titles
     * @param skipArchived     specifies whether to exclude archived deals
     * @return total number of matching deals
     */
    public long getTotalCount(final long orgId, final Long entityId, final Source source,
                              final DropdownCriteria dropdownCriteria, final boolean skipArchived) {
        if (Objects.nonNull(source) && Objects.nonNull(entityId)) {
            if (source.isContact()) {
                return getTotalCountByContact(orgId, entityId, dropdownCriteria);
            } else if (source.isCompany()) {
                return getTotalCountByCompany(orgId, entityId, dropdownCriteria);
            }
        }
        return getTotalCount(orgId, dropdownCriteria, skipArchived);
    }

    /**
     * Retrieves a collection of {@link Deal} objects, delegating to the appropriate source-specific
     * method based on the given {@link Source} and entity ID.
     *
     * <p>If {@code source} and {@code entityId} are both non-null, deals are scoped to the
     * matching contact or company. Otherwise, falls back to the general deal retrieval.</p>
     *
     * @param orgId            the organization ID for data scoping
     * @param entityId         the ID of the source entity (contact or company)
     * @param source           the {@link Source} used to determine the filter type (contact or company)
     * @param dropdownLimit    limit and offset information for pagination
     * @param dropdownCriteria criteria for filtering deals (e.g., by title)
     * @param skipArchived     specifies whether to exclude archived deals
     * @return a collection of matching deals
     */
    public Collection<Deal> getDeals(final long orgId, final Long entityId, final Source source,
                                     final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria,
                                     final boolean skipArchived) {
        if (Objects.nonNull(source) && Objects.nonNull(entityId)) {
            if (source.isContact()) {
                return getDealsByContact(orgId, entityId, dropdownLimit, dropdownCriteria);
            } else if (source.isCompany()) {
                return getDealsByCompany(orgId, entityId, dropdownLimit, dropdownCriteria);
            }
        }
        return getDeals(orgId, dropdownLimit, dropdownCriteria, skipArchived);
    }

    /**
     * Updates the deal owner and (optionally) its territory, and timeline.
     *
     * @param orgId     The organization in which the update is applied.
     * @param id        The identifier of the record being updated.
     * @param timeline  The timeline entry representing the assignment update.
     * @param ownerId   The newly assigned owner for the record.
     * @param territory The resolved territory to assign; may be {@code null} if no territory update is required.
     */
    public void updateOwnerAndTerritory(final long orgId, final Long id, final Timeline timeline,
                                        final long ownerId, final Territory territory) {
        final Table table = getTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        Optional.ofNullable(orgDataStore.get(table, id)).ifPresent(row -> {
            final DataContainer targetContainer = DataContainer.create();
            final Long existingOwnerId = row.get(MDODEAL.OWNER_ID);
            final Long existingTerritoryId = row.get(MDODEAL.TERRITORY_ID);

            row.set(MDODEAL.OWNER_ID, ownerId);

            if (Objects.nonNull(territory)) {
                row.set(MDODEAL.TERRITORY_ID, territory.id());
            }
            targetContainer.updateRow(row);

            if (Objects.nonNull(timeline)) {
                loadDealTimelineRowIntoContainer(timeline, id, targetContainer);
                final FieldAuditInfo editContext = timeline.getFieldAuditInfo();

                fieldAuditHandler.loadFieldAuditEntry(orgId, Source.DEAL, id, null, editContext, FieldKeys.SALES_OWNER.getName(),
                        Objects.isNull(existingOwnerId) ? null : String.valueOf(existingOwnerId), String.valueOf(ownerId), targetContainer);

                if (Objects.nonNull(territory)) {
                    fieldAuditHandler.loadFieldAuditEntry(orgId, Source.COMPANY, id, null, editContext, DealFields.TERRITORY,
                            Objects.isNull(existingTerritoryId) ? null : territoryManager.getName(orgId, existingTerritoryId),
                            territory.getName(), targetContainer);
                }
            }
            orgDataStore.commitChanges(targetContainer);
        });
    }

    /**
     * <p>
     * Gets deal summary by deal id
     * </p>
     *
     * @param orgId  It represents organization id
     * @param dealId It represents deal id
     * @return {@link Deal} An entity that holds the deal attributes
     */
    public Deal getDealSummary(final Long orgId, final Long dealId) {
        final ContactManager contactManager = ContactManager.getInstance();
        final CompanyManager companyManager = CompanyManager.getInstance();
        final Table dealTable = getTable();
        final Deal deal = new Deal();
        final Table pipelineTable = pipelineManager.getTable();
        final Table contactTable = contactManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final Table companyTable = companyManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Column pipelineIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_ID);
        final Column pipelineStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column monetaryColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column dealContactIdColumn = dealTable.getColumn(MDODEAL.CONTACT_ID);
        final Column pipelinePKColumn = pipelineTable.getPKColumn();
        final Column pipelineStagePKColumn = pipelineStageTable.getPKColumn();
        final Column monetaryPKColumn = monetaryTable.getPKColumn();
        final Column contactPkColumn = contactTable.getPKColumn();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table contactPhoneTable = contactManager.getContactPhoneTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Table companyAliasTable = Table.get(MCOCOMPANY.TABLE, MCOCOMPANY.TABLE);
        final Column dealCompanyIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID);
        final Column contactCompanyIdColumn = contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID);
        final Column territoryIdColumn = dealTable.getColumn(MDODEAL.TERRITORY_ID);
        final Column ownerIdColumn = dealTable.getColumn(MDODEAL.OWNER_ID);
        final JoinClause.JoinType leftJoin = LEFT;
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailTable = productDetailsHolderManager.getProductDetailTable();
        final Column prductDetailsHolderIdColumn = productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID);
        final Column productDetailsHolderPKColumn = productDetailsHolderTable.getPKColumn();
        final Column productDetailsHolderDealIdColumn = productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID);
        final Column dealStateColumn = dealTable.getColumn(MDODEAL.STATE);

        selectQuery.addJoinClause(List.of(new JoinClause(pipelineIdColumn, pipelinePKColumn),
                new JoinClause(pipelineStageIdColumn, pipelineStagePKColumn),
                new JoinClause(monetaryColumn, monetaryPKColumn), new JoinClause(dealContactIdColumn, contactPkColumn),
                new JoinClause(dealTable.getPKColumn(), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(dealCompanyIdColumn, companyTable.getPKColumn(), leftJoin),
                new JoinClause(contactTable.getPKColumn(), contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailTable.getPKColumn(), leftJoin),
                new JoinClause(contactTable.getPKColumn(), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), phoneTable.getPKColumn(), leftJoin),
                new JoinClause(contactTable.getPKColumn(), contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactCompanyIdColumn, companyAliasTable.getPKColumn(), leftJoin),
                new JoinClause(dealTable.getPKColumn(), productDetailsHolderDealIdColumn, leftJoin),
                new JoinClause(productDetailsHolderPKColumn, prductDetailsHolderIdColumn, leftJoin)));

        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.ORG_ID), pipelineStageIdColumn,
                dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), pipelineIdColumn, pipelineTable.getColumn(MDOPIPELINE.NAME),
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), monetaryTable.getColumn(OMETMONETARY.VALUE),
                monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID), monetaryTable.getColumn(OMETMONETARY.ORG_ID), monetaryColumn, contactTable.getColumn(MCOCONTACT.LAST_NAME),
                ownerIdColumn, dealContactIdColumn, contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE),
                dealCompanyIdColumn, dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), companyTable.getColumn(MCOCOMPANY.NAME),
                emailTable.getColumn(OMETEMAIL.VALUE), emailTable.getColumn(OMETEMAIL.TYPE), phoneTable.getColumn(OMETPHONE.NUMBER),
                phoneTable.getColumn(OMETPHONE.COUNTRY_ID), phoneTable.getColumn(OMETPHONE.TYPE), companyTable.getColumn(MCOCOMPANY.OWNER_ID),
                companyAliasTable.getPKColumn(), contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactCompanyIdColumn,
                companyAliasTable.getColumn(MCOCOMPANY.NAME), contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID),
                contactPhoneTable.getColumn(MCONPHONEFIELDS.PHONE_ID), contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID),
                contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactTable.getColumn(MCOCONTACT.ORG_ID),
                contactTable.getColumn(MCOCONTACT.FACEBOOK), contactTable.getColumn(MCOCONTACT.LINKEDIN),
                contactTable.getColumn(MCOCONTACT.TWITTER), companyTable.getColumn(MCOCOMPANY.WEBSITE),
                companyTable.getColumn(MCOCOMPANY.ORG_ID), dealStateColumn, contactTable.getColumn(MCOCONTACT.OWNER_ID),
                productDetailsHolderPKColumn, productDetailsHolderDealIdColumn, prductDetailsHolderIdColumn, territoryIdColumn,
                contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID), contactPhoneTable.getColumn(MCONPHONEFIELDS.FIELD_ID),
                contactTable.getColumn(MCOCONTACT.TERRITORY_ID), companyTable.getColumn(MCOCOMPANY.TERRITORY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final Row dealRow = dataContainer.getRow(dealTable);
            final Row pipelineRow = dataContainer.getRow(pipelineTable, new WhereClause(Criteria.eq(pipelinePKColumn,
                    dealRow.get(MDODEAL.PIPELINE_ID))));
            final Row pipelineStageRow = dataContainer.getRow(pipelineStageTable,
                    new WhereClause(Criteria.eq(pipelineStagePKColumn, dealRow.get(MDODEAL.PIPELINE_STAGE_ID))));
            final Row contactRow = dataContainer.getRow(contactTable, new WhereClause(Criteria.eq(contactPkColumn,
                    dealRow.get(MDODEAL.CONTACT_ID))));
            final Row monetaryRow = dataContainer.getRow(monetaryTable, new WhereClause(Criteria.eq(monetaryPKColumn,
                    dealRow.get(MDODEAL.MONETARY_ID))));
            final Row dealCompanyRow = dataContainer.getRow(dealCompanyTable,
                    new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealRow.getPKValue())));
            final Row companyRow = Objects.nonNull(dealCompanyRow) ? dataContainer.getRow(companyTable,
                    new WhereClause(Criteria.eq(companyTable.getPKColumn(), dealCompanyRow.get(MDOCOMPANYFIELDS.COMPANY_ID)))) : null;
            final Long territoryId = dealRow.get(territoryIdColumn);
            final Long salesOwnerId = dealRow.get(ownerIdColumn);
            final Integer stateId = dealRow.get(dealStateColumn);

            if (Objects.nonNull(monetaryRow)) {
                final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);
                deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));
            }

            if (Objects.nonNull(companyRow)) {
                deal.setCompany(companyManager.convertRowToPartialModel(companyRow));
            }

            if (Objects.nonNull(contactRow)) {
                deal.setPrimaryContact(ContactManager.getInstance().convertRowToModel(orgId, contactRow, true, dataContainer));
            }

            if (Objects.nonNull(stateId)) {
                deal.setState(State.getStateById(stateId));
            }
            deal.setId(dealRow.get(MDODEAL.ID));
            deal.setOrgId(dealRow.get(MDODEAL.ORG_ID));
            deal.setPipeline(pipelineManager.convertRowToModel(pipelineRow, dataContainer, true));
            deal.setPipelineStage(pipelineStageManager.convertPartialRowToModel(pipelineStageRow));
            deal.setTitle(dealRow.get(MDODEAL.TITLE));
            deal.setSalesOwner(userService.getUser(orgId, salesOwnerId));
            deal.setUserIds(List.of(salesOwnerId));
            deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
            setTotalProduct(deal, dataContainer);
            deal.setOwnerId(salesOwnerId);

            if (Objects.nonNull(territoryId)) {
                final Territory territory = new Territory();

                territory.setId(territoryId);
                deal.setTerritory(territory);
            }
        }

        return deal;
    }

    /**
     * Gets the total number of deal count that matches a specified product criterion.
     *
     * @param orgId     Represents the organisation ID
     * @param productId Represents the product ID
     * @param stateIds  Represents the {@link Collection} of states
     * @return the total number of deal count
     */
    public long getDealsCountByProduct(final Long orgId, final Long productId, final Collection<Long> stateIds) {
        final Table dealTable = getTable();
        final Table productDetailTable = productDetailsHolderManager.getProductDetailTable();
        final Table productDetailHolderTable = productDetailsHolderManager.getTable();
        final CountQuery countQuery = CountQuery.get(dealTable);

        countQuery.addJoinClause(List.of(new JoinClause(dealTable.getPKColumn(), productDetailHolderTable
                .getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID)), new JoinClause(productDetailHolderTable.getPKColumn(),
                productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID))));
        final WhereClause whereClause = new WhereClause(Criteria.eq(productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), productId));

        countQuery.setWhereClause(stateIds.isEmpty() ? whereClause : whereClause.and(Criteria.in(dealTable.getColumn(MDODEAL.STATE), stateIds)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Gets the collection of deal by product
     *
     * @param orgId        Represents the organization id
     * @param productId    Represents the product id
     * @param start        Represents the lower bound of deal to be fetched
     * @param limit        Represents the upper bound of deal to be fetched
     * @param stateIds     Which contains a collection of state ids
     * @param fieldSortMap Which contains key as field name value as {@link SortType}
     * @return the {@link Collection} of deals by product
     */
    public Collection<Deal> getDealsByProduct(final Long orgId, final Long productId, final int start, final int limit,
                                              final Collection<Long> stateIds, final Map<String, SortType> fieldSortMap) {
        final Table dealTable = getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table contactTable = ContactManager.getInstance().getTable();
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailsTable = productDetailsHolderManager.getProductDetailTable();
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column dealMonetaryColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column productHolderDealIdColumn = productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column productIdColumn = productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID);
        final Column productDetailsHolderIdColumn = productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID);
        final Column productQuantityColumn = productDetailsTable.getColumn(MDOPRODUCTDETAIL.QUANTITY);
        final Column contactFirstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
        final Column monetaryValueColumn = monetaryTable.getColumn(OMETMONETARY.VALUE);
        final Column dealTitleColumn = dealTable.getColumn(MDODEAL.TITLE);
        final Column dealExpectedCloseDateColumn = dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE);
        final List<JoinClause> joinClauses = List.of(new JoinClause(dealMonetaryColumn, monetaryTable.getPKColumn()),
                new JoinClause(dealPkColumn, productHolderDealIdColumn),
                new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsHolderIdColumn),
                new JoinClause(dealTable.getColumn(MDODEAL.CONTACT_ID), contactPkColumn));
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final BiConsumer<Column, SortType> orderByAppender = (column, sortType) -> {
            final OrderByClause.OrderType orderType = SortType.ASCENDING == sortType
                    ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING;

            selectQuery.addOrderByClause(new OrderByClause(column, orderType));
        };

        selectQuery.addJoinClause(joinClauses);
        selectQuery.addSelectClause(List.of(dealPkColumn, dealTable.getColumn(MDODEAL.ORG_ID), dealMonetaryColumn,
                dealTitleColumn, dealExpectedCloseDateColumn,
                dealTable.getColumn(MDODEAL.CONTACT_ID), monetaryValueColumn, productHolderDealIdColumn,
                productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), contactPkColumn,
                contactTable.getColumn(MCOCONTACT.LAST_NAME), contactFirstNameColumn,
                dealTable.getColumn(MDODEAL.STATE), productIdColumn,
                productQuantityColumn, productDetailsHolderIdColumn));
        selectQuery.addSelectClause(monetaryTable);

        for (final Map.Entry<String, SortType> fieldSortEntry : fieldSortMap.entrySet()) {

            switch (fieldSortEntry.getKey()) {
                case "productQuantity" -> orderByAppender.accept(productQuantityColumn, fieldSortEntry.getValue());
                case "contact" -> orderByAppender.accept(contactFirstNameColumn, fieldSortEntry.getValue());
                case "monetary" -> orderByAppender.accept(monetaryValueColumn, fieldSortEntry.getValue());
                case "title" -> orderByAppender.accept(dealTitleColumn, fieldSortEntry.getValue());
                case "expectedCloseDate" ->
                        orderByAppender.accept(dealExpectedCloseDateColumn, fieldSortEntry.getValue());
            }
        }
        final WhereClause whereClause = new WhereClause(Criteria.eq(productIdColumn, productId));

        selectQuery.setWhereClause(stateIds.isEmpty() ? whereClause : whereClause.and(Criteria.in(dealTable.getColumn(MDODEAL.STATE), stateIds)));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final DataContainer entityContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> dealRows = entityContainer.getRows(dealTable);

        return Objects.isNull(dealRows) ? Collections.emptyList() : dealRows.map(dealRow -> convertProductDealRowToModel(dealRow, entityContainer)).toList();
    }

    /**
     * Gets the total number of products associated with a given deal.
     *
     * @param orgId  Represents the organisation ID
     * @param dealId Represents the deal ID
     * @return the total number of products linked to the deal
     */
    public long getProductCount(final Long orgId, final Long dealId) {
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailTable = productDetailsHolderManager.getProductDetailTable();
        final CountQuery countQuery = CountQuery.get(productDetailTable);

        countQuery.addJoinClause(new JoinClause(productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), productDetailsHolderTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), dealId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Loads the deal related timeline
     * </p>
     *
     * @param deal            An entity that holds the deal attributes
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param oldValues       Map which holds the existing values of the deal
     * @param dealRow         Row which holds the value of the deal table
     * @param targetContainer Holds the updated information of user defined and system field values
     * @param sourceContainer Holds the user defined and system field values of existing deal
     */
    private void loadDealTimeline(final Deal deal, final boolean isCreateEvent, final Map<String, String> oldValues,
                                  final Row dealRow, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Long orgId = deal.orgId();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);

        if (isCreateEvent) {
            final Option source = deal.getSource();
            final I18nKey titleKey = fields.get(DealFields.TITLE).i18nName();
            final Company dealCompany = deal.getCompany();

            if (Objects.nonNull(source)) {
                final String dealSourceKey = DealFields.DEAL_SOURCE;
                final FieldInfo dealSourceFieldInfo = fields.get(dealSourceKey);
                final Option webForm = optionService.getOption(orgId, dealSourceFieldInfo,
                        DealSourceStaticOption.WEB_FORM.getName());
                final Long sourceId = source.id();
                final Long webFormId = webForm.id();

                if (Objects.equals(sourceId, webFormId)) {
                    loadEventCreationTimeline(dealRow, deal, TimelineType.DEAL_FORM_SUBMISSION, dealSourceFieldInfo.i18nName(), targetContainer);
                }
            }

            loadEventCreationTimeline(dealRow, deal, TimelineType.DEAL_CREATED, titleKey, targetContainer);
            buildDealAssociatedTimeline(dealRow, deal, deal.getTitle(), List.of(Source.CONTACT, Source.COMPANY), titleKey, targetContainer);

            if (Objects.nonNull(dealCompany)) {
                final String companyName = dealCompany.getName();
                final String companyDealField = DealFields.COMPANY;

                buildAndLoadTimeline(isCreateEvent, dealRow, deal, null, Objects.nonNull(companyName)
                        ? companyName : companyManager.getCompanyName(orgId, dealCompany.id()), fields.get(companyDealField).i18nName(), false, companyDealField, targetContainer);
            }
            final ForecastCategory forecastCategory = deal.getForecastCategory();

            if (Objects.nonNull(forecastCategory)) {
                final String forecastCategoryField = DealFields.FORECAST_CATEGORY;
                final String forecastCategoryName = forecastCategoryManager.getName(orgId, forecastCategory.id()).orElseGet(() -> null);

                buildAndLoadTimeline(isCreateEvent, dealRow, deal, null, forecastCategoryName,
                        fields.get(forecastCategoryField).i18nName(), false, forecastCategoryField, targetContainer);
            }
            final Date expectedCloseDate = deal.getExpectedCloseDate();

            if (Objects.nonNull(expectedCloseDate)) {
                final String expectedCloseDateField = DealFields.EXPECTED_CLOSE_DATE;

                buildAndLoadTimeline(isCreateEvent, dealRow, deal, null, expectedCloseDate.getTimeStamp().toString(),
                        fields.get(expectedCloseDateField).i18nName(), false, expectedCloseDateField, targetContainer);
            }
        } else {
            final String titleField = DealFields.TITLE;
            final String companyDealField = DealFields.COMPANY;
            final String pipelineStageDealField = DealFields.PIPELINE_STAGE;
            final String contactDealField = DealFields.PRIMARY_CONTACT;
            final String expectedCloseDateField = DealFields.EXPECTED_CLOSE_DATE;
            final String dealCreatedField = DealFields.DEAL_CREATED;
            final String lostReasonField = DealFields.LOST_REASON;
            final String valueDealField = DealFields.DEAL_VALUE;
            final String oldPipelineStage = oldValues.get(pipelineStageDealField);
            final String oldContactName = oldValues.get(contactDealField);
            final String oldDealValue = oldValues.get(valueDealField);
            final String oldCompanyName = oldValues.get(companyDealField);
            final String oldExpectedCloseDate = oldValues.get(expectedCloseDateField);
            final String oldLostReason = oldValues.get(lostReasonField);
            final Date expectedCloseDate = deal.getExpectedCloseDate();
            final Option newLostReason = deal.getLostReason();
            final String newExpectedCloseDate = Objects.isNull(expectedCloseDate) ? null : expectedCloseDate.getTimeStamp().toString();
            final String oldDealCreatedTime = oldValues.get(dealCreatedField);
            final DateTime createdTime = deal.getCreatedTime();
            final String newCreatedTime = Objects.isNull(createdTime) ? null : createdTime.getTimeStamp().toString();
            final Monetary monetary = deal.getDealValue();
            final String newDealValue = Objects.nonNull(monetary) ? Monetary.getValueWithSymbol(monetary
                    .getCurrency().getSymbol(), monetary.getValue().doubleValue()) : null;
            final String newTitle = deal.getTitle();
            final String oldTitle = oldValues.get(titleField);
            final String salesOwnerKey = ContactFields.SALES_OWNER;
            final String oldSalesOwner = oldValues.get(salesOwnerKey);

            if (Objects.nonNull(oldPipelineStage)) {
                final Long pipelineStageId = deal.getPipelineStage().id();
                final String pipelineStageName = pipelineStageManager.getPipelineStageName(orgId, pipelineStageId);

                if (Objects.equals(DealFields.LOST, pipelineStageName.toLowerCase())) {
                    final String lostReasonKey = DealFields.LOST_REASON;
                    final Long lostReasonFieldId = fields.get(lostReasonKey).id();
                    final Option option = optionService.get(orgId, lostReasonFieldId, newLostReason.id());

                    if (Objects.nonNull(option)) {
                        final String reason = option.getOption();

                        if (!Objects.equals(oldLostReason, reason)) {
                            buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldLostReason, reason,
                                    fields.get(lostReasonField).i18nName(), false, lostReasonField, targetContainer);
                        }
                    }
                }
                if (!Objects.equals(pipelineStageName, oldPipelineStage)) {
                    buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldPipelineStage, pipelineStageName,
                            fields.get(pipelineStageDealField).i18nName(), false, pipelineStageDealField, targetContainer);
                }
            }

            if (Objects.nonNull(newDealValue) && !Objects.equals(oldDealValue, newDealValue)) {
                buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldDealValue, newDealValue,
                        fields.get(valueDealField).i18nName(), false, valueDealField, targetContainer);
            }

            if (Objects.nonNull(newTitle) && !Objects.equals(oldTitle, newTitle)) {
                final Long primaryContactId = deal.getPrimaryContact().id();
                final Company company = deal.getCompany();

                buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldTitle, newTitle,
                        fields.get(titleField).i18nName(), false, titleField, targetContainer);
                updateSourceTimeline(primaryContactId, Objects.isNull(company) ? null : company.id(), targetContainer);
            }

            if (Objects.nonNull(newExpectedCloseDate) && !Objects.equals(oldExpectedCloseDate, newExpectedCloseDate)) {
                buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldExpectedCloseDate, newExpectedCloseDate,
                        fields.get(expectedCloseDateField).i18nName(), false, expectedCloseDateField, targetContainer);
            }

            if (Objects.nonNull(newCreatedTime) && !Objects.equals(oldDealCreatedTime, newCreatedTime)) {
                buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldDealCreatedTime, newCreatedTime,
                        fields.get(dealCreatedField).i18nName(), false, dealCreatedField, targetContainer);
            }

            if (Objects.nonNull(oldSalesOwner)) {
                buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldSalesOwner, String.valueOf(deal.getSalesOwner().id()),
                        fields.get(salesOwnerKey).i18nName(), false, salesOwnerKey, targetContainer);
            }

            if (Objects.isNull(oldPipelineStage) && Objects.nonNull(newLostReason)) {
                final String lostReasonKey = DealFields.LOST_REASON;
                final Long lostReasonFieldId = fields.get(lostReasonKey).id();
                final Option option = optionService.get(orgId, lostReasonFieldId, newLostReason.id());

                if (Objects.nonNull(option)) {
                    final String reason = option.getOption();

                    if (!Objects.equals(oldLostReason, reason)) {
                        buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldLostReason, reason,
                                fields.get(lostReasonField).i18nName(), false, lostReasonField, targetContainer);
                    }
                }
            }

            if (Objects.nonNull(oldContactName)) {
                final Contact contact = deal.getPrimaryContact();

                if (Objects.nonNull(contact)) {
                    final Long newContactId = contact.id();
                    final String fullName = contact.getFullName();
                    final String newContactFullName = Objects.nonNull(fullName) ? fullName
                            : ContactManager.getInstance().getContactFullName(orgId, newContactId);

                    buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldContactName, newContactFullName, fields.get(contactDealField).i18nName(),
                            true, contactDealField, targetContainer);
                    buildDealAssociatedTimeline(dealRow, deal, newTitle, List.of(Source.CONTACT), fields.get(titleField).i18nName(), targetContainer);
                }
            }
            final Company company = deal.getCompany();

            if (Objects.nonNull(oldCompanyName)) {
                final I18nKey companyFieldI18nKey = fields.get(companyDealField).i18nName();

                if (Objects.nonNull(company)) {
                    final String companyName = company.getName();

                    buildDealAssociatedTimeline(dealRow, deal, newTitle, List.of(Source.COMPANY), fields.get(titleField).i18nName(), targetContainer);
                    buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldCompanyName, Objects.nonNull(companyName)
                            ? companyName : companyManager.getCompanyName(orgId, company.id()), companyFieldI18nKey, false, companyDealField, targetContainer);
                } else {
                    buildAndLoadTimeline(isCreateEvent, dealRow, deal, null, oldCompanyName, companyFieldI18nKey, false, companyDealField, targetContainer);
                }
            } else if (Objects.nonNull(company)) {
                final FieldInfo companyField = fields.get(companyDealField);
                final Table dealCompanyTable = getDealCompanyTable();
                final Row companyDealRow = sourceContainer.getRow(dealCompanyTable, new WhereClause(Criteria
                        .eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), deal.id()))
                        .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyField.id())));

                if (Objects.isNull(companyDealRow)) {
                    final String newCompanyName = company.getName();

                    buildDealAssociatedTimeline(dealRow, deal, newTitle, List.of(Source.COMPANY), fields.get(titleField).i18nName(), targetContainer);
                    buildAndLoadTimeline(true, dealRow, deal, null, Objects.nonNull(newCompanyName)
                            ? newCompanyName : companyManager.getCompanyName(orgId, company.id()), companyField.i18nName(), false, companyDealField, targetContainer);
                }
            }
            final String pipelineKey = DealFields.PIPELINE;
            final String oldPipelineName = oldValues.get(pipelineKey);

            if (Objects.nonNull(oldPipelineName)) {
                final String pipelineStageName = pipelineStageManager.getPipelineStageName(orgId, dealRow.get(MDODEAL.PIPELINE_STAGE_ID));
                final JsonObject newValueJson = Json.object();
                final JsonObject pipelineJson = Json.object();
                final JsonObject pipelineStageJson = Json.object();

                pipelineJson.put(TimelineFields.NEW_VALUE.getName(), pipelineManager.partialGet(orgId,
                        (Long) dealRow.get(MDODEAL.PIPELINE_ID)).getName());
                pipelineJson.put(TimelineFields.OLD_VALUE.getName(), oldPipelineName);

                pipelineStageJson.put(TimelineFields.NEW_VALUE.getName(), pipelineStageName);
                pipelineStageJson.put(TimelineFields.OLD_VALUE.getName(), oldPipelineStage);

                newValueJson.put(pipelineKey, pipelineJson);
                newValueJson.put(pipelineStageDealField, pipelineStageJson);
                buildAndLoadTimeline(isCreateEvent, dealRow, deal, null, newValueJson.toString(), fields.get(pipelineKey).i18nName(), false,
                        pipelineKey, targetContainer);
            }
            final String forecastCategoryField = DealFields.FORECAST_CATEGORY;

            if (oldValues.containsKey(forecastCategoryField)) {
                final String oldForecastCategoryName = oldValues.get(forecastCategoryField);
                final ForecastCategory forecastCategory = deal.getForecastCategory();
                final String newForecastCategoryName = Objects.isNull(forecastCategory) ? null
                        : Objects.requireNonNullElseGet(forecastCategory.getName(),
                        () -> forecastCategoryManager.getName(orgId, forecastCategory.id()).orElseGet(() -> null));

                buildAndLoadTimeline(isCreateEvent, dealRow, deal, oldForecastCategoryName, newForecastCategoryName,
                        fields.get(forecastCategoryField).i18nName(), false, forecastCategoryField, targetContainer);
            }
        }
        loadRelatedContactsIntoTimeline(orgId, deal, dealRow, fields.get(DealFields.RELATED_CONTACTS).i18nName(), isCreateEvent, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the addition and removal of related contacts into the timeline
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param deal            Represents the {@link Deal}
     * @param dealRow         Represents the {@link Row} of deal
     * @param i18nKey         Represents the {@link I18nKey}
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param targetContainer Holds the updated information of user defined and system field values
     * @param sourceContainer Holds the user defined and system field values of existing deal
     */
    private void loadRelatedContactsIntoTimeline(final long orgId, final Deal deal, final Row dealRow,
                                                 final I18nKey i18nKey, final boolean isCreateEvent,
                                                 final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Set<Long> participantIds = deal.getParticipants().stream()
                .map(participant -> participant.getContact().id()).collect(Collectors.toSet());
        final Long primaryContactId;

        if (isCreateEvent) {
            final Contact primaryContact = deal.getPrimaryContact();

            primaryContactId = Objects.isNull(primaryContact) ? null : primaryContact.id();
        } else {
            final Row existingDealRow = sourceContainer.getRow(getTable(), deal.id());

            primaryContactId = Objects.isNull(existingDealRow) ? null : (Long) existingDealRow.get(MDODEAL.CONTACT_ID);
        }
        final Set<Long> contactIdsToAdd;
        final boolean isPrimaryContactNonNull = Objects.nonNull(primaryContactId);

        Set<Long> contactIdsToRemove = Collections.emptySet();

        if (isCreateEvent) {

            if (isPrimaryContactNonNull) {
                participantIds.remove(primaryContactId);
            }
            contactIdsToAdd = participantIds;
        } else {
            final Table participantTable = Table.get(MDOPARTICIPANT.TABLE);
            final Set<Long> existingParticipantIds = sourceContainer.getRows(participantTable,
                            new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), deal.id())))
                    .map(row -> (Long) row.get(MDOPARTICIPANT.CONTACT_ID)).collect(Collectors.toSet());

            if (isPrimaryContactNonNull) {
                existingParticipantIds.remove(primaryContactId);
            }
            contactIdsToAdd = participantIds.stream().filter(id -> !existingParticipantIds.contains(id))
                    .collect(Collectors.toSet());
            contactIdsToRemove = existingParticipantIds.stream().filter(id -> !participantIds.contains(id))
                    .collect(Collectors.toSet());
        }
        final Set<Long> contactIds = new HashSet<>(contactIdsToAdd);

        contactIds.addAll(contactIdsToRemove);

        if (!contactIds.isEmpty()) {
            final Map<Long, Contact> contacts = contactManager.getContactsForDealTimeline(orgId, contactIds);

            if (!contactIdsToAdd.isEmpty()) {
                loadRelatedContactsIntoTimeline(participantIds, TimelineType.DEAL_RELATED_CONTACTS_ADDED,
                        TimelineActionFields.RELATED_CONTACTS_ADDED, contacts, deal, i18nKey, dealRow, targetContainer);
            }

            if (!contactIdsToRemove.isEmpty()) {
                loadRelatedContactsIntoTimeline(contactIdsToRemove, TimelineType.DEAL_RELATED_CONTACTS_REMOVED,
                        TimelineActionFields.RELATED_CONTACTS_REMOVED, contacts, deal, i18nKey, dealRow, targetContainer);
            }
        }
    }

    /**
     * Loads the addition and removal of related contact data  into the timeline.
     *
     * @param contactIds      Collection of contact IDs to be included in the respective action timeline.
     * @param timelineType    Type of timeline event.
     * @param action          Action associated with the timeline update.
     * @param contacts        Which containing contact details mapped by their IDs.
     * @param deal            The {@link Deal} associated with the timeline event.
     * @param i18nKey         Represents the {@link I18nKey}
     * @param dealRow         The {@link Row} representing the deal data.
     * @param targetContainer Container holding updated user-defined and system field values.
     */
    private void loadRelatedContactsIntoTimeline(final Collection<Long> contactIds, final TimelineType timelineType, final String action,
                                                 final Map<Long, Contact> contacts, final Deal deal, final I18nKey i18nKey, final Row dealRow,
                                                 final DataContainer targetContainer) {
        final Timeline timeline = buildTimeline(false, dealRow, deal);
        final String newValue = Json.object().put(action, Json.array(contactIds.stream().map(contacts::get).filter(Objects::nonNull).map(Contact::getFullName).toList())).toString();

        timeline.setTimelineType(timelineType);
        timeline.setAction(action);
        timeline.setFieldTimeline(new FieldTimeline(newValue, i18nKey));

        loadTimelineRowIntoContainer(timeline, deal, null, targetContainer);
    }

    /**
     * <p>
     * Builds and loads the deal associated timeline into target {@link DataContainer}.
     * </p>
     *
     * @param dealRow         Represents the deal row
     * @param deal            Represents the {@link Deal}
     * @param newTitle        Represents the deal title
     * @param sources         Represents the {@link Collection} of {@link Source}
     * @param i18nName        Represents the {@link I18nKey} field name
     * @param targetContainer Holds the updated timeline records into the target {@link DataContainer}
     */
    private void buildDealAssociatedTimeline(final Row dealRow, final Deal deal, final String newTitle, final Collection<Source> sources,
                                             final I18nKey i18nName, final DataContainer targetContainer) {

        for (final Source source : sources) {
            final TimelineType timelineType = switch (source) {
                case CONTACT -> TimelineType.DEAL_ASSOCIATED_WITH_CONTACT;
                case COMPANY -> TimelineType.DEAL_ASSOCIATED_WITH_COMPANY;
                default -> null;
            };

            if (Objects.nonNull(timelineType)) {
                final Timeline timeline = buildTimeline(false, dealRow, deal);

                timeline.setTimelineType(timelineType);
                timeline.setAction(TimelineActionFields.ASSOCIATED);
                timeline.setFieldTimeline(new FieldTimeline(Collections.emptyList(), newTitle, i18nName));

                loadTimelineRowIntoContainer(timeline, deal, source, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Loads the timeline with relevant information following the creation of an event.
     * </p>
     *
     * @param dealRow         Represents the deal {@link Row}
     * @param deal            Represents the {@link Deal}
     * @param type            Represents the {@link TimelineType}
     * @param key             Represents the {@link I18nKey}
     * @param targetContainer Holds the updated information of timeline into the container
     */
    private void loadEventCreationTimeline(final Row dealRow, final Deal deal, final TimelineType type, final I18nKey key, final DataContainer targetContainer) {
        final Timeline timeline = buildTimeline(true, dealRow, deal);

        switch (type) {
            case DEAL_FORM_SUBMISSION -> {
                timeline.setAction(TimelineActionFields.SUBMITTED);
                timeline.setTimelineType(TimelineType.DEAL_FORM_SUBMISSION);
            }
            case DEAL_CREATED -> {
                final FieldTimeline fieldTimeline = new FieldTimeline(Collections.emptyList(), deal.getTitle(), key);

                timeline.setFieldTimeline(fieldTimeline);
            }
        }

        loadTimelineRowIntoContainer(timeline, deal, null, targetContainer);
    }

    /**
     * <p>
     * Updates and removes the unmapped company details from the timeline.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param newCompanyId    Represents the new company id
     * @param targetContainer Holds the updated information of timeline values stored into the target container
     * @param sourceContainer Holds the existing timeline related information
     */
    private void updateAndDeleteCompanyTimeline(final long orgId, final Long newCompanyId,
                                                final DataContainer targetContainer, final DataContainer sourceContainer) {
        final Table timelineTable = commonTimelineManager.getTable();
        final Column timelinePKColumn = timelineTable.getPKColumn();
        final Collection<Long> timelineIds = new HashSet<>();

        sourceContainer.getRows(timelineTable).forEach(timelineRow -> {
            final Long timelineId = timelineRow.get(timelinePKColumn);

            loadCompanyTimelineIntoContainer(timelineId, newCompanyId, sourceContainer, targetContainer);

            timelineIds.add(timelineId);
        });

        deleteUnmappedCompanyTimeline(timelineIds, orgId, newCompanyId, targetContainer);
    }

    /**
     * <p>
     * Builds the timeline based on type and loads into container
     * </p>
     *
     * @param dealRow         Row which holds the value of the deal table
     * @param deal            An entity that holds the deal attributes
     * @param oldValue        Holds the old Value of the deal fields
     * @param newValue        Holds the new Value of the deal fields
     * @param fieldName       Holds the i18n name of the deal fields
     * @param dealField       Represents the updated field of the deal
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void buildAndLoadTimeline(final boolean isCreateEvent, final Row dealRow, final Deal deal, final String oldValue,
                                      final String newValue, final I18nKey fieldName, final boolean isContact,
                                      final String dealField, final DataContainer targetContainer) {
        final Timeline timeline = buildTimeline(false, dealRow, deal);

        switch (dealField) {
            case "dealValue" -> {

                if (isDefaultValue(deal.orgId(), oldValue)) {
                    timeline.setTimelineType(TimelineType.DEAL_VALUE_ASSIGNED);
                    timeline.setAction(TimelineActionFields.ASSIGNED);
                } else {
                    timeline.setTimelineType(TimelineType.DEAL_VALUE_UPDATED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                }
            }
            case "expectedCloseDate" -> {
                if (Objects.isNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.DEAL_EXPECTED_CLOSE_DATE_ASSIGNED);
                    timeline.setAction(TimelineActionFields.ASSIGNED);
                } else {
                    timeline.setTimelineType(TimelineType.DEAL_EXPECTED_CLOSE_DATE_CHANGED);
                    timeline.setAction(TimelineActionFields.CHANGED);
                }
            }
            case "dealCreated" -> {
                if (Objects.nonNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.DEAL_CREATED_TIME_CHANGED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                }
            }
            case "pipelineStage" -> {
                timeline.setTimelineType(TimelineType.DEAL_PIPELINE_STAGE_MOVED);
                timeline.setAction(TimelineActionFields.MOVED);
            }
            case "pipeline" -> {
                timeline.setTimelineType(TimelineType.DEAL_PIPELINE_MOVED);
                timeline.setAction(TimelineActionFields.MOVED);
            }
            case "primaryContact" -> timeline.setTimelineType(TimelineType.NOW_POINT_OF_CONTACT_FOR_THIS_DEAL);
            case "lostReason" -> {
                if (Objects.isNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.DEAL_LOST_REASON_SET);
                    timeline.setAction(TimelineActionFields.CLOSED);
                } else {
                    timeline.setTimelineType(TimelineType.DEAL_LOST_REASON_UPDATED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                }
            }
            case "title" -> {
                if (Objects.nonNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.DEAL_TITLE_UPDATED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                }
            }
            case "salesOwner" -> {
                timeline.setTimelineType(TimelineType.DEAL_SALES_OWNER_REASSIGNED);
                timeline.setAction(TimelineActionFields.REASSIGNED);
            }
            case "company" -> {
                if (isCreateEvent) {
                    timeline.setTimelineType(TimelineType.DEAL_COMPANY_ADDED);
                } else if (Objects.nonNull(oldValue) && Objects.nonNull(newValue)) {
                    timeline.setTimelineType(TimelineType.DEAL_COMPANY_UPDATED);
                } else if (Objects.isNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.DEAL_COMPANY_REMOVED);
                    timeline.setAction(TimelineActionFields.COMPANY_REMOVED);
                }
            }
            case "forecastCategory" -> {
                if (Objects.isNull(oldValue) && Objects.nonNull(newValue)) {
                    timeline.setTimelineType(TimelineType.FORECAST_CATEGORY_ADDED);
                    timeline.setAction(TimelineActionFields.ASSIGNED);
                } else if (Objects.nonNull(oldValue) && Objects.nonNull(newValue)) {
                    timeline.setTimelineType(TimelineType.FORECAST_CATEGORY_UPDATED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                } else if (Objects.nonNull(oldValue)) {
                    timeline.setTimelineType(TimelineType.FORECAST_CATEGORY_REMOVED);
                    timeline.setAction(TimelineActionFields.UPDATED);
                }
            }
        }

        if (!Objects.equals(oldValue, newValue) || isContact) {
            final Collection<String> oldValues = Optional.ofNullable(oldValue).map(Collections::singletonList).orElseGet(Collections::emptyList);
            final FieldTimeline fieldTimeline = new FieldTimeline(oldValues, newValue, fieldName);

            timeline.setFieldTimeline(fieldTimeline);
        }

        loadTimelineRowIntoContainer(timeline, deal, null, targetContainer);
    }

    /**
     * <p>
     * Builds the timeline when any action performed in deal
     * </p>
     *
     * @param isCreateEvent A flag which indicates whether the action is related to create or update event
     * @param deal          An entity that holds the deal attributes
     * @param dealRow       Holds the value of the deal table
     * @return {@link Timeline}
     */
    private Timeline buildTimeline(final boolean isCreateEvent, final Row dealRow, final Deal deal) {
        final Timeline timeline = new Timeline();

        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
        timeline.setOrgId(deal.orgId());
        timeline.setCategory(Timeline.Category.DEAL);

        if (isCreateEvent) {
            timeline.setAction(TimelineActionFields.CREATED);
            timeline.setTimelineType(TimelineType.DEAL_CREATED);
            timeline.setOwnerId(deal.getCreatedBy().id());
        } else {
            timeline.setAction(TimelineActionFields.UPDATED);
            timeline.setOwnerId(deal.getUpdatedBy().id());
        }

        return timeline;
    }

    /**
     * <p>
     * Loads the deal timeline row into the container.
     * </p>
     *
     * @param timeline        An entity that holds the timeline attributes
     * @param deal            Represents the {@link Deal}
     * @param source          Represents the {@link Source}
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadTimelineRowIntoContainer(final Timeline timeline, final Deal deal,
                                              final Source source, final DataContainer targetContainer) {
        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), targetContainer);
        final Long timelineId = timeline.id();
        final Collection<TimelineType> validTypes = Arrays.asList(TimelineType.DEAL_TITLE_UPDATED, TimelineType.DEAL_ASSOCIATED_WITH_CONTACT, TimelineType.DEAL_ASSOCIATED_WITH_COMPANY);

        if (validTypes.contains(timeline.getTimelineType())) {
            final Table contactTimelineTable = commonTimelineManager.getContactTimelineTable();
            final Table companyTimelineTable = commonTimelineManager.getCompanyTimelineTable();
            final Contact contact = deal.getPrimaryContact();
            final Company company = deal.getCompany();

            if (Objects.nonNull(source)) {

                switch (source) {
                    case CONTACT -> {

                        if (Objects.nonNull(contact)) {
                            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, contact.id(),
                                    MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                                    new Row(contactTimelineTable), targetContainer);
                        }
                    }
                    case COMPANY -> {

                        if (Objects.nonNull(company)) {
                            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, company.id(),
                                    MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                                    new Row(companyTimelineTable), targetContainer);
                        }
                    }
                }
            }
        }
        commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, deal.id(), MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID,
                new Row(commonTimelineManager.getDealTimelineTable()), targetContainer);
    }

    /**
     * <p>
     * Updates the mapped contact and company with respective timeline.
     * </p>
     *
     * @param contactId           Represents contact id
     * @param companyId           Represents the company id
     * @param targetDataContainer Holds the updated information of user defined and system field values
     */
    private void updateSourceTimeline(final Long contactId, final Long companyId, final DataContainer targetDataContainer) {
        final Table timelineTable = commonTimelineManager.getTable();
        final Collection<Long> timelineIds = targetDataContainer.getRows(timelineTable, new WhereClause(Criteria
                        .eq(timelineTable.getColumn(OMTLTIMELINE.TYPE), TimelineType.DEAL_TITLE_UPDATED.getId())))
                .map(timelineRow -> (Long) timelineRow.get(timelineTable.getPKColumn())).collect(Collectors.toSet());
        final Table contactTimelineTable = commonTimelineManager.getContactTimelineTable();
        final Table companyTimelineTable = commonTimelineManager.getCompanyTimelineTable();
        final boolean isCompanyNonNull = Objects.nonNull(companyId);

        timelineIds.forEach(timelineId -> {

            commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, contactId, MTLCONTACTTIMELINE.CONTACT_ID, MTLCONTACTTIMELINE.TIMELINE_ID,
                    new Row(contactTimelineTable), targetDataContainer);

            if (isCompanyNonNull) {
                commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, companyId, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                        new Row(companyTimelineTable), targetDataContainer);
            }
        });
    }

    /**
     * Loads the company timeline row into container
     *
     * @param companyId           Represents company id
     * @param timelineId          Represents timeline id
     * @param sourceDataContainer Holds the existing records of the contact timeline table
     * @param targetContainer     Holds the updated information of field values provided by user
     */
    private void loadCompanyTimelineIntoContainer(final Long timelineId, final Long companyId,
                                                  final DataContainer sourceDataContainer, final DataContainer targetContainer) {
        final Table contactTimelineTable = commonTimelineManager.getCompanyTimelineTable();
        final Row contactTimelineRow = Objects.isNull(sourceDataContainer) ? null :
                Optional.ofNullable(sourceDataContainer.getRow(contactTimelineTable, new WhereClause(Criteria.eq(contactTimelineTable.getColumn(MTLCOMPANYTIMELINE.TIMELINE_ID), timelineId))
                        .and(Criteria.eq(contactTimelineTable.getColumn(MTLCOMPANYTIMELINE.COMPANY_ID), companyId)))).map(Row::new).orElseGet(() -> new Row(contactTimelineTable));

        commonTimelineManager.loadSourceTimelineRowIntoContainer(timelineId, companyId, MTLCOMPANYTIMELINE.COMPANY_ID, MTLCOMPANYTIMELINE.TIMELINE_ID,
                contactTimelineRow, targetContainer);
    }

    /**
     * <p>
     * Deletes the company timeline that are not associated with the deal
     * </p>
     *
     * @param timelineIds         Represents {@link Collection} timeline id
     * @param orgId               Represents organisation id
     * @param companyId           Represents company id
     * @param targetDataContainer Holds the new/updated/removed field values
     */
    private void deleteUnmappedCompanyTimeline(final Collection<Long> timelineIds, final Long orgId, final Long companyId,
                                               final DataContainer targetDataContainer) {
        final Table companyTimelineTable = commonTimelineManager.getCompanyTimelineTable();
        final Column companyTimelineIdColumn = companyTimelineTable.getColumn(MTLCOMPANYTIMELINE.TIMELINE_ID);
        final WhereClause companyTimelineWhereClause = new WhereClause(Criteria.in(companyTimelineIdColumn, timelineIds));
        final WhereClause whereClause = Objects.nonNull(companyId) ? companyTimelineWhereClause
                .and(Criteria.notEq(companyTimelineTable.getColumn(MTLCOMPANYTIMELINE.COMPANY_ID), companyId)) : companyTimelineWhereClause;
        final Stream<Row> companyTimelineRows = orgDataStore(orgId).get(companyTimelineTable, whereClause).getRows(companyTimelineTable);

        companyTimelineRows.forEach(targetDataContainer::deleteRow);
    }

    /**
     * <p>
     * Updates the given deal state and stage fields
     * </p>
     *
     * @param orgId           Organization id
     * @param deal            An entity that holds the deal attributes
     * @param pipelineStageId Represents the id of the {@link PipelineStage}
     */
    public void updateStateAndStage(final Long orgId, final Long userId, final Deal deal, final Long pipelineStageId) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Long dealId = deal.id();
        final Table dealTable = getTable();
        final Table dealDynamicOptionTable = getDealDynamicOptionTable();
        final JoinClause.JoinType leftJoin = LEFT;
        final String lostReason = DealFields.LOST_REASON;
        final String wonReason = DealFields.WON_REASON;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, lostReason, wonReason);
        final Long lostReasonFieldId = fields.get(lostReason).id();
        final Long wonReasonFieldId = fields.get(wonReason).id();
        final Column fieldIdColumn = dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.FIELD_ID);
        final State state = deal.getState();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery innerQuery = SelectQuery.get(dealTable);
        final DerivedTable derivedDealTable = DerivedTable.get(dealTable.getName(), dealTable.getAlias(), innerQuery);
        final Column dealPkColumn = derivedDealTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);

        innerQuery.addSelectClause(dealTable);
        innerQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        selectQuery.addSelectClause(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME));
        selectQuery.addJoinClause(new JoinClause(pipelineStageTable.getPKColumn(), derivedDealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), leftJoin));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(derivedDealTable.getPKColumn(), dealId))
                .or(Criteria.eq(pipelineStageTable.getPKColumn(), pipelineStageId)));
        selectQuery.addJoinClause(new JoinClause(dealPkColumn, dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), leftJoin));
        selectQuery.addSelectClause(dealDynamicOptionTable, derivedDealTable);
        buildDealProgressSelectQuery(derivedDealTable, selectQuery, leftJoin);
        final DataContainer entityContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Long> ids = entityContainer.getRows(derivedDealTable).map(Row::getPKValue).toList();

        entityContainer.append(fetchDealDynamicOptionDataContainer(orgId, ids));

        try {
            final DataContainer targetContainer = DataContainer.create();
            final Row dealRow = entityContainer.getRow(derivedDealTable, new WhereClause(Criteria.eq(dealPkColumn, dealId)));
            final WhereClause whereClause = new WhereClause(Criteria.eq(dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), dealId));
            final Row lostReasonRow = entityContainer.getRow(dealDynamicOptionTable, whereClause
                    .and(Criteria.eq(fieldIdColumn, lostReasonFieldId)));
            final Row wonReasonRow = entityContainer.getRow(dealDynamicOptionTable, whereClause
                    .and(Criteria.eq(fieldIdColumn, wonReasonFieldId)));
            final Long oldPipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);
            final String oldPipelineStageValue = entityContainer.getRow(pipelineStageTable, oldPipelineStageId).get(MDOPIPELINESTAGE.NAME);
            final String newPipelineStageValue = entityContainer.getRow(pipelineStageTable, pipelineStageId).get(MDOPIPELINESTAGE.NAME);
            final Map<String, String> oldDealValues = new HashMap<>();

            loadOldDealValues(deal, oldDealValues, dealRow, entityContainer); //oldDealValues should be updated before updating dealRow
            final Long dealClosedOn;

            if (State.WON == state) {

                if (Objects.nonNull(lostReasonRow)) {
                    targetContainer.deleteRow(lostReasonRow);
                }
                loadStateOptionIntoContainer(wonReasonRow, deal, wonReasonFieldId, deal.getWonReason().id(), targetContainer);
                dealClosedOn = dateTimeUtil.getTimeStamp(deal.getClosedTime());
            } else if (State.LOST == state) {

                if (Objects.nonNull(wonReasonRow)) {
                    targetContainer.deleteRow(wonReasonRow);
                }
                loadStateOptionIntoContainer(lostReasonRow, deal, lostReasonFieldId, deal.getLostReason().id(), targetContainer);
                dealClosedOn = dateTimeUtil.getTimeStamp(deal.getClosedTime());
            } else {

                if (Objects.nonNull(lostReasonRow)) {
                    targetContainer.deleteRow(lostReasonRow);
                }

                if (Objects.nonNull(wonReasonRow)) {
                    targetContainer.deleteRow(wonReasonRow);
                }
                dealClosedOn = null;
            }
            deal.setSalesOwner(userService.getUser(orgId, dealRow.get(MDODEAL.OWNER_ID)));
            final Long currentTime = Instant.now().toEpochMilli();

            dealRow.set(MDODEAL.MODIFIED_TIME, currentTime);
            loadDealStageProgressIntoContainer(false, deal, derivedDealTable, entityContainer, targetContainer);
            loadDealTimeline(deal, false, oldDealValues, dealRow, entityContainer, targetContainer);

            if (!Objects.equals(oldPipelineStageId, pipelineStageId)) {
                dealRow.set(MDODEAL.STAGE_MODIFIED_TIME, currentTime);
            }
            dealRow.set(MDODEAL.CLOSED_TIME, dealClosedOn);
            dealRow.set(MDODEAL.STATE, state.getId());
            dealRow.set(MDODEAL.PIPELINE_STAGE_ID, pipelineStageId);
            dealRow.set(MDODEAL.UPDATED_BY, userId);

            targetContainer.updateRow(dealRow);
            loadPipelineFieldAuditEntry(deal, dealRow, entityContainer, targetContainer);
            orgDataStore.commitChanges(targetContainer);

            auditLogger.log(orgId, List.of(new AuditLogData(userId, dealId, dealRow.get(MDODEAL.TITLE),
                    AuditSource.DEAL, AuditAction.UPDATED, new AuditAttribute(PIPELINE_STAGE_KEY, oldPipelineStageValue, newPipelineStageValue, SINGLE_STRING))));
        } catch (ErrorCode e) {
            throw ErrorCode.get(DealErrorCodes.UPDATE_ACTION_FAILED, "i18n.deal.action.update.failed");
        }
    }

    /**
     * <p>
     * Loads the field edit history of the deal into container.
     * </p>
     *
     * @param deal            Represents the {@link Deal}
     * @param oldDealRow      Represents the old deal row from the entity container
     * @param entityContainer Holds the fetched deal data including pipeline stage and dropdown option rows
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadPipelineFieldAuditEntry(final Deal deal, final Row oldDealRow,
            final DataContainer entityContainer, final DataContainer targetContainer) {
        final Long orgId = deal.orgId();
        final FieldTrackingConfig fieldTrackingConfig = fieldTrackingConfigManager.getOrDefault(orgId, (short) Source.DEAL.getId());

        if (!fieldTrackingConfig.isTrackingEnabled()) {
            return;
        }
        final Long dealId = deal.id();
        final Map<String, String> oldDealValues = new HashMap<>();
        final Map<String, String> newDealValues = new HashMap<>();
        final Pipeline pipeline = deal.getPipeline();
        final PipelineStage pipelineStage = deal.getPipelineStage();
        final Option wonReason = deal.getWonReason();
        final Option lostReason = deal.getLostReason();
        final String pipelineKey = DealFields.PIPELINE;
        final String pipelineStageKey = DealFields.PIPELINE_STAGE;
        final String wonReasonKey = DealFields.WON_REASON;
        final String lostReasonKey = DealFields.LOST_REASON;
        final String dealClosedOnKey = DealFields.DEAL_CLOSED_ON;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, pipelineKey, pipelineStageKey, wonReasonKey, lostReasonKey, dealClosedOnKey);
        final Long wonReasonFieldId = fields.get(wonReasonKey).id();
        final Long lostReasonFieldId = fields.get(lostReasonKey).id();
        final State state = deal.getState();
        final Set<String> trackedFields = fieldTrackingConfig.trackedFields();
        final Table dealDynamicOptionTable = getDealDynamicOptionTable();

        if (trackedFields.contains(pipelineKey)) {
            final Long oldPipelineId = oldDealRow.get(MDODEAL.PIPELINE_ID);

            if (Objects.nonNull(oldPipelineId)) {
                final Row pipelineRow = entityContainer.getRow(pipelineManager.getTable(), oldPipelineId);

                putIfNonNull(oldDealValues, pipelineKey, Objects.isNull(pipelineRow) ? null : pipelineRow.get(MDOPIPELINE.NAME));
            }
            putIfNonNull(newDealValues, pipelineKey, Objects.isNull(pipeline) ? null : (Objects.isNull(pipeline.getName()) ? pipelineManager.partialGet(orgId, pipeline.id()).getName() : pipeline.getName()));
        }

        if (trackedFields.contains(pipelineStageKey)) {
            final Long oldPipelineStageId = oldDealRow.get(MDODEAL.PIPELINE_STAGE_ID);

            if (Objects.nonNull(oldPipelineStageId)) {
                final Row pipelineStageRow = entityContainer.getRow(pipelineStageManager.getTable(), oldPipelineStageId);

                putIfNonNull(oldDealValues, pipelineStageKey, Objects.isNull(pipelineStageRow) ? null : pipelineStageRow.get(MDOPIPELINESTAGE.NAME));
            }
            putIfNonNull(newDealValues, pipelineStageKey, Objects.isNull(pipelineStage) ? null : (Objects.isNull(pipelineStage.getName()) ? pipelineStageManager.getPipelineStageName(orgId, pipelineStage.id()) : pipelineStage.getName()));
        }

        if (trackedFields.contains(wonReasonKey) && State.WON == state) {
            final Row dropdownRow = entityContainer.getRow(dealDynamicOptionTable, new WhereClause(Criteria.eq(dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), dealId))
                    .and(Criteria.eq(dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.FIELD_ID), wonReasonFieldId)));

            if (Objects.nonNull(dropdownRow)) {
                putIfNonNull(oldDealValues, wonReasonKey, optionService.get(orgId, wonReasonFieldId, dropdownRow.get(MDODROPDOWNFIELDS.OPTION_ID)).getOption());
            }
            putIfNonNull(newDealValues, wonReasonKey, Objects.isNull(wonReason) ? null : Objects.isNull(wonReason.getOption()) ?
                        optionService.get(orgId, wonReasonFieldId, wonReason.id()).getOption() : wonReason.getOption());
        }

        if (trackedFields.contains(lostReasonKey) && State.LOST == state) {
            final Row dropdownRow = entityContainer.getRow(dealDynamicOptionTable, new WhereClause(Criteria.eq(dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), dealId))
                    .and(Criteria.eq(dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.FIELD_ID), lostReasonFieldId)));

            if (Objects.nonNull(dropdownRow)) {
                putIfNonNull(oldDealValues, lostReasonKey, optionService.get(orgId, lostReasonFieldId, dropdownRow.get(MDODROPDOWNFIELDS.OPTION_ID)).getOption());
            }
            putIfNonNull(newDealValues, lostReasonKey, Objects.isNull(lostReason) ? null : Objects.isNull(lostReason.getOption()) ?
                        optionService.get(orgId, lostReasonFieldId, lostReason.id()).getOption() : lostReason.getOption());
        }

        if (trackedFields.contains(dealClosedOnKey)) {
            final DateTime closedTime = oldDealRow.get(MDODEAL.CLOSED_TIME);

            if (Objects.nonNull(closedTime)) {
                putIfNonNull(oldDealValues, dealClosedOnKey, closedTime.getTimeStamp());
            }
            putIfNonNull(newDealValues, dealClosedOnKey, Objects.isNull(closedTime) ? null : closedTime.getTimeStamp());
        }
        fieldAuditHandler.loadFieldAuditForTrackableFields(orgId, Source.DEAL, dealId, deal.getUpdatedBy().id(),
                deal.getFieldAuditInfo(), fields.values(), oldDealValues, newDealValues, targetContainer);
    }

    /**
     * <p>
     * Loads the options related to the state into container.
     * </p>
     *
     * @param optionRow       Represents the entity of {@link Row}
     * @param deal            Represents the entity of {@link Deal}
     * @param fieldId         Represents the id of the option field.
     * @param optionId        Represents the id of the {@link Option}.
     * @param targetContainer Represents the id of the {@link DataContainer}
     */
    private void loadStateOptionIntoContainer(final Row optionRow, final Deal deal, final Long fieldId, final Long optionId,
                                              final DataContainer targetContainer) {
        final Table dealDynamicOptionTable = getDealDynamicOptionTable();
        final Row row = Objects.isNull(optionRow) ? new Row(dealDynamicOptionTable) : optionRow;

        if (row.isNewRow()) {
            row.set(MDODROPDOWNFIELDS.ENTITY_ID, deal.id());
            row.set(MDODROPDOWNFIELDS.FIELD_ID, fieldId);
        }
        row.set(MDODROPDOWNFIELDS.OPTION_ID, optionId);
        targetContainer.updateRow(row);
    }

    /**
     * Gets the {@link Collection} of deals with respective state mapped for given contact.
     *
     * @param orgId     Represents the organization id
     * @param contactId Represents the contact id
     * @return A map which corresponds the {@link Collection} of deals in that state
     */
    public Map<Deal.State, Collection<Deal>> getContactDeals(final Long orgId, final Long contactId) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = buildPartialDealSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.CONTACT_ID), contactId)));

        return getDealsByState(orgId, selectQuery);
    }

    /**
     * Gets the deals based on state for given select query
     *
     * @param orgId       Represents the organization id
     * @param selectQuery Refers the {@link SelectQuery} to fetch the deal data
     * @return A map which corresponds the {@link Collection} of deals in that state
     */
    private Map<Deal.State, Collection<Deal>> getDealsByState(final Long orgId, final SelectQuery selectQuery) {
        final Map<Deal.State, Collection<Deal>> dealsByState = new HashMap<>();
        final Table dealTable = getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Function<Deal.State, Collection<Deal>> getDealsByState = (state) -> {
            final Collection<Row> rows = dataContainer.getRows(dealTable, new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.STATE), state.getId()))).toList();
            final Collection<Deal> deals = new ArrayList<>();

            rows.forEach(dealRow -> {
                final Deal deal = new Deal();
                final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);
                final Row pipelineStageRow = dataContainer.getRow(pipelineStageTable, pipelineStageId);
                final Long monetaryId = dealRow.get(MDODEAL.MONETARY_ID);
                final Row monetaryRow = dataContainer.getRow(monetaryTable, monetaryId);

                deal.setId(dealRow.getPKValue());
                deal.setOrgId(dealRow.getOrgId());
                deal.setTitle(dealRow.get(MDODEAL.TITLE));

                if (Objects.nonNull(pipelineStageRow)) {
                    deal.setPipelineStage(pipelineStageManager.convertPartialRowToModel(pipelineStageRow));
                }

                if (Objects.nonNull(monetaryRow)) {
                    final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);

                    deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));
                }
                deals.add(deal);
            });

            return deals;
        };
        final State open = State.OPEN;
        final State lost = State.LOST;
        final State won = State.WON;

        dealsByState.put(open, getDealsByState.apply(open));
        dealsByState.put(lost, getDealsByState.apply(lost));
        dealsByState.put(won, getDealsByState.apply(won));

        return dealsByState;
    }

    /**
     * Builds the {@link SelectQuery} to get partial deal data
     *
     * @return {@link SelectQuery} which contains the deal data
     */
    private SelectQuery buildPartialDealSelectQuery() {
        final Table dealTable = getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column stageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column stagePkColumn = pipelineStageTable.getPKColumn();
        final Column dealPkColumn = dealTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addJoinClause(List.of(new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()),
                new JoinClause(stageIdColumn, stagePkColumn)));
        selectQuery.addSelectClause(List.of(dealPkColumn, dealTable.getColumn(MDODEAL.ORG_ID), dealTable.getColumn(MDODEAL.TITLE),
                dealTable.getColumn(MDODEAL.STATE), stageIdColumn, stagePkColumn, pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), monetaryIdColumn));
        selectQuery.addSelectClause(monetaryTable);

        return selectQuery;
    }

    /**
     * Gets the {@link Collection} of deals with respective state mapped for given company.
     *
     * @param orgId     Represents the organization id
     * @param companyId Represents the company id
     * @return A map which corresponds the {@link Collection} of deals in that state
     */
    public Map<Deal.State, Collection<Deal>> getCompanyDeals(final Long orgId, final Long companyId) {
        final Table dealTable = getTable();
        final String companyKey = DealFields.COMPANY;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);
        final Long companyFieldId = fields.get(companyKey).id();
        final Table dealCompanyTable = getDealCompanyTable();
        final SelectQuery selectQuery = buildPartialDealSelectQuery();

        selectQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), companyId))
                .and(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyFieldId))));
        selectQuery.addSelectClause(dealCompanyTable);

        return getDealsByState(orgId, selectQuery);
    }

    /**
     * Converts partial row to {@link Deal}
     *
     * @param dealRow Represents deal row
     * @return {@link Deal}
     */
    public Deal convertRowToPartialModel(final Row dealRow) {
        final Deal deal = new Deal();

        deal.setId(dealRow.getPKValue());
        deal.setOrgId(dealRow.getOrgId());
        deal.setTitle(dealRow.get(MDODEAL.TITLE));

        return deal;
    }

    /**
     * <p>
     * Fetches the contact ids which is mapped with the given deal ids.
     * </p>
     *
     * @param orgId   Refers the organization id.
     * @param dealIds Refers the deal ids.
     * @return {@link Map} which contains deal id and their contact ids.
     */
    public Map<Long, Collection<Long>> getDealContactIds(final Long orgId, final Collection<Long> dealIds) {
        final Map<Long, Collection<Long>> dealContactIds = new HashMap<>();
        final Table participantTable = getParticipantTable();
        final SelectQuery selectQuery = SelectQuery.get(participantTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(participantTable).forEach(participantRow -> {
            final Collection<Long> contactIds = dealContactIds.computeIfAbsent(participantRow.get(MDOPARTICIPANT.DEAL_ID), deal -> new HashSet<>());

            contactIds.add(participantRow.get(MDOPARTICIPANT.CONTACT_ID));
        });

        return dealContactIds;
    }

    /**
     * Gets the contact id and company id of the deal.
     *
     * @param orgId  Refers the organization id.
     * @param dealId Refers the deal id.
     * @return {@link Map} which contains contact id and company id of the deal.
     */
    public Map<String, Long> getDealContactAndCompanyIds(final Long orgId, final Long dealId) {
        final Map<String, Long> contactAndCompanyIds = new HashMap<>();
        final Table table = getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column dealPkColumn = table.getPKColumn();
        final Column dealCompanyColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID);

        selectQuery.addSelectClause(List.of(dealCompanyColumn, table.getColumn(MDODEAL.CONTACT_ID)));
        selectQuery.addJoinClause(new JoinClause(dealPkColumn, dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID),
                LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPkColumn, dealId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row dealRow = dataContainer.getRow(table);
        final Row dealCompanyRow = dataContainer.getRow(dealCompanyTable);

        if (Objects.nonNull(dealRow)) {
            final Long contactId = dealRow.get(table.getColumn(MDODEAL.CONTACT_ID));

            if (Objects.nonNull(contactId)) {
                contactAndCompanyIds.put(MDODEAL.CONTACT_ID, contactId);
            }
        }

        if (Objects.nonNull(dealCompanyRow)) {
            contactAndCompanyIds.put(MDOCOMPANYFIELDS.COMPANY_ID, dealCompanyRow.get(dealCompanyColumn));
        }

        return contactAndCompanyIds;
    }

    /**
     * <p>
     * Gets the pipeline Id
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param dealId Represents the deal id
     * @return pipeline id matches for given deal id
     */
    public Long getPipelineId(final Long orgId, final Long dealId) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(dealTable.getColumn(MDODEAL.PIPELINE_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        return orgDataStore(orgId).get(selectQuery).getRow(dealTable).get(MDODEAL.PIPELINE_ID);
    }

    /**
     * <p>
     * Updates the pipeline id and pipeline stage id of the given deal.
     * </p>
     *
     * @param orgId           Represents the organization id.
     * @param pipelineId      Represents the id of the {@link Pipeline}
     * @param pipelineStageId Represents the id of the {@link PipelineStage}
     * @param dealId          Represents the id of the {@link Deal}
     */
    public void updatePipelineStage(final Long orgId, final Long userId, final Long pipelineId, final Long pipelineStageId, final Long dealId) {
        final Table dealTable = getTable();
        final Deal deal = new Deal();
        final Pipeline pipeline = new Pipeline();
        final PipelineStage pipelineStage = new PipelineStage();
        final DataContainer targetContainer = DataContainer.create();
        final long date = Instant.now().toEpochMilli();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery innerQuery = SelectQuery.get(dealTable);
        final DerivedTable derivedDealTable = DerivedTable.get(dealTable.getName(), dealTable.getAlias(), innerQuery);
        final SelectQuery selectQuery = SelectQuery.get(pipelineStageTable);
        final JoinClause.JoinType leftJoin = LEFT;
        final Map<String, String> oldDealValues = new HashMap<>();
        final Table dealDynamicOptionTable = getDealDynamicOptionTable();
        final String lostReason = DealFields.LOST_REASON;
        final String wonReason = DealFields.WON_REASON;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, wonReason, lostReason);
        final Long lostReasonFieldId = fields.get(lostReason).id();
        final Long wonReasonFieldId = fields.get(wonReason).id();
        final Column fieldIdColumn = dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.FIELD_ID);

        innerQuery.addSelectClause(dealTable);
        innerQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        selectQuery.addSelectClause(derivedDealTable, dealDynamicOptionTable);
        selectQuery.addSelectClause(pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME));
        selectQuery.addJoinClause(new JoinClause(pipelineStageTable.getPKColumn(), derivedDealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), leftJoin));
        selectQuery.addJoinClause(new JoinClause(derivedDealTable.getPKColumn(), dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), leftJoin));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(derivedDealTable.getPKColumn(), dealId))
                .or(Criteria.eq(pipelineStageTable.getPKColumn(), pipelineStageId)));
        buildDealProgressSelectQuery(derivedDealTable, selectQuery, leftJoin);

        final DataContainer entityContainer = orgDataStore(orgId).get(selectQuery);
        final Row dealRow = entityContainer.getRow(derivedDealTable, dealId);
        final WhereClause whereClause = new WhereClause(Criteria.eq(dealDynamicOptionTable.getColumn(MDODROPDOWNFIELDS.ENTITY_ID), dealId));
        final Row lostReasonRow = entityContainer.getRow(dealDynamicOptionTable, whereClause
                .and(Criteria.eq(fieldIdColumn, lostReasonFieldId)));
        final Row wonReasonRow = entityContainer.getRow(dealDynamicOptionTable, whereClause
                .and(Criteria.eq(fieldIdColumn, wonReasonFieldId)));
        final String oldPipelineStageValue = entityContainer.getRow(pipelineStageTable, (Long) dealRow.get(MDODEAL.PIPELINE_STAGE_ID)).get(MDOPIPELINESTAGE.NAME);
        final String newPipelineStageValue = entityContainer.getRow(pipelineStageTable, pipelineStageId).get(MDOPIPELINESTAGE.NAME);

        deal.setId(dealId);
        deal.setOrgId(orgId);
        pipeline.setId(pipelineId);
        pipelineStage.setId(pipelineStageId);
        deal.setPipelineStage(pipelineStage);
        deal.setPipeline(pipeline);
        deal.setState(State.getStateById(dealRow.get(MDODEAL.STATE)));
        deal.setSalesOwner(userService.getUser(orgId, dealRow.get(MDODEAL.OWNER_ID)));
        deal.setUpdatedBy(userService.getUser(orgId, userId));
        dealRow.set(MDODEAL.MODIFIED_TIME, Instant.now().toEpochMilli());

        loadDealStageProgressIntoContainer(false, deal, derivedDealTable, entityContainer, targetContainer);
        loadOldDealValues(deal, oldDealValues, dealRow, entityContainer);
        loadDealTimeline(deal, false, oldDealValues, dealRow, entityContainer, targetContainer);

        dealRow.set(MDODEAL.PIPELINE_ID, pipelineId);
        dealRow.set(MDODEAL.PIPELINE_STAGE_ID, pipelineStageId);
        dealRow.set(MDODEAL.STAGE_MODIFIED_TIME, date);
        dealRow.set(MDODEAL.STATE, State.OPEN.getId());
        dealRow.set(MDODEAL.UPDATED_BY, userId);
        dealRow.set(MDODEAL.CLOSED_TIME, null);

        if (Objects.nonNull(lostReasonRow)) {
            targetContainer.deleteRow(lostReasonRow);
        }

        if (Objects.nonNull(wonReasonRow)) {
            targetContainer.deleteRow(wonReasonRow);
        }
        targetContainer.updateRow(dealRow);
        loadPipelineFieldAuditEntry(deal, dealRow, entityContainer, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);

        auditLogger.log(orgId, List.of(new AuditLogData(userId, dealId, dealRow.get(MDODEAL.TITLE),
                AuditSource.DEAL, AuditAction.UPDATED, new AuditAttribute(PIPELINE_STAGE_KEY, oldPipelineStageValue, newPipelineStageValue, SINGLE_STRING))));
    }

    /**
     * Gets the deal id for the contact id provided.
     *
     * @param contactIds Represents the {@link Collection} contact id.
     * @param orgId      Represents the organization id.
     * @return {@link Map} deal ids mapped to the contact id.
     */
    public Map<Long, Collection<Long>> getDealIdsByContactIds(final Long orgId, final Collection<Long> contactIds) {
        final Table dealTable = getTable();
        final Table participantTable = ParticipantManager.getInstance().getTable();
        final Column contactColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Map<Long, Collection<Long>> dealIds = new HashMap<>();

        selectQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), participantTable.getColumn(MDOPARTICIPANT.DEAL_ID)));
        selectQuery.addSelectClause(List.of(contactColumn, participantTable.getColumn(MDOPARTICIPANT.DEAL_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(contactColumn, contactIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        for (final Long contactId : contactIds) {
            final Stream<Row> rows = dataContainer.getRows(participantTable, new WhereClause(Criteria.eq(contactColumn, contactId)));

            dealIds.put(contactId, rows.map(row -> (Long) row.get(MDOPARTICIPANT.DEAL_ID)).collect(Collectors.toUnmodifiableSet()));
        }

        return dealIds;
    }

    /**
     * <p>
     * Fetches the deal id associated with the specified organization id and contact ids if the provided contact ids
     * are linked to only one deal; otherwise, returns empty.
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param contactIds Represents the {@link Collection} of contact Ids
     * @return An Optional containing the deal ID if found, otherwise empty.
     */
    public Optional<Long> fetchAssociatedDealId(final Long orgId, final Collection<Long> contactIds) {

        if (!contactIds.isEmpty()) {
            final Table participantTable = ParticipantManager.getInstance().getTable();
            final SelectQuery selectQuery = SelectQuery.get(participantTable);
            final Column dealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);

            selectQuery.addSelectClause(dealIdColumn);
            selectQuery.addGroupByClause(new GroupByClause(dealIdColumn));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), contactIds)));
            final List<Map<String, Object>> dealIds = orgDataStore(orgId).getData(selectQuery);

            if (1 == dealIds.size()) {
                final Object dealIdValue = dealIds.stream().findFirst().get().get(MDOPARTICIPANT.DEAL_ID);

                if (dealIdValue instanceof final Long dealId) {
                    return Optional.of(dealId);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * Gets the deal by deal id, the deal contains only partial data.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param dealId Represents the id of the {@link Deal}
     * @return Entity of {@link Deal}
     */
    public Deal getPartialDealById(final Long orgId, final Long dealId) {
        final Table dealTable = getTable();
        final Column dealPKColumn = dealTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final String pipelineStageId = MDODEAL.PIPELINE_STAGE_ID;
        final String pipelineId = MDODEAL.PIPELINE_ID;

        selectQuery.addSelectClause(List.of(dealPKColumn, dealTable.getColumn(pipelineStageId),
                dealTable.getColumn(pipelineId), dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.OWNER_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPKColumn, dealId)));

        final Row dealRow = orgDataStore(orgId).get(selectQuery).getRow(dealTable);

        if (Objects.isNull(dealRow)) {
            return null;
        } else {
            final Deal deal = new Deal();
            final PipelineStage pipelineStage = new PipelineStage();
            final Pipeline pipeline = new Pipeline();

            deal.setId(dealRow.getPKValue());
            deal.setOrgId(orgId);
            deal.setTitle(dealRow.get(MDODEAL.TITLE));
            deal.setOwnerId(dealRow.get(MDODEAL.OWNER_ID));
            pipelineStage.setId(dealRow.get(pipelineStageId));
            pipeline.setId(dealRow.get(pipelineId));
            deal.setPipelineStage(pipelineStage);
            deal.setPipeline(pipeline);

            return deal;
        }
    }

    /**
     * Groups the primary emails of each participant based on the provided deals within a specific organization and user.
     *
     * @param orgId     Represents an organization id
     * @param dealIds   Represents the {@link Collection} of deal ids
     * @return A {@link Map} of {@link Collection} of contacts to their corresponding deal ids
     */
    public Map<Long, Collection<Contact>> groupParticipantsContactsByDeals(final long orgId, final Collection<Long> dealIds) {
        if (Objects.isNull(dealIds) || !exists(orgId, dealIds)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
        final Map<Long, Collection<Contact>> dealParticipantsContacts = new HashMap<>();
        final Collection<Participant> participants = getParticipantsByDeals(orgId, dealIds);

        for (final Participant participant : participants) {
            final Contact contact = participant.getContact();
            final Long dealId = participant.getDealId();

            if (Objects.nonNull(contact)) {
                dealParticipantsContacts.computeIfAbsent(dealId, emailIds -> new ArrayList<>()).add(contact);
            }
        }

        return dealParticipantsContacts;
    }

    /**
     * Retrieves a {@link Collection} of {@link Participant} associated with a {@link Collection} of deals
     * within a specific organization and linked to a particular user.
     *
     * @param orgId   Represents an organization id
     * @param dealIds Represents the {@link Collection} of deal id
     * @return {@link Collection} of {@link Participant}
     */
    public Collection<Participant> getParticipantsByDeals(final long orgId, final Collection<Long> dealIds) {
        final ContactManager contactManager = ContactManager.getInstance();
        final Table participantTable = ParticipantManager.getInstance().getTable();
        final Table contactTable = contactManager.getTable();
        final Table contactEmailTable = contactManager.getContactEmailTable();
        final Table emailTable = EmailManager.getInstance().getTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column emailPkColumn = emailTable.getPKColumn();
        final Column emailEntityIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID);
        final Column emailFieldIdColumn = contactEmailTable.getColumn(MCONEMAILFIELDS.FIELD_ID);
        final String emailKey = ContactFields.EMAILS;
        final Long emailFieldId = Optional.ofNullable(secureFieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailKey).get(emailKey))
                .map(FieldInfo::id).orElse(null);
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final JoinClause.JoinType leftJoin = LEFT;

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL),
                emailTable.getColumn(OMETEMAIL.VALUE), emailTable.getColumn(OMETEMAIL.TYPE),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID)));
        selectQuery.addSelectClause(contactEmailTable);
        selectQuery.addJoinClause(List.of(new JoinClause(contactPkColumn, participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID)),
                new JoinClause(contactPkColumn, emailEntityIdColumn, leftJoin),
                new JoinClause(contactEmailTable.getColumn(MCONEMAILFIELDS.EMAIL_ID), emailPkColumn, leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Collection<Participant> dealParticipants = new ArrayList<>();

        dataContainer.getRows(participantTable).forEach(participantRow -> {
            final Participant participant = new Participant();
            final Row contactRow = dataContainer.getRow(contactTable,
                    new WhereClause(Criteria.eq(contactPkColumn, participantRow.get(MDOPARTICIPANT.CONTACT_ID))));

            if (Objects.nonNull(contactRow)) {
                final Long contactId = contactRow.getPKValue();
                final Contact contact = new Contact();

                contact.setId(contactId);
                contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));

                if (Objects.nonNull(emailFieldId)) {
                    final Row contactEmailRow = dataContainer.getRow(contactEmailTable,
                            new WhereClause(Criteria.eq(emailEntityIdColumn, contactId)).and(Criteria.eq(emailFieldIdColumn, emailFieldId)));

                    if (Objects.nonNull(contactEmailRow)) {
                        final Row primaryEmailRow = dataContainer.getRow(emailTable,
                                new WhereClause(Criteria.eq(emailPkColumn, contactEmailRow.get(MCONEMAILFIELDS.EMAIL_ID))));

                        if (Objects.nonNull(primaryEmailRow)) {
                            contact.setPrimaryEmail(emailManager.convertRowToModel(primaryEmailRow));
                        }
                    }
                }
                participant.setContactId(contactId);
                participant.setDealId(participantRow.get(MDOPARTICIPANT.DEAL_ID));
                participant.setContact(contact);

                dealParticipants.add(participant);
            }
        });

        return dealParticipants;
    }

    /**
     * <p>
     * Converts the deal row to model
     * </p>
     *
     * @param dataContainer Represents the entity of {@link DataContainer}
     * @return Entity of {@link Deal}
     */
    public Deal convertDealRowToModel(final DataContainer dataContainer) {
        final Table dealTable = getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final Table monetaryTable = monetaryManager.getTable();
        final String companyKey = DealFields.COMPANY;
        final Long companyFieldId = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey).get(companyKey).id();
        final Row dealRow = dataContainer.getRow(dealTable);
        final Deal deal = new Deal();
        final PipelineStage pipelineStage = new PipelineStage();
        final Monetary monetary = monetaryManager.convertRowToModel(dataContainer.getRow(monetaryTable));
        final Long contactId = dealRow.get(MDODEAL.CONTACT_ID);
        final Row dealCompanyRow = dataContainer.getRow(dealTable,
                new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyFieldId)));

        pipelineStage.setName(dataContainer.getRow(pipelineStageTable).get(MDOPIPELINESTAGE.NAME));
        deal.setTitle(dealRow.get(MDODEAL.TITLE));
        deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
        deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));
        deal.setPipelineStage(pipelineStage);
        deal.setState(Deal.State.getStateById(dealRow.get(MDODEAL.STATE)));
        deal.setId(dealRow.getPKValue());
        deal.setOrgId(dealRow.getOrgId());
        deal.setStatus(dealRow.get(MDODEAL.STATUS));

        if (Objects.nonNull(contactId)) {
            final Contact contact = new Contact();

            contact.setId(contactId);
            deal.setPrimaryContact(contact);
        }

        if (Objects.nonNull(dealCompanyRow)) {
            final Company company = new Company();

            company.setId(dealCompanyRow.get(MDOCOMPANYFIELDS.COMPANY_ID));
            deal.setCompany(company);
        }

        return deal;
    }

    /**
     * Converts the {@link Row} of product deal to {@link Deal}
     *
     * @param dealRow       Represents the {@link Row} of {@link Deal}
     * @param dataContainer Holds the product related deals information
     * @return {@link Deal}
     */
    private Deal convertProductDealRowToModel(final Row dealRow, final DataContainer dataContainer) {
        final Table productDetailsHolderTable = Table.get(OMDOPRODUCTDETAILSHOLDER.TABLE);
        final Table productDetailTable = Table.get(MDOPRODUCTDETAIL.TABLE);
        final Table contactTable = getContactTable();
        final Long dealId = dealRow.getPKValue();
        final Deal deal = new Deal();
        final ProductDetailsHolder productDetailsHolder = new ProductDetailsHolder();
        final ProductDetail productDetail = new ProductDetail();

        deal.setOrgId(dealRow.getOrgId());
        deal.setId(dealId);
        deal.setTitle(dealRow.get(MDODEAL.TITLE));
        deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
        final Row productDetailsHolderRow = dataContainer.getRow(productDetailsHolderTable,
                new WhereClause(Criteria.eq(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), dealId)));
        final Row productDetailRow = dataContainer.getRow(productDetailTable,
                new WhereClause(Criteria.eq(productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID),
                        productDetailsHolderRow.getPKValue())));

        productDetail.setQuantity(productDetailRow.get(MDOPRODUCTDETAIL.QUANTITY));
        productDetailsHolder.setProductDetails(List.of(productDetail));
        final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable(), (Long) dealRow.get(MDODEAL.MONETARY_ID));

        if (Objects.nonNull(monetaryRow)) {
            final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);
            final DealValue dealValue = new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue());

            deal.setProductDetailsHolder(productDetailsHolder);
            deal.setDealValue(dealValue);
        }
        final Long primaryContactId = dealRow.get(MDODEAL.CONTACT_ID);
        final Row contactRow = dataContainer.getRow(contactTable, primaryContactId);

        if (Objects.nonNull(contactRow)) {
            deal.setPrimaryContact(ContactManager.getInstance().convertRowToPartialModel(contactRow));
        }
        return deal;
    }

    /**
     * <p>
     * Updates the pipeline based on given deal id and sets the default stage for the given deal id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param dealId     Represents teh deal id
     * @param pipelineId Represents the pipeline id
     * @param deal       Represents {@link Deal}.
     */
    public void changePipeline(final Long orgId, final Long userId, final Long dealId, final Long pipelineId, final Deal deal) {
        final Table dealTable = getTable();
        final Pipeline pipeline = new Pipeline();
        final DataContainer targetContainer = DataContainer.create();
        final PipelineStage defaultStage = pipelineStageManager.getDefaultPipelineStage(orgId, pipelineId);
        final Table pipelineTable = pipelineManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final SelectQuery innerQuery = SelectQuery.get(dealTable);
        final DerivedTable derivedDealTable = DerivedTable.get(dealTable.getName(), dealTable.getAlias(), innerQuery);
        final SelectQuery selectQuery = SelectQuery.get(pipelineTable);
        final Column pipelinePkColumn = pipelineTable.getPKColumn();
        final JoinClause.JoinType leftJoin = LEFT;

        innerQuery.addSelectClause(dealTable);
        innerQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        selectQuery.addSelectClause(derivedDealTable);
        selectQuery.addSelectClause(List.of(pipelineTable.getColumn(MDOPIPELINE.NAME), pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
        selectQuery.addJoinClause(List.of(
                new JoinClause(pipelinePkColumn, derivedDealTable.getColumn(MDODEAL.PIPELINE_ID), leftJoin),
                new JoinClause(derivedDealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID), pipelineStageTable.getPKColumn(), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(derivedDealTable.getPKColumn(), dealId))
                .or(Criteria.eq(pipelinePkColumn, pipelineId)));
        buildDealProgressSelectQuery(derivedDealTable, selectQuery, leftJoin);
        final DataContainer entityContainer = orgDataStore(orgId).get(selectQuery);
        final Row dealRow = entityContainer.getRow(derivedDealTable);
        final String oldPipelineValue = entityContainer.getRow(pipelineTable, (Long) dealRow.get(MDODEAL.PIPELINE_ID)).get(MDOPIPELINE.NAME);
        final String newPipelineValue = entityContainer.getRow(pipelineTable, pipelineId).get(MDOPIPELINE.NAME);
        final String oldPipelineStageValue = entityContainer.getRow(pipelineStageTable,
                (Long) dealRow.get(MDODEAL.PIPELINE_STAGE_ID)).get(MDOPIPELINESTAGE.NAME);
        final long currentTime = Instant.now().toEpochMilli();
        final String defaultStageName = defaultStage.getName();

        deal.setOrgId(orgId);
        deal.setId(dealId);
        pipeline.setId(pipelineId);
        pipeline.setName(newPipelineValue);
        deal.setPipeline(pipeline);
        deal.setPipelineStage(defaultStage);
        deal.setState(State.getStateById(dealRow.get(MDODEAL.STATE)));
        deal.setSalesOwner(userService.getUser(orgId, dealRow.get(MDODEAL.OWNER_ID)));
        loadDealStageProgressIntoContainer(false, deal, derivedDealTable, entityContainer, targetContainer);

        dealRow.set(MDODEAL.PIPELINE_ID, pipelineId);
        dealRow.set(MDODEAL.PIPELINE_STAGE_ID, defaultStage.id());
        dealRow.set(MDODEAL.STATE, State.OPEN.getId());
        dealRow.set(MDODEAL.STAGE_MODIFIED_TIME, currentTime);
        dealRow.set(MDODEAL.MODIFIED_TIME, currentTime);
        dealRow.set(MDODEAL.UPDATED_BY, userId);
        targetContainer.updateRow(dealRow);

        if (!Objects.equals(oldPipelineValue, newPipelineValue)) {
            final String pipelineKey = DealFields.PIPELINE;
            final JsonObject newValueJson = Json.object();

            newValueJson.put(pipelineKey, Json.object().put(TimelineFields.NEW_VALUE.getName(), newPipelineValue)
                    .put(TimelineFields.OLD_VALUE.getName(), oldPipelineValue));
            newValueJson.put(DealFields.PIPELINE_STAGE, Json.object().put(TimelineFields.NEW_VALUE.getName(),
                    defaultStageName).put(TimelineFields.OLD_VALUE.getName(), oldPipelineStageValue));

            deal.setUpdatedBy(userService.getUser(orgId, userId));
            buildAndLoadTimeline(false, dealRow, deal, null, newValueJson.toString(),
                    fieldService.groupDefaultFieldsByAliasName(Source.DEAL.getName()).get(pipelineKey).i18nName(), false,
                    pipelineKey, targetContainer);
        }
        loadPipelineFieldAuditEntry(userId, deal, oldPipelineValue, newPipelineValue, oldPipelineStageValue, defaultStageName, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
        final Collection<AuditLogData> auditLogDataList = new ArrayList<>();
        final AuditSource auditSource = AuditSource.DEAL;
        final String title = dealRow.get(MDODEAL.TITLE);
        final AuditAction auditAction = AuditAction.UPDATED;

        auditLogDataList.add(new AuditLogData(userId, dealId, title, auditSource, auditAction,
                new AuditAttribute(PIPELINE_STAGE_KEY, oldPipelineStageValue, defaultStageName, SINGLE_STRING)));
        auditLogDataList.add(new AuditLogData(userId, dealId, title, auditSource, auditAction,
                new AuditAttribute(PIPELINE_KEY, oldPipelineValue, newPipelineValue, DROPDOWN)));
        auditLogger.log(orgId, auditLogDataList);
    }

    /**
     * <p>
     * Loads the field edit history for pipeline and pipeline stage fields when pipeline is changed for a deal
     * </p>
     *
     * @param userId                Represents the user id who changed the pipeline
     * @param deal                  Represents the deal for which pipeline is changed
     * @param oldPipelineValue      Represents the old pipeline value before change
     * @param newPipelineValue      Represents the new pipeline value after change
     * @param oldPipelineStageValue Represents the old pipeline stage value before change
     * @param newStageName          Represents the default stage name of new pipeline
     * @param targetContainer       Represents the {@link DataContainer} to load the field edit history data
     */
    private void loadPipelineFieldAuditEntry(final Long userId, final Deal deal, final String oldPipelineValue, final String newPipelineValue,
            final String oldPipelineStageValue, final String newStageName, final DataContainer targetContainer) {
        final Long orgId = deal.orgId();
        final FieldTrackingConfig fieldTrackingConfig = fieldTrackingConfigManager.getOrDefault(orgId, (short) Source.DEAL.getId());

        if (fieldTrackingConfig.isTrackingEnabled()) {
            final Map<String, String> oldFieldValues = new HashMap<>();
            final Map<String, String> newFieldValues = new HashMap<>();
            final String pipelineFieldKey = DealFields.PIPELINE;
            final String pipelineStageFieldKey = DealFields.PIPELINE_STAGE;
            final Collection<FieldInfo> trackedFields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, pipelineFieldKey, pipelineStageFieldKey).values();

            if (Objects.isNull(deal.getUpdatedBy())) {
                deal.setUpdatedBy(userService.getUser(orgId, userId));
            }

            if (fieldTrackingConfig.isFieldTracked(pipelineFieldKey)) {
                putIfNonNull(oldFieldValues, pipelineFieldKey, oldPipelineValue);
                putIfNonNull(newFieldValues, pipelineFieldKey, newPipelineValue);
            }

            if (fieldTrackingConfig.isFieldTracked(pipelineStageFieldKey)) {
                putIfNonNull(oldFieldValues, pipelineStageFieldKey, oldPipelineStageValue);
                putIfNonNull(newFieldValues, pipelineStageFieldKey, newStageName);
            }

            if (!oldFieldValues.isEmpty() && !newFieldValues.isEmpty()) {
                fieldAuditHandler.loadFieldAuditForTrackableFields(orgId, Source.DEAL, deal.id(), deal.getUpdatedBy().id(),
                        deal.getFieldAuditInfo(), trackedFields, oldFieldValues, newFieldValues, targetContainer);
            }
        }
    }

    /**
     * <p>
     * Updates the expected close date of the given deal
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param userId            Represents the user id
     * @param deal              Represents the deal
     * @param expectedCloseDate Represents the expectedCloseDate
     */
    public void updateExpectedCloseDate(final Long orgId, final long userId, final Deal deal, final Long expectedCloseDate) {
        final Table dealTable = getTable();
        final DataContainer entityContainer = orgDataStore(orgId).get(dealTable, new WhereClause(Criteria.eq(dealTable.getPKColumn(), deal.id())));
        final Row dealRow = Objects.nonNull(entityContainer) ? entityContainer.getRow(dealTable) : null;
        final DataContainer targetContainer = DataContainer.create();

        if (Objects.nonNull(dealRow)) {
            final long currentTime = Instant.now().toEpochMilli();
            final Map<String, String> oldDealValues = new HashMap<>();
            final Date oldExpectedCloseDate = entityContainer.getRow(dealTable).get(MDODEAL.EXPECTED_CLOSE_DATE);
            final Long existingForecastCategoryId = dealRow.get(MDODEAL.FORECAST_CATEGORY_ID);
            final ForecastCategory existingForecastCategory = Objects.isNull(existingForecastCategoryId)
                    ? null : forecastCategoryManager.get(orgId, existingForecastCategoryId);

            if (Objects.nonNull(oldExpectedCloseDate)) {
                oldDealValues.put(DealFields.EXPECTED_CLOSE_DATE, String.valueOf(oldExpectedCloseDate.getTimeStamp()));
            }

            deal.setModifiedTime(new DateTime(currentTime));
            dealRow.set(dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), expectedCloseDate);
            dealRow.set(dealTable.getColumn(MDODEAL.MODIFIED_TIME), Instant.now().toEpochMilli());
            dealRow.set(dealTable.getColumn(MDODEAL.UPDATED_BY), userId);
            deal.setSalesOwner(userService.getUser(orgId, dealRow.get(MDODEAL.OWNER_ID)));

            if (Objects.isNull(existingForecastCategory) || existingForecastCategory.isCommittedCategory()) {
                final ForecastCategory defaultForecastCategory = forecastCategoryManager.getDefaultCategory(orgId);

                if (Objects.nonNull(defaultForecastCategory)) {
                    dealRow.set(dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID), defaultForecastCategory.id());
                    deal.setForecastCategory(defaultForecastCategory);
                    oldDealValues.put(DealFields.FORECAST_CATEGORY, Objects.isNull(existingForecastCategory) ? null : existingForecastCategory.getName());
                }
            }

            targetContainer.updateRow(dealRow);

            loadDealTimeline(deal, false, oldDealValues, dealRow, entityContainer, targetContainer);

            if (fieldTrackingConfigManager.isTracked(orgId, (short) Source.DEAL.getId(), DealFields.EXPECTED_CLOSE_DATE)) {
                fieldService.getDefaultFieldByAliasName(Source.DEAL, DealFields.EXPECTED_CLOSE_DATE)
                        .ifPresent(fieldInfo -> {
                            fieldAuditHandler.loadFieldAuditForTrackableFields(orgId, Source.DEAL, deal.id(),
                                    deal.getUpdatedBy().id(), deal.getFieldAuditInfo(), List.of(fieldInfo), oldDealValues,
                                    Map.of(DealFields.EXPECTED_CLOSE_DATE, String.valueOf(expectedCloseDate)), targetContainer);
                        });
            }
            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Gets the {@link DealStatistics} count of deal by contact based on state
     * </p>
     *
     * @param orgId     Represents an organisation id
     * @param contactId Represents the contact id
     * @return {@link DealStatistics} of deal count of contact
     */
    public DealStatistics getDealStatistics(final long orgId, final Long contactId) {
        final DealStatistics dealStatistics = new DealStatistics();
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column dealStateColumn = dealTable.getColumn(MDODEAL.STATE);

        selectQuery.addSelectClause(List.of(dealTable.getPKColumn().count(), dealStateColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.CONTACT_ID), contactId)));
        selectQuery.addGroupByClause(new GroupByClause(dealStateColumn));
        final List<Map<String, Object>> rows = orgDataStore(orgId).getData(selectQuery);

        rows.forEach(row -> {
            final int stateId = (int) row.get(MDODEAL.STATE);
            final Long count = (Long) row.get(MDODEAL.ID);

            switch (stateId) {
                case 1 -> dealStatistics.setOpenDealsCount(count);
                case 2 -> dealStatistics.setWonDealsCount(count);
                case 3 -> dealStatistics.setLostDealsCount(count);
            }
        });

        return dealStatistics;
    }

    /**
     * <p>
     * Checks whether the value is default along with base currency.
     * </p>
     *
     * @param orgId Represents an organization
     * @param value Represents the value
     * @return true, if the value is default; otherwise false
     */
    private boolean isDefaultValue(final Long orgId, final String value) {
        final String symbol = baseCurrencyService.getDefaultCurrency(orgId).getSymbol();

        if (!value.startsWith(symbol)) {
            return false;
        }

        try {
            final String numericPart = value.substring(symbol.length()).trim();

            return Double.parseDouble(numericPart) == 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * <p>
     * Fetches a collection of {@link Deal} for the given owner ids
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param ownerIds Which contains a collection of owner ids
     * @return The collection of {@link Deal}
     */
    public Collection<Deal> getDeals(final long orgId, final Collection<Long> ownerIds) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column ownerIdColumn = dealTable.getColumn(MDODEAL.OWNER_ID);

        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), ownerIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(ownerIdColumn, ownerIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }
        return dataContainer.getRows(dealTable).map(dealRow -> {
            final Deal deal = new Deal();

            deal.setOrgId(orgId);
            deal.setId(dealRow.getPKValue());
            deal.setTitle(dealRow.get(MDODEAL.TITLE));
            deal.setOwnerId(dealRow.get(MDODEAL.OWNER_ID));

            return deal;
        }).toList();
    }

    /**
     * <p>
     * Updates the deal values for the given product ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param productIds      Which contains the collection of product ids
     * @param targetContainer Holds the updated deal details into the target {@link DataContainer}
     */
    public void updateDealValues(final long orgId, final Set<Long> productIds, final DataContainer targetContainer) {
        final Table productDetailTable = ProductDetailsHolderManager.getInstance().getProductDetailTable();

        updateDealValues(orgId, productIds, productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID),
                productDetailsHolderManager.fetchDealValuesByProductIds(orgId, productIds), targetContainer);
    }

    /**
     * <p>
     * Updates the deal values for the given variation ids
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param variationIds    Which contains the collection of variation ids
     * @param targetContainer Holds the updated deal details into the target {@link DataContainer}
     */
    public void updateDealValuesByVariationIds(final long orgId, final Set<Long> variationIds, final DataContainer targetContainer) {
        final Table productDetailTable = ProductDetailsHolderManager.getInstance().getProductDetailTable();

        updateDealValues(orgId, variationIds, productDetailTable.getColumn(MDOPRODUCTDETAIL.VARIATION_ID),
                productDetailsHolderManager.fetchDealValuesByVariationIds(orgId, variationIds), targetContainer);
    }

    /**
     * <p>
     * Updates the deal values for the given linked product detail references
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param linkedEntityIds Which contains the collection of linked entity ids
     * @param entityIdColumn Represents the linked field in {@link MDOPRODUCTDETAIL}
     * @param dealValues      Contains deal id as key and linked product amount as value
     * @param targetContainer Holds the updated deal details into the target {@link DataContainer}
     */
    private void updateDealValues(final long orgId, final Set<Long> linkedEntityIds, final Column entityIdColumn,
                                  final Map<Long, Double> dealValues, final DataContainer targetContainer) {
        if (!dealValues.isEmpty()) {
            final Table dealTable = getTable();
            final Table monetaryTable = monetaryManager.getTable();
            final SelectQuery dealSelectQuery = SelectQuery.get(dealTable);
            final Column dealPkColumn = dealTable.getPKColumn();
            final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
            final JoinClause.JoinType left = LEFT;
            final ProductDetailsHolderManager detailsHolderManager = ProductDetailsHolderManager.getInstance();
            final Table produtcDetailHolderTable = detailsHolderManager.getTable();
            final Table productDetailTable = detailsHolderManager.getProductDetailTable();
            final Column productDealIdColumn = produtcDetailHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID);
            final Column productDetailsHolderIdColumn = productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID);

            dealSelectQuery.addSelectClause(List.of(monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID), productDealIdColumn,
                    productDetailsHolderIdColumn, entityIdColumn, monetaryTable.getColumn(OMETMONETARY.VALUE),
                    dealTable.getColumn(MDODEAL.MONETARY_ID)));
            dealSelectQuery.addJoinClause(List.of(new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()),
                    new JoinClause(dealPkColumn, productDealIdColumn, left),
                    new JoinClause(produtcDetailHolderTable.getPKColumn(), productDetailsHolderIdColumn, left)));
            dealSelectQuery.setWhereClause(new WhereClause(Criteria.in(dealPkColumn, dealValues.keySet())));

            final DataContainer dataContainer = orgDataStore(orgId).get(dealSelectQuery);

            dataContainer.getRows(dealTable).forEach(row -> {
                final Row monetaryRow = dataContainer.getRow(monetaryTable, (Long) row.get(MDODEAL.MONETARY_ID));
                final Long dealPkValue = row.getPKValue();

                if (Objects.nonNull(monetaryRow) && dealValues.containsKey(dealPkValue)) {
                    monetaryRow.set(OMETMONETARY.VALUE, (Double) monetaryRow.get(OMETMONETARY.VALUE) - dealValues.get(dealPkValue));
                    targetContainer.updateRow(monetaryRow);
                }
                final Row detailHolderRow = dataContainer.getRow(produtcDetailHolderTable,
                        new WhereClause(Criteria.eq(productDealIdColumn, dealPkValue)));

                if (Objects.nonNull(detailHolderRow)) {
                    final Stream<Row> detailRows = dataContainer.getRows(productDetailTable,
                            new WhereClause(Criteria.eq(productDetailsHolderIdColumn, detailHolderRow.getPKValue())));

                    final List<Row> detailRowList = detailRows.toList();
                    final long detailCount = detailRowList.stream()
                            .map(detailRow -> (Long) detailRow.get(entityIdColumn))
                            .filter(linkedEntityIds::contains)
                            .count();

                    if (detailRowList.isEmpty() || detailRowList.size() == detailCount) {
                        targetContainer.deleteRow(detailHolderRow);
                    }
                }
            });
        }
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Sets the total product for the given deal
     * </p>
     *
     * @param deal          Represents the {@link Deal}
     * @param dataContainer Holds the updated information of user defined and system field values
     */
    private void setTotalProduct(final Deal deal, final DataContainer dataContainer) {
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailTable = productDetailsHolderManager.getProductDetailTable();
        final Row productDetailsHolderRow = dataContainer.getRow(productDetailsHolderTable,
                new WhereClause(Criteria.eq(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), deal.id())));

        if (Objects.nonNull(productDetailsHolderRow)) {
            final Stream<Row> productDetailsRows = dataContainer.getRows(productDetailTable,
                    new WhereClause(Criteria.eq(productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID),
                            productDetailsHolderRow.getPKValue())));

            deal.setTotalProductCount(productDetailsRows.count());
        }
    }

    /**
     * <p>
     * Calculates the deal monetary value
     * </p>
     *
     * @param value            Represents the value to be calculated
     * @param stageProbability Represents the stage probability of the deal
     * @return The calculated value
     */
    public BigDecimal calculateDealMonetaryValue(final BigDecimal value, final Double stageProbability) {
        return value.multiply(BigDecimal.valueOf(stageProbability / 100));
    }

    /**
     * <p>
     * Gets the {@link Email} primary email mapped for the given deal id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param dealId Represents the deal id
     * @return the {@link Email}
     */
    public Optional<Email> getPrimaryEmail(final long orgId, final Long dealId) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Table contactTable = contactManager.getTable();
        final Table emailTable = emailManager.getTable();

        selectQuery.addSelectClause(List.of(emailTable.getColumn(OMETEMAIL.VALUE), emailTable.getColumn(OMETEMAIL.TYPE)));
        selectQuery.addJoinClause(List.of(new JoinClause(dealTable.getColumn(MDODEAL.CONTACT_ID), contactTable.getPKColumn()),
                new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        return Optional.ofNullable(orgDataStore(orgId).get(selectQuery).getRow(emailTable)).map(contactManager::convertEmailRowToModel);
    }

    /**
     * <p>
     * Gets the {@link Phone} primary phone mapped for the given deal id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param dealId Represents the deal id
     * @return the {@link Phone}
     */
    public Optional<Phone> getPrimaryPhone(final long orgId, final Long dealId) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Table contactTable = contactManager.getTable();
        final Table phoneTable = phoneManager.getTable();

        selectQuery.addSelectClause(List.of(phoneTable.getColumn(OMETPHONE.NUMBER), phoneTable.getColumn(OMETPHONE.TYPE),
                phoneTable.getColumn(OMETPHONE.COUNTRY_ID)));
        selectQuery.addJoinClause(List.of(new JoinClause(dealTable.getColumn(MDODEAL.CONTACT_ID), contactTable.getPKColumn()),
                new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        return Optional.ofNullable(orgDataStore(orgId).get(selectQuery).getRow(phoneTable)).map(contactManager::convertPhoneRowToModel);
    }

    /**
     * <p>
     * Retrieves the total deal count for dropdown
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represent the dropdown criteria
     * @param skipArchived     Specifies whether to skip the archived deal
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria, final boolean skipArchived) {
        final Table table = getTable();
        final Table stageTable = Table.get(MDOPIPELINESTAGE.TABLE);
        final Column dealPkColumn = table.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(stageTable);

        selectQuery.addJoinClause(DmlUtil.getJoins(stageTable, table));
        selectQuery.addSelectClause(dealPkColumn);
        selectQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(MDODEAL.TITLE)))
                .or(dropdownCriteria.build(stageTable.getColumn(MDOPIPELINESTAGE.NAME))));

        if (skipArchived) {
            selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(), new WhereClause(Criteria.eq(table.getColumn(MDODEAL.STATUS), Deal.Status.ACTIVE.getId())), Connector.AND));
        }
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.in(dealPkColumn, selectQuery)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link Deal} mapped for the given contact id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @param start    Represents the starting index of {@link Contact} to be fetched
     * @param limit    Represents the maximum number of {@link Contact} to be fetched
     * @param stateIds Represents the collection of state Ids
     * @param source   Represent the {@link Source}
     * @return {@link Collection} of {@link Deal}
     */
    public Collection<Deal> getDealsBySource(final long orgId, final Long entityId, final int start, final int limit, final Collection<Long> stateIds, final Source source) {
        final Table dealTable = getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table contactTable = getContactTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table participantTable = getParticipantTable();
        final SelectQuery pkFetchQuery = SelectQuery.get(dealTable);

        pkFetchQuery.addSelectClause(dealTable.getPKColumn());
        loadEntityCriteria(source, entityId, pkFetchQuery);

        if (Objects.nonNull(stateIds) && !stateIds.isEmpty()) {
            pkFetchQuery.setWhereClause(WhereClauseUtil.connect(pkFetchQuery.getWhereClause(), new WhereClause(Criteria
                    .in(dealTable.getColumn(MDODEAL.STATE), stateIds)), Connector.AND));
        }
        pkFetchQuery.setLimitClause(new LimitClause(start, limit));

        final Collection<Long> pkValues = orgDataStore(orgId).get(pkFetchQuery).getRows(dealTable).filter(Objects::nonNull).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Collection<Deal> deals = new ArrayList<>();
        final Column participantDealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);
        final Column dealPipelineStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column dealMonetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column participantContactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);

        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE),
                dealTable.getColumn(MDODEAL.PIPELINE_ID), dealTable.getColumn(MDODEAL.CONTACT_ID), dealPipelineStageIdColumn, dealMonetaryIdColumn,
                dealTable.getColumn(MDODEAL.ORG_ID), pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME), pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER),
                monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID), participantDealIdColumn, monetaryTable.getColumn(OMETMONETARY.VALUE),
                monetaryTable.getColumn(OMETMONETARY.ORG_ID), participantContactIdColumn,
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME)));
        selectQuery.addJoinClause(List.of(new JoinClause(dealPipelineStageIdColumn, pipelineStageTable.getPKColumn()),
                new JoinClause(dealMonetaryIdColumn, monetaryTable.getPKColumn()),
                new JoinClause(dealTable.getPKColumn(), participantDealIdColumn),
                new JoinClause(participantContactIdColumn, contactTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getPKColumn(), pkValues)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (!dataContainer.isEmpty()) {
            final Collection<Row> dealRows = dataContainer.getRows(dealTable).toList();
            final Collection<Long> dealIds = new HashSet<>();
            final Collection<Long> pipelineIds = new HashSet<>();

            dealRows.forEach(dealRow -> {
                dealIds.add(dealRow.getPKValue());
                pipelineIds.add(dealRow.get(MDODEAL.PIPELINE_ID));
            });

            final Map<Long, Long> pipelineStagesCount = pipelineStageManager.getPipelineStagesCount(orgId, pipelineIds);
            final Map<Long, Company> dealCompanies = companyManager.getDealCompanies(orgId, dealIds);

            dealRows.forEach(row -> {
                final Deal deal = partialConvertRowToModel(row, dataContainer);

                deal.setCompany(dealCompanies.get(deal.id()));

                loadPipelineIntoDeal(row, deal, pipelineStagesCount, dataContainer);

                deals.add(deal);
            });
        }
        return deals;
    }

    /**
     * <p>
     * Loads the pipeline and pipeline stage details into the given deal
     * </p>
     *
     * @param row                 Represents the {@link Deal} row
     * @param deal                Represents the {@link Deal}
     * @param pipelineStagesCount A {@link Map} which holds the pipeline and their stages count
     * @param dataContainer       Represents the {@link DataContainer}
     */
    private void loadPipelineIntoDeal(final Row row, final Deal deal, final Map<Long, Long> pipelineStagesCount, final DataContainer dataContainer) {
        final Pipeline pipeline = new Pipeline();
        final Long pipelineStageId = row.get(MDODEAL.PIPELINE_STAGE_ID);
        final Long pipelineId = row.get(MDODEAL.PIPELINE_ID);
        final Row pipelineStageRow = dataContainer.getRow(pipelineStageManager.getTable(), pipelineStageId);
        final PipelineStage pipelineStage = new PipelineStage();

        pipelineStage.setId(pipelineStageRow.getPKValue());
        pipelineStage.setOrder(pipelineStageRow.get(MDOPIPELINESTAGE.ORDER));
        pipelineStage.setName(pipelineStageRow.get(MDOPIPELINESTAGE.NAME));

        pipeline.setId(pipelineId);
        pipeline.setTotalStagesCount(pipelineStagesCount.get(pipelineId));

        deal.setPipelineStage(pipelineStage);
        deal.setPipeline(pipeline);
    }

    /**
     * <p>
     * Gets the count of the deals by the given contact id
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @param stateIds Represents the Collection of state ids
     * @param source   Represents the {@link Source}
     * @return the count of the deals by the given contact id
     */
    public Long getDealsCountBySource(final long orgId, final Long entityId, final Collection<Long> stateIds, final Source source) {
        final Table dealTable = getTable();
        final CountQuery countQuery = CountQuery.get(dealTable);

        loadEntityCriteria(source, entityId, countQuery);

        if (Objects.nonNull(stateIds) && !stateIds.isEmpty()) {
            countQuery.setWhereClause(WhereClauseUtil.connect(countQuery.getWhereClause(), new WhereClause(Criteria.in(dealTable.getColumn(MDODEAL.STATE), stateIds)), Connector.AND));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Loads the criteria for the given entity id
     * </p>
     *
     * @param source     Represents the {@link Source}
     * @param entityId   Represents the entity id
     * @param multiQuery Represents the {@link MultiQuery}
     */
    private void loadEntityCriteria(final Source source, final Long entityId, final MultiQuery multiQuery) {
        switch (source) {
            case PRODUCT -> {
                final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
                final Table productDetailsTable = productDetailsHolderManager.getProductDetailTable();
                final JoinClause.JoinType leftJoin = LEFT;

                multiQuery.addJoinClause(List.of(new JoinClause(getTable().getPKColumn(), productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), leftJoin),
                        new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), leftJoin)));
                multiQuery.setWhereClause(new WhereClause(Criteria.eq(productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), entityId)));
            }
            case CONTACT -> {
                final Table participantTable = getParticipantTable();

                multiQuery.addJoinClause(List.of(new JoinClause(getTable().getPKColumn(), participantTable.getColumn(MDOPARTICIPANT.DEAL_ID)),
                        new JoinClause(participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID), getContactTable().getPKColumn())));
                multiQuery.setWhereClause(new WhereClause(Criteria.eq(getParticipantTable().getColumn(MDOPARTICIPANT.CONTACT_ID), entityId)));
            }
            case COMPANY -> {
                final Table dealTable = getTable();
                final Table dealCompanyTable = getDealCompanyTable();
                final FieldInfo fieldInfo = fieldService.getDefaultFieldByAliasName(Source.DEAL, DealFields.COMPANY).orElseGet(() -> null);

                if (Objects.nonNull(fieldInfo)) {
                    multiQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID)));
                    multiQuery.setWhereClause(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), entityId)).and(
                            Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fieldInfo.id())));
                }
            }
        }
    }

    /**
     * <p>
     * Gets the {@link WhereClause} based on state
     * </p>
     *
     * @param state Represents the {@link State}
     * @return {@link WhereClause}
     */
    private WhereClause getWhereClauseByState(final State state) {
        final Column pipelineStageNameColumn = pipelineStageManager.getTable().getColumn(MDOPIPELINESTAGE.NAME);

        return new WhereClause(Objects.equals(State.OPEN, state) ? Criteria.notIn(pipelineStageNameColumn,
                List.of(State.WON.getName(), State.LOST.getName())) : Criteria.eq(pipelineStageNameColumn, state.getName()));
    }

    /**
     * <p>
     * Converts the {@link Row} to partial {@link Deal}
     * </p>
     *
     * @param row           Represents the deal row
     * @param dataContainer Represents the {@link DataContainer} which contains the deal related records
     * @return {@link Deal}
     */
    private Deal partialConvertRowToModel(final Row row, final DataContainer dataContainer) {
        final Table participantTable = getParticipantTable();
        final Long orgId = row.getOrgId();
        final Long dealId = row.getPKValue();
        final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable(), (Long) row.get(MDODEAL.MONETARY_ID));
        final Stream<Row> participantRows = dataContainer.getRows(participantTable, new WhereClause(Criteria.eq(participantTable.getColumn(MDOPARTICIPANT.DEAL_ID), dealId)));
        final Collection<Participant> participants = new ArrayList<>();
        final Deal deal = new Deal();
        final Long primaryContactId = row.get(MDODEAL.CONTACT_ID);

        if (Objects.nonNull(monetaryRow)) {
            final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);
            final DealValue dealValue = new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue());

            deal.setDealValue(dealValue);
        }

        participantRows.forEach(participantRow -> {
            final Long participantContactId = participantRow.get(MDOPARTICIPANT.CONTACT_ID);
            final Table contactTable = contactManager.getTable();
            final Row contactRow = dataContainer.getRow(contactTable, participantContactId);
            final Participant participant = new Participant();
            final Contact contact = new Contact();

            contact.setOrgId(orgId);
            contact.setId(participantContactId);

            if (Objects.nonNull(contactRow)) {
                contact.setFirstName(contactRow.get(MCOCONTACT.FIRST_NAME));
                contact.setLastName(contactRow.get(MCOCONTACT.LAST_NAME));
            }

            participant.setContact(contact);
            participants.add(participant);

            if (Objects.equals(primaryContactId, participantContactId)) {
                deal.setPrimaryContact(contact);
            }
        });

        deal.setTitle(row.get(MDODEAL.TITLE));
        deal.setExpectedCloseDate(row.get(MDODEAL.EXPECTED_CLOSE_DATE));
        deal.setId(dealId);
        deal.setOrgId(row.getOrgId());
        deal.setParticipants(participants);

        return deal;
    }

    /**
     * Fetches the {@link Deal} for the dropdown actions with their primary contact details.
     *
     * @param orgId            Represents an organization id
     * @param dropdownLimit    Represents the {@link DropdownLimit}
     * @param dropdownCriteria Represents the {@link DropdownCriteria}
     * @param skipArchived     Specifies whether to skip the archived deal
     * @return {@link Collection} of deals.
     */
    public Collection<Deal> fetchDeals(final long orgId, final DropdownLimit dropdownLimit, final DropdownCriteria dropdownCriteria,
                                       final boolean skipArchived) {
        final Table dealTable = getTable();
        final Table contactTable = getContactTable();
        final Column dealContactIdColumn = dealTable.getColumn(MDODEAL.CONTACT_ID);
        final SelectQuery selectQuery = getSelectQuery(dropdownLimit, dropdownCriteria, skipArchived);
        final Collection<Deal> deals = new ArrayList<>();

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME), dealContactIdColumn));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        } else {
            final Collection<Row> rows = dataContainer.getRows(dealTable).toList();
            final Map<Long, Company> dealCompanies = companyManager.getDealCompanies(orgId, rows.stream().map(Row::getPKValue).toList());

            rows.forEach(row -> {
                final Deal deal = convertRowToPartialModel(row, dataContainer);

                deal.setCompany(dealCompanies.get(deal.id()));

                deals.add(deal);
            });

            return deals;
        }
    }

    /**
     * <p>
     * Gets the {@link SelectQuery} for retrieving the contact ID associated with a specific deal
     * </p>
     *
     * @param dealId Represents the deal id
     * @return the {@link SelectQuery}
     */
    public SelectQuery getPrimaryContactSelectQuery(final long dealId) {
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(dealTable.getColumn(MDODEAL.CONTACT_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        return selectQuery;
    }

    /**
     * <p>
     * Fetches the names of web forms associated with a given collection of deal ids for a specified organization id
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param dealIds Which contains the collection of deal ids
     * @return A map where the key is the deal id and the value is the name of the associated web form.
     */
    public Map<Long, String> getWebFormNames(final long orgId, final Collection<Long> dealIds) {
        final Table dealWebFormTable = Table.get(MWFDEALWEBFORM.TABLE);
        final Table webFormTable = webFormManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealWebFormTable);
        final Column dealWebFormIdColumn = dealWebFormTable.getColumn(MWFDEALWEBFORM.WEB_FORM_ID);
        final Column webFormDealIdColumn = dealWebFormTable.getColumn(MWFDEALWEBFORM.DEAL_ID);
        final Column webFormPKColumn = webFormTable.getPKColumn();

        selectQuery.addSelectClause(List.of(webFormTable.getColumn(OMWFWEBFORM.NAME), dealWebFormIdColumn, webFormDealIdColumn));
        selectQuery.addJoinClause(new JoinClause(dealWebFormIdColumn, webFormPKColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(webFormDealIdColumn, dealIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Long, String> webFormNames = new HashMap<>();

        dataContainer.getRows(dealWebFormTable).forEach(dealRow -> {
            final Long dealId = dealRow.get(MWFDEALWEBFORM.DEAL_ID);
            final Row webFormDealRow = dataContainer.getRow(dealWebFormTable, new WhereClause(Criteria.eq(webFormDealIdColumn, dealId)));

            if (Objects.nonNull(webFormDealRow)) {
                final Row webFormRow = dataContainer.getRow(webFormTable, new WhereClause(Criteria.eq(webFormPKColumn, webFormDealRow.get(MWFCONTACTWEBFORM.WEB_FORM_ID))));

                if (Objects.nonNull(webFormRow)) {
                    webFormNames.put(dealId, webFormRow.get(OMWFWEBFORM.NAME));
                }
            }
        });
        return webFormNames;
    }

    /**
     * <p>
     * Fetches the collection of deals for thr given org id and deal ids
     * *</p>
     *
     * @param orgId   Represents the organization id
     * @param dealIds Which contains the collection of deal ids
     * @return The {@link Map} containing key is the deal id value is the {@link Deal}
     */
    public Map<Long, Deal> fetchDeals(final long orgId, final Collection<Long> dealIds) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column dealPkColumn = dealTable.getPKColumn();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table pipelineTable = pipelineManager.getTable();
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailsTable = productDetailsHolderManager.getProductDetailTable();
        final Table productTable = productManager.getTable();
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column stageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column stagePkColumn = pipelineStageTable.getPKColumn();
        final Column pipelinePkColumn = pipelineTable.getPKColumn();
        final Column pipelineIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_ID);
        final JoinClause.JoinType leftJoin = LEFT;

        selectQuery.addSelectClause(productDetailsTable, productDetailsHolderTable, productTable, pipelineTable, pipelineStageTable);
        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.CREATED_TIME), dealTable.getColumn(MDODEAL.ORG_ID), dealTable.getColumn(MDODEAL.MODIFIED_TIME),
                dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.STATE), dealTable.getColumn(MDODEAL.CLOSED_TIME),
                dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE),
                monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID), monetaryTable.getColumn(OMETMONETARY.VALUE),
                monetaryTable.getColumn(OMETMONETARY.ORG_ID), monetaryIdColumn, stageIdColumn, pipelineIdColumn));
        selectQuery.addJoinClause(List.of(new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()),
                new JoinClause(stageIdColumn, stagePkColumn), new JoinClause(pipelineIdColumn, pipelinePkColumn),
                new JoinClause(dealPkColumn, productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), leftJoin),
                new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), leftJoin),
                new JoinClause(productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), productTable.getPKColumn(), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealPkColumn, dealIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Long, Deal> dealsMap = new HashMap<>();

        dataContainer.getRows(dealTable).forEach(dealRow -> {
            final long dealId = dealRow.getPKValue();
            final Deal deal = new Deal();
            final Row monetaryRow = dataContainer.getRow(monetaryTable, (Long) dealRow.get(MDODEAL.MONETARY_ID));
            final Row pipelineRow = dataContainer.getRow(pipelineTable, (Long) dealRow.get(MDODEAL.PIPELINE_ID));
            final ProductDetailsHolder productDetailsHolder = getProductDetailsHolder(dealId, dataContainer);

            deal.setOrgId(dealRow.getOrgId());
            deal.setId(dealId);
            deal.setCreatedTime(dealRow.get(MDODEAL.CREATED_TIME));
            deal.setModifiedTime(dealRow.get(MDODEAL.MODIFIED_TIME));
            deal.setTitle(dealRow.get(MDODEAL.TITLE));
            deal.setState(State.getStateById(dealRow.get(MDODEAL.STATE)));
            deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
            deal.setClosedTime(dealRow.get(MDODEAL.CLOSED_TIME));
            deal.setProductDetailsHolder(productDetailsHolder);

            if (Objects.nonNull(monetaryRow)) {
                final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);
                final DealValue dealValue = new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue());

                deal.setDealValue(dealValue);
            }

            if (Objects.nonNull(productDetailsHolder) && !productDetailsHolder.isManuallyAdded()) {
                deal.setProductAmount(deal.getDealValue());
            }

            if (Objects.nonNull(pipelineRow)) {
                deal.setPipeline(pipelineManager.convertRowToModel(pipelineRow, dataContainer, false));

                final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);
                final Collection<PipelineStage> pipelineStages = deal.getPipeline().getPipelineStages();

                if (Objects.nonNull(pipelineStages)) {
                    pipelineStages.stream().filter(pipelineStage -> Objects.equals(pipelineStageId, pipelineStage.id()))
                            .findFirst().ifPresent(deal::setPipelineStage);
                }
            }
            dealsMap.put(dealId, deal);
        });

        return dealsMap;
    }

    /**
     * <p>
     * Loads the required preview details for the deal summary
     * </p>
     *
     * @param orgId Represents the organisation ID
     * @param deal  Represents the {@link Deal}
     * @param needHoverCardDetails Refers whether the card details are required or not.
     */
    public void loadSummaryPreviewDetails(final Long orgId, final Deal deal, final boolean needHoverCardDetails) {
        final Contact primaryContact = deal.getPrimaryContact();

        if (Objects.nonNull(primaryContact)) {
            final Table contactTable = contactManager.getTable();
            final Table emailTable = emailManager.getTable();
            final Table phoneTable = phoneManager.getTable();
            final SelectQuery selectQuery = SelectQuery.get(contactTable);
            final JoinClause.JoinType leftJoin = LEFT;

            selectQuery.setWhereClause(new WhereClause(Criteria.eq(contactTable.getPKColumn(), primaryContact.id())));

            if (needHoverCardDetails) {
                final Table contactCompanyTable = contactManager.getContactCompanyTable();
                final Table companyTable = companyManager.getTable();
                final Table contactEmailTable = contactManager.getContactEmailTable();
                final Table contactPhoneTable = contactManager.getContactPhoneTable();
                final Column phonePkColumn = phoneTable.getPKColumn();
                final Column emailPkColumn = emailTable.getPKColumn();
                final String companyFieldKey = ContactFields.COMPANY;
                final String emailsFieldKey = ContactFields.EMAILS;
                final String phonesFieldKey = ContactFields.PHONES;
                final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Source.CONTACT.getName(), emailsFieldKey, phonesFieldKey, companyFieldKey);

                selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FACEBOOK), contactTable.getColumn(MCOCONTACT.LINKEDIN),
                        contactTable.getColumn(MCOCONTACT.TWITTER), contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL)));

                final DataContainer dataContainer = defaultFieldsDataLoader.fetchDefaultFieldsDataContainer(orgId, selectQuery, contactTable, fields.values(), List.of(primaryContact.id()));
                final Long contactId = primaryContact.id();

                final Row contactCompanyRow = dataContainer.getRow(contactCompanyTable, new WhereClause(Criteria.
                        eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), contactId)));
                final Collection<Long> emailIds = dataContainer.getRows(contactEmailTable, new WhereClause(Criteria.eq(contactEmailTable.getColumn(MCONEMAILFIELDS.ENTITY_ID), contactId)))
                        .map(contactEmailRow -> (Long) contactEmailRow.get(MCONEMAILFIELDS.EMAIL_ID)).collect(Collectors.toUnmodifiableSet());
                final Collection<Long> phoneIds = dataContainer.getRows(contactPhoneTable, new WhereClause(Criteria.eq(contactPhoneTable.getColumn(MCONPHONEFIELDS.ENTITY_ID), contactId)))
                        .map(contactPhoneRow -> (Long) contactPhoneRow.get(MCONPHONEFIELDS.PHONE_ID)).collect(Collectors.toUnmodifiableSet());
                final Row contactRow = dataContainer.getRow(contactTable);

                if (!emailIds.isEmpty()) {
                    final Long primaryEmailId = contactRow.get(MCOCONTACT.PRIMARY_EMAIL);

                    dataContainer.getRows(emailTable, new WhereClause(Criteria.in(emailPkColumn, emailIds)))
                            .forEach(emailRow -> {
                                final Email email = new Email();
                                final Long emailId = emailRow.getPKValue();

                                email.setId(emailId);
                                email.setValue(emailRow.get(OMETEMAIL.VALUE));
                                primaryContact.addEmail(email);

                                if (Objects.equals(primaryEmailId, emailId)) {
                                    primaryContact.setPrimaryEmail(email);
                                }
                            });
                }

                if (!phoneIds.isEmpty()) {
                    final Long primaryPhoneId = contactRow.get(MCOCONTACT.PRIMARY_PHONE);

                    dataContainer.getRows(phoneTable, new WhereClause(Criteria.in(phonePkColumn, phoneIds)))
                            .forEach(phoneRow -> {
                                final Phone phone = new Phone();
                                final Long phoneId = phoneRow.getPKValue();

                                phone.setId(phoneId);
                                phone.setNumber(phoneRow.get(OMETPHONE.NUMBER));
                                primaryContact.addPhone(phone);

                                if (Objects.equals(primaryPhoneId, phoneId)) {
                                    primaryContact.setPrimaryPhone(phone);
                                }
                            });
                }

                if (Objects.nonNull(contactCompanyRow)) {
                    final Row companyRow = dataContainer.getRow(companyTable, (Long) contactCompanyRow.get(MCONCOMPANYFIELDS.COMPANY_ID));

                    if (Objects.nonNull(companyRow)) {
                        primaryContact.setCompany(companyManager.convertRowToPartialModel(companyRow));
                    }
                }

                primaryContact.setFacebook(contactRow.get(MCOCONTACT.FACEBOOK));
                primaryContact.setLinkedIn(contactRow.get(MCOCONTACT.LINKEDIN));
                primaryContact.setTwitter(contactRow.get(MCOCONTACT.TWITTER));
            } else {
                selectQuery.addSelectClause(List.of(emailTable.getColumn(OMETEMAIL.TYPE), emailTable.getColumn(OMETEMAIL.VALUE),
                        phoneTable.getColumn(OMETPHONE.COUNTRY_ID), phoneTable.getColumn(OMETPHONE.NUMBER)));
                selectQuery.addJoinClause(List.of(new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn(), leftJoin),
                        new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn(), leftJoin)));

                final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
                final Row emailRow = dataContainer.getRow(emailTable);
                final Row phoneRow = dataContainer.getRow(phoneTable);

                if (Objects.nonNull(phoneRow)) {
                    primaryContact.setPrimaryPhone(phoneManager.convertRowToModel(phoneRow));
                }

                if (Objects.nonNull(emailRow)) {
                    primaryContact.setPrimaryEmail(emailManager.convertRowToModel(emailRow));
                }
            }
        }
    }

    /**
     * <p>
     * Gets the deal preview details to be loaded into the deal list
     * </p>
     *
     * @param orgId   Represents the organisation ID
     * @param dealIds Represents the collection of {@link Deal} ids
     * @return The {@link DataContainer}
     */
    public DataContainer getPreviewDetails(final Long orgId, final Collection<Long> dealIds) {
        final Table dealTable = getTable();
        final Table contactTable = contactManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table participantTable = getParticipantTable();

        final Column dealPkColumn = dealTable.getPKColumn();
        final Column participantContactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
        final Column participantDealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);

        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(List.of(contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME),
                contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getColumn(OMETEMAIL.TYPE), emailTable.getColumn(OMETEMAIL.VALUE),
                contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getColumn(OMETPHONE.COUNTRY_ID), phoneTable.getColumn(OMETPHONE.NUMBER),
                dealTable.getColumn(MDODEAL.CONTACT_ID), participantDealIdColumn, participantContactIdColumn));
        selectQuery.addJoinClause(List.of(
                new JoinClause(dealPkColumn, participantDealIdColumn),
                new JoinClause(participantContactIdColumn, contactTable.getPKColumn()),
                new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn(), LEFT),
                new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE), phoneTable.getPKColumn(), LEFT)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealPkColumn, dealIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Loads the deal timeline row into the container
     * </p>
     *
     * @param dealId        Represent deal id
     * @param timeline      An entity that holds the timeline attributes
     * @param dataContainer Holds the updated information of user defined and system field values
     */
    public void loadDealTimelineRowIntoContainer(final Timeline timeline, final Long dealId, final DataContainer dataContainer) {
        commonTimelineManager.loadTimelineRowIntoContainer(timeline, new Row(commonTimelineManager.getTable()), dataContainer);
        commonTimelineManager.loadSourceTimelineRowIntoContainer(timeline.id(), dealId, MTLDEALTIMELINE.DEAL_ID, MTLDEALTIMELINE.TIMELINE_ID,
                new Row(commonTimelineManager.getDealTimelineTable()), dataContainer);
    }

    /**
     * <p>
     * Gets the deal stages information including pipeline id and order mapped for the {@link Collection} of deal id
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param dealIds Represents the {@link Collection} of deal id
     * @return the {Map} of deal stage info
     */
    public Map<Long, Pair<Long, Short>> getDealsStageInfo(final long orgId, final Collection<Long> dealIds) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Map<Long, Pair<Long, Short>> deals = new HashMap<>();
        final Column dealPkColumn = dealTable.getPKColumn();
        final Column pipelineStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column pipelinePkColumn = pipelineStageTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(pipelineStageIdColumn, pipelinePkColumn));
        selectQuery.addSelectClause(List.of(pipelineStageTable.getColumn(MDOPIPELINESTAGE.PIPELINE_ID), dealPkColumn,
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.ORDER), pipelineStageIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealPkColumn, dealIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(dealTable).forEach(row -> {
            final Row pipelineStageRow = dataContainer.getRow(pipelineStageTable,
                    new WhereClause(Criteria.eq(pipelinePkColumn, row.get(MDODEAL.PIPELINE_STAGE_ID))));

            deals.put(row.getPKValue(), Pair.of(pipelineStageRow.get(MDOPIPELINESTAGE.PIPELINE_ID),
                    pipelineStageRow.get(MDOPIPELINESTAGE.ORDER)));
        });

        return deals;
    }

    /**
     * <p>
     * Fetches the collection of {@link Deal} for the given product id
     * </p>
     *
     * @param orgId     Represents org id
     * @param productId Represent product id
     * @return The collection of {@link Deal}
     */
    public Collection<Deal> getDealsByProduct(final long orgId, final Long productId) {
        final Table dealTable = getTable();
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Table productDetailsTable = productDetailsHolderManager.getProductDetailTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Collection<Deal> deals = new HashSet<>();

        selectQuery.addSelectClause(dealTable.getColumn(MDODEAL.TITLE));
        selectQuery.addJoinClause(List.of(new JoinClause(dealTable.getPKColumn(), productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID)),
                new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID))));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), productId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(dealTable).forEach(row -> {
            final Deal deal = new Deal();

            deal.setTitle(row.get(MDODEAL.TITLE));
            deal.setId(row.getPKValue());
            deals.add(deal);
        });

        return deals;
    }

    /**
     * <p>
     * Gets the deal profile details
     * </p>
     *
     * @param orgId  Represents the organisation Id
     * @param dealId Represents the deal Id
     * @return The {@link Deal}
     */
    public Deal getProfile(final Long orgId, final Long dealId) {
        final Table dealTable = getTable();
        final Table contactTable = contactManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table pipelineTable = pipelineManager.getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
        final Column dealPkColumn = dealTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final JoinClause.JoinType leftJoin = LEFT;

        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.ORG_ID), dealTable.getColumn(MDODEAL.TITLE), dealTable.getColumn(MDODEAL.ORG_ID),
                dealTable.getColumn(MDODEAL.STATE), dealTable.getColumn(MDODEAL.CONTACT_ID), dealTable.getColumn(MCOCOMPANY.CREATED_TIME),
                dealTable.getColumn(MCOCOMPANY.MODIFIED_TIME), dealTable.getColumn(MDODEAL.MONETARY_ID), dealTable.getColumn(MDODEAL.STATUS),
                contactTable.getColumn(MCOCONTACT.FIRST_NAME), contactTable.getColumn(MCOCONTACT.LAST_NAME), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID), dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID),
                dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyTable.getColumn(MCOCOMPANY.NAME), pipelineTable.getColumn(MDOPIPELINE.ROTTEN_DAYS),
                monetaryTable.getColumn(OMETMONETARY.ORG_ID), monetaryTable.getColumn(OMETMONETARY.VALUE), monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID)));
        selectQuery.addSelectClause(productDetailsHolderTable);
        selectQuery.addJoinClause(List.of(new JoinClause(dealTable.getColumn(MDODEAL.CONTACT_ID), contactTable.getPKColumn(), leftJoin),
                new JoinClause(dealPkColumn, dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(dealCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyTable.getPKColumn(), leftJoin),
                new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryTable.getColumn(OMETMONETARY.ID), leftJoin),
                new JoinClause(dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineTable.getPKColumn(), leftJoin),
                new JoinClause(dealPkColumn, productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealPkColumn, dealId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row dealRow = dataContainer.getRow(dealTable);

        if (Objects.isNull(dealRow)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
        return convertRowToProfile(dealRow, dataContainer);
    }

    /**
     * Fetches the deals and grouped by entity ID for the given organization and source.
     *
     * @param orgId     The organization ID to filter the deals.
     * @param entityIds The collection of entity IDs for which deals are to be fetched.
     * @param source    The source type used to filter deals.
     * @return A map where each key is an entity ID and the value is the collection of corresponding deals.
     */
    public Map<Long, Collection<Deal>> fetchDealsBySource(final long orgId, final Collection<Long> entityIds, final Source source) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column dealPkColumn = dealTable.getPKColumn();
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column stageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final Column stagePkColumn = pipelineStageTable.getPKColumn();

        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), monetaryIdColumn, stageIdColumn,
                dealTable.getColumn(MDODEAL.STATE), monetaryTable.getColumn(OMETMONETARY.VALUE), monetaryTable.getColumn(OMETMONETARY.ORG_ID),
                monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID), pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME)));
        selectQuery.addJoinClause(List.of(new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()),
                new JoinClause(stageIdColumn, stagePkColumn)));

        return switch (source) {
            case CONTACT -> {
                final Table participantTable = getParticipantTable();
                final Column contactIdColumn = participantTable.getColumn(MDOPARTICIPANT.CONTACT_ID);
                final Column participantDealIdColumn = participantTable.getColumn(MDOPARTICIPANT.DEAL_ID);

                selectQuery.addSelectClause(List.of(contactIdColumn, participantDealIdColumn));
                selectQuery.addJoinClause(new JoinClause(dealPkColumn, participantDealIdColumn));
                selectQuery.setWhereClause(new WhereClause(Criteria.in(contactIdColumn, entityIds)));
                final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

                yield convertDeals(dataContainer, entityIds, dealTable, participantTable, source);
            }

            case COMPANY -> {
                final String companyKey = DealFields.COMPANY;
                final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);
                final Long companyFieldId = fields.get(companyKey).id();
                final Table dealCompanyTable = getDealCompanyTable();
                final Column dealCompanyIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.COMPANY_ID);
                final Column companyDealIdColumn = dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID);

                selectQuery.addSelectClause(List.of(dealCompanyIdColumn, companyDealIdColumn));
                selectQuery.addJoinClause(new JoinClause(dealPkColumn, companyDealIdColumn));
                selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), companyFieldId))
                        .and(Criteria.in(dealCompanyIdColumn, entityIds)));
                final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

                yield convertDeals(dataContainer, entityIds, dealTable, dealCompanyTable, source);
            }
            default -> Collections.emptyMap();
        };
    }

    /**
     * Converts the deal row data from the given {@code DataContainer} into a map of entity IDs to their associated deals.
     *
     * @param dataContainer A container which contains the deal rows
     * @param entityIds     Which contains the collection of entity ids
     * @param dealTable     Represents the deal table
     * @param sourceTable   Represents the source table
     * @param source        Represents the {@link Source}
     * @return A {@link Map} where the key is the entity id and the value is a collection of deals associated with that entity id
     */
    private Map<Long, Collection<Deal>> convertDeals(final DataContainer dataContainer, final Collection<Long> entityIds,
                                                     final Table dealTable, final Table sourceTable, final Source source) {

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table pipelineStageTable = pipelineStageManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Column dealPKColumn = dealTable.getPKColumn();
        final Map<Long, Collection<Deal>> entityDealMap = new HashMap<>();
        String entityIdColumn = null;
        String dealIdColumn = null;

        switch (source) {

            case CONTACT -> {
                entityIdColumn = MDOPARTICIPANT.CONTACT_ID;
                dealIdColumn = MDOPARTICIPANT.DEAL_ID;
            }
            case COMPANY -> {
                entityIdColumn = MDOCOMPANYFIELDS.COMPANY_ID;
                dealIdColumn = MDOCOMPANYFIELDS.ENTITY_ID;
            }
        }

        for (final Long entityId : entityIds) {
            final String sourceDealIdColumn = dealIdColumn;
            final Set<Long> dealIds = dataContainer.getRows(sourceTable,
                            new WhereClause(Criteria.eq(sourceTable.getColumn(entityIdColumn), entityId)))
                    .map(row -> (Long) row.get(sourceDealIdColumn)).collect(Collectors.toUnmodifiableSet());
            final Stream<Row> dealRows = dataContainer.getRows(dealTable, new WhereClause(Criteria.in(dealPKColumn, dealIds)));
            final Collection<Deal> deals = new ArrayList<>();

            dealRows.forEach(dealRow -> {
                final Deal deal = new Deal();
                final Long pipelineStageId = dealRow.get(MDODEAL.PIPELINE_STAGE_ID);
                final Row pipelineStageRow = dataContainer.getRow(pipelineStageTable, pipelineStageId);
                final Long monetaryId = dealRow.get(MDODEAL.MONETARY_ID);
                final Row monetaryRow = dataContainer.getRow(monetaryTable, monetaryId);

                deal.setId(dealRow.getPKValue());
                deal.setOrgId(dealRow.getOrgId());
                deal.setTitle(dealRow.get(MDODEAL.TITLE));
                deal.setState(State.getStateById(dealRow.get(MDODEAL.STATE)));

                if (Objects.nonNull(pipelineStageRow)) {
                    deal.setPipelineStage(pipelineStageManager.convertPartialRowToModel(pipelineStageRow));
                }

                if (Objects.nonNull(monetaryRow)) {
                    final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);

                    deal.setDealValue(new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue()));
                }
                deals.add(deal);
            });
            entityDealMap.put(entityId, deals);
        }

        return entityDealMap;
    }

    /**
     * Converts row to {@link Deal} profile
     *
     * @param dealRow Represents deal row
     * @return The {@link Deal}
     */
    private Deal convertRowToProfile(final Row dealRow, final DataContainer dataContainer) {
        final Deal deal = new Deal();
        final Long dealId = dealRow.getPKValue();
        final Table contactTable = contactManager.getTable();
        final Table dealCompanyTable = getDealCompanyTable();
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        final long currentTime = Instant.now().toEpochMilli();
        final DateTime modifiedTime = dealRow.get(MDODEAL.MODIFIED_TIME);
        final DateTime createdTime = dealRow.get(MDODEAL.CREATED_TIME);
        final Row monetaryRow = dataContainer.getRow(monetaryManager.getTable(), (Long) dealRow.get(MDODEAL.MONETARY_ID));
        final Long orgId = dealRow.getOrgId();

        deal.setId(dealId);
        deal.setOrgId(orgId);
        deal.setTitle(dealRow.get(MDODEAL.TITLE));
        deal.setInactiveDays(timeUnit.toDays(currentTime - modifiedTime.getTimeStamp()));
        deal.setAge(timeUnit.toDays(currentTime - createdTime.getTimeStamp()));
        deal.setStatus(dealRow.get(MDODEAL.STATUS));
        deal.setProductDetailsHolder(getProductDetailsHolder(dealId, dataContainer));
        final Integer state = dealRow.get(MDODEAL.STATE);

        if (Objects.nonNull(state)) {
            deal.setState(State.getStateById(state));
        }
        final Row pipelineRow = dataContainer.getRow(pipelineManager.getTable());

        if (Objects.nonNull(pipelineRow)) {
            final long timeDifference = timeUnit.toDays(currentTime - dateTimeUtil.getTimeStamp(modifiedTime));
            final Short defaultRottingDays = pipelineRow.get(MDOPIPELINE.ROTTEN_DAYS);

            deal.setRottenDays(timeDifference > defaultRottingDays ? timeDifference - defaultRottingDays : 0);
        }

        if (Objects.nonNull(monetaryRow)) {
            final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);
            final DealValue dealValue = new DealValue(monetary.orgId(), monetary.id(), monetary.getCurrency(), monetary.getValue());

            deal.setDealValue(dealValue);
        }
        final Long primaryContactId = dealRow.get(MDODEAL.CONTACT_ID);
        final Row contactRow = Objects.isNull(primaryContactId) ? null : dataContainer.getRow(contactTable,new WhereClause(Criteria.eq(contactTable.getPKColumn(), primaryContactId)));

        if (Objects.nonNull(contactRow)) {
            deal.setPrimaryContact(contactManager.convertRowToPartialModel(contactRow));
        }
        final String companyKey = DealFields.COMPANY;
        final Map<String, FieldInfo> fields = secureFieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, companyKey);

        if (fields.containsKey(companyKey)) {
            final Row dealCompanyRow = dataContainer.getRow(dealCompanyTable,
                    new WhereClause(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.ENTITY_ID), dealId))
                            .and(Criteria.eq(dealCompanyTable.getColumn(MDOCOMPANYFIELDS.FIELD_ID), fields.get(companyKey).id())));

            if (Objects.nonNull(dealCompanyRow)) {
                deal.setCompany(convertRowToCompany(orgId, dealCompanyRow, dataContainer));
            }
        }
        return deal;
    }

    /**
     * <p>
     * Gets the activity deal mapping table
     * </p>
     *
     * @return The activity deal mapping {@link Table}
     */
    @Override
    public Table getActivityEntityTable() {
        return Table.get(MACDEALACTIVITY.TABLE);
    }

    /**
     * <p>
     * Gets the primary contact id of the deal
     * </p>
     *
     * @param orgId  Represents the organisation Id
     * @param dealId Represents the deal id
     * @return The primary contact id of the deal
     */
    public Long getPrimaryContactId(final Long orgId, final Long dealId) {
        final Table dealTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(dealTable, new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)),
                List.of(dealTable.getColumn(MDODEAL.CONTACT_ID)));
        final Row row = dataContainer.getRow(dealTable);

        return Objects.isNull(row) ? null : row.get(MDODEAL.CONTACT_ID);
    }

    /**
     * <p>
     * Updates the primary contact of the deal
     * </p>
     *
     * @param orgId            Represents the organisation Id
     * @param dealId           Represents the deal id
     * @param primaryContactId The primary contact id of the deal
     */
    public void updatePrimaryContact(final Long orgId, final Long dealId, final Long userId, final Long primaryContactId) {
        final Table dealTable = getTable();
        final Long oldPrimaryContactId = getPrimaryContactId(orgId, dealId);
        final UpdateQuery updateQuery = UpdateQuery.get(dealTable);

        updateQuery.addUpdateColumn(dealTable.getColumn(MDODEAL.CONTACT_ID), primaryContactId);
        updateQuery.addUpdateColumn(dealTable.getColumn(MDODEAL.MODIFIED_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        orgDataStore(orgId).update(updateQuery);
        final DataContainer targetContainer = DataContainer.create();

        fieldAuditHandler.loadFieldAuditEntry(orgId, Source.DEAL, dealId, userId,
                FieldAuditInfo.defaultContext(), DealFields.PRIMARY_CONTACT,
                contactManager.getName(orgId, oldPrimaryContactId),
                contactManager.getName(orgId, primaryContactId), targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
        final Deal deal = new Deal();

        deal.setOrgId(orgId);
        deal.setId(dealId);
        deal.setPrimaryContact(contactManager.get(orgId, primaryContactId));
        IndexDataPublisher.getInstance().publishIndexData(orgId, List.of(deal), Action.PARTIAL_UPDATE);
    }

    /**
     * <p>
     * Updates the deal archive state
     * </p>
     *
     * @param orgId      Represents the organisation Id
     * @param userId     Represents the user Id
     * @param dealId     Represents the deal Id
     * @param isArchived Represents the archive state
     */
    public void updateArchiveState(final Long orgId, final long userId, final Long dealId, final boolean isArchived) {
        final Table dealTable = getTable();
        final DataContainer dataContainer = fetchPartialDealContainer(orgId, List.of(dealId));
        final Row dealRow = dataContainer.getRow(dealTable, dealId);
        final DataContainer targetContainer = DataContainer.create();

        dealRow.set(dealTable.getColumn(MDODEAL.STATUS), isArchived ? Deal.Status.ARCHIVED.getId() : Deal.Status.ACTIVE.getId());
        dealRow.set(dealTable.getColumn(MDODEAL.MODIFIED_TIME), Instant.now().toEpochMilli());

        targetContainer.updateRow(dealRow);
        final Deal deal = convertRowToPartialModel(dealRow);

        loadDealTimelineRowIntoContainer(buildArchiveTimeline(orgId, userId, isArchived), dealId, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
        auditLogger.log(orgId, List.of(new AuditLogData(userId, dealId, deal.getTitle(), AuditSource.DEAL, isArchived ? AuditAction.ARCHIVED : AuditAction.UNARCHIVED, null)));
        DealIndexManager.getInstance().updateDealArchive(orgId, dealId, isArchived);
    }

    /**
     * <p>
     * Builds the {@link Timeline} for archived deal
     * </p>
     *
     * @param orgId      Represents the organisation Id
     * @param userId     Represents the user Id
     * @param isArchived Represents the archive state
     * @return The {@link Timeline}
     */
    private Timeline buildArchiveTimeline(final Long orgId, final Long userId, final boolean isArchived) {
        final Timeline timeline = new Timeline();

        timeline.setTimelineType(isArchived ? TimelineType.DEAL_ARCHIVED : TimelineType.DEAL_UNARCHIVED);
        timeline.setAction(isArchived ? TimelineActionFields.ARCHIVED : TimelineActionFields.UNARCHIVED);
        timeline.setOrgId(orgId);
        timeline.setOwnerId(userId);
        timeline.setAuditedTime(new DateTime(Instant.now().toEpochMilli()));
        timeline.setIconType(TimelineFields.ARCHIVE.getName());
        timeline.setCategory(Timeline.Category.DEAL);

        return timeline;
    }

    /**
     * <p>
     * Extracts the archived deal ids from the given deal ids
     * </p>
     *
     * @param orgId    Represents the organisation Id
     * @param dealIds  Represents the collection of deal ids
     * @return The archived deal ids
     */
    public Collection<Long> extractArchivedDealIds(final Long orgId, final Collection<Long> dealIds) {
        final Table dealTable = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(dealTable, new WhereClause(Criteria.in(dealTable.getPKColumn(), dealIds)),
                List.of(dealTable.getColumn(MDODEAL.STATUS)));

        return dataContainer.getRows(dealTable).filter(row -> Objects.equals(Deal.Status.ARCHIVED.getId(), row.get(MDODEAL.STATUS)))
                .map(Row::getPKValue).toList();
    }

    /**
     * <p>
     * Fetches the deal associated with the given contact within the specified organization,
     * but only if the contact is mapped to exactly one deal
     * </p>
     *
     * @param orgId     Represent an organization id
     * @param contactId Represents the contact id
     * @return An {@link Optional} containing the {@link Deal} if the contact has exactly one deal;
     * otherwise {@link Optional} empty
     */
    public Optional<Deal> findSingleDealByContact(final long orgId, final long contactId) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final SelectQuery derivedQuery = SelectQuery.get(dealTable);
        final Column contactIdColumn = dealTable.getColumn(MDODEAL.CONTACT_ID);
        final Column dealOrgIdColumn = dealTable.getColumn(MDODEAL.ORG_ID);

        derivedQuery.addSelectClause(List.of(contactIdColumn, dealOrgIdColumn));
        derivedQuery.setWhereClause(new WhereClause(Criteria.eq(contactIdColumn, contactId)));
        derivedQuery.addGroupByClause(List.of(new GroupByClause(contactIdColumn), new GroupByClause(dealOrgIdColumn)));
        derivedQuery.addHavingClause(new HavingClause(Criteria.eq(contactIdColumn.count(), 1)));
        final DerivedTable dealDerivedTable = DerivedTable.get(dealTable.getName(), String.format("%s%d", dealTable.getAlias(), contactId), derivedQuery);

        selectQuery.addSelectClause(dealTable.getPKColumn());
        selectQuery.addJoinClause(new JoinClause(contactIdColumn, dealDerivedTable.getColumn(MDODEAL.CONTACT_ID)));
        final Row dealRow = orgDataStore(orgId).get(selectQuery).getRow(dealTable);

        if (Objects.nonNull(dealRow)) {
            final Deal deal = new Deal();

            deal.setId(dealRow.getPKValue());
            return Optional.of(deal);
        }
        return Optional.empty();
    }

    /**
     * <p>
     * Fetches the collection of {@link Deal} non commited forecast category for given org id
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represents the {@link DropdownCriteria}
     * @param dropdownLimit    Represents the {@link DropdownLimit}
     * @param sortConfig       Represents the {@link SortConfig}
     * @return The collection of {@link Deal}
     */
    public Collection<Deal> getForecastCategoryDeals(final long orgId, final DropdownCriteria dropdownCriteria,
                                                     final DropdownLimit dropdownLimit, final com.tlc.crm.common.models.SortConfig sortConfig) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Table forecastCategoryTable = forecastCategoryManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);

        selectQuery.addSelectClause(List.of(dealTable.getColumn(MDODEAL.TITLE), monetaryIdColumn,  dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), dealTable.getColumn(MDODEAL.OWNER_ID)));
        selectQuery.addSelectClause(monetaryTable);
        selectQuery.addJoinClause(List.of(new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()),
                new JoinClause(dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID), forecastCategoryTable.getPKColumn(), LEFT)));
        selectQuery.setWhereClause(buildNonCommittedCategoryDealCriteria(dropdownCriteria));
        selectQuery.setLimitClause(dropdownLimit.getLimitClause());

        if (sortConfig.fieldName().equals(DealFields.TITLE)) {
            selectQuery.addOrderByClause(new OrderByClause(dealTable.getColumn(MDODEAL.TITLE), sortConfig.type().getOrderType()));
        }
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(dealTable).map(dealRow -> {
            final Deal deal = new Deal();

            deal.setTitle(dealRow.get(MDODEAL.TITLE));
            deal.setExpectedCloseDate(dealRow.get(MDODEAL.EXPECTED_CLOSE_DATE));
            deal.setId(dealRow.getPKValue());
            deal.setSalesOwner(userService.getUser(orgId, dealRow.get(MDODEAL.OWNER_ID)));
            deal.setDealValue(new DealValue(monetaryManager.convertRowToModel(dataContainer.getRow(monetaryTable, (Long) dealRow.get(MDODEAL.MONETARY_ID)))));

            return deal;
        }).toList();
    }

    /**
     * <p>
     * Fetches the counts of {@link Deal} non commited forecast category for given org id
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represents the {@link DropdownCriteria}
     * @return The total records of non commited forecast category
     */
    public long getForecastCategoryDealCount(final long orgId, final DropdownCriteria dropdownCriteria) {
        final Table dealTable = getTable();
        final CountQuery countQuery = CountQuery.get(dealTable);

        countQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID), forecastCategoryManager.getTable().getPKColumn(), LEFT));
        countQuery.setWhereClause(buildNonCommittedCategoryDealCriteria(dropdownCriteria));
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Updates deal forecast category based on {@link Deal}
     *
     * @param deal Represents the {@link Deal}
     */
    public void updateForecastCategory(final Deal deal) {
        final long orgId = deal.orgId();
        final Table dealTable = getTable();
        final Long id = deal.id();
        final ForecastCategory forecastCategory = deal.getForecastCategory();
        final Date expectedCloseDate = deal.getExpectedCloseDate();
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        WhereClause whereClause = new WhereClause(Criteria.eq(dealTable.getPKColumn(), id));

        if (Objects.isNull(expectedCloseDate)) {
            whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.notEq(dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), null)), Connector.AND);
        }
        final DataContainer entityContainer = orgDataStore.get(dealTable, whereClause);
        final Row dealRow = entityContainer.getRow(dealTable);
        final DataContainer targetContainer = DataContainer.create();

        if (Objects.nonNull(dealRow)) {
            final Map<String, String> oldDealValues = new HashMap<>();
            final Long committedForecastCategoryId;
            final Long expectedCloseDateTimeStamp;

            if (Objects.nonNull(forecastCategory) && forecastCategory.isCommittedCategory()) {
                deal.setForecastCategory(null);
                loadOldDealValues(deal, oldDealValues, dealRow, entityContainer);
                loadDealTimeline(deal, false, oldDealValues, dealRow, entityContainer, targetContainer);

                committedForecastCategoryId = null;
                expectedCloseDateTimeStamp = null;
            } else {
                final Optional<Long> optionalForeCastCategoryId =
                        Optional.ofNullable(forecastCategory).map(ForecastCategory::id)
                                .or(() -> forecastCategoryManager.getCommitedCategoryId(orgId));

                if (optionalForeCastCategoryId.isEmpty()) {
                    return;
                }
                final ForecastCategory category = new ForecastCategory();

                committedForecastCategoryId = optionalForeCastCategoryId.get();
                category.setId(committedForecastCategoryId);
                deal.setForecastCategory(category);
                loadOldDealValues(deal, oldDealValues, dealRow, entityContainer);
                loadDealTimeline(deal, false, oldDealValues, dealRow, entityContainer, targetContainer);

                expectedCloseDateTimeStamp = Objects.isNull(expectedCloseDate) ? null : expectedCloseDate.getTimeStamp();
            }

            final UpdateQuery updateQuery = UpdateQuery.get(dealTable);

            updateQuery.addUpdateColumn(dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID), committedForecastCategoryId);

            if (Objects.nonNull(expectedCloseDateTimeStamp) || (Objects.nonNull(forecastCategory) && forecastCategory.isCommittedCategory())) {
                updateQuery.addUpdateColumn(dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), expectedCloseDateTimeStamp);
            }
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), id)));

            DataStoreProvider.getSecureOrgDataStore(orgId).update(updateQuery);
            orgDataStore.commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Builds a {@link WhereClause} to non-committed deals
     * </p>
     *
     * @param dropdownCriteria Represents the {@link DropdownCriteria}
     * @return {@link WhereClause} containing all non-committed deal conditions
     */
    private WhereClause buildNonCommittedCategoryDealCriteria(final DropdownCriteria dropdownCriteria) {
        final Table dealTable = getTable();

        return new WhereClause(Criteria.notEq(dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), null))
                .and(dropdownCriteria.build(dealTable.getColumn(MDODEAL.TITLE))).and(new WhereClause(Criteria.notEq(forecastCategoryManager.getTable().getColumn(OMQFFORECASTCATEGORY.NAME),
                        DefaultForecastCategory.COMMITTED.getName())).or(Criteria.eq(dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID), null)));
    }

    /**
     * <p>
     * Gets the total number of deals by applying the given criteria list.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param criteriaList Represents the collection of {@link FilterCriteria}
     * @return The total number of deals.
     */
    public Long getDealCount(final Long orgId, final Collection<FilterCriteria> criteriaList) {
        final Table dealTable = getTable();
        final CountQuery countQuery = CountQuery.get(dealTable);

        filterCriteriaHandler.applyCriteria(orgId, Provider.DEAL_LIST, countQuery, criteriaList);

        return DataStoreProvider.getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * Returns the number of deals linked to a contact in the given organization.
     *
     * @param orgId the organization ID
     * @param contactId the contact ID
     * @return count of linked deals
     */
    public long getDealCountByContact(final Long orgId, final Long contactId) {
        final Table dealTable = getTable();
        final CountQuery countQuery = CountQuery.get(dealTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.CONTACT_ID), contactId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the total number of deals and the total monetary value by applying the given criteria list
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param criteriaList Represents the collection of {@link FilterCriteria}
     * @return The total number of deals and the total monetary value
     */
    public Pair<Long, Monetary> getTotalDealCountAndValue(final Long orgId, final Collection<FilterCriteria> criteriaList) {
        final Table dealTable = getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column currencyIdColumn = monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID);

        selectQuery.addSelectClause(List.of(currencyIdColumn, monetaryTable.getColumn(OMETMONETARY.VALUE, TOTAL_VALUE).sum(),
                dealTable.getColumn(MDODEAL.ID, DEAL_COUNT).countWithDistinct()));
        selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryTable.getPKColumn()));
        selectQuery.addGroupByClause(new GroupByClause(currencyIdColumn));
        filterCriteriaHandler.applyCriteria(orgId, Provider.DEAL_LIST, selectQuery, criteriaList);
        final WhereClause activeCriteria = new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.STATUS), Deal.Status.ACTIVE.getId()));
        final WhereClause existingWhereClause = selectQuery.getWhereClause();

        selectQuery.setWhereClause(Objects.isNull(existingWhereClause) ? activeCriteria : WhereClauseUtil.connect(existingWhereClause, activeCriteria, Connector.AND));
        final List<Map<String, Object>> container = orgDataStore(orgId).getData(selectQuery);
        final Currency defaultCurrency = baseCurrencyService.getDefaultCurrency(orgId);
        final short defaultCurrencyId = defaultCurrency.getId();

        BigDecimal totalValues = BigDecimal.ZERO;
        long totalCount = 0;

        for (final Map<String, Object> row : container) {
            final short currencyId = (short) row.get(OMETMONETARY.CURRENCY_ID);
            final BigDecimal bigDecimalValue = Optional.ofNullable((Number) row.get(TOTAL_VALUE))
                    .map(value -> BigDecimal.valueOf(value.doubleValue()))
                    .orElseGet(() -> BigDecimal.ZERO);
            totalCount += (Long) row.get(DEAL_COUNT);

            if (!Objects.equals(defaultCurrencyId, currencyId)) {
                totalValues = totalValues.add(bigDecimalValue.divide(baseCurrencyService.getExchangeRate(defaultCurrency,
                        Currency.getCurrencyById(currencyId)), NumericalConstant.FIVE, RoundingMode.HALF_EVEN));
            } else {
                totalValues = totalValues.add(bigDecimalValue);
            }
        }
        final Monetary monetary = new Monetary();

        monetary.setValue(baseCurrencyService.applyPrecision(orgId, totalValues));
        monetary.setCurrency(defaultCurrency);

        return Pair.of(totalCount, monetary);
    }
    
    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the org id
     * @param dealId Represents the deal id
     * @return the user ids
     */
    @Override
    public Collection<Long> getRestrictedIamUserIdsForEntity(final Long orgId, final Long dealId) {
        final Table userTable = Table.get(OMCRMUSER.TABLE);
        final Table dealTable = getTable();
        final Column userTablePkColumn = userTable.getPKColumn();
        final Column iamUserIdColumn = userTable.getColumn(OMCRMUSER.IAM_USER_ID);
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final Collection<Long> userIdsWithAccess = getUserIdsWithEntityAccess(orgId, dealId, dealTable, dealTable.getColumn(MDODEAL.TERRITORY_ID), SubCategory.Type.DEALS);

        userIdsWithAccess.addAll(sourceFollowersManager.getFollowerIdsByEntityId(orgId, dealId, Source.DEAL));

        selectQuery.addSelectClause(List.of(userTablePkColumn, iamUserIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.notIn(userTablePkColumn, userIdsWithAccess)));

        return orgDataStore(orgId).get(selectQuery).getRows(userTable).map(userRow -> (Long) userRow.get(OMCRMUSER.IAM_USER_ID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Fetch deal IDs matching the specified criteria (owner, time range, pipeline, territory, products).
     *
     * @param orgId        Organization ID
     * @param userIds      Collection of owner user IDs to filter
     * @param startTime    Quota period start time (milliseconds)
     * @param endTime      Quota period end time (milliseconds)
     * @param pipelineIds  Pipeline IDs to filter (nullable - no filter if null/empty)
     * @param territoryIds Territory IDs to filter (nullable - no filter if null/empty)
     * @param productIds   Product IDs to filter (nullable - no filter if null/empty)
     * @return Collection of deal IDs matching all criteria
     */
    public Collection<Long> getDealIds(final Long orgId, final Collection<Long> userIds, final Long startTime,
                                       final Long endTime, final Collection<Long> pipelineIds,
                                       final Collection<Long> territoryIds, final Collection<Long> productIds, final Collection<Long> excludedDealIds) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column ownerIdColumn = dealTable.getColumn(MDODEAL.OWNER_ID);
        final Column stateColumn = dealTable.getColumn(MDODEAL.STATE);
        final int wonId = State.WON.getId();
        final WhereClause ownerIdCriteria = new WhereClause(Criteria.in(ownerIdColumn, userIds));
        final WhereClause wonCriteria = ownerIdCriteria.and(Criteria.eq(stateColumn, wonId))
                .and(Criteria.between(dealTable.getColumn(MDODEAL.CLOSED_TIME), startTime, endTime));
        final WhereClause notWonCriteria = ownerIdCriteria.and(Criteria.notEq(stateColumn, wonId))
                .and(Criteria.between(dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), startTime, endTime));
        WhereClause baseCriteria = wonCriteria.or(notWonCriteria);

        if (!pipelineIds.isEmpty()) {
            baseCriteria = baseCriteria.and(Criteria.in(dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineIds));
        }

        if (!territoryIds.isEmpty()) {
            baseCriteria = baseCriteria.and(Criteria.in(dealTable.getColumn(MDODEAL.TERRITORY_ID), territoryIds));
        }

        if (!productIds.isEmpty()) {
            final Table productDetailsHolderTable = productDetailsHolderManager.getTable();
            final Table productDetailsTable = productDetailsHolderManager.getProductDetailTable();
            final Table productTable = productManager.getTable();
            final Column productIdColumn = productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID);

            selectQuery.addJoinClause(new JoinClause(dealTable.getPKColumn(), productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID), LEFT));
            selectQuery.addJoinClause(new JoinClause(productDetailsHolderTable.getPKColumn(), productDetailsTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID), LEFT));
            selectQuery.addJoinClause(new JoinClause(productIdColumn, productTable.getPKColumn(), LEFT));

            baseCriteria = baseCriteria.and(Criteria.in(productIdColumn, productIds));
        }

        if (!excludedDealIds.isEmpty()) {
            baseCriteria = baseCriteria.and(Criteria.notIn(dealTable.getPKColumn(), excludedDealIds));
        }
        selectQuery.addSelectClause(dealTable.getPKColumn());
        selectQuery.setWhereClause(baseCriteria);

        return DataStoreProvider.getOrgDataStore(orgId).get(selectQuery).getRows(dealTable)
                .map(Row::getPKValue).collect(Collectors.toSet());
    }

    /**
     * Gets the deals by their IDs and converts their values into the organization's base currency.
     *
     * @param orgId the ID of the organization
     * @param ids   the collection of deal IDs whose values need to be fetched
     * @return a collection of deals with their values converted to the base currency
     */
    public Collection<Deal> getDealValuesInBaseCurrency(final long orgId, final Collection<Long> ids) {
        final Table dealTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Table monetaryTable = monetaryManager.getTable();
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);

        selectQuery.addSelectClause(monetaryTable);
        selectQuery.addSelectClause(monetaryIdColumn);
        selectQuery.addJoinClause(new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getPKColumn(), ids)));
        final DataContainer dataContainer = DataStoreProvider.getOrgDataStore(orgId).get(selectQuery);
        final Currency defaultCurrency = baseCurrencyService.getDefaultCurrency(orgId);

        return dataContainer.getRows(dealTable).map(dealRow -> {
            final Deal deal = new Deal();
            final Long monetaryId = dealRow.get(MDODEAL.MONETARY_ID);
            final Row monetaryRow = dataContainer.getRow(monetaryTable, monetaryId);

            deal.setId(dealRow.getPKValue());

            if (Objects.nonNull(monetaryRow)) {
                final Monetary baseCurrencyValue = new Monetary();
                final BigDecimal bigDecimalValue = Optional.ofNullable((Number) monetaryRow.get(OMETMONETARY.VALUE))
                        .map(value -> BigDecimal.valueOf(value.doubleValue()))
                        .orElseGet(() -> BigDecimal.ZERO);
                final BigDecimal exchangeRate = baseCurrencyService.getCurrencyExchangeRate(orgId,
                        Currency.getCurrencyById(monetaryRow.get(OMETMONETARY.CURRENCY_ID)));

                baseCurrencyValue.setValue(Objects.isNull(exchangeRate) ? bigDecimalValue :
                        bigDecimalValue.divide(exchangeRate, NumericalConstant.FIVE, RoundingMode.HALF_EVEN));
                baseCurrencyValue.setCurrency(defaultCurrency);
                baseCurrencyValue.setOrgId(orgId);
                deal.setDealValueInBaseCurrency(baseCurrencyValue);
            }
            return deal;
        }).toList();
    }

    /**
     * <p>
     * Gets the {@link Currency} associated with the given deal.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param dealId Represents the deal id.
     * @return The {@link Currency} of the deal, or {@code null} if not found.
     */
    public Optional<Currency> getDealCurrency(final long orgId, final long dealId) {
        final Table dealTable = getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Column monetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(List.of(monetaryIdColumn, monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID)));
        selectQuery.addJoinClause(new JoinClause(monetaryIdColumn, monetaryTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row dealRow = dataContainer.getRow(dealTable);

        if (Objects.isNull(dealRow)) {
            return Optional.empty();
        }
        final Row monetaryRow = dataContainer.getRow(monetaryTable, (Long) dealRow.get(MDODEAL.MONETARY_ID));

        if (Objects.isNull(monetaryRow)) {
            return Optional.empty();
        }
        return Optional.of(Currency.getCurrencyById(monetaryRow.get(OMETMONETARY.CURRENCY_ID)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id
     * @param entityId  Represents the entity id
     * @return The entity name
     */
    @Override
    public String getName(final Long orgId, final Long entityId) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, entityId, List.of(table.getColumn(MDODEAL.TITLE)));

        return Objects.isNull(row) ? null : row.get(MDODEAL.TITLE);
    }

    /**
     * Gets the primary contact for the given deal ID with primary email.
     *
     * @param orgId  Represents an organization's ID
     * @param dealId Represents the deal ID
     * @return the optional of primary {@link Contact}
     */
    public Optional<Contact> getPrimaryContact(final Long orgId, final Long dealId) {
        final Table dealTable = getTable();
        final Table contactTable = contactManager.getTable();
        final Table emailTable = emailManager.getTable();
        final Column contactPkColumn = contactTable.getPKColumn();
        final Column emailPkColumn = emailTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addJoinClause(List.of(new JoinClause(dealTable.getColumn(MDODEAL.CONTACT_ID), contactPkColumn),
                new JoinClause(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), emailTable.getPKColumn(), JoinClause.JoinType.LEFT)));

        selectQuery.addSelectClause(List.of(contactPkColumn, contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME), contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL),
                emailTable.getColumn(OMETEMAIL.ID), emailTable.getColumn(OMETEMAIL.VALUE)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getPKColumn(), dealId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row contactRow = dataContainer.getRow(contactTable);

        if (Objects.nonNull(contactRow)) {
            final Contact contact = contactManager.convertRowToPartialModel(contactRow);
            final Row emailRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailPkColumn, contactRow.get(MCOCONTACT.PRIMARY_EMAIL))));

            if (Objects.nonNull(emailRow)) {
                contact.setPrimaryEmail(emailManager.convertRowToModel(emailRow));
            }

            return Optional.of(contact);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Loads the deal preview data such as title, monetary currency value, pipeline stage name into container
     * </p>
     *
     * @param orgId           Represents the organization ID
     * @param ids             Represents the collection of deal ids
     * @param dataContainer   Represents the dataContainer to which the deal data to be loaded
     */
    public void loadDealPreviewDataIntoContainer(final long orgId, final Collection<Long> ids,
                                                 final DataContainer dataContainer) {
        final Table dealTable = getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final Table pipelineStageTable = pipelineStageManager.getTable();

        final Column dealMonetaryIdColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final Column dealPipelineStageIdColumn = dealTable.getColumn(MDODEAL.PIPELINE_STAGE_ID);
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(List.of(
                dealTable.getColumn(MDODEAL.TITLE), dealMonetaryIdColumn, dealPipelineStageIdColumn,
                monetaryTable.getColumn(OMETMONETARY.CURRENCY_ID), monetaryTable.getColumn(OMETMONETARY.VALUE),
                monetaryTable.getColumn(OMETMONETARY.ORG_ID), monetaryTable.getColumn(MDOPIPELINESTAGE.ORG_ID),
                pipelineStageTable.getColumn(MDOPIPELINESTAGE.NAME))
        );
        selectQuery.addJoinClause(List.of(
                new JoinClause(dealMonetaryIdColumn, monetaryTable.getPKColumn(), LEFT),
                new JoinClause(dealPipelineStageIdColumn, pipelineStageTable.getPKColumn()))
        );
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealTable.getPKColumn(), ids)));

        dataContainer.append(orgDataStore(orgId).get(selectQuery));
    }

}
