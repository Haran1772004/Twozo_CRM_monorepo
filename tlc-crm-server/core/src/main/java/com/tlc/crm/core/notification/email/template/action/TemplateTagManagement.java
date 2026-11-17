package com.tlc.crm.core.notification.email.template.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.list.ListStateProcessor;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.common.list.pagination.ListPaginationState;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;
import com.tlc.crm.core.notification.email.template.api.model.converter.TemplateTagConverter;
import com.tlc.crm.core.notification.email.template.fields.TemplateFields;
import com.tlc.crm.core.notification.email.template.fields.TemplateTagFields;
import com.tlc.crm.core.notification.email.template.status.TemplateTagErrorCodes;
import com.tlc.crm.core.notification.email.template.api.TemplateTagManager;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.iam.common.models.User;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link TemplateTag} related web actions
 * </P>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@WebAction(path = "/email/template/tag/mgmt")
public class TemplateTagManagement extends CrmConfigAction<TemplateTag> {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 50;

    private final TemplateTagConverter templateTagConverter;
    private final TemplateTagManager templateTagManager;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final UserService crmUserService;
    private final com.tlc.iam.client.UserService userService;
    private final OrganizationService organizationService;
    private final ListStateProcessor listStateProcessor;

    public TemplateTagManagement() {
        templateTagManager = TemplateTagManager.getInstance();
        templateTagConverter = TemplateTagConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        crmUserService = UserService.getInstance();
        userService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        listStateProcessor = ListStateProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The received request
     * @param response The response to be sent
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(TemplateTagFields.TYPE.getName());

        switch (type) {
            case "list"-> fetchAndDispatchTemplateTagList(orgId, request.userId(), request, response);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link TemplateTag}
     */
    @Override
    public ConfigManager<TemplateTag> getConfigManager() {
        return templateTagManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The received request
     * @param jsonObject An object that holds request data
     * @return The {@link TemplateTag}
     */
    @Override
    public TemplateTag convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return templateTagConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The received request data
     * @param jsonObject An object that holds request data
     * @return The {@link TemplateTag}
     */
    @Override
    public TemplateTag convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return templateTagConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param templateTag Represents the template tag attributes
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convert(final TemplateTag templateTag) {
        return templateTagConverter.convert(templateTag);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id.
     * @param action       Represents the {@link Action} for given details
     * @param templateTags Represent the {@link Collection} of {@link TemplateTag}
     * @return True if all the details are valid, otherwise returns false
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<TemplateTag> templateTags) {
        if (Objects.nonNull(templateTags) && !templateTags.isEmpty()) {

            switch (action) {
                case CREATE, UPDATE -> {
                    final Collection<String> names = templateTags.stream().map(tag -> tag.getName().toLowerCase()).collect(Collectors.toSet());

                    if (templateTagManager.checkNamesExistence(orgId, names)) {
                        throw ErrorCode.get(TemplateTagErrorCodes.TEMPLATE_TAG_NAME_ALREADY_EXISTS, "i18n.email.template.tag.name.already.exists");
                    }
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Fetches and dispatches a list of template tags for an organization
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param request  Represents the crm request
     * @param response Represents the crm response
     */
    private void fetchAndDispatchTemplateTagList(final Long orgId, final Long userId, final CrmRequest request, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(TemplateFields.DATA.getName());
        final ListPaginationState paginationState = new ListPaginationState(orgId, userId, ListType.TEMPLATE_TAG_LIST);
        final JsonObject limitJson = listStateProcessor.processAndGetLimitJson(data, paginationState);
        final int start = limitJson.getInt(ListActionFields.START);
        final int limit = limitJson.getInt(ListActionFields.COUNT);
        final String searchText = data.optString(TemplateFields.SEARCH_TEXT.getName(), "");
        final Long totalRecords = templateTagManager.getTemplateTagCount(orgId, searchText);
        final String createdAtKey = TemplateTagFields.CREATED_AT.getName();

        if (totalRecords > 0 && start <= totalRecords) {
            final JsonObject sortJson = data.optJsonObject(TemplateTagFields.SORT.getName());
            final String field;
            final SortType sortType;

            if (Objects.nonNull(sortJson)) {
                field = sortJson.optString(TemplateTagFields.FIELD.getName(), createdAtKey);
                sortType = SortType.getSortType(sortJson.optString(TemplateFields.TYPE.getName(), TemplateTagFields.DESC.getName()));
            } else {
                field = createdAtKey;
                sortType = SortType.DESCENDING;
            }

            final Collection<TemplateTag> templateTags = templateTagManager.getTagList(orgId, start, limit, field, sortType, searchText);
            final Collection<Long> ownerIds = templateTags.stream().map(TemplateTag::getOwnerId).collect(Collectors.toUnmodifiableSet());
            final Collection<Long> iamUserIds = crmUserService.getUsers(orgId, ownerIds).stream().map(CrmUser::getIamId).toList();

            response.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> Users) {
                    final Map<Long, User> userMap = Users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    for (final TemplateTag templateTag : templateTags) {
                        final CrmUser crmUser = crmUserService.getUser(orgId, templateTag.getOwnerId());

                        crmUser.setUser(userMap.get(crmUser.getIamId()));
                        templateTag.setCreatedBy(crmUser);
                        jsonArray.put(convert(templateTag));
                    }
                    response.put(TemplateFields.TOTAL_RECORDS.getName(), totalRecords);
                    response.put(TemplateFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
                    response.put(TemplateFields.LIST.getName(), jsonArray);
                    response.send();
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(response, null, ErrorCode.get(TemplateTagErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        } else {
            response.put(TemplateFields.TOTAL_RECORDS.getName(), totalRecords);
            response.put(TemplateFields.HAS_MORE.getName(), false);
            response.put(TemplateFields.LIST.getName(), jsonArray);
            response.send();
        }
    }
}