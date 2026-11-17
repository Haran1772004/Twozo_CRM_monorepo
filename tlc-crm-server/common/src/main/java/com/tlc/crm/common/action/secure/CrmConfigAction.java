package com.tlc.crm.common.action.secure;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.CrmModel;
import com.tlc.crm.common.models.RequestType;
import com.tlc.iam.common.models.User;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.client.event.EventCallback;
import jakarta.validation.ConstraintViolation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class CrmConfigAction<T extends TlcModel> extends SecureEventAction {

    private static final JsonObject JSON_OBJECT = Json.object();

    protected final com.tlc.iam.client.UserService userService;
    protected final UserService crmUserService;
    protected final OrganizationService organizationService;

    protected CrmConfigAction() {
        this.userService = com.tlc.iam.client.UserService.getInstance();
        this.crmUserService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
    }

    @Override
    protected void secureProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = request.orgId();
        final Long userId = request.userId();
        final CrmEventRequest crmEventRequest = (CrmEventRequest) request;
        final EventData eventData = crmEventRequest.getEventData();

        switch (type) {
            case "get" -> fetchAndDispatchUser(request, get(orgId, data.getLong("id")), crmResponse);
            case "create" -> {
                final T model = convert(request, data);

                create(orgId, userId, crmResponse, eventData, List.of(model), type);
            }
            case "bCreate" -> bCreate(crmEventRequest, crmResponse, eventData);
            case "update" -> {
                final T model = convert(request, data);

                update(orgId, userId, crmResponse, data, eventData, List.of(model), type);
            }
            case "bUpdate" -> bUpdate(request, crmResponse, eventData, false);
            case "pUpdate" ->
                    update(orgId, userId, crmResponse, data, eventData, List.of(convertPartialData(request, data)), type);
            case "pBulkUpdate" -> bUpdate(request, crmResponse, eventData, true);
            case "delete" -> delete(crmResponse, orgId, userId, data, eventData);
            case "bDelete" -> bDelete(crmEventRequest, crmResponse);
            case "exists" -> crmResponse.put("found", exists(convert(request, data)));
            default -> handle(request, crmResponse);
        }
    }

    /**
     * <p>
     * Handles non default config actions
     * </p>
     *
     * @param request
     * @param response
     */
    public abstract void handle(final CrmRequest request, final CrmResponse response);

    /**
     * {@inheritDoc}
     *
     * @param apiType Represents the current API.
     * @return The {@link RequestType}.
     */
    @Override
    protected RequestType fetchRequestType(final String apiType) {
        return switch (apiType) {
            case "bCreate", "bUpdate", "pBulkUpdate", "bDelete" -> RequestType.BULK;
            default -> RequestType.SINGLE;
        };
    }

    /**
     * <p>
     * Creates entities
     * </p>
     *
     * @param models
     * @return
     */
    public void create(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<T> models, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(models, Create.class);
        final Action action = Action.CREATE;

        if (errors.isEmpty() && isValidDetails(orgId, userId, action, models, crmResponse)) {
            final ProcessOutcome processOutcome = getConfigManager().create(orgId, models);

            loadResponseAndInvokePostAction(action, crmResponse, processOutcome, models, eventData);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * Updates the given entities
     * </p>
     *
     * @param models
     */
    public void update(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<T> models, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(models, Update.class);
        final Action action = Action.UPDATE;

        if (errors.isEmpty() && isValidDetails(orgId, userId, action, models)) {
            preAction(action, models, eventData);
            final ProcessOutcome processOutcome = getConfigManager().bulkUpdate(orgId, models);

            loadResponseAndInvokePostAction(action, crmResponse, processOutcome, models, eventData);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * <p>
     * loads the response with the occurred validation errors
     * </p>
     * @param action       The {@link Action} type
     * @param crmResponse  The {@link CrmResponse}
     * @param errors       The collection of {@link ErrorDescriptor}
     */
    protected void loadValidationErrors(final Action action, final CrmResponse crmResponse, final Collection<ErrorDescriptor> errors) {
        final ErrorCode errorCode = getActionErrorCode(action);

        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                getActionValidationErrorKey(action)), errors));
    }

    /**
     * <p>
     * Loads the response and invokes the post action
     * </p>
     *
     * @param action               The {@link Action} type
     * @param crmResponse          The {@link CrmResponse}
     * @param processOutcome The {@link ProcessOutcome}
     * @param models               The collection of {@link TlcModel}
     * @param eventData            The {@link EventData}
     */
    protected void loadResponseAndInvokePostAction(final Action action, final CrmResponse crmResponse, final ProcessOutcome processOutcome,
                                                   final Collection<T> models, final EventData eventData) {
        crmResponse.put(getActionKey(action), processOutcome.getProcessedRecords());
        final ErrorSummary errorSummary = processOutcome.getErrorSummary();

        if (Objects.nonNull(errorSummary)) {
            crmResponse.setErrorCode(getActionErrorCode(action));
            crmResponse.setErrorSummary(errorSummary);
        }
        postAction(action, models.stream().filter(model -> processOutcome.getProcessedRecords().contains(model.id()))
                    .collect(Collectors.toUnmodifiableSet()), eventData);
    }

    /**
     * <p>
     * Loads the response
     * </p>
     *
     * @param action               The {@link Action} type
     * @param crmResponse          The {@link CrmResponse}
     * @param processOutcome The {@link ProcessOutcome}
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
     * Gets the action key
     * </p>
     *
     * @param action The {@link Action} type
     * @return The action key
     */
    private String getActionKey(final Action action) {
        return switch (action) {
            case CREATE -> ActionFields.CREATED_RECORDS;
            case UPDATE -> ActionFields.UPDATED_RECORDS;
            case DELETE -> ActionFields.DELETED_RECORDS;
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
    protected ErrorCode getActionErrorCode(final Action action) {
        return switch (action) {
            case UPDATE -> ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");
            case CREATE -> ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");
            case DELETE -> ErrorCode.get(CommonErrorCodes.DELETE_ACTION_FAILED, "i18n.common.delete.failed");
            default -> null;
        };
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
            default -> null;
        };
    }

    /**
     * Deletes the given entity
     *
     * @param crmResponse Refers the response to be sent.
     * @param orgId       Refers the orgId.
     * @param userId      Refers the userId.
     * @param data        Refers the request data.
     * @param eventData   Refers the event data.
     */
    public void delete(final CrmResponse crmResponse, final Long orgId, final Long userId, final JsonObject data, final EventData eventData) {
        delete(crmResponse, userId, get(orgId, data.getLong("id")), eventData);
    }

    /**
     * <p>
     * Deletes the given entity
     * </p>
     *
     * @param model
     */
    public void delete(final CrmResponse crmResponse, final Long userId, final T model, final EventData eventData) {
        delete(crmResponse, model.orgId(), userId, List.of(model), eventData);
    }

    /**
     * <p>
     * Deletes the models based on the provided {@link CrmRequest}
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest}
     * @param crmResponse Holds the {@link CrmResponse}
     */
    protected void bDelete(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final CrmEventRequest crmEventRequest = (CrmEventRequest) crmRequest;
        final Long orgId = crmRequest.orgId();
        final Set<Long> ids = crmRequest.getRequestJson().getJsonObject(ActionFields.DATA).getJsonArray(ActionFields.IDS).toList().stream()
                .map(id -> (Long) id).collect(Collectors.toUnmodifiableSet());

        this.delete(crmResponse, orgId, crmRequest.userId(), get(orgId, ids), crmEventRequest.getEventData());
    }

    /**
     * <p>
     * Deletes the given entities
     * </p>
     *
     * @param models
     * @param eventData Represents the {@link EventData}
     */
    public void delete(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<T> models, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(models, Delete.class);
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
        } else  {
            final ErrorCode errorCode = ErrorCode.get(CommonErrorCodes.DELETE_ACTION_FAILED, "i18n.common.delete.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.DELETE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Checks the existence of an entity
     * </p>
     *
     * @param model
     * @return
     */
    public boolean exists(final T model) {
        return getConfigManager().exists(model);
    }

    /**
     * <p>
     * Partially fetches the values of an entity
     * </p>
     *
     * @param orgId
     * @param id
     * @return
     */
    public T partialGet(final Long orgId, final Long id) {
        return getConfigManager().partialGet(orgId, id);
    }

    /**
     * <p>
     * Fetches an entity by its id and organization id
     * </p>
     *
     * @param orgId
     * @param id
     * @return
     */
    public T get(final Long orgId, final Long id) {
        return getConfigManager().get(orgId, id);
    }

    /**
     * <p>
     * Fetches the entities by its ids and organization id
     * </p>
     *
     * @param orgId
     * @param ids
     * @return
     */
    public Collection<T> get(final Long orgId, final Collection<Long> ids) {
        return getConfigManager().get(orgId, ids);
    }

    /**
     * <p>
     * Fetches the CRM config manager
     * </p>
     *
     * @return
     */
    public ConfigManager<T> getConfigManager() {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into subclass of {@link TlcModel}
     * </p>
     *
     * @param request    Crm request
     * @param jsonObject Holds entity details
     * @return Converted entity
     */
    public T convert(final CrmRequest request, final JsonObject jsonObject) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
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
     * Converts the {@link JsonArray} into collection of subclass of {@link TlcModel}
     * </p>
     *
     * @param request   - Crm request
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
     * Fetches and dispatches the summary details
     * </p>
     *
     * @param request     Holds the {@link CrmRequest} details
     * @param model       Represents the model details
     * @param data        Holds the entity details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected void fetchAndDispatchSummary(final CrmRequest request, final T model, final JsonObject data, final CrmResponse crmResponse) {
        if (model instanceof final CrmModel crmModel) {
            final Collection<Long> userIds = crmModel.userIds();
            final Long orgId = request.orgId();

            if (userIds.isEmpty()) {
                convertSummary(request, model, data, List.of()).forEach(crmResponse::put);
            } else {
                crmResponse.convertIntoAsync();

                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        setUser(model, users);
                        convertSummary(request, model, data, users).forEach(crmResponse::put);
                        crmResponse.send();
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                        crmResponse.send();
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), getIamUserIds(orgId, userIds), usersFetchCallBack);
            }
        } else {
            convertSummary(request, model, data, List.of()).forEach(crmResponse::put);
        }
    }

    /**
     * <p>
     * Fetches and dispatches the summary details
     * </p>
     *
     * @param request     Holds the {@link CrmRequest} details
     * @param model       Represents the model details
     * @param data        Holds the entity details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected void fetchAndDispatchSummaryV2(final CrmRequest request, final T model, final JsonObject data, final CrmResponse crmResponse) {
        if (model instanceof final CrmModel crmModel) {
            final Collection<Long> userIds = crmModel.userIds();
            final Long orgId = request.orgId();

            if (userIds.isEmpty()) {
                convertSummaryV2(request, model, data, List.of()).forEach(crmResponse::put);
            } else {
                crmResponse.convertIntoAsync();

                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {
                            setUser(model, users);
                            convertSummaryV2(request, model, data, users).forEach(crmResponse::put);
                        } finally {
                            crmResponse.send();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                        crmResponse.send();
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), getIamUserIds(orgId, userIds), usersFetchCallBack);
            }
        } else {
            convertSummaryV2(request, model, data, List.of()).forEach(crmResponse::put);
        }
    }

    /**
     * <p>
     * Fetches and dispatches the profile details
     * </p>
     *
     * @param request     Holds the {@link CrmRequest} details
     * @param model       Represents the model details
     * @param data        Holds the entity details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected void fetchAndDispatchProfile(final CrmRequest request, final T model, final JsonObject data, final CrmResponse crmResponse) {
        if (model instanceof final CrmModel crmModel) {
            final Collection<Long> userIds = crmModel.userIds();
            final Long orgId = request.orgId();

            if (Objects.isNull(userIds) || userIds.isEmpty()) {
                convertProfile(request, model, data, List.of()).forEach(crmResponse::put);
            } else {
                crmResponse.convertIntoAsync();

                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        setUser(model, users);
                        convertProfile(request, model, data, users).forEach(crmResponse::put);
                        crmResponse.send();
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                        crmResponse.send();
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), getIamUserIds(orgId, userIds), usersFetchCallBack);
            }
        } else {
            convertProfile(request, model, data, List.of()).forEach(crmResponse::put);
        }
    }

    /**
     * <p>
     * Fetches and dispatches the summary details for mobile view
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param jsonObject  Holds the entity details
     * @param model       Represents the model details
     * @param crmResponse Represents the {@link CrmResponse}
     */
    protected void fetchAndDispatchSummary(final CrmRequest crmRequest, final JsonObject jsonObject, final T model, final CrmResponse crmResponse) {
        if (model instanceof final CrmModel crmModel) {
            final Collection<Long> userIds = crmModel.userIds();
            final long orgId = crmRequest.orgId();

            if (userIds.isEmpty()) {
                convertSummary(crmRequest, jsonObject, model, List.of()).forEach(crmResponse::put);
            } else {
                crmResponse.convertIntoAsync();

                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        setUser(model, users);
                        convertSummary(crmRequest, jsonObject, model, users).forEach(crmResponse::put);
                        crmResponse.send();
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                        crmResponse.send();
                    }
                };
                userService.fetchUsers(getIamOrgId(orgId), getIamUserIds(orgId, userIds), usersFetchCallBack);
            }
        } else {
            convertSummary(crmRequest, jsonObject, model, List.of()).forEach(crmResponse::put);
        }
    }

    /**
     * <p>
     * Converts the {@link TlcModel} into the summary {@link JsonObject}
     * </p>
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param jsonObject Holds the entity details
     * @param model      Represents the entity model
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    protected JsonObject convertSummary(final CrmRequest crmRequest, final JsonObject jsonObject, final T model, final Collection<User> users) {
        return JSON_OBJECT;
    }

    /**
     * <p>
     * Converts the {@link TlcModel} into the summary {@link JsonObject}
     * </p>
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param model      Represents the entity model
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    protected JsonObject convertSummary(final CrmRequest crmRequest, final T model, final JsonObject data, final Collection<User> users) {
        return JSON_OBJECT;
    }

    /**
     * <p>
     * Converts the {@link TlcModel} into the summary {@link JsonObject}
     * </p>
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param model      Represents the entity model
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    protected JsonObject convertSummaryV2(final CrmRequest crmRequest, final T model, final JsonObject data, final Collection<User> users) {
        return JSON_OBJECT;
    }

    /**
     * <p>
     * Converts the {@link TlcModel} into the summary {@link JsonObject}
     * </p>
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param model      Represents the entity model
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    protected JsonObject convertProfile(final CrmRequest crmRequest, final T model, final JsonObject data, final Collection<User> users) {
        return JSON_OBJECT;
    }

    /**
     * <p>
     * Converts collection of subclass of {@link TlcModel} into {@link JsonArray}
     * </p>
     *
     * @param models
     * @return {@link JsonArray}
     */
    public JsonArray convert(final Collection<T> models) {
        final JsonArray array = Json.array();

        models.stream().forEach(model -> array.put(convert(model)));

        return array;
    }

    /**
     * <p>
     * Converts partial data into {@link TlcModel}
     * </p>
     *
     * @param request    Represents the {@link CrmRequest}
     * @param jsonObject Represents the {@link JsonObject}
     * @return the converted model with partial data
     */
    public T convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * <p>
     * Validates the models against the given validator groups
     * </p>
     *
     * @param models
     * @param validatorGroups
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
     * Validates the given details with the {@link CrmResponse}
     *
     * @param orgId       Organization id
     * @param userId      Represents the user id.
     * @param action      Perform action like create, update or delete
     * @param models      Collection of models
     * @param crmResponse Represents the {@link CrmResponse}
     * @return true if the given details are valid
     */
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<T> models,
                                     final CrmResponse crmResponse) {
        return isValidDetails(orgId, userId, action, models);
    }

    /**
     * <p>
     * Gets the iam org id
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
     * Gets the iam user ids given by org id and crm user ids
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Contains the collection of user ids
     * @return The collection of Iam user ids
     */
    protected Collection<Long> getIamUserIds(final Long orgId, final Collection<Long> userIds) {
        return crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Fetches the current models based on the request information
     * </p>
     *
     * @param request Represents the {@link CrmRequest}
     * @return The collection of {@link TlcModel}
     */
    @Override
    public Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ActionFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);
        final long orgId = request.orgId();

        return switch (type) {
            case "update", "pUpdate" ->
                    data.containsKey(ActionFields.ID) ? (Collection<TlcModel>) get(orgId, List.of(data.getLong(ActionFields.ID))) : Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of api type
     */
    @Override
    protected Collection<String> stateRequiredTypes() {
        return Set.of(ActionFields.UPDATE, ActionFields.BULK_UPDATE, ActionFields.PARTIAL_UPDATE,
                ActionFields.PARTIAL_BULK_UPDATE, ActionFields.DELETE);
    }

    /**
     * <p>
     * Updates the given entity.
     * </p>
     *
     * @param orgId     An organization's id.
     * @param userId    A user's id.
     * @param response  {@link CrmResponse}.
     * @param data      {@link CrmRequest}
     * @param models    Holds Models.
     * @param eventData {@link EventData}
     */
    protected void update(final Long orgId, final Long userId, final CrmResponse response, final JsonObject data,
                          final EventData eventData, final Collection<T> models, final String requestType) {
        update(response, orgId, userId, models, eventData);
    }

    /**
     * <p>
     * Updates models based on the provided CRM request and response
     * </p>
     *
     * @param crmRequest  Holds the {@link CrmRequest}
     * @param crmResponse Holds the {@link CrmResponse}
     * @param eventData   Represents the {@link EventData}
     * @param isPartial   A flag indicating whether the update is partial or full
     */
    protected void bUpdate(final CrmRequest crmRequest, final CrmResponse crmResponse,
                           final EventData eventData, final boolean isPartial) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);
        final Collection<T> models = isPartial ? convertToPartial(crmRequest, data)
                : convert(crmRequest, data.getJsonArray(ActionFields.LIST));

        update(crmRequest.orgId(), crmRequest.userId(), crmResponse, data, eventData,
                models, requestJson.getString(ActionFields.TYPE));
    }

    /**
     * <p>
     * Creates the given entity.
     * </p>
     *
     * @param orgId     An organization's id.
     * @param userId    A user's id.
     * @param response  {@link CrmResponse}.
     * @param models    Holds Models.
     * @param eventData {@link EventData}
     */
    protected void create(final Long orgId, final Long userId, final CrmResponse response, final EventData eventData,
                          final Collection<T> models, final String requestType) {
        create(response, orgId, userId, models, eventData);
    }

    /**
     * <p>
     * Creates the given entity.
     * </p>
     *
     * @param crmRequest Holds the {@link CrmRequest}
     * @param response   Holds the  {@link CrmResponse}.
     * @param eventData  {@link EventData}
     */
    protected void bCreate(final CrmRequest crmRequest, final CrmResponse response,
                           final EventData eventData) {
        final JsonArray jsonArray = crmRequest.getRequestJson().getJsonObject(ActionFields.DATA).getJsonArray(ActionFields.LIST);
        final Collection<T> models = convert(crmRequest, jsonArray);

        create(response, crmRequest.orgId(), crmRequest.userId(), models, eventData);
    }
}