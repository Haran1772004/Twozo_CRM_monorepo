package com.tlc.crm.core.features.internal.quota.consumer;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.core.action.parser.impl.NativeParser;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.internal.quota.manager.QuotaDealCreateActionManager;
import com.tlc.crm.core.features.internal.quota.manager.QuotaDealDeleteActionManager;
import com.tlc.crm.core.features.internal.quota.manager.QuotaDealUpdateActionManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stream consumer responsible for processing Deal-related events
 * and routing them to quota background operations.
 *
 * <p><b>Purpose</b></p>
 * Listens to deal stream events (CREATE, UPDATE, DELETE) and
 * synchronizes quota tracking for both USER and TEAM quota paths.
 *
 * <p><b>Quota Qualification Rules</b></p>
 * A deal is considered for quota processing only when:
 *
 * <ul>
 *     <li><b>WON Deals:</b>
 *         <ul>
 *             <li>Deal state is WON</li>
 *             <li>Closed time falls within quota START_TIME and END_TIME</li>
 *         </ul>
 *     </li>
 *     <li><b>Forecasted (Non-WON) Deals:</b>
 *         <ul>
 *             <li>Expected close date is within quota START_TIME and END_TIME</li>
 *             <li>Forecast category is present</li>
 *         </ul>
 *     </li>
 *     <li>Pipeline must match quota pipeline criteria</li>
 *     <li>Territory must match quota territory criteria</li>
 *     <li>Product set must match quota product criteria</li>
 * </ul>
 *
 * <p><b>Event Handling Logic</b></p>
 * <ul>
 *     <li><b>CREATE:</b>
 *         Registers deal as WON or Forecasted based on its current state.
 *     </li>
 *     <li><b>UPDATE:</b>
 *         Detects state transitions (WON ↔ Forecasted),
 *         structural changes (owner, pipeline, territory, products, dates),
 *         and value changes, then performs appropriate batched quota updates.
 *     </li>
 *     <li><b>DELETE:</b>
 *         Removes deal from all associated quota records.
 *     </li>
 * </ul>
 *
 * <p><b>Update Scenarios Covered</b></p>
 * <ul>
 *     <li>WON → Forecasted transition</li>
 *     <li>Forecasted → WON transition</li>
 *     <li>Owner change</li>
 *     <li>Pipeline change</li>
 *     <li>Territory change</li>
 *     <li>Product change</li>
 *     <li>Closed time change</li>
 *     <li>Expected close date change</li>
 *     <li>Forecast category change</li>
 *     <li>Deal value or currency change</li>
 * </ul>
 *
 * <p><b>Data Integrity</b></p>
 * All structural transitions are executed via batched background operations
 * to ensure quota totals and forecast summaries remain consistent.
 *
 * <p>This consumer guarantees eventual consistency between Deal events
 * and quota tracking aggregates.</p>
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaDealEventProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaDealEventProcessor.class);

    private final DataParser parser;
    private final QuotaDealCreateActionManager quotaDealCreateActionManager;
    private final QuotaDealUpdateActionManager quotaDealUpdateActionManager;
    private final QuotaDealDeleteActionManager quotaDealDeleteActionManager;

    public QuotaDealEventProcessor() {
        parser = NativeParser.getInstance();
        quotaDealCreateActionManager = QuotaDealCreateActionManager.getInstance();
        quotaDealUpdateActionManager = QuotaDealUpdateActionManager.getInstance();
        quotaDealDeleteActionManager = QuotaDealDeleteActionManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param message stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> message) {
        final Map<?, ?> payload = new HashMap<>((Map<?, ?>) parser.deserialize(message.data()));
        final Deal deal = (Deal) payload.get(QuotaFields.MODEL);

        try {
            final Deal existingDeal = (Deal) payload.get(QuotaFields.EXISTING_MODEL);
            final Action action = (Action) payload.get(ActionFields.ACTION);

            switch (action) {
                case CREATE -> processCreate(deal);
                case UPDATE, PARTIAL_UPDATE -> processUpdate(existingDeal, deal);
                case DELETE -> quotaDealDeleteActionManager.deleteDealFromAllQuotas(deal);
            }
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception,
                    "Quota Deal Event: Failed to process quota deal event for orgId: {}, dealId: {}. Error: {}",
                    deal.orgId(), deal.id(), exception.getMessage());
        } finally {
            message.clean();
        }
    }

    /**
     * Processes quota registration for a newly created deal.
     *
     * <p>If the deal is:</p>
     * <ul>
     *     <li>WON → registers under WON quota tracking</li>
     *     <li>Forecasted → registers under forecast quota tracking</li>
     * </ul>
     *
     * @param deal newly created deal
     */
    private void processCreate(final Deal deal) {
        if (isWon(deal)) {
            quotaDealCreateActionManager.addWonDeal(deal);
        } else if (isForecasted(deal)) {
            quotaDealCreateActionManager.addForecastedDeal(deal);
        }
    }

    /**
     * Processes quota updates for an existing deal modification.
     *
     * <p>Handles the following scenarios:</p>
     * <ul>
     *     <li>WON ↔ Forecasted state transitions</li>
     *     <li>Structural changes (owner, pipeline, territory, products, dates, forecast category)</li>
     *     <li>Deal value or currency changes</li>
     * </ul>
     *
     * <p>Executes batched quota operations when structural changes occur
     * to maintain aggregate consistency.</p>
     *
     * @param existingDeal previous persisted snapshot
     * @param deal         updated deal snapshot
     */
    private void processUpdate(final Deal existingDeal, final Deal deal) {
        final boolean wasWon = isWon(existingDeal);
        final boolean isNowWon = isWon(deal);

        if (!wasWon && isNowWon) {
            if (isForecasted(existingDeal)) {
                quotaDealUpdateActionManager.executeBatchedOperations(existingDeal, deal,
                        QuotaDealUpdateActionManager.QuotaDealBatchOperation.OPEN_TO_WON);
            } else {
                quotaDealCreateActionManager.addWonDeal(deal);
            }
        } else if (wasWon && !isNowWon) {
            if (isForecasted(deal)) {
                quotaDealUpdateActionManager.executeBatchedOperations(existingDeal, deal,
                        QuotaDealUpdateActionManager.QuotaDealBatchOperation.WON_TO_OPEN);
            } else {
                quotaDealUpdateActionManager.removeWonDeal(existingDeal);
            }
        } else if (wasWon) {

            if (isOwnerChanged(existingDeal, deal) || isPipelineChanged(existingDeal, deal)
                    || isTerritoryChanged(existingDeal, deal) || isProductChanged(existingDeal, deal) ||
                    isCloseTimeChanged(existingDeal, deal)) {
                quotaDealUpdateActionManager.executeBatchedOperations(existingDeal, deal,
                        QuotaDealUpdateActionManager.QuotaDealBatchOperation.WON_REMAINS_UNCHANGED);
                return;
            }

            if (isDealValueChanged(existingDeal, deal)) {
                quotaDealUpdateActionManager.adjustWonDealValue(existingDeal, deal);
            }
        } else {
            processForecastedDealChanges(existingDeal, deal);
        }
    }

    /**
     * Handles quota adjustments for deals that remain in a non-WON (forecasted) state.
     *
     * <p>Manages:</p>
     * <ul>
     *     <li>Forecasted → Non-forecasted removal</li>
     *     <li>Non-forecasted → Forecasted addition</li>
     *     <li>Structural attribute changes</li>
     *     <li>Deal value adjustments</li>
     * </ul>
     *
     * @param existingDeal previous persisted snapshot
     * @param deal         updated deal snapshot
     */
    private void processForecastedDealChanges(final Deal existingDeal, final Deal deal) {
        final boolean wasForecasted = isForecasted(existingDeal);
        final boolean isNowForecasted = isForecasted(deal);

        if (wasForecasted && !isNowForecasted) {
            quotaDealUpdateActionManager.removeForecastedDeal(existingDeal);
            return;
        }

        if (!wasForecasted && isNowForecasted) {
            quotaDealCreateActionManager.addForecastedDeal(deal);
            return;
        }

        if (wasForecasted) {
            if (isOwnerChanged(existingDeal, deal) || isTerritoryChanged(existingDeal, deal) || isPipelineChanged(existingDeal, deal)
                    || isProductChanged(existingDeal, deal) || isExpectedCloseDateChanged(existingDeal, deal) || isForecastCategoryChanged(existingDeal, deal)) {
                quotaDealUpdateActionManager.executeBatchedOperations(existingDeal, deal,
                        QuotaDealUpdateActionManager.QuotaDealBatchOperation.OPEN_REMAINS_UNCHANGED);
                return;
            }

            if (isDealValueChanged(existingDeal, deal)) {
                quotaDealUpdateActionManager.adjustForecastedDealValue(existingDeal, deal);
            }
        }
    }

    /**
     * Determines whether the deal qualifies as a WON deal
     * for quota processing.
     *
     * <p>A deal is considered WON if:</p>
     * <ul>
     *     <li>State is {@code Deal.State.WON}</li>
     *     <li>Closed time is not null</li>
     * </ul>
     *
     * @param deal deal to evaluate
     * @return true if the deal is WON and eligible for won quota tracking
     */
    private boolean isWon(final Deal deal) {
        return Deal.State.WON == deal.getState() && Objects.nonNull(deal.getClosedTime());
    }

    /**
     * Determines whether the deal qualifies as a forecasted
     * (non-WON) deal for quota tracking.
     *
     * <p>A deal is considered forecasted if:</p>
     * <ul>
     *     <li>Expected close date is present</li>
     *     <li>Forecast category is present</li>
     * </ul>
     *
     * @param deal deal to evaluate
     * @return true if eligible for forecast quota tracking
     */
    private boolean isForecasted(final Deal deal) {
        return Objects.nonNull(deal.getExpectedCloseDate()) && Objects.nonNull(deal.getForecastCategory());
    }

    /**
     * Checks whether the deal owner has changed between
     * the existing and updated snapshots.
     *
     * @param existingDeal previous deal snapshot
     * @param deal         updated deal snapshot
     * @return true if ownerId differs
     */
    private boolean isOwnerChanged(final Deal existingDeal, final Deal deal) {
        return !Objects.equals(existingDeal.getOwnerId(), deal.getOwnerId());
    }

    /**
     * Checks whether the deal value or currency has changed
     * between two deal snapshots.
     *
     * <p>Returns true if:</p>
     * <ul>
     *     <li>One value is null and the other is not</li>
     *     <li>Monetary value differs</li>
     *     <li>Currency differs</li>
     * </ul>
     *
     * @param existingDeal previous deal snapshot
     * @param deal         updated deal snapshot
     * @return true if value or currency changed
     */
    private boolean isDealValueChanged(final Deal existingDeal, final Deal deal) {
        final DealValue existingValue = existingDeal.getDealValue();
        final DealValue currentValue = deal.getDealValue();

        if (Objects.isNull(existingValue) && Objects.isNull(currentValue)) {
            return false;
        }

        if (Objects.isNull(existingValue) || Objects.isNull(currentValue)) {
            return true;
        }
        return !Objects.equals(existingValue.getValue(), currentValue.getValue())
                || !Objects.equals(existingValue.getCurrency(), currentValue.getCurrency());
    }

    /**
     * Checks whether the territory association has changed
     * between two deal snapshots.
     *
     * @param existingDeal previous deal snapshot
     * @param deal         updated deal snapshot
     * @return true if territory differs
     */
    private boolean isTerritoryChanged(final Deal existingDeal, final Deal deal) {
        final Territory existingTerritory = existingDeal.getTerritory();
        final Territory currentTerritory = deal.getTerritory();

        if (Objects.isNull(existingTerritory) && Objects.isNull(currentTerritory)) {
            return false;
        }

        if (Objects.isNull(existingTerritory) || Objects.isNull(currentTerritory)) {
            return true;
        }
        return !Objects.equals(existingTerritory.id(), currentTerritory.id());
    }

    /**
     * Checks whether the pipeline association has changed
     * between two deal snapshots.
     *
     * @param existingDeal previous deal snapshot
     * @param deal         updated deal snapshot
     * @return true if pipeline differs
     */
    private boolean isPipelineChanged(final Deal existingDeal, final Deal deal) {
        final Pipeline existingPipeline = existingDeal.getPipeline();
        final Pipeline currentPipeline = deal.getPipeline();

        if (Objects.isNull(existingPipeline) && Objects.isNull(currentPipeline)) {
            return false;
        }

        if (Objects.isNull(existingPipeline) || Objects.isNull(currentPipeline)) {
            return true;
        }
        return !Objects.equals(existingPipeline.id(), currentPipeline.id());
    }

    /**
     * Checks whether the associated product set has changed
     * between two deal snapshots.
     *
     * <p>Compares product IDs from both snapshots.</p>
     *
     * @param existingDeal previous deal snapshot
     * @param deal         updated deal snapshot
     * @return true if product set differs
     */
    private boolean isProductChanged(final Deal existingDeal, final Deal deal) {
        final ProductDetailsHolder existingHolder = existingDeal.getProductDetailsHolder();
        final ProductDetailsHolder currentHolder = deal.getProductDetailsHolder();

        if (Objects.isNull(existingHolder) && Objects.isNull(currentHolder)) {
            return false;
        }

        if (Objects.isNull(existingHolder) || Objects.isNull(currentHolder)) {
            return true;
        }
        final Set<Long> existingProductIds = existingHolder.getProductDetails()
                .stream()
                .map(pd -> pd.getProduct().id())
                .collect(Collectors.toSet());

        final Set<Long> currentProductIds = currentHolder.getProductDetails()
                .stream()
                .map(pd -> pd.getProduct().id())
                .collect(Collectors.toSet());

        return !Objects.equals(existingProductIds, currentProductIds);
    }

    /**
     * Checks whether the closed time has changed
     * between two deal snapshots.
     *
     * @param existingDeal previous deal snapshot
     * @param deal         updated deal snapshot
     * @return true if closedTime differs
     */
    private boolean isCloseTimeChanged(final Deal existingDeal, final Deal deal) {
        return !Objects.equals(existingDeal.getClosedTime(), deal.getClosedTime());
    }

    /**
     * Checks whether the expected close date has changed
     * between two deal snapshots.
     *
     * @param existingDeal previous deal snapshot
     * @param deal         updated deal snapshot
     * @return true if expectedCloseDate differs
     */
    private boolean isExpectedCloseDateChanged(final Deal existingDeal, final Deal deal) {
        return !Objects.equals(existingDeal.getExpectedCloseDate(), deal.getExpectedCloseDate());
    }

    /**
     * Checks whether the forecast category has changed
     * between two deal snapshots.
     *
     * @param existingDeal previous deal snapshot
     * @param deal         updated deal snapshot
     * @return true if forecast category differs
     */
    private boolean isForecastCategoryChanged(final Deal existingDeal, final Deal deal) {
        final ForecastCategory existingCategory = existingDeal.getForecastCategory();
        final ForecastCategory currentCategory = deal.getForecastCategory();

        if (Objects.isNull(existingCategory) && Objects.isNull(currentCategory)) {
            return false;
        }

        if (Objects.isNull(existingCategory) || Objects.isNull(currentCategory)) {
            return true;
        }
        return !Objects.equals(existingCategory.id(), currentCategory.id());
    }
}