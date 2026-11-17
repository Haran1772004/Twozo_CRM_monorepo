package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.insights.service.EntityInsightsService;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncDeleteProcessor;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;
import com.tlc.crm.notification.app.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>
 * Processor responsible for handling async delete operations for {@link Deal} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
class DealAsyncDeleteProcessor extends AsyncDeleteProcessor<Deal> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DealAsyncDeleteProcessor.class);

    private final DealManager dealManager;
    private final EntityInsightsService entityInsightsService;

    DealAsyncDeleteProcessor() {
        dealManager = DealManager.getInstance();
        entityInsightsService = EntityInsightsService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AbstractConfigManager} instance
     */
    @Override
    protected AbstractConfigManager<Deal> getAbstractConfigManager() {
        return dealManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param deal Refers the {@code Deal}.
     */
    @Override
    protected void loadSummaryDetails(final Deal deal) {
        dealManager.loadSummaryDetails(deal.orgId(), deal);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Collects the company IDs linked to this deal via {@code MDOCompanyFields}
     * <em>before</em> the delete so the mapping rows are still readable, then
     * proceeds with the standard delete flow.
     * </p>
     */
    @Override
    protected void process(final AsyncActionRequest asyncActionRequest) {
        final long orgId = asyncActionRequest.orgId();
        final long userId = asyncActionRequest.userId();
        final long modelId = asyncActionRequest.modelId();

        try {
            final AbstractConfigManager<Deal> abstractConfigManager = getAbstractConfigManager();
            final Deal model = abstractConfigManager.get(orgId, modelId);

            entityInsightsService.enqueueMappedEntitiesForDeletedDeals(orgId, List.of(modelId));
            loadSummaryDetails(model);
            final SecureEventData secureEventData = new SecureEventData(orgId, userId);

            secureEventData.setValues(List.of(model));
            secureEventData.setPreviousValues(List.of(model));
            secureEventData.setAction(Action.DELETE);
            eventDataHandler.handleEvents(secureEventData, Event.Type.PRE);

            abstractConfigManager.delete(model);

            eventDataHandler.handleEvents(secureEventData, Event.Type.POST);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Bulk Action: Bulk delete failed. Org ID: {}, User ID: {}, Id: {}, Error: {}",
                    orgId, userId, modelId, exception.getMessage());
        }
    }
}
