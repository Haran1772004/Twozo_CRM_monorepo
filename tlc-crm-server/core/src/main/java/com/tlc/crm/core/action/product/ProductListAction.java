package com.tlc.crm.core.action.product;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;

import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import com.tlc.web.WebAction;

@SourceAuthorizer(subCategory = SubCategory.Type.PRODUCTS, validateFieldPermission = true)
@WebAction(path = "/product/list")
public class ProductListAction extends EntityListAction {

    private final SourceSettingsService sourceSettingsService;

    public ProductListAction() {
        sourceSettingsService = SourceSettingsService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The provider name.
     */
    @Override
    protected Provider getProvider() {
        return Provider.PRODUCT_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @param request Refers the {@link CrmRequest}.
     */
    @Override
    protected final void validateHttpRequest(final CrmRequest request) {
        if (!sourceSettingsService.isSourceEnabled(request.orgId(), Product.ENTITY_TYPE)) {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_IS_DISABLED);
        }
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    protected ListType getListType() {
        return ListType.PRODUCT_LIST;
    }
}