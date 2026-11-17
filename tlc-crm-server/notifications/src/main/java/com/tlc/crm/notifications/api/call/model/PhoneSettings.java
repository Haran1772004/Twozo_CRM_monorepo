package com.tlc.crm.notifications.api.call.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;

import com.tlc.i18n.I18nKey;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;
import java.util.Objects;

/**
 * A model which represents the phone settings
 *
 * @author Maharaja S
 * @version 1.0
 */
public class PhoneSettings implements TlcModel {

    private Long id;

    @NotNull(groups = Group.Update.class, message = "i18n.common.org.invalid.id")
    private final Long orgId;

    @NotNull(groups = Group.Update.class, message = "i18n.notifications.call.calling.service.required")
    @Size(min = 1, max = 1, groups = Group.Update.class, message = "i18n.notifications.call.min.calling.service.required")
    private Collection<CallingService> callingServices;

    @NotNull(groups = {Group.Update.class}, message = "i18n.notifications.call.calling.method.required")
    private CallingMethod preferredCallingMethod;

    private boolean isAutoCreateContactEnabled;

    private Long lifeCycleStageId;

    private boolean anyOrgBasedCallingServiceExist;

    public PhoneSettings(final Long orgId) {
        this.orgId = orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Collection<CallingService> getCallingServices() {
        return callingServices;
    }

    public void setCallingServices(final Collection<CallingService> callingServices) {
        this.callingServices = callingServices;
    }

    public CallingMethod getPreferredCallingMethod() {
        return preferredCallingMethod;
    }

    public void setPreferredCallingMethod(final CallingMethod preferredCallingMethod) {
        this.preferredCallingMethod = preferredCallingMethod;
    }

    public boolean isAnyOrgBasedCallingServiceExist() {
        return anyOrgBasedCallingServiceExist;
    }

    public void setAnyOrgBasedCallingServiceExist(final boolean anyOrgBasedCallingServiceExist) {
        this.anyOrgBasedCallingServiceExist = anyOrgBasedCallingServiceExist;
    }

    public boolean isAutoCreateContactEnabled() {
        return isAutoCreateContactEnabled;
    }

    public Long getLifeCycleStageId() {
        return lifeCycleStageId;
    }

    public void setLifeCycleStageId(Long lifeCycleStageId) {
        this.lifeCycleStageId = lifeCycleStageId;
    }

    public void setAutoCreateContactEnabled(boolean autoCreateContactEnabled) {
        isAutoCreateContactEnabled = autoCreateContactEnabled;
    }

    /**
     * {@inheritDoc}
     *
     * @return the id
     */
    @Override
    public Long id() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @return the org id
     */
    @Override
    public Long orgId() {
        return orgId;
    }

    /**
     * {@inheritDoc}
     *
     * @return the identity
     */
    @Override
    public Object identity() {
        return null;
    }

    /**
     * A record which contains the calling service details
     *
     * @param id          Represents the id of the calling service
     * @param orgId       Represents the org id
     * @param displayName Represents the display name of the calling service
     * @param name        Represents the name
     * @param isSelected  Represents whether the calling service is selected or not
     * @param appId       Represents the marketplace app id
     * @param appName     Represents the marketplace app name
     */
    public record CallingService(Long id, Long orgId, I18nKey displayName, String name, boolean isSelected, Long appId, String appName) {

        public CallingService(final Long id, final Long orgId) {
            this(id, orgId, null, null, false, null, null);
        }

        /**
         * <p>
         * Gets the calling service name. If appName is available, returns appName.
         * Otherwise, returns the service name. For TWOZO_DIALLER, returns the default provider name.
         * </p>
         *
         * @return The calling service name
         */
        public String getServiceName() {
            if (Objects.nonNull(appName)) {
                return appName;
            }

            if (TelephoneFields.TWOZO_DIALLER.equals(name)) {
                return TelephoneProvider.getDefaultProvider().name().toLowerCase();
            }

            return name;
        }
    }

    /**
     * An enum which represents the calling method
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum CallingMethod {

        ASK_EVERY_TIME((short) 1, "Ask every time"),
        WEB((short) 2, "Web"),
        MOBILE((short) 3, "Mobile");

        private final short id;
        private final String displayName;

        public short getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        CallingMethod(final short id, final String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        /**
         * <p>
         * Gets the {@link CallingMethod} based on the provided id
         * </p>
         *
         * @param id Represents the enum id
         * @return the {@link CallingMethod}
         */
        public static CallingMethod get(final short id) {
            return switch (id) {
                case 1 -> ASK_EVERY_TIME;
                case 2 -> WEB;
                case 3 -> MOBILE;
                default -> throw ErrorCode.get(NotificationErrorCodes.INVALID_CALLING_METHOD, "i18n.notifications.call.invalid.calling.method");
            };
        }
    }
}