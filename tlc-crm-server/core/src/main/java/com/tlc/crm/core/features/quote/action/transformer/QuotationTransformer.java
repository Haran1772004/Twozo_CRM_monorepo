package com.tlc.crm.core.features.quote.action.transformer;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;
import com.tlc.crm.core.action.transformer.AbstractModelTransformer;
import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.role.permission.util.SubCategoryTypeUtils;
import com.tlc.crm.entity.role.permission.validator.EntityPermissionValidator;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Transforms the quotation entities into required forms.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "quotation", type = Transformer.Type.ENTITY)
public class QuotationTransformer extends AbstractModelTransformer {

    /**
     * <p>
     * Transforms the quotation entities into the required forms.
     * </p>
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transform(final TransformerRequest transformerRequest) {
        final Collection<Long> userIds = new HashSet<>();
        final Collection<Long> quotationIds = new HashSet<>();
        final Collection<Map<String, Object>> quotationRows = transformerRequest.records();
        final TransformerResponse transformerResponse = new TransformerResponse();
        final String ownerField = TransformerFields.OWNER.getName();
        final String userId = TransformerFields.USER_ID.getName();

        for (final Map<String, Object> quotationRow : quotationRows) {
            final Map<String, Object> users = new HashMap<>();
            final Long quotationId = (Long) quotationRow.get(TransformerFields.ID.getName());

            collectUserIds(ownerField, quotationRow, userIds, users);

            if (Objects.nonNull(quotationId)) {
                quotationIds.add(quotationId);
            }

            if (!userIds.isEmpty()) {
                quotationRow.put(userId, users);
            }
        }
        final long orgId = transformerRequest.orgId();
        final CrmUser user = LoggedInUserInfo.getUser();
        final Long roleId = user.getRoleId();
        final RolePermissionService rolePermissionService = RolePermissionService.getInstance();
        final EntityPermissionValidator entityPermissionValidator = new EntityPermissionValidator(orgId, user.id(), roleId);
        final Map<Long, Map<Source, Collection<Long>>> companyAndContactIds = QuotationManager.getInstance()
                .getCompanyAndContactIds(orgId, quotationIds);

        for (final Map<String, Object> quotationRow : quotationRows) {
            final Long quotationId = (Long) quotationRow.get(TransformerFields.ID.getName());

            if (Objects.nonNull(quotationId)) {
                companyAndContactIds.get(quotationId).forEach((source, entityIds) -> {
                    if (Objects.nonNull(source) && Objects.nonNull(entityIds)) {
                        final StoreType storeType = findStoreType(source);
                        final SubCategory.Type subCategoryType = SubCategoryTypeUtils.getBySourceName(source.getName());
                        final RolePermission permission = rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, subCategoryType, Action.Type.ACCESS);

                        if (Objects.nonNull(storeType) && !entityPermissionValidator.validate(Set.copyOf(entityIds), storeType, permission.getScope(), false)) {
                            quotationRow.put(QuotationFields.HAS_NO_ENTITY_ACCESS, true);
                        }
                    }
                });
            }
        }

        transformerResponse.setResults(quotationRows);
        transformerResponse.setUserIds(userIds);

        return transformerResponse;
    }

    /**
     * Maps the given {@link Source} to its corresponding {@link StoreType}.
     *
     * @param source the source to map
     * @return the matching {@link StoreType}, or {@code null} if no mapping exists
     */
    private StoreType findStoreType(final Source source) {
        return switch (source) {
            case CONTACT -> StoreType.CONTACT;
            case COMPANY -> StoreType.COMPANY;
            default -> null;
        };
    }
}
