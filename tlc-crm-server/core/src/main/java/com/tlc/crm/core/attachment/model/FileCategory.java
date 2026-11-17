package com.tlc.crm.core.attachment.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.features.internal.file.status.FileHolderErrorCodes;

/**
 * Enum representing different file categories in the system.
 * Each category is associated with a unique identifier and a descriptive name.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum FileCategory {

    CONTACT((short) 1, "contact"),
    COMPANY((short) 2, "company"),
    DEAL((short) 3, "deal"),
    IMPORT((short) 4, "import"),
    SOURCE_PROFILE((short) 5, "source-profile"),
    USER_PROFILE((short) 6, "user-profile"),
    EMAIL((short) 7, "email"),
    EXPORT((short) 8, "export"),
    FACEBOOK((short) 9, "facebook"),
    WHATSAPP((short) 10, "whatsapp"),
    INSTAGRAM((short) 11, "instagram"),
    QUOTATION((short) 12, "quotation"),
    WEBFORM_IMAGE((short) 13, "webform-image"),
    INVOICE((short) 14, "invoice"),
    ;

    private final short id;
    private final String name;

    FileCategory(final short id, final String name) {
        this.id = id;
        this.name = name;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the {@link FileCategory} by id.
     *
     * @param id Refers the category id.
     * @return {@link FileCategory}
     */
    public static FileCategory getById(final short id) {
        return switch (id) {
            case 1 -> CONTACT;
            case 2 -> COMPANY;
            case 3 -> DEAL;
            case 4 -> IMPORT;
            case 5 -> SOURCE_PROFILE;
            case 6 -> USER_PROFILE;
            case 7 -> EMAIL;
            case 8 -> EXPORT;
            case 9 -> FACEBOOK;
            case 10 -> WHATSAPP;
            case 11 -> INSTAGRAM;
            case 12 -> QUOTATION;
            case 13 -> WEBFORM_IMAGE;
            case 14 -> INVOICE;
            default -> throw ErrorCode.get(FileHolderErrorCodes.INVALID_FILE_CATEGORY);
        };
    }
}
