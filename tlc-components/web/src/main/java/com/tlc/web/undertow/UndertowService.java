package com.tlc.web.undertow;

import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.executor.context.session.SessionContext;
import com.tlc.commons.executor.context.session.SessionContextHolder;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.Configurations;
import com.tlc.web.Action;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.TwozoWebService;
import com.tlc.web.WebAction;
import com.tlc.web.WebExchange;
import com.tlc.web.undertow.cors.PreflightConfig;
import com.tlc.web.undertow.cors.PreflightHandler;
import com.tlc.web.undertow.cors.PreflightHandlerRegistry;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.RouteContext;
import com.tlc.web.undertow.rest.RestWebService;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Abishek
 * @version 1.0
 */
public class UndertowService implements TwozoWebService, RestWebService {

    private static final String FORWARDED_PROTOCOL_HEADER = "X-Forwarded-Proto";
    private static final Logger LOGGER = LoggerFactory.getLogger(UndertowService.class);
    private static final String PATH_KEY = "path";
    private static final String OPERATIONS_KEY = "operations";
    private static final String REST_KEY = "rest";

    private String pathPrefix;
    private String restPathPrefix;
    private Collection<PreflightConfig> preflightConfigs;
    private String appName;
    private Undertow server;
    private RoutingHandler routingHandler;
    private BlockingHandler blockingHandler;
    private EagerFormParsingHandler formParsingHandler;

    private ThreadPoolExecutor executor;

    @Override
    public void start(final Configurations configurations) {
        configure(configurations);
        start();
    }

    /**
     * Configures the Undertow server without starting it. Call {@link #start()} after route registration is complete.
     *
     * @param configurations The web service configurations.
     */
    public void configure(final Configurations configurations) {
        final String port = Objects.requireNonNull(configurations.getConfig("http.port"));
        final int workerThreadsCount = Integer.parseInt(configurations.optConfig("worker.threads.count", "50"));

        this.executor = ExecutorsHandler.createFixedThreadExecutor(workerThreadsCount, "web-req-handler", false);
        final String webPathPrefix = configurations.optConfig("path.prefix", "");
        this.pathPrefix = webPathPrefix.isEmpty() ? String.format("%s/", webPathPrefix) : webPathPrefix;
        this.restPathPrefix = String.format("%s%s%s", "/", REST_KEY, pathPrefix);
        this.preflightConfigs = getPreflightConfigs(configurations);

        this.formParsingHandler = new EagerFormParsingHandler(FormParserFactory.builder(false).build());
        this.routingHandler = Handlers.routing();
        this.blockingHandler = new BlockingHandler();
        this.server = Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .setServerOption(UndertowOptions.IDLE_TIMEOUT, 120000)
                .setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 120000)
                .addHttpListener(Integer.parseInt(port), "0.0.0.0", withCorsInterceptor(routingHandler)).build();
        this.appName = configurations.getConfig("app.name");

