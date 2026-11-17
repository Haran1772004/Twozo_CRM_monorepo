package com.tlc.crm.core.internal.deal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Contains the {@link com.tlc.crm.core.api.deal.model.Deal} related Error codes
 *
 * @author Sundar
 * @version 1.0
 */
public enum DealErrorCodes implements ErrorCodeProvider {
    DEAL_NOT_FOUND(0x01),
    CONTACT_NOT_FOUND(0x02),
    PIPELINE_STAGE_NOT_FOUND(0x03),
    INVALID_EXPECTED_CLOSE_DATE(0x05),

    PIPELINE_NOT_FOUND(0x07),
    CONTACT_ALREADY_LINKED(0x08),
    PARTICIPANT_NOT_FOUND(0x09),
    PIPELINE_ALREADY_EXISTS(0X10),
    PIPELINE_STAGE_ALREADY_EXISTS(0X11),
    INVALID_DEAL_STATE(0X13),
    FIELD_IS_NOT_CONFIGURED_FOR_SORTING(0x14),
    INVALID_SORT_TYPE(0x15),
    PRODUCT_DETAILS_HOLDER_NOT_FOUND(0x16),
    PRODUCT_DETAILS_NOT_FOUND(0x17),
    PRODUCT_NOT_FOUND(0x18),
    INVALID_VARIATION_ID(0x19),
    CURRENCY_NOT_FOUND(0x20),
    DEAL_ID_REQUIRED(0x21),
    PIPELINE_DELETION_RESTRICTED(0x22),
    PIPELINE_STAGE_DELETION_RESTRICTED(0x23),
    DUPLICATE_STAGE_ORDER(0x24),
    LOST_REASON_NOT_FOUND(0x25),
    CLOSED_TIME_NOT_FOUND(0x26),
    WON_REASON_NOT_FOUND(0x27),
    UPDATE_ACTION_FAILED(0x28),
    COMPANY_NOT_FOUND(0x30),
    INVALID_PIPELINE_OR_STAGE(0x31),
    INVALID_OPTION(0x29),
    SAME_PRIMARY_AND_SECONDARY_ID_RESTRICTED(0x30),
    UNABLE_TO_LINK_PRODUCTS_WITH_DEAL_AS_PRODUCT_IS_DISABLED(0x31),
    PIPELINE_MUST_BE_DEFAULT(0x32),
    PIPELINE_LIMIT_REACHED(0x33),
    PIPELINE_STAGE_LIMIT_REACHED(0x34),
    DEFAULT_PIPELINE_EXISTS(0x35),
    DEFAULT_PIPELINE_STAGE_EXISTS(0x36),
    WON_OR_LOST_STAGE_UPDATE_RESTRICTED(0X37),
    IS_DEFAULT_UPDATE_RESTRICTED(0X38),
    DEFAULT_PIPELINE_STAGE_SHOULD_BE_POSITIONED_FIRST(0x39),
    NON_DRAGGABLE_STAGES_CANNOT_BE_REORDERED(0x40),
    PIPELINE_STAGE_REMAINS_UNCHANGED(0X41),
    INVALID_PIPELINE_STAGE_NAME(0x42),
    DEAL_STATE_STAGES_MISSING(0x43),
    CRM_USER_NOT_FOUND(0x44),
    TERRITORY_NOT_FOUND(0x45),
    USER_RETRIEVAL_FAILED(0x46),
    INVALID_FORECAST_INTERVAL(0x47),
    INVALID_FORECAST_ARRANGEMENT(0x48),
    INVALID_FORECAST_STAGE(0x49),
    TAG_LIMIT_EXCEEDED(0x50),
    TAG_NOT_FOUND(0x51),
    TAG_NAME_ALREADY_EXISTS(0x52),
    PIPELINE_STAGE_NAME_LIMIT_EXCEEDED(0x53),
    PIPELINE_ID_REQUIRED(0x54),
    ;

    private final int code;

    DealErrorCodes(int localCode) {
        this.code = localCode;
    }

    /**
     * {@inheritDoc}
     *
     * @return the code
     */
    @Override
    public int getCode() {
        return code;
    }

    private static class DealErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new DealErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x25_0_0001;
        }
    }
}