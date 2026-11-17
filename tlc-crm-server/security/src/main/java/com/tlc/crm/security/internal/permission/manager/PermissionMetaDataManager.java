package com.tlc.crm.security.internal.permission.manager;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the {@link com.tlc.crm.security.permission.model.Permission} metadata related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionMetaDataManager {

    static PermissionMetaDataManager getInstance() {
        return PermissionMetaDataManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link Category} by using the category id.
     * </p>
     *
     * @param categoryId Represents the id of the category.
     * @return {@link Category} holds the details of the category.
     */
    Category getCategory(final Long categoryId);

    /**
     * <p>
     * Fetches the {@link SubCategory} by using the sub-category id.
     * </p>
     *
     * @param subCategoryId Represents the id of the sub-category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    SubCategory getSubCategory(final Long subCategoryId);

    /**
     * <p>
     * Fetches the {@link Action} by using the action id.
     * </p>
     *
     * @param actionId      Represents the id of the action.
     * @param subCategoryId Represents the sub-category id.
     * @return {@link Action} holds the details of the action.
     */
    Action getAction(final Long actionId, final Long subCategoryId);

    /**
     * <p>
     * Provides all the {@link Category} of the permission.
     * </p>
     *
     * @return Collection of {@link Category} holds the details of the category.
     */
    Collection<Category> getAllCategories();

    /**
     * <p>
     * Provides all the {@link SubCategory} of the permission.
     * </p>
     *
     * @return Collection of {@link SubCategory} holds the details of the sub-category.
     */
    Collection<SubCategory> getAllSubCategories();

    /**
     * <p>
     * Provides all the {@link Action} of the permission.
     * </p>
     *
     * @return Collection of {@link Action} holds the details of the action.
     */
    Collection<Action> getAllActions();

    /**
     * <p>
     * Fetches the {@link Action} by using the action id.
     * </p>
     *
     * @param subCategoryActionId Represents the id of the sub-category action.
     * @return {@link Action} holds the details of the action.
     */
    Action getSubCategoryAction(final Long subCategoryActionId);

    /**
     * <p>
     * Fetches the grouped subcategory actions.
     * </p>
     *
     * @return Grouped subcategory actions.
     */
    Map<Long, Map<Long, Long>> getSubCategoryActionIds();
}