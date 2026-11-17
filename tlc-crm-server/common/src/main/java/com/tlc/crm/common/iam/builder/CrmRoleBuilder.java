package com.tlc.crm.common.iam.builder;

import com.tlc.crm.common.iam.api.model.CrmRole;

import com.tlc.crm.security.iam.DefaultRole;
import com.tlc.crm.security.permission.model.Scope;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Builder for building the {@link CrmRole} with the provided data
 *
 * @author Sathish
 * @version 1.0
 */
public final class CrmRoleBuilder {

    private static class Instance {
        private static final CrmRoleBuilder INSTANCE = new CrmRoleBuilder();
    }

    private CrmRoleBuilder() {
    }

    /**
     * Gets the instance of the {@link CrmRoleBuilder}
     *
     * @return {@link CrmRoleBuilder} instance
     */
    public static CrmRoleBuilder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     *     Builds the {@link CrmRole} for the given {@link DefaultRole}
     * </p>
     *
     * @param defaultRoles The default role to use as a basis.
     * @return The resulting {@link CrmRole}.
     */
    public Collection<CrmRole> build(final DefaultRole... defaultRoles) {
        final Collection<CrmRole> roles = new ArrayList<>();

        for (final DefaultRole defaultRole : defaultRoles) {
            final CrmRole crmRole = constructRole(defaultRole);

            crmRole.setPermissionJson(defaultRole.getPermissions());
            roles.add(crmRole);
        }

        return roles;
    }

    /**
     * <p>
     * Constructs the {@link CrmRole} from the {@link DefaultRole}
     * </p>
     *
     * @param defaultRole Represents the default role.
     * @return The {@link CrmRole}.
     */
    private CrmRole constructRole(final DefaultRole defaultRole) {
        final CrmRole crmRole = new CrmRole();
        final CrmRole.Config config = new CrmRole.Config();

        config.setRenamable(defaultRole.isRenamable());
        config.setAdmin(defaultRole.isAdmin());
        config.setDefault(true);
        crmRole.setConfig(config);
        crmRole.setName(defaultRole.getName());
        crmRole.setDefaultScope(Scope.getDefaultScope());

        return crmRole;
    }
}