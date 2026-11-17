package com.tlc.notification.model.template;

import java.util.Collection;

/**
 * <p>
 * Holds the basic information of a template
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public interface Template {

    Collection<PlaceHolder> placeHolders();
}