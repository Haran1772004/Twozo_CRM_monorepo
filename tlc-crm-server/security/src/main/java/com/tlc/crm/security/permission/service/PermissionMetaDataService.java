package com.tlc.crm.security.permission.service;

import com.tlc.crm.security.internal.permission.service.PermissionMetaDataServiceImpl;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Offers services for retrieving the MetaData of the role permissions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionMetaDataService {

    /**
     * <p>
     * Gets the instance of {@link PermissionMetaDataService}
     * </p>
     *
     * @return The {@link PermissionMetaDataServiceImpl} instance
     */
    static PermissionMetaDataService getInstance() {
        return PermissionMetaDataServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link Category} by using the category id.
     * </p>
     *
     * @param categoryId Represents the id of the category.
     * @return {@link Category} holds the details of the category.
     */
    Category getCategory(final long categoryId);

    /**
     * <p>
     * Fetches the {@link SubCategory} by using the sub-category id.
     * </p>
     *
     * @param subCategoryId Represents the id of the sub-category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    SubCategory getSubCategory(final long subCategoryId);

    /**
     * <p>
     * Fetches the {@link Action} by using the action id.
     * </p>
     *
     * @param subCategoryId Represents the sub-category id.
     * @param actionId      Represents the id of the action.
     * @return {@link Action} holds the details of the action.
     */
    Action getAction(final long subCategoryId, final long actionId);

    /**
     * <p>
     * Fetches the {@link Action} for the given {@link ApiDetails}.
     * </p>
     *
     * @param apiDetails Represents the API details.
     * @return {@link Action} holds the details of the action.
     */
    Optional<Action> getActionByApiDetails(final ApiDetails apiDetails);

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
     * Fetches the {@link Category} by using the category name.
     * </p>
     *
     * @param categoryType Represents the type of the category.
     * @return {@link Category} holds the details of the category.
     */
    Category getCategoryByType(final Category.Type categoryType);

    /**
     * <p>
     * Fetches the {@link SubCategory} by using the sub-category name.
     * </p>
     *
     * @param subCategoryType Represents the type of the sub-category.
     * @param categoryType    Represents the type of the category.
     * @return {@link SubCategory} holds the details of the sub-category.
     */
    Optional<SubCategory> getSubCategoryByType(final SubCategory.Type subCategoryType, final Category.Type categoryType);

    /**
     * <p>
     * Fetches the {@link Action} by using the action name.
     * </p>
     *
     * @param subCategoryId Represents the sub-category id.
     * @param actionType    Represents the type of the action.
     * @return {@link Action} holds the details of the action.
     */
    Optional<Action> getSubCategoryActionByType(final long subCategoryId, final Action.Type actionType);

    /**
     * <p>
     * Fetches the available action for the given {@link SubCategory} and {@link Action}.
     * </p>
     *
     * @param subCategoryType Represents the subcategory type.
     * @param actionType      Represents the action type.
     * @return {@link Action}.
     */
    Optional<Action> getSubCategoryActionByType(final SubCategory.Type subCategoryType, final Action.Type actionType);

    /**
     * <p>
     * Fetches the available actions for the given {@link SubCategory}.
     * </p>
     *
     * @param subCategoryId Represents the sub-category id.
     * @return Collection of {@link Action}.
     */
    Collection<Action> getSubCategoryActions(final long subCategoryId);
}
