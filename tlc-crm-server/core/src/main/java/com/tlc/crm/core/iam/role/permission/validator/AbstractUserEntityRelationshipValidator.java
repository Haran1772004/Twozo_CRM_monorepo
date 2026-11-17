package com.tlc.crm.core.iam.role.permission.validator;

import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.models.UserEntityRelationshipType;

import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.internal.permission.model.ColumnDataPermissionValidator;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.Scope;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

/**
 * <p>
 * A validator that validates the relationship(Owner of entity/Follower of entity/Territory entity) between the current user and the given entity.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ColumnDataPermissionValidator
 */
public abstract class AbstractUserEntityRelationshipValidator implements ColumnDataPermissionValidator<Long> {

    /**
     * {@inheritDoc}
     *
     * @param value The value to check the permission.
     * @return Permission status of the value.
     */
    @Override
    public boolean validate(final Long value) {
        final SecurityContext securityContext = LoggedInUserInfo.getSecurityContext();
        final Optional<RolePermission> rolePermission = Optional.ofNullable(securityContext.getRolePermission());

        return rolePermission.map(permission -> {
            final Optional<Scope> optionalScope = Optional.ofNullable(permission.getSubCategory().getScope());
            final BaseCrmUser user = securityContext.getUser();
            final Long orgId = user.orgId();
            final Long userId = user.id();
            final Collection<UserEntityRelationshipType> relationshipTypes = optionalScope.isPresent()
                    && Scope.TERRITORY_ACCESS == optionalScope.get() ? EnumSet.of(UserEntityRelationshipType.OWNER_OF_ENTITY,
                    UserEntityRelationshipType.USER_TERRITORY_ENTITY, UserEntityRelationshipType.FOLLOWER_OF_ENTITY)
                    : EnumSet.of(UserEntityRelationshipType.OWNER_OF_ENTITY, UserEntityRelationshipType.USER_TERRITORY_ENTITY);

            return getEntityConfigManager().doesUserRelateToEntity(orgId, userId, value, relationshipTypes);
        }).orElseGet(() -> true);
    }

    /**
     * <p>
     * Provides the manager of the entity.
     * </p>
     *
     * @return {@link EntityConfigManager}.
     */
    protected abstract EntityConfigManager<?> getEntityConfigManager();
}