package com.tlc.notification.model.telephone.mcube;

import com.tlc.commons.code.ErrorCode;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.telephone.TelephoneModel;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an MCube-specific telephony agent.
 *
 * @author Sathish
 * @version 1.0
 */
public class MCubeUser implements TelephoneModel, TlcModel {

    /**
     * Unique agent ID assigned by MCube upon creation.
     */
    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.notification.mcube.user.invalid.id")
    private String id;

    /**
     * First name of the MCube agent.
     */
    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.mcube.user.invalid.first.name")
    private String firstName;

    /**
     * Last name of the MCube agent.
     */
    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.mcube.user.invalid.last.name")
    private String lastName;

    /**
     * Email address of the MCube agent.
     */
    @NotNull(groups = {Group.Create.class, Group.Delete.class}, message = "i18n.notification.mcube.user.invalid.email")
    private String email;

    /**
     * Phone number of the MCube agent (used for click-to-call).
     */
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notification.mcube.user.invalid.phone")
    private String phoneNumber;

    private String previousPhoneNumber;

    private int roleId;

    /**
     * Authentication data associated with this user's MCube account.
     */
    private MCubeAuthData mCubeAuthenticationData;

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public MCubeAuthData getMCubeAuthData() {
        return mCubeAuthenticationData;
    }

    public void setMCubeAuthData(final MCubeAuthData mCubeAuthenticationData) {
        this.mCubeAuthenticationData = mCubeAuthenticationData;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(final int roleId) {
        this.roleId = roleId;
    }

    @Override
    public Long id() {
        throw ErrorCode.get(NotificationErrorCodes.UNSUPPORTED_OPERATION);
    }

    @Override
    public Long orgId() {
        throw ErrorCode.get(NotificationErrorCodes.UNSUPPORTED_OPERATION);
    }

    @Override
    public Object identity() {
        throw ErrorCode.get(NotificationErrorCodes.UNSUPPORTED_OPERATION);
    }

    public String getPreviousPhoneNumber() {
        return previousPhoneNumber;
    }

    public void setPreviousPhoneNumber(final String previousPhoneNumber) {
        this.previousPhoneNumber = previousPhoneNumber;
    }
}
