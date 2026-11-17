package com.tlc.crm.security.internal.permission.model;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Stores the permission meta data and the primary permission actions.
 * </p>
 *
 * @param permissionDataMap        Represents the map containing key as category and value as map of subcategories and its actions
 * @param primaryPermissionActions Represents the collection of primary permission category subcategory and its actions
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public record PermissionMetaData(Map<Category.Type, Map<SubCategory.Type, Collection<Action.Type>>> permissionDataMap,
                                 Collection<Pair<Category.Type, Pair<SubCategory.Type, Action.Type>>> primaryPermissionActions) {
}