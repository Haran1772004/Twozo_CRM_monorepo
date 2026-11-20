package com.tlc.components.starter;

import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.components.starter.initializer.ComponentModules;
import com.tlc.components.starter.initializer.ModuleRegistry;
import com.tlc.components.starter.initializer.components.ComponentsInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ComponentScan;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * An abstract class that implements {@code ApplicationRunner} interface serves as a base class for application starters in the Spring framework.
 * Subclasses are expected to override the {@code run} method to provide
 * application-specific initialization logic.
 * <p>
 * Implements {@link DisposableBean} to ensure all started services are gracefully stopped
 * when the application shuts down (Docker stop, Spring context close, or partial boot failure).
 *
 * @author ThalaimalaiPandiyanT
 * @author Sathish
 * @version 1.1
 */
@ComponentScan(basePackages = {"com.tlc.*"})
public abstract class ApplicationStarter implements ApplicationRunner, DisposableBean {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ApplicationStarter.class);

    @Autowired
    private ComponentsInitializer componentsInitializer;

    protected ModuleRegistry moduleRegistry;

    /**
     * Tracks shutdown actions in LIFO order — services started first are stopped last.
     */
    private final Deque<ShutdownAction> shutdownActions = new ArrayDeque<>();

    protected ApplicationStarter() {
        this.moduleRegistry = ModuleRegistry.getInstance();
    }

    /**
     * Application will be initialized only after this method run.
     * This method will be called once all the required beans and spring components are initialized.
     * We can call the loading of meta.xml, data.xml and other application related stuffs inside this method.
     *
     * @param args incoming application arguments.
     * @throws Exception on error.
     */
    @Override
    public void run(final ApplicationArguments args) throws Exception {
        try {
            moduleRegistry.register(List.of(ComponentModules.values()));
            register();

            initComponentServices();
            shutdownActions.push(new ShutdownAction("ComponentServices", componentsInitializer::shutdown));

            init();
            shutdownActions.push(new ShutdownAction("ApplicationModules", this::shutdownModules));

            postInitComponentServices();
            postInit();

            LOGGER.info("Application Initializer: Application has started successfully");
        } catch (Exception exception) {
            LOGGER.error("Application Initializer: Error occurred in the application initialization process. Reason: {}, Stack: ", exception.getMessage(), exception);
            destroy();
            throw exception;
        }
    }

    /**
     * Called by Spring when the application context is closed (Docker stop, SIGTERM, graceful shutdown).
     * Stops all started services in reverse order of initialization.
     */
    @Override
    public void destroy() {
        LOGGER.info("Application Initializer: Shutting down services...");

        while (!shutdownActions.isEmpty()) {
            final ShutdownAction action = shutdownActions.pop();
            final String name = action.name();

            try {
                LOGGER.info("Application Initializer: Stopping '{}'", name);
                action.task().run();
                LOGGER.info("Application Initializer: '{}' stopped successfully", name);
            } catch (Exception exception) {
                LOGGER.error("Application Initializer: Error stopping '{}'. Reason: {}", name, exception.getMessage(), exception);
            }
        }

        LOGGER.info("Application Initializer: All services stopped");
    }

    /**
     * Registers the necessary components or modules.
     * This method should be implemented by subclasses to define
     * the registration logic specific to their use case.
     */
    protected abstract void register();

    /**
     * Initializes the component services by invoking the {@link ComponentsInitializer#initialize()} method.
     * Responsible for initializing various services of the components required by the application.
     * It ensures that the necessary setup and configuration tasks are performed before the application starts.
     *
     * @throws Exception If an error occurs during the initialization process.
     */
    private void initComponentServices() throws Exception {
        componentsInitializer.initialize();
    }

    /**
     * Performs post-initialization tasks for the component services
     * to execute additional setup or configuration steps after the initialization process.
     *
     * @throws Exception If an error occurs during the post-initialization process.
     */
    private void postInitComponentServices() throws Exception {
        componentsInitializer.postInitialize();
    }

    /**
     * Performs application-specific initialization tasks.
     * Subclasses should override this method to provide custom pre-run initialization.
     *
     * @throws Exception If an error occurs during pre-run initialization.
     */
    protected abstract void init() throws Exception;

    /**
     * Performs application-specific tasks after the main run logic.
     * Subclasses should override this method to provide custom post-run actions.
     *
     * @throws Exception If an error occurs during post-run tasks.
     */
    protected abstract void postInit() throws Exception;

    /**
     * Subclasses may override to provide custom module shutdown logic.
     * Called during shutdown in reverse order of initialization.
     */
    protected void shutdownModules() {
    }

    /**
     * Represents a named shutdown action to be executed during application teardown.
     */
    private record ShutdownAction(String name, RunnableTask task) {
    }
}
