package com.tlc.payment.gateway.models;

import com.tlc.commons.util.StringUtil;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Represents a payer in the payment gateway system.
 *
 * @author Sathish
 * @version 1.0
 */
public class Payer implements TlcModel {

    @NotNull(groups = Group.Update.class, message = "i18n.payment.gateway.payer.id.notnull")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.payment.gateway.payer.orgId.notnull")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.payment.gateway.payer.fullName.notnull")
    private String fullName;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.payment.gateway.payer.firstName.notnull")
    private String firstName;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.payment.gateway.payer.lastName.notnull")
    private String lastName;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.payment.gateway.payer.company.notnull")
    private String company;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.payment.gateway.payer.email.notnull")
    @Size(max = 356, groups = {Group.Create.class, Group.Update.class}, message = "i18n.payment.gateway.payer.email.size")
    private String email;

    @Valid
    private Address address;

    @Size(max = 25, groups = {Group.Create.class, Group.Update.class}, message = "i18n.payment.gateway.payer.phone.size")
    private String phone;
    private String phoneCountryCode;
    private String phoneWithCountryCode;
    private PayerLocalDetails localDetails;
    private String refenceId;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getRefenceId() {
        return refenceId;
    }

    public void setRefenceId(final String refenceId) {
        this.refenceId = refenceId;
    }

    public PayerLocalDetails getLocalDetails() {
        return localDetails;
    }

    public void setLocalDetails(final PayerLocalDetails localDetails) {
        this.localDetails = localDetails;
    }

    public String getFullName() {
        return fullName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
        updateFullName();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
        updateFullName();
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(final String company) {
        this.company = company;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public String getPhoneCountryCode() {
        return phoneCountryCode;
    }

    public void setPhoneCountryCode(final String phoneCountryCode) {
        this.phoneCountryCode = phoneCountryCode;
    }

    public String getPhoneWithCountryCode() {
        return phoneWithCountryCode = Objects.isNull(phoneWithCountryCode) ? Objects.isNull(phoneCountryCode) || Objects.isNull(phone)
                ? phone : StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, phoneCountryCode, phone) : phoneWithCountryCode;
    }

    public void setPhoneWithCountryCode(final String phoneWithCountryCode) {
        this.phoneWithCountryCode = phoneWithCountryCode;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    private void updateFullName() {
        fullName = (Objects.isNull(firstName) || firstName.isBlank()) ? lastName
                : (Objects.isNull(lastName) || lastName.isBlank()) ? firstName : firstName + " " + lastName;
    }

    @Override
    public Object identity() {
        return null;
    }
}