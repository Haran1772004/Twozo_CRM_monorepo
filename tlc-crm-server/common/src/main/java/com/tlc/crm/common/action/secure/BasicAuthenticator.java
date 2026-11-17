package com.tlc.crm.common.action.secure;

/**
 * An abstract base class for authenticators that handle Basic Authentication.
 * Subclasses should implement the logic to extract and validate credentials
 * from the incoming request (typically from the Authorization header).
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.o
 */
public abstract class BasicAuthenticator implements SecureRequestAuthenticator {
}
