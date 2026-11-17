package com.tlc.crm.core.page;

import com.tlc.commons.page.Page;
import com.tlc.crm.common.page.MobilePageHandler;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.ProductDealHolder;
import com.tlc.crm.entity.dropdown.DropdownCriteria;

/**
 * <p>
 * Represents the page handler for the {@link ProductDealHolder}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ProductDealPageHandler extends MobilePageHandler<ProductDealHolder> {

    private final ProductManager productManager;
    private DropdownCriteria dropdownCriteria;

    public ProductDealPageHandler(final int start, final int limit) {
        recordStartingIndex = start;
        recordsPerPage = limit;
        productManager = ProductManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Page<ProductDealHolder>}
     */
    @Override
    public Page<ProductDealHolder> fetchPageData() {
        final Page<ProductDealHolder> page = new Page<>();

        page.setData(productManager.getProductDealHolder(getOrgId(), getRecordStartingIndex(), getRecordsPerPage(), dropdownCriteria));

        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return the total count
     */
    @Override
    public long totalRecordsCount() {
        return productManager.getTotalCount(getOrgId(), dropdownCriteria);
    }

    public void setDropdownCriteria(final DropdownCriteria dropdownCriteria) {
        this.dropdownCriteria = dropdownCriteria;
    }
}
