package com.tlc.crm.core.api.insights.cdc;

import com.tlc.cdc.core.event.CdcEvent;
import com.tlc.cdc.core.models.AggregatedCdcEventRequest;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.cdc.CrmCdcEventHandler;
import com.tlc.crm.core.api.analytics.general.keys.AnalyticsCdcKeys;
import com.tlc.crm.core.api.insights.manager.EntityInsightsRecomputeRequestManager;
import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.core.api.insights.models.EntityInsightsIdentifierType;
import com.tlc.crm.core.api.insights.models.EntityInsightsRecomputeBatchRequest;
import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * CDC event handler for the Entity Insights pipeline.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@CdcEvent(event = "insights")
public class InsightsCrmCdcHandler extends CrmCdcEventHandler {

    private static final Collection<String> ACTIVITY_INSIGHT_COLUMNS = Set.of(
            OMACACTIVITY.IS_DONE, OMACACTIVITY.START_TIME, OMACACTIVITY.TYPE_ID
    );

    private static final Collection<String> DEAL_INSIGHT_COLUMNS = Set.of(
            MDODEAL.STATE, MDODEAL.CONTACT_ID
    );

    private static final Collection<String> EMAIL_CONVERSATION_INSIGHT_COLUMNS = Set.of(
            OMEMCONVERSATIONS.IS_ACTIVE, OMEMCONVERSATIONS.IS_DELETED, OMEMCONVERSATIONS.MESSAGE_CATEGORY
    );

    private static final String INSERT_OPERATION = "c";
    private static final String UPDATE_OPERATION = "u";

    private final EntityInsightsRecomputeRequestManager recomputeRequestManager;

