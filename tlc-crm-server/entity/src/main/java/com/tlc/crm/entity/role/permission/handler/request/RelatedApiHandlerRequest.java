package com.tlc.crm.entity.role.permission.handler.request;

import com.tlc.crm.entity.role.permission.handler.RelatedApiPermissionHandler;

import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.validator.PermissionValidator;
import com.tlc.crm.security.permission.handler.request.ApiHandlerRequest;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Holds the request data of the {@link RelatedApiPermissionHandler}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RelatedApiHandlerRequest extends ApiHandlerRequest {

    private final Category.Type categoryType;
    private final Map<PermissionValidator.Type, PermissionValidator> permissionValidatorMap;

    public RelatedApiHandlerRequest(final Category.Type categoryType, final Collection<? extends PermissionValidator> permissionValidators,
                                    final VerificationRequest<?> request, final VerificationResponse response) {
        super(request, response);
        this.categoryType = categoryType;
        this.permissionValidatorMap = new HashMap<>();

        for (final PermissionValidator permissionValidator : permissionValidators) {
            this.permissionValidatorMap.put(permissionValidator.getType(), permissionValidator);
        }
    }

    public Category.Type getCategoryType() {
        return categoryType;
    }

    /**
     * Gets the appropriate {@link PermissionValidator} associated with the given {@link PermissionValidator.Type}
     *
     * @param permissionValidatorType Represents the permission validator type
     * @param <T>                     Represents the type of the {@link PermissionValidator}
     * @return the relevant {@link PermissionValidator}
     */
    public <T extends PermissionValidator> Optional<T> getPermissionValidator(final PermissionValidator.Type permissionValidatorType) {
        return permissionValidatorMap.containsKey(permissionValidatorType)
                ? Optional.of((T) permissionValidatorMap.get(permissionValidatorType)) : Optional.empty();
    }
}
