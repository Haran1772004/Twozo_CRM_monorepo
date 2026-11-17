package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.internal.async.bulk.processor.AsyncUpdateProcessor;
import com.tlc.crm.entity.fields.source.Source;

/**
 * <p>
 * Processor responsible for handling async update operations for {@link Product} entities
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
final class ProductAsyncUpdateProcessor extends AsyncUpdateProcessor<Product> {

    private final ProductManager productManager;
    private final ProductConverter productConverter;

    ProductAsyncUpdateProcessor() {
        productManager = ProductManager.getInstance();
        productConverter = ProductConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Refers the organization id
     * @param entity Refers the {@link Product} to be cloned
     * @return The cloned {@link Product}
     */
    @Override
    protected Product cloneModel(final long orgId, final Product entity) {
        final Product product = super.cloneModel(orgId, entity);

        product.setOrgId(orgId);
        product.setId(entity.id());

        return product;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Source}
     */
    protected Source getSource() {
        return Source.PRODUCT;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param product         Represents the model to be updated
     */
    @Override
    protected void mapJsonToModel(final ConverterConfig converterConfig, final Product product) {
        final JsonObject dataJson = converterConfig.dataToBeConverted();

        if (converterConfig.isPartialData()) {
            productConverter.setDataToProduct(converterConfig, product, dataJson);
        } else {
            productConverter.convert(converterConfig, product);
        }
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