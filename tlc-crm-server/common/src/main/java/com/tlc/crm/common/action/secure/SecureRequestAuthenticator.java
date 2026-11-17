package com.tlc.crm.common.action.secure;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import java.util.function.Consumer;

/**
 * Defines a contract for authenticating incoming CRM requests.
 * Implementations of this interface handle specific authentication strategies
 * such as JWT, Basic Auth, or OAuth.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.o
 */
public interface SecureRequestAuthenticator {

    /**
     * Authenticates the given CRM request.
     *
     * @param request  The incoming CRM request
     * @param response The CRM response, which may be updated based on authentication result
     * @param callback A callback function that accepts {@code true} if authentication is successful, or {@code false} otherwise.
     */
    void authenticate(final CrmRequest request, final CrmResponse response, final Consumer<Boolean> callback);

    /**
     * Determines whether this authenticator is capable of handling the given CRM request.
     *
     * @param request the incoming {@link CrmRequest} to be evaluated
     * @return {@code true} if this authenticator should handle the request, {@code false} otherwise.
     */
    boolean canHandle(final CrmRequest request);

}
