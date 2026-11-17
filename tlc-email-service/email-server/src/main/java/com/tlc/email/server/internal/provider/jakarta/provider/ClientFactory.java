package com.tlc.email.server.internal.provider.jakarta.provider;

/**
 * <p>
 * Represents a factory capable of creating email clients
 * using the provided configuration.
 * </p>
 *
 * @param <T> The type of the configuration used to create the client.
 * @param <R> The type of the client returned after creation.
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface ClientFactory<T, R> {

    /**
     * <p>
     * Creates an email client using the specified configuration.
     * Note: This method does not establish an actual connection to the server.
     * </p>
     *
     * @param configuration The configuration containing server details, credentials,
     *                      and security settings required to create the client.
     * @return A configured client of type {@code R}.
     */
    R createClient(final T configuration);
}