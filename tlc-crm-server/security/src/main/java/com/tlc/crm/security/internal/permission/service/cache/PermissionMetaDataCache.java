package com.tlc.crm.security.internal.permission.service.cache;

import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Offers services for retrieving the MetaData of the role permissions from the cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionMetaDataCache extends LocalCache {

    /**
     * <p>
     * Gets the instance of {@link PermissionMetaDataCache}
     * </p>
     *
     * @return The {@link PermissionMetaDataCache}
     */
    static PermissionMetaDataCache getInstance() {
        return PermissionMetaDataCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link Category} by using the category id.
     * </p>
     *
     * @param categoryId Represents the id of the category.
     * @return {@link Category} holds the details of the category.
     */
    Optional<Category> getCategory(final Long categoryId);

    /**
     * <p>
     * Fetches the {@link SubCategory} by using the sub-category id.
     * </p>
     *
     * @param subCategoryId Represents the id of the sub-category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    Optional<SubCategory> getSubCategory(final Long subCategoryId);

    /**
     * <p>
     * Fetches the {@link Action} by using the action id.
     * </p>
     *
     * @param subCategoryId Represents the sub-category id.
     * @param actionId      Represents the id of the action.
     * @return {@link Action} holds the details of the action.
     */
    Optional<Action> getAction(final Long subCategoryId, final Long actionId);

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
     * Provides the {@link Action} of the permission.
     * </p>
     *
     * @return {@link Action} holds the details of the action.
     */
    Collection<Action> getAllActions();

    /**
     * <p>
     * Provides all the {@link Action} of the sub-category.
     * </p>
     *
     * @param subCategoryId Represents the sub-category id.
     * @return Collection of {@link Action} holds the details of the action.
     */
    Collection<Action> getSubCategoryActions(final Long subCategoryId);

    /**
     * <p>
     * Adds the {@link Collection} of {@link Category} into cache.
     * </p>
     *
     * @param categories {@link Category}(s) to be cached.
     */
    void addCategories(final Collection<Category> categories);

    /**
     * <p>
     * Adds the {@link Collection} of {@link SubCategory} into cache.
     * </p>
     *
     * @param subCategories {@link SubCategory}(s) to be cached.
     */
    void addSubCategories(final Collection<SubCategory> subCategories);

    /**
     * <p>
     * Adds the {@link Collection} of {@link Action} into cache.
     * </p>
     *
     * @param actions {@link Action}(s) to be cached.
     */
    void addActions(final Collection<Action> actions);

    /**
     * <p>
     * Adds the grouped subcategory action ids into cache.
     * </p>
     *
     * @param subCategoryActionIds Represents the grouped subcategory action ids.
     */
    void addSubCategoryActionIds(final Map<Long, Map<Long, Long>> subCategoryActionIds);
}