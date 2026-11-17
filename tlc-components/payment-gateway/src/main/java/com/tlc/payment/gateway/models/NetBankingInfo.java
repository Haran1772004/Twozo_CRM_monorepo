package com.tlc.payment.gateway.models;

/**
 * Represents information related to a net banking payment method.
 *
 * @author Sathish
 * @version 1.0
 */
public final class NetBankingInfo implements PaymentMethodInfo {

    private final String id;
    private final String accountReference;
    private final String accountantName;
    private final String bankName;
    private final String token;
    private final Status status;
    private final boolean isDefault;

    private NetBankingInfo(final Builder builder) {
        this.id = builder.id;
        this.accountReference = builder.accountReference;
        this.accountantName = builder.accountantName;
        this.bankName = builder.bankName;
        this.token = builder.token;
        this.status = builder.status;
        this.isDefault = builder.isDefault;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getAccountReference() {
        return accountReference;
    }

    public String getAccountantName() {
        return accountantName;
    }

    public String getBankName() {
        return bankName;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public PaymentMethod.Type getPaymentType() {
        return PaymentMethod.Type.NET_BANKING;
    }

    /**
     * Builder class for constructing {@link NetBankingInfo} instances.
     * <p>
     * Provides fluent setters for each field and a {@code build()} method
     * to create immutable {@code WalletInfo} objects.
     * </p>
     */
    public static class Builder {

        private String id;
        private String accountReference;
        private String accountantName;
        private String bankName;
        private Status status;
        private String token;
        private boolean isDefault;

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder accountReference(final String accountReference) {
            this.accountReference = accountReference;
            return this;
        }

        public Builder accountantName(final String accountantName) {
            this.accountantName = accountantName;
            return this;
        }

        public Builder bankName(final String bankName) {
            this.bankName = bankName;
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
         * Builds and returns a new {@link NetBankingInfo} instance.
         *
         * @return constructed immutable {@link NetBankingInfo} object
         */
        public NetBankingInfo build() {
            return new NetBankingInfo(this);
        }
    }
}
