package com.tlc.payment.gateway.models;

/**
 * Represents detailed information about a card payment type.
 *
 * @author Sathish
 * @version 1.0
 */
public class CardInfo implements PaymentMethodInfo {

    private final String id;
    private final String number;
    private final String cardHolderName;
    private final String issuer;
    private final String network;
    private final String token;
    private final String expiryMonth;
    private final String expiryYear;
    private final String provider;
    private final boolean isExpired;
    private final boolean isDefault;
    private final CardType type;
    private final Status status;

    private CardInfo(final Builder builder) {
        this.id = builder.id;
        this.number = builder.number;
        this.cardHolderName = builder.cardHolderName;
        this.issuer = builder.issuer;
        this.network = builder.network;
        this.token = builder.token;
        this.expiryMonth = builder.expiryMonth;
        this.expiryYear = builder.expiryYear;
        this.provider = builder.provider;
        this.isExpired = builder.isExpired;
        this.isDefault = builder.isDefault;
        this.type = builder.type;
        this.status = builder.status;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public PaymentMethod.Type getPaymentType() {
        return PaymentMethod.Type.CARD;
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

    public String getNumber() {
        return number;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getNetwork() {
        return network;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public String getProvider() {
        return provider;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public CardType getType() {
        return type;
    }

    /**
     * Builder class for constructing {@link CardInfo} instances.
     * <p>
     * Provides fluent setters for each field and a {@code build()} method
     * to create immutable {@code WalletInfo} objects.
     * </p>
     */
    public static class Builder {

        private String id;
        private String number;
        private String cardHolderName;
        private String issuer;
        private String network;
        private String token;
        private String expiryMonth;
        private String expiryYear;
        private String provider;
        private boolean isExpired;
        private boolean isDefault;
        private CardType type;
        private Status status;

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder number(final String number) {
            this.number = number;
            return this;
        }

        public Builder cardHolderName(final String cardHolderName) {
            this.cardHolderName = cardHolderName;
            return this;
        }

        public Builder issuer(final String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder network(final String network) {
            this.network = network;
            return this;
        }

        public Builder token(final String token) {
            this.token = token;
            return this;
        }

        public Builder expiryMonth(final String expiryMonth) {
            this.expiryMonth = expiryMonth;
            return this;
        }

        public Builder expiryYear(final String expiryYear) {
            this.expiryYear = expiryYear;
            return this;
        }

        public Builder provider(final String provider) {
            this.provider = provider;
            return this;
        }

        public Builder isExpired(final boolean expired) {
            isExpired = expired;
            return this;
        }

        public Builder isDefault(final boolean aDefault) {
            isDefault = aDefault;
            return this;
        }

        public Builder type(final CardType type) {
            this.type = type;
            return this;
        }

        public Builder status(final Status status) {
            this.status = status;
            return this;
        }

        /**
         * Builds and returns a new {@link CardInfo} instance.
         *
         * @return constructed immutable {@link CardInfo} object
         */
        public CardInfo build() {
            return new CardInfo(this);
        }
    }
}
