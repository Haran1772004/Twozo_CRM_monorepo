package com.tlc.crm.entity.search.script;

/**
 * <p>
 * Pertains to script id for crm global search (Mobile client).
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ScriptKeysMobileClient {

    public static final String DEAL_CONTACT_COMPANY_SCRIPT_MOBILE = "deal_contact_company_script_mobile";
    public static final String DEAL_CONTACT_SCRIPT_MOBILE = "deal_contact_script_mobile";
    public static final String DEAL_COMPANY_SCRIPT_MOBILE = "deal_company_script_mobile";

    private ScriptKeysMobileClient() {
    }

    private static final class Instance {
        private static final ScriptKeysMobileClient INSTANCE = new ScriptKeysMobileClient();
    }

    /**
     * <p>
     * Gets the {@link ScriptKeysMobileClient} instance.
     * </p>
     *
     * @return {@link ScriptKeysMobileClient}.
     */
    public static ScriptKeysMobileClient getInstance() {
        return ScriptKeysMobileClient.Instance.INSTANCE;
    }

    /**
     * <p>
     * Resolves the script id for deal index (mobile) based on which modules have restricted access.
     * A module needs scripting only when its permission map is restricted (not null and not empty).
     * null = no access, empty = global access, populated = restricted access (needs script).
     * </p>
     *
     * @param isContactRestricted true if contact permission map is populated (restricted access, needs script).
     * @param isCompanyRestricted true if company permission map is populated (restricted access, needs script).
     * @return script id, or null if no module needs scripting.
     */
    public String getScriptIdForDealIndex(final boolean isContactRestricted,
                                          final boolean isCompanyRestricted) {
        if (isContactRestricted && isCompanyRestricted) {
            return DEAL_CONTACT_COMPANY_SCRIPT_MOBILE;
        } else if (isContactRestricted) {
            return DEAL_CONTACT_SCRIPT_MOBILE;
        } else if (isCompanyRestricted) {
            return DEAL_COMPANY_SCRIPT_MOBILE;
        }

        return null;
    }
}
