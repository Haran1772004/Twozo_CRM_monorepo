package com.tlc.crm.core.features.internal.dataimport.loader.source;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.converter.ProductConverter;

import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.builder.model.ProductBuilder;

import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.AbstractModelLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * Loader to load the {@link Product} models into the {@link SourceModelContainer}
 *
 * @author sundar
 * @version 2.0
 */
public final class ProductLoader extends AbstractModelLoader<Product> {

    private final ProductBuilder productBuilder;
    private final ProductManager productManager;
    private final ProductConverter productConverter;

    private ProductLoader() {
        super(Source.PRODUCT);
        this.productBuilder = ProductBuilder.getInstance();
        this.productManager = ProductManager.getInstance();
        this.productConverter = ProductConverter.getInstance();
    }

    private static final class Instance {
        private static final ProductLoader PRODUCT_LOADER = new ProductLoader();
    }

    /**
     * Gets the instance of {@link ProductLoader}
     *
     * @return {@link ProductLoader} instance
     */
    public static ProductLoader getInstance() {
        return Instance.PRODUCT_LOADER;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder   Which holds the data to be converted to {@link Product} and load into the {@link SourceModelContainer}
     * @param errorTracker Tracks the errors that arise while loading the {@link Product} into the {@link SourceModelContainer}
     */
    @Override
    public void load(final DataHolder dataHolder, final ErrorTracker errorTracker) {
        final SourceRecordContainer sourceRecordContainer = dataHolder.sourceRecordContainer();
        final Map<FieldInfo, Map<Object, SourceRecord>> indexedProductData = sourceRecordContainer.indexData(source);
        final Map<Integer, Collection<Integer>> mergedIndexMap = sourceRecordContainer.getMergedIndicesWithinTheSheet(source);
        final Map<Integer, Product> convertedProducts = new HashMap<>();

        loadIndicesInfo(sourceRecordContainer, dataHolder.sourceModelContainer());
        loadDistinctValues(dataHolder.orgId(), sourceRecordContainer, errorTracker.getImportId());

        if (!indexedProductData.isEmpty()) {
            final Map<String, FieldInfo> dealFields = importFieldService.groupDefaultFieldsByAliasName(dataHolder.orgId(), source);
            final Map<Object, SourceRecord> entityIdRecordMap =  indexedProductData.get(dealFields.get(ProductFields.ID));

            if (!entityIdRecordMap.isEmpty()) {
                loadUpdatableModels(dataHolder, convertedProducts, entityIdRecordMap, errorTracker);
            }
        }
        loadModels(dataHolder, mergedIndexMap, convertedProducts, errorTracker);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ProductBuilder}
     */
    @Override
    protected ProductBuilder getModelBuilder() {
        return productBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link ProductConverter}
     */
    @Override
    protected EntityConverter<Product> getConverter() {
        return productConverter;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Represents the organisation ID
     * @param id                   Represents the ID
     * @param sourceModelContainer Represents the {@link SourceModelContainer}
     * @return  The Optional {@link Product}
     */
    @Override
    protected Optional<Product> get(final long orgId, final long id, final SourceModelContainer sourceModelContainer) {
        final DataContainer dataContainer = sourceModelContainer.getImportDataContainerHolder().getRelatedDataContainer(source, id);
        final Table table = Table.get(MPOPRODUCT.TABLE_NAME);
        final Row row = dataContainer.getRow(table, new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        if (Objects.isNull(row)) {
            throw ErrorCode.get(ImportErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found");
        }
        return Optional.of(productManager.convertRowToModel(row, dataContainer));
    }
}