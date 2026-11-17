package com.tlc.crm.core.action.iam;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.fields.ApiKeys;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;
import com.tlc.crm.core.api.iam.model.UserCompanyPreference;
import com.tlc.crm.core.api.iam.model.UserContactPreference;
import com.tlc.crm.core.api.iam.model.UserEntityPreference;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.web.WebAction;

import java.util.Objects;

/**
 * Manages the user entity preference web actions
 *
 * @author kishore J
 * @version 1.0
 */
@WebAction(path = "/user/entity/preference")
public class UserEntityPreferenceAction extends AuthorizationController {

    private final UserPreferenceManager userPreferenceManager;

    public UserEntityPreferenceAction() {
        userPreferenceManager = UserPreferenceManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final long userId = crmRequest.userId();
        final long orgId = crmRequest.orgId();

        switch (type) {
            case ApiKeys.UPDATE -> {
                final UserEntityPreference userEntityPreference = switch (Source.get(data.getString(FieldKeys.SOURCE.getName()))) {
                    case CONTACT -> new UserContactPreference();
                    case COMPANY -> new UserCompanyPreference();
                    default -> null;
                };

                if (Objects.nonNull(userEntityPreference)) {
                    userEntityPreference.setOrgId(orgId);
                    userEntityPreference.setUserId(userId);
                    userEntityPreference.setDuplicateCheckEnabled(data.optBoolean(PreferenceFields.IS_DUPLICATE_CHECK_ENABLED.getName()));
                    userPreferenceManager.updatePreference(userEntityPreference);

                    crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.user.entity.preference.updated"));
                }
            }
            case ApiKeys.GET -> {
                final UserPreferenceHolder userPreferenceHolder = userPreferenceManager.get(orgId, userId);

                final UserEntityPreference userEntityPreference = switch (Source.get(data.getString(FieldKeys.SOURCE.getName()))) {
                    case CONTACT -> userPreferenceHolder.get(UserPreferenceType.USER_CONTACT_PREFERENCE);
                    case COMPANY -> userPreferenceHolder.get(UserPreferenceType.USER_COMPANY_PREFERENCE);
                    default -> null;
                };

                if (Objects.nonNull(userEntityPreference)) {
                    crmResponse.put(PreferenceFields.IS_DUPLICATE_CHECK_ENABLED.getName(), userEntityPreference.isDuplicateCheckEnabled());
                }
            }
        }
    }
}
