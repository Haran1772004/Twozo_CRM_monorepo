package com.tlc.crm.core.action.transformer;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Locale;

/**
 * Base Transformer for quota-related fields
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class AbstractQuotaFieldTransformer implements FieldTransformer {

    private final UserService crmUserService;
    private final I18nService i18nService;

    protected AbstractQuotaFieldTransformer() {
        crmUserService = UserService.getInstance();
        i18nService = I18nServiceProvider.get();
    }

    /**
     * <p>
     * Resolves the localized i18n value for the given key.
     * </p>
     *
     * @param key The i18n key to resolve
     * @return The localized value corresponding to the given key
     */
    protected String resolveI18nValue(final String key) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = crmUserService.getUser(crmUser.orgId(), crmUser.id())
                .getPreferenceHolder().get(UserRegionalPreference.TYPE);

        return i18nService.getResolver(Locale.forLanguageTag(userRegionalPreference.getLanguage().getName())).get(key);
    }
}
