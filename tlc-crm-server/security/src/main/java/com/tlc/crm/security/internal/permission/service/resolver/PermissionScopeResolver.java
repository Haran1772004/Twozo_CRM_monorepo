package com.tlc.crm.security.internal.permission.service.resolver;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.RolePermissionService;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A resolver that resolves the {@link Scope} from the dependent sub-category.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PermissionScopeResolver {

    /**
     * <p>
     * Resolves the {@link Scope} from the dependent sub-category.
     * </p>
     *
     * @param orgId        Represents the organisation id.
     * @param roleId       Represents the role id.
     * @param scope        Represents the sub-category scope.
     * @param dependencies Holds the details of the dependencies.
     * @return If sub-category scope is null, then fetch the scope from the dependent sub-category, otherwise return sub-category scope.
     */
    public static Scope resolve(final Long orgId, final Long roleId, final Scope scope, final Collection<Action.Dependency> dependencies) {
        if (Objects.isNull(scope) && Objects.nonNull(dependencies)) {
            return dependencies.stream().findFirst().map(dependency -> fetchScope(orgId, roleId, dependency))
                    .filter(Optional::isPresent).map(Optional::get).orElseGet(() -> null);
        }
        return scope;
    }

    /**
     * <p>
     * Fetches the {@link Scope} for the sub-category associated with the current API type.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param roleId     Signifies the role of the current user.
     * @param dependency Which contains the related subcategory and action.
     * @return The {@link Scope} of the sub-category.
     */
    private static Optional<Scope> fetchScope(final long orgId, final long roleId, final Action.Dependency dependency) {
        final Action dependentAction = dependency.getDependentAction();
        final SubCategory subCategory = dependentAction.getSubCategory();

        return Optional.ofNullable(RolePermissionService.getInstance().getPermission(orgId, roleId, subCategory.getCategory().getType(),
                subCategory.getType(), dependentAction.getType()).getScope());
    }
}