package com.tlc.social.common.whatsapp.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.social.common.internal.status.SocialErrorCodes;
import com.tlc.social.common.model.SocialEntity;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;
import java.util.Objects;

/**
 * A model which represents WhatsApp Business Account. One or many phone numbers can be registered with single WABA account
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WABusinessAccount implements SocialEntity {

    private Long id;
    @NotNull(groups = {Group.Create.class}, message = "i18n.social.whatsapp.business.id.required")
    private Long externalBusinessId;
    @NotNull(groups = {Group.Create.class}, message = "i18n.social.whatsapp.business.account.id.required")
    private Long externalWABAId;
    @Valid
    @Size(min = 1, groups = {Group.Create.class}, message = "i18n.social.whatsapp.business.min.phone.number.required")
    @NotNull(groups = {Group.Create.class}, message = "i18n.social.whatsapp.business.phone.number.required")
    private Collection<WABusinessPhoneNumber> businessPhoneNumbers;
    private TemplateSyncStatus templateSyncStatus;
    private DateTime lastSyncedTime;

    public void setExternalBusinessId(final Long externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public Long getExternalWABAId() {
        return externalWABAId;
    }

    public void setExternalWABAId(final Long externalWABAId) {
        this.externalWABAId = externalWABAId;
    }

    public Collection<WABusinessPhoneNumber> getBusinessPhoneNumbers() {
        return businessPhoneNumbers;
    }

    public void setBusinessPhoneNumbers(final Collection<WABusinessPhoneNumber> businessPhoneNumbers) {
        this.businessPhoneNumbers = businessPhoneNumbers;
    }

    public Long getExternalBusinessId() {
        return externalBusinessId;
    }

    public WABusinessPhoneNumber getBusinessPhoneNumber() {
        return Objects.nonNull(businessPhoneNumbers) && !businessPhoneNumbers.isEmpty() ? businessPhoneNumbers.stream().findFirst().get() : null;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        throw ErrorCode.get(SocialErrorCodes.ORG_ID_NOT_SUPPORTED, "i18n.social.model.org.id.not.supported");
    }

    @Override
    public Object identity() {
        return null;
    }

    public TemplateSyncStatus getSyncStatus() {
        return templateSyncStatus;
    }

    public void setSyncStatus(final TemplateSyncStatus templateSyncStatus) {
        this.templateSyncStatus = templateSyncStatus;
    }

    public DateTime getLastSyncedTime() {
        return lastSyncedTime;
    }

    public void setLastSyncedTime(final DateTime lastSyncedTime) {
        this.lastSyncedTime = lastSyncedTime;
    }

    /**
     * <p>
     * Represents the status of template sync with WhatsApp Business Account.
     * </p>
     */
    public enum TemplateSyncStatus {

        NEW((short) 1),
        IN_PROGRESS((short) 2),
        SYNCED((short) 3),
        FAILED((short) 4);

        private final short id;

        TemplateSyncStatus(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the status of {@link TemplateSyncStatus}.
         * </p>
         *
         * @param id Represents the syncStatus id
         * @return {@link TemplateSyncStatus}
         */
        public static TemplateSyncStatus getStatus(final short id) {
            return switch (id) {
                case 1 -> NEW;
                case 2 -> IN_PROGRESS;
                case 3 -> SYNCED;
                case 4 -> FAILED;
                default -> throw ErrorCode.get(WAErrorCodes.INVALID_SYNC_STATUS,"i18n.social.whatsapp.sync.status.invalid");
            };
        }

        /**
         * <p>
         * Checks whether the sync status is completed or not.
         * </p>
         *
         * @return true if the sync status is completed, false otherwise
         */
        public boolean isCompleted() {
            return this == SYNCED || this == FAILED;
        }
    }
}