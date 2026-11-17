package com.tlc.validator.test;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the valid and invalid urls
 *
 * @author Maharaja S
 * @version 1.0
 */
public class URLValidationTest {

    private static final String URL_PATTERN = "^(http|https|ftp):\\/\\/(([^:@]+(:[^:@]*)?@)?[^:\\s]+\\.[A-Za-z]{2,})(:\\d+)?(\\/[^?#]*)?(\\?[^#]*)?(#.*)?$";

    /**
     * <p>
     * Validates the URL based on the regex pattern
     * </p>
     *
     * @param url Represents the url string to validate
     * @return true if the url is valid, false otherwise
     */
    public boolean isValidURL(final String url) {
        return url.matches(URL_PATTERN);
    }

    /**
     * <p>
     * Tests the valid URLs
     * </p>
     */
    @Test
    public void testValidMobileNumbers() {
        assertTrue(isValidURL("http://example.com"));
        assertTrue(isValidURL("https://www.example.com"));
        assertTrue(isValidURL("ftp://user:password@example.com:8080/path/to/resource"));
        assertTrue(isValidURL("http://subdomain.example.co.uk:8080"));
        assertTrue(isValidURL("https://www.example.com/path/to/resource"));
        assertTrue(isValidURL("http://www.example.com:8080/path/to/resource?query=value"));
        assertTrue(isValidURL("http://www.example.com#fragment"));
        assertTrue(isValidURL("http://www.example.com?query=value#fragment"));
        assertTrue(isValidURL("https://www.google.co.in"));
        assertTrue(isValidURL("https://www.google.co.in/path/to/resource"));
        assertTrue(isValidURL("https://example.io"));
        assertTrue(isValidURL("https://www.facebook.com/TwozoCRM"));
        assertTrue(isValidURL("https://www.facebook.com/9764343523"));
    }

    /**
     * <p>
     * Tests the invalid URLs
     * </p>
     */
    @Test
    public void testInvalidMobileNumbers() {
        assertFalse(isValidURL("www.google.com"));
        assertFalse(isValidURL("http:www.google.com"));
        assertFalse(isValidURL("http//www.google.com"));
        assertFalse(isValidURL("http/www.google.com"));
        assertFalse(isValidURL("http/www.google.com.."));
        assertFalse(isValidURL("http/www.google.com.hh.hh"));
        assertFalse(isValidURL("http:/www.google.com"));
        assertFalse(isValidURL("https://www.google.co m"));
        assertFalse(isValidURL("https://www.google.c"));
        assertFalse(isValidURL("https://www."));
        assertFalse(isValidURL("http://www"));
        assertFalse(isValidURL("https://facebook"));
        assertFalse(isValidURL("example.com"));
        assertFalse(isValidURL("http://localhost"));
        assertFalse(isValidURL("https://.com"));
        assertFalse(isValidURL("http://user@:8080"));
        assertFalse(isValidURL("http://exa mple.co"));
        assertFalse(isValidURL("htp://www.example.co"));
    }
}