package com.tlc.crm.core.page;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;

import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.api.product.model.Variation;

import com.tlc.crm.entity.dropdown.DropdownCriteria;

/**
 * Represents the page list for the {@code Variation} fetch using pagination.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class VariationPageHandler extends PageHandler<Variation> {

    private static final int DEFAULT_LIMIT = 20;

    private final VariationManager variationManager;
    private final DropdownCriteria dropdownCriteria;
    private final Long productId;

    public VariationPageHandler(final Long productId, final int start, final int limit, final DropdownCriteria dropdownCriteria) {
        super(start, limit);
        this.recordsPerPage = Math.min(limit, DEFAULT_LIMIT);
        this.productId = productId;
        this.dropdownCriteria = dropdownCriteria;
        this.variationManager = VariationManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Page} of expired history.
     */
    @Override
    public Page<Variation> fetchPageData() {
        return variationManager.getProductVariations(getOrgId(), productId, getRecordStartingIndex(), getRecordsPerPage(), dropdownCriteria);
    }

    /**
     * {@inheritDoc}
     *
     * @return The expired history count.
     */
    @Override
    public long totalRecordsCount() {
        return variationManager.getVariationCount(getOrgId(), productId, dropdownCriteria);
    }
}
