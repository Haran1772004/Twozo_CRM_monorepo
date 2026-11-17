package com.tlc.crm.entity.role.permission.verifier.internal;

import com.tlc.commons.annotation.InstanceProvider;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.role.permission.validator.SourceFieldPermissionValidator;
import com.tlc.crm.entity.role.permission.verifier.AbstractPermissionsVerifier;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.security.permission.annotation.authorizer.FieldPermissionAuthorizer;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.validator.FieldPermissionValidator;
import com.tlc.crm.security.permission.verifier.request.VerificationRequest;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Optional;

/**
 * Specialized verifier implementation responsible for enforcing field-level security constraints.
 * <p>
 * This class is triggered whenever an API endpoint is annotated with {@link FieldPermissionAuthorizer}.
 * It acts as the orchestrator that:
 * <ul>
 * <li>Resolves the target {@link Source} from the annotation.</li>
 * <li>Initializes the specific {@link SourceFieldPermissionValidator}.</li>
 * <li>Delegates the validation of request data.</li>
 * <li>Registers the validator for subsequent response sanitization.</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Verifier(authorizerClass = FieldPermissionAuthorizer.class)
public class FieldPermissionsVerifier extends AbstractPermissionsVerifier<FieldPermissionAuthorizer> {

    private final FieldSourceService sourceService;

    private FieldPermissionsVerifier() {
        sourceService = FieldSourceService.getInstance();
    }

    private static final class Instance {
        private static final FieldPermissionsVerifier INSTANCE = new FieldPermissionsVerifier();
    }

    /**
     * Retrieves the singleton instance of the verifier.
     *
     * @return the singleton instance.
     */
    @InstanceProvider
    public static FieldPermissionsVerifier getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Orchestrates the field permission verification process for a specific request.
     * <p>
     * This method performs the following steps:
     * <ol>
     * <li>Extracts the target {@link Source} defined in the {@link FieldPermissionAuthorizer} annotation.</li>
     * <li>Instantiates a {@link SourceFieldPermissionValidator} populated with the current organization, role, and API context.</li>
     * <li>Delegates the validation of the incoming JSON request payload to the validator via {@link SourceFieldPermissionValidator#validateRequestData}.</li>
     * <li>If the request is valid, it registers the validator within the {@link VerificationResponse}.
     * This ensures the validator is available later to sanitize the outgoing response data.</li>
     * </ol>
     * </p>
     *
     * @param request  contains the verification context including the authorizer annotation, user role, and request payload.
     * @param response the object used to track verification status and store validators for post-processing.
     */
    @Override
    public void verify(final VerificationRequest<FieldPermissionAuthorizer> request, final VerificationResponse response) {
        final FieldPermissionAuthorizer authorizer = request.authorizer();
        final String sourceName = authorizer.source();
        final ApiDetails apiDetails = request.apiDetails();
        final Optional<Source> optionalSource = sourceName.isEmpty()
                ? fetchSourceName(request.requestJson()).map(Source::get)
                : Optional.of(Source.get(sourceName));

        if (optionalSource.isPresent()) {
            final FieldPermissionValidator fieldPermissionValidator
                    = new SourceFieldPermissionValidator(request.orgId(), request.roleId(), apiDetails, optionalSource.get());

            if (fieldPermissionValidator.validateRequestData(request.requestJson(), response)) {
                response.setFieldPermissionValidator(fieldPermissionValidator);
            }
        }
    }

    /**
     * Fetches the source name from the nested 'data' object in the provided JSON.
     *
     * @param jsonObject the input JSON.
     * @return an {@link Optional} containing the source name, or empty if missing.
     */
    private Optional<String> fetchSourceName(final JsonObject jsonObject) {
        final String dataKey = FieldKeys.DATA.getName();

        if (jsonObject.containsKey(dataKey)) {
            final JsonObject data = jsonObject.getJsonObject(dataKey);
            final String sourceKey = FieldKeys.SOURCE.getName();
            final String sourceIdKey = FieldKeys.SOURCE_ID.getName();

            if (data.containsKey(sourceKey)) {
                return Optional.of(data.getString(sourceKey));
            } else if (data.containsKey(sourceIdKey)) {
                final long sourceId = data.getLong(sourceIdKey);
                final Optional<FieldSource> optionalSource = Optional.ofNullable(sourceService.getSource(sourceId));

                return optionalSource.map(FieldSource::getName);
            }
        }
        return Optional.empty();
    }
}