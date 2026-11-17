package com.tlc.crm.entity.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.RequestType;
import com.tlc.crm.entity.builder.EntitySalesOwnerHandler;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.SummaryOverviewFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.manager.EntityConfigManager;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.SummaryOverview;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.notification.app.Event;
import com.tlc.iam.common.models.User;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * A Config manager to engage entity related activities.
 * </p>
 *
 * @param <T> Refers {@link Entity}.
 * @author Ajay.
 * @version 1.0.
 */
public abstract class EntityConfigAction<T extends Entity> extends AsyncBulkConfigAction<T> {

    protected final FieldService fieldService;
    private final CrmUserManager crmUserManager;
    private final EntitySalesOwnerHandler entitySalesOwnerHandler;
    private final UserService crmUserService;
    private final com.tlc.iam.client.UserService userService;
    private final OrganizationService organizationService;
    private final CrmUserConverter crmUserConverter;
    private final IconService iconService;

    public EntityConfigAction() {
        crmUserManager = CrmUserManager.getInstance();
        fieldService = FieldService.getSecureInstance();
        entitySalesOwnerHandler = EntitySalesOwnerHandler.getInstance();
        crmUserService = UserService.getInstance();
        userService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        iconService = IconService.getInstance();
    }

    /**
     * <p>
     * Handles non default config actions
     * </p>
     *
     * @param request  Refers {@link CrmRequest}.
     * @param response Refers {@link CrmResponse}.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final CrmEventRequest crmEventRequest = (CrmEventRequest) request;
        final EventData eventData = crmEventRequest.getEventData();
        final Long userId = request.userId();

        switch (type) {
            case "clone" -> {
                final T model = convert(request, data);

                create(response, orgId, userId, List.of(model), eventData);
            }
            case "assignOwner" -> assignOwner(orgId, userId, data.getLong(FieldKeys.USER_ID.getName()),
                    Set.of(data.getLong(FieldKeys.ID.getName())), response, eventData);
            case "bAssignOwner", "bAssignFollowers", "deleteMappedTag", "mapTag" -> bUpdate(crmEventRequest, response, eventData, false);
            case "overview" -> overview(orgId, data, response);
            default -> handleRequest(request, response);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param apiType Represents the current API.
     * @return The {@link RequestType}.
     */
    @Override
    protected RequestType fetchRequestType(final String apiType) {
        return switch (apiType) {
            case "bAssignOwner" -> RequestType.BULK;
            default -> super.fetchRequestType(apiType);
        };
    }

    /**
     * <p>
     * Handles non default config actions
     * </p>
     *
     * @param request  Refers {@link CrmRequest}.
     * @param response Refers {@link CrmResponse}.
     */
    protected abstract void handleRequest(final CrmRequest request, final CrmResponse response);

