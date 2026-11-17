package com.tlc.crm.core.features.action.quota.list;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.common.sql.resource.MCRMUSERTEAM;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.action.quota.list.handler.QuotaStructureHandler;
import com.tlc.crm.core.sql.resource.OMQFFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.filter.FilterCriteriaBuilder;
import com.tlc.crm.entity.list.filter.handler.FilterCriteriaHandler;
import com.tlc.crm.entity.list.filter.loader.impl.DefaultJoinLoader;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.WhereClauseBuilder;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Process the quota list actions in the quota and Provides the implementation for the {@link ListInterpreter}.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class QuotaSQLListInterpreter implements QuotaListInterpreter {

    protected static final Logger LOGGER = LoggerFactory.getLogger(QuotaSQLListInterpreter.class);
    private static final Collection<String> USER_TEAM_SELECTOR_FIELDS = Set.of(QuotaListFields.TEAM_IDS, QuotaListFields.USER_ID, QuotaListFields.USER_IDS);

    private static final Long NO_MATCH_ID = -1L;

    protected final ListStructure listStructure;
    protected final ListRequestData listRequestData;
    protected final Long userId;
    protected final Long orgId;
    protected final I18nResolutionAdapter i18nResolutionAdapter;
    protected final JsonObject criteriaJson;
    protected final SelectQuery derivedQuery;
    protected final Provider provider;
    protected final Table teamQuotaTable;
    protected final Table teamUserQuotaTable;
    protected final Table userQuotaTable;
    protected final FilterCriteriaBuilder filterCriteriaBuilder;
    protected final FilterCriteriaHandler filterCriteriaHandler;
    private final RolePermissionService rolePermissionService;

    protected SelectQuery selectQuery;
    protected Table primaryTable;

    private final QuotaStructureHandler quotaStructureHandler;

    protected QuotaSQLListInterpreter(final ListRequestData listRequestData, final Provider provider) {
        this.orgId = listRequestData.getOrgId();
        this.userId = listRequestData.getUserId();
        this.listRequestData = listRequestData;
        this.i18nResolutionAdapter = listRequestData.getI18nPlaceholderResolver();
        this.quotaStructureHandler = QuotaStructureHandler.get();
        this.listStructure = quotaStructureHandler.getStructure(userId, orgId, provider.name());
        this.provider = provider;
        this.criteriaJson = listRequestData.getRequestJson().optJsonObject(ListActionFields.CRITERIA);
        this.primaryTable = getTable();
        this.derivedQuery = SelectQuery.get(primaryTable);
        this.selectQuery = SelectQuery.get(primaryTable);
        this.teamQuotaTable = getTeamQuotaTable();
        this.teamUserQuotaTable = getTeamUserQuotaTable();
        this.userQuotaTable = getUserQuotaTable();
        this.filterCriteriaBuilder = FilterCriteriaBuilder.getInstance();
        this.filterCriteriaHandler = FilterCriteriaHandler.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
    }

    /**
     * <p>
     * Fetches the quota table
     * </p>
     *
     * @return The Quota table
     */
    protected Table getTable() {
        return Table.get(OMQFQUOTA.TABLE);
    }

    /**
     * <p>
     * Gets the user team table
     * </p>
     *
     * @return The user team {@link Table}
     */
    protected Table getUserTeamTable() {
        return Table.get(MCRMUSERTEAM.TABLE);
    }

    /**
     * <p>
     * Fetches the team quota table
     * </p>
     *
     * @return The team quota table
     */
    protected Table getTeamQuotaTable() {
        return Table.get(OMQFTEAMQUOTA.TABLE);
    }

    /**
     * <p>
     * Fetches the team user quota table
     * </p>
     *
     * @return The team user quota table
     */
    protected Table getTeamUserQuotaTable() {
        return Table.get(OMQFTEAMUSERQUOTA.TABLE);
    }

    /**
     * <p>
     * Fetches the user quota table
     * </p>
     *
     * @return The user quota table
     */
    protected Table getUserQuotaTable() {
        return Table.get(OMQFUSERQUOTA.TABLE);
    }

    /**
     * <p>
     * Returns an aliased version of the given table for the specified field.
     * This is typically used when the same table needs to be joined multiple times
     * with different contexts.
     * </p>
     *
     * @param table     The original table reference.
     * @param fieldName The field for which the alias is being created.
     * @return A {@code Table} with an alias applied based on the field name.
     */
    protected Table getAliasTable(final Table table, final String fieldName) {
        return Table.get(table.getName(), getAliasName(fieldName, table.getAlias()));
    }

    /**
     * <p>
     * Gets the alias name.
     * </p>
     *
     * @param fieldName    Represents the field name.
     * @param subFieldName Represents the sub-field name.
     * @return the alias name.
     */
    protected String getAliasName(final String fieldName, final String subFieldName) {
        return StringUtil.join(StringUtil.Delimiter.UNDER_SCORE, fieldName, subFieldName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultOrderBy() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultSelect() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyDefaultLimit() {
        derivedQuery.setLimitClause(new LimitClause(NumericalConstant.ONE, NumericalConstant.TEN));
    }

    /**
     * {@inheritDoc}
     *
     * @param field The field to be included in the select clause.
     */
    @Override
    public void selectClause(final String field) {
    }

    /**
     * {@inheritDoc}
     *
     * @param field The field to be used for ordering.
     * @param isAsc A boolean indicating whether the ordering is ascending (true) or descending (false).
     */
    @Override
    public void orderByClause(final String field, final boolean isAsc) {
    }

    /**
     * {@inheritDoc}
     *
     * @param start The index of the first result to retrieve.
     * @param limit The maximum number of results to retrieve.
     */
    @Override
    public void setLimits(final int start, final int limit) {
        derivedQuery.setLimitClause(new LimitClause(start, limit));
    }


    /**
     * {@inheritDoc}
     *
     * @return The name of the primary field.
     */
    @Override
    public String primaryField() {
        return StringUtil.Delimiter.EMPTY_STRING.getDelimiter();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ListStructure} details
     */
    @Override
    public ListStructure structure() {
        return listStructure;
    }

    /**
     * {@inheritDoc}
     *
     * @param key        The key or field name for the criteria.
     * @param value      The value to match against the specified key.
     * @param comparator The condition specifying how to apply the criteria (e.g., "equals", "greater than").
     */
    @Override
    public void criteria(final String key, final Object value, final String comparator) {
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of default {@link ConfiguredField}
     */
    @Override
    public Collection<ConfiguredField> getDefaultConfiguredFields() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param whereClause The {@link WhereClause} to be set
     */
    @Override
    public void setWhereClause(final WhereClause whereClause) {
        derivedQuery.setWhereClause(whereClause);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection dynamic fields
     */
    @Override
    public Collection<String> getDynamicFields() {
        return Collections.emptyList();
    }

    /**
     * <p>
     * Fetches and returns all configured fields for the current organization and user.
     * </p>
     *
     * @return A collection of configured {@link com.tlc.crm.common.list.model.FieldConfig}
     */
    @Override
    public Collection<FieldConfig> fetchConfiguredFields() {
        final Collection<FieldConfig> configuredFields = quotaStructureHandler.fetchConfiguredFields(orgId, userId, provider.name());
        final Collection<FieldConfig> structureFields = listStructure.getFields();
        final List<FieldConfig> fieldConfigs = new ArrayList<>(configuredFields.size() + structureFields.size());
        final Collection<FieldConfig> configuredPersistentFields = new HashSet<>();
        int maxOrder = 0;

        for (final FieldConfig fieldConfig : configuredFields) {

            if (Objects.nonNull(fieldConfig)) {
                fieldConfigs.add(fieldConfig);
                final int order = fieldConfig.getOrder();

                if (order > maxOrder) {
                    maxOrder = order;
                }

                if (fieldConfig.isPersistent()) {
                    configuredPersistentFields.add(fieldConfig);

                    if (0 == order) {
                        fieldConfig.setOrder(++maxOrder);
                    }
                }
            }
        }

        for (final FieldConfig field : structureFields) {

            if (Objects.nonNull(field) && field.isPersistent() && !configuredPersistentFields.contains(field)) {
                field.setOrder(++maxOrder);
                fieldConfigs.add(field);
            }
        }
        fieldConfigs.sort(Comparator.comparingInt(FieldConfig::getOrder));

        return fieldConfigs;
    }

    /**
     * <p>
     * Updates the configured fields for the current organization and user.
     * </p>
     *
     * @param configuredFields the collection of fields to be persisted as configuration
     */
    @Override
    public void updateConfiguredFields(final Collection<ConfiguredField> configuredFields) {
        final Collection<FieldConfig> fieldConfigs = configuredFields.stream().map(configuredField -> {
            final FieldConfig fieldconfig = listStructure.getField(configuredField.fieldName());

            fieldconfig.setEnabled(configuredField.isEnabled(), getFieldEnableType());
            fieldconfig.setOrder(configuredField.getOrder());

            return fieldconfig;
        }).toList();

        quotaStructureHandler.updateConfiguredFields(orgId, userId, provider.name(), fieldConfigs);
    }

    /**
     * {@inheritDoc}
     *
     * @param filterCriteriaList holds the values of {@link FilterCriteria}.
     */
    @Override
    public void applyCriteria(final Collection<FilterCriteria> filterCriteriaList) {
        final WhereClauseBuilder userTeamSelectorBuilder = new WhereClauseBuilder();
        final WhereClauseBuilder additionalCriteriaBuilder = new WhereClauseBuilder(derivedQuery.getWhereClause());

        for (final FilterCriteria filterCriteria : filterCriteriaList) {
            final WhereClause criteriaWhereClause = buildCriteriaWhereClause(filterCriteria);

            if (Objects.nonNull(criteriaWhereClause)) {
                final String fieldAliasName = filterCriteria.getCriteriaField().getAliasName();

                if (USER_TEAM_SELECTOR_FIELDS.contains(fieldAliasName)) {
                    userTeamSelectorBuilder.append(criteriaWhereClause, Connector.OR);
                } else {
                    final FilterConnector filterConnector = filterCriteria.getConnector();
                    final Connector connector = Objects.nonNull(filterConnector) ? filterConnector.getConnector() : Connector.AND;

                    additionalCriteriaBuilder.append(criteriaWhereClause, connector);
                }
            }
        }
        final WhereClause finalCriteria = additionalCriteriaBuilder.append(userTeamSelectorBuilder.getWhereClause(), Connector.AND).getWhereClause();

        if (Objects.nonNull(finalCriteria)) {
            derivedQuery.setWhereClause(finalCriteria);
        }
    }

    /**
     * <p>
     * Fetches the secure readable data store
     * </p>
     *
     * @param orgId Represents an org id
     * @return The {@link ReadableDataStore}
     */
    protected ReadableDataStore getReadableDataStore(final Long orgId) {
        return DataStoreProvider.getSecureReadableOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the derived table required for the query
     * </p>
     *
     * @param field  Represents the field
     * @param table  Represents the field table
     * @param column Represents the field column
     * @return {@link Table}.
     */
    protected Table getDerivedTable(final FieldConfig field, final Table table, final Column column) {
        final SelectQuery selectQuery = SelectQuery.get(table);
        final String fieldName = field.getName();

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(column, field.getFieldId())));

        return DerivedTable.get(table.getName(), getAliasName(fieldName, table.getAlias()), selectQuery);
    }

    /**
     * <p>
     * Adds the appropriate select clause to the given {@link SelectQuery}
     * for the WITHOUT_WON column.
     * </p>
     *
     * @param selectQuery  The {@link SelectQuery} to which the select clause will be added
     * @param derivedTable The derived table containing forecast summary columns
     * @param fieldName    The logical field name used for generating the column alias
     * @param quotaType    The quota type used for alias generation
     */
    protected void addWonSelectClause(final SelectQuery selectQuery, final Table derivedTable,
                                      final String fieldName, final String quotaType) {
        selectQuery.addSelectClause(derivedTable.getColumn(OMQFFORECASTSUMMARY.WITHOUT_WON, getAliasName(fieldName, quotaType)));
    }

    /**
     * <p>
     * Adds the override value column to the given {@link SelectQuery}
     * </p>
     *
     * @param selectQuery  The {@link SelectQuery} to which the override value select clause will be added
     * @param derivedTable The derived table containing the override value column
     * @param fieldConfig  Represents the {@link FieldConfig}
     * @param quotaType    The quota type used for alias generation
     */
    protected void addOverrideValue(final SelectQuery selectQuery, final Table derivedTable, final FieldConfig fieldConfig, final String quotaType) {
        selectQuery.addSelectClause(derivedTable.getColumn(OMQFFORECASTSUMMARY.OVERRIDE_VALUE, getAliasName(StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, QuotaListFields.OVERRIDE_VALUE, fieldConfig.getName()), quotaType)));
    }

    /**
     * <p>
     * Adds SELECT clauses required for user-level quota queries with team context.
     * </p>
     *
     * @param selectQuery the {@link SelectQuery} to which user and team-related
     *                    select clauses will be added
     * @param fieldName   Represents the field name
     */
    protected void addUserSelect(final SelectQuery selectQuery, final String fieldName) {
        final Table teamQuotaTable = getTeamQuotaTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table userQuotaTable = getUserQuotaTable();
        final Table teamTable = Table.get(OMCRMTEAM.TABLE);

        selectQuery.addSelectClause(List.of(userQuotaTable.getColumn(OMQFUSERQUOTA.USER_ID, getAliasName(fieldName, QuotaListFields.USER_QUOTA)),
                teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID, QuotaListFields.TEAM_ID),
                teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID, getAliasName(fieldName, QuotaListFields.TEAM_USER_QUOTA)),
                teamTable.getColumn(OMCRMTEAM.NAME, QuotaListFields.TEAM_NAME)));
    }

    /**
     * <p>
     * Adds the base select columns required for quota list queries.
     * </p>
     *
     */
    protected void loadBaseSelectClause() {
        final Table quotaTable = getTable();

        selectQuery.addSelectClause(List.of(quotaTable.getColumn(OMQFQUOTA.ID, QuotaListFields.ID),
                quotaTable.getColumn(OMQFQUOTA.METRIC, QuotaListFields.METRIC),
                quotaTable.getColumn(OMQFQUOTA.TARGET, QuotaListFields.TARGET),
                quotaTable.getColumn(OMQFQUOTA.CREATED_BY, QuotaListFields.CREATED_BY),
                quotaTable.getColumn(OMQFQUOTA.START_TIME, QuotaListFields.START_TIME),
                quotaTable.getColumn(OMQFQUOTA.END_TIME, QuotaListFields.END_TIME),
                quotaTable.getColumn(OMQFQUOTA.TYPE, QuotaListFields.TYPE),
                quotaTable.getColumn(OMQFQUOTA.TIME_PERIOD, QuotaListFields.TIME_PERIOD)));
    }

    /**
     * <p>
     * Loads user-level and team-user-level quota and forecast data
     * </p>
     *
     * @param fieldConfig       configuration of the quota-related field
     * @param defaultJoinLoader join loader responsible for managing table joins
     */
    protected void loadUserQuota(final FieldConfig fieldConfig, final DefaultJoinLoader defaultJoinLoader) {
        final Table forecastSummaryTable = Table.get(OMQFFORECASTSUMMARY.TABLE);
        final Table userQuotaTable = getUserQuotaTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final Table userQuotaForecastSummary = Table.get(OMQFUSERQUOTAFORECASTSUMMARY.TABLE);
        final Table teamUserQuotaForecastSummary = Table.get(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TABLE);
        final String fieldName = fieldConfig.getName();
        final Table forecastCategoryAliasTable = getAliasTable(forecastSummaryTable, fieldName);
        final Table forecastCategoryDerivedTable = getDerivedTable(fieldConfig, forecastSummaryTable, forecastSummaryTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID));
        final Table forecastCategoryAliasDerivedTable = getDerivedTable(fieldConfig, forecastCategoryAliasTable, forecastCategoryAliasTable.getColumn(OMQFFORECASTSUMMARY.CATEGORY_ID));
        final String summaryIdFieldName = StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, QuotaListFields.SUMMARY_ID, fieldConfig.getName());

        addOverrideValue(selectQuery, forecastCategoryAliasDerivedTable, fieldConfig, QuotaListFields.USER_QUOTA);
        addOverrideValue(selectQuery, forecastCategoryDerivedTable, fieldConfig, QuotaListFields.TEAM_USER_QUOTA);
        selectQuery.addSelectClause(forecastCategoryAliasDerivedTable.getColumn(OMQFFORECASTSUMMARY.ID, getAliasName(summaryIdFieldName, QuotaListFields.USER_QUOTA)));
        selectQuery.addSelectClause(forecastCategoryDerivedTable.getColumn(OMQFFORECASTSUMMARY.ID, getAliasName(summaryIdFieldName, QuotaListFields.TEAM_USER_QUOTA)));

        addWonSelectClause(selectQuery, forecastCategoryDerivedTable, fieldName, QuotaListFields.TEAM_USER_QUOTA);
        addWonSelectClause(selectQuery, forecastCategoryAliasDerivedTable, fieldName, QuotaListFields.USER_QUOTA);

        defaultJoinLoader.load(userQuotaTable.getPKColumn(), userQuotaForecastSummary.getColumn(OMQFUSERQUOTAFORECASTSUMMARY.USER_QUOTA_ID));
        defaultJoinLoader.load(teamUserQuotaTable.getPKColumn(), teamUserQuotaForecastSummary.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID));
        defaultJoinLoader.load(userQuotaForecastSummary.getColumn(OMQFUSERQUOTAFORECASTSUMMARY.SUMMARY_ID), forecastCategoryAliasDerivedTable.getPKColumn());
        defaultJoinLoader.load(teamUserQuotaForecastSummary.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID), forecastCategoryDerivedTable.getPKColumn());
    }

    /**
     * <p>
     * Adds the user team table SELECT clauses and composite JOIN to the selectQuery.
     * Should be called whenever the teamUserQuota table is joined.
     * </p>
     *
     * @param teamUserQuotaTable The team user quota {@link Table}
     * @param teamQuotaTable     The team quota {@link Table}
     */
    protected void loadUserTeamSelectAndJoin(final Table teamUserQuotaTable, final Table teamQuotaTable) {
        final Table userTeamTable = getUserTeamTable();

        selectQuery.addSelectClause(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID, getAliasName(QuotaListFields.TEAM_USER_ID, QuotaListFields.TEAM_USER_QUOTA)));
        selectQuery.addSelectClause(userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID, getAliasName(QuotaListFields.USER_TEAM_ID, QuotaListFields.TEAM_USER_QUOTA)));
        loadUserTeamCompositeJoin(selectQuery, teamUserQuotaTable, teamQuotaTable, userTeamTable);
    }

    /**
     * <p>
     * Checks if the given table is already joined in the select query.
     * </p>
     *
     * @param selectQuery The select query to check
     * @param table       The table to check for
     * @return true if the table is already joined, false otherwise
     */
    private boolean isTableAlreadyJoined(final SelectQuery selectQuery, final Table table) {
        for (final JoinClause existingJoinClause : selectQuery.getJoinClause()) {

            if (Objects.equals(existingJoinClause.getRemoteTable(), table)
                    || Objects.equals(existingJoinClause.getLocalTable(), table)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * Builds the primary {@link SelectQuery} for Quota records by joining core quota tables
     * </p>
     *
     * @return A configured {@link SelectQuery} containing the necessary join clauses and
     * inherited where clauses from the derived query
     */
    protected SelectQuery buildBaseSelectQuery() {
        final Table table = getTable();
        final Column quotaPKColumn = table.getPKColumn();
        final Table userQuotaTable = getUserQuotaTable();
        final Table teamQuotaTable = getTeamQuotaTable();
        final Table teamUserQuotaTable = getTeamUserQuotaTable();
        final SelectQuery selectQuery = SelectQuery.get(primaryTable);

        derivedQuery.addJoinClause(List.of(new JoinClause(quotaPKColumn, userQuotaTable.getColumn(OMQFUSERQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(quotaPKColumn, teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), JoinClause.JoinType.LEFT),
                new JoinClause(teamQuotaTable.getPKColumn(), teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID), JoinClause.JoinType.LEFT)));
        selectQuery.addJoinClause(derivedQuery.getJoinClause());
        selectQuery.setWhereClause(derivedQuery.getWhereClause());

        return selectQuery;
    }

    /**
     * <p>
     * Collects a collection of identifiers from the provided {@link JsonObject}.
     * </p>
     *
     * @param key The key in {@code criteriaJson} associated with the {@link JsonArray} of IDs.
     * @return A {@link Collection} of unique {@link Long} values; returns an empty set if
     * the key is missing or the array is empty.
     */
    protected Collection<Long> collectIdsFromJson(final String key) {
        final JsonArray jsonArray = criteriaJson.getJsonArray(key);
        final int size = jsonArray.size();

        if (Objects.equals(size, NumericalConstant.ZERO)) {
            return Collections.emptySet();
        }
        final Collection<Long> ids = new HashSet<>(size);

        for (int index = 0; index < size; index++) {
            ids.add(jsonArray.getLong(index));
        }
        return QuotaListFields.TEAM_IDS.equals(key) && hasUserOrReportLevelAccess() ? Collections.emptySet() : ids;
    }

    /**
     * Checks whether the logged-in user is restricted to viewing their own quotas.
     *
     * @return {@code true} if the user has the user-level or report-level access
     */
    private boolean hasUserOrReportLevelAccess() {
        final CrmUser crmUser = LoggedInUserInfo.getUser();

        if (Objects.isNull(crmUser)) {
            return false;
        }
        final SubCategory.Type viewType = provider == Provider.QUOTA_HIERARCHY_VIEW_LIST
                ? SubCategory.Type.HIERARCHY_VIEW : SubCategory.Type.TEAM_VIEW;
        final RolePermission viewPermission = rolePermissionService.getPermission(orgId, crmUser.getRoleId(),
                Category.Type.QUOTAS_AND_FORECASTING, viewType, Action.Type.ACCESS);

        if (Objects.isNull(viewPermission) || !viewPermission.isGranted()) {
            return false;
        }
        final Scope scope = viewPermission.getScope();

        return Scope.USER_LEVEL_ACCESS == scope || Scope.REPORT_LEVEL_ACCESS == scope;
    }

    /**
     * <p>
     * Gets the {@link FieldConfig.FieldEnableType}
     * </p>
     *
     * @return The {@link FieldConfig.FieldEnableType}
     */
    private FieldConfig.FieldEnableType getFieldEnableType() {
        return FieldConfig.FieldEnableType.IS_ENABLED_FOR_WEB;
    }

    /**
     * <p>
     * Builds a {@link WhereClause} from the given {@link FilterCriteria} by resolving
     * the criteria field, comparator, and values into an SQL condition.
     * </p>
     *
     * @param filterCriteria The filter criteria containing field, comparator, and values
     * @return A {@link WhereClause} representing the condition, or null if criteria cannot be resolved
     */
    private WhereClause buildCriteriaWhereClause(final FilterCriteria filterCriteria) {
        final com.tlc.crm.entity.filter.api.models.Comparator comparator = filterCriteria.getComparator();
        final List<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();

        if (Objects.isNull(comparator) || Objects.isNull(criteriaValues) || criteriaValues.isEmpty()) {
            return null;
        }
        final Operator operator = Operator.getOperator(comparator.getAliasName());
        final CriteriaField criteriaField = filterCriteria.getCriteriaField();
        final String fieldAliasName = criteriaField.getAliasName();
        final boolean isRestricted = hasUserOrReportLevelAccess();

        if (isRestricted && QuotaListFields.TEAM_IDS.equals(fieldAliasName)) {
            return new WhereClause(Criteria.in(getTeamQuotaTable().getColumn(OMQFTEAMQUOTA.TEAM_ID), List.of(NO_MATCH_ID)));
        }

        if (QuotaListFields.USER_ID.equals(fieldAliasName) || QuotaListFields.USER_IDS.equals(fieldAliasName)) {
            return buildUserFieldWhereClause(operator, isRestricted ? restrictToLoggedInUser(criteriaValues) : criteriaValues);
        }
        final Column column = resolveColumn(fieldAliasName);

        if (Objects.isNull(column)) {
            return null;
        }

        if (operator == Operator.IN || operator == Operator.NOT_IN) {
            final Collection<Object> values = criteriaValues.stream()
                    .map(CriteriaValue::getValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (values.isEmpty()) {
                return null;
            }

            if (operator == Operator.IN) {
                return new WhereClause(Criteria.in(column, values));
            }
            return new WhereClause(Criteria.notIn(column, values));
        }
        final Object value = criteriaValues.get(0).getValue();

        if (Objects.isNull(value)) {
            return null;
        }

        if (operator == Operator.EQUAL || operator == Operator.NOT_EQUAL) {
            return new WhereClause(operator == Operator.EQUAL ? Criteria.eq(column, value)
                    : Criteria.notEq(column, value));
        }

        if (!(value instanceof Number numberValue)) {
            return null;
        }
        return new WhereClause(switch (operator) {
            case GREATER_THAN -> Criteria.gt(column, numberValue);
            case GREATER_EQUAL -> Criteria.ge(column, numberValue);
            case LESS_THAN -> Criteria.lt(column, numberValue);
            case LESS_EQUAL -> Criteria.le(column, numberValue);
            default -> Criteria.eq(column, value);
        });
    }

    /**
     * <p>
     * Restricts the requested user criteria values to the logged-in user. When the logged-in user is not part
     * of the requested values, a value that matches no user is returned so that no quota is fetched.
     * </p>
     *
     * @param criteriaValues The requested user {@link CriteriaValue}s
     * @return The criteria values the logged-in user is permitted to see
     */
    private List<CriteriaValue> restrictToLoggedInUser(final List<CriteriaValue> criteriaValues) {
        final List<CriteriaValue> userCriteriaValues = criteriaValues.stream()
                .filter(criteriaValue -> Objects.nonNull(criteriaValue.getValue())
                        && Objects.equals(String.valueOf(criteriaValue.getValue()), String.valueOf(userId)))
                .toList();

        if (!userCriteriaValues.isEmpty()) {
            return userCriteriaValues;
        }
        final CriteriaValue nonMatchingCriteriaValue = new CriteriaValue();

        nonMatchingCriteriaValue.setValue(NO_MATCH_ID);
        return List.of(nonMatchingCriteriaValue);
    }

    /**
     * <p>
     * Builds a composite {@link WhereClause} for the user field criteria.
     * Since a user can be present in either the user quota table (direct assignment)
     * or the team user quota table (team-based assignment), the WHERE clause checks
     * both tables using OR logic.
     * </p>
     *
     * @param operator       The {@link Operator} to apply (IN, EQUAL, NOT_IN, etc.)
     * @param criteriaValues The list of {@link CriteriaValue} containing user IDs
     * @return A composite {@link WhereClause} covering both user quota and team user quota tables,
     *         or null if criteria values are invalid
     */
    private WhereClause buildUserFieldWhereClause(final Operator operator, final List<CriteriaValue> criteriaValues) {
        final Column userQuotaUserIdColumn = getUserQuotaTable().getColumn(OMQFUSERQUOTA.USER_ID);
        final Column teamUserQuotaUserIdColumn = getTeamUserQuotaTable().getColumn(OMQFTEAMUSERQUOTA.USER_ID);

        if (operator == Operator.IN) {
            final Collection<Object> values = criteriaValues.stream()
                    .map(CriteriaValue::getValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (values.isEmpty()) {
                return null;
            }
            return new WhereClause(Criteria.in(userQuotaUserIdColumn, values))
                    .or(Criteria.in(teamUserQuotaUserIdColumn, values));
        }
        return null;
    }

    /**
     * <p>
     * Resolves the field alias name to the corresponding table {@link Column}.
     * </p>
     *
     * @param fieldAliasName The alias name of the criteria field
     * @return The resolved {@link Column}, or null if the field is not recognized
     */
    private Column resolveColumn(final String fieldAliasName) {
        final Table quotaTable = getTable();

        return switch (fieldAliasName) {
            case QuotaListFields.TEAM_IDS -> getTeamQuotaTable().getColumn(OMQFTEAMQUOTA.TEAM_ID);
            case QuotaListFields.START_TIME -> quotaTable.getColumn(OMQFQUOTA.START_TIME);
            case QuotaListFields.END_TIME -> quotaTable.getColumn(OMQFQUOTA.END_TIME);
            case QuotaListFields.METRIC -> quotaTable.getColumn(OMQFQUOTA.METRIC);
            default -> null;
        };
    }

    /**
     * <p>
     * Loads a composite join for the user team table that joins on both USER_ID and TEAM_ID.
     * This ensures correct matching when a user belongs to multiple teams.
     * The join is only added if the user team table is not already joined.
     * </p>
     *
     * @param selectQuery        The select query to add the join to
     * @param teamUserQuotaTable The team user quota table containing USER_ID
     * @param teamQuotaTable     The team quota table containing TEAM_ID
     * @param userTeamTable      The user team table to join
     */
    private void loadUserTeamCompositeJoin(final SelectQuery selectQuery, final Table teamUserQuotaTable,
                                           final Table teamQuotaTable, final Table userTeamTable) {
        if (isTableAlreadyJoined(selectQuery, userTeamTable)) {
            return;
        }
        final List<JoinClause.Relation> relations = List.of(
                new JoinClause.Relation(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), userTeamTable.getColumn(MCRMUSERTEAM.USER_ID)),
                new JoinClause.Relation(teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID), userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID))
        );

        selectQuery.addJoinClause(new JoinClause(teamUserQuotaTable, userTeamTable, relations, JoinClause.JoinType.LEFT));
    }
}