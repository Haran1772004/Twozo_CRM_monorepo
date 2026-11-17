package com.tlc.crm.common.iam.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureEventAction;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.internal.manager.IamConfigManger;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.notification.app.Event;
import com.tlc.iam.client.UserService;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import jakarta.validation.ConstraintViolation;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;

/**
 * <p>
 * Provides an abstract way to configure the {@link IamConfigAction}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public abstract class IamConfigAction<T extends TlcModel> extends SecureEventAction {

    protected final static int DEFAULT_START_VALUE = 1;
    protected final static int DEFAULT_LIMIT = 20;

    protected final UserService userService;
    protected final OrganizationService organizationService;
    protected final CrmUserManager crmUserManager;
    protected final CrmUserConverter crmUserConverter;

    protected IamConfigAction() {
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void secureProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(IamFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(IamFields.DATA);
        final Long orgId = crmRequest.orgId();
        final CrmEventRequest eventRequest = (CrmEventRequest) crmRequest;
        final EventData eventData = eventRequest.getEventData();

        switch (type) {
            case "create" -> {
                final T model = convert(crmRequest, data, true);

                create(crmResponse, orgId, List.of(model), eventData);
            }
            case "delete" -> delete(crmResponse, get(orgId, data.getLong(IamFields.ID)));
            case "pUpdate" -> partialUpdate(crmResponse, convertPartialData(crmRequest, data));
            case "getUsers" -> fetchAndDispatchUserList(crmRequest, crmResponse);
            case "list" -> fetchAndDispatchList(crmRequest, crmResponse);
            case "getPartialUsers" -> fetchAndDispatchPartialUserList(orgId, data, crmResponse);
            case "update" -> {
                final T model = convert(crmRequest, data, false);

                update(crmResponse, orgId, List.of(model), eventData);
            }
            default -> handle(crmRequest, crmResponse, eventData);
        }
    }

    /**
     * <p>
     * Fetches and dispatches a list of entities given by org id
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected abstract void fetchAndDispatchList(final CrmRequest crmRequest, final CrmResponse crmResponse);

    /**
     * <p>
     * Fetches and dispatches a user list given by entity id
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected abstract void fetchAndDispatchUserList(final CrmRequest crmRequest, final CrmResponse crmResponse);

    /**
     * <p>
     * Fetches and dispatches a partial user list given by entity id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Holds the entity details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected abstract void fetchAndDispatchPartialUserList(final Long orgId, final JsonObject jsonObject, final CrmResponse crmResponse);

    /**
     * <p>
     * Handles a CRM request and generates a CRM response.
     * </p>
     *
     * @param request   Holds the {@link CrmRequest} details
     * @param response  Represents the {@link CrmResponse}
     * @param eventData Represents the {@link EventData}
     */
    protected abstract void handle(final CrmRequest request, final CrmResponse response, final EventData eventData);

    /**
     * <p>
     * Gets the IAM config manager
     * </p>
     *
     * @return The {@link IamConfigManger}
     */
    protected abstract IamConfigManger<T> getIamConfigManger();

    /**
     * <p>
     * Saves the models in the data base
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param orgId       Represents an organization id
     * @param models      Represents the models
     * @param eventData   Represents the {@link EventData}
     */
    private void create(final CrmResponse crmResponse, final Long orgId, final List<T> models, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(models, Group.Create.class);
        final Action action = Action.CREATE;

        if (errors.isEmpty() && isValidDetails(orgId, action, models)) {
            final ProcessOutcome processOutcome = getIamConfigManger().create(orgId, models);

            loadResponseAndInvokePostAction(action, crmResponse, processOutcome, models, eventData);
            triggerEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * loads the response with the occurred validation errors
     * </p>
     *
     * @param action       The {@link Action} type
     * @param crmResponse  The {@link CrmResponse}
     * @param errors       The collection of {@link ErrorDescriptor}
     */
    protected void loadValidationErrors(final Action action, final CrmResponse crmResponse, final Collection<ErrorDescriptor> errors) {
        final ErrorCode errorCode = getActionErrorCode(action);

        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, getActionValidationErrorKey(action)), errors));
    }

    /**
     * <p>
     * Loads the response and invokes the post action
     * </p>
     *
     * @param action                  The {@link Action} type
     * @param crmResponse             The {@link CrmResponse}
     * @param processOutcome The collection of {@link ErrorDescriptor}
     * @param models                  The collection of {@link TlcModel}
     * @param eventData               The {@link EventData}
     */
    protected void loadResponseAndInvokePostAction(final Action action, final CrmResponse crmResponse, final ProcessOutcome processOutcome,
                                                   final Collection<T> models, final EventData eventData) {
        crmResponse.put(getActionKey(action), processOutcome.getProcessedRecords());
        final ErrorSummary errorSummary = processOutcome.getErrorSummary();

        if (Objects.nonNull(errorSummary)) {
            crmResponse.setErrorCode(getActionErrorCode(action));
            crmResponse.setErrorSummary(errorSummary);
        }
        eventData.setValues(models.stream().filter(model -> processOutcome.getProcessedRecords().contains(model.id())).toList());
        eventData.setAction(action);
    }

    /**
     * <p>
     * Loads the response
     * </p>
     *
     * @param action                  The {@link Action} type
     * @param crmResponse             The {@link CrmResponse}
     * @param processOutcome The collection of {@link ErrorDescriptor}
     */
    protected void loadResponse(final Action action, final CrmResponse crmResponse, final ProcessOutcome processOutcome) {
        crmResponse.put(getActionKey(action), processOutcome.getProcessedRecords());
        final ErrorSummary errorSummary = processOutcome.getErrorSummary();

        if (Objects.nonNull(errorSummary)) {
            crmResponse.setErrorCode(getActionErrorCode(action));
            crmResponse.setErrorSummary(errorSummary);
        }
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
     * Gets the action key
     * </p>
     *
     * @param action The {@link Action} type
     * @return The action key
     */
    private String getActionKey(final Action action) {
        return switch (action) {
            case CREATE -> IamFields.CREATED_RECORDS;
            case UPDATE -> IamFields.UPDATED_RECORDS;
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

    /**
     * <p>
     * Updates the given models and stored into the data base
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param orgId       Represents an organization id
     * @param models      Represents the models
     * @param eventData   Represents the {@link EventData}
     */
    private void update(final CrmResponse crmResponse, final Long orgId, final Collection<T> models, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(models, Group.Update.class);
        final Action action = Action.UPDATE;

        if (errors.isEmpty() && isValidDetails(orgId, action, models)) {
            final ProcessOutcome processOutcome = getIamConfigManger().update(orgId, models);

            loadResponseAndInvokePostAction(action, crmResponse, processOutcome, models, eventData);
            triggerEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * Partially updates the given model
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param model       Represents the model
     */
    private void partialUpdate(final CrmResponse crmResponse, final T model) {
        partialUpdate(model.orgId(), crmResponse, List.of(model));
    }

    /**
     * <p>
     * Partially updates the given models
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param orgId       Represents the organization id
     * @param models      Represents the collection of models
     */
    private void partialUpdate(final Long orgId, final CrmResponse crmResponse, final Collection<T> models) {
        final Collection<ErrorDescriptor> errors = validate(models, Group.Update.class);
        final Action action = Action.UPDATE;

        if (errors.isEmpty() && isValidDetails(orgId, Action.PARTIAL_UPDATE, models)) {
            final ProcessOutcome processOutcome = getIamConfigManger().partialUpdate(orgId, models);

            loadResponse(action, crmResponse, processOutcome);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * Deletes the given model
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param model       Represents the model
     */
    private void delete(final CrmResponse crmResponse, final T model) {
        delete(crmResponse, model.orgId(), List.of(model));
    }

    /**
     * <p>
     * Deletes the given models
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param orgId       Represents the organization id
     * @param models      Represents the models
     */
    private void delete(final CrmResponse crmResponse, final Long orgId, final Collection<T> models) {
        final Collection<ErrorDescriptor> errors = validate(models, Group.Delete.class);
        final Action action = Action.DELETE;

        if (errors.isEmpty() && isValidDetails(orgId, action, models)) {
            getIamConfigManger().delete(orgId, models);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * Fetches the entity by using given id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the entity id
     * @return Converted the entity details
     */
    protected T get(final Long orgId, final Long id) {
        return getIamConfigManger().get(orgId, id);
    }

    /**
     * <p>
     * Converts the data from a CRM request into the model
     * </p>
     *
     * @param request Holds the {@link CrmRequest} details
     * @param data    The JSON data to be converted.
     * @return Converted the {@link JsonObject} to entity details
     */
    protected abstract T convert(final CrmRequest request, final JsonObject data, final boolean isCreateEvent);

    /**
     * <p>
     * Converts the partial data from a CRM request into the model
     * </p>
     *
     * @param request Holds the {@link CrmRequest} details
     * @param data    The JSON data to be converted.
     * @return Converted the {@link JsonObject} to entity details
     */
    protected abstract T convertPartialData(final CrmRequest request, final JsonObject data);

    /**
     * <p>
     * Converts the {@link TlcModel} into the {@link JsonObject}
     * </p>
     *
     * @param model Represents the model
     * @return Converted the entity to {@link JsonObject}
     */
    protected JsonObject convert(final T model) {
        return Json.object();
    }

    /**
     * <p>
     * Validates the models depends on the validator groups
     * </p>
     *
     * @param models          Represents the collection of models
     * @param validatorGroups Validates the given models
     */
    protected final Collection<ErrorDescriptor> validate(final Collection<T> models, final Class<?>... validatorGroups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<T>> violations = new HashSet<>();
        final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;

        models.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Validates the details of the given models
     * </p>
     *
     * @param orgId  Represents an Organization id
     * @param action Represents the {@link Action} for given details
     * @param models Represents the Collection of models
     * @return true if the given details are valid
     */
    protected boolean isValidDetails(final Long orgId, final Action action, final Collection<T> models) {
        return true;
    }

    /**
     * <p>
     * Gets the IAM org id by CRM org id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The IAM org id
     */
    protected Long getIamOrgId(final Long orgId) {
        return organizationService.getIamOrgId(orgId);
    }

    /**
     * <p>
     * Fetch and validates the user ids by given user {@link JsonArray}
     * </p>
     *
     * @param orgId     Represents the organisation id
     * @param jsonArray Holds the user {@link JsonArray} details
     * @return The collection of user ids
     */
    protected final Collection<Long> fetchAndValidateUserIds(final long orgId, final JsonArray jsonArray) {
        final Collection<Long> userIds = new HashSet<>();

        jsonArray.forEach(userId -> userIds.add((Long) userId));

        if (!userIds.isEmpty() && !crmUserManager.exists(orgId, userIds)) {
            throw ErrorCode.get(IamErrorCodes.CRM_USERS_NOT_FOUND);
        }

        return userIds;
    }
}
