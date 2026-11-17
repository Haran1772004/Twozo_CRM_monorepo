package com.tlc.crm.zapier.api.models;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import java.util.Optional;

/**
 * Represents the {@link ZapAction} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ZapAction {

    CREATE_CONTACT("createContact", (short) 1),
    UPDATE_CONTACT("updateContact", (short) 2),
    CREATE_COMPANY("createCompany", (short) 3),
    UPDATE_COMPANY("updateCompany", (short) 4),
    CREATE_DEAL("createDeal", (short) 5),
    UPDATE_DEAL("updateDeal", (short) 6),
    CREATE_PRODUCT("createProduct", (short) 7),
    UPDATE_PRODUCT("updateProduct", (short) 8),
    CREATE_ACTIVITY("createActivity", (short) 9),
    UPDATE_ACTIVITY("updateActivity", (short) 10),
    ADD_PRODUCT_TO_DEAL("addProductToDeal", (short) 11),
    ADD_NOTE_TO_CONTACT("addNoteToDeal", (short) 12),
    ADD_NOTE_TO_COMPANY("addNoteToCompany", (short) 13),
    ADD_NOTE_TO_DEAL("addNoteToDeal", (short) 14),
    ADD_FOLLOWER_TO_CONTACT("addFollowerToContact", (short) 15),
    ADD_FOLLOWER_TO_COMPANY("addFollowerToCompany", (short) 16),
    ADD_FOLLOWER_TO_DEAL("addFollowerToDeal", (short) 17),
    ADD_TAGS_TO_CONTACT("addTagsToContact", (short) 18),
    ADD_TAGS_TO_COMPANY("addTagsToCompany", (short) 19),
    ADD_TAGS_TO_DEAL("addTagsToDeal", (short) 20),
    ;

    private final String aliasName;
    private final short id;

    ZapAction(final String aliasName, final short id) {
        this.aliasName = aliasName;
        this.id = id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public short getId() {
        return id;
    }

    /**
     * Retrieves the {@link ZapAction} corresponding to the given ID.
     *
     * @param id The unique short ID of the ZapAction.
     * @return The matching {@link ZapAction}.
     */
    public static ZapAction get(final short id) {
        return switch (id) {
            case 1 -> CREATE_CONTACT;
            case 2 -> UPDATE_CONTACT;
            case 3 -> CREATE_COMPANY;
            case 4 -> UPDATE_COMPANY;
            case 5 -> CREATE_DEAL;
            case 6 -> UPDATE_DEAL;
            case 7 -> CREATE_PRODUCT;
            case 8 -> UPDATE_PRODUCT;
            case 9 -> CREATE_ACTIVITY;
            case 10 -> UPDATE_ACTIVITY;
            case 11 -> ADD_PRODUCT_TO_DEAL;
            case 12 -> ADD_NOTE_TO_CONTACT;
            case 13 -> ADD_NOTE_TO_COMPANY;
            case 14 -> ADD_NOTE_TO_DEAL;
            case 15 -> ADD_FOLLOWER_TO_CONTACT;
            case 16 -> ADD_FOLLOWER_TO_COMPANY;
            case 17 -> ADD_FOLLOWER_TO_DEAL;
            case 18 -> ADD_TAGS_TO_CONTACT;
            case 19 -> ADD_TAGS_TO_COMPANY;
            case 20 -> ADD_TAGS_TO_DEAL;
            default -> throw ErrorCode.get(ZapierErrorCodes.INVALID_ZAP_ACTION, "i18n.crm.zapier.action.invalid");
        };
    }

    /**
     * Checks the action is create event or not.
     */
    public boolean isCreateAction() {
        return switch (this) {
            case CREATE_CONTACT, CREATE_COMPANY, CREATE_DEAL,
                 CREATE_ACTIVITY, CREATE_PRODUCT -> true;
            default -> false;
        };
    }

    /**
     * Retrieves the source string associated with the given {@link ZapAction}, if available.
     *
     * @param zapAction The Zapier action for which the source is to be fetched.
     * @return An {@link Optional} containing the source if present, or empty if not defined.
     */
    public static Optional<Source> getSource(final ZapAction zapAction) {
        return Optional.ofNullable(switch (zapAction) {
            case CREATE_CONTACT, UPDATE_CONTACT -> Source.CONTACT;
            case CREATE_DEAL, UPDATE_DEAL -> Source.DEAL;
            case CREATE_COMPANY, UPDATE_COMPANY -> Source.COMPANY;
            case CREATE_ACTIVITY, UPDATE_ACTIVITY -> Source.ACTIVITY;
            case CREATE_PRODUCT, UPDATE_PRODUCT -> Source.PRODUCT;
            default -> null;
        });
    }
}