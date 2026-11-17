package com.tlc.identity.service.oidc.fields;

/**
 * <p>
 * Contains fields related to OIDC
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public final class OIDCFields {

    private OIDCFields() {
    }

    public static final String OIDC = "oidc";
    public static final String CLIENT_ID = "client.id";
    public static final String CLIENT_SECRET = "client.secret";
    public static final String CLIENT_ID_PARAM = "client_id";
    public static final String CLIENT_SECRET_PARAM = "client_secret";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String REDIRECT_URL = "redirectUrl";
    public static final String JWKS_URL = "jwksUrl";
    public static final String RESPONSE_TYPE = "responseType";
    public static final String AUTHORITY = "authority";
    public static final String CODE = "code";
    public static final String TYPE = "grantType";
    public static final String GRANT_TYPE = "grant_type";
    public static final String SCOPE = "scope";
    public static final String ISSUER = "issuer";
    public static final String TOKEN_URL = "tokenUrl";
    public static final String USER_INFO_URL = "userInfoUrl";
    public static final String GOOGLE = "google";
    public static final String MICROSOFT = "microsoft";
    public static final String ID_TOKEN = "id_token";
    public static final String KEY_ID = "kid";
    public static final String KEYS = "keys";
    public static final String KEY_TYPE = "kty";
    public static final String RSA = "RSA";
    public static final String RS256 = "RS256";
    public static final String RSA_MODULUS = "n";
    public static final String RSA_EXPONENT = "e";
    public static final String ALGORITHM = "alg";
    public static final String AUDIENCE = "aud";
    public static final String AUTHORIZED_PARTY = "azp";
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String PROVIDER = "provider";
    public static final String DOMAIN = "domain";
    public static final String BEARER = "Bearer";
    public static final String AUTHORIZATION = "Authorization";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String COMMON = "/common/";
    public static final String DOT = "\\.";
}
