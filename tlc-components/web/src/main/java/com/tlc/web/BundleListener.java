package com.tlc.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.web.registrar.WebResourceRegistrar;
import com.tlc.web.registrar.model.WebRegistrarData;

import com.tlc.web.undertow.UndertowService;
import org.osgi.framework.Bundle;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
class BundleListener extends com.tlc.commons.service.listener.BundleListener
{
    private final WebResourceRegistrar webRegistrar;

    BundleListener(final UndertowService webService) {
        this.webRegistrar = new WebResourceRegistrar(webService);
    }


    @Override
    public void bundleStarted(Bundle bundle)
    {
        final Map<WebAction, Action> actionMap = fetchActions(bundle);
        final WebRegistrarData webRegistrarData = new WebRegistrarData();

        webRegistrarData.setModuleName(bundle.getSymbolicName());
        webRegistrarData.setActionMap(actionMap);
        webRegistrar.register(webRegistrarData);
    }

    @Override
    public void bundleStopped(Bundle bundle)
    {
        final WebRegistrarData webRegistrarData = new WebRegistrarData();

        webRegistrarData.setModuleName(bundle.getSymbolicName());
        webRegistrar.unregister(webRegistrarData);
    }

    @SuppressWarnings("unchecked")
    private Map<WebAction, Action> fetchActions(final Bundle bundle) {
        try {
            final Map<WebAction, Action> actionsMap = new HashMap<>();

            for (final Class<?> classObj : getClasses("/action/", bundle)) {
                final WebAction annotation = classObj.getAnnotation(WebAction.class);

                if (Objects.nonNull(annotation)) {
                    final Constructor<Action> constructor = (Constructor<Action>) classObj.getConstructor();
                    final Action action = constructor.newInstance();

                    if (actionsMap.containsKey(annotation)) {
                        throw ErrorCode.get(ErrorCodes.WEB_ACTION_ALREADY_REGISTERED, String.format("PATH : %s, METHOD : %s", annotation.path(), annotation.method()));
                    }
                    actionsMap.put(annotation, action);
                }
            }
            return actionsMap;
        } catch (Exception exp) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, exp);
        }
    }
}
