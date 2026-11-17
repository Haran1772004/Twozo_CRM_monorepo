package com.tlc.crm.security.internal.permission.service.resolver;

import com.tlc.crm.security.internal.permission.manager.PermissionMetaDataManager;
import com.tlc.crm.security.internal.permission.service.cache.PermissionMetaDataCache;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Implements the services offered by the {@link PermissionMetaDataCache} for resolving the MetaData of the role permissions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PermissionMetaDataResolverImpl implements PermissionMetaDataResolver {

    private final PermissionMetaDataCache permissionMetaDataCache;
    private final PermissionMetaDataManager permissionMetaDataManager;

    public PermissionMetaDataResolverImpl() {
        permissionMetaDataCache = PermissionMetaDataCache.getInstance();
        permissionMetaDataManager = PermissionMetaDataManager.getInstance();
    }

    private static class Instance {
        private static final PermissionMetaDataResolverImpl INSTANCE = new PermissionMetaDataResolverImpl();
    }

    /**
     * Gets the instance of the {@link PermissionMetaDataResolver}
     *
     * @return the instance of the {@link PermissionMetaDataResolver}
     */
    public static PermissionMetaDataResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param categoryId Represents the id of the category.
     * @return {@link Category} holds the details of the category.
     */
    @Override
    public Category getCategory(final Long categoryId) {
        final Optional<Category> cachedCategory = permissionMetaDataCache.getCategory(categoryId);

        if (cachedCategory.isEmpty()) {
            final Category category = permissionMetaDataManager.getCategory(categoryId);

            permissionMetaDataCache.addCategories(Set.of(category));
            return category;
        }

        return cachedCategory.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Represents the id of the sub-category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    @Override
    public SubCategory getSubCategory(final Long subCategoryId) {
        final Optional<SubCategory> cachedSubCategory = permissionMetaDataCache.getSubCategory(subCategoryId);

        if (cachedSubCategory.isEmpty()) {
            final SubCategory subCategory = permissionMetaDataManager.getSubCategory(subCategoryId);

            permissionMetaDataCache.addSubCategories(Set.of(subCategory));
            return subCategory;
        }

        return cachedSubCategory.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Represents the sub-category id.
     * @param actionId      Represents the id of the action.
     * @return {@link Action} holds the details of the action.
     */
    @Override
    public Action getAction(final Long subCategoryId, final Long actionId) {
        final Optional<Action> cachedAction = permissionMetaDataCache.getAction(subCategoryId, actionId);

        if (cachedAction.isEmpty()) {
            final Action action = permissionMetaDataManager.getAction(actionId, subCategoryId);

            permissionMetaDataCache.addActions(Set.of(action));
            return action;
        }

        return cachedAction.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return Grouped subcategory action ids.
     */
    @Override
    public Map<Long, Map<Long, Long>> getSubCategoryActionIds() {
        final Map<Long, Map<Long, Long>> cachedSubCategoryActionIds = permissionMetaDataCache.getSubCategoryActionIds();

        if (cachedSubCategoryActionIds.isEmpty()) {
            final Map<Long, Map<Long, Long>> subCategoryActionIds = permissionMetaDataManager.getSubCategoryActionIds();

            permissionMetaDataCache.addSubCategoryActionIds(subCategoryActionIds);
            return subCategoryActionIds;
        }
        return cachedSubCategoryActionIds;
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link Category} holds the details of the category.
     */
    @Override
    public Collection<Category> getAllCategories() {
        final Collection<Category> cachedCategories = permissionMetaDataCache.getAllCategories();

        if (cachedCategories.isEmpty()) {
            final Collection<Category> categories = permissionMetaDataManager.getAllCategories();

            permissionMetaDataCache.addCategories(categories);
            return categories;
        }

        return cachedCategories;
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link SubCategory} holds the details of the sub-category.
     */
    @Override
    public Collection<SubCategory> getAllSubCategories() {
        final Collection<SubCategory> cachedSubCategories = permissionMetaDataCache.getAllSubCategories();

        if (cachedSubCategories.isEmpty()) {
            final Collection<SubCategory> subCategories = permissionMetaDataManager.getAllSubCategories();

            permissionMetaDataCache.addSubCategories(subCategories);
            return subCategories;
        }

        return cachedSubCategories;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Action} holds the details of the action.
     */
    @Override
    public Collection<Action> getAllActions() {
        return permissionMetaDataCache.getAllActions();
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Represents the sub-category id.
     * @return Collection of {@link Action} holds the details of the action.
     */
    @Override
    public Collection<Action> getSubCategoryActions(final Long subCategoryId) {
        final Collection<Action> cachedActions = permissionMetaDataCache.getSubCategoryActions(subCategoryId);

        if (cachedActions.isEmpty()) {
            final Collection<Action> actions = permissionMetaDataManager.getAllActions();

            permissionMetaDataCache.addActions(actions);
            return actions;
        }

        return cachedActions;
    }
}