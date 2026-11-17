package com.tlc.crm.core.action.deal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

/**
 * This class is an abstract implementation of a product-related action
 *
 * @author KarthickV
 * @version 1.0
 */
public abstract class AbstractProductAction extends AuthorizationController {

    /**
     * {@inheritDoc}
     *
     * @param request Refers the {@link CrmRequest}.
     * @param crmResponse Refers the {@link CrmResponse}.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        pProcess(request, crmResponse);
    }

    /**
     * {@inheritDoc}
     *
     * @param request Refers the {@link CrmRequest}.
     */
    protected final void validateHttpRequest(final CrmRequest request) {
        if (!SourceSettingsService.getInstance().isSourceEnabled(request.orgId(), Product.ENTITY_TYPE)) {
            throw ErrorCode.get(DealErrorCodes.UNABLE_TO_LINK_PRODUCTS_WITH_DEAL_AS_PRODUCT_IS_DISABLED);
        }
    }

    /**
     * <p>
     *     Handles the request and response.
     * </p>
     *
     * @param request Refers the {@link CrmRequest}.
     * @param crmResponse Refers the {@link CrmResponse}.
     */
    public abstract void pProcess(final CrmRequest request, final CrmResponse crmResponse);
}
