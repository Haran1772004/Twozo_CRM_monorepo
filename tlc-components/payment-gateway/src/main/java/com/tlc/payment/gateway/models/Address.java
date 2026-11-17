package com.tlc.payment.gateway.models;

import com.tlc.l10n.model.country.Country;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a postal address with basic address fields, including
 * address lines, city, state, country, and ZIP code.
 *
 * @author sathish
 * @version 1.0
 */
public class Address implements TlcModel {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.payment.gateway.payer.address.orgId.notnull")
    private Long orgId;

    private Long id;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private Country country;
    private String zip;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(final String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(final String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(final Country country) {
        this.country = country;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(final String zip) {
        this.zip = zip;
    }

    @Override
    public Object identity() {
        return null;
    }
}