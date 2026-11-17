package com.tlc.payment.gateway.models;

/**
 * Represents a payment method supported by the gateway.
 *
 * @author Sathish
 * @version 1.0
 */
public class PaymentMethod {

    private final String name;
    private final String description;
    private final Type type;

    private PaymentMethod(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.type = builder.type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Type getType() {
        return type;
    }

    public enum Type {

        CARD((short) 1, "card"),
        WALLET((short) 2, "wallet"),
        UPI((short) 3, "upi"),
        NET_BANKING((short) 4, "net_banking"),
        ;

        private final short id;
        private final String name;

        Type(final short id, String name) {
            this.id = id;
            this.name = name;
        }

        public short getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static Type getByName(final String name) {
            return switch (name) {
                case "card" -> CARD;
                case "wallet" -> WALLET;
                case "upi" -> UPI;
                case "net_banking" -> NET_BANKING;
                default -> null;
            };
        }
    }

    public static class Builder {

        private String name;
        private String description;
        private Type type;

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(final String description) {
            this.description = description;
            return this;
        }

        public Builder setType(final Type type) {
            this.type = type;
            return this;
        }

        public PaymentMethod build() {
            return new PaymentMethod(this);
        }
    }
}
