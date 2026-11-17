package com.tlc.iam.client.models;

import com.tlc.iam.common.models.User;
import com.tlc.web.UserAgent;

/**
 * Holds the data for setting the password
 *
 * @author Maharaja S
 * @version 1.0
 */
public record PasswordSetRequest(User user, String applicationName, UserAgent userAgent) {
}
