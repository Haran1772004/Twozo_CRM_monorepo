package com.tlc.crm.common.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.rest.secure.AbstractSecureRestAction;
import com.tlc.crm.common.action.event.EventAction;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.CrmModel;

import com.tlc.crm.notification.app.Event;
import com.tlc.iam.common.models.User;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.HttpStatus;
import com.tlc.web.client.event.EventCallback;
import jakarta.validation.ConstraintViolation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base class for all secure REST actions in the CRM application.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class CrmSecureRestAction<T extends TlcModel> extends AbstractSecureRestAction implements EventAction {

    protected final com.tlc.iam.client.UserService userService;
    protected final UserService crmUserService;
    protected final OrganizationService organizationService;

    private final EventDataHandler eventDataHandler;

    protected CrmSecureRestAction() {
        this.userService = com.tlc.iam.client.UserService.getInstance();
        this.crmUserService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.eventDataHandler = EventDataHandler.getInstance();
    }

    /**
     * Handles an HTTP GET request for retrieving a single resource.
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    public abstract void get(final CrmRequest request, final CrmResponse response);

    /**
     * Handles an HTTP PUT request for fully updating an existing resource.
     *
     * @param request  The CRM request containing the payload and request metadata.
     * @param response The CRM response used to send the updated resource or status result.
     */
    public abstract void put(final CrmRequest request, final CrmResponse response);

    /**
     * Handles an HTTP GET request for retrieving a list or collection of resources.
     *
     * @param request  The CRM request containing pagination, filters, or query parameters.
     * @param response The CRM response used to write the resulting collection.
     */
    public abstract void getAll(final CrmRequest request, final CrmResponse response);

    /**
     * Handles an HTTP POST request for creating a new resource.
     *
     * @param request  The CRM request containing the payload of the resource to be created.
     * @param response The CRM response used to send the created resource or creation status.
     */
    public abstract void post(final CrmRequest request, final CrmResponse response);

    /**
     * Handles an HTTP PATCH request for performing a partial update on an existing resource.
     *
     * @param request  The CRM request containing the partial update payload.
     * @param response The CRM response used to return the modified resource or status details.
     */
    public abstract void patch(final CrmRequest request, final CrmResponse response);

    /**
     * Handles an HTTP DELETE request for removing a specific resource.
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    public abstract void delete(final CrmRequest request, final CrmResponse response);

    /**
     * Handles an HTTP OPTIONS request for CORS checking.
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    public abstract void options(final CrmRequest request, final CrmResponse response);

    /**
     * <p>
     * Fetches and dispatches the entity users
     * </p>
     *
     * @param request     Holds the {@link CrmRequest} details
     * @param model       Represents the model details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected void fetchAndDispatchUser(final CrmRequest request, final T model, final CrmResponse crmResponse) {

        if (model instanceof final CrmModel crmModel) {
            final Collection<Long> userIds = crmModel.userIds();
            final Long orgId = request.orgId();

            if (userIds.isEmpty()) {
                convert(request, model).forEach(crmResponse::put);
            } else {
                crmResponse.convertIntoAsync();

                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        setUser(model, users);
                        convert(request, model).forEach(crmResponse::put);
                        crmResponse.send();
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.user.detail.retrieve.failed", throwable));
                        crmResponse.send();
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), getIamUserIds(orgId, userIds), usersFetchCallBack);
            }
        } else {
            convert(request, model).forEach(crmResponse::put);
        }
    }

    /**
     * <p>
     * Converts subclass of {@link TlcModel} into {@link JsonObject}
     * </p>
     *
     * @param t Model
     * @return {@link JsonObject}
     */
    protected JsonObject convert(final CrmRequest request, final T t) {
        return convert(t);
    }

    /**
     * Converts the {@link TlcModel} into the {@link JsonObject}
     *
     * @param model Represents the entity model
     * @return {@link JsonObject}
     */
    protected JsonObject convert(final T model) {
        return Json.object();
    }

    /**
     * <p>
     * Sets the {@link User} details into {@link TlcModel}
     * </p>
     *
     * @param model Represents the model details
     * @param users Contains the collection of {@link User}
     */
    protected void setUser(final T model, final Collection<User> users) {
    }

    /**
     * <p>
     * Gets the iam org id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The IAM org id
     */
    private Long getIamOrgId(final Long orgId) {
        return organizationService.getIamOrgId(orgId);
    }

    /**
     * <p>
     * Gets the iam user ids given by org id and crm user ids
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Contains the collection of user ids
     * @return The collection of Iam user ids
     */
    private Collection<Long> getIamUserIds(final Long orgId, final Collection<Long> userIds) {
        return crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());
    }

    /**
     * Converts the given CRM request and its associated JSON payload.
     *
     * @param crmRequest The CRM request containing contextual information.
     * @param jsonObject The JSON payload to be converted.
     * @return The converted model object of type {@code T}.
     */
    protected abstract T convert(final CrmRequest crmRequest, final JsonObject jsonObject);

    /**
     * Returns the {@link ConfigManager} instance responsible for managing configuration.
     *
     * @return The configuration manager for the type {@code T}.
     */
    protected abstract ConfigManager<T> getConfigManager();

    /**
     * <p>
     * Converts the {@link JsonArray} into collection of subclass of {@link TlcModel}
     * </p>
     *
     * @param request   Crm request
     * @param jsonArray Holds multiple entries
     * @return List of models
     */
    public Collection<T> convert(final CrmRequest request, final JsonArray jsonArray) {
        final Collection<T> models = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(i);
            final T model = convert(request, jsonObject);

            models.add(model);
        }

        return models;
    }

    /**
     * <p>
     * Converts the {@link JsonArray} into partial data {@link TlcModel}
     * </p>
     *
     * @param request Crm request
     * @param data    json Object
     * @return models
     */
    private Collection<T> convertToPartial(final CrmRequest request, final JsonObject data) {
        final Collection<T> models = new ArrayList<>();
        final JsonArray jsonArray = data.getJsonArray("list");

        for (int index = 0; index < jsonArray.size(); index++) {
            models.add(convertPartialData(request, jsonArray.getJsonObject(index)));
        }
        return models;
    }

    /**
     * <p>
     * Converts partial data into {@link TlcModel}
     * </p>
     *
     * @param request    The request data.
     * @param jsonObject The request json.
     * @return The converted model.
     */
    protected abstract T convertPartialData(final CrmRequest request, final JsonObject jsonObject);

    public void create(final CrmResponse crmResponse, final Long orgId, final Long userId, final EventData eventData, final Collection<T> models) {
        final Collection<ErrorDescriptor> errors = validate(models, Group.Create.class);
        final Action action = Action.CREATE;

        if (errors.isEmpty() && isValidDetails(orgId, userId, action, models)) {
            final ProcessOutcome processOutcome = getConfigManager().create(orgId, models);

            loadResponseAndInvokePostAction(action, crmResponse, processOutcome, eventData, models);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * Updates the given entities.
     * </p>
     *
     * @param crmResponse The response to be sent.
     * @param orgId       The organization id.
     * @param userId      The user id.
     * @param models      Refers the models to be updated.
     */
    public void update(final CrmResponse crmResponse, final Long orgId, final Long userId, final EventData eventData, final Collection<T> models) {
        final Collection<ErrorDescriptor> errors = validate(models, Group.Update.class);
        final Action action = Action.UPDATE;

        if (errors.isEmpty() && isValidDetails(orgId, userId, action, models)) {
            preAction(action, models, eventData);
            final ProcessOutcome processOutcome = getConfigManager().bulkUpdate(orgId, models);

            loadResponseAndInvokePostAction(action, crmResponse, processOutcome, eventData, models);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * Deletes the given entities
     * </p>
     *
     * @param crmResponse The response to be sent.
     * @param orgId       The organization id.
     * @param userId      The user id.
     * @param models      Refers the models to be updated.
     */
    public void delete(final CrmResponse crmResponse, final Long orgId, final Long userId, final EventData eventData, final Collection<T> models) {
        final Collection<ErrorDescriptor> errors = validate(models, Group.Delete.class);
        final Action action = Action.DELETE;

        if (errors.isEmpty() && isValidDetails(orgId, userId, action, models)) {

            try {
                preAction(action, models, eventData);
                getConfigManager().bulkDelete(orgId, models);
                postAction(action, models, eventData);
            } catch (ErrorCode errorCode) {
                crmResponse.setErrorCode(errorCode);
                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.DELETE_ACTION_FAILED, "i18n.common.delete.failed");

            crmResponse.setStatus(HttpStatus.BAD_REQUEST);
            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DELETE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Loads the response.
     * </p>
     *
     * @param action         The {@link Action} type
     * @param crmResponse    The {@link CrmResponse}
     * @param processOutcome The collection of {@link ErrorDescriptor}
     */
    protected void loadResponseAndInvokePostAction(final Action action, final CrmResponse crmResponse, final ProcessOutcome processOutcome, final EventData eventData, final Collection<T> models) {
        final String actionKey = switch (action) {
            case CREATE -> ActionFields.CREATED_RECORDS;
            case UPDATE -> ActionFields.UPDATED_RECORDS;
            case DELETE -> ActionFields.DELETED_RECORDS;
            default -> null;
        };
        crmResponse.put(actionKey, processOutcome.getProcessedRecords());
        final ErrorSummary errorSummary = processOutcome.getErrorSummary();

        if (Objects.nonNull(errorSummary)) {
            final ErrorCode errorCode = switch (action) {
                case UPDATE -> ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");
                case CREATE -> ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");
                case DELETE -> ErrorCode.get(CommonErrorCodes.DELETE_ACTION_FAILED, "i18n.common.delete.failed");
                default -> null;
            };
            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(errorSummary);
        }
        postAction(action, models.stream().filter(model -> processOutcome.getProcessedRecords().contains(model.id()))
                .collect(Collectors.toUnmodifiableSet()), eventData);
    }

    /**
     * <p>
     * Validates the models against the given validator groups
     * </p>
     *
     * @param models          The collection of models to validate.
     * @param validatorGroups The validator groups.
     */
    protected final Collection<ErrorDescriptor> validate(final Collection<T> models, final Class... validatorGroups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<T>> violations = new HashSet<>();
        final ErrorCodeProvider errorCodeProvider = CommonErrorCodes.DATA_VALIDATION_FAILED;

        models.forEach(model -> violations.addAll(validator.validate(model, validatorGroups)));

        return violations.stream().map(violation -> new ErrorDescriptor(ErrorCode.get(errorCodeProvider, violation.getMessage()), null,
                violation.getPropertyPath().toString())).collect(Collectors.toSet());
    }

    /**
     * Validates the given details
     *
     * @param orgId  Organization id
     * @param userId Represents the user id.
     * @param action Perform action like create, update or delete
     * @param models Collection of models
     * @return true if the given details are valid
     */
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<T> models) {
        return true;
    }

    /**
     * <p>
     * loads the response with the occurred validation errors
     * </p>
     *
     * @param action      The {@link Action} type
     * @param crmResponse The {@link CrmResponse}
     * @param errors      The collection of {@link ErrorDescriptor}
     */
    protected void loadValidationErrors(final Action action, final CrmResponse crmResponse, final Collection<ErrorDescriptor> errors) {
        final ErrorCode errorCode = switch (action) {
            case UPDATE -> ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");
            case CREATE -> ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");
            case DELETE -> ErrorCode.get(CommonErrorCodes.DELETE_ACTION_FAILED, "i18n.common.delete.failed");
            default -> null;
        };
        final String validationKey = switch (action) {
            case CREATE -> ErrorKeys.CREATE_ACTION_DATA_VALIDATION_FAILED;
            case UPDATE -> ErrorKeys.UPDATE_ACTION_DATA_VALIDATION_FAILED;
            default -> null;
        };
        crmResponse.setStatus(HttpStatus.BAD_REQUEST);
        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, validationKey), errors));
    }

    /**
     * <p>
     * Executes pre action for the given action and models, {@link EventData} and triggering the PRE event
     * </p>
     *
     * @param action    Represents the {@link Action}
     * @param models    Which contains the collection of models
     * @param eventData Represents the  {@link EventData}
     */
    protected void preAction(final Action action, final Collection<? extends TlcModel> models, final EventData eventData) {
        eventData.setAction(action);
        eventData.setValues(models);
        triggerEvents(eventData, Event.Type.PRE);
    }

    /**
     * <p>
     * Executes post action for the given action and models, {@link EventData} and triggering the POST event
     * </p>
     *
     * @param action    Represents the {@link Action}
     * @param models    Which contains the collection of models
     * @param eventData Represents the  {@link EventData}
     */
    protected void postAction(final Action action, final Collection<? extends TlcModel> models, final EventData eventData) {
        eventData.setAction(action);
        eventData.setValues(models);
        triggerEvents(eventData, Event.Type.POST);
    }

    /**
     * {@inheritDoc}
     *
     * @param events    Which contains the collection of {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
    public void triggerEvents(final Collection<Event> events, final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvents(events, eventData, eventType);
    }

    /**
     * {@inheritDoc}
     *
     * @param event     Represents the {@link Event}
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
    public void triggerEvent(final Event event, final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvent(event, eventData, eventType);
    }

    /**
     * {@inheritDoc}
     *
     * @param eventData Represents the {@link EventData}
     * @param eventType Represents the {@link Event.Type}
     */
    @Override
    public void triggerEvents(final EventData eventData, final Event.Type eventType) {
        eventDataHandler.handleEvents(eventData, eventType);
    }

    /**
     * <p>
     * Fetches the current models based on the request information
     * </p>
     *
     * @param request Represents the {@link CrmRequest}
     * @returnt The collection of {@link TlcModel}
     */
    protected Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        return Collections.emptyList();
    }

}