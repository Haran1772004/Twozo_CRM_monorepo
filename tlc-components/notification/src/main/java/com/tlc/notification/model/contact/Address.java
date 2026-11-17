package com.tlc.notification.model.contact;

/**
 * <p>
 * A model that represents the Address
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class Address {

    private String addressLine1;
    private String addressLine2;
    private String country;
    private String state;
    private String city;
    private String zipcode;
    private String fullAddress;

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

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(final String zipcode) {
        this.zipcode = zipcode;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(final String fullAddress) {
        this.fullAddress = fullAddress;
    }
}
