package com.tlc.crm.security.internal.permission.builder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.security.permission.model.FieldAccessLevel;
import com.tlc.crm.security.permission.model.FieldPermission;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A builder class responsible for constructing and serializing {@link FieldPermission} objects.
 *
 * @author  Sathish
 * @version 1.0
 */
public final class FieldPermissionBuilder {

    private static final String SOURCE_ID = "sourceId";
    private static final String FIELDS = "fields";
    private static final String ID = "id";
    private static final String ACCESS_LEVEL = "accessLevel";
    private static final String IS_REQUIRED = "isRequired";
    private static final String ORDER = "order";
    private static final String IS_REQUIRED_ON_DATA_FIELDS = "isRequiredOnDataFields";

    private static final class InstanceHolder {
        private static final FieldPermissionBuilder FIELD_PERMISSION_BUILDER = new FieldPermissionBuilder();
    }

    private FieldPermissionBuilder() {
    }

    /**
     * Returns the singleton instance of {@link FieldPermissionBuilder}.
     *
     * @return the singleton instance of the builder
     */
    public static FieldPermissionBuilder getInstance() {
        return InstanceHolder.FIELD_PERMISSION_BUILDER;
    }

    /**
     * Builds a collection of {@link FieldPermission} objects from the given JSON array.
     * <p>
     * Each entry in the JSON array represents a source entity with its associated field permissions.
     * The method parses each JSON object and constructs the corresponding {@link FieldPermission}
     * instances with appropriate organization, role, and access level details.
     * </p>
     *
     * @param orgId          the organization ID
     * @param roleId         the role ID
     * @param permissionJson the JSON array containing field permission definitions
     * @return a collection of {@link FieldPermission} objects
     */
    public Collection<FieldPermission> build(final long orgId, final Long roleId, final JsonArray permissionJson) {
        return build(orgId, roleId, permissionJson, Collections.emptySet());
    }

    /**
     * Builds a collection of {@link FieldPermission} objects from the given JSON array.
     * <p>
     * Each entry in the JSON array represents a source entity with its associated field permissions.
     * The method parses each JSON object and constructs the corresponding {@link FieldPermission}
     * instances with appropriate organization, role, and access level details.
     * </p>
     *
     * @param orgId          the organization ID
     * @param roleId         the role ID
     * @param permissionJson the JSON array containing field permission definitions
     * @param existingPermissions Holds the existing field permission to be updated.
     * @return a collection of {@link FieldPermission} objects
     */
    public Collection<FieldPermission> build(final long orgId, final Long roleId, final JsonArray permissionJson,
                                             final Collection<FieldPermission> existingPermissions) {
        final Map<Long, FieldPermission> permissionsMap = existingPermissions.stream()
                .collect(Collectors.toMap(FieldPermission::getFieldId, Function.identity()));
        final Collection<FieldPermission> fieldPermissions = new TreeSet<>();
        final int permissionJsonSize = permissionJson.size();

        for (int permissionJsonIndex = 0; permissionJsonIndex < permissionJsonSize; permissionJsonIndex++) {
            final JsonObject sourceFieldPermissionJson = permissionJson.getJsonObject(permissionJsonIndex);
            final JsonArray fieldsPermissionJson = sourceFieldPermissionJson.getJsonArray(FIELDS);
            final long sourceId = sourceFieldPermissionJson.getLong(SOURCE_ID);
            final int fieldsPermissionJsonSize = fieldsPermissionJson.size();

            for (int fieldsPermissionJsonIndex = 0; fieldsPermissionJsonIndex < fieldsPermissionJsonSize; fieldsPermissionJsonIndex++) {
                final JsonObject fieldPermissionJson = fieldsPermissionJson.getJsonObject(fieldsPermissionJsonIndex);
                final long fieldId = fieldPermissionJson.getLong(ID);
                final FieldPermission fieldPermission = Optional.ofNullable(permissionsMap.remove(fieldId)).orElseGet(FieldPermission::new);

                fieldPermission.setOrgId(orgId);
                fieldPermission.setRoleId(roleId);
                fieldPermission.setSourceId(sourceId);
                fieldPermission.setFieldId(fieldId);
                fieldPermission.setAccessLevel(FieldAccessLevel.get(fieldPermissionJson.getShort(ACCESS_LEVEL)));
                fieldPermission.setRequired(fieldPermissionJson.getBoolean(IS_REQUIRED));

                if (fieldPermissionJson.containsKey(ORDER)) {
                    fieldPermission.setOrder(fieldPermissionJson.getInt(ORDER));
                }

                if (fieldPermissionJson.containsKey(IS_REQUIRED_ON_DATA_FIELDS)) {
                    fieldPermission.setRequiredOnDataFields(fieldPermissionJson.getBoolean(IS_REQUIRED_ON_DATA_FIELDS));
                }

                fieldPermissions.add(fieldPermission);
            }
        }
        fieldPermissions.addAll(permissionsMap.values());

        return fieldPermissions;
    }

    /**
     * Converts a collection of {@link FieldPermission} objects into a JSON array.
     * <p>
     * The resulting JSON structure groups field permissions by their {@code sourceId},
     * with each source containing its list of fields, access levels, and required flags.
     * </p>
     *
     * @param fieldPermissions the collection of {@link FieldPermission} objects to convert
     * @return a {@link JsonArray} representing the field permissions in JSON format
     */
    public JsonArray build(final Collection<FieldPermission> fieldPermissions) {
        final JsonArray fieldPermissionsArray = Json.array();
        final Map<Long, List<FieldPermission>> sourceFieldPermissions = fieldPermissions.stream().collect(Collectors.groupingBy(FieldPermission::getSourceId));

        for (final Map.Entry<Long, List<FieldPermission>> sourceFieldPermissionsEntry : sourceFieldPermissions.entrySet()) {
            final JsonObject sourceFieldPermissionJson = Json.object();
            final JsonArray fieldsPermissionArray = Json.array();
            final Collection<FieldPermission> fieldPermissionList = sourceFieldPermissionsEntry.getValue();

            for (final FieldPermission fieldPermission : fieldPermissionList) {
                final JsonObject fieldPermissionJson = Json.object();

                fieldPermissionJson
                        .put(ID, fieldPermission.getFieldId())
                        .put(ACCESS_LEVEL, fieldPermission.getAccessLevel().getId())
                        .put(IS_REQUIRED, fieldPermission.isRequired())
                        .put(ORDER, fieldPermission.getOrder())
                        .put(IS_REQUIRED_ON_DATA_FIELDS, fieldPermission.isRequiredOnDataFields());

                fieldsPermissionArray.put(fieldPermissionJson);
            }

            sourceFieldPermissionJson
                    .put(SOURCE_ID, sourceFieldPermissionsEntry.getKey())
                    .put(FIELDS, fieldsPermissionArray);
            fieldPermissionsArray.put(sourceFieldPermissionJson);
        }

        return fieldPermissionsArray;
    }
}
