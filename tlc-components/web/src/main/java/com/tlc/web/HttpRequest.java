package com.tlc.web;

import java.net.URL;
import java.util.Map;

/**
 * <p>
 * Used to execute the HTTP requests
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public record HttpRequest(URL requestUri, Map<String, String> requestParams, HttpMethod requestMethod, Map<String, String> headers) {

}
