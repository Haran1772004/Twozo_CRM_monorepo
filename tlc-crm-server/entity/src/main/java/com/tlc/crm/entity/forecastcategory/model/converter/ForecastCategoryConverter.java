package com.tlc.crm.entity.forecastcategory.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.forecastcategory.fields.ForecastCategoryFields;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Objects;

/***
 * Converter class for transforming between {@link ForecastCategory} domain models and their JSON representations
 *
 * @author kishore J
 * @version 1.0
 */
public class ForecastCategoryConverter extends CrmModelConverter<ForecastCategory, JsonObject> {

    private final ForecastCategoryManager forecastCategoryManager;
    private final UserService userService;
    private final I18nService i18nService;

    private ForecastCategoryConverter() {
        i18nService = I18nServiceProvider.get();
        forecastCategoryManager = ForecastCategoryManager.getInstance();
        userService = UserService.getInstance();
    }

    private static class InstanceHolder {
        private static final ForecastCategoryConverter INSTANCE = new ForecastCategoryConverter();
    }

    /***
     * <p>
     * Gets the {@link ForecastCategoryConverter}
     * </p>
     *
     * @return The {@link ForecastCategoryConverter}
     */
    public static ForecastCategoryConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData Represents the {@link CrmConverterData}
     * @return The {@link ForecastCategory}
     */
    @Override
    public ForecastCategory convertToModel(final CrmConverterData<JsonObject> converterData) {
        final Long orgId = converterData.orgId();
        final CrmUser crmUser = userService.getUser(orgId, converterData.ownerId());
        final ForecastCategory forecastCategory;
        final JsonObject data = converterData.dataToBeConverted();

        if (data.containsKey(ForecastCategoryFields.ID)) {
            forecastCategory = forecastCategoryManager.get(orgId, data.getLong(ForecastCategoryFields.ID));
        } else {
            forecastCategory = new ForecastCategory();
        }

        if (data.containsKey(ForecastCategoryFields.DESCRIPTION)) {
            forecastCategory.setDescription(data.getNullableString(ForecastCategoryFields.DESCRIPTION));
        }
        forecastCategory.setOrgId(orgId);
        forecastCategory.setCreatedBy(crmUser);
        forecastCategory.setUpdatedBy(crmUser);

        if (data.containsKey(ForecastCategoryFields.NAME)) {
            forecastCategory.setName(data.getString(ForecastCategoryFields.NAME));
        }

        if (data.containsKey(ForecastCategoryFields.IS_DEFAULT)) {
            forecastCategory.setDefault(data.getBoolean(ForecastCategoryFields.IS_DEFAULT));
        }

        return forecastCategory;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData Represents the {@link CrmConverterData}
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<ForecastCategory> converterData) {
        final ForecastCategory forecastCategory = converterData.dataToBeConverted();
        final JsonObject forecastCategoryJson = Json.object();
        final I18nResolver i18nServiceResolver = i18nService.getResolver(converterData.locale());
        final I18nKey displayName = forecastCategory.getDisplayName();

        forecastCategoryJson.put(ForecastCategoryFields.ID, forecastCategory.id());
        forecastCategoryJson.put(ForecastCategoryFields.NAME, Objects.nonNull(displayName) && !forecastCategory.isBestCase() ? i18nServiceResolver.get(displayName):  forecastCategory.getName());
        forecastCategoryJson.put(ForecastCategoryFields.DESCRIPTION, i18nServiceResolver.get(forecastCategory.getDescription()));
        forecastCategoryJson.put(ForecastCategoryFields.IS_DEFAULT, forecastCategory.isDefault());
        forecastCategoryJson.put(ForecastCategoryFields.IS_DELETABLE, forecastCategory.isDeletable());
        forecastCategoryJson.put(ForecastCategoryFields.IS_EDITABLE, forecastCategory.isEditable());

        return forecastCategoryJson;
    }
}
