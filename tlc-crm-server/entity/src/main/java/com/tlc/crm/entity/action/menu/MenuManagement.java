package com.tlc.crm.entity.action.menu;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.MenuFields;
import com.tlc.crm.entity.internal.status.SettingsErrorCodes;
import com.tlc.crm.entity.manager.menu.MenuManager;
import com.tlc.crm.entity.models.menu.Menu;
import com.tlc.crm.entity.models.type.converter.IconConverter;
import com.tlc.crm.entity.models.menu.HomeMenuMetrics;

import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Handles the menu settings related web actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@WebAction(path = "/menu/mgmt")
public class MenuManagement extends AuthorizationController {

    private static final int DEFAULT_MENU_LIMIT = 4;

    private final MenuManager menuManager;
    private final IconConverter iconConverter;
    private final SourceSettingsService sourceSettingsService;
    private final ConverterConfigBuilder converterConfigBuilder;

    public MenuManagement() {
        this.menuManager = MenuManager.getInstance();
        this.iconConverter = IconConverter.getInstance();
        this.sourceSettingsService = SourceSettingsService.getInstance();
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @throws Exception if an error occurs during processing
     */
    @Override
    public void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(MenuFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(MenuFields.DATA);
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();

        switch (type) {
            case "updateOrder" -> updateOrder(orgId, userId, data, crmResponse);
            case "list" -> crmResponse.put(MenuFields.LIST, convertIntoJsonArray(menuManager.getMenuList(orgId, userId), crmRequest, crmResponse));
            case "getConfig" -> crmResponse.put(MenuFields.CONFIG, getMenuConfig(orgId, userId, data, crmRequest));
            case "updateConfig" -> updateConfig(orgId, userId, data, crmResponse);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    public void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        authorizedProcess(crmRequest, crmResponse);
    }

    /**
     * <p>
     * Converts the {@link Collection} of {@link Menu} into {@link JsonArray}
     * </p>
     *
     * @param menuList    Represents the {@link Collection} of {@link Menu}
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @return the {@link JsonArray}
     */
    private JsonArray convertIntoJsonArray(final Collection<Menu> menuList, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();
        final Counter counter = new Counter();
        final ConverterConfig converterConfig = converterConfigBuilder.build(crmRequest, false, null);
        final String isIncludedInMoreModulesKey = MenuFields.IS_INCLUDED_IN_MORE_MODULES;
        final String idKey = MenuFields.ID;
        final String menuKey = MenuFields.MENU;
        final String iconKey = MenuFields.ICON;
        final String displayNameKey = MenuFields.DISPLAY_NAME;

        for (final Menu menu : menuList) {
            final Long sourceId = menu.getSourceId();
            final JsonObject jsonObject = Json.object()
                    .put(isIncludedInMoreModulesKey, counter.incrementAndGet() > DEFAULT_MENU_LIMIT)
                    .put(idKey, menu.getId())
                    .put(menuKey, menu.getName())
                    .put(iconKey, iconConverter.convert(converterConfig, menu.getIcon()))
                    .put(displayNameKey, crmResponse.resolveI18nString(menu.getDisplayName()))
                    .put(MenuFields.IMMOVABLE, Objects.equals(MenuFields.HOME, menu.getName()));

            if (Objects.nonNull(sourceId)) {
                final SourceSettings sourceSettings = sourceSettingsService.getSourceSettings(menu.getOrgId(), sourceId);
                final String alternateName = sourceSettings.getAlternateName();

                if (sourceSettings.isEnabled()) {
                    if (Objects.nonNull(alternateName)) {
                        jsonObject.put(displayNameKey, alternateName);
                    }
                } else {
                    continue;
                }
            }
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Updates the order of menu based on menu ids provided in the JSON object
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param jsonObject  The JSON object containing the menu IDs representing the new order
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updateOrder(final long orgId, final long userId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final Set<Long> menuIds = new LinkedHashSet<>();

        jsonObject.getJsonArray(MenuFields.IDS).forEach(menuId -> menuIds.add((Long) menuId));

        final Optional<Long> optionalMenuId = menuManager.getMenuIdByName(MenuFields.HOME);

        optionalMenuId.ifPresent(menuId -> {
            if (menuIds.contains(menuId)) {
                throw ErrorCode.get(SettingsErrorCodes.IMMOVABLE_MENU_ORDER_CANNOT_BE_CHANGED, "i18n.entity.menu.order.immutable");
            }
        });

        if (menuManager.exists(orgId, userId, menuIds)) {
            menuManager.updateOrder(orgId, userId, menuIds);
        } else {
            throw ErrorCode.get(SettingsErrorCodes.MENU_NOT_FOUND, "i18n.entity.menu.not.found");
        }

        crmResponse.put(MenuFields.MESSAGE, crmResponse.resolveI18nString("i18n.entity.menu.order.update.success"));
    }

    /**
     * Gets the config JSON for the given menu.
     *
     * @param orgId   Refers the organization ID.
     * @param userId  Refers the user ID.
     * @param data    The JSON object containing the menu IDs representing the new order.
     * @param request Refers the {@link CrmRequest}.
     */
    private JsonObject getMenuConfig(final long orgId, final long userId, final JsonObject data, final CrmRequest request) {
        final Menu menu = menuManager.getMenu(orgId, userId, data.getLong(MenuFields.MENU_ID));
        final JsonObject jsonObject = Json.object();
        final JsonArray array = Json.array();
        final Collection<Integer> metricsIds = menu.getConfig().getMetricsIds();

        if (Objects.nonNull(metricsIds)) {
            final String name = menu.getName();
            final I18NCRMResolutionAdapter i18nResolver = request.getI18NCRMResolutionAdapter();

            switch (name) {
                case MenuFields.HOME -> {
                    for (final HomeMenuMetrics homeMenuMetrics : HomeMenuMetrics.values()) {
                        final JsonObject metricJson = Json.object();
                        final int id = homeMenuMetrics.getId();

                        metricJson.put(MenuFields.NAME, i18nResolver.get(homeMenuMetrics.getDisplayName()));
                        metricJson.put(MenuFields.ID, id);
                        metricJson.put(MenuFields.SOURCE, homeMenuMetrics.getSource().getName());
                        metricJson.put(MenuFields.IS_ENABLED, metricsIds.contains(NumberUtils.parseNumber(id).intValue()));
                        array.put(metricJson);
                    }
                }
            }
            jsonObject.put(MenuFields.METRICS, array);
            return jsonObject;
        }
        throw ErrorCode.get(SettingsErrorCodes.MENU_CONFIG_NOT_FOUND, "i18n.entity.menu.config.not.found");
    }

    /**
     * Updates the config of menu based on menu ID provided in the JSON object.
     *
     * @param orgId       Refers an organization ID.
     * @param userId      Refers the user ID.
     * @param data        The JSON object containing the menu ID.
     * @param crmResponse Refers the {@link CrmResponse} to be sent.
     */
    private void updateConfig(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Long menuId = data.getLong(MenuFields.MENU_ID);
        final Collection<Integer> metricsIds = new LinkedHashSet<>();

        data.getJsonArray(MenuFields.METRICS).forEach(metricId -> metricsIds.add(NumberUtils.parseNumber(metricId).intValue()));
        final Menu menu = menuManager.getMenu(orgId, userId, menuId);

        switch (menu.getName()) {
            case MenuFields.HOME -> {
                if (HomeMenuMetrics.getIds().containsAll(metricsIds)) {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(MenuFields.METRICS, metricsIds);
                    menu.getConfig().setMetrics(jsonObject.toString());
                    menuManager.updateConfig(orgId, userId, menu);
                    crmResponse.put(MenuFields.MESSAGE, crmResponse.resolveI18nString("i18n.entity.menu.config.update.success"));
                } else {
                    throw ErrorCode.get(SettingsErrorCodes.MENU_CONFIG_NOT_FOUND, "i18n.entity.menu.config.not.found");
                }
            }
        }
    }
}