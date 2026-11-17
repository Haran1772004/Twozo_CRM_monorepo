package com.tlc.notification.model.telephone.telecmi;

import com.tlc.commons.code.ErrorCode;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.telephone.TelephoneModel;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a TeleCMI-specific telephony agent.
 *
 * @author kishore J
 * @version 1.0
 */
public class TeleCMIUser implements TelephoneModel, TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.notification.telecmi.user.invalid.id")
    private String id;
    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.telecmi.user.invalid.first.name")
    private  String firstName;
    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.telecmi.user.invalid.last.name")
    private String lastName;
    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.telecmi.user.invalid.email")
    private String email;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notification.telecmi.user.invalid.password")
    private String password;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notification.telecmi.user.invalid.phone")
    private String phoneNumber;
    @NotNull(groups = {Group.Create.class}, message = "i18n.notification.telecmi.user.extension")
    private Integer extension;

    private TeleCMIAuthData teleCMIAuthenticationData;

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Integer getExtension() {
        return extension;
    }

    public void setExtension(final Integer extension) {
        this.extension = extension;
    }

    public TeleCMIAuthData getTeleCMIAuthData() {
        return teleCMIAuthenticationData;
    }

    public void setTeleCMIAuthData(final TeleCMIAuthData teleCMIAuthenticationData) {
        this.teleCMIAuthenticationData = teleCMIAuthenticationData;
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
}