        routingHandler.get(prependPath(pathPrefix, "/healthcheck"), exchange -> processHealthCheck(exchange, appName));
        //TODO set request body max limit
    }

    /**
     * Starts the Undertow server. Should be called after all routes are registered.
     */
    public void start() {
        server.start();
        LOGGER.info("Undertow Service Activated");
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }

        if (Objects.nonNull(executor)) {
            executor.shutdown();
        }
    }

    @Override
    public void register(final WebAction annotation, final Action action) {
        for (final HttpMethod httpMethod : annotation.method()) {
            if (Objects.equals(httpMethod, HttpMethod.POST)) {
                routingHandler.post(prependPath(pathPrefix, annotation.path()), exchange ->
                {
                    final String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
                    final boolean isFormData = Objects.nonNull(contentType) && contentType.startsWith(MultiPartParserDefinition.MULTIPART_FORM_DATA);

                    process(action, annotation, exchange, isFormData);
                });
            } else if (Objects.equals(httpMethod, HttpMethod.GET)) {
                routingHandler.get(prependPath(pathPrefix, annotation.path()), exchange -> process(action, annotation, exchange, false));
            } else {
                LOGGER.info("Unable to register web action for path {}, Routing handler not defined for {} method",
                        annotation.path(), httpMethod);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param restApiHandler Refers the {@code RestController}.
     * @param routeContext   The {@link RouteContext} containing method, action instance, and route configuration.
     */
    @Override
    public void register(final RestApiHandler restApiHandler, final RouteContext routeContext) {
        final ApiRoute annotation = routeContext.annotation();
        final Action action = routeContext.action();
        final String canonicalPath = String.format("%s%s", restApiHandler.path(), annotation.path());
        final String routePath = prependPath(restPathPrefix, canonicalPath);

        switch (annotation.method()) {
            case POST -> routingHandler.post(routePath, exchange ->
            {
                final String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
                final boolean isFormData = Objects.nonNull(contentType) && contentType.startsWith(MultiPartParserDefinition.MULTIPART_FORM_DATA);

                process(action, exchange, routeContext, isFormData);
            });
            case GET -> routingHandler.get(routePath, exchange -> process(action, exchange, routeContext, false));
            case DELETE -> routingHandler.delete(routePath, exchange -> process(action, exchange, routeContext, false));
            case PUT -> routingHandler.put(routePath, exchange -> process(action, exchange, routeContext, false));
            case PATCH -> routingHandler.add(Methods.PATCH, routePath, exchange -> process(action, exchange, routeContext, false));
            case OPTIONS -> routingHandler.add(Methods.OPTIONS, routePath, exchange -> process(action, exchange, routeContext, false));
            default -> LOGGER.error("Unable to register rest web action for path {}, Routing handler not defined for {} method", annotation.path(), annotation.method());
        }
    }

    /**
     * Wraps the given {@link HttpHandler} with CORS handling logic.
     * <p>
     * This interceptor handles HTTP OPTIONS requests by responding with
     * appropriate CORS headers for preflight checks. For all other requests,
     * it ensures CORS headers are added to the response and then delegates
     * to the original handler.
     *
     * @param httpHandler The original handler to wrap
     * @return The CORS-aware {@code HttpHandler} that intercepts preflight requests.
     */
    private HttpHandler withCorsInterceptor(final HttpHandler httpHandler) {
        return exchange -> {
            if (HttpMethod.OPTIONS.name().equalsIgnoreCase(exchange.getRequestMethod().toString())) {
                final Optional<PreflightHandler> optionalPreflightHandler = PreflightHandlerRegistry.getInstance().get(appName);

                optionalPreflightHandler.ifPresent(preflightHandler -> {
                    exchange.getRequestReceiver().receiveFullBytes((exchange1, message) -> {
                        final WebExchange webExchange = new UndertowExchange(new ExchangeContext(exchange, RequestReadMode.EAGER_READ, false, message, null));

                        preflightHandler.handleAccess(webExchange, message, preflightConfigs);
                    });
                });
            } else {
                httpHandler.handleRequest(exchange);
            }
        };
    }

    /**
     * Extracts and builds a collection of {@link PreflightConfig} objects
     * from the provided {@link Configurations} source.
     *
     * @param configurations The configuration source containing preflight rules.
     * @return A collection of {@code PreflightConfig} instances representing allowed paths and types.
     */
    private Collection<PreflightConfig> getPreflightConfigs(final Configurations configurations) {
        final Collection<Map<String, String>> groupedConfigs = configurations.groupedConfigArray("preflight.allowed.endpoints");

        return groupedConfigs.stream().map((config) -> {
            final String path = config.get(PATH_KEY);
            final String operations = config.get(OPERATIONS_KEY);

            return new PreflightConfig(path, Objects.isNull(operations)
                    ? Collections.emptyList() : List.of(operations.split(",")));
        }).toList();
    }

    /**
     * Handles the request by determining whether it is multipart form data, a blocking request, or a non-blocking request.
     *
     * @param action     Represents the specific action to perform for the given request.
     * @param annotation Represents the annotation for the web action.
     * @param exchange   Represents the current web exchange.
     * @param isFormData Indicates whether the received request is of multipart type.
     */
    private void process(final Action action, final WebAction annotation, final HttpServerExchange exchange, final boolean isFormData) {

        if (isFormData) {
            exchange.dispatch(formParsingHandler);
            formParsingHandler.setNext(multipartExchange -> process(action,
                    new ExchangeContext(multipartExchange, RequestReadMode.LAZY_READ, true)));
            return;
        }

        if (annotation.isBlockingOperation()) {
            exchange.dispatch(blockingHandler);
            blockingHandler.setRootHandler(blockExchange -> process(blockExchange, action));
        } else {
            process(exchange, action);
        }
    }

    /**
     * Processes the web request
     *
     * @param exchange web exchange
     * @param action   registered web action
     */
    private void process(final HttpServerExchange exchange, final Action action) {
        exchange.getRequestReceiver().receiveFullBytes((webExchange, message) -> {
            LOGGER.info("Undertow: Request read completed. URL: {}", webExchange.getRequestURL());

            process(action, new ExchangeContext(webExchange, RequestReadMode.EAGER_READ, false, message, null));
        });
    }

    /**
     * Processes the rest standard web request.
     *
     * @param action       Refers the registered web action.
     * @param exchange     Refers the {@code ExchangeContext}.
     * @param routeContext The {@link RouteContext} containing method, action instance, and route configuration.
     * @param isFormData   Indicates whether the received request is of multipart type.
     */
    private void process(final Action action, final HttpServerExchange exchange, final RouteContext routeContext, final boolean isFormData) {
        if (isFormData) {
            exchange.dispatch(formParsingHandler);
            formParsingHandler.setNext(multipartExchange -> process(action, new ExchangeContext(multipartExchange, RequestReadMode.LAZY_READ, true, routeContext)));
        } else {
            exchange.getRequestReceiver().receiveFullBytes((webExchange, message) -> {
                LOGGER.info("Undertow: Request read completed. URL: {}", webExchange.getRequestURL());

                process(action, new ExchangeContext(webExchange, RequestReadMode.EAGER_READ, false, message, routeContext));
            });
        }
    }

    /**
     * Processes the web request
     *
     * @param action          Refers the registered web action.
     * @param exchangeContext Refers the {@code ExchangeContext}.
     */
    private void process(final Action action, final ExchangeContext exchangeContext) {
        final HttpServerExchange exchange = exchangeContext.exchange();
        final UndertowExchange uExchange = new UndertowExchange(exchangeContext);

        LOGGER.info("Undertow: Dispatching request to worker thread. URL: {}", exchange.getRequestURL());

        exchange.dispatch(executor, () -> {

            try {
                final Optional<PreflightHandler> optionalPreflightHandler = PreflightHandlerRegistry.getInstance().get(appName);
                final SessionContext sessionContext = new SessionContext();

                sessionContext.add("domain", getDomain(uExchange));
                SessionContextHolder.getInstance().setContext(sessionContext);

                optionalPreflightHandler.ifPresentOrElse(preflightHandler -> {
                    if (preflightHandler.isAccessAllowed(uExchange, exchangeContext.message(), preflightConfigs)) {
                        action.process(uExchange);
                    } else {
                        uExchange.responseSender().send(Collections.emptyMap(), new byte[0], HttpStatus.FORBIDDEN);
                    }
                }, () -> action.process(uExchange));
            } catch (Throwable throwable) {
                LOGGER.error("Undertow: Request process failed. Reason: {}, URL: {}, ", throwable.getMessage(), exchange.getRequestURL(), throwable);
                exchange.setStatusCode(500).getResponseSender().send("Error occurred");
            } finally {
                SessionContextHolder.getInstance().clear();
            }
        });
    }

    /**
     * Processes the health check request of the application
     *
     * @param exchange Represents the server exchange
     * @param appName  Represents the application name
     */
    private void processHealthCheck(final HttpServerExchange exchange, final String appName) {
        final UndertowExchange undertowExchange = new UndertowExchange(new ExchangeContext(exchange, RequestReadMode.EAGER_READ));
        final JsonObject healthResponse = Json.object();

        healthResponse.put("status", "SUCCESS"); // TODO - It should be replaced with actual health check logic
        healthResponse.put("application", appName);
        healthResponse.put("message", "Application is running smoothly");
        healthResponse.put("timestamp", Instant.now().toEpochMilli());

        undertowExchange.responseSender().send(Collections.emptyMap(), healthResponse.getBytes());
    }

    @Override
    public void unregister(String path) {
        LOGGER.info("Removing web action from path {}", path);
        routingHandler.remove(path);
    }

    /**
     * Prepends the prefix to the web path if it is not already present
     *
     * @param path The web path to be processed
     * @return The web path with the prefix
     */
    private String prependPath(final String pathPrefix, final String path) {
        return pathPrefix.isEmpty() ? path : String.format("%s%s", pathPrefix, path);
    }

    /**
     * Gets the domain of the request
     *
     * @param exchange the exchange to handle the request
     * @return the domain of the request
     */
    private String getDomain(final WebExchange exchange) {
        final String scheme = exchange.header(FORWARDED_PROTOCOL_HEADER).findFirst().orElseGet(exchange::getRequestScheme);

        return String.format("%s%s%s", scheme, "://", exchange.getHostName());
    }
}