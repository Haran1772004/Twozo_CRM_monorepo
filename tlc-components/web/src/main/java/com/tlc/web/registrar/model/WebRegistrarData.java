package com.tlc.web.registrar.model;

import com.tlc.commons.service.registrar.model.RegistrarData;

import com.tlc.web.Action;
import com.tlc.web.WebAction;
import com.tlc.web.registrar.APIStyle;
import com.tlc.web.registrar.WebResourceRegistrar;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * <p>
 * A model that holds the data of the {@link WebResourceRegistrar} which is needed for registering the resources/actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class WebRegistrarData extends RegistrarData {

    private Map<WebAction, Action> actionMap;
    private Map<RestApiHandler, Map<Method, Action>> restActionMap;
    private APIStyle apiStyle;

    public void setActionMap(final Map<WebAction, Action> actionMap) {
        this.actionMap = actionMap;
    }

    public Map<WebAction, Action> getActionMap() {
        return actionMap;
    }

    public Map<RestApiHandler, Map<Method, Action>> getRestActionMap() {
        return restActionMap;
    }

    public void setRestActionMap(final Map<RestApiHandler, Map<Method, Action>> restActionMap) {
        this.restActionMap = restActionMap;
    }

    public APIStyle getApiStyle() {
        return apiStyle;
    }

    public void setApiStyle(final APIStyle apiStyle) {
        this.apiStyle = apiStyle;
    }
}
