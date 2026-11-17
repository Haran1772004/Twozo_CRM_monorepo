package com.tlc.crm.core.internal.index.service;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.internal.index.service.impl.IndexDataFetcherImpl;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Facilitates to fetch required data for indexes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface IndexDataFetcher {

    /**
     * <p>
     * Gets the {@link IndexDataFetcher} Instance
     * </p>
     *
     * @return {@link IndexDataFetcher}
     */
    static IndexDataFetcher get() {
        return IndexDataFetcherImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link Contact} for the given id.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @param id    To fetch the {@link Contact}.
     * @return {@link Contact}.
     */
    Contact fetchContact(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the {@link Company} for the given id.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @param id    To fetch the {@link Company}.
     * @return {@link Company}.
     */
    Company fetchCompany(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the {@link Deal} for the given id.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @param id    To fetch the {@link Deal}.
     * @return {@link Deal}.
     */
    Deal fetchDeal(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the {@link Pipeline} for the given id.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @param id    To fetch the {@link Pipeline}.
     * @return {@link Pipeline}.
     */
    Pipeline fetchPipeline(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the {@link PipelineStage} for the given id.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @param id    To fetch the {@link PipelineStage}.
     * @return {@link PipelineStage}.
     */
    PipelineStage fetchPipelineStage(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the {@link ActivityType} for the given id.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @param id    To fetch the {@link ActivityType}.
     * @return {@link ActivityType}.
     */
    ActivityType fetchActivityType(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the values of the entity.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param entityId Refers the id of the entity.
     * @param source   Refers the type of the entity.
     * @return Entity value.
     */
    Map.Entry<String, Object> fetchEntityValue(final Long orgId, final Long entityId, final String source);

    /**
     * <p>
     * Gets the contacts permission related ids for the mapped activity id.
     * </p>
     *
     * @param orgId             Represents the organisation id.
     * @param contactIds        Contact ids.
     * @return Map of contact's owner and territory id field and ids pair.
     */
    Map<String, Collection<Long>> getContactPermissionRelatedIdsMap(final Long orgId, final Collection<Long> contactIds);
}