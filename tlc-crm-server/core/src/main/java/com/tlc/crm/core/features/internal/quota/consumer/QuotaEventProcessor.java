package com.tlc.crm.core.features.internal.quota.consumer;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.core.action.parser.impl.NativeParser;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.manager.internal.UserQuotaManager;
import com.tlc.crm.core.features.api.quota.model.Quota;
import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.TeamQuota;
import com.tlc.crm.core.features.api.quota.model.TeamUserQuota;
import com.tlc.crm.core.features.api.quota.model.UserQuota;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.notification.app.Event;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A stream listener which listens to the events related to the quota and sends the quota related events to the respective topic.
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaEventProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaEventProcessor.class);

    private final DataParser dataParser;
    private final DealManager dealManager;
    private final EventDataHandler eventDataHandler;
    private final UserQuotaManager userQuotaManager;
    private final Executor executor;

    public QuotaEventProcessor(final Executor executor) {
        dataParser = NativeParser.getInstance();
        dealManager = DealManager.getInstance();
        eventDataHandler = EventDataHandler.getInstance();
        userQuotaManager =  UserQuotaManager.getInstance();
        this.executor = executor;
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message which contains the data related to the quota event.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final Map<?, ?> data = new HashMap<>((Map<?, ?>) dataParser.deserialize(streamMessage.data()));
        final QuotaHolder quotaHolder = (QuotaHolder) data.get(QuotaFields.MODEL);
        final Long orgId = quotaHolder.orgId();
        final Action action = (Action) data.get(ActionFields.ACTION);

        executor.execute(()-> {
            try {
                if (Objects.equals(action, Action.CREATE)) {
                    final Set<Long> userIds;
                    final Quota quota;

                    if (quotaHolder.hasTeamQuota()) {
                        final TeamQuota teamQuota = quotaHolder.getTeamQuota();

                        userIds = teamQuota.getTeamUserQuotas().stream()
                                .filter(TeamUserQuota::isEnabled)
                                .map(teamUserQuota -> teamUserQuota.getTeamUser().getCrmUser().id())
                                .collect(Collectors.toSet());

                        quota = teamQuota;
                    } else {
                        final UserQuota userQuota = quotaHolder.getUserQuota();
                        userIds = userQuota.getCrmUsers().stream()
                                .map(CrmUser::id)
                                .collect(Collectors.toSet());

                        quota = userQuota;
                    }
                    processQuotaDeals(orgId, userIds, quota);
                } else if (Objects.equals(action, Action.UPDATE)) {

                    if (quotaHolder.hasUserQuota()) {
                        final UserQuota userQuota = quotaHolder.getUserQuota();
                        final QuotaHolder existingQuotaHolder = (QuotaHolder) data.get(QuotaFields.EXISTING_MODEL);
                        final QuotaMetric quotaMetric = userQuota.getQuotaMetric();

                        if (!Objects.equals(existingQuotaHolder.getUserQuota().getQuotaMetric(), quotaMetric)) {
                            userQuotaManager.changeQuotaMetricData(orgId, quotaHolder.id(), quotaMetric);
                        }
                    }
                }
            } catch (Exception e) {
                LoggerUtil.logError(LOGGER, e, "Quota: Failed to process quota event message. orgId: {}, reason: {}", orgId, e.getMessage());
            } finally {
                streamMessage.clean();
            }
        });
    }

    /**
     * Processes the quota deals based on the given organization ID, user IDs and quota details.
     *
     * @param orgId   The organization ID for which the quota deals need to be processed.
     * @param userIds The set of user IDs associated with the quota.
     * @param quota   The quota details based on which the deals need to be processed.
     */
    private void processQuotaDeals(final Long orgId, final Set<Long> userIds, final Quota quota) {

        if (userIds.isEmpty()) {
            return;
        }
        final Collection<Product> products = quota.getProducts();
        final Collection<Territory> territories = quota.getTerritories();
        final Collection<Pipeline> pipelines = quota.getPipelines();
        final boolean isPipelineAbsent = Objects.isNull(pipelines);
        final boolean isTerritoryAbsent = Objects.isNull(territories);
        final boolean isProductAbsent = Objects.isNull(products);
        final Collection<Long> pipelineIds = isPipelineAbsent ? Collections.emptySet() : pipelines.stream().map(Pipeline::id).collect(Collectors.toSet());
        final Collection<Long> territoryIds = isTerritoryAbsent ? Collections.emptySet() : territories.stream().map(Territory::id).collect(Collectors.toSet());
        final Collection<Long> productIds = isProductAbsent ? Collections.emptySet() : products.stream().map(Product::id).collect(Collectors.toSet());
        final Collection<Long> dealIds = dealManager.getDealIds(orgId, userIds, quota.getStartTime().getTimeStamp(), quota.getEndTime().getTimeStamp(),
                pipelineIds, territoryIds, productIds, Collections.emptySet());

        if (!dealIds.isEmpty()) {

            for (final Collection<Long> batchedDealIds : JavaUtil.splitCollection(dealIds, NumericalConstant.TEN)) {
                final Collection<Deal> deals = dealManager.get(orgId, batchedDealIds);
                final SecureEventData secureEventData = new SecureEventData(orgId, quota.getCreatedBy().id());

                for (final Deal deal : deals) {

                    if (isPipelineAbsent) {
                        deal.setPipeline(null);
                    }

                    if (isTerritoryAbsent) {
                        deal.setTerritory(null);
                    }

                    if (isProductAbsent) {
                        deal.setProductDetailsHolder(null);
                    }
                }
                secureEventData.setValues(deals);
                secureEventData.setAction(Action.CREATE);
                eventDataHandler.handleEvent(CoreEvent.QUOTA_DEAL_EVENT, secureEventData, Event.Type.POST);
            }
        }
    }
}
