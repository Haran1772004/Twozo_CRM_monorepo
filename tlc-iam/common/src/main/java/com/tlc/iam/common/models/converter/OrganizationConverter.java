package com.tlc.iam.common.models.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.models.Organization;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Objects;

/**
 * A converter that converts {@link Organization} into various formats
 *
 * @author Selvakumar
 * @version 1.0
 */
public class OrganizationConverter {

    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final OrganizationConverter INSTANCE = new OrganizationConverter();
    }

    private OrganizationConverter() {
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    public static OrganizationConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Organization}
     *
     * @param organization Represents the organization
     * @return {@link Organization}
     */
    public Organization convert(final JsonObject organization) {

        return convert(organization, new Organization());
    }

    /**
     * Converts {@link JsonObject} into {@link Organization}
     *
     * @param organizationJson Represents the {@link JsonObject} of organization
     * @param iamOrganization  Holds iamOrganization details
     * @return {@link Organization}
     */
    public Organization convert(final JsonObject organizationJson, final Organization iamOrganization) {

        if (organizationJson.containsKey(OrganizationFields.ID)) {
            iamOrganization.setId(organizationJson.getLong(OrganizationFields.ID));
        }
        iamOrganization.setActive(organizationJson.optBoolean(OrganizationFields.ACTIVE, true));
        iamOrganization.setName(organizationJson.optString(OrganizationFields.NAME, iamOrganization.getName()));
        iamOrganization.setOrgSize(organizationJson.containsKey(OrganizationFields.ORG_SIZE)
                ? Organization.OrgSize.valueOf(organizationJson.getShort(OrganizationFields.ORG_SIZE)) : null);
        iamOrganization.setUsersSize(organizationJson.containsKey(OrganizationFields.USERS_SIZE)
                ? Organization.UsersSize.valueOf(organizationJson.getShort(OrganizationFields.USERS_SIZE)) : null);
        iamOrganization.setIndustryType(organizationJson.containsKey(OrganizationFields.INDUSTRY_TYPE)
                ? Organization.IndustryType.valueOf(organizationJson.getShort(OrganizationFields.INDUSTRY_TYPE)) : null);
        iamOrganization.setOrgState(organizationJson.containsKey(OrganizationFields.ORG_STATE)
                ? Organization.State.valueOf(organizationJson.getShort(OrganizationFields.ORG_STATE)) : iamOrganization.getOrgState());

        return iamOrganization;
    }

    /**
     * Converts {@link Organization} into {@link JsonObject}
     *
     * @param organization Holds organization details
     * @return JsonObject
     */
    public JsonObject convert(final Organization organization) {
        final JsonObject jsonObject = Json.object();
        final Organization.OrgSize orgSize = organization.getOrgSize();
        final Organization.UsersSize usersSize = organization.getUsersSize();
        final Organization.State orgState = organization.getOrgState();

        jsonObject.put(OrganizationFields.ID, organization.id());
        jsonObject.put(OrganizationFields.NAME, organization.getName());
        jsonObject.put(OrganizationFields.ACTIVE, organization.isActive());
        jsonObject.put(OrganizationFields.OWNER_ID, organization.getOwnerId());
        jsonObject.put(OrganizationFields.ORG_SIZE, Objects.isNull(orgSize) ? null : orgSize.id());
        jsonObject.put(OrganizationFields.USERS_SIZE, Objects.isNull(usersSize) ? null : usersSize.id());
        jsonObject.put(OrganizationFields.STATE, Objects.isNull(orgState) ? null : orgState.id());
        jsonObject.put(OrganizationFields.CREATED_TIME, dateTimeUtil.getTimeStamp(organization.getCreatedTime()));
        jsonObject.put(OrganizationFields.STATE_UPDATED_TIME, dateTimeUtil.getTimeStamp(organization.getStateUpdatedTime()));

        return jsonObject;
    }

}
