package com.tlc.crm.core.list;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.model.ListPreviewFetchRequest;
import com.tlc.crm.entity.list.model.ListPreviewResponse;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the service for retrieving preview details to be loaded into the deal list response
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class DealListPreviewRetriever extends AbstractListPreviewRetriever {

    private final DealManager dealManager;

    private DealListPreviewRetriever() {
        this.dealManager = DealManager.getInstance();
    }

    private static class Instance {
        private static final DealListPreviewRetriever DEAL_LIST_PREVIEW_RETRIEVER =  new DealListPreviewRetriever();
    }

    /**
     * <p>
     * Gets the instance of {@link DealListPreviewRetriever}
     * </p>
     *
     * @return The instance of {@link DealListPreviewRetriever}
     */
    public static DealListPreviewRetriever getInstance() {
        return Instance.DEAL_LIST_PREVIEW_RETRIEVER;
    }

    /**
     * {@inheritDoc}
     *
     * @param listPreviewFetchRequest Represents the {@link ListPreviewFetchRequest}
     * @return The optional {@link ListPreviewResponse}
     */
    @Override
    public Optional<ListPreviewResponse> fetchPreviewResponse(final ListPreviewFetchRequest listPreviewFetchRequest) {
        final Collection<Long> dealIds = listPreviewFetchRequest.entityIds();
        final DataContainer dataContainer = dealManager.getPreviewDetails(listPreviewFetchRequest.orgId(), dealIds);

        if (!dataContainer.isEmpty()) {
            return fetchContactRelatedListPreview(Source.DEAL, dealIds, dataContainer);
        }
        return Optional.empty();
    }
}