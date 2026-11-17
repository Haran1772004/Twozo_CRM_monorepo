package com.tlc.crm.entity.campaign.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes for campaign folder actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum CampaignErrorCodes implements ErrorCodeProvider {

    CAMPAIGN_FOLDER_NAME_ALREADY_EXISTS(0x01),
    CAMPAIGN_FOLDER_NOT_FOUND(0x02),
    INVALID_CAMPAIGN_CATEGORY(0x03),
    INVALID_CAMPAIGN_FILTER_GROUP(0x04),
    INVALID_CAMPAIGN_DEFAULT_FILTER(0x05),
    CAMPAIGN_NOT_FOUND(0x06),
    CAMPAIGN_NAME_ALREADY_EXISTS(0x07),
    INVALID_CAMPAIGN_STATUS(0x08),
    INVALID_CAMPAIGN_RECIPIENT_STATUS(0x09),
    CAMPAIGN_NOT_READY_TO_SEND(0x10),
    CAMPAIGN_INVALID_SEND_STATUS(0x11),
    CAMPAIGN_VALIDATION_FAILED(0x14),
    CAMPAIGN_DELETION_FAILED(0x15),
    CAMPAIGN_TEMPLATE_OR_PHONE_NUMBER_UPDATE_RESTRICTED(0x16),
    MARKETING_LIST_NOT_FOUND(0x17),
    SEGMENT_NOT_FOUND(0x18),
    FAILED_TO_STOP_CAMPAIGN(0x19),
    FAILED_TO_PAUSE_CAMPAIGN(0x20),
    FAILED_TO_RESCHEDULE_CAMPAIGN(0x21),
    INVALID_RECIPIENT_TYPE(0x22),
    ;

    private final int code;

    CampaignErrorCodes(final int localCode) {
        this.code = CampaignErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class CampaignErrorCodesGroup implements ErrorCodeGroup {

        private static final ErrorCodeGroup GROUP = new CampaignErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x10_0_0016;
        }
    }
}