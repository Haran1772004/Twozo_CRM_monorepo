package com.tlc.crm.entity.role.permission.provider;

import com.tlc.crm.common.iam.api.model.CrmUser;

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
 * Provider for providing the hidden field IDs
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see SecureDefaultValueProvider
 */
public class HiddenFieldIdsProvider implements SecureDefaultValueProvider<Collection<Long>> {

    private static final String PROVIDER_NAME = "HIDDEN_FIELD_IDS_PROVIDER";

    private final FieldPermissionService fieldPermissionService;

    private HiddenFieldIdsProvider() {
        fieldPermissionService = FieldPermissionService.getInstance();
    }

    private static final class Instance {
        private static final HiddenFieldIdsProvider HIDDEN_FIELD_IDS_PROVIDER = new HiddenFieldIdsProvider();
    }

    /**
     * Gets the {@link HiddenFieldIdsProvider} instance
     *
     * @return {@link HiddenFieldIdsProvider}
     */
    public static HiddenFieldIdsProvider getInstance() {
        return Instance.HIDDEN_FIELD_IDS_PROVIDER;
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
     * @return the hidden field IDs
     */
    @Override
    public Collection<Long> get() {
        final CrmUser user = LoggedInUserInfo.getUser();
        final FieldPermissions fieldPermissions = fieldPermissionService.getPermissions(user.orgId(), user.getRoleId());
        final Collection<Long> hiddenFieldIds = fieldPermissions.getPermissions().stream()
                .filter(fieldPermission -> FieldAccessLevel.HIDDEN == fieldPermission.getAccessLevel())
                .map(FieldPermission::getFieldId).collect(Collectors.toSet());

        return hiddenFieldIds.isEmpty() ? null : hiddenFieldIds;
    }
}