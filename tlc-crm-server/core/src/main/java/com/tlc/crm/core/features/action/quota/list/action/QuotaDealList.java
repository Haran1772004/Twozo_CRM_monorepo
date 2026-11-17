package com.tlc.crm.core.features.action.quota.list.action;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.sql.resource.MCRMUSERTEAM;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.action.quota.list.QuotaDealSQLListInterpreter;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.manager.QuotaHolderManager;
import com.tlc.crm.core.features.api.quota.model.Quota.RelatedEntityType;
import com.tlc.crm.core.features.api.quota.model.QuotaPeriod;
import com.tlc.crm.core.features.api.quota.util.DateIntervalResolver;
import com.tlc.crm.core.features.api.quota.model.ForecastType;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MDOPRODUCTDETAIL;
import com.tlc.crm.core.sql.resource.OMDOPRODUCTDETAILSHOLDER;
import com.tlc.crm.core.sql.resource.OMQFTEAMQUOTA;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTA;
import com.tlc.crm.core.sql.resource.OMQFQUOTAFORECASTSUMMARYDEAL;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAFORECASTSUMMARY;
import com.tlc.crm.core.sql.resource.OMQFTEAMUSERQUOTAWONDEAL;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.WhereClauseBuilder;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Process the quota deal list web action
 * </p>
 *
 * @author kishore
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.DEALS, validateFieldPermission = true)
@WebAction(path = "/quota/deal/list")
public class QuotaDealList extends EntityListAction {

    private final ForecastCategoryManager forecastCategoryManager;
    private final QuotaHolderManager quotaHolderManager;

