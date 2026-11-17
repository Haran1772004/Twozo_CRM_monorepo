package com.tlc.commons.dns.util;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Objects;

/**
 * <p>
 * Utility class for validating DNS records using public DNS lookup.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class DnsValidatorUtil {

    private static final String DNS_CONTEXT_FACTORY_KEY = "java.naming.factory.initial";
    private static final String DNS_CONTEXT_FACTORY_VALUE = "com.sun.jndi.dns.DnsContextFactory";
    private static final String CNAME_ATTRIBUTE = "CNAME";
    private static final String DOT = ".";
    private static final String EMPTY_STRING = "";

    /**
     * <p>
     * Private constructor to prevent instantiation.
     * </p>
     */
    private DnsValidatorUtil() {
    }

    /**
     * <p>
     * Validates whether the given CNAME record exists in public DNS
     * and resolves to the expected value.
     * </p>
     *
     * @param host          Represents the DNS host name.
     * @param expectedValue Represents the expected CNAME target value.
     * @return {@code true} if the CNAME record is valid; {@code false} otherwise.
     */
    public static boolean isCnameValid(final String host, final String expectedValue) {

        try {
            final Hashtable<String, String> env = new Hashtable<>();

            env.put(DNS_CONTEXT_FACTORY_KEY, DNS_CONTEXT_FACTORY_VALUE);
            final DirContext ctx = new InitialDirContext(env);
            final Attributes attrs = ctx.getAttributes(host, new String[]{CNAME_ATTRIBUTE});
            final Attribute attribute = attrs.get(CNAME_ATTRIBUTE);

            if (Objects.isNull(attribute)) {
                return false;
            }
            final String actualValue = attribute.get().toString().replace(DOT, EMPTY_STRING);

            return actualValue.equalsIgnoreCase(expectedValue.replace(DOT, EMPTY_STRING));
        } catch (Exception exception) {
            return false;
        }
    }
}