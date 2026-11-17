package com.tlc.crm.entity.role.permission.resolver;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.sql.resource.OMETFIELDCONFIG;

import com.tlc.crm.security.cache.service.FieldPermissionService;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.model.SecureColumnValueResolver;
import com.tlc.crm.security.permission.model.FieldPermission;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link SecureColumnValueResolver} that resolves secure values
 * for columns based on field values.
 *
 * <p>
 * This class provides a mechanism to process and transform sensitive column values,
 * such as applying encryption, masking, or other security transformations.
 * </p>
 *
 * <h3>Customization:</h3>
 * <p>
 * Extend this class and override the {@link SecureColumnValueResolver#resolve(Collection, Column)} method to implement
 * custom security logic.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see SecureColumnValueResolver
 */
public class FieldConfigColumnDataResolver implements SecureColumnValueResolver {

    public static final String READ_ONLY = "readOnly";
    public static final String ADD_VIEW = "addView";
    public static final String REQUIRED = "required";
    public static final String IMMUTABLE = "immutable";

    private static final String NAME = "ometfieldconfig_preset_config";

    private final FieldPermissionService fieldPermissionService;

    private static final class InstanceHolder {
        private static final FieldConfigColumnDataResolver COLUMN_DATA_RESOLVER = new FieldConfigColumnDataResolver();
    }

    private FieldConfigColumnDataResolver() {
        fieldPermissionService = FieldPermissionService.getInstance();
    }

    public static FieldConfigColumnDataResolver getInstance() {
        return InstanceHolder.COLUMN_DATA_RESOLVER;
    }

    /**
     * {@inheritDoc}
     *
     * @return Name of the {@link SecureColumnValueResolver}.
     */
    @Override
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @param rows   Holds the details of {@link Row}.
     * @param column Holds the details of {@link Column}.
     */
    @Override
    public void resolve(final Collection<Row> rows, final Column column) {
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Map<Long, FieldPermission> fieldPermissionByFieldIds = fieldPermissionService.getPermissions(user.orgId(), user.getRoleId())
                .getPermissions().stream().collect(Collectors.toMap(FieldPermission::getFieldId, Function.identity()));

        for (final Row row : rows) {
            final String config = row.get(column);

            if (Objects.isNull(config)) {
                continue;
            }
            final JsonObject configJson;

            try {
                configJson = Json.object(config);
            } catch (final Throwable throwable) {
                continue;
            }
            final long fieldId = row.get(OMETFIELDCONFIG.FIELD_ID);
            final FieldPermission fieldPermission = fieldPermissionByFieldIds.get(fieldId);

            if (Objects.isNull(fieldPermission)) {
                continue;
            }

            switch (fieldPermission.getAccessLevel()) {
                case EDITABLE -> {
                    if (!fieldPermission.isRequiredOnDataFields() && fieldPermission.isRequired()) {
                        configJson.put(REQUIRED, true).put(ADD_VIEW, true).put(IMMUTABLE, true);
                    }
                }
                case READ_ONLY -> configJson.put(READ_ONLY, true).put(ADD_VIEW, false);
            }
            row.set(column, configJson.toString());
        }
    }
}
