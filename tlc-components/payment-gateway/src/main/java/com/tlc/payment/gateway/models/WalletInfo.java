package com.tlc.payment.gateway.models;

/**
 * Represents wallet payment method details.
 *
 * @author Sathish
 * @version 1.0
 */
public final class WalletInfo implements PaymentMethodInfo {

    private final String id;
    private final String mobileNumber;
    private final String name;
    private final String provider;
    private final Status status;
    private final String token;
    private final boolean isDefault;

    private WalletInfo(final Builder builder) {
        this.id = builder.id;
        this.mobileNumber = builder.mobileNumber;
        this.name = builder.name;
        this.provider = builder.provider;
        this.status = builder.status;
        this.token = builder.token;
        this.isDefault = builder.isDefault;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public PaymentMethod.Type getPaymentType() {
        return PaymentMethod.Type.WALLET;
    }

    /**
     * Builder class for constructing {@link WalletInfo} instances.
     * <p>
     * Provides fluent setters for each field and a {@code build()} method
     * to create immutable {@code WalletInfo} objects.
     * </p>
     */
    public static class Builder {

        private String id;
        private String mobileNumber;
        private String name;
        private String provider;
        private Status status;
        private String token;
        private boolean isDefault;

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder mobileNumber(final String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder setProvider(final String provider) {
            this.provider = provider;
            return this;
        }

        public Builder status(final Status status) {
            this.status = status;
            return this;
        }

        public Builder token(final String token) {
            this.token = token;
            return this;
        }

        public Builder isDefault(final boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        /**
         * Builds and returns a new {@link WalletInfo} instance.
         *
         * @return constructed immutable {@link WalletInfo} object
         */
        public WalletInfo build() {
            return new WalletInfo(this);
        }
    }
}