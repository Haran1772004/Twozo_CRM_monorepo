package com.tlc.iam.server.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.jwt.keys.AsymmetricKey;
import com.tlc.commons.jwt.keys.SigningKey;
import com.tlc.commons.jwt.keys.SigningKeysRegistry;
import com.tlc.commons.jwt.keys.SymmetricKey;
import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.meta.TokenIssuer;
import com.tlc.commons.jwt.registry.KeyGroupRegistry;
import com.tlc.commons.jwt.registry.TokenIssuerRegistry;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.iam.common.AuthenticationKeys;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.iam.server.internal.status.IamServerErrorCodes;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A service that handles the IAM server related activities
 *
 * @author Selvakumar
 * @version 1.0
 */
public class IamServerService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(IamServerService.class);

    private final SigningKeysRegistry signingKeysRegistry;
    private final ApplicationRegistry applicationRegistry;

    private IamServerService() {
        signingKeysRegistry = SigningKeysRegistry.getInstance();
        applicationRegistry = ApplicationRegistry.getInstance();
    }

    private static final class Instance {
        private static final IamServerService INSTANCE = new IamServerService();
    }

    public static IamServerService getInstance() {
        return IamServerService.Instance.INSTANCE;
    }

    /**
     * Performs the initialization activities of IamServer service.
     * <pre>
     *
     * <b>Symmetric key format </b> -> keyGroupName.symmetricKey
     *
     * Example: crm.symmetricKey=keySecret
     *
     * <b>Asymmetric key format </b> -> keyGroupName.asymmetricKey=publicKeyPath,privateKeyPath
     *
     * Example: crm.asymmetricKey=/home/user/keys/public.pem, /home/user/keys/private.pem
     *
     * <b>Note</b> This keyGroupName should be resolves {@link IAMKeyGroup}
     * </pre>
     *
     * @param configurations Holds the metadata to starts this service
     */
    @Override
    public void start(final Configurations configurations) {
        loadApplicationRegistry(configurations);

        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader newClassLoader = DefaultJwtBuilder.class.getClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(newClassLoader);
            final Optional<String> key = configurations.getFirstKey();
            final Collection<IAMKeyGroup> iamKeyGroups = new HashSet<>();

            if (key.isPresent()) {
                final String configKey = key.get();

                if (configKey.endsWith(".symmetricKey")) {
                    final String groupName = getGroupName(configKey, ".symmetricKey");
                    final Configurations groupConfigurations = configurations.getNestedConfigs(groupName);

                    processSecurityConfig(groupConfigurations, iamKeyGroups);
                    final SigningKey signingKey = new SymmetricKey(groupName.toUpperCase(), groupConfigurations.getConfig("symmetricKey"));

                    signingKeysRegistry.add(signingKey);
                    LOGGER.info("JWT signing key registered for group {}", groupName);
                } else if (configKey.contains(".asymmetricKey")) {
                    final String groupName = getGroupName(configKey, ".asymmetricKey");
                    final Configurations groupConfigurations = configurations.getNestedConfigs(groupName);
                    final String[] keyPaths = groupConfigurations.getConfig("asymmetricKey").split(",", 2);

                    if (keyPaths.length == 2) {
                        final SigningKey signingKey = new AsymmetricKey(groupName, keyPaths[0].trim(), keyPaths[1].trim());

                        processSecurityConfig(groupConfigurations, iamKeyGroups);
                        signingKeysRegistry.add(signingKey);
                        LOGGER.info("JWT signing key registered for group {}", groupName);
                    } else {
                        throw ErrorCode.get(IamServerErrorCodes.INVALID_ASYMMETRIC_KEYS_PATH);
                    }
                }
            }

            loadSigningKeyIfNotExists(iamKeyGroups);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    /**
     * <p>
     * Process the security config
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     * @param iamKeyGroups   Which contains the collection of {@link IAMKeyGroup}
     */
    private void processSecurityConfig(final Configurations configurations, final Collection<IAMKeyGroup> iamKeyGroups) {
        final Configurations groupedConfigs = configurations.getNestedConfigs("security");
        final Collection<String> tokenIssuers = groupedConfigs.getValues("token.issuers");
        final Collection<String> keyGroups = groupedConfigs.getValues("keyGroups");

        if (!tokenIssuers.isEmpty()) {
            final TokenIssuerRegistry tokenIssuerRegistry = TokenIssuerRegistry.getInstance();

            for (final String tokenIssuer : tokenIssuers) {
                final String[] tokenIssuerValues = tokenIssuer.split(":");

                tokenIssuerRegistry.register(new IAMTokenIssuer(tokenIssuerValues[0].trim(), tokenIssuerValues[1].trim()));
            }
        }

        if (!keyGroups.isEmpty()) {
            final KeyGroupRegistry keyGroupRegistry = KeyGroupRegistry.getInstance();

            for (final String keyGroup : keyGroups) {
                final IAMKeyGroup iamKeyGroup = new IAMKeyGroup(keyGroup.trim());

                keyGroupRegistry.register(iamKeyGroup);
                iamKeyGroups.add(iamKeyGroup);
            }
        }
    }

    /**
     * <p>
     * Extracts the group name from the given configuration key by removing the specified suffix
     * </p>
     *
     * @param configKey Represents the configuration key string
     * @param suffix    Represents the suffix to remove from the configuration key
     * @return the group name portion of the configuration
     */
    private String getGroupName(final String configKey, final String suffix) {
        return configKey.substring(0, configKey.indexOf(suffix));
    }

    /**
     * Loads the application into registry
     *
     * <pre>
     *    Required properties
     *       * productName.auth.success.redirect.url
     *              Example : crm.auth.success.redirect.url=https://twozo.in/landingPage
     *       * productName.auth.failure.redirect.url
     *              Example : crm.auth.failure.redirect.url=https://twozo.in/401page.html
     *
     *    Optional properties
     *       * productName.auth.cookie.domain
     *              By default it will be blank. Example: crm.auth.cookie.domain=.twozo.in
     *       * productName.auth.cookie.path
     *              Default value is <b>/</b>
     *       * productName.auth.cookie.isSecured
     *              Default value is true
     *       * productName.auth.cookie.validity
     *              This value has to be in seconds. Default value is 8 hrs
     *
     * </pre>
     *
     * @param configurations Metadata required to start the IAM Client Services
     */
    private void loadApplicationRegistry(final Configurations configurations) {
        final Optional<String> key = configurations.getFirstKey();

        if (key.isPresent()) {
            final String config = key.get();
            final String applicationName = config.substring(0, config.indexOf("."));
            final Configurations crmConfigurations = configurations.getNestedConfigs(applicationName);
            final String successRedirectUrl = crmConfigurations.getConfig(AuthenticationKeys.AUTH_SUCCESS_REDIRECT_URL);
            final String failureRedirectUrl = crmConfigurations.getConfig(AuthenticationKeys.AUTH_FAILURE_REDIRECT_URL);
            final String domain = crmConfigurations.optConfig(AuthenticationKeys.AUTH_COOKIE_DOMAIN, null);
            final String cookiePath = crmConfigurations.optConfig(AuthenticationKeys.AUTH_COOKIE_PATH, null);
            final String sameSite = crmConfigurations.optConfig(AuthenticationKeys.AUTH_COOKIE_SAME_SITE, null);
            final String maxAge = crmConfigurations.optConfig(AuthenticationKeys.AUTH_COOKIE_VALIDITY, null);
            final boolean isSecuredCookieEnabled = Boolean.parseBoolean(crmConfigurations.optConfig(AuthenticationKeys.AUTH_COOKIE_IS_SECURED, "true"));

            final ClientApplication clientApplication = new ClientApplication(applicationName, successRedirectUrl, failureRedirectUrl,
                    isSecuredCookieEnabled, domain,
                    Objects.nonNull(cookiePath) && !cookiePath.isBlank() ? cookiePath : "/",
                    Objects.nonNull(maxAge) && !maxAge.isBlank() ? Integer.parseInt(maxAge) : (int) TimeUnit.HOURS.toSeconds(8), sameSite);

            applicationRegistry.add(clientApplication);
            LOGGER.info("{} application details registered successfully", applicationName);
        }
        LOGGER.info("No of registered applications {}", applicationRegistry.size());
    }

    /**
     * If no {@link SigningKey} is registered for a {@link IAMKeyGroup},
     * a default {@link SigningKey} will be registered
     */
    private void loadSigningKeyIfNotExists(final Collection<IAMKeyGroup> groups) {

        for (final IAMKeyGroup IAMKeyGroup : groups) {
            if (!signingKeysRegistry.contains(IAMKeyGroup)) {
                final KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
                final AsymmetricKey asymmetricKey = new AsymmetricKey(IAMKeyGroup, keyPair);
                final String groupName = IAMKeyGroup.name();

                LOGGER.info("Registering random JWT signing key for group {} as no JWT signing key registered", groupName);
                signingKeysRegistry.add(asymmetricKey);
                LOGGER.info("Random JWT signing key registered for group {}", groupName);
            }
        }

    }

    /**
     * Performs the cleanup activities
     */
    @Override
    public void stop() {
        signingKeysRegistry.clear();
        applicationRegistry.clear();
    }

    private record IAMTokenIssuer(String name, String issuer) implements TokenIssuer {
    }

    public record IAMKeyGroup(String name) implements KeyGroup {
    }
}
