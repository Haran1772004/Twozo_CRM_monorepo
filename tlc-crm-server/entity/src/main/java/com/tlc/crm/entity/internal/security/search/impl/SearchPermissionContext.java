package com.tlc.crm.entity.internal.security.search.impl;

import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;

/**
 * <p>
 * Wrapper to hold info pertaining to search-permission action.
 * </p>
 *
 * @param orgId                Organization's id.
 * @param roleId               Role id.
 * @param ownerId              Owner's id.
 * @param territoryIds         Territory ids.
 * @param sourceScope          {@link Scope}.
 * @param followerSubType      {@link SubCategory.Type}.
 * @param isAccessible         Whether accessible.
 * @param isGloballyAccessible Whether it's globally accessible.
 *
 * @author Ajay.
 * @version 1.0.
 */
record SearchPermissionContext(long orgId, long roleId, long ownerId, Collection<Long> territoryIds, Scope sourceScope,
                               SubCategory.Type followerSubType, boolean isAccessible, boolean isGloballyAccessible) {
}
