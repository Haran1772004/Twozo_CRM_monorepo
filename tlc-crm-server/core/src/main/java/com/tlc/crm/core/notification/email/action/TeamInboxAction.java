package com.tlc.crm.core.notification.email.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.fields.QueryParamAttributes;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.api.model.email.CrmEmailDomain;
import com.tlc.crm.core.notification.email.api.service.CrmEmailDomainService;
import com.tlc.crm.core.notification.email.inbox.api.model.EmailVerificationLink;
import com.tlc.crm.core.notification.email.inbox.api.model.converter.TeamInboxConverter;
import com.tlc.crm.core.notification.email.inbox.fields.TeamInboxFields;
import com.tlc.crm.core.notification.email.api.service.TeamInboxService;
import com.tlc.crm.core.notification.email.api.service.impl.TeamInboxServiceImpl;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox.VisibilityStatus;
import com.tlc.crm.core.notification.email.inbox.status.TeamInboxErrorCodes;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Manages {@link TeamInbox} related web actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
@WebAction(path = "/email/teamInbox")
public class TeamInboxAction extends AuthorizationController {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final TeamInboxManager teamInboxManager;
    private final TeamInboxConverter teamInboxConverter;
    private final ModelValidator modelValidator;
    private final TeamInboxService teamInboxService;
    private final Configurations configurations;
    private final RandomIdGenerator randomIdGenerator;
    private final CrmEmailDomainService crmEmailDomainService;

    public TeamInboxAction() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        teamInboxManager = TeamInboxManager.getInstance();
        teamInboxConverter = TeamInboxConverter.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        teamInboxService = TeamInboxServiceImpl.getInstance();
        configurations = ConfHolder.getInstance().getProperties(TeamInboxFields.EMAIL.getName())
                .getNestedConfigs(TeamInboxFields.ADD_ON_SERVICES.getName())
                .getNestedConfigs(TeamInboxFields.TEAM_INBOX.getName());
        this.randomIdGenerator = RandomIdGenerator.getInstance();
        this.crmEmailDomainService = CrmEmailDomainService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the received request
     * @param crmResponse Represents the response to be sent
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final JsonObject data = requestJson.getJsonObject(EmailFields.DATA.getName());
        final String type = requestJson.getString(EmailFields.TYPE.getName());

