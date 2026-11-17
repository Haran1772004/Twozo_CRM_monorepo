package com.tlc.crm.core.action.transformer;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;

import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Transforms the company entities into required forms.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "company", type = Transformer.Type.ENTITY)
public class CompanyTransformer extends AbstractModelTransformer {

    private final DisplayPictureManager displayPictureManager;
    private final CompanyConverter companyConverter;

    public CompanyTransformer() {
        displayPictureManager = DisplayPictureManager.getInstance();
        companyConverter = CompanyConverter.getInstance();
    }

    /**
     * <p>
     * Transforms the company entities into the required forms.
     * </p>
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transform(final TransformerRequest transformerRequest) {
        final Map<Long, Map<String, Object>> companies = new LinkedHashMap<>();
        final Collection<Map<String, Object>> records = transformerRequest.records();
        final long orgId = transformerRequest.orgId();
        final TransformerResponse transformerResponse = new TransformerResponse();
        final Collection<Long> userIds = new HashSet<>();
        final String salesOwner = TransformerFields.SALES_OWNER.getName();
        final String userId = TransformerFields.USER_ID.getName();
        final String createdBy = TransformerFields.CREATED_BY.getName();
        final String updatedBy = TransformerFields.UPDATED_BY.getName();
        final String tagsKey = TransformerFields.TAGS.getName();
        final String nameKey = StoreSubType.NAME.getAliasName();
        final String followersKey = TransformerFields.FOLLOWERS.getName();
        final String idKey = TransformerFields.ID.getName();
        final String companyIdKey = TransformerFields.COMPANY_ID.getName();
        final Collection<FieldInfo> companyPersistentFields = FieldService.getSecureInstance().getPersistentFieldsBySource(orgId, TransformerFields.COMPANY.getName());
        final Collection<FieldInfo> ownerFields = Objects.isNull(companyPersistentFields) ? Collections.emptySet() : companyPersistentFields.stream()
                .filter(fieldInfo -> Objects.equals(StoreType.OWNER, fieldInfo.storeType())).collect(Collectors.toUnmodifiableSet());
        final Map<Long, Map<String, Boolean>> permissionAccess = getPermissionAccess(orgId, records);

        for (final Map<String, Object> companyRow : records) {
            final Map<String, Object> users = new HashMap<>();
            final Long pkValue = (Long) companyRow.get(idKey);

            if (permissionAccess.containsKey(pkValue)) {
                companyRow.putAll(permissionAccess.get(pkValue));
            }
            groupUserIdsFields(followersKey, companyRow, companies);
            groupFields(tagsKey, companyRow, companies, nameKey);
            collectUserIds(followersKey, companyRow, userIds, users);
            collectUserIds(salesOwner, companyRow, userIds, users);
            collectUserIds(createdBy, companyRow, userIds, users);
            collectUserIds(updatedBy, companyRow, userIds, users);
            mapLinkProperties(pkValue, companyRow, companyIdKey);

            for (final FieldInfo customOwnerField : ownerFields) {
                final String idAsString = customOwnerField.idAsString();

                if (Objects.nonNull(companyRow.get(idAsString))) {
                    collectUserIds(idAsString, companyRow, userIds, users);
                }
            }

            if (!userIds.isEmpty()) {
                companyRow.put(userId, users);
            }
        }
        transformerResponse.setResults(companies.values());
        transformerResponse.setUserIds(userIds);

        return transformerResponse;
    }

    /**
     * {@inheritDoc}
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transformDataForMobile(final TransformerRequest transformerRequest) {
        final TransformerResponse transformerResponse = transform(transformerRequest);
        final Collection<Map<String, Object>> results = transformerResponse.getResults();
        final String idKey = TransformerFields.ID.getName();
        final Collection<Long> companyIds = results.stream().map(companyRow -> (Long) companyRow.get(idKey)).collect(Collectors.toSet());

        if (!companyIds.isEmpty()) {
            final Map<Long, DisplayPicture> companyProfilePictures = displayPictureManager
                    .getDisplayPicture(transformerRequest.orgId(), companyIds, Source.COMPANY, ProfilePicture.Quality.THUMBNAIL);

            loadProfilePicturesIntoCompanies(results, companyProfilePictures);
        }

        return transformerResponse;
    }

    /**
     * <p>
     * Groups the given sub fields by field
     * </p>
     *
     * @param fieldName  Name of the field to be transformed
     * @param companyRow Retrieved company entities
     * @param companies  Holds the details of the transformed entities
     * @param subFields  Name of the subfields to be grouped
     */
    private void groupFields(final String fieldName, final Map<String, Object> companyRow,
                             final Map<Long, Map<String, Object>> companies, final String... subFields) {
        final String idKey = TransformerFields.ID.getName();
        final Long pkValue = (Long) companyRow.get(idKey);
        final Long fieldId = (Long) companyRow.get(fieldName);

        if (companies.containsKey(pkValue)) {
            final Map<String, Object> company = companies.get(pkValue);

            if (Objects.nonNull(company) && company.containsKey(fieldName)) {
                final Object value = company.get(fieldName);

                if (value instanceof Collection) {
                    final Collection<Map<String, Object>> existingFields = new ArrayList<>((Collection<Map<String, Object>>) value);
                    final Collection<Object> fieldValues = existingFields.stream().map(existingValue -> existingValue.get(idKey))
                            .collect(Collectors.toUnmodifiableSet());

                    if (!fieldValues.contains(fieldId)) {
                        final Map<String, Object> groupedFields = groupFields(fieldName, companyRow, subFields);

                        if (Objects.nonNull(groupedFields)) {
                            existingFields.add(groupedFields);
                            company.put(fieldName, existingFields);
                        }
                    }
                } else {
                    final Map<String, Object> groupedFields = groupFields(fieldName, companyRow, subFields);

                    company.put(fieldName, Objects.isNull(groupedFields) ? null : List.of(groupedFields));
                }
            }
        } else if (companyRow.containsKey(fieldName) && Objects.nonNull(fieldId)) {
            final Map<String, Object> groupedFields = groupFields(fieldName, companyRow, subFields);

            companyRow.put(fieldName, Objects.isNull(groupedFields) ? null : List.of(groupedFields));
            companies.put(pkValue, companyRow);
        } else {
            companies.put(pkValue, companyRow);
        }
    }

