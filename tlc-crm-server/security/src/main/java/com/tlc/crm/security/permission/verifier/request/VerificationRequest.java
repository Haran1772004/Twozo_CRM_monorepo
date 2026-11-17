package com.tlc.crm.security.permission.verifier.request;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.security.permission.model.ApiDetails;

import java.lang.annotation.Annotation;

/**
 * <p>
 * Wraps all the details needed for the verifier request.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public record VerificationRequest<T extends Annotation>(long orgId, long userId, long roleId, ApiDetails apiDetails, T authorizer,
                                                        JsonObject requestJson) {

}
