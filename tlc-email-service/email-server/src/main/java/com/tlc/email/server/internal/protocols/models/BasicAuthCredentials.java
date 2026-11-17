package com.tlc.email.server.internal.protocols.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.crypto.EncryptionHelper;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.address.EmailAddress;

import java.util.Objects;

/**
 * <p>
 * Represents basic username/password authentication credentials
 * for connecting to an email server.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class BasicAuthCredentials extends AbstractAuthCredentials {

    private final EncryptionHelper encryptionHelper = EncryptionHelper.getInstance();

    private final String username;
    private final String password;
    private final String encryptedPassword;

    /**
     * <p>
     * Private constructor invoked via {@link Builder}.
     * </p>
     *
     * <p>
     * Initializes the credentials by encrypting the plain password
     * if provided, or decrypting the encrypted password otherwise.
     * </p>
     *
     * @param builder the builder containing credential data
     */
    private BasicAuthCredentials(final Builder builder) {
        this.username = builder.username;
        this.authType = builder.authType;
        this.emailAddress = builder.emailAddress;
        final String plainPassword = builder.plainPassword;

        if (Objects.isNull(plainPassword)) {
            this.password = decrypt(this.encryptedPassword = builder.encryptedPassword);
        } else {
            this.encryptedPassword = encrypt(this.password = plainPassword);
        }
    }

    /**
     * <p>
     * Returns the username used for authentication.
     * </p>
     *
     * @return authentication username
     */
    public String username() {
        return username;
    }

    /**
     * <p>
     * Returns the authentication type.
     * </p>
     *
     * @return authentication type
     */
    public AuthType authType() {
        return authType;
    }

    /**
     * <p>
     * Returns the encrypted password.
     * </p>
     *
     * @return encrypted password
     */
    public String encryptedPassword() {
        return encryptedPassword;
    }

    /**
     * <p>
     * Returns the decrypted password.
     * </p>
     *
     * @return decrypted password
     */
    public String password() {
        return password;
    }

    /**
     * <p>
     * Encrypts the given plain password using the email address
     * as the encryption secret.
     * </p>
     *
     * @param password plain password
     * @return encrypted password
     */
    private String encrypt(final String password) {
        return encryptionHelper.encrypt(emailAddress.getEmailAddress(), password);
    }

    /**
     * <p>
     * Decrypts the given encrypted password using the email address
     * as the encryption secret.
     * </p>
     *
     * @param encryptedPassword encrypted password
     * @return decrypted password
     */
    private String decrypt(final String encryptedPassword) {
        return encryptionHelper.decrypt(emailAddress.getEmailAddress(), encryptedPassword);
    }

    /**
     * <p>
     * Returns a new {@link Builder} instance for constructing
     * {@link BasicAuthCredentials}.
     * </p>
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Builder class for constructing {@link BasicAuthCredentials} instances.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static final class Builder {

        private String username;
        private AuthType authType;
        private EmailAddress emailAddress;

        private String plainPassword;
        private String encryptedPassword;

        private Builder() {
        }

        /**
         * Sets the authentication username.
         *
         * @param username authentication username
         * @return this builder
         */
        public Builder username(final String username) {
            this.username = username;

            return this;
        }

        /**
         * Sets the authentication type.
         *
         * @param authType authentication type
         * @return this builder
         */
        public Builder authType(final AuthType authType) {
            this.authType = authType;

            return this;
        }

        /**
         * Sets the email address associated with the credentials.
         *
         * @param emailAddress email address
         * @return this builder
         */
        public Builder emailAddress(final EmailAddress emailAddress) {
            this.emailAddress = emailAddress;

            return this;
        }

        /**
         * <p>
         * Sets the plain password.
         * </p>
         *
         * @param password plain password
         * @return this builder
         */
        public Builder plainPassword(final String password) {
            this.plainPassword = password;

            return this;
        }

        /**
         * <p>
         * Sets the encrypted password.
         * </p>
         *
         * @param encryptedPassword encrypted password
         * @return this builder
         */
        public Builder encryptedPassword(final String encryptedPassword) {
            this.encryptedPassword = encryptedPassword;

            return this;
        }

        /**
         * <p>
         * Builds and returns a {@link BasicAuthCredentials} instance.
         * </p>
         *
         * @return {@link BasicAuthCredentials}
         * @throws IllegalStateException if validation fails
         */
        public BasicAuthCredentials build() {
            validate();

            return new BasicAuthCredentials(this);
        }

        /**
         * <p>
         * Validates the builder state before object creation.
         * </p>
         */
        private void validate() {
            Objects.requireNonNull(username);
            Objects.requireNonNull(authType);
            Objects.requireNonNull(emailAddress);

            if ((Objects.isNull(plainPassword) && Objects.isNull(encryptedPassword)) ||
                    (Objects.nonNull(plainPassword) && Objects.nonNull(encryptedPassword))) {
                throw ErrorCode.get(EmailErrorCodes.AUTH_CREDENTIALS_BUILD_ACTION_FAILED);
            }
        }
    }
}