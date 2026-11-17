package com.tlc.notification.model.contact;

import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * A model that represents the people
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class People implements TlcModel {

    private Long id;
    private Long orgId;
    private String firstName;
    private String lastName;
    private Collection<String> emailAddresses;
    private Collection<String> phoneNumbers;
    private String company;
    private String jobTitle;
    private Long birthday;
    private String notes;
    private Address address;
    private Long createdTime;
    private Long modifiedTime;
    private String resourceName;
    private boolean isDeleted;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public String getFirstName() {
        return firstName;
    }

    public Collection<String> getEmailAddresses() {
        return emailAddresses;
    }

    public Collection<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public String getCompany() {
        return company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public Long getBirthday() {
        return birthday;
    }

    public String getNotes() {
        return notes;
    }

    public Address getAddress() {
        return address;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    public String getResourceName() {
        return resourceName;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public void setEmailAddresses(final Collection<String> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public void setPhoneNumbers(final Collection<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public void setCompany(final String company) {
        this.company = company;
    }

    public void setJobTitle(final String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public void setBirthday(final Long birthday) {
        this.birthday = birthday;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public void setCreatedTime(final Long createdTime) {
        this.createdTime = createdTime;
    }

    public void setModifiedTime(final Long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public void setResourceName(final String resourceName) {
        this.resourceName = resourceName;
    }

    public void setDeleted(final boolean deleted) {
        isDeleted = deleted;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    @Override
    public Object identity() {
        return null;
    }
}