package com.tlc.crm.notifications.api.call.model;

import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.validator.TlcModel;

/**
 * Represents a phone number entity within the CRM system.
 * <p>
 * This class stores details such as the phone number itself, a user-friendly name,
 * associated location information, and recording preferences.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PhoneNumber implements TlcModel {

    private Long id;
    private Long orgId;
    private String number;
    private String displayName;
    private Locality locality;
    private RecordType recordType;
    private TelephoneProvider telephoneProvider;
    private boolean isActive;

    /**
     * {@inheritDoc}
     */
    @Override
    public Long id() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long orgId() {
        return orgId;
    }

    /**
     * Sets the org ID of the phone number.
     *
     * @param orgId Represents the organisation id.
     */
    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    /**
     * Sets the unique ID of the phone number.
     *
     * @param id The phone number ID to set.
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Retrieves the actual phone number.
     *
     * @return The phone number.
     */
    public String getNumber() {
        return number;
    }

    /**
     * Sets the actual phone number.
     *
     * @param number The phone number to set.
     */
    public void setNumber(final String number) {
        this.number = number;
    }

    /**
     * Retrieves the user-friendly name of the phone number.
     *
     * @return The friendly name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the user-friendly name for the phone number.
     *
     * @param displayName The friendly name to set.
     */
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Retrieves the locality details associated with this phone number.
     *
     * @return The {@link Locality} object.
     */
    public Locality getLocality() {
        return locality;
    }

    /**
     * Sets the locality details for this phone number.
     *
     * @param location The {@link Locality} object to set.
     */
    public void setLocality(final Locality location) {
        this.locality = location;
    }

    /**
     * Retrieves the recording preferences for this phone number.
     *
     * @return The {@link RecordType} associated with this number.
     */
    public RecordType getRecordType() {
        return recordType;
    }

    /**
     * Sets the recording preferences for this phone number.
     *
     * @param recordType The {@link RecordType} to set.
     */
    public void setRecordType(final RecordType recordType) {
        this.recordType = recordType;
    }

    public TelephoneProvider getTelephoneProvider() {
        return telephoneProvider;
    }

    public void setTelephoneProvider(final TelephoneProvider telephoneProvider) {
        this.telephoneProvider = telephoneProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object identity() {
        return null;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean isActive) {
        this.isActive = isActive;
    }
}