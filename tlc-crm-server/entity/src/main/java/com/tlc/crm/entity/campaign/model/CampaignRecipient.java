package com.tlc.crm.entity.campaign.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.campaign.status.CampaignErrorCodes;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Base type for a campaign audience (marketing list or segment).
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class CampaignRecipient implements TlcModel {

    private final RecipientType recipientType;

    private long contactCount;

    protected CampaignRecipient(final RecipientType recipientType) {
        this.recipientType = recipientType;
    }

    public RecipientType getRecipientType() {
        return recipientType;
    }

    public long getContactCount() {
        return contactCount;
    }

    public void setContactCount(final long contactCount) {
        this.contactCount = contactCount;
    }

    /**
     * <p>
     * Type of campaign recipient.
     * </p>
     */
    public enum RecipientType {

        MARKETING_LIST((short) 1),
        SEGMENT((short) 2);

        private final short id;

        RecipientType(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link RecipientType} by id
         * </p>
         *
         * @param id Represents the id
         * @return the {@link RecipientType}
         */
        public static RecipientType get(final short id) {
            return switch (id){
                case 1 -> MARKETING_LIST;
                case 2 -> SEGMENT;
                default -> throw ErrorCode.get(CampaignErrorCodes.INVALID_RECIPIENT_TYPE, "i18n.campaign.invalid.recipient.type");
            };
        }
    }
}