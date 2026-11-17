package com.tlc.crm.core.notification.email.template.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.converter.TeamConverter;
import com.tlc.crm.common.iam.api.model.converter.TerritoryConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.notification.email.template.service.TemplateFolderService;
import com.tlc.crm.core.notification.email.template.fields.TemplateFields;
import com.tlc.crm.core.notification.email.template.fields.TemplateFolderFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder;
import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder.Visibility;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link TemplateFolder}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TemplateFolderConverter extends EntityConverter<TemplateFolder> {

    private final TemplateFolderService templateFolderService;
    private final TemplateConverter templateConverter;
    private final CrmUserConverter crmUserConverter;
    private final TeamConverter teamConverter;
    private final TerritoryConverter territoryConverter;

    public TemplateFolderConverter() {
        templateFolderService = TemplateFolderService.getInstance();
        templateConverter = TemplateConverter.getInstance();
        this.crmUserConverter = CrmUserConverter.getInstance();
        this.teamConverter = TeamConverter.getInstance();
        this.territoryConverter = TerritoryConverter.getInstance();

    }

    public static class Instance {
        private static final TemplateFolderConverter INSTANCE = new TemplateFolderConverter();
    }

    /**
     * Gets the {@link TemplateFolderConverter} Instance
     *
     * @return {@link TemplateFolderConverter}
     */
    public static TemplateFolderConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Represents the request data
     * @return The {@link TemplateFolder}
     */
    @Override
    public TemplateFolder convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? buildFolderByPartialData(converterConfig, jsonObject) :
                buildFolder(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Converts {@link TemplateFolder} into {@link JsonObject}
     * </p>
     *
     * @param templateFolder Represents the {@link TemplateFolder} attributes
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final TemplateFolder templateFolder) {
        return convertFolderToJson(converterConfig, templateFolder);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param templateFolder Represents the entity model
     * @return template folder {@link JsonObject}
     */
    @Override
    public JsonObject convert(final TemplateFolder templateFolder) {
        return convertFolderToJson(null, templateFolder);
    }

    /**
     * <p>
     * Converts the partial {@link TemplateFolder} attributes into {@link JsonObject}
     * </p>
     *
     * @param templateFolder Represents the {@link TemplateFolder} attributes
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convertPartialData(final TemplateFolder templateFolder) {
        return Json.object()
                .put(TemplateFolderFields.NAME.getName(), templateFolder.getName())
                .put(TemplateFolderFields.PREVENT_SHARE.getName(), templateFolder.preventShare())
                .put(TemplateFolderFields.ACCESS_TYPE.getName(), templateFolder.getAccessType().getId())
                .put(TemplateFolderFields.VISIBLE_TO.getName(), templateFolder.getVisibility().getId());
    }

    /**
     * <p>
     * Converts the partial {@link TemplateFolder} attributes into {@link JsonObject}
     * </p>
     *
     * @param userId         Represents a user ID
     * @param templateFolder Represents the {@link TemplateFolder} attributes
     * @return The {@link JsonObject}
     */
    public JsonObject convertPartialData(final long userId, final TemplateFolder templateFolder) {
        final JsonObject jsonObject = convertPartialData(templateFolder);
        final Collection<CrmUser> users = templateFolder.getCrmUsers();
        final Collection<Team> teams = templateFolder.getTeams();
        final Collection<Territory> territories = templateFolder.getTerritories();

        if (Objects.nonNull(users) && !users.isEmpty()) {
            jsonObject.put(TemplateFolderFields.USERS.getName(),
                    crmUserConverter.convertUsersIntoJson(userId, users));
        }

        if (Objects.nonNull(teams) && !teams.isEmpty()) {
            jsonObject.put(TemplateFolderFields.TEAMS.getName(),
                    teamConverter.convertTeamsIntoJson(teams));
        }

        if (Objects.nonNull(territories) && !territories.isEmpty()) {
            jsonObject.put(TemplateFolderFields.TERRITORIES.getName(),
                    territoryConverter.convertTerritoriesIntoJson(territories));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts json attributes into {@link TemplateFolder}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Represents the request data
     * @return The {@link TemplateFolder}
     */
    private TemplateFolder buildFolder(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final TemplateFolder templateFolder = new TemplateFolder();
        final String idKey = TemplateFolderFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            templateFolder.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, templateFolder, jsonObject);

        return templateFolder;
    }

    /**
     * <p>
     * Sets JSON data into {@link TemplateFolder}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param templateFolder  Represents the {@link TemplateFolder} attributes
     * @param jsonObject      Represents the request data
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final TemplateFolder templateFolder,
                                    final JsonObject jsonObject) {
        final String nameKey = TemplateFolderFields.NAME.getName();
        final String accessTypeKey = TemplateFolderFields.ACCESS_TYPE.getName();
        final String visibleToKey = TemplateFolderFields.VISIBLE_TO.getName();
        final Long orgId = converterConfig.orgId();

        templateFolder.setOrgId(orgId);
        templateFolder.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey) : templateFolder.getName());
        templateFolder.setOwnerId(converterConfig.ownerId());
        templateFolder.setPreventShare(jsonObject.optBoolean(TemplateFolderFields.PREVENT_SHARE.getName(), true));

        if (jsonObject.containsKey(visibleToKey)) {
            templateFolder.setVisibility(Visibility.get(jsonObject.getShort(visibleToKey)));
        }

        if (jsonObject.containsKey(TemplateFolderFields.USERS.getName())) {
            setUserJsonToModel(templateFolder, jsonObject);
        }

        if (jsonObject.containsKey(TemplateFolderFields.TEAMS.getName())) {
            setTeamJsonToModel(templateFolder, jsonObject);
        }

        if (jsonObject.containsKey(TemplateFolderFields.TERRITORIES.getName())) {
            setTerritoryJsonToModel(templateFolder, jsonObject);
        }

        if (jsonObject.containsKey(accessTypeKey)) {
            templateFolder.setAccessType(TemplateFolder.AccessType.getAccessTypeById(jsonObject.getShort(accessTypeKey)));
        }
    }

    /**
     * <p>
     * Sets the crm user json to model
     * </p>
     *
     * @param templateFolder Represents the {@link TemplateFolder} attributes
     * @param usersJson      Holds the users {@link JsonArray}
     */
    private void setUserJsonToModel(final TemplateFolder templateFolder, final JsonObject usersJson) {
        final JsonArray jsonArray = usersJson.getJsonArray(TemplateFolderFields.USERS.getName());
        final Collection<CrmUser> crmUsers = new HashSet<>();
        final UserService userService = UserService.getInstance();
        final long orgId = templateFolder.orgId();

        for (int userIndex = 0; userIndex < jsonArray.size(); userIndex++) {
            final CrmUser crmUser = userService.getUser(orgId, jsonArray.getLong(userIndex));

            crmUsers.add(crmUser);
        }
        templateFolder.setCrmUsers(crmUsers);
    }

    /**
     * <p>
     * Sets the team json to model
     * </p>
     *
     * @param templateFolder Represents the {@link TemplateFolder} attributes
     * @param teamJson       Holds the teams {@link JsonArray}
     */
    private void setTeamJsonToModel(final TemplateFolder templateFolder, final JsonObject teamJson) {
        final JsonArray jsonArray = teamJson.getJsonArray(TemplateFolderFields.TEAMS.getName());
        final Collection<Team> teams = new HashSet<>();

        for (int teamIndex = 0; teamIndex < jsonArray.size(); teamIndex++) {
            final Team team = new Team();

            team.setId(jsonArray.getLong(teamIndex));
            teams.add(team);
        }
        templateFolder.setTeams(teams);
    }

    /**
     * <p>
     * Sets the territory json to model
     * </p>
     *
     * @param templateFolder Represents the {@link TemplateFolder} attributes
     * @param territoryJson  Holds the territory {@link JsonArray}
     */
    private void setTerritoryJsonToModel(final TemplateFolder templateFolder, final JsonObject territoryJson) {
        final JsonArray jsonArray = territoryJson.getJsonArray(TemplateFolderFields.TERRITORIES.getName());
        final Collection<Territory> territories = new HashSet<>();

        for (int territoryIndex = 0; territoryIndex < jsonArray.size(); territoryIndex++) {
            final Territory territory = new Territory();

            territory.setId(jsonArray.getLong(territoryIndex));
            territories.add(territory);
        }
        templateFolder.setTerritories(territories);
    }

    /**
     * <p>
     * Converts partial data into {@link TemplateFolder}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Holds the {@link TemplateFolder} attributes
     * @return The {@link TemplateFolder}
     */
    private TemplateFolder buildFolderByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final TemplateFolder templateFolder = templateFolderService.get(converterConfig.orgId(), jsonObject.getLong(TemplateFields.ID.getName()));

        setJsonDataToModel(converterConfig, templateFolder, jsonObject);

        return templateFolder;
    }

    /**
     * <p>
     * Converts the {@link TemplateFolder} to {@link JsonObject}.
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param templateFolder  Represents the {@link TemplateFolder}
     * @return
     */
    private JsonObject convertFolderToJson(final ConverterConfig converterConfig, final TemplateFolder templateFolder) {
        final JsonObject jsonObject = Json.object();
        final JsonArray jsonArray = Json.array();
        final Collection<EntityMailTemplate> templates = templateFolder.getTemplates();
        final CrmUser owner = templateFolder.getOwner();

        jsonObject.put(TemplateFolderFields.NAME.getName(), templateFolder.getName());
        jsonObject.put(TemplateFolderFields.ID.getName(), templateFolder.id());
        jsonObject.put(TemplateFolderFields.CREATED_TIME.getName(), templateFolder.getCreatedTime());
        jsonObject.put(TemplateFolderFields.IS_VISIBILITY_EDITABLE.getName(), true);
        jsonObject.put(TemplateFolderFields.IS_EDITABLE.getName(), true);

        if (Objects.nonNull(templates) && !templates.isEmpty()) {

            if (Objects.isNull(converterConfig)) {
                templates.forEach(template -> jsonArray.put(templateConverter.convert(template)));
            } else {
                templates.forEach(template -> jsonArray.put(templateConverter.convert(converterConfig, template)));
            }
            jsonObject.put(TemplateFolderFields.TEMPLATES.getName(), jsonArray);
        }

        if (Objects.nonNull(owner)) {
            jsonObject.put(TemplateFolderFields.OWNER.getName(), crmUserConverter.convert(owner, true, false));
            jsonObject.put(TemplateFolderFields.HAS_OWNER_ICON.getName(), true);
            jsonObject.put(TemplateFolderFields.IS_VISIBILITY_EDITABLE.getName(), !templateFolder.preventShare());
            jsonObject.put(TemplateFolderFields.IS_EDITABLE.getName(), TemplateFolder.AccessType.CAN_EDIT.equals(templateFolder.getAccessType()));
        }

        return jsonObject;
    }
}
