package com.tlc.commons.util;

import java.io.File;

/**
 * <p>
 * Provides utility functionalities to handle a bundle's meta data
 * </p>
 *
 * @author Selvakumar G
 * @version 1.0
 */
public final class Env
{
    private static final boolean IS_DEV_MODE = Boolean.parseBoolean(System.getProperty("tlc.dev.mode"));
    private static final boolean SECURITY_ENABLED = Boolean.parseBoolean(System.getProperty("tlc.security.enabled"));
    private static final String USER_HOME = String.join(File.separator, System.getProperty("user.home"), "twozo-crm");

    private static final String WORK_DIRECTORY = String.join(File.separator, USER_HOME, "data", "tlc");
    private static final String CONF_DIRECTORY = String.join(File.separator, USER_HOME, "conf");

    /**
     * <p>
     * Checks whether development mode enabled or not
     * </p>
     *
     * @return
     */
    public static Boolean isDevMode()
    {
        return IS_DEV_MODE;
    }

    /**
     * <p>
     * Checks whether security is enabled or disabled to manage authorization
     * </p>
     *
     * @return true if security is enabled; otherwise false
     */
    public static boolean isSecurityEnabled() {
        return SECURITY_ENABLED;
    }

    /**
     * <p>
     * Returns TLC Home directory location
     * </p>
     *
     * @return
     */
    public static String getWorkDirectory()
    {
        return WORK_DIRECTORY;
    }

    public static String getConfDirectory()
    {
        return CONF_DIRECTORY;
    }
}
