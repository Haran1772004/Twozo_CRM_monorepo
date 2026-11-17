package com.tlc.crm.entity.models.type;

import com.tlc.l10n.model.country.Country;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * <p>
 * A model that represents an address data
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class Address implements TlcModel {

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = Group.Delete.class, message = "i18n.entity.field.type.addressId.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.entity.field.type.orgId.required")
    private Long orgId;

    @SearchField(name = "city")
    @Size(max = 125, message = "i18n.entity.field.type.invalid.city", groups = {Group.Create.class, Group.Update.class})
    private String city;

    @SearchField(name = "state")
    @Size(max = 125, message = "i18n.entity.field.type.invalid.state", groups = {Group.Create.class, Group.Update.class})
    private String state;

    @SearchField(name = "country")
    private String countryAsString;

    private Country country;

    @SearchField(name = "zipcode")
    @Size(max = 25, message = "i18n.entity.field.type.invalid.zipcode", groups = {Group.Create.class, Group.Update.class})
    private String zipcode;

    @SearchField(name = "addressLine1")
    @Size(max = 300, message = "i18n.entity.field.type.invalid.addressLine1", groups = {Group.Create.class, Group.Update.class})
    private String addressLine1;

    @SearchField(name = "addressLine2")
    @Size(max = 300, message = "i18n.entity.field.type.invalid.addressLine2", groups = {Group.Create.class, Group.Update.class})
    private String addressLine2;

    @DecimalMin(value = "-90", groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.field.type.invalid.min.lat")
    @DecimalMax(value = "90", groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.field.type.invalid.max.lat")
    private Double latitude;

    @DecimalMin(value = "-180", groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.field.type.invalid.min.lon")
    @DecimalMax(value = "180", groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.field.type.invalid.max.lon")
    private Double longitude;

    public Address() {
    }

    private Address(final Builder builder) {
        final Country country = builder.getCountry();

        this.orgId = builder.getOrgId();
        this.id = builder.getId();
        this.addressLine1 = builder.getAddressLine1();
        this.addressLine2 = builder.getAddressLine2();
        this.city = builder.getCity();
        this.state = builder.getState();
        this.country = country;
        this.zipcode = builder.getZipcode();
        this.latitude = builder.getLatitude();
        this.longitude = builder.getLongitude();

        if (Objects.nonNull(country)) {
            this.countryAsString = country.getAliasName();
        }
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
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
        this.countryAsString = Objects.isNull(country) ? null : country.getAliasName();
    }

    public String getCountryAsString() {
        return countryAsString;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(final String zipcode) {
        this.zipcode = zipcode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(final Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(final Double longitude) {
        this.longitude = longitude;
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

    @Override
    public Object identity() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(addressLine1, addressLine2, city, state, zipcode, latitude, longitude, country, orgId);
    }

    /**
     *{@inheritDoc}
     *
     * @param object Represents the object to be compared
     * @return true if given object is equals to the current object
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof Address) {
            return this.hashCode() == object.hashCode();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return the string representation of the Address object
     */
    @Override
    public String toString() {
        return String.join(" ", Stream.of(addressLine1, addressLine2, city, state, countryAsString, zipcode).filter(Objects::nonNull).toList());
    }

    /**
     * <p>
     * Builds the Address objects, provides methods to set the various properties of an Address
     * </p>
     */
    public static class Builder {

        private Long id;
        private Long orgId;
        private String city;
        private String state;
        private Country country;
        private String zipcode;
        private Double latitude;
        private Double longitude;
        private String addressLine1;
        private String addressLine2;

        /**
         * <p>
         * Sets the id of the {@link Address}.
         * </p>
         *
         * @param id Represents the id of the address
         * @return Instance of {@link Builder}
         */
        public Builder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * <p>
         * Sets the org id of the {@link Address}.
         * </p>
         *
         * @param orgId Represents the org id of the address
         * @return Instance of {@link Builder}
         */
        public Builder orgId(final Long orgId) {
            this.orgId = orgId;
            return this;
        }

        /**
         * <p>
         * Sets the address line 1 of the {@link Address}
         * </p>
         *
         * @param addressLine1 Represents the address line 1.
         * @return Instance of {@link Builder}
         */
        public Builder addressLine1(final String addressLine1) {
            this.addressLine1 = addressLine1;
            return this;
        }

        /**
         * <p>
         * Sets the address line 2 of the {@link Address}
         * </p>
         *
         * @param addressLine2 Represents the address line 2.
         * @return Instance of {@link Builder}
         */
        public Builder addressLine2(final String addressLine2) {
            this.addressLine2 = addressLine2;
            return this;
        }

        /**
         * <p>
         * Sets the city name of the {@link Address}
         * </p>
         *
         * @param city Represents the name of the city.
         * @return Instance of {@link Builder}
         */
        public Builder city(final String city) {
            this.city = city;
            return this;
        }

        /**
         * <p>
         * Sets the city name of the {@link Address}
         * </p>
         *
         * @param state Represents the name of the state.
         * @return Instance of {@link Builder}
         */
        public Builder state(final String state) {
            this.state = state;
            return this;
        }

        /**
         * <p>
         * Sets the country name of the {@link Address}
         * </p>
         *
         * @param country Represents the name of the country.
         * @return Instance of {@link Builder}
         */
        public Builder country(final Country country) {
            this.country = country;
            return this;
        }

        /**
         * <p>
         * Sets the zip code of the {@link Address}.
         * </p>
         *
         * @param zipcode Represents the zip code of the address.
         * @return Instance of {@link Builder}
         */
        public Builder zipcode(final String zipcode) {
            this.zipcode = zipcode;
            return this;
        }

        /**
         * <p>
         * Sets the latitude of the {@link Address}
         * </p>
         *
         * @param latitude Represents the latitude of the address.
         * @return Instance of {@link Builder}.
         */
        public Builder latitude(final Double latitude) {
            this.latitude = latitude;
            return this;
        }

        /**
         * <p>
         * Sets the longitude of the {@link Address}
         * </p>
         *
         * @param longitude Represents the longitude of the address.
         * @return Instance of {@link Builder}.
         */
        public Builder longitude(final Double longitude) {
            this.longitude = longitude;
            return this;
        }

        /**
         * <p>
         * Builds the {@link Address} object.
         * </p>
         *
         * @return Instance of {@link Address}
         */
        public Address build() {
            return new Address(this);
        }

        /**
         * <p>
         * Gets the if the {@link Address}
         * </p>
         *
         * @return Id of the address.
         */
        public Long getId() {
            return this.id;
        }

        /**
         * <p>
         * Gets the org id of the {@link Address}
         * </p>
         *
         * @return Org id of the address.
         */
        public Long getOrgId() {
            return this.orgId;
        }

        /**
         * <p>
         * Gets the latitude of the {@link Address}.
         * </p>
         *
         * @return Latitude of the address.
         */
        public Double getLatitude() {
            return this.latitude;
        }

        /**
         * <p>
         * Gets the longitude of the {@link Address}
         * </p>
         *
         * @return Longitude of the address.
         */
        public Double getLongitude() {
            return this.longitude;
        }

        /**
         * <p>
         * Gets the city name of the {@link Address}
         * </p>
         *
         * @return City name of the address.
         */
        public String getCity() {
            return this.city;
        }

        /**
         * <p>
         * Gets the country name of the {@link Address}
         * </p>
         *
         * @return Country name of the address.
         */
        public Country getCountry() {
            return this.country;
        }

        /**
         * <p>
         * Gets the zip code of the {@link Address}.
         * </p>
         *
         * @return Zip code of the address.
         */
        public String getZipcode() {
            return this.zipcode;
        }

        /**
         * <p>
         * Gets the address line 2 of the {@link Address}
         * </p>
         *
         * @return Address line 2 of the address.
         */
        public String getAddressLine2() {
            return this.addressLine2;
        }

        /**
         * <p>
         * Gets the state name of the {@link Address}
         * </p>
         *
         * @return State name of the address
         */
        public String getState() {
            return this.state;
        }

        /**
         * <p>
         * Gets the address line 1 of the {@link Address}
         * </p>
         *
         * @return Address line 1
         */
        public String getAddressLine1() {
            return this.addressLine1;
        }
    }
}
