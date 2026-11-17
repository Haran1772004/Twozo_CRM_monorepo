package com.tlc.crm.core.features.internal.dataimport.builder.model;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;

import java.util.Objects;
import java.util.Optional;

/**
 * Builder that can construct the {@link Product} for importing
 *
 * @author sundar
 * @version 1.0
 * @see AbstractModelBuilder
 */
public class ProductBuilder implements ModelBuilder<Product> {

    private static final Source SOURCE = Source.PRODUCT;
    private final ProductConverter productConverter;

    private ProductBuilder() {
        this.productConverter = ProductConverter.getInstance();
    }

    private static final class Instance {
        private static final ProductBuilder INSTANCE = new ProductBuilder();
    }

    /**
     * Gets the instance of {@link ProductBuilder}
     *
     * @return {@link ProductBuilder}
     */
    public static ProductBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param index      Represents the index.
     * @return The constructed object of type {@link Product} using the data from the DataHolder for importing
     */
    @Override
    public Optional<Product> build(final DataHolder dataHolder, final int index) {
        final JsonObject productJson = dataHolder.sourceRecordContainer().get(index).get(Source.PRODUCT);

        if (Objects.nonNull(productJson)) {
            return Optional.of(productConverter.convert(new ConverterConfig(dataHolder.orgId(), dataHolder.userId(), dataHolder.locale(),
                    false, productJson, true), productJson));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param index      Represents the index.
     * @param product    Represents the existing model that will be updated with the latest data.
     * @return {@link Product} loaded using the data from the {@link DataHolder} for import
     */
    @Override
    public Optional<Product> build(final DataHolder dataHolder, final int index, final Product product) {
        final JsonObject productJson = dataHolder.sourceRecordContainer().get(index).get(SOURCE);

        if (Objects.nonNull(productJson)) {
            productConverter.setDataToProduct(new ConverterConfig(dataHolder.orgId(), dataHolder.userId(), dataHolder.locale(),
                    false, productJson, true), product, productJson);
        }
        return Optional.of(product);
    }
}