    /**
     * <p>
     * Assigns owner to the entity.
     * </p>
     *
     * @param orgId       Represents an organization's id.
     * @param userId      Represents the user id.
     * @param ownerId     Represents the owner id.
     * @param ids         Represents the entity id.
     * @param crmResponse Represents the response to be sent.
     * @param eventData   Represents the {@link EventData}
     */
    private void assignOwner(final Long orgId, final long userId, final Long ownerId, final Collection<Long> ids, final CrmResponse crmResponse, final EventData eventData) {
        if (!crmUserManager.exists(orgId, List.of(ownerId))) {
            throw ErrorCode.get(EntityErrorCodes.CRM_USER_NOT_FOUND, crmResponse.resolveI18nString("i18n.user.not.found"));
        }
        final EntityConfigManager<T> entityConfigManager = (EntityConfigManager<T>) getConfigManager();

        if (entityConfigManager.exists(orgId, ids)) {
            final Collection<T> models = entityConfigManager.get(orgId, ids);

            entityConfigManager.updateOwner(orgId, userId, ids, ownerId, Source.get(getConfigActionName()));
            crmResponse.put(ActionFields.UPDATED_RECORDS, ids);

            models.forEach(model -> entitySalesOwnerHandler.assignOwner(orgId, ownerId, model));
            eventData.setValues(models);
            eventData.setAction(Action.CREATE);
            triggerEvents(eventData, Event.Type.POST);
        } else {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_NOT_FOUND, crmResponse.resolveI18nString("i18n.entity.not.found"));
        }
    }

    /**
     * Gets the overview details.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json.
     * @param response Refers the {@link CrmResponse} to be sent.
     */
    protected abstract void overview(final long orgId, final JsonObject data, final CrmResponse response);

    /**
     * Converts the {@link SummaryOverview} into {@link JsonObject} by fetching the users.
     *
     * @param orgId           Refers the organization id.
     * @param summaryOverview Refers the {@link SummaryOverview} details.
     * @param response        Refers the {@link CrmResponse} to be sent.
     */
    protected void fetchUsersForOverview(final long orgId, final SummaryOverview summaryOverview, final CrmResponse response) {
        final Collection<Long> userIds = summaryOverview.getUserIds();

        if (Objects.isNull(userIds) || userIds.isEmpty()) {
            convertOverviewToJson(summaryOverview, response);
        } else {
            response.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                /**
                 * {@inheritDoc}
                 *
                 * @param users Refers the collection of users.
                 */
                @Override
                public void onSuccess(final Collection<User> users) {
                    convertOverviewToJson(summaryOverview, response);
                    convertUsersToJson(orgId, summaryOverview, users.stream().collect(Collectors.toMap(User::id, Function.identity())), response);
                    response.send();
                }

                /**
                 * {@inheritDoc}
                 *
                 * @param throwable Cause of the failure.
                 */
                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(response, null, ErrorCode.get(EntityErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                }
            };

            userService.fetchUsers(organizationService.getIamOrgId(orgId), crmUserService.getUsers(orgId, userIds).stream()
                    .map(CrmUser::getIamId).collect(Collectors.toSet()), usersFetchCallBack);
        }
    }

    /**
     * Converts the {@link SummaryOverview} into {@link JsonObject}.
     *
     * @param orgId           Refers the organization id.
     * @param summaryOverview Refers the {@link SummaryOverview} details.
     * @param crmUsers        Refers the map of {@link User} with IAM id.
     * @param response        Refers the {@link CrmResponse} to be sent.
     */
    private void convertUsersToJson(final long orgId, final SummaryOverview summaryOverview, final Map<Long, User> crmUsers, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final Collection<Map<String, Object>> activityUsers = summaryOverview.getActivityUsers();

        if (Objects.nonNull(activityUsers) && !activityUsers.isEmpty()) {
            final String percentageKey = SummaryOverviewFields.PERCENTAGE;
            final String countKey = SummaryOverviewFields.COUNT;
            final String userKey = SummaryOverviewFields.USER;
            final String activeUserIdKey = SummaryOverviewFields.ACTIVE_USER_ID;
            final String assigneeIdKey = SummaryOverviewFields.ASSIGNEE_ID;
            final long totalActivityCount = summaryOverview.getTotalActivities();
            int count = 0;

            for (final Map<String, Object> activityUser : activityUsers) {

                if (count > 3) {
                    break;
                }
                final CrmUser crmUser = crmUserService.getUser(orgId, (long) activityUser.get(activeUserIdKey));

                crmUser.setUser(crmUsers.get(crmUser.getIamId()));
                final long activityCount = (long) activityUser.get(assigneeIdKey);
                final JsonObject jsonObject = Json.object();

                jsonObject.put(percentageKey, calculatePercentage(activityCount, totalActivityCount));
                jsonObject.put(countKey, activityCount);
                jsonObject.put(userKey, crmUserConverter.convert(crmUser));

                jsonArray.put(jsonObject);
                count++;
            }

            if (count < activityUsers.size()) {
                final JsonObject jsonObject = Json.object();
                final int othersCount = activityUsers.stream().skip(4).mapToInt(activityUser -> Math
                        .toIntExact((long) activityUser.get(assigneeIdKey))).sum();

                jsonObject.put(percentageKey, calculatePercentage(othersCount, totalActivityCount));
                jsonObject.put(countKey, othersCount);
                jsonObject.put(FieldKeys.NAME.getName(), SummaryOverviewFields.OTHERS);

                jsonArray.put(jsonObject);
            }
            response.put(SummaryOverviewFields.MOST_ACTIVE_USERS, jsonArray);
        }
    }

    /**
     * Converts the {@link SummaryOverview} details into the response.
     *
     * @param summaryOverview Refers the {@link SummaryOverview} details.
     * @param response        Refers the {@link CrmResponse} to be sent.
     */
    private void convertOverviewToJson(final SummaryOverview summaryOverview, final CrmResponse response) {
        final Collection<Map<String, Object>> activityTypes = summaryOverview.getActivityTypes();

        if (Objects.nonNull(activityTypes) && !activityTypes.isEmpty()) {
            final String percentageKey = SummaryOverviewFields.PERCENTAGE;
            final String countKey = SummaryOverviewFields.COUNT;
            final String nameKey = FieldKeys.NAME.getName();
            final String typeIdKey = SummaryOverviewFields.TYPE_ID;
            final String iconIdKey = SummaryOverviewFields.ICON_ID;
            final String iconKey = SummaryOverviewFields.ICON;
            final String displayNameKey = FieldKeys.DISPLAY_NAME.getName();
            final String idKey = FieldKeys.ID.getName();
            final long totalActivityCount = summaryOverview.getTotalActivities();
            final JsonArray jsonArray = Json.array();
            int count = 0;

            for (final Map<String, Object> activityType : activityTypes) {

                if (count > 3) {
                    break;
                }
                final long activityTypeCount = (long) activityType.get(typeIdKey);
                final Icon icon = iconService.get((long) activityType.get(iconIdKey));
                final JsonObject jsonObject = Json.object();
                final JsonObject iconJson = Json.object();

                iconJson.put(idKey, icon.id());
                iconJson.put(nameKey, icon.getName());
                iconJson.put(displayNameKey, response.resolveI18nString((icon.getI18nName())));

                jsonObject.put(percentageKey, calculatePercentage(activityTypeCount, totalActivityCount));
                jsonObject.put(countKey, activityTypeCount);
                jsonObject.put(nameKey, (String) activityType.get(FieldKeys.NAME.name()));
                jsonObject.put(iconKey, iconJson);

                jsonArray.put(jsonObject);
                count++;
            }

            if (count < activityTypes.size()) {
                final JsonObject jsonObject = Json.object();
                final int othersCount = activityTypes.stream().skip(4).mapToInt(activityType -> Math.toIntExact((long) activityType.get(typeIdKey))).sum();

                jsonObject.put(percentageKey, calculatePercentage(othersCount, totalActivityCount));
                jsonObject.put(countKey, othersCount);
                jsonObject.put(nameKey, SummaryOverviewFields.OTHERS);

                jsonArray.put(jsonObject);
            }
            response.put(SummaryOverviewFields.ACTIVITY_TYPES, jsonArray);
        }
    }

    /**
     * Calculates percentage for summary overview.
     *
     * @param value      Represents the value.
     * @param totalCount Represents the total count.
     * @return The percentage value.
     */
    private double calculatePercentage(final long value, final long totalCount) {
        final float percentage = (value * 100) / (float) totalCount;

        return Math.round(percentage * 100.0) / 100.0;
    }
}