    public QuotaDealList() {
        forecastCategoryManager = ForecastCategoryManager.getInstance();
        quotaHolderManager = QuotaHolderManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Provider}.
     */
    @Override
    protected Provider getProvider() {
        return Provider.DEAL_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId           Represents the org id
     * @param provider        Represents the {@link Provider}
     * @param listInterpreter Represents the {@link EntityListInterpreter}
     * @param criteria        Represents the filter criteria
     */
    @Override
    protected void applyCriteria(final long orgId, final Provider provider, final EntityListInterpreter listInterpreter, final JsonArray criteria) {
        final int criteriaSize = criteria.size();
        final Table dealTable = Table.get(MDODEAL.TABLE);
        final Criteria wonCriteria = Criteria.eq(dealTable.getColumn(MDODEAL.STATE), Deal.State.WON.getId());
        ForecastType forecastType = ForecastType.WON;
        JsonObject dateIntervalCriteria = null;
        Long criteriaQuotaId = null;
        boolean hasForecastCategoryCriteria = false;
        boolean hasWonCriteria = false;

        for (int index = 0; index < criteriaSize; index++) {
            final JsonObject criteriaJson = criteria.getJsonObject(index);
            final String field = criteriaJson.getString(ListActionFields.FIELD);

            if (criteriaJson.containsKey(QuotaListFields.QUOTA_ID)) {
                criteriaQuotaId = criteriaJson.getLong(QuotaListFields.QUOTA_ID);
            } else if (QuotaListFields.ID.equals(field) && criteriaJson.containsKey(QuotaListFields.VALUE)) {
                criteriaQuotaId = criteriaJson.getLong(QuotaListFields.VALUE);
            }

            if (QuotaListFields.FORECAST_TYPE.equals(field)) {
                forecastType = ForecastType.getById(NumberUtils.parseNumber(criteriaJson.getLong(QuotaListFields.VALUE)).shortValue());
            } else if (QuotaListFields.TIME_PERIOD.equals(field)) {
                dateIntervalCriteria = criteriaJson;
            } else if (QuotaListFields.FORECAST_CATEGORY.equals(field)) {
                hasForecastCategoryCriteria = true;
            } else if (QuotaListFields.WON.equals(field)) {
                hasWonCriteria = true;
            }
        }
        final QuotaPeriod quotaPeriod = resolveQuotaPeriod(orgId, criteriaQuotaId, dateIntervalCriteria);

        WhereClause dealCriteria = null;

        for (int index = 0; index < criteriaSize; index++) {
            final JsonObject criteriaJson = criteria.getJsonObject(index);
            final String field = criteriaJson.getString(ListActionFields.FIELD);

            final WhereClause whereClause = switch (field) {
                case QuotaListFields.USER -> new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.OWNER_ID), criteriaJson.getLong(QuotaListFields.VALUE)));
                case QuotaListFields.TEAM -> {
                    final long quotaId = criteriaJson.getLong(QuotaListFields.QUOTA_ID);
                    final long teamId = criteriaJson.getLong(QuotaListFields.VALUE);

                    yield buildTeamDealCriteria(dealTable, quotaId, teamId);
                }
                case QuotaListFields.TIME_PERIOD -> {
                    if (hasForecastCategoryCriteria || hasWonCriteria || Objects.isNull(quotaPeriod)) {
                        yield null;
                    }

                    final String dateColumn = forecastType.isWon() ? MDODEAL.CLOSED_TIME : MDODEAL.EXPECTED_CLOSE_DATE;

                    yield new WhereClause(Criteria.between(dealTable.getColumn(dateColumn), quotaPeriod.startTime(), quotaPeriod.endTime()));
                }
                case QuotaListFields.WON -> {
                    final WhereClauseBuilder wonWhereClauseBuilder = new WhereClauseBuilder(new WhereClause(wonCriteria));

                    if (Objects.nonNull(quotaPeriod)) {
                        wonWhereClauseBuilder.append(new WhereClause(Criteria.between(dealTable.getColumn(MDODEAL.CLOSED_TIME), quotaPeriod.startTime(), quotaPeriod.endTime())), Connector.AND);
                    }
                    yield wonWhereClauseBuilder.getWhereClause();
                }
                case QuotaListFields.FORECAST_CATEGORY -> {
                    final long categoryId = criteriaJson.getLong(QuotaListFields.VALUE);
                    final Column forecastCategoryColumn = dealTable.getColumn(MDODEAL.FORECAST_CATEGORY_ID);
                    final Criteria categoryCriteria;

                    if (forecastType.isWon() && forecastCategoryManager.get(orgId, categoryId).isBestCase()) {
                        final Optional<Long> optionalCommitedCategoryId = forecastCategoryManager.getCommitedCategoryId(orgId);

                        categoryCriteria = optionalCommitedCategoryId.map(commitedCategoryId -> Criteria.in(forecastCategoryColumn, Set.of(commitedCategoryId, categoryId))).orElseGet(() -> Criteria.eq(forecastCategoryColumn, categoryId));
                    } else {
                        categoryCriteria = Criteria.eq(forecastCategoryColumn, categoryId);
                    }

                    if (forecastType.isWon()) {
                        WhereClause categoryWithinPeriod = new WhereClause(categoryCriteria);
                        WhereClause wonWithinPeriod = new WhereClause(wonCriteria);

                        if (Objects.nonNull(quotaPeriod)) {
                            final long startTime = quotaPeriod.startTime();
                            final long endTime = quotaPeriod.endTime();

                            categoryWithinPeriod = categoryWithinPeriod.and(Criteria.between(dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), startTime, endTime));
                            wonWithinPeriod = wonWithinPeriod.and(Criteria.between(dealTable.getColumn(MDODEAL.CLOSED_TIME), startTime, endTime));
                        }
                        yield wonWithinPeriod.or(categoryWithinPeriod);
                    } else {
                        WhereClause whereClauses = new WhereClause(Criteria.notEq(dealTable.getColumn(MDODEAL.STATE), Deal.State.WON.getId())).and(categoryCriteria);

                        if (Objects.nonNull(quotaPeriod)) {
                            whereClauses = whereClauses.and(Criteria.between(dealTable.getColumn(MDODEAL.EXPECTED_CLOSE_DATE), quotaPeriod.startTime(), quotaPeriod.endTime()));
                        }
                        yield whereClauses;
                    }
                }
                default -> null;
            };

            if (Objects.nonNull(whereClause)) {
                dealCriteria = WhereClauseUtil.connect(dealCriteria, whereClause, Connector.AND);
            }
        }
        dealCriteria = applyQuotaScopeCriteria(orgId, criteriaQuotaId, dealTable, dealCriteria);
        listInterpreter.setWhereClause(dealCriteria);
        applyDefaultCriteria(listInterpreter);
    }
    /**
     * Builds owner/deal criteria for team quota drill-down, including deals owned by enabled team members
     * or previously tracked before a member's removal.
     *
     * @param dealTable The deal table
     * @param quotaId   The quota ID
     * @param teamId    The team ID
     * @return The criteria for the deal list
     */
    private WhereClause buildTeamDealCriteria(final Table dealTable, final long quotaId, final long teamId) {
        final Table teamQuotaTable = Table.get(OMQFTEAMQUOTA.TABLE);
        final Table teamUserQuotaTable = Table.get(OMQFTEAMUSERQUOTA.TABLE);
        final Table userTeamTable = Table.get(MCRMUSERTEAM.TABLE);
        final WhereClause teamScope = new WhereClause(Criteria.eq(teamQuotaTable.getColumn(OMQFTEAMQUOTA.QUOTA_ID), quotaId))
                .and(Criteria.eq(teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID), teamId));

        final SelectQuery activeUserQuery = SelectQuery.get(teamQuotaTable);

        activeUserQuery.addSelectClause(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID));
        activeUserQuery.addJoinClause(new JoinClause(teamQuotaTable.getPKColumn(),
                teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID)));
        activeUserQuery.addJoinClause(new JoinClause(teamUserQuotaTable, userTeamTable, List.of(
                new JoinClause.Relation(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.USER_ID), userTeamTable.getColumn(MCRMUSERTEAM.USER_ID)),
                new JoinClause.Relation(teamQuotaTable.getColumn(OMQFTEAMQUOTA.TEAM_ID), userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID))),
                JoinClause.JoinType.INNER));
        activeUserQuery.setWhereClause(teamScope.and(Criteria.eq(teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.IS_ENABLED), true)));

        final Table wonDealTable = Table.get(OMQFTEAMUSERQUOTAWONDEAL.TABLE);
        final SelectQuery wonDealQuery = SelectQuery.get(teamQuotaTable);

        wonDealQuery.addSelectClause(wonDealTable.getColumn(OMQFTEAMUSERQUOTAWONDEAL.DEAL_ID));
        wonDealQuery.addJoinClause(new JoinClause(teamQuotaTable.getPKColumn(),
                teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID)));
        wonDealQuery.addJoinClause(new JoinClause(teamUserQuotaTable.getPKColumn(),
                wonDealTable.getColumn(OMQFTEAMUSERQUOTAWONDEAL.TEAM_USER_QUOTA_ID)));
        wonDealQuery.setWhereClause(teamScope);

        final Table forecastLinkTable = Table.get(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TABLE);
        final Table forecastDealTable = Table.get(OMQFQUOTAFORECASTSUMMARYDEAL.TABLE);
        final SelectQuery forecastDealQuery = SelectQuery.get(teamQuotaTable);

        forecastDealQuery.addSelectClause(forecastDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.DEAL_ID));
        forecastDealQuery.addJoinClause(new JoinClause(teamQuotaTable.getPKColumn(),
                teamUserQuotaTable.getColumn(OMQFTEAMUSERQUOTA.TEAM_QUOTA_ID)));
        forecastDealQuery.addJoinClause(new JoinClause(teamUserQuotaTable.getPKColumn(),
                forecastLinkTable.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.TEAM_USER_QUOTA_ID)));
        forecastDealQuery.addJoinClause(new JoinClause(forecastLinkTable.getColumn(OMQFTEAMUSERQUOTAFORECASTSUMMARY.SUMMARY_ID),
                forecastDealTable.getColumn(OMQFQUOTAFORECASTSUMMARYDEAL.SUMMARY_ID)));
        forecastDealQuery.setWhereClause(teamScope);

        return new WhereClause(Criteria.in(dealTable.getColumn(MDODEAL.OWNER_ID), activeUserQuery))
                .or(new WhereClause(Criteria.in(dealTable.getPKColumn(), wonDealQuery)))
                .or(new WhereClause(Criteria.in(dealTable.getPKColumn(), forecastDealQuery)));
    }

    /**
     * Applies quota-specific pipeline, territory, and product filters to drill-down deals.
     *
     * @param orgId the organization ID
     * @param quotaId the quota ID, may be null
     * @param dealTable the deal table
     * @param dealCriteria the existing criteria, may be null
     * @return the criteria with quota filters applied
     */
    private WhereClause applyQuotaScopeCriteria(final long orgId, final Long quotaId, final Table dealTable, final WhereClause dealCriteria) {
        if (Objects.isNull(quotaId)) {
            return dealCriteria;
        }
        final WhereClauseBuilder scopeBuilder = new WhereClauseBuilder(dealCriteria);

        final Collection<Long> pipelineIds = quotaHolderManager.fetchRelatedEntityIds(orgId, quotaId, RelatedEntityType.PIPELINE);
        final Collection<Long> territoryIds = quotaHolderManager.fetchRelatedEntityIds(orgId, quotaId, RelatedEntityType.TERRITORY);
        final Collection<Long> productIds = quotaHolderManager.fetchRelatedEntityIds(orgId, quotaId, RelatedEntityType.PRODUCT);

        if (!pipelineIds.isEmpty()) {
            scopeBuilder.append(new WhereClause(Criteria.in(dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineIds)), Connector.AND);
        }

        if (!territoryIds.isEmpty()) {
            scopeBuilder.append(new WhereClause(Criteria.in(dealTable.getColumn(MDODEAL.TERRITORY_ID), territoryIds)), Connector.AND);
        }

        if (!productIds.isEmpty()) {
            final Table productDetailsHolderTable = Table.get(OMDOPRODUCTDETAILSHOLDER.TABLE);
            final Table productDetailTable = Table.get(MDOPRODUCTDETAIL.TABLE);
            final SelectQuery productQuery = SelectQuery.get(productDetailsHolderTable);

            productQuery.addSelectClause(productDetailsHolderTable.getColumn(OMDOPRODUCTDETAILSHOLDER.DEAL_ID));
            productQuery.addJoinClause(new JoinClause(productDetailsHolderTable.getPKColumn(),
                    productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_DETAILS_HOLDER_ID)));
            productQuery.setWhereClause(new WhereClause(Criteria.in(productDetailTable.getColumn(MDOPRODUCTDETAIL.PRODUCT_ID), productIds)));
            scopeBuilder.append(new WhereClause(Criteria.in(dealTable.getPKColumn(), productQuery)), Connector.AND);
        }
        return scopeBuilder.getWhereClause();
    }

    /**
     * Resolves the drill-down window using the quota's period when a quota id is provided,
     * otherwise falls back to the forecast period criteria.
     *
     * @param orgId                Represents the org id
     * @param quotaId              Represents the quota id, may be null
     * @param dateIntervalCriteria Represents the forecast period criteria, may be null
     * @return The resolved {@link QuotaPeriod}, or null when neither is available
     */
    private QuotaPeriod resolveQuotaPeriod(final long orgId, final Long quotaId, final JsonObject dateIntervalCriteria) {
        if (Objects.nonNull(quotaId)) {
            final QuotaPeriod quotaPeriod = quotaHolderManager.getQuotaPeriod(orgId, quotaId);

            if (Objects.nonNull(quotaPeriod)) {
                return quotaPeriod;
            }
        }

        if (Objects.isNull(dateIntervalCriteria)) {
            return null;
        }
        final Interval<Long, Long> interval = DateIntervalResolver.resolve(dateIntervalCriteria.getJsonObject(QuotaListFields.DATE_INTERVAL));

        return new QuotaPeriod(interval.start(), interval.end());
    }


    /**
     * {@inheritDoc}
     *
     * @param request               Represents the request {@link JsonObject}
     * @param provider              Represents the {@link Provider}
     * @param joinLoader            Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return {@link ListInterpreter}
     */
    @Override
    protected EntityListInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                             final Provider provider, final JoinLoader joinLoader,
                                                             final I18nResolutionAdapter i18NResolutionAdapter) {
        return new QuotaDealSQLListInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     *
     * @param config Represents the {@link FieldConfig.Config}
     * @return True if the field is enabled, false otherwise
     */
    @Override
    protected boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isEnabledForQuotaDealList();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ListType}
     */
    @Override
    protected ListType getListType() {
        return ListType.QUOTA_DEAL_LIST;
    }
}