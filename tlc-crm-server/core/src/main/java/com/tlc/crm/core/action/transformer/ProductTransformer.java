package com.tlc.crm.core.action.transformer;

import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;

import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Transforms the product entities into required forms.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "product", type = Transformer.Type.ENTITY)
public class ProductTransformer extends AbstractModelTransformer {

    /**
     * <p>
     * Transforms the product entities into the required forms.
     * </p>
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transform(final TransformerRequest transformerRequest) {
        final Collection<Long> userIds = new HashSet<>();
        final long orgId = transformerRequest.orgId();
        final Collection<Map<String, Object>> records = transformerRequest.records();
        final TransformerResponse transformerResponse = new TransformerResponse();
        final String salesOwner = TransformerFields.SALES_OWNER.getName();
        final String createdBy = TransformerFields.CREATED_BY.getName();
        final String updatedBy = TransformerFields.UPDATED_BY.getName();
        final String userId = TransformerFields.USER_ID.getName();
        final Collection<FieldInfo> productPersistentFields = FieldService.getSecureInstance().getPersistentFieldsBySource(orgId, TransformerFields.PRODUCT.getName());
        final Collection<FieldInfo> ownerFields = Objects.isNull(productPersistentFields) ? Collections.emptySet() : productPersistentFields.stream()
                .filter(fieldInfo -> Objects.equals(StoreType.OWNER, fieldInfo.storeType())).collect(Collectors.toUnmodifiableSet());

        for (final Map<String, Object> productRow : records) {
            final Map<String, Object> users = new HashMap<>();

            collectUserIds(salesOwner, productRow, userIds, users);
            collectUserIds(createdBy, productRow, userIds, users);
            collectUserIds(updatedBy, productRow, userIds, users);

            for (final FieldInfo customOwnerField : ownerFields) {
                final String idAsString = customOwnerField.idAsString();

                if (Objects.nonNull(productRow.get(idAsString))) {
                    collectUserIds(idAsString, productRow, userIds, users);
                }
            }

            if (!userIds.isEmpty()) {
                productRow.put(userId, users);
            }
        }
        transformerResponse.setResults(records);
        transformerResponse.setUserIds(userIds);

        return transformerResponse;
    }
}
