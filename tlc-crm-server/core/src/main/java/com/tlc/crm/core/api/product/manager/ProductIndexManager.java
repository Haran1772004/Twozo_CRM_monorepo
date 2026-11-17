package com.tlc.crm.core.api.product.manager;

import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.entity.search.api.manager.AbstractIndexManager;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides the way to manage the search related actions of {@link Product}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class ProductIndexManager extends AbstractIndexManager<Product> {

    private static final Collection<String> RELATED_FIELDS = List.of(ProductFields.NAME, ProductFields.PRODUCT_CODE,
            CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.TERRITORY_ID, CommonSearchFields.OWNER_ID);

    private static class Instance {
        private static final ProductIndexManager INSTANCE = new ProductIndexManager();
    }

    private ProductIndexManager() {
    }

    public static ProductIndexManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchIndex getSearchIndex() {
        return SearchIndex.PRODUCT;
    }

    /**
     * <p>
     * Updates related fields.
     * </p>
     *
     * @param indexDoc       A map holding values from Respective Index.
     * @param constructedDoc A map holding constructed values.
     */
    @Override
    protected void updateRelatedFields(final Map<String, Object> indexDoc, final Map<String, Object> constructedDoc) {

        for (final String relatedField : RELATED_FIELDS) {
            indexDoc.put(relatedField, constructedDoc.getOrDefault(relatedField, null));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValuesIntoModel(final Product product) {
        final Long ownerId = product.getOwnerId();

        if (Objects.isNull(ownerId)) {
            final ProductManager productManager = ProductManager.getInstance();
            final Table table = productManager.getTable();

            product.setOwnerId(orgDataStore(product.orgId()).get(
                    table, product.id(), List.of(table.getColumn(MPOPRODUCT.OWNER_ID))).get(MPOPRODUCT.OWNER_ID));
        }
    }

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param product {@link Product}.
     * @param doc     Holds the entity data to be indexed.
     */
    protected void loadPermissionRelatedFieldsInDoc(final Long orgId, final Product product, final Map<String, Object> doc) {
    }

    /**
     * {@inheritDoc}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }
}