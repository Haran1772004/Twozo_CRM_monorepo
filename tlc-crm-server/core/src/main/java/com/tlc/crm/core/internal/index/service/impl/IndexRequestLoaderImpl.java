package com.tlc.crm.core.internal.index.service.impl;

import com.tlc.crm.core.api.contact.manager.CompanyIndexManager;
import com.tlc.crm.core.api.contact.manager.ContactIndexManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.product.manager.ProductIndexManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.internal.index.service.IndexRequestLoader;

import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.search.api.manager.AbstractIndexManager;
import com.tlc.search.SearchService;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;
import com.tlc.search.request.index.single.write.UpsertRequest;
import com.tlc.search.service.provider.SearchServiceProvider;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Facilitates to load requests in respective indexes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class IndexRequestLoaderImpl implements IndexRequestLoader {

    private final SearchService searchService;

    private IndexRequestLoaderImpl() {
        searchService = SearchServiceProvider.get();
    }

    private static class Instance {
        private static final IndexRequestLoaderImpl INSTANCE = new IndexRequestLoaderImpl();
    }

    /**
     * <p>
     * Gets the {@link IndexRequestLoaderImpl} Instance
     * </p>
     *
     * @return {@link IndexRequestLoaderImpl}
     */
    public static IndexRequestLoaderImpl getInstance() {
        return IndexRequestLoaderImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads the company requests into company {@link BulkWriteRequest}.
     * </p>
     *
     * @param company          Holds the details of the {@link Company}.
     * @param bulkWriteRequest {@link BulkWriteRequest} to executes the actions of the {@link Company}.
     */
    @Override
    public void loadCompanyIndexRequest(final Long orgId, final Company company, final BulkWriteRequest bulkWriteRequest) {
        loadEntityIndexRequest(orgId, company, CompanyIndexManager.getInstance(), bulkWriteRequest);
    }

    /**
     * <p>
     * Loads the contact requests into contact {@link BulkWriteRequest}.
     * </p>
     *
     * @param contact          Holds the details of the {@link Contact}.
     * @param bulkWriteRequest {@link BulkWriteRequest} to executes the actions of the {@link Contact}.
     */
    @Override
    public void loadContactIndexRequest(final Long orgId, final Contact contact, final BulkWriteRequest bulkWriteRequest) {
        loadEntityIndexRequest(orgId, contact, ContactIndexManager.getInstance(), bulkWriteRequest);
    }

    /**
     * <p>
     * Loads Product's requests into product {@link BulkWriteRequest}
     * </p>
     *
     * @param productDetailsHolder An entity that holds Product Details.
     * @param bulkWriteRequest     {@link BulkWriteRequest} to executes the actions of the {@link Product}.
     */
    public void loadProductIndexRequests(final Long orgId, final ProductDetailsHolder productDetailsHolder,
                                         final BulkWriteRequest bulkWriteRequest) {
        final ProductIndexManager productIndexManager = ProductIndexManager.getInstance();
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetails)) {

            for (final ProductDetail productDetail : productDetails) {
                loadEntityIndexRequest(orgId, productDetail.getProduct(), productIndexManager, bulkWriteRequest);
            }
        }
    }

    /**
     * <p>
     * Loads entity's bulk write request.
     * </p>
     *
     * @param entity           {@link Entity}
     * @param indexManager     {@link AbstractIndexManager}.
     * @param bulkWriteRequest {@link BulkWriteRequest}.
     */
    private <T extends Entity> void loadEntityIndexRequest(final Long orgId, final T entity,
                                                           final AbstractIndexManager<T> indexManager,
                                                           final BulkWriteRequest bulkWriteRequest) {
        if (Objects.nonNull(entity)) {
            final Long entityId = entity.id();

            if (Objects.nonNull(orgId) && Objects.nonNull(entityId)) {
                final String entityIdAsString = String.valueOf(entityId);
                final String indexName = indexManager.getSearchIndex().getName();

                if (!searchService.getReadableService(orgId).exists(entityIdAsString, indexName)) {
                    final UpsertRequest request = UpsertRequest.get(entityIdAsString, indexName);

                    request.setDoc(indexManager.constructDoc(orgId, entity, indexName));
                    bulkWriteRequest.addInsertRequest(request);
                }
            }
        }
    }
}
