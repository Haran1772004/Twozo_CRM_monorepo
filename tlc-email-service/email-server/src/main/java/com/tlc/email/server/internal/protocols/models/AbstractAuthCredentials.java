package com.tlc.email.server.internal.protocols.models;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.server.internal.protocols.AuthCredentials;

/**
 * <p>
 * Provides a base implementation for authentication credential models used by different email protocols.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractAuthCredentials implements AuthCredentials {

    protected EmailAddress emailAddress;
    protected AuthType authType;
}