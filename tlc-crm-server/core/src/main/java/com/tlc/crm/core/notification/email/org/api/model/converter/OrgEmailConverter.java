package com.tlc.crm.core.notification.email.org.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.core.notification.email.org.api.OrgEmailManager;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.org.api.model.OrgEmail;

import com.tlc.email.common.model.address.EmailAddress;

/**
 * <p>
 * Provides different ways to convert the data from database into {@link OrgEmail}
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class OrgEmailConverter extends EntityConverter<OrgEmail> {

    private final OrgEmailManager orgEmailManager;

    private OrgEmailConverter() {
        orgEmailManager = OrgEmailManager.getInstance();
    }

    private static class Instance {
        private static final OrgEmailConverter INSTANCE = new OrgEmailConverter();
    }

    /**
     * Gets the {@link OrgEmailConverter} Instance
     *
     * @return {@link OrgEmailConverter}
     */
    public static OrgEmailConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts json attributes data into {@link OrgEmail}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Holds the org email attributes
     * @return {@link OrgEmail}
     */
    @Override
    public OrgEmail convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? constructOrgEmailPartialData(converterConfig, jsonObject) :
                constructOrgEmail(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Converts json attributes data into {@link OrgEmail}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Holds the org email attributes
     * @return {@link OrgEmail}
     */
    private OrgEmail constructOrgEmailPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final OrgEmail orgEmail = orgEmailManager.get(converterConfig.orgId(), jsonObject.getLong(CoreEmailFields.ID.getName()));

        setJsonDataToModel(converterConfig, orgEmail, jsonObject);

        return orgEmail;
    }

    /**
     * <p>
     * Converts json attributes into {@link OrgEmail}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Holds the org email attributes
     * @return {@link OrgEmail}
     */
    private OrgEmail constructOrgEmail(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final OrgEmail orgEmail = new OrgEmail();
        final String idKey = CoreEmailFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            orgEmail.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, orgEmail, jsonObject);

        return orgEmail;
    }

    /**
     * <p>
     * Sets the Json data to the {@link OrgEmail}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param orgEmail        An entity that holds org email attributes
     * @param jsonObject      Holds the org email attributes to be set into the entity
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final OrgEmail orgEmail, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final String nameKey = CoreEmailFields.NAME.getName();
        final String emailAddressKey = CoreEmailFields.EMAIL_ADDRESS.getName();
        final EmailAddress emailAddress = new EmailAddress(jsonObject.optString(emailAddressKey.trim(), null));
        final JsonArray jsonArray = jsonObject.optJsonArray(CoreEmailFields.ROLE_IDS.getName());

        orgEmail.setOrgId(orgId);
        orgEmail.setUserId(converterConfig.ownerId());
        orgEmail.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey).trim() : orgEmail.getName());
        orgEmail.setAddress(emailAddress);

        jsonArray.forEach(roleId -> {
            final CrmRole crmRole = new CrmRole();

            crmRole.setId((Long) roleId);
            orgEmail.addCrmRole(crmRole);
        });
    }

    /**
     * <p>
     * Converts the {@link OrgEmail} into {@link JsonObject}
     * </p>
     *
     * @param orgEmail An entity that holds org email attributes
     * @return {@link JsonObject}
     */
    public JsonObject convert(final OrgEmail orgEmail) {
        final JsonObject jsonObject = Json.object();
        final JsonArray roleArray = Json.array();

        jsonObject.put(CoreEmailFields.ID.getName(), orgEmail.id());
        jsonObject.put(CoreEmailFields.USER_ID.getName(), orgEmail.getUserId());
        jsonObject.put(CoreEmailFields.NAME.getName(), orgEmail.getName());
        jsonObject.put(CoreEmailFields.EMAIL_ADDRESS.getName(), orgEmail.getAddress().getEmailAddress());
        jsonObject.put(CoreEmailFields.IS_VERIFIED.getName(), orgEmail.isVerified());

        for (final CrmRole crmUserRole : orgEmail.getCrmRoles()) {
            final JsonObject roleJson = Json.object();

            roleJson.put(CoreEmailFields.ID.getName(), crmUserRole.id());
            roleJson.put(CoreEmailFields.NAME.getName(), crmUserRole.getName());

            roleArray.put(roleJson);
        }
        jsonObject.put(CoreEmailFields.ROLE.getName(), roleArray);

        return jsonObject;
    }
}
