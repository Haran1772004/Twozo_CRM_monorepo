package com.tlc.components.starter.initializer;

import com.tlc.commons.model.TlcEnum;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.service.registry.auto.service.AutoService;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistrar;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistry;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.JavaUtil;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.scanner.ClassScanner;
import com.tlc.components.starter.scanner.ResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract class for module initializers, providing common functionality and structure for initializing modules.
 * This abstract class defines common behavior and dependencies required for initializing modules.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractModuleInitializer implements ModuleInitializer {

    // Example URL: jar:file:/path/to/your-app.jar/!BOOT-INF/lib/dependency-artifactId-version.jar!/
    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile("/!BOOT-INF/lib/([^/]+?)-\\d+(\\.\\d+)*(-SNAPSHOT)?\\.jar!/");

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractModuleInitializer.class);

    @Autowired
    protected ClassScanner classScanner;
    @Autowired
    protected ResourceScanner resourceScanner;

    private final ConfHolder confHolder;
    private final ModuleRegistry moduleRegistry;

    protected AbstractModuleInitializer() {
        this.confHolder = ConfHolder.getInstance();
        this.moduleRegistry = ModuleRegistry.getInstance();
    }

    /**
     * Retrieves the necessary properties essential for initializing a module identified by its name.
     *
     * @param name The name of the module for which properties are to be retrieved.
     * @return A map containing the properties defined for the given module name.
     */
    protected final Configurations getProperties(final String name) {
        return confHolder.getProperties(name);
    }

    /**
     * Categorizes the collection of annotated classes into a structured map, organizing them based on
     * their respective module associations. This enables the seamless integration of their services into
     * the corresponding module containers, allowing for efficient execution of their designated tasks.
     *
     * @param classes The collection of classes to be grouped.
     * @return A map containing modules as keys and collections of classes associated with each module as values.
     */
    protected final Map<Modules, Collection<Class<?>>> segregateClassesByModule(final Collection<Class<?>> classes) {
        final Map<Modules, Collection<Class<?>>> groupedRegisteredClasses = new HashMap<>();
        final Collection<Modules> modules = moduleRegistry.getModules();

        for (final Class<?> clazz : classes) {

            for (final Modules module : modules) {

                if (clazz.getName().startsWith(module.getClassNamePrefix())) {
                    groupedRegisteredClasses.computeIfAbsent(module, k -> new ArrayList<>()).add(clazz);
                    break;
                }
            }
        }

        return groupedRegisteredClasses;
    }

    /**
     * Categorizes the collection of resources into a structured map, organizing them based on
     * their respective module associations enables integration into the corresponding module containers.
     *
     * @param resources The collection of resources to be grouped.
     * @return A map containing modules as keys and collections of resources associated with each module as values.
     */
    protected final Map<Modules, Collection<Resource>> segregateResourcesByModule(final Collection<Resource> resources) throws IOException {
        final Map<Modules, Collection<Resource>> groupedResourcesByModule = new HashMap<>();
        final Collection<Modules> modules = moduleRegistry.getModules();

        for (final Resource resource : resources) {
            final URL url = resource.getURL();

            for (final Modules module : modules) {
                if (isModuleAssociatedWithURL(module, url)) {
                    groupedResourcesByModule.computeIfAbsent(module, k -> new ArrayList<>()).add(resource);
                    break;
                }
            }
        }

        return groupedResourcesByModule;
    }

    /**
     * Gracefully shuts down the module, releasing any resources or stopping services that were started during initialization.
     * Subclasses should override this method to provide custom shutdown logic.
     */
    public void shutdown() {
        if (isServiceEnabled()) {
            final ServiceRegistry<? extends Service> serviceRegistry = getServiceRegistry();

            if (Objects.nonNull(serviceRegistry)) {
                final Service service = serviceRegistry.get();

                if (Objects.nonNull(service)) {
                    service.stop();
                    serviceRegistry.unregister();
                }
            }
        }
    }

    /**
     * Executes post-initialization tasks subsequent to module initiation, such as loading resource actions, meta-data actions,
     * and verifying annotation registrations.
     *
     * @throws Exception If an error arises during the post-initialization process.
     */
    protected void postInitialize() throws Exception {
    }

    /**
     * Checks whether the given module is associated with the given URL
     *
     * @param module Represents the module
     * @param url    Represents the URL
     * @return true if the module is associated with the given URL, false otherwise
     */
    private boolean isModuleAssociatedWithURL(final Modules module, final URL url) {
        final String artifactId = extractArtifactIdFromUrl(url);

        return Objects.equals(artifactId, module.getArtifactId());
    }

    /**
     * <p>
     * Loads {@link AutoServiceRegistry} impl modules.
     * </p>
     *
     * @param basePackage Refers to base package.
     */
    protected <K extends TlcEnum, V extends AutoService<K>> void loadAutoServiceRegistry(final String basePackage) {

        final Map<Class<? extends AutoServiceRegistry<K, V>>, Collection<Class<V>>> grouped =
                getGroupedAutoServiceRegistrar(basePackage);

        for (Map.Entry<Class<? extends AutoServiceRegistry<K, V>>, Collection<Class<V>>> entry : grouped.entrySet()) {
            final Class<? extends AutoServiceRegistry<K, V>> registryClass = entry.getKey();
            final Collection<Class<V>> serviceClasses = entry.getValue();
            final Optional<? extends AutoServiceRegistry<K, V>> optionalServiceRegistry = JavaUtil.getInstance(registryClass);

            if (optionalServiceRegistry.isPresent()) {
                final AutoServiceRegistry<K, V> serviceRegistry = optionalServiceRegistry.get();

                for (final Class<V> serviceClass : serviceClasses) {
                    JavaUtil.getInstance(serviceClass).ifPresent(
                            serviceInstance -> serviceRegistry.put(serviceInstance.getType(), serviceInstance));
                }
            }
        }
    }

    /**
     * <p>
     * Gets grouped {@link AutoServiceRegistry} as key and {@link AutoService} as value.
     * </p>
     *
     * @param basePackage Package's name.
     * @param <K>         {@link AutoServiceRegistry}.
     * @param <V>         {@link AutoService}.
     * @return grouped {@link AutoServiceRegistry} as key and {@link AutoService} as value.
     */
    public <K extends TlcEnum, V extends AutoService<K>> Map<Class<? extends AutoServiceRegistry<K, V>>,
            Collection<Class<V>>> getGroupedAutoServiceRegistrar(final String basePackage) {
        final Set<Class<?>> annotatedClasses = classScanner.findClassesWithAnnotation(basePackage, AutoServiceRegistrar.class);
        final Map<Class<? extends AutoServiceRegistry<K, V>>, Collection<Class<V>>> grouped = new HashMap<>();

        for (final Class<?> clazz : annotatedClasses) {

            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            } else if (!AutoService.class.isAssignableFrom(clazz)) {
                continue;
            }

            final AutoServiceRegistrar annotation = clazz.getAnnotation(AutoServiceRegistrar.class);
            @SuppressWarnings("unchecked")
            final Class<? extends AutoServiceRegistry<K, V>> registryClass =
                    (Class<? extends AutoServiceRegistry<K, V>>) annotation.registryClazz();

            grouped.computeIfAbsent(registryClass, k -> new ArrayList<>()).add(JavaUtil.uncheckedCast(clazz));
        }

        return grouped;
    }

    /**
     * <p>
     * Checks whether the given service is present in enabled service list
     * </p>
     *
     * @param serviceName Represents the service name
     * @return true, if present
     */
    protected boolean isServiceEnabled(final String serviceName) {
        return confHolder.getProperties(ComponentsStarterFields.SERVICES).containsValue(serviceName);
    }

    /**
     * Determines whether the service managed by this module initializer is enabled.
     * <p>
     * Subclasses must implement this to indicate whether their corresponding service
     * should participate in lifecycle operations such as {@link #shutdown()}.
     * </p>
     *
     * @return {@code true} if the service is enabled and active, {@code false} otherwise
     */
    protected abstract boolean isServiceEnabled();

    /**
     * Returns the {@link ServiceRegistry} that holds the singleton service instance
     * managed by this module initializer.
     * <p>
     * This is used by the {@link #shutdown()} method to retrieve, stop, and unregister
     * the service during graceful shutdown.
     * </p>
     *
     * @return the service registry for this module's service
     */
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return null;
    }

    /**
     * Extracts the artifact ID from the given resource URL
     *
     * @param url Represents the resource URL
     * @return the extracted artifact ID
     */
    private String extractArtifactIdFromUrl(final URL url) {
        final String urlStr = url.toString();

        final Matcher matcher = ARTIFACT_ID_PATTERN.matcher(urlStr);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}