        switch (type) {
            case "create" -> create(request, crmResponse, data);
            case "delete" -> delete(crmResponse, orgId, data.getLong(TeamInboxFields.ID.getName()));
            case "update" -> updateTeamInbox(request, data, crmResponse);
            case "list" -> teamInboxList(orgId, data, crmResponse);
            case "sendVerificationMail" -> crmResponse.put(TeamInboxFields.IS_VERIFICATION_MAIL_SENT.getName(),
                    verifyAndSendEmail(orgId, userId, data.getString(TeamInboxFields.EMAIL_ADDRESS.getName()), request));
            case "teamInboxEmails" ->
                    crmResponse.put(TeamInboxFields.TEAM_INBOXES.getName(), this.getTeamInboxMails(orgId, userId, data));
            case "getAutoForwardingAddress" ->
                    crmResponse.put(TeamInboxFields.AUTO_FORWARDING_ADDRESS.getName(), teamInboxService.getAutoForwardingAddress(orgId));
        }
    }

    /**
     * <p>
     * Retrieves all team inbox emails for the given organization and user and converts them into a {@link JsonArray}.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param data   Represents the data
     * @return a {@link JsonArray} containing the converted team inbox emails
     */
    private JsonArray getTeamInboxMails(final Long orgId, final Long userId, final JsonObject data) {
        final int start = Math.max(data.optInt(EmailFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(EmailFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);

        return teamInboxConverter.convert(teamInboxService.getUserTeamInboxList(orgId, userId, start, limit));
    }

    /**
     * <p>
     * Verifies whether the given email address exists in the team inbox for the specified organization
     * and sends a verification email if it exists.
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param emailAddress Represents the email address to verify and send a verification email
     * @return {@code true} if the email exists and the verification email was sent,
     * {@code false} otherwise
     */
    private boolean verifyAndSendEmail(final Long orgId, final Long userId, final String emailAddress, final CrmRequest crmRequest) {
        final EmailAddress address = new EmailAddress(emailAddress);

        if (teamInboxManager.exists(orgId, address, TeamInbox.VerificationState.NOT_VERIFIED)) {
            final EmailVerificationLink emailVerificationLink = new EmailVerificationLink();

            emailVerificationLink.setOrgId(orgId);
            emailVerificationLink.setUserId(userId);
            emailVerificationLink.setEmailAddress(new UserSyncAddressInfo(emailAddress));
            emailVerificationLink.setCategory(EmailVerificationLink.Category.TEAM_INBOX);
            emailVerificationLink.setExpireTime(TemporalServiceProvider.get().plus(NumericalConstant.FORTY_EIGHT, DateInterval.HOURS));

            loadVerificationLink(crmRequest, emailVerificationLink);
            teamInboxService.sendVerificationEmail(emailVerificationLink);

            return true;
        }

        return false;
    }

    /**
     * <p>
     * Builds a verification link for the provided email address. The email is URL-safe base64 encoded.
     * </p>
     *
     * @param emailVerificationLink Represent the email verification link
     */
    private void loadVerificationLink(final CrmRequest crmRequest, final EmailVerificationLink emailVerificationLink) {
        final String trackingId = randomIdGenerator.generateUniqueId();
        final String encodedTrackingId = Base64.getUrlEncoder().withoutPadding().encodeToString(trackingId.getBytes());
        final boolean paramExists = Boolean.parseBoolean(crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED));
        final String redirectUrl = HttpUtil.buildAbsoluteUrl(configurations.getConfig(paramExists ? TeamInboxFields.LOCAL_VALIDATE_LINK_URL.getName()
                : TeamInboxFields.SERVER_VALIDATE_LINK_URL.getName()));
        final String queryParam = String.format("?token=%s&localRedirectionRequired=%s", encodedTrackingId, paramExists);

        emailVerificationLink.setTrackingId(trackingId);
        emailVerificationLink.setUrl(String.format("%s%s", redirectUrl, queryParam));
    }

    /**
     * <p>
     * Retrieves the available team inbox emails for a particular user provided in
     * the specified organization by applying pagination values
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param data     Represents the {@link JsonObject}
     * @param response The response to be sent
     */
    private void teamInboxList(final long orgId, final JsonObject data, final CrmResponse response) {
        final int start = Math.max(data.optInt(EmailFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(EmailFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalRecords = teamInboxManager.getTeamInboxCount(orgId);
        final Collection<TeamInbox> teamInboxes = start <= totalRecords ? teamInboxManager.getTeamInboxList(orgId, start, limit) : Collections.emptyList();

        final JsonArray jsonArray = Json.array();

        if (!teamInboxes.isEmpty()) {
            teamInboxes.forEach(teamInbox -> jsonArray.put(teamInboxConverter.convert(teamInbox)));
        }
        response.put(TeamInboxFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(TeamInboxFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
        response.put(TeamInboxFields.TEAM_INBOXES.getName(), jsonArray);
    }

    /**
     * <p>
     * Updates the team inbox with specific list of users to whom access is provided
     * </p>
     *
     * @param request  Represents the request data
     * @param data     Represents the JSON object
     * @param response The response to be displayed
     */
    private void updateTeamInbox(final CrmRequest request, final JsonObject data, final CrmResponse response) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, data);
        final TeamInbox teamInbox = teamInboxConverter.convert(converterConfig, data);
        final Long teamInboxId = teamInbox.id();
        final Long orgId = converterConfig.orgId();
        final String visibleToKey = TeamInboxFields.VISIBLE_TO.getName();

        if (data.containsKey(visibleToKey)) {
            final VisibilityStatus visibilityStatus = VisibilityStatus.getVisibilityStatus(data.getString(visibleToKey));

            if (VisibilityStatus.DESIGNATED == visibilityStatus) {

                if (data.containsKey(TeamInboxFields.ACCESS_TO.getName())) {
                    teamInboxConverter.setVisibilityDetails(teamInbox, data);
                } else {
                    throw ErrorCode.get(TeamInboxErrorCodes.ACCESS_DETAILS_REQUIRED,
                            "i18n.notifications.team.inbox.access.details.required");
                }
            }
            teamInbox.setVisibilityStatus(visibilityStatus);
        } else {
            throw ErrorCode.get(TeamInboxErrorCodes.VISIBILITY_STATUS_REQUIRED,
                    "i18n.notifications.team.inbox.visibility.status.required");
        }
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(teamInbox), Group.Update.class);

        if (errors.isEmpty()) {

            if (Objects.nonNull(teamInboxId) && teamInboxManager.exists(orgId, teamInboxId)) {
                loadDomain(orgId, teamInbox);
                response.put(TeamInboxFields.RESPONSE.getName(), teamInboxManager.update(orgId, List.of(teamInbox)));
            } else {
                throw ErrorCode.get(TeamInboxErrorCodes.TEAM_INBOX_NOT_FOUND, "i18n.notifications.team.inbox.not.found");
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.notifications.team.inbox.update.failed");

            response.setErrorCode(errorCode);
            response.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Deletes the team inbox email
     * </p>
     *
     * @param orgId     Represents the orgId.
     * @param teamInbox Represents the teamInbox.
     */
    private void loadDomain(final Long orgId, final TeamInbox teamInbox) {
        final CrmEmailDomain crmEmailDomain = new CrmEmailDomain();

        crmEmailDomain.setId(teamInboxManager.getDomainId(orgId, teamInbox.id()));

        teamInbox.setDomain(crmEmailDomain);
    }

    /**
     * <p>
     * Deletes the team inbox email
     * </p>
     *
     * @param crmResponse The response to be displayed
     * @param orgId       Represents an organization id
     * @param id          Contains the id to be deleted
     */
    private void delete(final CrmResponse crmResponse, final Long orgId, final Long id) {
        final TeamInbox teamInbox = new TeamInbox();

        teamInbox.setId(id);
        teamInbox.setOrgId(orgId);

        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(teamInbox), Group.Delete.class);

        if (errors.isEmpty()) {

            if (teamInboxManager.exists(orgId, id)) {
                teamInboxManager.delete(orgId, id);
            } else {
                throw ErrorCode.get(TeamInboxErrorCodes.TEAM_INBOX_NOT_FOUND, "i18n.notifications.team.inbox.team.not.found");
            }
        }

        if (!errors.isEmpty()) {
            final ErrorCode errorCode = ErrorCode.get(TeamInboxErrorCodes.DELETE_ACTION_FAILED, "i18n.common.delete.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DELETE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Creates the team inbox email address
     * </p>
     *
     * @param request     Represents the request data
     * @param crmResponse Represents the response to be displayed
     * @param data        Represents the JSON object
     */
    private void create(final CrmRequest request, final CrmResponse crmResponse, final JsonObject data) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, data);
        final TeamInbox teamInbox = teamInboxConverter.convert(converterConfig, data);
        final Long orgId = teamInbox.orgId();
        final Long userId = teamInbox.getOwnerId();
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(teamInbox), Group.Create.class);

        if (errors.isEmpty()) {
            final EmailAddress teamInboxAddress = teamInbox.getAddress();

            if (teamInboxManager.exists(orgId, teamInboxAddress, TeamInbox.State.ACTIVE)) {
                throw ErrorCode.get(TeamInboxErrorCodes.EMAIL_ALREADY_IN_USE, "i18n.notifications.team.inbox.email.already.exists");
            }

            if (teamInboxManager.isInActive(orgId, teamInboxAddress)) {
                teamInboxManager.updateState(orgId, teamInboxAddress);
            } else if (teamInboxManager.getTeamInboxCount(orgId) < 20) {
                teamInbox.setDomain(crmEmailDomainService.getOrInitDomainByEmail(orgId, userId, teamInbox.getAddress().getEmailAddress()));
                final ProcessOutcome processOutcome = teamInboxManager.create(orgId, List.of(teamInbox));

                crmResponse.put(TeamInboxFields.CREATED_RECORDS.getName(), processOutcome.getProcessedRecords());
                final ErrorSummary errorSummary = processOutcome.getErrorSummary();

                if (Objects.nonNull(errorSummary)) {
                    crmResponse.setErrorSummary(errorSummary);
                    crmResponse.setErrorCode(ErrorCode.get(TeamInboxErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed"));
                }
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(TeamInboxErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }
}