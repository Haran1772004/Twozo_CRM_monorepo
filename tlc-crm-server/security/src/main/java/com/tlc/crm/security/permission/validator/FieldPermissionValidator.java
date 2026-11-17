package com.tlc.crm.security.permission.validator;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

/**
 * Defines the contract for enforcing field-level security permissions on API payloads.
 * <p>
 * Implementations of this interface are responsible for intercepting both incoming requests
 * and outgoing responses to ensure compliance with the configured field permissions (e.g., Read-Only, Hidden).
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface FieldPermissionValidator extends PermissionValidator {

    /**
     * Validates the incoming request JSON to ensure no unauthorized field modifications are being attempted.
     * <p>
     * This method checks if the request payload contains values for fields that are restricted
     * (e.g., trying to write to a Read-Only field or a Hidden field). If violations are found,
     * specific errors should be added to the provided {@link VerificationResponse}.
     * </p>
     *
     * @param requestJson the JSON body of the incoming API request to validate.
     * @param response the response object used to collect privilege errors or validation failures.
     * @return {@code true} if the request is valid and compliant with permissions; {@code false} otherwise.
     */
    boolean validateRequestData(final JsonObject requestJson, final VerificationResponse response);

    /**
     * Validates and modifies the outgoing response JSON to enforce visibility rules.
     * <p>
     * This method is responsible for sanitizing the response payload by:
     * <ul>
     * <li>Removing fields that the user is not permitted to see (Hidden).</li>
     * <li>Marking or flagging fields that the user cannot edit (Read-Only).</li>
     * </ul>
     * Note: This method mutates the {@code responseJson} in place.
     * </p>
     *
     * @param responseJson the JSON body of the outgoing API response to be sanitized.
     * @return {@code true} if the processing completed successfully; {@code false} otherwise.
     */
    boolean validateAndAlterResponseData(final JsonObject responseJson);
}
