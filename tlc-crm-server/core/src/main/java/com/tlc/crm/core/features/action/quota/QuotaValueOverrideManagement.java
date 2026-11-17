package com.tlc.crm.core.features.action.quota;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.manager.QuotaValueOverrideManager;
import com.tlc.crm.core.features.api.quota.manager.QuotaHolderManagerImpl;
import com.tlc.crm.core.features.api.quota.model.*;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.security.permission.annotation.authorizer.QuotaAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import jakarta.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Controller responsible for managing quota forecast category overrides, including updating overrides, fetching recent logs, adding notes, and retrieving notes and forecast progress.
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
@QuotaAuthorizer(subCategory = SubCategory.Type.OVERRIDE)
@WebAction(path = "/quota/value/override/mgmt")
public class QuotaValueOverrideManagement extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaValueOverrideManagement.class);
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 100;
    private static final String OVERRIDDEN = "overridden";
    private static final String CURRENT = "Current";
    private static final String OVERRIDE = "override";

    private final QuotaValueOverrideManager quotaValueOverrideManager;
    private final UserService iamUserService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final OrganizationService organizationService;
    private final CrmUserConverter crmUserConverter;
    private final QuotaHolderManagerImpl quotaHolderManagerImpl;
    private final ForecastCategoryManager forecastCategoryManager;
    private final BaseCurrencyService baseCurrencyService;

    public QuotaValueOverrideManagement() {
        this.quotaValueOverrideManager = QuotaValueOverrideManager.getInstance();
        iamUserService = UserService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        quotaHolderManagerImpl = QuotaHolderManagerImpl.getInstance();
        forecastCategoryManager = ForecastCategoryManager.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     The CRM request containing the details of the action to be performed
     * @param crmResponse The CRM response to be populated with the result of the action
     * @throws Exception if any error occurs during the processing of the request
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(QuotaFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(QuotaFields.DATA);
        final Long orgId = request.orgId();

        switch (type) {
            case "update" -> update(orgId, data, request.getUser(), crmResponse);
            case "getRecentLogs" -> getRecentLogs(orgId, data, request.getI18NCRMResolutionAdapter(), crmResponse);
            case "addNote" -> addNote(orgId, data, request.getUser(), crmResponse);
            case "getNotes" -> getNotes(orgId, data, crmResponse);
            case "getForecastProgress" ->
                    getForecastCategoryProgress(orgId, data, request.getI18NCRMResolutionAdapter(), crmResponse);
            case "delete" -> {
                quotaValueOverrideManager.delete(orgId, data.getLong(QuotaFields.ID));
                crmResponse.put(QuotaFields.MESSAGE, crmResponse.resolveI18nPlaceholder("i18n.quota.override.delete"));
            }
        }
    }

    /**
     * <p>
     * Updates the quota forecast category override log based on the provided data.
     * </p>
     *
     * @param orgId       The organization ID
     * @param data        The JSON object containing the override log details
     * @param crmUser     The CRM user performing the update
     * @param crmResponse The CRM response to be populated with the result of the operation
     */
    private void update(final long orgId, final JsonObject data, final CrmUser crmUser, final CrmResponse crmResponse) {
        final QuotaValueOverrideLog quotaValueOverrideLog = new QuotaValueOverrideLog();
        final long forecastCategoryId = data.getLong(QuotaFields.FORECAST_CATEGORY_ID);

        quotaValueOverrideLog.setOrgId(orgId);
        quotaValueOverrideLog.setQuotaId(data.getLong(QuotaFields.ID));
        quotaValueOverrideLog.setUserId(data.getLong(QuotaFields.USER_ID));
        quotaValueOverrideLog.setForecastCategoryId(forecastCategoryId);
        quotaValueOverrideLog.setValue(data.getString(QuotaFields.VALUE));
        quotaValueOverrideLog.setOverrideBy(crmUser);
        final Collection<ErrorDescriptor> errorDescriptors = validate(quotaValueOverrideLog);

        if (errorDescriptors.isEmpty()) {
            final Optional<String> forecastCategoryName = forecastCategoryManager.getName(orgId, forecastCategoryId);

            quotaValueOverrideManager.update(quotaValueOverrideLog);
            crmResponse.put(QuotaFields.MESSAGE, StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, crmResponse.resolveI18nPlaceholder("i18n.quota.override.update"), forecastCategoryName.get()));
        } else {
            loadValidationErrors(Action.UPDATE, crmResponse, errorDescriptors);
        }
    }

    /**
     * <p>
     * Adds a note to the quota forecast category override summary based on the provided data.
     * </p>
     *
     * @param orgId       The organization ID
     * @param data        The JSON object containing the note details
     * @param crmUser     The CRM user performing the action
     * @param crmResponse The CRM response to be populated with the result of the operation
     */
    private void addNote(final long orgId, final JsonObject data, final CrmUser crmUser, final CrmResponse crmResponse) {
        final Note note = new Note();

        note.setOrgId(orgId);
        note.setCreatedBy(crmUser);
        note.setUpdatedBy(crmUser);
        note.setContent(data.getString(QuotaFields.CONTENT));
        final QuotaValueOverrideNote quotaForecastCategoryOverrideSummaryNote = new QuotaValueOverrideNote(note);

        quotaForecastCategoryOverrideSummaryNote.setOrgId(orgId);
        quotaForecastCategoryOverrideSummaryNote.setSummaryId(data.getLong(QuotaFields.SUMMARY_ID));
        final Collection<ErrorDescriptor> errorDescriptors = validate(quotaForecastCategoryOverrideSummaryNote);

        if (errorDescriptors.isEmpty()) {
            quotaValueOverrideManager.addNote(quotaForecastCategoryOverrideSummaryNote);
        } else {
            loadValidationErrors(Action.CREATE, crmResponse, errorDescriptors);
        }
    }

    /**
     * <p>
     * Gets the forecast category override progress for a given quota and user, and populates the CRM response with the data.
     * </p>
     *
     * @param orgId                 The organization ID
     * @param data                  The JSON object containing the request details
     * @param i18NResolutionAdapter The I18n resolution adapter for localizing display names
     * @param crmResponse           The CRM response to be populated with the forecast category override progress data
     */
    private void getForecastCategoryProgress(final Long orgId, final JsonObject data, final I18nResolutionAdapter i18NResolutionAdapter, final CrmResponse crmResponse) {
        final long quotaId = data.getLong(QuotaFields.ID);
        final long targetUserId = data.getLong(QuotaFields.USER_ID);
        final long forecastCategoryId = data.getLong(QuotaFields.FORECAST_CATEGORY_ID);
        final List<QuotaValueOverrideProgress> forecastProgress = quotaValueOverrideManager.getProgress(orgId, quotaId, targetUserId);
        final JsonArray jsonArray = Json.array();
        final QuotaHolder quotaHolder = quotaHolderManagerImpl.get(orgId, quotaId);
        final Quota quota = quotaHolder.hasUserQuota() ? quotaHolder.getUserQuota() : quotaHolder.getTeamQuota();
        final boolean isDealValue = quota.getQuotaMetric().isDealValue();
        final String symbol = isDealValue ? baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency().getSymbol() : null;

        forecastProgress.sort((firstProgress, secondProgress) -> {
            final ForecastCategory firstCategory = firstProgress.getForecastCategory();
            final ForecastCategory secondCategory = secondProgress.getForecastCategory();
            final boolean isFirstMatching = Objects.nonNull(firstCategory) && Objects.equals(firstCategory.id(), forecastCategoryId);
            final boolean isSecondMatching = Objects.nonNull(secondCategory) && Objects.equals(secondCategory.id(), forecastCategoryId);

            if (isFirstMatching == isSecondMatching) {
                return NumericalConstant.ZERO;
            }
            return isFirstMatching ? NumericalConstant.NEGATIVE_ONE : NumericalConstant.ONE;
        });

        for (final QuotaValueOverrideProgress progress : forecastProgress) {
            final String progressValue = String.valueOf(progress.getValue());
            final String displayValue = isDealValue
                    ? StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, symbol, progressValue) : progressValue;
            final ForecastCategory forecastCategory = progress.getForecastCategory();
            final JsonObject jsonObject = Json.object();
            final I18nKey displayName = forecastCategory.getDisplayName();
            final String name = (Objects.nonNull(displayName) && !forecastCategory.isBestCase())
                    ? i18NResolutionAdapter.get(displayName) : forecastCategory.getName();
            final Long summaryId = progress.getId();

            jsonObject.put(QuotaFields.FORECAST_CATEGORY_ID, forecastCategory.id());
            jsonObject.put(QuotaFields.ID, summaryId);
            jsonObject.put(QuotaFields.VALUE, displayValue);
            jsonObject.put(QuotaFields.DISPLAY_NAME, StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, CURRENT, name.toLowerCase(), QuotaFields.VALUE));
            jsonArray.put(jsonObject);

            if (forecastCategory.id().equals(forecastCategoryId)) {
                final Long overrideValue = progress.getOverrideValue();

                if (Objects.nonNull(overrideValue) && overrideValue > NumericalConstant.ZERO) {
                    final JsonObject overrideJson = Json.object();
                    final String overrideValueAsString = String.valueOf(overrideValue);
                    final String displayOverrideValue = isDealValue
                            ? StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, symbol, overrideValueAsString) : overrideValueAsString;

                    overrideJson.put(QuotaFields.VALUE, displayOverrideValue);
                    overrideJson.put(QuotaFields.IS_DELETED, true);
                    overrideJson.put(QuotaFields.ID, summaryId);
                    overrideJson.put(QuotaFields.DISPLAY_NAME, StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, name, OVERRIDE));
                    jsonArray.put(overrideJson);
                }
            }
        }
        crmResponse.put(QuotaFields.PROGRESS, jsonArray);
    }

    /**
     * <p>
     * Gets notes associated with a quota forecast category override summary, including user details, and populates the CRM response with the data.
     * </p>
     *
     * @param orgId       The organization ID
     * @param data        The JSON object containing the request details, including summary ID and pagination parameters
     * @param crmResponse The CRM response to be populated with the notes data
     */
    private void getNotes(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final long summaryId = data.getLong(QuotaFields.SUMMARY_ID);
        final int start = Math.max(data.optInt(QuotaFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(QuotaFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalRecords = quotaValueOverrideManager.getNoteCount(orgId, summaryId);

        if (totalRecords <= 0 || start > totalRecords) {
            crmResponse.put(QuotaFields.NOTES, Json.array());
            crmResponse.put(QuotaFields.TOTAL_RECORDS, totalRecords);
            crmResponse.put(QuotaFields.HAS_MORE, false);
            return;
        }
        final LimitConfig limitConfig = new LimitConfig(start, limit);
        final Collection<QuotaValueOverrideNote> quotaForecastCategoryOverrideSummaryNotes = quotaValueOverrideManager.getNotes(orgId, summaryId, limitConfig);
        final Collection<Long> userIds = new HashSet<>();

        for (final QuotaValueOverrideNote quotaForecastSummaryNote : quotaForecastCategoryOverrideSummaryNotes) {
            final Note note = quotaForecastSummaryNote.getNote();

            if (Objects.nonNull(note) && Objects.nonNull(note.getCreatedBy())) {
                userIds.add(note.getCreatedBy().id());
            }
        }
        final Map<Long, CrmUser> crmUserMap = crmUserService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));
        final Collection<Long> iamUserIds = crmUserMap.values().stream().map(CrmUser::getIamId).collect(Collectors.toSet());

        crmResponse.convertIntoAsync();
        iamUserService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                try {
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final JsonArray noteArray = Json.array();

                    for (final QuotaValueOverrideNote quotaForecastSummaryNote : quotaForecastCategoryOverrideSummaryNotes) {
                        final Note note = quotaForecastSummaryNote.getNote();

                        if (Objects.nonNull(note)) {
                            final JsonObject noteJson = Json.object();
                            final CrmUser crmUser = crmUserMap.get(note.getCreatedBy().id());

                            if (Objects.nonNull(crmUser)) {
                                final User user = userMap.get(crmUser.getIamId());

                                if (Objects.nonNull(user)) {
                                    crmUser.setUser(user);
                                }
                                noteJson.put(QuotaFields.USER, crmUserConverter.convert(crmUser));
                            }
                            noteJson.put(QuotaFields.CONTENT, note.getContent());
                            noteJson.put(QuotaFields.CREATED_TIME, note.getCreatedTime().getUtcTimeStamp());
                            noteArray.put(noteJson);
                        }
                    }
                    crmResponse.put(QuotaFields.TOTAL_RECORDS, totalRecords);
                    crmResponse.put(QuotaFields.HAS_MORE, (start + limit) <= totalRecords);
                    crmResponse.put(QuotaFields.NOTES, noteArray);
                } catch (final Exception exception) {
                    LoggerUtil.logError(LOGGER, exception, "Quota forecast category override: Failed to users retrieved action. Reason: {}, Org Id: {}", exception.getMessage(), orgId);
                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.FAILED_TO_RETRIEVE_DATA));
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
            }
        });
    }

    /**
     * <p>
     * Gets recent override logs for a given quota forecast category override summary, including user details, and populates the CRM response with the data.
     * </p>
     *
     * @param orgId                 The organization ID
     * @param data                  The JSON object containing the request details, including summary ID and pagination parameters
     * @param i18NResolutionAdapter The I18n resolution adapter for localizing display names
     * @param crmResponse           The CRM response to be populated with the recent logs data
     */
    private void getRecentLogs(final Long orgId, final JsonObject data, final I18nResolutionAdapter i18NResolutionAdapter, final CrmResponse crmResponse) {
        final long summaryId = data.getLong(QuotaFields.SUMMARY_ID);
        final int start = Math.max(data.optInt(QuotaFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(QuotaFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalRecords = quotaValueOverrideManager.getLogCount(orgId, summaryId);
        final JsonArray jsonArray = Json.array();

        if (totalRecords > 0 && start <= totalRecords) {
            final long forecastCategoryId = data.getLong(QuotaFields.FORECAST_CATEGORY_ID);
            final Collection<QuotaValueOverrideLog> overrideLogs = quotaValueOverrideManager.getLogs(orgId, summaryId, new LimitConfig(start, limit));
            final ForecastCategory forecastCategory = forecastCategoryManager.get(orgId, forecastCategoryId);
            final I18nKey displayName = forecastCategory.getDisplayName();
            final String name = (Objects.nonNull(displayName) && !forecastCategory.isBestCase())
                    ? i18NResolutionAdapter.get(displayName) : forecastCategory.getName();

            for (final QuotaValueOverrideLog overrideLog : overrideLogs) {
                final JsonObject overrideLogJson = Json.object();
                final JsonObject userJson = Json.object();
                final CrmUser overrideBy = overrideLog.getOverrideBy();

                userJson.put(QuotaFields.ID, overrideBy.id());
                userJson.put(QuotaFields.NAME, overrideBy.getFullName());

                overrideLogJson.put(QuotaFields.USER, userJson);
                overrideLogJson.put(QuotaFields.ADDED_AT, overrideLog.getAddedAt());
                overrideLogJson.put(QuotaFields.DISPLAY_NAME, StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, name, OVERRIDDEN, overrideLog.getValue()));

                jsonArray.put(overrideLogJson);
            }
        }
        crmResponse.put(QuotaFields.LOGS, jsonArray);
        crmResponse.put(QuotaFields.TOTAL_RECORDS, totalRecords);
        crmResponse.put(QuotaFields.HAS_MORE, (start + limit) <= totalRecords);
    }

    /**
     * <p>
     * Validates the given TlcModel and returns a collection of error descriptors if there are any validation errors.
     * </p>
     *
     * @param tlcModel The TlcModel to validate
     * @return A collection of ErrorDescriptor representing the validation errors, or an empty collection if there are no errors
     */
    private Collection<ErrorDescriptor> validate(final TlcModel tlcModel) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<TlcModel>> violations = validator.validate(tlcModel, Group.Create.class);

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED,
                violation.getMessage()), null, violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Loads the response with the occurred validation errors
     * </p>
     *
     * @param action      The {@link Action} type
     * @param crmResponse The {@link CrmResponse}
     * @param errors      The collection of {@link ErrorDescriptor}
     */
    protected void loadValidationErrors(final Action action, final CrmResponse crmResponse, final Collection<ErrorDescriptor> errors) {
        final ErrorCode errorCode = getActionErrorCode(action);

        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, getActionValidationErrorKey(action)), errors));
    }

    /**
     * <p>
     * Gets the action validation error key
     * </p>
     *
     * @param action The {@link Action} type
     * @return The action validation error key
     */
    private String getActionValidationErrorKey(final Action action) {
        return switch (action) {
            case CREATE -> ErrorKeys.CREATE_ACTION_DATA_VALIDATION_FAILED;
            case UPDATE -> ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED;
            case DELETE -> ErrorKeys.DELETE_ACTION_DATA_VALIDATION_FAILED;
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the action error code
     * </p>
     *
     * @param action The {@link Action} type
     * @return The action error code
     */
    private ErrorCode getActionErrorCode(final Action action) {
        return switch (action) {
            case CREATE -> ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");
            case UPDATE -> ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");
            case DELETE -> ErrorCode.get(CommonErrorCodes.DELETE_ACTION_FAILED, "i18n.common.delete.failed");
            default -> null;
        };
    }
}