    /**
     * <p>
     * Groups the given sub fields by field.
     * </p>
     *
     * @param fieldName  Name of the field to be transformed
     * @param subFields  Name of the subfields to be grouped
     * @param companyRow Retrieved company entities
     */
    private Map<String, Object> groupFields(final String fieldName, final Map<String, Object> companyRow,
                                            final String[] subFields) {
        final Optional<Object> optValue = Optional.ofNullable(companyRow.get(fieldName));

        return optValue.map(fieldId -> {
            final Map<String, Object> fields = new HashMap<>();
            final String columnFormat = Formats.COLUMN_FORMAT.get();

            for (final String subField : subFields) {
                final Object value = companyRow.remove(String.format(columnFormat, fieldName, subField));

                if (Objects.nonNull(value)) {
                    fields.put(subField, value);
                }
            }
            fields.put(TransformerFields.ID.getName(), fieldId);

            return fields;
        }).orElseGet(() -> null);
    }

    /**
     * <p>
     * Loads the profile pictures into companies
     * </p>
     *
     * @param companies              Holds the details of the companies
     * @param companyProfilePictures Holds the details of the profile pictures
     */
    private void loadProfilePicturesIntoCompanies(final Collection<Map<String, Object>> companies,
                                                  final Map<Long, DisplayPicture> companyProfilePictures) {
        final String thumbnailKey = TransformerFields.THUMBNAIL.getName();
        final String companyKey = TransformerFields.COMPANY.getName();
        final String idKey = TransformerFields.ID.getName();

        for (final Map<String, Object> company : companies) {
            final Long companyId = (Long) company.get(idKey);
            final DisplayPicture displayPicture = companyProfilePictures.get(companyId);

            if (Objects.nonNull(displayPicture)) {
                final JsonObject thumbnailJson = companyConverter.convertThumbnailToJson(companyId, companyKey, displayPicture);

                company.put(thumbnailKey, thumbnailJson.toMap());
            }
        }
    }
}
