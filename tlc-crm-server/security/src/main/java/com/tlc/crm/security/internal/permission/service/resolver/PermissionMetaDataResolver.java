package com.tlc.crm.security.internal.permission.service.resolver;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A resolver that provides the MetaData of the role permissions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionMetaDataResolver {

    /**
     * <p>
     * Gets the {@link PermissionMetaDataResolver} instance
     * </p>
     *
     * @return The instance of {@link PermissionMetaDataResolverImpl}
     */
    static PermissionMetaDataResolver getInstance() {
        return PermissionMetaDataResolverImpl.getInstance();
    }

    /**
     * <p>
     * Resolves the {@link Category} by using the category id.
     * </p>
     *
     * @param categoryId Represents the id of the category.
     * @return {@link Category} holds the details of the category.
     */
    Category getCategory(final Long categoryId);

    /**
     * <p>
     * Resolves the {@link SubCategory} by using the sub-category id.
     * </p>
     *
     * @param subCategoryId Represents the id of the sub-category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    SubCategory getSubCategory(final Long subCategoryId);

    /**
     * <p>
     * Resolves the {@link Action} by using the action id.
     * </p>
     *
     * @param subCategoryId Represents the sub-category id.
     * @param actionId      Represents the id of the action.
     * @return {@link Action} holds the details of the action.
     */
    Action getAction(final Long subCategoryId, final Long actionId);

    /**
     * <p>
     * Fetches the grouped subcategory action ids.
     * </p>
     *
     * @return Grouped subcategory action ids.
     */
    Map<Long, Map<Long, Long>> getSubCategoryActionIds();

    /**
     * <p>
     * Resolves all the {@link Category} of the permission.
     * </p>
     *
     * @return Collection of {@link Category} holds the details of the category.
     */
    Collection<Category> getAllCategories();

    /**
     * <p>
     * Resolves all the {@link SubCategory} of the permission.
     * </p>
     *
     * @return Collection of {@link SubCategory} holds the details of the sub-category.
     */
    Collection<SubCategory> getAllSubCategories();

    /**
     * <p>
     * Provides the {@link Action} of the permission.
     * </p>
     *
     * @return {@link Action} holds the details of the action.
     */
    Collection<Action> getAllActions();

    /**
     * <p>
     * Resolves all the {@link Action} of the permission.
     * </p>
     *
     * @param subCategoryId Represents the sub-category id.
     * @return Collection of {@link Action} holds the details of the action.
     */
    Collection<Action> getSubCategoryActions(final Long subCategoryId);
}