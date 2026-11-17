package com.tlc.crm.common.iam.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.preferences.model.OrgPreference;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * A model that represents the {@link CrmOrg}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class CrmOrg implements TlcModel {

    private Long id;
    private Long iamOrgId;
    private InitializationStatus initializationStatus;

    private final OrgPreferenceHolder orgPreferenceHolder;

    public CrmOrg() {
        orgPreferenceHolder = new OrgPreferenceHolder();
    }

    /**
     * <p>
     * Represents the different phases of org initialization process
     * </p>
     */
    public enum InitializationStatus {

        NEW(1),
        COMPLETED(2),
        FAILED(3),
        INTERNALLY_COMPLETED(4),
        SIGNUP_COMPLETED(5),
        ;

        private final int id;

        public int getId() {
            return id;
        }

        InitializationStatus(final int id) {
            this.id = id;
        }

        /**
         * Gets the {@link InitializationStatus} by its ID
         *
         * @param id Represents the {@link InitializationStatus} ID
         * @return the {@link InitializationStatus} associated with the ID
         */
        public static InitializationStatus get(final int id) {
            return switch (id) {
                case 1 -> InitializationStatus.NEW;
                case 2 -> InitializationStatus.COMPLETED;
                case 3 -> InitializationStatus.FAILED;
                case 4 -> INTERNALLY_COMPLETED;
                case 5 -> SIGNUP_COMPLETED;
                default -> throw ErrorCode.get(CommonErrorCodes.INVALID_ORGANIZATION_INITIALIZATION_STATUS);
            };
        }
    }

    public InitializationStatus getInitializationStatus() {
        return initializationStatus;
    }

    public void setInitializationStatus(final InitializationStatus initializationStatus) {
        this.initializationStatus = initializationStatus;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getIamOrgId() {
        return iamOrgId;
    }

    public void setIamOrgId(final Long iamOrgId) {
        this.iamOrgId = iamOrgId;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return null;
    }

    @Override
    public Object identity() {
        return null;
    }

    public OrgPreferenceHolder getPreferenceHolder() {
        return orgPreferenceHolder;
    }

    public void addPreference(final OrgPreference preference) {
        this.orgPreferenceHolder.add(preference);
    }
}
