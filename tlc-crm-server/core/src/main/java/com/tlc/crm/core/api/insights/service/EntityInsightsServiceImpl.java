package com.tlc.crm.core.api.insights.service;

import com.tlc.crm.common.page.MappedEntityIdsPageHandler;
import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.core.sql.resource.MACCOMPANYACTIVITY;
import com.tlc.crm.core.sql.resource.MACCONTACTACTIVITY;
import com.tlc.crm.core.sql.resource.MACDEALACTIVITY;
import com.tlc.crm.core.sql.resource.MDOCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.core.api.insights.manager.EntityInsightsRecomputeRequestManager;
import com.tlc.crm.core.api.insights.models.EntityInsightsIdentifierType;

import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * <p>
 * Default implementation of {@link EntityInsightsService}.
 * </p>
 *
 * @author Mohamed Yasar K
 */
final class EntityInsightsServiceImpl implements EntityInsightsService {

    private final EntityInsightsRecomputeRequestManager entityInsightsRecomputeRequestManager;

    private EntityInsightsServiceImpl() {
        this.entityInsightsRecomputeRequestManager = EntityInsightsRecomputeRequestManager.getInstance();
    }

    private static final class Holder {
        private static final EntityInsightsServiceImpl INSTANCE = new EntityInsightsServiceImpl();
    }

    public static EntityInsightsService getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * <p>
     * Returns the {@link OrgDataStore} for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return The {@link OrgDataStore} for the organization.
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processRelatedEntity(final long orgId, final EntityInsightsGroup group,
                                     final Source refSource, final Collection<Long> sourceKeys,
                                     final BiConsumer<Source, Collection<Long>> consumer) {
        if (Objects.isNull(sourceKeys) || sourceKeys.isEmpty()) {
            return;
        }
        final OrgDataStore dataStore = orgDataStore(orgId);

        switch (group) {
            case ACTIVITY -> {
                MappedEntityIdsPageHandler.of(dataStore, MACCONTACTACTIVITY.TABLE,
                                MACCONTACTACTIVITY.ACTIVITY_ID, MACCONTACTACTIVITY.CONTACT_ID, sourceKeys)
                        .forEach(batch -> consumer.accept(Source.CONTACT, batch));

                MappedEntityIdsPageHandler.of(dataStore, MACCOMPANYACTIVITY.TABLE,
                                MACCOMPANYACTIVITY.ACTIVITY_ID, MACCOMPANYACTIVITY.COMPANY_ID, sourceKeys)
                        .forEach(batch -> consumer.accept(Source.COMPANY, batch));

                MappedEntityIdsPageHandler.of(dataStore, MACDEALACTIVITY.TABLE,
                                MACDEALACTIVITY.ACTIVITY_ID, MACDEALACTIVITY.DEAL_ID, sourceKeys)
                        .forEach(batch -> consumer.accept(Source.DEAL, batch));
            }
            case DEAL -> {
                if (refSource == Source.DEAL) {
                    MappedEntityIdsPageHandler.of(dataStore, MDOCOMPANYFIELDS.TABLE,
                                    MDOCOMPANYFIELDS.ENTITY_ID, MDOCOMPANYFIELDS.COMPANY_ID, sourceKeys)
                            .forEach(batch -> consumer.accept(Source.COMPANY, batch));
                }
            }
            default -> {}
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enqueueMappedEntitiesForDeletedActivities(final long orgId, final Collection<Long> activityIds) {
        if (Objects.isNull(activityIds) || activityIds.isEmpty()) {
            return;
        }
        final OrgDataStore dataStore = orgDataStore(orgId);

        MappedEntityIdsPageHandler.of(dataStore, MACCONTACTACTIVITY.TABLE,
                        MACCONTACTACTIVITY.ACTIVITY_ID, MACCONTACTACTIVITY.CONTACT_ID, activityIds)
                .forEach(batch -> entityInsightsRecomputeRequestManager.add(orgId, Source.CONTACT, batch,
                        EntityInsightsGroup.ACTIVITY, EntityInsightsIdentifierType.ENTITY));

        MappedEntityIdsPageHandler.of(dataStore, MACCOMPANYACTIVITY.TABLE,
                        MACCOMPANYACTIVITY.ACTIVITY_ID, MACCOMPANYACTIVITY.COMPANY_ID, activityIds)
                .forEach(batch -> entityInsightsRecomputeRequestManager.add(orgId, Source.COMPANY, batch,
                        EntityInsightsGroup.ACTIVITY, EntityInsightsIdentifierType.ENTITY));

        MappedEntityIdsPageHandler.of(dataStore, MACDEALACTIVITY.TABLE,
                        MACDEALACTIVITY.ACTIVITY_ID, MACDEALACTIVITY.DEAL_ID, activityIds)
                .forEach(batch -> entityInsightsRecomputeRequestManager.add(orgId, Source.DEAL, batch,
                        EntityInsightsGroup.ACTIVITY, EntityInsightsIdentifierType.ENTITY));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enqueueMappedEntitiesForDeletedEntityConversations(final long orgId, final Collection<Long> entityConversationIds) {
        if (Objects.isNull(entityConversationIds) || entityConversationIds.isEmpty()) {
            return;
        }
        final OrgDataStore dataStore = orgDataStore(orgId);

        MappedEntityIdsPageHandler.of(dataStore, OMECCONTACTCONVERSATIONS.TABLE,
                        OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID, OMECCONTACTCONVERSATIONS.CONTACT_ID, entityConversationIds)
                .forEach(batch -> entityInsightsRecomputeRequestManager.add(orgId, Source.CONTACT, batch,
                        EntityInsightsGroup.EMAIL, EntityInsightsIdentifierType.ENTITY));

        MappedEntityIdsPageHandler.of(dataStore, OMECCOMPANYCONVERSATIONS.TABLE,
                        OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID, OMECCOMPANYCONVERSATIONS.COMPANY_ID, entityConversationIds)
                .forEach(batch -> entityInsightsRecomputeRequestManager.add(orgId, Source.COMPANY, batch,
                        EntityInsightsGroup.EMAIL, EntityInsightsIdentifierType.ENTITY));

        MappedEntityIdsPageHandler.of(dataStore, OMECDEALCONVERSATIONS.TABLE,
                        OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID, OMECDEALCONVERSATIONS.DEAL_ID, entityConversationIds)
                .forEach(batch -> entityInsightsRecomputeRequestManager.add(orgId, Source.DEAL, batch,
                        EntityInsightsGroup.EMAIL, EntityInsightsIdentifierType.ENTITY));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void enqueueMappedEntitiesForDeletedDeals(final long orgId, final Collection<Long> dealIds) {
        if (Objects.isNull(dealIds) || dealIds.isEmpty()) {
            return;
        }
        final OrgDataStore dataStore = orgDataStore(orgId);

        MappedEntityIdsPageHandler.of(dataStore, MDOCOMPANYFIELDS.TABLE,
                        MDOCOMPANYFIELDS.ENTITY_ID, MDOCOMPANYFIELDS.COMPANY_ID, dealIds)
                .forEach(batch -> entityInsightsRecomputeRequestManager.add(orgId, Source.COMPANY, batch,
                        EntityInsightsGroup.DEAL, EntityInsightsIdentifierType.ENTITY));
    }
}