package com.tlc.crm.entity.action.secure.settings;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.type.converter.IconConverter;

import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.icon.model.Icon;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Manages {@link Icon} related web actions
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see AuthorizationController
 */
@WebAction(path = "/icon/list")
public class IconAction extends AuthorizationController {

    private final IconService iconService;
    private final IconConverter iconConverter;

    public IconAction() {
        this.iconService = IconService.getInstance();
        this.iconConverter = IconConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the request data
     * @param crmResponse Represents the response data
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final String iconType = data.getString("name");

        if (FieldKeys.LIST.getName().equals(type)) {
            crmResponse.put(FieldKeys.ICON_LIST.getName(), convertModelsToJsonArray(iconType, request));
        }
    }

    /**
     * Gets the list of icons based on the entity type
     *
     * @param type Represents the type of icon
     * @param request Represents the crm request data
     * @return {@link JsonArray} of icon
     */
    private JsonArray convertModelsToJsonArray(final String type, final CrmRequest request) {
        final JsonArray jsonArray = Json.array();

        if (Objects.nonNull(type)) {
            final Collection<Icon> icons = iconService.getIconsByType(Icon.Type.getIconType(type));

            icons.forEach(icon -> jsonArray.put(iconConverter.convert(ConverterConfigBuilder.getInstance().build(request, false, null), icon)));
        }
        return jsonArray;
    }
}