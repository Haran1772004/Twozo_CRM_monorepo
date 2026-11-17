package com.tlc.crm.entity.role.permission.util;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.permission.model.SubCategory;

import java.util.Optional;

/**
 * <p>
 * Utility class for handling operations related to subcategory type.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class SubCategoryTypeUtils {

    /**
     * <p>
     * Finds the {@link Source} of the given {@link SubCategory.Type}.
     * </p>
     *
     * @param subCategoryType Represents the type of the sub-category.
     * @return {@link Source}.
     */
    public static Optional<Source> findSource(final SubCategory.Type subCategoryType) {
        return Optional.ofNullable(switch (subCategoryType) {
            case CONTACTS, CONTACT_FOLLOWERS, CONTACT_FOLLOWERS_RECORDS -> Source.CONTACT;
            case COMPANIES, COMPANY_FOLLOWERS, COMPANY_FOLLOWERS_RECORDS -> Source.COMPANY;
            case DEALS, DEAL_FOLLOWERS, DEAL_FOLLOWERS_RECORDS -> Source.DEAL;
            case PRODUCTS -> Source.PRODUCT;
            default -> null;
        });
    }

    /**
     * <p>
     * Finds the {@link StoreType} of the given {@link SubCategory.Type}.
     * </p>
     *
     * @param subCategoryType Represents the type of the sub-category.
     * @return {@link StoreType}.
     */
    public static StoreType findStoreType(final SubCategory.Type subCategoryType) {
        return switch (subCategoryType) {
            case CONTACTS -> StoreType.CONTACT;
            case COMPANIES -> StoreType.COMPANY;
            case DEALS -> StoreType.DEAL;
            default -> null;
        };
    }

    /**
     * <p>
     * Finds the {@link SubCategory.Type} of the module referred by the given {@link StoreType}.
     * </p>
     *
     * @param storeType Represents the {@link StoreType}.
     * @return {@link Optional} of {@link SubCategory.Type} of the referred module, or empty if the store type does not refer to a module.
     */
    public static Optional<SubCategory.Type> findSubCategoryType(final StoreType storeType) {
        return Optional.ofNullable(switch (storeType) {
            case CONTACT, CONTACT_ONBOARD -> SubCategory.Type.CONTACTS;
            case COMPANY, COMPANY_ONBOARD -> SubCategory.Type.COMPANIES;
            case DEAL -> SubCategory.Type.DEALS;
            case PRODUCT -> SubCategory.Type.PRODUCTS;
            default -> null;
        });
    }

    /**
     * <p>
     * Provides the {@link SubCategory.Type} for the given source name.
     * </p>
     *
     * @param sourceName Represents the source name.
     * @return {@link SubCategory.Type} holds the details of the sub-category.
     */
    public static SubCategory.Type getBySourceName(final String sourceName) {
        return switch (sourceName.toLowerCase()) {
            case "contact" -> SubCategory.Type.CONTACTS;
            case "company" -> SubCategory.Type.COMPANIES;
            case "deal" -> SubCategory.Type.DEALS;
            case "product" -> SubCategory.Type.PRODUCTS;
            case "activity" -> SubCategory.Type.ACTIVITIES;
            default -> null;
        };
    }
}