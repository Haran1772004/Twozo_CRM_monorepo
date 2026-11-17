package com.tlc.crm.core.list.dynamic;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.ListStatisticsManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.list.dynamic.model.ListDynamicFields;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldsData;
import com.tlc.crm.entity.models.type.Monetary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Implements {@link DynamicFieldsFetcher} the for fetching dynamic field data to deal static fields
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class DealStatisticsFieldsFetcher extends DynamicFieldsFetcher {

    private static final String DEAL_VALUE_FORMAT = "%s %s";

    private final BaseCurrencyService baseCurrencyService;

    private DealStatisticsFieldsFetcher() {
        this.baseCurrencyService = BaseCurrencyService.getInstance();
    }

    private static class InstanceHolder {
        private static final DealStatisticsFieldsFetcher INSTANCE = new DealStatisticsFieldsFetcher();
    }

    /**
     * <p>
     * Gets the {@link DealStatisticsFieldsFetcher} instance
     * </p>
     *
     * @return The {@link DealStatisticsFieldsFetcher}
     */
    public static DealStatisticsFieldsFetcher getInstance() {
        return DealStatisticsFieldsFetcher.InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param dynamicFieldRequest Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    @Override
    public DynamicFieldsData get(final DynamicFieldRequest dynamicFieldRequest) {
        final Collection<String> dealStatisticFields = getDealStatisticFields();
        final Collection<String> dynamicFields = dynamicFieldRequest.dynamicFields();
        final Source source = dynamicFieldRequest.provider().getSource();
        final Collection<Long> entityIds = dynamicFieldRequest.entityIds();
        final long orgId = dynamicFieldRequest.orgId();
        final boolean dealStatisticsRequired = dynamicFields.stream().anyMatch(dealStatisticFields::contains);
        final DynamicFieldsData dynamicFieldsData = new DynamicFieldsData();

        if (dealStatisticsRequired) {
            final Map<Long, Collection<Deal>> dealsMap = DealManager.getInstance().fetchDealsBySource(orgId, entityIds, source);
            final Map<String, Map<String, Object>> entityRows = new HashMap<>();

            if (!dealsMap.isEmpty()) {

                entityIds.forEach(id -> {

                    if (dealsMap.containsKey(id)) {
                        final Map<String, Object> entityRow = new HashMap<>();
                        final Collection<Deal> deals = dealsMap.get(id);

                        for (final String fieldName : dynamicFields) {

                            switch (fieldName) {
                                case ListDynamicFields.DEALS ->
                                        loadDealStatesToEntityMap(fieldName, deals, Deal.State.OPEN, entityRow);
                                case ListDynamicFields.CLOSED_DEALS -> {
                                    final Set<Deal.State> states = Set.of(Deal.State.WON, Deal.State.LOST);
                                    final Collection<Deal> filteredDeals = deals.stream().filter(deal -> states.contains(deal.getState())).toList();

                                    if (!filteredDeals.isEmpty()) {
                                        entityRow.put(fieldName, convertDealsToMap(filteredDeals));
                                    }
                                }
                                case ListDynamicFields.WON_DEALS ->
                                        loadDealStatesToEntityMap(fieldName, deals, Deal.State.WON, entityRow);
                                case ListDynamicFields.LOST_DEALS ->
                                        loadDealStatesToEntityMap(fieldName, deals, Deal.State.LOST, entityRow);
                            }
                        }
                        entityRows.put(id.toString(), entityRow);
                    }
                });
            }
            dynamicFieldsData.addAll(entityRows);
        }
        return dynamicFieldsData;
    }

    /**
     * <p>
     * Filters the provided collection of deals by the specified state and loads them to the entity map
     * </p>
     *
     * @param fieldName Represents the field name
     * @param deals     Which contains the collections of {@link Deal}
     * @param state     Represents the {@link Deal.State}
     * @param entityMap The {@link Map} representing the entity data to be updated.
     */
    private void loadDealStatesToEntityMap(final String fieldName, final Collection<Deal> deals,
                                           final Deal.State state, final Map<String, Object> entityMap) {
        final Collection<Deal> filteredDeals = deals.stream().filter(deal -> Objects.equals(state, deal.getState())).toList();

        if (!filteredDeals.isEmpty()) {
            entityMap.put(fieldName, convertDealsToMap(filteredDeals));
        }
    }

    /**
     * <p>
     * Converts a collection of deals into a statistics response map
     * </p>
     *
     * @param deals Which contains the collection of deals
     * @return A {@link Map} containing the deal statistics, including the count and a list of deals with detailed information
     */
    private Map<String, Object> convertDealsToMap(final Collection<Deal> deals) {
        final Map<String, Object> dealStatisticsResponse = new HashMap<>();
        final Collection<Object> dealList = new ArrayList<>();
        final String idKey = TransformerFields.ID.getName();

        for (final Deal deal : deals) {
            final Map<String, Object> dealMap = new HashMap<>();
            final Monetary monetary = deal.getDealValue();

            dealMap.put(idKey, deal.id());
            dealMap.put(ListDynamicFields.TITLE, deal.getTitle());
            dealMap.put(ListDynamicFields.DEAL_VALUE, String.format(DEAL_VALUE_FORMAT,
                    monetary.getCurrency().getSymbol(), baseCurrencyService.applyPrecision(monetary.orgId(), monetary.getValue())));
            dealMap.put(ListDynamicFields.PIPELINE_STAGE, deal.getPipelineStage().getName());

            dealList.add(dealMap);
        }
        dealStatisticsResponse.put(ListDynamicFields.DEAL_LIST, dealList);

        return dealStatisticsResponse;
    }

    /**
     * <p>
     * Fetches the collections of deal statistics fields
     * </p>
     *
     * @return The collections of deal statistics fields
     */
    private Collection<String> getDealStatisticFields() {
        return Set.of(ListDynamicFields.DEALS, ListDynamicFields.CLOSED_DEALS, ListDynamicFields.WON_DEALS, ListDynamicFields.LOST_DEALS);
    }
}
