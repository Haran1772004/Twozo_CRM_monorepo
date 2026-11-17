package com.tlc.crm.entity.search.script;

/**
 * <p>
 * Pertains to script id for crm global search (Web client).
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class ScriptKeysWebClient {
    public static final String CONTACT_COMPANY_TERRITORY_SCRIPT = "contact_company_territory_script";
    public static final String CONTACT_COMPANY_RESTRICTED_SCRIPT = "contact_company_restricted_script";
    public static final String CONTACT_COMPANY_FOLLOWER_SCRIPT = "contact_company_follower_script";
    public static final String DEAL_CONTACT_COMPANY_SCRIPT_WEB = "deal_contact_company_script_web";
    public static final String DEAL_CONTACT_SCRIPT_WEB = "deal_contact_script_web";
    public static final String DEAL_COMPANY_SCRIPT_WEB = "deal_company_script_web";
    public static final String NOTE_OR_FILE_CONTACT_COMPANY_SCRIPT = "note_or_file_contact_company_script";
    public static final String NOTE_OR_FILE_CONTACT_SCRIPT = "note_or_file_contact_script";
    public static final String NOTE_OR_FILE_COMPANY_SCRIPT = "note_or_file_company_script";
    public static final String ACTIVITY_ALL_ENTITIES_SCRIPT = "activity_all_entities_script";
    public static final String ACTIVITY_DEAL_COMPANY_SCRIPT = "activity_deal_company_script";
    public static final String ACTIVITY_DEAL_CONTACT_SCRIPT = "activity_deal_contact_script";
    public static final String ACTIVITY_CONTACT_COMPANY_SCRIPT = "activity_contact_company_script";
    public static final String ACTIVITY_DEAL_SCRIPT = "activity_deal_script";
    public static final String ACTIVITY_CONTACT_SCRIPT = "activity_contact_script";
    public static final String ACTIVITY_COMPANY_SCRIPT = "activity_company_script";

    private ScriptKeysWebClient() {
    }

    private static final class Instance {
        private static final ScriptKeysWebClient INSTANCE = new ScriptKeysWebClient();
    }

    /**
     * <p>
     * Gets the {@link ScriptKeysWebClient} instance.
     * </p>
     *
     * @return {@link ScriptKeysWebClient}.
     */
    public static ScriptKeysWebClient getInstance() {
        return ScriptKeysWebClient.Instance.INSTANCE;
    }

    /**
     * <p>
     * Resolves the script id for activity index based on which modules have restricted access.
     * A module needs scripting only when its permission map is restricted (not null and not empty).
     * null = no access, empty = global access, populated = restricted access (needs script).
     * </p>
     *
     * @param isDealRestricted   true if deal permission map is populated (restricted access, needs script).
     * @param isContactRestricted true if contact permission map is populated (restricted access, needs script).
     * @param isCompanyRestricted true if company permission map is populated (restricted access, needs script).
     * @return script id, or null if no module needs scripting.
     */
    public String getScriptIdForActivityIndex(final boolean isDealRestricted,
                                              final boolean isContactRestricted,
                                              final boolean isCompanyRestricted) {
        if (isDealRestricted && isContactRestricted && isCompanyRestricted) {
            return ACTIVITY_ALL_ENTITIES_SCRIPT;
        } else if (isDealRestricted && isContactRestricted) {
            return ACTIVITY_DEAL_CONTACT_SCRIPT;
        } else if (isDealRestricted && isCompanyRestricted) {
            return ACTIVITY_DEAL_COMPANY_SCRIPT;
        } else if (isContactRestricted && isCompanyRestricted) {
            return ACTIVITY_CONTACT_COMPANY_SCRIPT;
        } else if (isDealRestricted) {
            return ACTIVITY_DEAL_SCRIPT;
        } else if (isContactRestricted) {
            return ACTIVITY_CONTACT_SCRIPT;
        } else if (isCompanyRestricted) {
            return ACTIVITY_COMPANY_SCRIPT;
        }

        return null;
    }

    /**
     * <p>
     * Resolves the script id for deal index based on which modules have restricted access.
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
            return DEAL_CONTACT_COMPANY_SCRIPT_WEB;
        } else if (isContactRestricted) {
            return DEAL_CONTACT_SCRIPT_WEB;
        } else if (isCompanyRestricted) {
            return DEAL_COMPANY_SCRIPT_WEB;
        }

        return null;
    }

    /**
     * <p>
     * Resolves the script id for note and file index based on which modules have restricted access.
     * A module needs scripting only when its permission map is restricted (not null and not empty).
     * null = no access, empty = global access, populated = restricted access (needs script).
     * </p>
     *
     * @param isContactRestricted true if contact permission map is populated (restricted access, needs script).
     * @param isCompanyRestricted true if company permission map is populated (restricted access, needs script).
     * @return script id, or null if no module needs scripting.
     */
    public String getScriptIdForNoteAndFileIndex(final boolean isContactRestricted,
                                                 final boolean isCompanyRestricted) {
        if (isContactRestricted && isCompanyRestricted) {
            return NOTE_OR_FILE_CONTACT_COMPANY_SCRIPT;
        } else if (isContactRestricted) {
            return NOTE_OR_FILE_CONTACT_SCRIPT;
        } else if (isCompanyRestricted) {
            return NOTE_OR_FILE_COMPANY_SCRIPT;
        }

        return null;
    }
}