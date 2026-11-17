package com.tlc.crm.entity.action.forecastcategory;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.iam.api.preferences.manager.OrgPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.type.OrgPreferenceType;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.entity.api.iam.model.OrgForecastCategoryPreference;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.forecastcategory.fields.ForecastCategoryFields;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.entity.forecastcategory.model.converter.ForecastCategoryConverter;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Manages the forecast category settings related actions
 *
 * @author kishore J
 * @version 1.0
 */
@WebAction(path = "/settings/forecast/category/mgmt")
public class ForecastCategoryManagement extends CrmConfigAction<ForecastCategory> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 20;
    private static final long CREATION_LIMIT = 5;

    private final ForecastCategoryConverter forecastCategoryConverter;
    private final ForecastCategoryManager forecastCategoryManager;
    private final OrgPreferenceManager orgPreferenceManager;

    public ForecastCategoryManagement() {
        forecastCategoryConverter = ForecastCategoryConverter.getInstance();
        forecastCategoryManager = ForecastCategoryManager.getInstance();
        orgPreferenceManager = OrgPreferenceManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final Long orgId = request.orgId();

        switch (type) {
            case ForecastCategoryFields.UPDATE_DEFAULT_CATEGORY -> updateDefaultCategory(orgId, data, response);
            case ForecastCategoryFields.LIST -> getAll(request, response);
            case ForecastCategoryFields.UPDATE_ENABLE_STATUS -> updateSettingsStatus(orgId, data, response);
            case ForecastCategoryFields.GET_ENABLE_STATUS -> {
                final OrgForecastCategoryPreference orgForecastCategoryPreference = orgPreferenceManager.get(orgId).get(OrgPreferenceType.ORG_FORECAST_CATEGORY_PREFERENCE);

                response.put(ForecastCategoryFields.IS_ENABLED, Objects.nonNull(orgForecastCategoryPreference) && orgForecastCategoryPreference.isEnabled());
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmResponse        Represents the crm response
     * @param orgId              Represents an organization id
     * @param forecastCategories Which contains the collection of {@link ForecastCategory}
     * @param eventData          Represents the {@link EventData}
     */
    @Override
    public void create(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<ForecastCategory> forecastCategories, final EventData eventData) {

        if (forecastCategoryManager.getTotalRecords(orgId) >= NumericalConstant.FIVE) {
            throw ErrorCode.get(EntityErrorCodes.FORECAST_CATEGORY_CREATION_LIMIT_REACHED, "i18n.entity.forecast.category.creation.limit.reached");
        }
        super.create(crmResponse, orgId, userId, forecastCategories, eventData);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Organization id
     * @param userId             Represents the user id.
     * @param action             Perform action like create, update or delete
     * @param forecastCategories Collection of {@link ForecastCategory}
     * @return true if the given details are valid
     */
    @Override
    public boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<ForecastCategory> forecastCategories) {

        switch (action) {
            case CREATE -> {
                final int size = forecastCategories.size();
                final Collection<String> names = new HashSet<>(size);

                for (final ForecastCategory forecastCategory : forecastCategories) {

                    if (!names.add(forecastCategory.getName())) {
                        throw ErrorCode.get(EntityErrorCodes.DUPLICATE_FORECAST_CATEGORY_NAME, "i18n.entity.forecast.category.duplicate.name");
                    }
                }

                if (forecastCategoryManager.nameAlreadyExists(orgId, names)) {
                    throw ErrorCode.get(EntityErrorCodes.FORECAST_CATEGORY_NAME_ALREADY_EXISTS, "i18n.entity.forecast.category.already.exists");
                }
            }
            case UPDATE -> {
                final int size = forecastCategories.size();
                final Collection<String> names = new HashSet<>(size);
                final Collection<Long> ids = new HashSet<>(size);

                for (final ForecastCategory forecastCategory : forecastCategories) {

                    validateNotCommittedCategory(forecastCategory, EntityErrorCodes.FORECAST_CATEGORY_NOT_EDITABLE, "i18n.entity.forecast.category.not.editable");

                    if (!names.add(forecastCategory.getName())) {
                        throw ErrorCode.get(EntityErrorCodes.DUPLICATE_FORECAST_CATEGORY_NAME, "i18n.entity.forecast.category.duplicate.name");
                    }
                    ids.add(forecastCategory.id());
                }

                if (forecastCategoryManager.nameAlreadyExists(orgId, ids, names)) {
                    throw ErrorCode.get(EntityErrorCodes.FORECAST_CATEGORY_NAME_ALREADY_EXISTS, "i18n.entity.forecast.category.already.exists");
                }
            }
            case DELETE -> {
                for (final ForecastCategory forecastCategory : forecastCategories) {
                    validateNotCommittedCategory(forecastCategory, EntityErrorCodes.FORECAST_CATEGORY_NOT_DELETABLE, "i18n.entity.forecast.category.not.editable");
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ForecastCategoryManager}
     */
    @Override
    public ConfigManager<ForecastCategory> getConfigManager() {
        return forecastCategoryManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Crm request
     * @param jsonObject Holds entity details
     * @return {@link ForecastCategory}
     */
    @Override
    public ForecastCategory convert(final CrmRequest request, final JsonObject jsonObject) {
        return forecastCategoryConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(),
                request.getRequestJson().getJsonObject(FieldKeys.DATA.getName())));
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Represents the {@link CrmRequest}
     * @param jsonObject Represents the {@link JsonObject}
     * @return {@link ForecastCategory}
     */
    @Override
    public ForecastCategory convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        throw ErrorCode.get(EntityErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest       Represents the {@link CrmRequest}
     * @param forecastCategory Represents the {@link ForecastCategory}
     * @return The {@link JsonObject}
     */
    @Override
    protected JsonObject convert(final CrmRequest crmRequest, final ForecastCategory forecastCategory) {
        return forecastCategoryConverter.convertToJson(new CrmConverterData<>(crmRequest.getUser(), crmRequest.locale(), forecastCategory));
    }

    /**
     * <p>
     * Validates that the given forecast category is not a committed category, throwing the
     * specified error if it is.
     * </p>
     *
     * @param forecastCategory Represents the {@link ForecastCategory}
     * @param errorCode        Error code to raise when the category is committed
     * @param i18nKey          i18n key describing the error
     */
    private void validateNotCommittedCategory(final ForecastCategory forecastCategory, final EntityErrorCodes errorCode, final String i18nKey) {
        if (forecastCategory.isCommittedCategory()) {
            throw ErrorCode.get(errorCode, i18nKey);
        }
    }

    /**
     * <p>
     * Updates the forecasting settings status for the specified organization
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param data        Represents the data {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updateSettingsStatus(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final OrgForecastCategoryPreference orgForecastCategoryPreference = new OrgForecastCategoryPreference();
        final boolean isEnabled = data.getBoolean(ForecastCategoryFields.IS_ENABLED);

        orgForecastCategoryPreference.setOrgId(orgId);
        orgForecastCategoryPreference.setEnabled(isEnabled);
        orgPreferenceManager.updatePreference(orgForecastCategoryPreference);
        crmResponse.put(ForecastCategoryFields.MESSAGE, crmResponse.resolveI18nString(isEnabled ? "i18n.entity.quotas.forecasting.settings.enabled.success" :
                "i18n.entity.quotas.forecasting.settings.disabled.success"));
    }

    /**
     *
     * @param orgId       Represents an organization id
     * @param data        Represents the data {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void updateDefaultCategory(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        forecastCategoryManager.updateDefaultCategory(orgId, data.getLong(ForecastCategoryFields.ID));
        crmResponse.put(ForecastCategoryFields.MESSAGE, crmResponse.resolveI18nString("i18n.entity.default.forecast.category.update.success"));
    }

    /**
     * <p>
     * Fetches the collection of {@link ForecastCategory}
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    private void getAll(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(FieldKeys.DATA.getName());
        final Long orgId = request.orgId();
        final int start = Math.max(data.optInt(ForecastCategoryFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(ForecastCategoryFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final long totalRecords = forecastCategoryManager.getTotalRecords(orgId);

        if (totalRecords > NumericalConstant.ZERO) {
            final JsonArray forecastCategoryArray = Json.array();
            final Collection<ForecastCategory> forecastCategories = forecastCategoryManager.get(orgId, new LimitConfig(start, limit));

            for (final ForecastCategory forecastCategory : forecastCategories) {
               final JsonObject forecastCategoryJson = forecastCategoryConverter.convertToJson(new CrmConverterData<>(request.getUser(), request.locale(), forecastCategory));

                if (forecastCategory.isCommittedCategory()) {
                    forecastCategoryJson.put(ForecastCategoryFields.IS_DELETABLE, false);
                    forecastCategoryJson.put(ForecastCategoryFields.IS_EDITABLE, false);
                } else if (forecastCategory.isDefault()) {
                    forecastCategoryJson.put(ForecastCategoryFields.IS_DELETABLE, false);
                }
                forecastCategoryArray.put(forecastCategoryJson);
            }
            response.put(ForecastCategoryFields.LIST, forecastCategoryArray);
        }
        response.put(ForecastCategoryFields.IS_CREATION_LIMIT_EXCEEDED, Objects.equals(CREATION_LIMIT, totalRecords));
        response.put(ForecastCategoryFields.TOTAL_RECORDS, totalRecords);
        response.put(ForecastCategoryFields.HAS_MORE, (start + limit) <= totalRecords);
    }
}