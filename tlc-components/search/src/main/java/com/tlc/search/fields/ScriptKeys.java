package com.tlc.search.fields;

/**
 * <p>
 * Provides the constants related to stored script operations.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public final class ScriptKeys {

    public static final String SCRIPT_DIR_LOCATION = "resources/stored_scripts/";
    public static final String SCRIPT_FILE_EXTENSION = "*.painless";
    public static final String PAINLESS = "painless";

    public static final String AUTH_HEADER = "X-Script-Auth";
    public static final String AUTH_PASSWORD_CONFIG = "script.auth.password";
    public static final String SEARCH_CONFIG = "search";

    public static final String TYPE = "type";
    public static final String UPLOAD_ALL = "uploadAll";
    public static final String UPLOAD = "upload";
    public static final String DELETE = "delete";
    public static final String SCRIPT_ID = "scriptId";
    public static final String SCRIPT_SOURCE = "scriptSource";
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";

    private ScriptKeys() {
    }
}