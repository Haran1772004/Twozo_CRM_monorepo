package com.tlc.payment.gateway.models;

/**
 * Represents UPI (Unified Payments Interface) payment method details.
 *
 * @author Sathish
 * @version 1.0
 */
public final class UPIInfo implements PaymentMethodInfo {

    private final String id;
    private final String channel;
    private final String vpa;
    private final String token;
    private final boolean isDefault;
    private final Status status;

    private UPIInfo(final Builder builder) {
        this.id = builder.id;
        this.channel = builder.channel;
        this.vpa = builder.vpa;
        this.token = builder.token;
        this.isDefault = builder.isDefault;
        this.status = builder.status;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getChannel() {
        return channel;
    }

    public String getVpa() {
        return vpa;
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
    public Status getStatus() {
        return status;
    }

    @Override
    public PaymentMethod.Type getPaymentType() {
        return PaymentMethod.Type.UPI;
    }

    /**
     * Builder class for constructing {@link UPIInfo} instances.
     * <p>
     * Provides fluent setters for each field and a build() method to create
     * immutable UPIInfo objects.
     * </p>
     */
    public static class Builder {

        private String id;
        private String channel;
        private String vpa;
        private String token;
        private boolean isDefault;
        private Status status;

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder channel(final String channel) {
            this.channel = channel;
            return this;
        }

        public Builder vpa(final String vpa) {
            this.vpa = vpa;
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

        public Builder status(final Status status) {
            this.status = status;
            return this;
        }

        /**
         * Builds and returns a new {@link UPIInfo} instance.
         *
         * @return constructed immutable {@link UPIInfo} object
         */
        public UPIInfo build() {
            return new UPIInfo(this);
        }
    }
}