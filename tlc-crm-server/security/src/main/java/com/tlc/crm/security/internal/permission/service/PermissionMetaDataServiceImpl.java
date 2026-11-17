package com.tlc.crm.security.internal.permission.service;

import com.tlc.crm.security.internal.permission.service.resolver.PermissionMetaDataResolver;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.Requisites;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.service.PermissionMetaDataService;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the implementation for the services offered by the {@link PermissionMetaDataService} to fetch the MetaData of the role permissions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PermissionMetaDataServiceImpl implements PermissionMetaDataService {

    private final PermissionMetaDataResolver permissionMetaDataResolver;

    public PermissionMetaDataServiceImpl() {
        this.permissionMetaDataResolver = PermissionMetaDataResolver.getInstance();
    }

    private static class Instance {
        private static final PermissionMetaDataServiceImpl INSTANCE = new PermissionMetaDataServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of the {@link PermissionMetaDataService}
     * </p>>
     *
     * @return The {@link PermissionMetaDataServiceImpl} instance
     */
    public static PermissionMetaDataServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param categoryId Represents the id of the category.
     * @return {@link Category} holds the details of the category.
     */
    @Override
    public Category getCategory(final long categoryId) {
        return permissionMetaDataResolver.getCategory(categoryId).copy();
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Represents the id of the sub-category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    @Override
    public SubCategory getSubCategory(final long subCategoryId) {
        return permissionMetaDataResolver.getSubCategory(subCategoryId).copy();
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Represents the sub-category id.
     * @param actionId      Represents the id of the action.
     * @return {@link Action} holds the details of the action.
     */
    @Override
    public Action getAction(final long subCategoryId, final long actionId) {
        return permissionMetaDataResolver.getAction(subCategoryId, actionId).copy();
    }

    /**
     * {@inheritDoc}
     *
     * @param apiDetails Represents the API details.
     * @return {@link Action} holds the details of the action.
     */
    @Override
    public Optional<Action> getActionByApiDetails(final ApiDetails apiDetails) {
        return permissionMetaDataResolver.getAllActions().stream().filter(action -> {
            final Requisites requisites = action.getRequisites();

            return Objects.nonNull(requisites) && requisites.getRelativeApis().contains(apiDetails);
        }).findAny().map(Action::copy);
    }

    /**
     * {@inheritDoc}
     *
     * @return Grouped subcategory action ids.
     */
    @Override
    public Map<Long, Map<Long, Long>> getSubCategoryActionIds() {
        return permissionMetaDataResolver.getSubCategoryActionIds();
    }

    /**
     * {@inheritDoc}
     *
     * @param categoryType Represents the type of the category.
     * @return {@link Category} holds the details of the category.
     */
    @Override
    public Category getCategoryByType(final Category.Type categoryType) {
        return permissionMetaDataResolver.getAllCategories().stream().filter(category ->
                Objects.equals(category.getType(), categoryType)).findFirst().get().copy();
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryType Represents the type of the sub-category.
     * @param categoryType    Represents the type of the category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    @Override
    public Optional<SubCategory> getSubCategoryByType(final SubCategory.Type subCategoryType, final Category.Type categoryType) {
        return permissionMetaDataResolver.getAllSubCategories().stream().filter(subCategory ->
                subCategory.getCategory().getType() == categoryType && Objects.equals(subCategory.getType(), subCategoryType)).findFirst().map(SubCategory::copy);
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Holds the details of the sub-category.
     * @param actionType    Represents the type of the action.
     * @return {@link Action} holds the details of the action.
     */
    @Override
    public Optional<Action> getSubCategoryActionByType(final long subCategoryId, final Action.Type actionType) {
        return permissionMetaDataResolver.getSubCategoryActions(subCategoryId).stream().filter(action ->
                Objects.equals(action.getType(), actionType)).findFirst().map(Action::copy);
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryType Represents the subcategory type.
     * @param actionType      Represents the action type.
     * @return {@link Action}.
     */
    @Override
    public Optional<Action> getSubCategoryActionByType(final SubCategory.Type subCategoryType, final Action.Type actionType) {
        return permissionMetaDataResolver.getAllActions().stream().filter(action ->
                action.getSubCategory().getType() == subCategoryType && action.getType() == actionType).findAny().map(Action::copy);
    }

    /**
     * {@inheritDoc}
     *
     * @param subCategoryId Represents the sub-category id.
     * @return Collection of {@link Action}.
     */
    @Override
    public Collection<Action> getSubCategoryActions(final long subCategoryId) {
        return permissionMetaDataResolver.getSubCategoryActions(subCategoryId).stream().map(Action::copy).collect(Collectors.toUnmodifiableSet());
    }
}
