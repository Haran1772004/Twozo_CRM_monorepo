package com.tlc.email.common.action.res;

import com.tlc.web.action.res.HttpResponse;

/**
 * Represents an HTTP response that contains email-related data from an external provider.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see HttpResponse
 */
public interface EmailResponse extends HttpResponse {

    /**
     * Sets the body content.
     *
     * @param rawContent Represents the raw content
     */
    void setBody(String rawContent);
}
