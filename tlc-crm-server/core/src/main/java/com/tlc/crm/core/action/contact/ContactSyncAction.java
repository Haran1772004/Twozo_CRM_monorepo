package com.tlc.crm.core.action.contact;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.util.oauth.RedirectHandler;
import com.tlc.crm.core.api.contact.manager.ContactSyncManager;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.core.field.contact.ContactFields;

import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;

import com.tlc.notification.model.email.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.SyncUrlConfig;
import com.tlc.crm.common.sync.SyncMode;
import com.tlc.notification.model.sync.ContactGroup;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.service.sync.SyncService;
import com.tlc.sync.service.oauth.model.provider.ContactProvider;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Configures the user consent using oauth authorization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@WebAction(path = "/oauth/contact/sync/{provider}", method = HttpMethod.GET)
public class ContactSyncAction extends AuthorizationController {

    private static final String LOCAL_NAVIGATE_URL = "local.navigateUrl";
    private static final String SYNC_REDIRECT_URL_KEY = "redirectUrl.sync";
    private static final String SYNC_ERROR_REDIRECT_URL = "redirectUrl.error";

    private final ContactSyncManager contactSyncManager;
    private final Configurations configurations;
    private final RedirectHandler redirectHandler;

    public ContactSyncAction() {
        contactSyncManager = ContactSyncManager.getInstance();
        configurations = ConfHolder.getInstance().getProperties(ContactFields.CONTACT_SYNC);
        redirectHandler = RedirectHandler.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     the received request
     * @param crmResponse the response to be sent
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final ContactProvider provider = ContactProvider.get(request.getParameter(ContactFields.PROVIDER));
        final SyncUrlConfig syncUrlConfig = SyncService.getSyncUrlDetails(provider);
        final String code = request.getParameter(ContactFields.CODE);

        if (Objects.isNull(code)) {
            redirectHandler.buildRedirectUrlWithState(request, syncUrlConfig.getConsentUrl(), crmResponse);
        } else {
            final long orgId = request.orgId();
            final long userId = request.userId();

            if (contactSyncManager.isAlreadyConnected(orgId, userId)) {
                throw ErrorCode.get(ContactErrorCodes.CONNECTION_ALREADY_ESTABLISHED, "i18n.contact.sync.connection.already.established");
            }
            final EmailTokenWriteRequest contactTokenWriteRequest = SyncTokenRequestInstanceProvider.getContactTokenWriteRequest(code, syncUrlConfig, TokenRequest.TokenType.REFRESH);

            provider.getSyncService().fetchAndStoreRefreshToken(contactTokenWriteRequest);
            final EmailAddress address = new EmailAddress(contactTokenWriteRequest.getEmailAddress().getEmailId());

            if (contactSyncManager.exists(orgId, address)) {
                crmResponse.setTemporaryRedirectUrl(HttpUtil.buildAbsoluteUrl(configurations.getConfig(SYNC_ERROR_REDIRECT_URL)));
                crmResponse.send();
                return;
            }

            if (contactSyncManager.exists(orgId, userId, address)) {
                contactSyncManager.updateConnectionStatus(orgId, userId, address);
            } else {
                final ContactSyncSettings contactSyncSettings = new ContactSyncSettings();

                contactSyncSettings.setAddress(address);
                contactSyncSettings.setOrgId(orgId);
                contactSyncSettings.setUserId(userId);
                contactSyncSettings.setConfig(getDefaultConfig(provider));
                contactSyncSettings.setSyncStatus(false);
                createContactSettings(crmResponse, orgId, List.of(contactSyncSettings));
            }
            redirectHandler.handleClientRedirect(request, crmResponse, HttpUtil.buildAbsoluteUrl(configurations.getConfig(SYNC_REDIRECT_URL_KEY)),
                    configurations.getConfig(LOCAL_NAVIGATE_URL));
        }
    }

    /**
     * <p>
     * Gets the default config
     * </p>
     *
     * @return the {@link JsonObject}
     */
    private String getDefaultConfig(final ContactProvider contactProvider) {
        final JsonObject configJson = Json.object();

        configJson.put(ContactFields.SYNC_WAY, SyncMode.ONE_WAY);
        configJson.put(ContactFields.GROUP_OF_CONTACTS, ContactGroup.getDefaultGroup(contactProvider).getId());

        return configJson.toString();
    }

    /**
     * <p>
     * Creates a contact sync account
     * </p>
     *
     * @param crmResponse  The response to be displayed
     * @param orgId        Represents an organization id
     * @param syncSettings An entity that holds sync contact attributes
     */
    private void createContactSettings(final CrmResponse crmResponse, final long orgId, final Collection<ContactSyncSettings> syncSettings) {
        final Collection<ErrorDescriptor> errors = validate(syncSettings, Group.Create.class);

        if (errors.isEmpty()) {
            final ProcessOutcome processOutcome = contactSyncManager.create(orgId, syncSettings);

            crmResponse.put(ActionFields.CREATED_RECORDS, processOutcome.getProcessedRecords());
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            if (Objects.nonNull(errorSummary)) {
                crmResponse.setErrorCode(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed"));
                crmResponse.setErrorSummary(errorSummary);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.CREATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Validates the models against the given validator groups
     * </p>
     *
     * @param models          Represents the collection of models to be validated
     * @param validatorGroups Represents the validation groups
     */
    private Collection<ErrorDescriptor> validate(final Collection<ContactSyncSettings> models,
                                                                      final Class... validatorGroups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<ContactSyncSettings>> violations = new HashSet<>();
        final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;

        models.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }
}