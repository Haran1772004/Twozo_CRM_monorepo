package com.tlc.crm.core.list;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.common.list.model.ListRequestData;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.sql.resource.MDODEAL;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.fields.CriteriaFields;
import com.tlc.crm.entity.filter.fields.CriteriaValueAttributes;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.filter.ComparatorService;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.l10n.model.currency.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Process the list actions in the entity summary and inherits the {@link EntityListAction}
 * </p>
 *
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public abstract class EntitySummaryListAction extends EntityListAction {

    protected final FieldSourceService fieldSourceService;
    protected final FieldService fieldService;
    protected final DealManager dealManager;

    private final BaseCurrencyService baseCurrencyService;
    private final MonetaryManager monetaryManager;
    private final ComparatorService comparatorService;

    protected EntitySummaryListAction() {
        fieldSourceService = FieldSourceService.getInstance();
        fieldService = FieldService.getSecureInstance();
        dealManager = DealManager.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
        comparatorService = ComparatorService.getInstance();
    }

    /**
     * <p>
     * Loads the summary details into the response JSON.
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}.
     * @param response        Holds the response {@link JsonObject}.
     */
    @Override
    protected void loadSummaryIntoResponse(final EntityListInterpreter listInterpreter, final JsonObject response) {
    }

    /**
     * <p>
     * Loads the required preview details into the provided entity rows.
     * </p>
     *
     * @param listInterpreter Represents the {@link EntityListInterpreter}
     * @param entityRows      The {@link Map} containing entity IDs to their associated data rows
     */
    @Override
    protected void loadPreviewData(final EntityListInterpreter listInterpreter, final Map<String, Map<String, Object>> entityRows) {
    }

    /**
     * <p>
     * Gets the criteria array with the given values
     * </p>
     *
     * @param sourceId    Represents the source Id
     * @param entityId    Represents the entity Id
     * @return The criteria array
     */
    protected JsonArray getCriteriaArray(final long sourceId, final long entityId, final Map<Long, Pair<String, FilterConnector>> filterFieldMap) {
        final JsonArray criteriaArray = Json.array();

        filterFieldMap.forEach((fieldId, pair) -> {
            final JsonObject criteriaJson = Json.object();
            final JsonObject fieldJson = Json.object();

            fieldJson.put(ListActionFields.FIELD_ID, fieldId);

            criteriaJson.put(ListActionFields.FIELD, fieldJson);
            criteriaJson.put(ListActionFields.SOURCE_ID, sourceId);
            criteriaJson.put(CriteriaFields.CONNECTOR_ID.getName(), pair.getValue().getId());
            criteriaJson.put(CriteriaFields.COMPARATOR_ID.getName(), Operator.EQUAL.getId());
            final JsonArray valueArray = Json.array();
            final JsonObject valueJson = Json.object();

            valueJson.put(pair.getKey(), entityId);
            valueArray.put(valueJson);
            criteriaJson.put(CriteriaFields.VALUES.getName(), valueArray);
            criteriaArray.put(criteriaJson);
        });
        return criteriaArray;
    }

    /**
     * {@inheritDoc}
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param responseJson    Represents the list response json
     */
    @Override
    protected void postProcess(final Provider provider, final ListRequestData listRequestData, final JsonObject responseJson) {

        if (Objects.equals(Provider.DEAL_LIST, provider)) {
            final long entityId = NumberUtils.parseNumber(listRequestData.getParameter(FieldKeys.ID.getName())).longValue();

            loadDealListSummary(listRequestData, entityId, responseJson);
        }
    }

    /**
     * <p>
     * Gets the deal criteria array with the given values
     * </p>
     *
     * @param sourceId    Represents the source Id
     * @param entityId    Represents the entity Id
     * @param requestJson Represents the request json
     * @return The deal criteria array
     */
    protected JsonArray getDealCriteriaArray(final Long sourceId, final long entityId, final JsonObject requestJson,
            final Map<Long, Pair<String, FilterConnector>> filterFieldMap) {
        final JsonArray dealCriteriaArray = getCriteriaArray(sourceId, entityId, filterFieldMap);
        final String additionalCriteriaKey = CriteriaFields.ADDITIONAL_CRITERIA.getName();

        if (requestJson.containsKey(additionalCriteriaKey)) {
            final JsonArray additionalCriteriaArray = requestJson.getJsonArray(additionalCriteriaKey);
            final int criteriaArraySize = additionalCriteriaArray.size();
            final String fieldKey = FieldKeys.FIELD.getName();
            final String dealStateKey = DealFields.DEAL_STATE;
            final String valuesKey = FieldKeys.VALUES.getName();

            for (int index = 0; index < criteriaArraySize; index++) {
                final JsonObject criteriaJson = additionalCriteriaArray.getJsonObject(index);
                final String fieldName = criteriaJson.getString(fieldKey);

                if (Objects.equals(dealStateKey, fieldName)) {
                    final JsonArray stateIdsArray = criteriaJson.getJsonArray(valuesKey);
                    final int stateIdsSize = stateIdsArray.size();

                    if (stateIdsSize > 0 && stateIdsSize < 3) {
                        loadDealStateCriteria(stateIdsArray, dealCriteriaArray);
                    }
                    additionalCriteriaArray.remove(index);

                    if (criteriaArraySize == 1) {
                        requestJson.remove(additionalCriteriaKey);
                    }
                    break;
                }
            }
        }
        return dealCriteriaArray;
    }

    /**
     * <p>
     * Loads the deal state criteria array
     * </p>
     *
     * @param stateIdsArray  Represents the state ids array
     */
    private void loadDealStateCriteria(final JsonArray stateIdsArray, final JsonArray dealCriteriaArray) {
        final String statusKey = DealFields.STATUS;
        final Long dealSourceId = fieldSourceService.getSource(Source.DEAL.getName()).id();
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, statusKey);
        final JsonObject stateFieldJson = Json.object();
        final Comparator comparator = comparatorService.getByName(Operator.IN.name());

        stateFieldJson.put(ListActionFields.FIELD_ID, fields.get(statusKey).id());
        final JsonObject stateIdCriteria = Json.object();

        stateIdCriteria.put(ListActionFields.FIELD, stateFieldJson);
        stateIdCriteria.put(ListActionFields.SOURCE_ID, dealSourceId);
        stateIdCriteria.put(CriteriaFields.CONNECTOR_ID.getName(), FilterConnector.AND.getId());
        stateIdCriteria.put(CriteriaFields.COMPARATOR_ID.getName(), comparator.getId());
        final JsonArray valueArray = Json.array();

        for (int index = 0; index < stateIdsArray.size(); index++) {
            final long stateId = stateIdsArray.getLong(index);
            final JsonObject valueJson = Json.object();

            valueJson.put(CriteriaValueAttributes.VALUE_ENTITY_ID.getName(), stateId);
            valueArray.put(valueJson);
        }
        stateIdCriteria.put(CriteriaFields.VALUES.getName(), valueArray);
        dealCriteriaArray.put(stateIdCriteria);
    }

    /**
     * <p>
     * Load the deal list summary data
     * </p>
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @param entityId        Represents the entity Id
     * @param responseJson    Represents the response json
     */
    protected void loadDealListSummary(final ListRequestData listRequestData, final long entityId, final JsonObject responseJson) {
        final long orgId = listRequestData.getOrgId();
        final Table dealTable = dealManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);
        final Column dealStateColumn = dealTable.getColumn(MDODEAL.STATE);
        final Column dealMonetaryColumn = dealTable.getColumn(MDODEAL.MONETARY_ID);
        final int openStateId = Deal.State.OPEN.getId();
        final int wonStateId = Deal.State.WON.getId();

        selectQuery.addSelectClause(monetaryTable);
        selectQuery.addSelectClause(List.of(dealMonetaryColumn, dealStateColumn));
        selectQuery.addJoinClause(new JoinClause(dealMonetaryColumn, monetaryTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(dealStateColumn, List.of(openStateId, wonStateId)))
                .and(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.STATUS), Deal.Status.ACTIVE.getId()))));

        loadDealEntityCriteria(entityId, selectQuery);

        final ReadableDataStore secureReadableOrgDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
        final DataContainer summaryDataContainer = secureReadableOrgDataStore.get(selectQuery);
        final JsonArray listSummaryJsonArray = convertListSummaryJson(orgId, summaryDataContainer, listRequestData.getI18nPlaceholderResolver());

        responseJson.put(ContactFields.SUMMARY, listSummaryJsonArray);
    }

    /**
     * <p>
     * Converts the list summary data
     * </p>
     *
     * @param orgId                Represents the organisation Id
     * @param summaryDataContainer Represents the {@link DataContainer} containing list summary data
     * @return The {@link JsonArray} containing list summary data
     */
    private JsonArray convertListSummaryJson(final long orgId, final DataContainer summaryDataContainer, final I18nResolutionAdapter i18NResolutionAdapter) {
        final Table dealTable = dealManager.getTable();
        final Column dealStateColumn = dealTable.getColumn(MDODEAL.STATE);
        final int openStateId = Deal.State.OPEN.getId();
        final int wonStateId = Deal.State.WON.getId();
        final Currency defaultCurrency = baseCurrencyService.getDefaultCurrency(orgId);

        final Collection<Long> openDealMonetaryIds = summaryDataContainer.getRows(dealTable, new WhereClause(Criteria
                .eq(dealStateColumn, openStateId))).map(dealRow -> (Long) dealRow.get(MDODEAL.MONETARY_ID)).toList();
        final Collection<Long> wonDealMonetaryIds = summaryDataContainer.getRows(dealTable, new WhereClause(Criteria
                .eq(dealStateColumn, wonStateId))).map(dealRow -> (Long) dealRow.get(MDODEAL.MONETARY_ID)).toList();

        final BigDecimal openDealsValue = getTotalDealValueByStage(summaryDataContainer, openDealMonetaryIds, defaultCurrency);
        final BigDecimal wonDealsValue = getTotalDealValueByStage(summaryDataContainer, wonDealMonetaryIds, defaultCurrency);
        final JsonArray listSummaryArray = Json.array();

        final JsonArray openDealsSummaryArray = Json.array();
        final JsonObject openDealsCountJson = Json.object();
        final JsonObject openDealsValueJson = Json.object();

        openDealsCountJson.put(FieldKeys.DISPLAY_NAME.getName(), i18NResolutionAdapter.get("i18n.contact.fields.deal.placeholder"));
        openDealsCountJson.put(FieldKeys.VALUE.getName(), openDealMonetaryIds.size());
        openDealsSummaryArray.put(openDealsCountJson);

        openDealsValueJson.put(FieldKeys.DISPLAY_NAME.getName(), i18NResolutionAdapter.get("i18n.entity.field.value"));
        openDealsValueJson.put(FieldKeys.VALUE.getName(), String.format("%s %s", defaultCurrency.getSymbol(),
                baseCurrencyService.applyPrecision(orgId, openDealsValue)));
        openDealsSummaryArray.put(openDealsValueJson);

        listSummaryArray.put(openDealsSummaryArray);

        final JsonArray wonDealsSummaryArray = Json.array();
        final JsonObject wonDealsCountJson = Json.object();
        final JsonObject wonDealsValueJson = Json.object();

        wonDealsCountJson.put(FieldKeys.DISPLAY_NAME.getName(), i18NResolutionAdapter.get("i18n.contact.fields.wonDeals.placeholder"));
        wonDealsCountJson.put(FieldKeys.VALUE.getName(), wonDealMonetaryIds.size());
        wonDealsSummaryArray.put(wonDealsCountJson);

        wonDealsValueJson.put(FieldKeys.DISPLAY_NAME.getName(), i18NResolutionAdapter.get("i18n.entity.field.value"));
        wonDealsValueJson.put(FieldKeys.VALUE.getName(), String.format("%s %s", defaultCurrency.getSymbol(),
                baseCurrencyService.applyPrecision(orgId, wonDealsValue)));
        wonDealsSummaryArray.put(wonDealsValueJson);

        listSummaryArray.put(wonDealsSummaryArray);

        return listSummaryArray;
    }

    /**
     * <p>
     * Loads the deal entity criteria
     * </p>
     *
     * @param entityId     Represents the entity Id
     * @param selectQuery  Represent the {@link SelectQuery} to fetch deal list summary data
     */
    protected void loadDealEntityCriteria(final long entityId, final SelectQuery selectQuery) {
    }

    /**
     * <p>
     * Gets the {@link Source}
     * </p>
     *
     * @return The {@link Source}
     */
    protected Source getSource() {
        return null;
    }

    /**
     * <p>
     * Gets the total deal value in the deal stage
     * </p>
     *
     * @param summaryDataContainer Represents the {@link DataContainer}
     * @param dealMonetaryIds      Represents the collection monetary Ids
     * @param defaultCurrency      Represents the default {@link Currency}
     * @return The total deal value in the deal stage
     */
    private BigDecimal getTotalDealValueByStage(final DataContainer summaryDataContainer, final Collection<Long> dealMonetaryIds, final Currency defaultCurrency) {
        final Table monetaryTable = monetaryManager.getTable();

        return summaryDataContainer.getRows(monetaryTable,
                new WhereClause(Criteria.in(monetaryTable.getPKColumn(), dealMonetaryIds))).map(monetaryRow -> {
            final Monetary monetary = monetaryManager.convertRowToModel(monetaryRow);
            final Currency currency = monetary.getCurrency();
            final BigDecimal value = monetary.getValue();

            if (!Objects.equals(defaultCurrency, currency)) {
                return value.divide(baseCurrencyService.getExchangeRate(defaultCurrency, currency), NumericalConstant.FIVE, RoundingMode.HALF_EVEN);
            }
            return value;
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}