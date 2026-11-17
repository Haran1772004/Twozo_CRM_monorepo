package com.tlc.crm.core.internal.index.service;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.internal.index.service.impl.IndexRequestLoaderImpl;
import com.tlc.search.request.index.single.write.bulk.BulkWriteRequest;

/**
 * <p>
 * Facilitates to load requests in respective indexes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface IndexRequestLoader {

    /**
     * <p>
     * Gets the {@link IndexRequestLoader}.
     * </p>
     *
     * @return {@link IndexRequestLoader}.
     */
    static IndexRequestLoader get() {
        return IndexRequestLoaderImpl.getInstance();
    }

    /**
     * <p>
     * Loads the company requests into company {@link BulkWriteRequest}.
     * </p>
     *
     * @param company          Holds the details of the {@link Company}.
     * @param bulkWriteRequest {@link BulkWriteRequest} to executes the actions of the {@link Company}.
     */
    void loadCompanyIndexRequest(final Long orgId, final Company company, final BulkWriteRequest bulkWriteRequest);

    /**
     * <p>
     * Loads the contact requests into contact {@link BulkWriteRequest}.
     * </p>
     *
     * @param contact          Holds the details of the {@link Contact}.
     * @param bulkWriteRequest {@link BulkWriteRequest} to executes the actions of the {@link Contact}.
     */
    void loadContactIndexRequest(final Long orgId, final Contact contact, final BulkWriteRequest bulkWriteRequest);

    /**
     * <p>
     * Loads Product's requests into product {@link BulkWriteRequest}
     * </p>
     *
     * @param productDetailsHolder An entity that holds Product Details.
     * @param bulkWriteRequest     {@link BulkWriteRequest} to executes the actions of the {@link Product}.
     */
    void loadProductIndexRequests(final Long orgId, final ProductDetailsHolder productDetailsHolder, final BulkWriteRequest bulkWriteRequest);
}
