package com.tlc.crm.common.action;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.i18n.I18nResolver;
import com.tlc.web.action.req.HttpRequest;

import java.util.Optional;

/**
 * @author Abishek
 * @version 1.0
 */
public interface CrmRequest extends HttpRequest {

    /**
     * Gets the orgId.
     *
     * @return The organisation id.
     */
    Long orgId();

    /**
     * Gets the userId.
     *
     * @return The user id.
     */
    Long userId();

    /**
     * <p>
     * Sets the crm user
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser}
     */
    void setUser(final CrmUser crmUser);

    /**
     * <p>
     * Gets the {@link CrmUser}
     * </p>
     *
     * @return The {@link CrmUser}
     */
    CrmUser getUser();

    /**
     * <p>
     * Represents the request client type
     * </p>
     */
    enum ClientType {
        WEB,
        MOBILE,
        ZAPIER,
        REST_API
        ;

        /**
         * <p>
         * Gets the {@link ClientType} matches for the given client name
         * </p>
         *
         * @param name Represents the name of the client
         * @return {@link ClientType}
         */
        public static ClientType get(final String name) {
            return Optional.ofNullable(name).map(String::toLowerCase)
                    .map(clientName -> switch (clientName) {
                        case "mobile" -> MOBILE;
                        case "zapier" -> ZAPIER;
                        default -> WEB;
                    }).orElseGet(() -> (WEB));
        }
    }

    /**
     * <p>
     * Gets the {@link ClientType}
     * </p>
     *
     * @return the {@link ClientType}
     */
    ClientType getClientType();

    /**
     * <p>
     * Gets the {@link I18NCRMResolutionAdapter}
     * </p>
     *
     * @return the {@link I18NCRMResolutionAdapter}
     */
    I18NCRMResolutionAdapter getI18NCRMResolutionAdapter();

    /**
     * <p>
     * Initializes the i18n resolution adapter
     * </p>
     *
     * @param orgId        Represents the organisation Id
     * @param i18nResolver Represents the {@link I18nResolver}
     */
    void initializeI18nResolutionAdapter(final Long orgId, final I18nResolver i18nResolver);
}