    public InsightsCrmCdcHandler() {
        this.recomputeRequestManager = EntityInsightsRecomputeRequestManager.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    protected void secureHandle(final AggregatedCdcEventRequest aggregatedCdcEventRequest) {
        LOGGER.info("Insights CDC: Processing aggregated CDC event. ID: {}", aggregatedCdcEventRequest.id());

        final JsonArray events = aggregatedCdcEventRequest.data().getJsonArray(AnalyticsCdcKeys.EVENT);
        final int size = events.size();
        final EntityInsightsRecomputeBatchRequest entityInsightsRecomputeBatchRequest = new EntityInsightsRecomputeBatchRequest();

        for (int index = 0; index < size; index++) {
            try {
                processEvent(events.getJsonObject(index), entityInsightsRecomputeBatchRequest);
            } catch (final Exception exception) {
                LOGGER.error("Insights CDC: Error processing event at index {}. ID: {}, Reason: {}",
                        index, aggregatedCdcEventRequest.id(), exception.getMessage(), exception);
            }
        }
        entityInsightsRecomputeBatchRequest.processEvent(recomputeRequestManager);
    }

    /**
     * <p>
     * Routes a single CDC event to the appropriate handler based on the
     * operation type and table name.
     * </p>
     *
     * @param cdcEvent                            Represents the CDC event JSON object.
     * @param entityInsightsRecomputeBatchRequest Represents the batch context accumulating entries for this event.
     */
    private void processEvent(final JsonObject cdcEvent, final EntityInsightsRecomputeBatchRequest entityInsightsRecomputeBatchRequest) {
        final String operation = cdcEvent.getString(AnalyticsCdcKeys.OPERATION);
        final String table = cdcEvent.getJsonObject(AnalyticsCdcKeys.SOURCE).getString(AnalyticsCdcKeys.TABLE);
        final JsonObject after = cdcEvent.optJsonObject(AnalyticsCdcKeys.AFTER);

        if (Objects.isNull(after)) {
            return;
        }

        if (MACCONTACTACTIVITY.TABLE.equalsIgnoreCase(table)) {
            enqueueActivityMapping(Source.CONTACT,
                    after.getLong(MACCONTACTACTIVITY.ORG_ID),
                    after.getLong(MACCONTACTACTIVITY.CONTACT_ID), entityInsightsRecomputeBatchRequest);

        } else if (MACCOMPANYACTIVITY.TABLE.equalsIgnoreCase(table)) {
            enqueueActivityMapping(Source.COMPANY,
                    after.getLong(MACCOMPANYACTIVITY.ORG_ID),
                    after.getLong(MACCOMPANYACTIVITY.COMPANY_ID), entityInsightsRecomputeBatchRequest);

        } else if (MACDEALACTIVITY.TABLE.equalsIgnoreCase(table)) {
            enqueueActivityMapping(Source.DEAL,
                    after.getLong(MACDEALACTIVITY.ORG_ID),
                    after.getLong(MACDEALACTIVITY.DEAL_ID), entityInsightsRecomputeBatchRequest);

        } else if (OMACACTIVITY.TABLE.equalsIgnoreCase(table) && UPDATE_OPERATION.equals(operation)) {
            if (hasRelevantColumnsModified(cdcEvent, ACTIVITY_INSIGHT_COLUMNS)) {
                entityInsightsRecomputeBatchRequest.addEvent(after.getLong(OMACACTIVITY.ORG_ID), Source.ACTIVITY,
                        after.getLong(OMACACTIVITY.ID), EntityInsightsGroup.ACTIVITY, EntityInsightsIdentifierType.SOURCE_REF);
            }

        } else if (OMECCONTACTCONVERSATIONS.TABLE.equalsIgnoreCase(table)) {
            enqueueEmailMapping(Source.CONTACT,
                    after.getLong(OMECCONTACTCONVERSATIONS.ORG_ID),
                    after.getLong(OMECCONTACTCONVERSATIONS.CONTACT_ID), entityInsightsRecomputeBatchRequest);

        } else if (OMECCOMPANYCONVERSATIONS.TABLE.equalsIgnoreCase(table)) {
            enqueueEmailMapping(Source.COMPANY,
                    after.getLong(OMECCOMPANYCONVERSATIONS.ORG_ID),
                    after.getLong(OMECCOMPANYCONVERSATIONS.COMPANY_ID), entityInsightsRecomputeBatchRequest);

        } else if (OMECDEALCONVERSATIONS.TABLE.equalsIgnoreCase(table)) {
            enqueueEmailMapping(Source.DEAL,
                    after.getLong(OMECDEALCONVERSATIONS.ORG_ID),
                    after.getLong(OMECDEALCONVERSATIONS.DEAL_ID), entityInsightsRecomputeBatchRequest);

        } else if (MDODEAL.TABLE.equalsIgnoreCase(table)) {
            if (INSERT_OPERATION.equals(operation) || (UPDATE_OPERATION.equals(operation) && hasRelevantColumnsModified(cdcEvent, DEAL_INSIGHT_COLUMNS))) {
                enqueueDealInsights(after, entityInsightsRecomputeBatchRequest);
            }

        } else if (OMEMCONVERSATIONS.TABLE.equalsIgnoreCase(table) && UPDATE_OPERATION.equals(operation)) {
            if (hasRelevantColumnsModified(cdcEvent, EMAIL_CONVERSATION_INSIGHT_COLUMNS)) {
                entityInsightsRecomputeBatchRequest.addEmailConversationEvent(after.getLong(OMEMCONVERSATIONS.ORG_ID),
                        after.getLong(OMEMCONVERSATIONS.ID));
            }
        }
    }

    /**
     * <p>
     * Accumulates an ACTIVITY insight recompute entry for the given entity.
     * </p>
     *
     * @param source                              Represents the entity source.
     * @param orgId                               Represents the organization id.
     * @param entityId                            Represents the entity id to accumulate.
     * @param entityInsightsRecomputeBatchRequest Represents the batch context accumulating entries for this event.
     */
    private static void enqueueActivityMapping(final Source source, final long orgId, final long entityId,
                                               final EntityInsightsRecomputeBatchRequest entityInsightsRecomputeBatchRequest) {
        entityInsightsRecomputeBatchRequest.addEvent(orgId, source, entityId, EntityInsightsGroup.ACTIVITY, EntityInsightsIdentifierType.ENTITY);
    }

    /**
     * <p>
     * Accumulates an EMAIL insight recompute entry for the given entity.
     * </p>
     *
     * @param source                              Represents the entity source.
     * @param orgId                               Represents the organization id.
     * @param entityId                            Represents the entity id to accumulate.
     * @param entityInsightsRecomputeBatchRequest Represents the batch context accumulating entries for this event.
     */
    private static void enqueueEmailMapping(final Source source, final long orgId, final long entityId,
                                            final EntityInsightsRecomputeBatchRequest entityInsightsRecomputeBatchRequest) {
        entityInsightsRecomputeBatchRequest.addEvent(orgId, source, entityId, EntityInsightsGroup.EMAIL, EntityInsightsIdentifierType.ENTITY);
    }

    /**
     * <p>
     * Accumulates DEAL insight recompute entries for both the deal itself and
     * its linked contact (when present).
     * </p>
     *
     * @param after                               Represents the {@code after} image of the CDC event.
     * @param entityInsightsRecomputeBatchRequest Represents the batch context accumulating entries for this event.
     */
    private static void enqueueDealInsights(final JsonObject after,
                                            final EntityInsightsRecomputeBatchRequest entityInsightsRecomputeBatchRequest) {
        final long orgId = after.getLong(MDODEAL.ORG_ID);
        final long dealId = after.getLong(MDODEAL.ID);
        final long contactId = after.optLong(MDODEAL.CONTACT_ID, 0L);

        if (contactId > 0) {
            entityInsightsRecomputeBatchRequest.addEvent(orgId, Source.CONTACT, contactId,
                    EntityInsightsGroup.DEAL, EntityInsightsIdentifierType.ENTITY);
        }
        entityInsightsRecomputeBatchRequest.addEvent(orgId, Source.DEAL, dealId,
                EntityInsightsGroup.DEAL, EntityInsightsIdentifierType.SOURCE_REF);
    }


    /**
     * <p>
     * Returns {@code true} when any column in {@code relevantColumns} differs
     * between the {@code before} and {@code after} images of the CDC event.
     * </p>
     *
     * @param cdcEvent        Represents the CDC event JSON object.
     * @param relevantColumns Represents the set of column names to inspect.
     * @return {@code true} if a relevant column changed or the before-image is unavailable.
     */
    private static boolean hasRelevantColumnsModified(final JsonObject cdcEvent, final Collection<String> relevantColumns) {
        final JsonObject before = cdcEvent.optJsonObject(AnalyticsCdcKeys.BEFORE);
        final JsonObject after = cdcEvent.optJsonObject(AnalyticsCdcKeys.AFTER);

        if (Objects.isNull(before)) {
            return true;
        }

        for (final String relevantColumn : relevantColumns) {
            final String beforeValue = before.optString(relevantColumn, null);
            final String afterValue = after.optString(relevantColumn, null);

            if (Objects.isNull(beforeValue) || !Objects.equals(beforeValue, afterValue)) {
                return true;
            }
        }
        return false;
    }

}