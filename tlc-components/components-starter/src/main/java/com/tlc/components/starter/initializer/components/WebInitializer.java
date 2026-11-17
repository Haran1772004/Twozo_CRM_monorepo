package com.tlc.components.starter.initializer.components;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;
import com.tlc.web.Action;
import com.tlc.web.WebAction;
import com.tlc.web.WebService;
import com.tlc.web.internal.registry.WebServiceRegistry;
import com.tlc.web.registrar.APIStyle;
import com.tlc.web.registrar.WebResourceRegistrar;
import com.tlc.web.registrar.model.WebRegistrarData;
import com.tlc.web.secure.CSRFTokenService;
import com.tlc.web.service.provider.WebServiceProvider;
import com.tlc.web.undertow.UndertowService;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;
import com.tlc.web.undertow.rest.RouteContext;
import com.tlc.web.versioning.rest.VersionedRestAction;
import com.tlc.web.internal.registry.VersionedRestActionRegistry;
import com.tlc.web.versioning.ApiVersion;
import com.tlc.web.versioning.VersionedAction;
import com.tlc.web.internal.registry.VersionedActionRegistry;

import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Responsible for initiating the initialization process of web services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class WebInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<WebService> webServiceRegistry;
    private final CSRFTokenService csrfTokenService;
    private final VersionedActionRegistry versionedActionRegistry;
    private final VersionedRestActionRegistry versionedRestActionRegistry;

    private WebInitializer() {
        this.webServiceRegistry = WebServiceRegistry.getInstance();
        csrfTokenService = CSRFTokenService.getInstance();
        versionedActionRegistry = VersionedActionRegistry.getInstance();
        versionedRestActionRegistry = VersionedRestActionRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final UndertowService undertowService = new UndertowService();
            final Configurations configurations = getProperties(ComponentsStarterFields.WEB);

            undertowService.configure(configurations);
            webServiceRegistry.register(undertowService);

            csrfTokenService.start(configurations);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during the post-initialization process.
     */
    @Override
    public void postInitialize() {
        if (isServiceEnabled()) {

            final UndertowService undertowService = (UndertowService) WebServiceProvider.get();
            final WebResourceRegistrar webRegistrar = new WebResourceRegistrar(undertowService);

            final BiConsumer<APIStyle, Map<Modules, Collection<Class<?>>>> annotationWebClassRegistrar = (type, registeredClasses) -> {

                for (final Map.Entry<Modules, Collection<Class<?>>> webRegisteredClasses : registeredClasses.entrySet()) {
                    final WebRegistrarData webRegistrarData = new WebRegistrarData();
                    final Modules modules = webRegisteredClasses.getKey();

                    webRegistrarData.setModuleId(modules.getId());
                    webRegistrarData.setModuleName(modules.getName());
                    webRegistrarData.setApiStyle(type);

                    switch (type) {
                        case INTERNAL -> webRegistrarData.setActionMap(fetchActions(webRegisteredClasses.getValue()));
                        case REST -> webRegistrarData.setRestActionMap(fetchRestActions(webRegisteredClasses.getValue()));
                    }
                    webRegistrar.register(webRegistrarData);
                }
            };

            annotationWebClassRegistrar.accept(APIStyle.INTERNAL, fetchRegisteredWebClasses());
            annotationWebClassRegistrar.accept(APIStyle.REST, fetchRegisteredRestWebClasses());

            undertowService.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.WEB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return webServiceRegistry;
    }

    /**
     * <p>
     * Retrieves a map of web actions with their corresponding actions.
     * Supports API versioning - multiple actions with the same path but different @ApiVersion
     * annotations will be combined into a single VersionedAction.
     * </p>
     *
     * @param webClasses Refers the collection of classes.
     * @return The registered web actions.
     */
    private Map<WebAction, Action> fetchActions(final Collection<Class<?>> webClasses) {
        final Map<WebAction, Action> actionMap = new HashMap<>();
        final Map<String, WebAction> versionedPaths = new HashMap<>();
        final Map<WebAction, Action> unversionedActions = new HashMap<>();

        try {
            for (final Class<?> clazz : webClasses) {
                final String className = clazz.getName().replace('.', '/') + ".class";
                final URL url = clazz.getClassLoader().getResource(className);

                if (null != url) {
                    final String file = url.getFile();

                    if (file.contains("/action/")) {
                        final Constructor<Action> constructor = (Constructor<Action>) clazz.getConstructor();
                        final Action action = constructor.newInstance();
                        final WebAction webAction = clazz.getAnnotation(WebAction.class);
                        final ApiVersion apiVersion = clazz.getAnnotation(ApiVersion.class);

                        if (Objects.isNull(apiVersion)) {
                            unversionedActions.put(webAction, action);
                        } else {
                            final String path = webAction.path();

                            versionedActionRegistry.register(webAction, apiVersion, action);
                            versionedPaths.putIfAbsent(path, webAction);
                        }
                    }
                }
            }

            for (final Map.Entry<WebAction, Action> entry : unversionedActions.entrySet()) {
                final WebAction webAction = entry.getKey();
                final String path = webAction.path();

                if (versionedPaths.containsKey(path)) {
                    versionedActionRegistry.register(webAction, null, entry.getValue());
                } else {
                    if (actionMap.containsKey(webAction)) {
                        throw ErrorCode.get(ErrorCodes.WEB_ACTION_ALREADY_REGISTERED);
                    }
                    actionMap.put(webAction, entry.getValue());
                }
            }

            for (final Map.Entry<String, WebAction> entry : versionedPaths.entrySet()) {
                final WebAction webAction = entry.getValue();
                final Map<VersionedActionRegistry.ActionKey, VersionedAction> builtActions = versionedActionRegistry.buildAll();

                for (final Map.Entry<VersionedActionRegistry.ActionKey, VersionedAction> builtEntry : builtActions.entrySet()) {
                    final VersionedActionRegistry.ActionKey key = builtEntry.getKey();

                    if (key.path().equals(entry.getKey())) {
                        actionMap.put(webAction, builtEntry.getValue());
                        break;
                    }
                }
            }
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, exception);
        }

        return actionMap;
    }

    /**
     * <p>
     * Fetches the {@link WebAction} registered {@link Class} files and organizes them into a map ordered by module.
     * </p>
     *
     * @return {@link Map} of modules with its web registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchRegisteredWebClasses() {
        try {
            final Collection<Class<?>> registeredWebClasses = classScanner.findClassesWithAnnotation("com.tlc", WebAction.class);

            return segregateClassesByModule(registeredWebClasses);
        } catch (Exception exception) {
            LOGGER.error("Failed to fetch registered web classes. Reason: {}", exception.getMessage(), exception);
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, exception);
        }
    }

    /**
     * <p>
     * Fetches the {@link RestApiHandler} registered {@link Class} files and organizes them into a map ordered by module.
     * </p>
     *
     * @return {@link Map} of modules with its rest web registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchRegisteredRestWebClasses() {
        try {
            final Collection<Class<?>> registeredClasses = classScanner.findClassesWithAnnotation("com.tlc", RestApiHandler.class);

            return segregateClassesByModule(registeredClasses);
        } catch (Exception exception) {
            LOGGER.error("Failed to fetch registered REST web classes. Reason: {}", exception.getMessage(), exception);
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, exception);
        }
    }

    /**
     * <p>
     * Retrieves a map of REST actions with their corresponding actions.
     * </p>
     *
     * @param webClasses Refers the collection of classes.
     * @return The registered REST actions.
     */
    private Map<RestApiHandler, Map<Method, Action>> fetchRestActions(final Collection<Class<?>> webClasses) {
        final Map<RestApiHandler, Map<Method, Action>> actionMap = new HashMap<>();

        try {
            for (final Class<?> clazz : webClasses) {
                final String className = clazz.getName().replace('.', '/') + ".class";
                final URL url = clazz.getClassLoader().getResource(className);

                if (null != url) {
                    final String file = url.getFile();

                    if (file.contains("/action/rest/")) {
                        final RestApiHandler restApiHandler = clazz.getAnnotation(RestApiHandler.class);
                        final ApiVersion apiVersion = clazz.getAnnotation(ApiVersion.class);
                        final Constructor<Action> constructor = (Constructor<Action>) clazz.getConstructor();
                        final Action action = constructor.newInstance();

                        if (Objects.isNull(apiVersion)) {
                            if (actionMap.containsKey(restApiHandler)) {
                                throw ErrorCode.get(ErrorCodes.WEB_ACTION_ALREADY_REGISTERED);
                            }

                            for (final Method method : clazz.getMethods()) {
                                if (method.isAnnotationPresent(ApiRoute.class)) {
                                    actionMap.computeIfAbsent(restApiHandler, k -> new HashMap<>()).put(method, action);
                                }
                            }
                        } else {

                            for (final Method method : clazz.getMethods()) {
                                if (method.isAnnotationPresent(ApiRoute.class)) {
                                    final ApiRoute apiRoute = method.getAnnotation(ApiRoute.class);
                                    final RouteContext routeContext = new RouteContext(apiRoute, method, action);

                                    versionedRestActionRegistry.register(restApiHandler, apiVersion, apiRoute, routeContext);
                                }
                            }
                        }
                    }
                }
            }

            final Map<RestApiHandler, Collection<VersionedRestAction>> versionedActions = versionedRestActionRegistry.buildAll();

            for (final Map.Entry<RestApiHandler, Collection<VersionedRestAction>> entry : versionedActions.entrySet()) {
                final RestApiHandler restApiHandler = entry.getKey();

                for (final VersionedRestAction versionedRestAction : entry.getValue()) {
                    final RouteContext sample = versionedRestAction.anyRouteContext();

                    actionMap.computeIfAbsent(restApiHandler, k -> new HashMap<>())
                            .put(sample.method(), versionedRestAction);
                }
            }
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.FAILED_TO_REGISTER_WEB_ACTION, exception);
        }

        return actionMap;
    }
}