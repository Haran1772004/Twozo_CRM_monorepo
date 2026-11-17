package com.tlc.web.registrar;

import com.tlc.commons.service.registrar.ResourceRegistrar;

import com.tlc.web.Action;
import com.tlc.web.WebAction;
import com.tlc.web.registrar.model.WebRegistrarData;
import com.tlc.web.undertow.UndertowService;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.RouteContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import java.lang.reflect.Method;

/**
 * <p>
 * The registrar that registers the given web resources/files.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ResourceRegistrar
 */
public class WebResourceRegistrar implements ResourceRegistrar<WebRegistrarData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebResourceRegistrar.class);

    private final UndertowService webService;
    private final Map<String, Set<String>> loadedPaths;
    private final Map<String, Set<String>> restApiLoadedPaths;

    public WebResourceRegistrar(final UndertowService webService) {
        this.webService = webService;
        this.loadedPaths = new HashMap<>();
        this.restApiLoadedPaths = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link WebRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final WebRegistrarData registrarData) {
        switch (registrarData.getApiStyle()) {
            case INTERNAL -> {
                final Map<WebAction, Action> actionMap = registrarData.getActionMap();

                if (!actionMap.isEmpty()) {
                    final String moduleName = registrarData.getModuleName();

                    actionMap.forEach(webService::register);
                    LOGGER.info("Total Registered actions : {}, from Module : {}", actionMap.size(), moduleName);
                    loadedPaths.put(moduleName, actionMap.keySet().stream().map(WebAction::path).collect(Collectors.toSet()));
                }
            }
            case REST -> {
                final Map<RestApiHandler, Map<Method, Action>> restActionMap = registrarData.getRestActionMap();

                if (!restActionMap.isEmpty()) {
                    final String moduleName = registrarData.getModuleName();

                    for (final Map.Entry<RestApiHandler, Map<Method, Action>> groupedActions : restActionMap.entrySet()) {
                        final RestApiHandler restApiHandler = groupedActions.getKey();
                        final Map<Method, Action> actionMap = groupedActions.getValue();

                        actionMap.forEach((method, action) -> {
                            final ApiRoute annotation = method.getAnnotation(ApiRoute.class);

                            webService.register(restApiHandler, new RouteContext(annotation, method, action));
                            restApiLoadedPaths.computeIfAbsent(moduleName, k -> new HashSet<>()).add(String.join("", restApiHandler.path(), annotation.path()));
                        });
                        LOGGER.info("Total Registered rest actions : {}, from Module : {}", actionMap.size(), moduleName);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link WebRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final WebRegistrarData registrarData) {
        final String moduleName = registrarData.getModuleName();
        final Consumer<Set<String>> unregister = paths -> {
            if (paths != null) {
                paths.forEach(webService::unregister);
                LOGGER.info("Total Unregistered actions : {}, from Module : {}", paths.size(), moduleName);
            }
        };

        unregister.accept(loadedPaths.remove(moduleName));
        unregister.accept(restApiLoadedPaths.remove(moduleName));
    }
}
