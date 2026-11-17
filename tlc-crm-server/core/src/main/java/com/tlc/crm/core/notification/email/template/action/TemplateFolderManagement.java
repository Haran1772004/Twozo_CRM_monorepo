package com.tlc.crm.core.notification.email.template.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.security.permission.annotation.authorizer.EmailAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.crm.core.notification.email.template.api.model.converter.TemplateFolderConverter;
import com.tlc.crm.core.notification.email.template.fields.TemplateFields;
import com.tlc.crm.core.notification.email.template.fields.TemplateFolderFields;
import com.tlc.crm.core.notification.email.template.service.TemplateFolderService;
import com.tlc.crm.core.notification.email.template.status.TemplateFolderErrorCodes;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.core.notification.email.template.api.TemplateFolderManager;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.FilterAction;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.Visibility;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.AccessType;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link TemplateFolder} related web actions
 * </P>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@EmailAuthorizer(subCategories = SubCategory.Type.SHARE_EMAIL_TEMPLATES_FOLDER, needUniqueApiHandling = true)
@WebAction(path = "/email/template/folder/mgmt")
public class TemplateFolderManagement extends CrmConfigAction<TemplateFolder> {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_TEMPLATE_COUNT = 0;

    private final TemplateFolderConverter templateFolderConverter;
    private final TemplateFolderManager templateFolderManager;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final UserService userService;
    private final CrmUserManager crmUserManager;
    private final TeamManager teamManager;
    private final TerritoryManager territoryManager;
    private final TemplateFolderService templateFolderService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;

