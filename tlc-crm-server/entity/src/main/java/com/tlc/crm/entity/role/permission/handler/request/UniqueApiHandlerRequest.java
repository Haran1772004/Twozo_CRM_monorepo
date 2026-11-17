package com.tlc.crm.entity.role.permission.handler.request;

import com.tlc.crm.entity.role.permission.handler.UniqueApiPermissionHandler;
import com.tlc.crm.security.permission.handler.request.ApiHandlerRequest;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

/**
 * <p>
 * Holds the request data of the {@link UniqueApiPermissionHandler}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class UniqueApiHandlerRequest extends ApiHandlerRequest {

    private final Category.Type categoryType;
    private final SubCategory.Type subcategoryType;

    public UniqueApiHandlerRequest(final Category.Type categoryType, final SubCategory.Type subcategoryType,
                                   final VerificationRequest<?> request, final VerificationResponse response) {
        super(request, response);
        this.categoryType = categoryType;
        this.subcategoryType = subcategoryType;
    }

    public Category.Type getCategoryType() {
        return categoryType;
    }

    public SubCategory.Type getSubcategoryType() {
        return subcategoryType;
    }
}
