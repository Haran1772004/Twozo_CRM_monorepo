package com.tlc.crm.common.mcp;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.service.UserPreferenceHolderService;

import com.tlc.iam.client.TokenResolver;
import com.tlc.iam.client.models.AuthRequest;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.converter.UserConverter;

import com.tlc.mcp.action.McpIdentityExtractor;
import com.tlc.mcp.action.McpIdentityProvider;

import com.tlc.web.HttpCookie;
import com.tlc.web.WebExchange;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventNotifier;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * CRM-specific MCP Identity Extractor. Extracts CRM user identity from HTTP requests containing JWT tokens.
 * Integrates with CRM authentication system to resolve and validate user sessions.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@McpIdentityProvider
public class CrmMcpIdentityExtractor implements McpIdentityExtractor<CrmMcpIdentity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmMcpIdentityExtractor.class);
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final OrganizationService organizationService;
    private final UserService userService;

    public CrmMcpIdentityExtractor() {
        this.organizationService = OrganizationService.getInstance();
        this.userService = UserService.getInstance();
    }

    /**
     * Extract CRM user identity from web exchange.
     * Asynchronously resolves the user from JWT token and invokes callback.
     *
     * @param exchange WebExchange containing HTTP request/response context
     * @param callback Consumer to invoke with resolved identity (CrmUser or null)
     */
    @Override
    public void extract(final WebExchange exchange, final Consumer<CrmMcpIdentity> callback) {
        try {
            final HttpCookie tokenCookie = exchange.cookie(ACCESS_TOKEN_COOKIE);
            if (tokenCookie != null) {
                resolve(tokenCookie, exchange.ipAddress(), callback);
                return;
            }
            final Stream<String> authHeaders = exchange.header(AUTHORIZATION_HEADER);
            final String authHeader = authHeaders == null ? null : authHeaders.findFirst().orElse(null);

            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                final String token = authHeader.substring(BEARER_PREFIX.length()).strip();

                if (!token.isEmpty()) {
                    resolveApiToken(token, callback);
                    return;
                }
            }
            callback.accept(null);

        } catch (Exception e) {
            LOGGER.error("MCP: Error extracting MCP identity ", e);
            callback.accept(null);
        }
    }

    /**
     * Resolves the MCP identity associated with the given API token.
     *
     * @param token    The API token used to identify the user.
     * @param callback The callback invoked with the resolved MCP identity.
     */
    private void resolveApiToken(final String token, final Consumer<CrmMcpIdentity> callback) {
        final Event<byte[]> authenticateApiKeyEvent = new Event<>(IamEvents.API_KEY_AUTHENTICATION, Json.object()
                .put(IamFields.ACCESS_TOKEN, token).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param bytes Response from the consumer.
             */
            @Override
            public void onSuccess(final byte[] bytes) {
                if (Objects.isNull(bytes)) {
                    callback.accept(null);
                    return;
                }
                final JsonObject jsonObject = Json.object(bytes);
                final User user = UserConverter.getInstance().convert(jsonObject.getLong(IamFields.ORG_ID), jsonObject);

                if (null == user) {
                    callback.accept(null);
                    return;
                }
                final Long crmOrgId = organizationService.getOrgId(user.orgId());
                final Optional<CrmUser> optionalCrmUser = userService.getUserByIamId(crmOrgId, user.id());

                if (optionalCrmUser.isEmpty()) {
                    callback.accept(null);
                } else {
                    final CrmUser crmUser = optionalCrmUser.get();

                    crmUser.setPreferenceHolder(UserPreferenceHolderService.get().get(crmUser.orgId(), crmUser.id()));
                    callback.accept(new CrmMcpIdentity(crmUser));
                }
            }

            /**
             * {@inheritDoc}
             *
             * @param throwable Cause of the failure.
             */
            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("MCP: MCP API key token resolution failed: {}", throwable.getMessage(), throwable);
                callback.accept(null);
            }
        };

        EventNotifier.get().send(authenticateApiKeyEvent, eventCallback);
    }

    /**
     * Resolves the MCP identity associated with the given authentication cookie and client IP address.
     *
     * @param cookie    The authentication cookie used to identify the user.
     * @param ipAddress The client's IP address.
     * @param callback  The callback invoked with the resolved MCP identity.
     */
    private void resolve(final HttpCookie cookie, final String ipAddress, final Consumer<CrmMcpIdentity> callback) {
        TokenResolver.get().resolve(new AuthRequest(cookie, ipAddress), new EventCallback<User>() {

            @Override
            public void onSuccess(final User user) {
                if (null == user) {
                    callback.accept(null);
                    return;
                }
                final Long crmOrgId = organizationService.getOrgId(user.orgId());
                final Optional<CrmUser> optionalCrmUser = userService.getUserByIamId(crmOrgId, user.id());

                if (optionalCrmUser.isEmpty()) {
                    callback.accept(null);
                } else {
                    final CrmUser crmUser = optionalCrmUser.get();

                    crmUser.setPreferenceHolder(UserPreferenceHolderService.get().get(crmUser.orgId(), crmUser.id()));
                    callback.accept(new CrmMcpIdentity(crmUser));
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("MCP: MCP token resolution failed: {}", throwable.getMessage());
                callback.accept(null);
            }
        });
    }
}
