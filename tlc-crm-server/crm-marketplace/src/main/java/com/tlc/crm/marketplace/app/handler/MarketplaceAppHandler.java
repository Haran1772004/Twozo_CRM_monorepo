package com.tlc.crm.marketplace.app.handler;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * An annotation for annotating the classes that used to handle the marketplace app actions
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@Retention(RUNTIME)
@Target({TYPE})
@Documented
public @interface MarketplaceAppHandler {

    /**
     * <p>
     * Holds the marketplace app name
     * </p>
     *
     * @return The marketplace app name
     */
    String app();
}