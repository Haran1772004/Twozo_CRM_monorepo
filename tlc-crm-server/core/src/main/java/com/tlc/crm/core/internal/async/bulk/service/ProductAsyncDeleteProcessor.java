package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncDeleteProcessor;

/**
 * <p>
 * Processor responsible for handling async delete operations for {@link Product} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
class ProductAsyncDeleteProcessor extends AsyncDeleteProcessor<Product> {

    private final ProductManager productManager;

    ProductAsyncDeleteProcessor() {
        productManager = ProductManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link AbstractConfigManager} instance
     */
    @Override
    protected AbstractConfigManager<Product> getAbstractConfigManager() {
        return productManager;
    }
}
