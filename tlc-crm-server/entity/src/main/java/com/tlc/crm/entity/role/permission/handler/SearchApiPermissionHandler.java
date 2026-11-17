package com.tlc.crm.entity.role.permission.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.entity.internal.status.CommonSearchErrorCodes;
import com.tlc.crm.entity.role.permission.handler.request.RelatedApiHandlerRequest;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.search.fields.SearchApiKeys;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Handles source-related authorization for Search related APIs.
 *
 * @author Ajay.
 * @version 1.0.
 */
public class SearchApiPermissionHandler extends RelatedApiPermissionHandler {

    private SearchApiPermissionHandler() {
    }

    private static final class Instance {
        private static final SearchApiPermissionHandler SEARCH_API_PERMISSION_HANDLER = new SearchApiPermissionHandler();
    }

    /**
     * <p>
     * Gets {@link SearchApiPermissionHandler}.
     * </p>
     *
     * @return {@link SearchApiPermissionHandler}.
     */
    public static SearchApiPermissionHandler getInstance() {
        return SearchApiPermissionHandler.Instance.SEARCH_API_PERMISSION_HANDLER;
    }

    /**
     * {@inheritDoc}
     *
     * @param apiHandlerRequest Represents the request of the API handler.
     */
    @Override
    public void handle(final RelatedApiHandlerRequest apiHandlerRequest) {
        final VerificationRequest<?> verificationRequest = apiHandlerRequest.getVerificationRequest();
        final VerificationResponse verificationResponse = apiHandlerRequest.getVerificationResponse();
        final JsonObject requestJson = verificationRequest.requestJson();
        final JsonObject data = requestJson.getJsonObject(CommonSearchFields.DATA);
        final Long orgId = verificationRequest.orgId();
        final Long roleId = LoggedInUserInfo.getUser().getRoleId();

        switch (apiHandlerRequest.getVerificationRequest().apiDetails().type()) {
            case SearchApiKeys.SEARCH -> validateSearchRequests(data, orgId, roleId, verificationResponse);
        }
    }

    /**
     * <<p>
     * Validates search requests.
     * </p>
     *
     * @param data                 Search's nam api data.
     * @param orgId                An organization's id.
     * @param roleId               Role's id.
     * @param verificationResponse {@link VerificationResponse}.
     */
    private void validateSearchRequests(final JsonObject data, final Long orgId, final Long roleId,
                                        final VerificationResponse verificationResponse) {
        final boolean isWebReq = data.containsKey(CommonSearchFields.INDEX);
        final String message = "i18n.role.permission.error.entity.privilege.restricted";
        final Consumer<SubCategory.Type> permissionVerifier = (type) -> {
            final RolePermission rolePermission = rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, type, Action.Type.VIEW);

            if (!rolePermission.isGranted()) {
                verificationResponse.addError(message);
            }
        };

        if (isWebReq) {
            final String index = data.getString(CommonSearchFields.INDEX);

            if (index.isEmpty()) {
                throw ErrorCode.get(CommonSearchErrorCodes.INDEX_VALUE_CANNOT_BE_EMPTY, "i18n.index.value.cannot.be.empty");
            } else if (!Objects.equals(SearchApiKeys.ALL, index)) {
                switch (index) {
                    case CommonSearchFields.CONTACT -> permissionVerifier.accept(SubCategory.Type.CONTACTS);
                    case CommonSearchFields.COMPANY -> permissionVerifier.accept(SubCategory.Type.COMPANIES);
                    case CommonSearchFields.DEAL -> permissionVerifier.accept(SubCategory.Type.DEALS);
                    case CommonSearchFields.PRODUCT -> permissionVerifier.accept(SubCategory.Type.PRODUCTS);
                }
            }
        } else {
            final JsonArray indexes = data.getJsonArray(CommonSearchFields.INDEXES);
            final int size = indexes.size();

            if (NumericalConstant.ZERO >= size) {
                throw ErrorCode.get(CommonSearchErrorCodes.INDEX_VALUE_CANNOT_BE_EMPTY, "i18n.index.value.cannot.be.empty");
            }
            final Map<String, SubCategory.Type> sourceMap = Map.of(CommonSearchFields.CONTACT, SubCategory.Type.CONTACTS,
                    CommonSearchFields.COMPANY, SubCategory.Type.COMPANIES, CommonSearchFields.DEAL, SubCategory.Type.DEALS,
                    CommonSearchFields.PRODUCT, SubCategory.Type.PRODUCTS);

            for (int index = NumericalConstant.ZERO; index < size; index++) {
                final String indexName = indexes.getString(index);

                if (sourceMap.containsKey(indexName)) {
                    permissionVerifier.accept(sourceMap.get(indexName));
                }
            }
        }
    }
}