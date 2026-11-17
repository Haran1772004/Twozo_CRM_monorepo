package com.tlc.crm.security.permission.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;
import com.tlc.crm.security.permission.validator.LimitValidator;

import com.tlc.i18n.I18nKey;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Represents the various type of the {@link LimitValidator}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum LimitActionType {

    BULK_EMAIL((short) 1, "i18n.permission.limit.validator.type.bulkEmail", Category.Type.EMAILS, SubCategory.Type.BULK_EMAIL_LIMIT),
    INDIVIDUAL_EMAIL((short) 2, "i18n.permission.limit.validator.type.individualEmail", Category.Type.EMAILS, SubCategory.Type.INDIVIDUAL_EMAIL_LIMIT),
    IMPORT_CREATE((short) 3, "i18n.permission.limit.validator.type.importCreate", Category.Type.ACTIONS, SubCategory.Type.LIMIT_FOR_CREATING_RECORDS_THROUGH_IMPORT),
    IMPORT_UPDATE((short) 4, "i18n.permission.limit.validator.type.importUpdate", Category.Type.ACTIONS, SubCategory.Type.LIMIT_FOR_UPDATING_RECORDS_THROUGH_IMPORT),
    BULK_SMS((short) 5, "i18n.permission.limit.validator.type.bulkSms", Category.Type.SMS, SubCategory.Type.BULK_SMS_LIMIT),
    INDIVIDUAL_SMS((short) 6, "i18n.permission.limit.validator.type.individualSms", Category.Type.SMS, SubCategory.Type.INDIVIDUAL_SMS_LIMIT),
    ;

    private static final Map<SubCategory.Type, LimitActionType> LIMIT_PERMISSION_TYPE_MAP;

    private final short id;
    private final I18nKey displayName;
    private final Category.Type category;
    private final SubCategory.Type subCategory;

    static {
        LIMIT_PERMISSION_TYPE_MAP = new HashMap<>();

        for (final LimitActionType limitActionType : values()) {
            LIMIT_PERMISSION_TYPE_MAP.put(limitActionType.getSubCategory(), limitActionType);
        }
    }

    LimitActionType(final short id, final String displayName, final Category.Type category, final SubCategory.Type subCategory) {
        this.id = id;
        this.displayName = new I18nKey(displayName);
        this.category = category;
        this.subCategory = subCategory;
    }

    public short getId() {
        return id;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public Category.Type getCategory() {
        return category;
    }

    public SubCategory.Type getSubCategory() {
        return subCategory;
    }

    /**
     * <p>
     * Provides the {@link LimitActionType} of the given ID.
     * </p>
     *
     * @param id Represents the {@link LimitActionType} ID.
     * @return {@link LimitActionType}.
     */
    public static LimitActionType get(final short id) {
        return switch (id) {
            case 1 -> BULK_EMAIL;
            case 2 -> INDIVIDUAL_EMAIL;
            case 3 -> IMPORT_CREATE;
            case 4 -> IMPORT_UPDATE;
            case 5 -> BULK_SMS;
            case 6 -> INDIVIDUAL_SMS;
            default -> throw ErrorCode.get(PermissionErrorCodes.INVALID_TYPE, String.format("Invalid limit permission type id %d", id));
        };
    }

    /**
     * <p>
     * Provides the {@link LimitActionType} of the given subcategory type.
     * </p>
     *
     * @param subCategory Represents the {@link SubCategory} type.
     * @return {@link LimitActionType}.
     */
    public static LimitActionType get(final SubCategory.Type subCategory) {
        return LIMIT_PERMISSION_TYPE_MAP.get(subCategory);
    }
}
