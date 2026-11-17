package com.tlc.commons.jwt.meta;

/**
 * Represents an entity responsible for issuing JWTs
 *
 * @author sundar
 * @version 1.0
 */
public interface TokenIssuer extends JwtMeta {

    String issuer();
}