    public TemplateFolderManagement() {
        templateFolderConverter = TemplateFolderConverter.getInstance();
        templateFolderManager = TemplateFolderManager.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        userService = UserService.getInstance();
        teamManager = TeamManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        templateFolderService = TemplateFolderService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
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
        final Long userId = request.userId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(TemplateFolderFields.TYPE.getName());

        switch (type) {
            case "list" -> getFolders(orgId, userId, request, response);
            case "settings" -> {
                final TemplateFolder templateFolder = convert(request, requestJson.getJsonObject(TemplateFolderFields.DATA.getName()));

                modifyTemplateFolderVisibility(orgId, userId, response, templateFolder);
            }
            case "getSettings" -> {
                final JsonObject jsonObject = requestJson.getJsonObject(TemplateFolderFields.DATA.getName());
                final Long folderId = jsonObject.getLong(TemplateFolderFields.ID.getName());

                response.put(TemplateFolderFields.VISIBLE_TO.getName(),
                        fetchAndDispatchTemplateFolderVisibility(orgId, userId, folderId, response));
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Handles the received request
     * @param crmResponse Generates the CRM response
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    public void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final long orgId = crmRequest.orgId();
        final Long userId = crmRequest.userId();

        switch (type) {
            case "getEmailTemplates" -> crmResponse.put(TemplateFolderFields.TEMPLATES.getName(), getTemplates(orgId, userId, data, crmResponse));
            case "getEmailTemplatesCount" -> crmResponse.put(TemplateFolderFields.TEMPLATES.getName(), getTemplatesCount(orgId, userId));
            default -> authorizedProcess(crmRequest, crmResponse);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link TemplateFolderManager}
     */
    @Override
    public ConfigManager<TemplateFolder> getConfigManager() {
        return templateFolderManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The received request
     * @param jsonObject An object that holds request data
     * @return The {@link TemplateFolder}
     */
    @Override
    public TemplateFolder convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return templateFolderConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The received request
     * @param jsonObject An object that holds request data
     * @return The {@link TemplateFolder}
     */
    @Override
    public TemplateFolder convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return templateFolderConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param templateFolder Represents the template folder attributes
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convert(final TemplateFolder templateFolder) {
        return templateFolderConverter.convert(templateFolder);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id.
     * @param action          Represents the {@link Action} for given details
     * @param templateFolders Represent the {@link Collection} of {@link TemplateFolder}
     * @return True if all the details are valid, otherwise returns false
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action,
                                           final Collection<TemplateFolder> templateFolders) {
        if (Objects.nonNull(templateFolders) && !templateFolders.isEmpty()) {

            switch (action) {
                case CREATE, UPDATE -> {
                    final Collection<String> folderNames = new HashSet<>();

                    for (final TemplateFolder templateFolder : templateFolders) {
                        templateFolderService.populateFolderUsers(orgId, templateFolder);
                        folderNames.add(templateFolder.getName());
                    }

                    if (templateFolderService.checkTemplateFoldersAreExists(orgId, folderNames)) {
                        throw ErrorCode.get(TemplateFolderErrorCodes.TEMPLATE_FOLDER_NAME_ALREADY_EXISTS,
                                "i18n.template.folder.name.already.exists");
                    }
                }
                case DELETE -> {
                    final Collection<Long> folderIds = templateFolders.stream().map(TemplateFolder::id).collect(Collectors.toSet());

                    if (!templateFolderService.exists(orgId, folderIds)) {
                        throw ErrorCode.get(TemplateFolderErrorCodes.TEMPLATES_EXISTS_IN_FOLDER,
                                "i18n.template.exists.in.folder");
                    }
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Fetches the template folders using filter
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param request     Represents the {@link CrmRequest} from request data
     * @param crmResponse Represents the {@link CrmResponse} data
     */
    private void getFolders(final long orgId, final long userId, final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject jsonObject = requestJson.getJsonObject(TemplateFolderFields.DATA.getName());
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);
        final FilterAction filterAction = FilterAction.getFilterAction(jsonObject.getShort(TemplateFields.FILTER_TYPE.getName()));
        final String searchText = jsonObject.optString(TemplateFields.SEARCH_TEXT.getName(), null);
        final int start = Math.max(jsonObject.optInt(TemplateFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(jsonObject.optInt(TemplateFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final JsonArray jsonArray = Json.array();
        final long totalRecords = templateFolderService.getUserAccessibleFolderCount(orgId, userId, filterAction, searchText);

        if (totalRecords > 0) {
            final Collection<TemplateFolder> userAccessibleFolders = templateFolderService.fetchUserAccessibleFolders(
                    orgId, userId, filterAction, searchText, start, limit);
            final Map<Long, Long> sharedFolderOwners = new HashMap<>();
            final Collection<TemplateFolder> ownedFolders = new HashSet<>();

            for (final TemplateFolder templateFolder : userAccessibleFolders) {
                final Visibility visibility = templateFolder.getVisibility();

                switch (visibility) {
                    case EVERYONE, DESIGNATED -> {
                        final Long ownerId = templateFolder.getOwnerId();

                        if (Objects.equals(userId, ownerId)) {
                            ownedFolders.add(templateFolder);
                        } else {
                            sharedFolderOwners.put(templateFolder.id(), ownerId);
                        }
                    }
                    case JUST_ME -> ownedFolders.add(templateFolder);
                }
            }

            if (!ownedFolders.isEmpty()) {

                for (final TemplateFolder templateFolder : ownedFolders) {
                    jsonArray.put(templateFolderConverter.convert(converterConfig, templateFolder));
                }
            }

            if (!sharedFolderOwners.isEmpty()) {
                fetchAndDispatchTemplateFolders(orgId, converterConfig, sharedFolderOwners, jsonArray, userAccessibleFolders,
                        start, limit, totalRecords, crmResponse);
            }
        }
        crmResponse.put(TemplateFolderFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
        crmResponse.put(TemplateFolderFields.TOTAL_RECORDS.getName(), totalRecords);
        crmResponse.put(TemplateFields.LIST.getName(), getSortedFolderList(jsonArray));
    }

    /**
     * <p>
     * Modifies the visibility of a template folder based on the user's folder visibility
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param crmResponse    Represents the {@link CrmResponse}
     * @param templateFolder Represents the {@link TemplateFolder}
     */
    private void modifyTemplateFolderVisibility(final long orgId, final long userId, final CrmResponse crmResponse,
                                                final TemplateFolder templateFolder) {
        final Long folderId = templateFolder.id();

        if (Objects.nonNull(folderId) && templateFolderService.exists(orgId, folderId)) {
            final TemplateFolder existingFolder = templateFolderService.get(orgId, folderId);

            if (validateUserAccessToTemplateFolder(orgId, userId, existingFolder, templateFolder)) {
                final String updatedRecordKey = TemplateFolderFields.UPDATED_RECORDS.getName();

                if (!Objects.equals(userId, existingFolder.getOwnerId())) {
                    templateFolder.setAccessType(existingFolder.getAccessType());
                    templateFolder.setVisibility(existingFolder.getVisibility());
                }

                if (Visibility.JUST_ME == templateFolder.getVisibility()) {
                    templateFolder.setAccessType(AccessType.CAN_EDIT);
                    templateFolder.setPreventShare(false);
                }

                crmResponse.put(updatedRecordKey, templateFolderService.updateFolderVisibility(orgId, templateFolder)
                        .get(updatedRecordKey));
            }
        } else {
            throw ErrorCode.get(TemplateFolderErrorCodes.TEMPLATE_FOLDER_NOT_FOUND,
                    "i18n.template.folder.not.found");
        }
    }

    /**
     * <p>
     * Validates whether the specified user has the necessary access rights to modify a template folder
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param existingFolder Represents the {@link TemplateFolder} holding the existing folder details
     * @param templateFolder Represents the {@link TemplateFolder} holding the current folder details for the user
     * @return the true if user is valid
     */
    private boolean validateUserAccessToTemplateFolder(final long orgId, final long userId,
                                                       final TemplateFolder existingFolder,
                                                       final TemplateFolder templateFolder) {
        final Visibility visibility = existingFolder.getVisibility();
        final boolean isFolderOwnedByUser = Objects.equals(userId, existingFolder.getOwnerId());

        if (Objects.isNull(templateFolder.getVisibility())) {
            throw ErrorCode.get(TemplateFolderErrorCodes.VISIBILITY_TYPE_SHOULD_NOT_BE_NULL,
                    "i18n.template.folder.invalid.visibility.type");
        }

        if (!isFolderOwnedByUser && existingFolder.preventShare()) {
            throw ErrorCode.get(TemplateFolderErrorCodes.PERMISSION_DENIED,
                    "i18n.template.folder.user.permission.denied");
        }

        if (Visibility.DESIGNATED == visibility) {
            final Collection<CrmUser> crmUsers = templateFolder.getCrmUsers();
            final Collection<Team> teams = templateFolder.getTeams();
            final Collection<Territory> territories = templateFolder.getTerritories();
            final Collection<Long> associatedUserIds = new HashSet<>();

            associatedUserIds.add(userId);

            if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
                final Collection<Long> userIds = crmUsers.stream().map(CrmUser::id).collect(Collectors.toUnmodifiableSet());

                if (!crmUserManager.exists(orgId, userIds)) {
                    throw ErrorCode.get(TemplateFolderErrorCodes.USERS_NOT_FOUND,
                            "i18n.template.users.not.found");
                }
            }

            if (Objects.nonNull(teams) && !teams.isEmpty()) {
                final Collection<Long> teamIds = teams.stream().map(Team::id).collect(Collectors.toUnmodifiableSet());

                if (!teamManager.exists(orgId, teamIds)) {
                    throw ErrorCode.get(TemplateFolderErrorCodes.TEAMS_NOT_FOUND,
                            "i18n.template.teams.not.found");
                }
            }

            if (Objects.nonNull(territories) && !territories.isEmpty()) {
                final Collection<Long> territoryIds = territories.stream().map(Territory::id)
                        .collect(Collectors.toUnmodifiableSet());

                if (!territoryManager.exists(orgId, territoryIds)) {
                    throw ErrorCode.get(TemplateFolderErrorCodes.TERRITORIES_NOT_FOUND,
                            "i18n.template.territories.not.found");
                }
            }

            if (!templateFolderService.retrieveAssociatedUserIdsFromSharedFolder(existingFolder, associatedUserIds)
                    .contains(userId)) {
                throw ErrorCode.get(TemplateFolderErrorCodes.PERMISSION_DENIED,
                        "i18n.template.folder.user.permission.denied");
            }
        }

        return true;
    }

    /**
     * <p>
     * Fetches template folder visibility information based on organization and folder IDs, processes the data,
     * and dispatches the result through a provided CrmResponse object.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param folderId    Represents the template folder id
     * @param crmResponse Represents the {@link CrmResponse}
     * @return the {@link JsonArray} that containing the visibility information
     */
    private JsonArray fetchAndDispatchTemplateFolderVisibility(final Long orgId, final long userId, final Long folderId,
                                                               final CrmResponse crmResponse) {
        if (templateFolderService.exists(orgId, folderId)) {
            final TemplateFolder templateFolder = templateFolderService.get(orgId, folderId);

            if (Objects.isNull(templateFolder)) {
                throw ErrorCode.get(TemplateFolderErrorCodes.TEMPLATE_FOLDER_NOT_FOUND,
                        "i18n.template.folder.not.found");
            }
            final JsonArray jsonArray = Json.array();
            final Visibility visibility = templateFolder.getVisibility();

            switch (visibility) {
                case JUST_ME, EVERYONE -> jsonArray.put(templateFolderConverter.convertPartialData(templateFolder));
                case DESIGNATED -> {
                    templateFolderService.populateFolderUsers(orgId, templateFolder);
                    final Collection<CrmUser> crmUsers = templateFolder.getCrmUsers();

                    if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
                        final Long iamOrgId = organizationService.getIamOrgId(orgId);
                        final Collection<Long> iamUserIds = crmUsers.stream().map(CrmUser::getIamId)
                                .collect(Collectors.toUnmodifiableSet());

                        crmResponse.convertIntoAsync();
                        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                            @Override
                            public void onSuccess(final Collection<User> users) {
                                final Map<Long, User> crmUserMap = users.stream().collect(Collectors.toMap(User::id,
                                        Function.identity()));

                                for (final CrmUser crmUser : crmUsers) {
                                    crmUser.setUser(crmUserMap.get(crmUser.getIamId()));
                                }
                                templateFolder.setCrmUsers(crmUsers);
                                jsonArray.put(templateFolderConverter.convertPartialData(userId, templateFolder));
                                crmResponse.put(TemplateFolderFields.VISIBLE_TO.getName(), jsonArray);
                                crmResponse.send();
                            }

                            @Override
                            public void onFailure(final Throwable throwable) {
                                sendError(crmResponse, null, ErrorCode.get(TemplateFolderErrorCodes.USERS_NOT_FOUND,
                                        "i18n.template.folder.user.not.found"));
                            }
                        };
                        userService.fetchUsers(iamOrgId, iamUserIds, usersFetchCallBack);
                    } else {
                        jsonArray.put(templateFolderConverter.convertPartialData(userId, templateFolder));
                    }
                }
                default -> throw ErrorCode.get(TemplateFolderErrorCodes.INVALID_VISIBILITY_TYPE,
                        "i18n.template.folder.invalid.visibility.type");
            }

            return jsonArray;
        } else {
            throw ErrorCode.get(TemplateFolderErrorCodes.TEMPLATE_FOLDER_NOT_FOUND,
                    "i18n.template.folder.not.found");
        }
    }

    /**
     * <p>
     * Fetches and dispatches the template folders information based on organization and user accessible folders
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param converterConfig    Represents the {@link ConverterConfig}
     * @param sharedFolderOwners Represents the {@link Map} contains key as folder id and user id as value
     * @param jsonArray          Which the holds the template details
     * @param templateFolders    Which holds the {@link Collection} of {@link TemplateFolder}
     * @param start              Represents the starting index of {@link TemplateFolder} to be fetched
     * @param limit              Represents the maximum number of {@link TemplateFolder} to be fetched
     * @param totalRecords       Represents the total count of all matching folders (from CountQuery, not page size)
     * @param crmResponse        Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchTemplateFolders(final long orgId, final ConverterConfig converterConfig, final Map<Long, Long> sharedFolderOwners,
                                                 final JsonArray jsonArray, final Collection<TemplateFolder> templateFolders,
                                                 final int start, final int limit, final long totalRecords, final CrmResponse crmResponse) {
        final Collection<CrmUser> crmUsers = crmUserService.getUsers(orgId, sharedFolderOwners.values());

        if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
            final Long iamOrgId = organizationService.getIamOrgId(orgId);
            final Collection<Long> iamUserIds = crmUsers.stream().map(CrmUser::getIamId)
                    .collect(Collectors.toUnmodifiableSet());

            crmResponse.convertIntoAsync();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> crmUserMap = users.stream().collect(Collectors.toMap(User::id,
                            Function.identity()));

                    for (final TemplateFolder templateFolder : templateFolders) {

                        if (sharedFolderOwners.containsKey(templateFolder.id())) {
                            final CrmUser crmUser = crmUserService.getUser(orgId, templateFolder.getOwnerId());

                            crmUser.setUser(crmUserMap.get(crmUser.getIamId()));
                            templateFolder.setOwner(crmUser);
                            jsonArray.put(templateFolderConverter.convert(converterConfig, templateFolder));
                        }
                    }

                    crmResponse.put(TemplateFields.LIST.getName(), getSortedFolderList(jsonArray));
                    crmResponse.put(TemplateFolderFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
                    crmResponse.put(TemplateFolderFields.TOTAL_RECORDS.getName(), totalRecords);
                    crmResponse.send();
                }

                @Override
                public void onFailure(final Throwable cause) {
                    sendError(crmResponse, null, ErrorCode.get(TemplateFolderErrorCodes.USERS_NOT_FOUND, "i18n.template.folder.user.not.found"));
                }
            };

            userService.fetchUsers(iamOrgId, iamUserIds, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Gets the sorted folder {@link JsonArray}.
     * </p>
     *
     * @param jsonArray Represents the {@link JsonArray}
     * @return {@link JsonArray}
     */
    private JsonArray getSortedFolderList(final JsonArray jsonArray) {
        final Collection<JsonObject> jsonObjects = new ArrayList<>();
        final String createdTimeKey = TemplateFolderFields.CREATED_TIME.getName();

        for (int index = 0; index < jsonArray.size(); index++) {
            jsonObjects.add(jsonArray.getJsonObject(index));
        }
        final Collection<JsonObject> sortedJsonObjects = jsonObjects.stream().sorted((Comparator.comparing(json ->
                json.getLong(createdTimeKey)))).toList();
        final JsonArray array = Json.array();

        sortedJsonObjects.forEach(array::put);

        return array;
    }

    /**
     * <p>
     * Retrieves the template counts based on visibility for a specific user within their organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the {@link JsonArray}
     */
    private JsonArray getTemplatesCount(final long orgId, final long userId) {
        final JsonArray jsonArray = Json.array();
        final Map<FilterAction, Integer> templatesCount = new HashMap<>(templateFolderManager.getTemplatesCount(orgId, userId));

        Arrays.stream(FilterAction.values()).forEach(action -> templatesCount.put(action, templatesCount.isEmpty()
                ? DEFAULT_TEMPLATE_COUNT : templatesCount.getOrDefault(action, DEFAULT_TEMPLATE_COUNT)));

        templatesCount.forEach((filterAction, count) -> {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(TemplateFolderFields.TEMPLATES_COUNT.getName(), count);
            jsonObject.put(TemplateFolderFields.IS_DEFAULT.getName(), Objects.equals(FilterAction.ALL, filterAction));
            jsonObject.put(TemplateFolderFields.NAME.getName(), filterAction.getFilterActionType());

            jsonArray.put(jsonObject);
        });

        return jsonArray;
    }

    /**
     * <p>
     * Fetches the template using the specified filter, organization id, and user id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param jsonObject  Represents the request data
     * @param crmResponse Represents the {@link CrmResponse}
     * @return the {@link JsonArray}
     */
    private JsonArray getTemplates(final long orgId, final long userId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final FilterAction filterAction = FilterAction.getFilterAction(jsonObject.getShort(TemplateFields.FILTER_TYPE.getName()));
        final String searchText = jsonObject.optString(TemplateFields.SEARCH_TEXT.getName(), null);
        final int start = Math.max(jsonObject.optInt(TemplateFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(jsonObject.optInt(TemplateFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final JsonArray jsonArray = Json.array();
        final long totalRecords = templateFolderManager.getTemplatesCount(orgId, userId, searchText, filterAction);

        if (totalRecords > 0) {
            final Collection<EntityMailTemplate> templates = templateFolderService.getTemplates(orgId, userId, start, limit, filterAction, searchText);

            for (final EntityMailTemplate template : templates) {
                final JsonObject templateJson = Json.object();

                templateJson.put(TemplateFolderFields.NAME.getName(), template.getName());
                templateJson.put(TemplateFolderFields.ID.getName(), template.id());

                jsonArray.put(templateJson);
            }
        }

        crmResponse.put(TemplateFolderFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
        crmResponse.put(TemplateFolderFields.TOTAL_RECORDS.getName(), totalRecords);

        return jsonArray;
    }
}