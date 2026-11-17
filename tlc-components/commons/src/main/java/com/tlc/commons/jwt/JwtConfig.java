package com.tlc.commons.jwt;

import com.tlc.commons.jwt.meta.TokenIssuer;

import java.time.temporal.ChronoUnit;

/**
 * Holds for JWT settings
 *
 * @param subject  The subject or principal the token is issued for
 * @param issuer   The entity issuing the token, represented by {@link TokenIssuer}
 * @param validity The duration for which the token remains valid
 * @param timeUnit The unit of time used for the validity period
 * @author sundar
 * @version 1.0
 */
public record JwtConfig(String subject, TokenIssuer issuer, long validity, ChronoUnit timeUnit) {
}
