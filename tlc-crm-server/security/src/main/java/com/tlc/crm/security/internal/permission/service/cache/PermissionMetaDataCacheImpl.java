package com.tlc.crm.security.internal.permission.service.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.security.internal.permission.manager.PermissionMetaDataManager;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Implements the services offered by the {@link PermissionMetaDataCache} and caches permission groups for default roles
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PermissionMetaDataCacheImpl implements PermissionMetaDataCache {

    private final Cache<Long, Category> categoriesCache;
    private final Cache<Long, SubCategory> subCategoriesCache;
    private final Cache<Long, Map<Long, Action>> actionsCache;
    private final Cache<Long, Map<Long, Long>> subCategoryActionIdsCache;
    private final PermissionMetaDataManager permissionMetaDataManager;

    private PermissionMetaDataCacheImpl() {
        final CacheManager cacheManager = CacheManager.getInstance();

        categoriesCache = cacheManager.createCache();
        subCategoriesCache = cacheManager.createCache();
        actionsCache = cacheManager.createCache();
        subCategoryActionIdsCache = cacheManager.createCache();
        permissionMetaDataManager = PermissionMetaDataManager.getInstance();
    }

    private static class Instance {
        private static final PermissionMetaDataCacheImpl INSTANCE = new PermissionMetaDataCacheImpl();
    }

    /**
     * <p>
     * Gets the {@link PermissionMetaDataCacheImpl} instance
     * </p>
     *
     * @return The instance of {@link PermissionMetaDataCacheImpl}
     */
    public static PermissionMetaDataCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        categoriesCache.putAll(permissionMetaDataManager.getAllCategories().stream().collect(Collectors.toMap(Category::getId, Function.identity())));
        subCategoriesCache.putAll(permissionMetaDataManager.getAllSubCategories().stream().collect(Collectors.toMap(SubCategory::getId, Function.identity())));
        actionsCache.putAll(permissionMetaDataManager.getAllActions().stream().collect(Collectors.groupingBy(action -> action.getSubCategory().getId(), Collectors.toMap(Action::getId, Function.identity()))));
        subCategoryActionIdsCache.putAll(permissionMetaDataManager.getSubCategoryActionIds());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        categoriesCache.clear();
        subCategoriesCache.clear();
        actionsCache.clear();
        subCategoryActionIdsCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param categoryId Represents the id of the category.
     * @return {@link Category} holds the details of the category.
     */
    @Override
    public Optional<Category> getCategory(final Long categoryId) {
        return Optional.ofNullable(categoriesCache.get(categoryId));
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Represents the id of the sub-category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    @Override
    public Optional<SubCategory> getSubCategory(final Long subCategoryId) {
        return Optional.ofNullable(subCategoriesCache.get(subCategoryId));
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Represents the sub-category id.
     * @param actionId      Represents the id of the action.
     * @return {@link Action} holds the details of the action.
     */
    @Override
    public Optional<Action> getAction(final Long subCategoryId, final Long actionId) {
        final Map<Long, Action> actions = actionsCache.get(subCategoryId);

        return Optional.ofNullable(Objects.isNull(actions) ? null : actions.get(actionId));
    }

    /**
     * {@inheritDoc}
     *
     * @return Grouped subcategory action ids.
     */
    @Override
    public Map<Long, Map<Long, Long>> getSubCategoryActionIds() {
        final Map<Long, Map<Long, Long>> subCategoryActionIds = new HashMap<>();

        subCategoryActionIdsCache.forEach(subCategoryActionIds::put);
        return subCategoryActionIds;
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link Category} holds the details of the category.
     */
    @Override
    public Collection<Category> getAllCategories() {
        final Collection<Category> categories = new HashSet<>();

        categoriesCache.forEach((id, category) -> categories.add(category));
        return categories;
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link SubCategory} holds the details of the sub-category.
     */
    @Override
    public Collection<SubCategory> getAllSubCategories() {
        final Collection<SubCategory> subCategories = new HashSet<>();

        subCategoriesCache.forEach((id, subCategory) -> subCategories.add(subCategory));
        return subCategories;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Action} holds the details of the action.
     */
    @Override
    public Collection<Action> getAllActions() {
        return actionsCache.getAll().values().stream().flatMap(actions -> actions.values().stream()).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Represents the sub-category id.
     * @return Collection of {@link Action} holds the details of the action.
     */
    @Override
    public Collection<Action> getSubCategoryActions(final Long subCategoryId) {
        final Optional<Map<Long, Action>> actions = Optional.ofNullable(actionsCache.get(subCategoryId));

        return actions.map(Map::values).orElseGet(Collections::emptyList);
    }

    /**
     * {@inheritDoc}
     *
     * @param categories {@link Category}(s) to be cached.
     */
    @Override
    public void addCategories(final Collection<Category> categories) {
        categoriesCache.putAll(categories.stream().collect(Collectors.toMap(Category::getId, Function.identity())));
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategories {@link SubCategory}(s) to be cached.
     */
    @Override
    public void addSubCategories(final Collection<SubCategory> subCategories) {
        subCategoriesCache.putAll(subCategories.stream().collect(Collectors.toMap(SubCategory::getId, Function.identity())));
    }

    /**
     * {@inheritDoc}
     *
     * @param actions {@link Action}(s) to be cached.
     */
    @Override
    public void addActions(final Collection<Action> actions) {
        actionsCache.putAll(actions.stream().collect(Collectors.groupingBy(action -> action.getSubCategory().getId(),
                Collectors.toMap(Action::getId, Function.identity()))));
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryActionIds Represents the grouped subcategory action ids.
     */
    @Override
    public void addSubCategoryActionIds(final Map<Long, Map<Long, Long>> subCategoryActionIds) {
        subCategoryActionIdsCache.putAll(subCategoryActionIds);
    }
}
