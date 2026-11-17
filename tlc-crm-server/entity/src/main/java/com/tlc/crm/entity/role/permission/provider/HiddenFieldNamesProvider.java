package com.tlc.crm.entity.role.permission.provider;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.security.cache.service.FieldPermissionService;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.FieldAccessLevel;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.model.FieldPermissions;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * <p>
 * Provider for providing the hidden field names.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see SecureDefaultValueProvider
 */
public class HiddenFieldNamesProvider implements SecureDefaultValueProvider<Collection<String>> {

    private static final String PROVIDER_NAME = "HIDDEN_FIELD_NAMES_PROVIDER";

    private final FieldService fieldService;
    private final FieldPermissionService fieldPermissionService;

    private HiddenFieldNamesProvider() {
        fieldService = FieldService.getInstance();
        fieldPermissionService = FieldPermissionService.getInstance();
    }

    private static final class Instance {
        private static final HiddenFieldNamesProvider HIDDEN_FIELD_NAMES_PROVIDER = new HiddenFieldNamesProvider();
    }

    /**
     * Gets the {@link HiddenFieldNamesProvider} instance
     *
     * @return {@link HiddenFieldNamesProvider}
     */
    public static HiddenFieldNamesProvider getInstance() {
        return Instance.HIDDEN_FIELD_NAMES_PROVIDER;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the value provider
     */
    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the hidden field names
     */
    @Override
    public Collection<String> get() {
        final CrmUser user = LoggedInUserInfo.getUser();
        final FieldPermissions fieldPermissions = fieldPermissionService.getPermissions(user.orgId(), user.getRoleId());
        final Collection<Long> hiddenFieldIds = fieldPermissions.getPermissions().stream()
                .filter(fieldPermission -> FieldAccessLevel.HIDDEN == fieldPermission.getAccessLevel())
                .map(FieldPermission::getFieldId).collect(Collectors.toSet());

        final Collection<String> hiddenFieldNames = fieldService.getFieldsById(user.orgId(), hiddenFieldIds).values().stream()
                .map(FieldInfo::aliasName).collect(Collectors.toSet());

        return hiddenFieldNames.isEmpty() ? null : hiddenFieldNames;
    }
}