package com.tlc.crm.common.iam.action.preferences;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.iam.api.preferences.manager.OrgPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.OrgPreference;
import com.tlc.crm.common.iam.api.preferences.model.converter.OrgPreferenceConverter;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.WebAction;
import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides an abstract way to configure the {@link OrgPreferenceAction}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/settings/preference")
public class OrgPreferenceAction extends CrmSecureAction {

    private final OrgPreferenceConverter preferenceConverter;
    private final OrgPreferenceManager preferenceManager;

    public OrgPreferenceAction() {
        preferenceConverter = OrgPreferenceConverter.getInstance();
        preferenceManager = OrgPreferenceManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(PreferenceFields.TYPE.getName());
        final long orgId = crmRequest.orgId();

        switch (type) {
            case "pUpdate" -> {
                final OrgPreferenceHolder preferenceHolder = preferenceConverter.convert(orgId, requestJson.getJsonObject(PreferenceFields.DATA.getName()));

                pUpdate(preferenceHolder, crmResponse);
            }
            case "get" -> {
                crmResponse.put(PreferenceFields.PREFERENCES.getName(), preferenceConverter.convertToJson(preferenceManager.get(orgId)));
            }
        }
    }

    /**
     * <p>
     * Validates the {@link OrgPreference} depends on the validator groups.
     * </p>
     *
     * @param preferenceHolder Represents the {@link OrgPreference} details.
     * @param groups           Validates the given group validator.
     */
    private Collection<ErrorDescriptor> validate(final OrgPreferenceHolder preferenceHolder, final Class<?>... groups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;
        final Set<ConstraintViolation<OrgPreferenceHolder>> violations = new HashSet<>(validator.validate(preferenceHolder, groups));

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Partially updates the given {@link OrgPreference} details.
     * </p>
     *
     * @param preferenceHolder Represents the {@link OrgPreference} details.
     * @param crmResponse      Represents the {@link CrmResponse} details
     */
    private void pUpdate(final OrgPreferenceHolder preferenceHolder, final CrmResponse crmResponse) {
        final Collection<ErrorDescriptor> errors = validate(preferenceHolder, Group.Update.class);

        if (errors.isEmpty()) {
            preferenceManager.update(preferenceHolder);
            crmResponse.put(PreferenceFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.organisation.preference.successfully.updated"));
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.organisation.preference.update.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }
}
