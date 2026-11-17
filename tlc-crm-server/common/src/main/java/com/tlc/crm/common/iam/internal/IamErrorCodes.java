package com.tlc.crm.common.iam.internal;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * <p>
 * Provides the error codes of crm iam module
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum IamErrorCodes implements ErrorCodeProvider {

    CRM_USER_NOT_FOUND(0x20),
    TEAM_NOT_FOUND(0x21),
    TEAM_NAME_ALREADY_EXISTS(0x22),
    TERRITORY_NOT_FOUND(0x23),
    TEAM_MUST_HAVE_ATLEAST_ONE_USER(0x24),
    CRM_USERS_NOT_FOUND(0x25),
    TERRITORY_MUST_HAVE_ATLEAST_ONE_USER_OR_TEAM(0x26),
    TEAMS_NOT_FOUND(0x27),
    TERRITORY_ALREADY_EXISTS(0x28),
    ROLE_NOT_FOUND(0x29),
    ROLE_NAME_ALREADY_EXISTS(0x30),
    ROLE_IS_ALREADY_MAPPED(0x31),
    ROLE_IS_NOT_MAPPED(0x31),
    AUTHORIZATION_FAILED(0x32),
    TEAM_IS_NOT_MAPPED(0x33),
    TERRITORY_IS_NOT_MAPPED(0x34),
    FAILED_TO_REGISTER_ORG_INITIATOR_ACTIONS(0x35),
    TERRITORY_IS_ALREADY_MAPPED(0x36),
    TEAM_IS_ALREADY_MAPPED(0x37),
    ROLE_CANNOT_BE_DELETED_DUE_TO_USER_MAPPING(0x38),
    USERS_SHOULD_NOT_BE_EMPTY(0x39),
    USER_RETRIEVAL_FAILED(0x40),
    ADMIN_ROLE_MODIFICATION_RESTRICTED(0x41),
    DEFAULT_ROLE_CANNOT_BE_DELETED(0x42),
    MANAGER_ROLE_UPDATE_ACTION_FAILED(0x43),
    USER_IS_NOT_PART_OF_TEAM(0x44),
    FAILED_TO_REGISTER_USER_DATA_MIGRATION_ACTION(0x45),
    USER_DATA_TRANSFER_IN_PROGRESS(0x46),
    USER_STATE_UPDATE_ACTION_FAILED(0x47),
    FIELD_IS_INVALID(0x48),
    USER_CANNOT_BE_UNLINKED_FROM_THE_ROLE(0x49),
    USER_DATA_TRANSFER_ALREADY_INITIALIZED(0x50),
    ROLE_UPDATE_RESTRICTED(0x51),
    ROLE_CREATION_RESTRICTED(0X52),
    ROLE_UPDATE_FAILED(0X53),
    TEAM_UPDATE_FAILED(0X54),
    TERRITORY_UPDATE_FAILED(0X55),
    EMAIL_ADDRESS_ALREADY_EXISTS(0x56),
    EMAIL_VERIFICATION_FAILED(0x57),
    ADMIN_ROLE_NOT_FOUND(0x58),
    ;

    private final int code;

    IamErrorCodes(final int localCode) {
        this.code = IamErrorCodes.IamErrorCodeGroup.GROUP.getConvertedCode(localCode);
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

    private static class IamErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new IamErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0x15_0_0001;
        }
    }
}