package com.tlc.crm.core.api.subscription.model;

import com.tlc.crm.entity.models.type.Monetary;

/**
 * Summarizes the entity data that may be lost for an organization.
 *
 * @author Sathish
 * @version 1.0
 */
public class DataLossSummary {

    private String openDealsValue;
    private int openDealsCount;
    private int contactsCount;
    private int companiesCount;
    private int scheduledActivitiesCount;

    public String getOpenDealsValue() {
        return openDealsValue;
    }

    public void setOpenDealsValue(final Monetary openDealsValue) {
        this.openDealsValue = openDealsValue.getSymbol().concat(openDealsValue.getValue().toString());
    }

    public void setOpenDealsValue(final String openDealsValue) {
        this.openDealsValue = openDealsValue;
    }

    public int getOpenDealsCount() {
        return openDealsCount;
    }

    public void setOpenDealsCount(final int openDealsCount) {
        this.openDealsCount = openDealsCount;
    }

    public int getContactsCount() {
        return contactsCount;
    }

    public void setContactsCount(final int contactsCount) {
        this.contactsCount = contactsCount;
    }

    public int getCompaniesCount() {
        return companiesCount;
    }

    public void setCompaniesCount(final int companiesCount) {
        this.companiesCount = companiesCount;
    }

    public int getScheduledActivitiesCount() {
        return scheduledActivitiesCount;
    }

    public void setScheduledActivitiesCount(final int scheduledActivitiesCount) {
        this.scheduledActivitiesCount = scheduledActivitiesCount;
    }
}