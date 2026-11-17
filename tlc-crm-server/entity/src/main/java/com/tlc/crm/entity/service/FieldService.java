package com.tlc.crm.entity.service;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.internal.FieldServiceImpl;
import com.tlc.crm.entity.service.internal.SecureFieldService;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides an abstract way to deal with {@link FieldInfo}.
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FieldService {

    /**
     * Gets the instance of {@link FieldService}.
     *
     * @return The {@link FieldService}
     */
    static FieldService getInstance() {
        return FieldServiceImpl.getInstance();
    }

    /**
     * Gets the instance of secure {@link FieldService}.
     *
     * @return The {@link FieldService}
     */
    static FieldService getSecureInstance() {
        return getSecureInstance(false);
    }

    /**
     * Gets the instance of secure {@link FieldService}.
     *
     * @return The {@link FieldService}
     */
    static FieldService getSecureInstance(final boolean isStrictlyFetchSecureInstance) {
        if (!isStrictlyFetchSecureInstance && LoggedInUserInfo.hasSecurityContext()) {
            final RolePermission rolePermission = LoggedInUserInfo.getSecurityContext().getRolePermission();

            if (Objects.nonNull(rolePermission)) {
                final Optional<Source> optionalSource = findSource(rolePermission.getSubCategory().getType());

                if (optionalSource.map(Source::isPrimarySource).orElse(false)
                        && Action.Type.EDIT == rolePermission.getAction().getType()) {
                    return getInstance();
                }
            }
        }
        return SecureFieldService.getInstance();
    }

    /**
     * <p>
     * Fetches the default entity available for the specified entity sources
     * </p>
     *
     * @param formSources Represents the source name.
     * @return Collection of entity
     */
    Collection<FieldInfo> getDefaultFieldsBySource(final Collection<String> formSources);

    /**
     * <p>
     * Fetches the default entity and the entity available for the organization by source name
     * </p>
     *
     * @param orgId         Organization id
     * @param entitySources Source names list
     * @return Collection of entity
     */
    Collection<FieldInfo> getFieldsBySource(final Long orgId, final String... entitySources);

    /**
     * <p>
     * Fetches the default entity and the entity available for the organization by source name
     * </p>
     *
     * @param orgId         Organization id
     * @param entitySources Source names list
     * @return Collection of entity
     */
    Collection<FieldInfo> getFieldsBySource(final Long orgId, final Collection<String> entitySources);


    /**
     * <p>
     * Fetches the default entity and the entity available for an organization by fields
     * </p>
     *
     * @param fields Fields list
     * @return A map that holds the field type as key and the appropriate entity as value
     */
    Map<Integer, Collection<FieldInfo>> groupAllFieldsByType(final Collection<FieldInfo> fields);

    /**
     * <p>
     * Fetches the default entity and the entity available for an organization by custom fields
     * </p>
     *
     * @param fields Fields list
     * @return A map that holds the field type as key and the appropriate entity as value
     */
    Map<Integer, Collection<FieldInfo>> groupAllCustomFieldsByType(final Collection<FieldInfo> fields);

    /**
     * <p>
     * Fetches the default entity and the entity available for an organization by source names
     * </p>
     *
     * @param orgId         Organization id
     * @param entitySources Source names
     * @return A map that holds the field type as key and the appropriate entity as value
     */
    Map<Integer, Collection<FieldInfo>> groupAllFieldsByType(final Long orgId, final String... entitySources);

    /**
     * <p>
     * Fetches the default entity and the entity available for an organization by source name
     * </p>
     *
     * @param orgId         Organization id
     * @param entitySources Source names
     * @return A map that holds the field type as key and the appropriate entity as value
     */
    Map<Integer, Collection<FieldInfo>> groupAllFieldsByType(final Long orgId, final Collection<String> entitySources);

    /**
     * <p>
     * Fetches the entity available for an organization
     * </p>
     *
     * @param orgId Organization id
     * @return Collection of entity
     */
    Collection<FieldInfo> getOrgFields(final Long orgId);

    /**
     * <p>
     * Fetches the entity available for an organization
     * </p>
     *
     * @param orgId Organization id
     * @return A map that holds the source as key and the source entity as value
     */
    Map<String, Collection<FieldInfo>> getOrgFieldsBySource(final Long orgId);

    /**
     * <p>
     * Fetches the specified entity available of an organization
     * </p>
     *
     * @param orgId Organization id
     * @param ids   FieldNameHolder id
     * @return A map that holds the field id as key and the field info as value
     */
    Map<Long, FieldInfo> getFieldsById(final Long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Fetches the {@link FieldInfo} for given source name and field id
     * </p>
     *
     * @param sourceName Source name
     * @param fieldId    Field id
     * @return Field
     */
    FieldInfo getField(final String sourceName, final Long fieldId);

    /**
     * <p>
     * Fetches the {@link FieldInfo} for given org id and field id
     * </p>
     *
     * @param orgId   Organization id
     * @param fieldId Field id
     * @return Field details
     */
    FieldInfo getField(final Long orgId, final Long fieldId);

    /**
     * <p>
     * Fetches the collection of fields grouped by source name for the given organization id and source names
     * </p>
     *
     * @param orgId         Organization id
     * @param entitySources Source names
     * @return A map that holds the source name as key and the appropriate entity as value
     */
    Map<String, Collection<FieldInfo>> groupAllFieldsBySource(final Long orgId, final String... entitySources);

    /**
     * <p>
     * Fetches the collection of fields grouped by source name for the given organization id and source names
     * </p>
     *
     * @param orgId       Organization id
     * @param sourceNames Source names
     * @return A map that holds the source name as key and the appropriate entity as value
     */
    Map<String, Collection<FieldInfo>> groupAllFieldsBySource(final Long orgId, final Collection<String> sourceNames);

    /**
     * <p>
     * Fetches the default entity of a source by using the aliasName
     * </p>
     *
     * @param sourceName Source name
     * @param aliasName  Alias names of entity
     * @return A map that holds the alias name as key and the respective field as value
     */
    Map<String, FieldInfo> groupDefaultFieldsByAliasName(final String sourceName, final String... aliasName);

    /**
     * <p>
     * Fetches the default entity of a source that matches the one of the given alias names
     * </p>
     *
     * @param sourceName Source name
     * @param aliasNames Alias name of entity
     * @return A map that holds the alias name as key and the respective field as value
     */
    Map<String, FieldInfo> groupDefaultFieldsByAliasName(final String sourceName, final Collection<String> aliasNames);

    /**
     * <p>
     * Fetches the default entity of the source that has the alias name
     * </p>
     *
     * @param sourceName Source name
     * @return A map that holds the alias name as key and the respective field as value
     */
    Map<String, FieldInfo> groupDefaultFieldsByAliasName(final String sourceName);

    /**
     * <p>
     * Fetches the default entity of the source that has the alias name and organization specific configurations
     * </p>
     *
     * @param sourceName Source name
     * @return A map that holds the alias name as key and the respective field as value
     */
    Map<String, FieldInfo> groupDefaultFieldsByAliasName(final Long orgId, final String sourceName);

    /**
     * <p>
     * Fetches all the default fields and organization fields.
     * </p>
     *
     * @param orgId Organization id.
     * @return Collection of Field details.
     */
    Collection<FieldInfo> getAllFields(final Long orgId);

    /**
     * <p>
     * Fetches the source persistent fields for an organization
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param sourceName Represents the source name
     * @return the {@link Collection} of field details
     */
    Collection<FieldInfo> getPersistentFieldsBySource(final Long orgId, final String sourceName);

    /**
     * <p>
     * Fetches the default entity of the source that has the alias name and organization specific configurations
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param sourceName Represents the source name
     * @param aliasNames Represents the {@link Collection} of aliasNames
     * @return A map that holds the alias name as key and the respective field as value
     */
    Map<String, FieldInfo> groupDefaultFieldsByAliasName(final long orgId, final String sourceName, final Collection<String> aliasNames);

    /**
     * <p>
     * Gets the collection of {@link FieldInfo} based on the field ids
     * </p>
     *
     * @param fieldIds Contains the collection of field ids
     * @return The collection of {@link FieldInfo}
     */
    Collection<FieldInfo> getDefaultFieldsByFieldId( final Collection<Long> fieldIds );

    /**
     * <p>
     * Gets the fieldInfo object.
     * </p>
     *
     * @param fieldId Represents the field id.
     * @return The {@link FieldInfo}
     */
    FieldInfo getDefaultFieldById(final Long fieldId);

    /**
     * <p>
     * Fetches the default entity of a source by using the aliasName
     * </p>
     *
     * @param source    Represents the {@link Source}
     * @param aliasName Represents the alias names of entity
     * @return the {@link FieldInfo}
     */
    Optional<FieldInfo> getDefaultFieldByAliasName(final Source source, final String aliasName);

    /**
     * Fetches the default entity of a source by using the internal name.
     *
     * @param source       Refers the source name.
     * @param internalName Refers the internal name.
     * @return The {@code FieldInfo}.
     */
    Optional<FieldInfo> getDefaultFieldByInternalName(final Source source, final String internalName);

    /**
     * Fetches the default entity of a source by using the internal name.
     *
     * @param orgId        Represents the organisation ID.
     * @param internalName Represents the internal names of entity.
     * @return the {@link FieldInfo}.
     */
    Optional<FieldInfo> getFieldByInternalName(final long orgId, final String internalName);

    /**
     * Fetches the collection of {@code FieldInfo} by using the internal names.
     *
     * @param orgId         Represents the organisation ID.
     * @param internalNames Represents the internal names of entity.
     * @return The collection of {@link FieldInfo}.
     */
    Collection<FieldInfo> getFieldsByInternalNames(final long orgId, final Collection<String> internalNames);

    /**
     * Provides the {@link SubCategory.Type} to its equivalent {@link Source} representation.
     *
     * @param subCategoryType the type to map.
     * @return an {@link Optional} containing the source, or empty if the type is not a core entity source.
     */
    private static Optional<Source> findSource(final SubCategory.Type subCategoryType) {
        return Optional.ofNullable(switch (subCategoryType) {
            case CONTACTS -> Source.CONTACT;
            case COMPANIES -> Source.COMPANY;
            case DEALS ->  Source.DEAL;
            case PRODUCTS -> Source.PRODUCT;
            default -> null;
        });
    }
}