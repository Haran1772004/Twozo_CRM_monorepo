package com.tlc.crm.security.internal.permission.model.rule.constraint;

import com.tlc.crm.security.model.Constraint;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.Scope;
import com.tlc.crm.security.permission.model.SubCategory;

/**
 * Holds the constraint to be checked for the table and column access
 *
 * @param subCategoryType      Represents the subcategory type
 * @param actionType           Represents the action type
 * @param permissionStatusFlag A flag indicating whether the permission is granted or not
 * @author sundar
 * @version 1.0
 */
public record PermissionConstraint(Category.Type categoryType, SubCategory.Type subCategoryType, Action.Type actionType, Scope scope,
                                   boolean permissionStatusFlag, boolean checkCurrentUserPermission) implements Constraint {
}