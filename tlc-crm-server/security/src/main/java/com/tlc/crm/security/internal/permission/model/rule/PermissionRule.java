package com.tlc.crm.security.internal.permission.model.rule;

import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.iam.BaseCrmUser;

import com.tlc.crm.security.internal.permission.model.rule.constraint.PermissionConstraint;
import com.tlc.crm.security.model.Rule;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.sql.common.api.dml.Connector;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Rules for establishing permission criteria required for operations
 *
 * @author sundar
 * @version 1.0
 * @see Rule
 */
public class PermissionRule extends Rule<PermissionConstraint, SecurityContext> {

    public PermissionRule(final Connector connector, final Collection<? extends PermissionRule> rules) {
        super(connector, rules);
    }

    public PermissionRule(final PermissionConstraint constraint) {
        super(constraint);
    }

    /**
     * {@inheritDoc}
     *
     * @param securityContext the data to evaluate
     * @return true if the constraints are met, false otherwise
     */
    @Override
    protected boolean testConstraint(final SecurityContext securityContext) {
        final Predicate<Scope> scopeChecker = scope -> {
            final Scope constraintScope = constraint.scope();

            return Objects.isNull(scope) || Objects.isNull(constraintScope) || Objects.equals(scope, constraintScope);
        };

        if (constraint.checkCurrentUserPermission()) {
            final RolePermission permission = securityContext.getRolePermission();

            return Objects.nonNull(permission) && constraint.categoryType() == permission.getCategory().getType()
                    && constraint.subCategoryType() == permission.getSubCategory().getType()
                    && constraint.actionType() == permission.getAction().getType() && scopeChecker.test(permission.getSubCategory().getScope())
                    && constraint.permissionStatusFlag() == permission.isGranted();
        }
        final BaseCrmUser baseCrmUser = securityContext.getUser();
        final RolePermissionService rolePermissionService = RolePermissionService.getInstance();
        final RolePermission permission = rolePermissionService.getPermission(baseCrmUser.orgId(), baseCrmUser.getRoleId(),
                constraint.categoryType(), constraint.subCategoryType(), constraint.actionType(), true);

        return constraint.permissionStatusFlag() == permission.isGranted() && scopeChecker.test(permission.getSubCategory().getScope());
    }
}
