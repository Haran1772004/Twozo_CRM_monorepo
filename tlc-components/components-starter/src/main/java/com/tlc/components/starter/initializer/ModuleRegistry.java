package com.tlc.components.starter.initializer;

import com.tlc.commons.AtomicCounter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * A thread-safe registry for managing and storing modules within the system.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.1
 */
public final class ModuleRegistry {

    private final Collection<Modules> modules;
    private final AtomicCounter idIncrementer;

    private ModuleRegistry() {
        this.modules = Collections.synchronizedSet(new LinkedHashSet<>());
        this.idIncrementer = new AtomicCounter(1);
    }

    private static class InstanceHolder {
        private static final ModuleRegistry MODULE_REGISTRY = new ModuleRegistry();
    }

    public static ModuleRegistry getInstance() {
        return InstanceHolder.MODULE_REGISTRY;
    }

    /**
     * Registers a collection of modules into the registry.
     *
     * @param modules The collection of modules to be registered.
     */
    public void register(final Collection<Modules> modules) {
        this.modules.addAll(modules);
    }

    /**
     * Retrieves the collection of registered modules.
     *
     * @return A collection of registered modules.
     */
    public Collection<Modules> getModules() {
        return Collections.unmodifiableCollection(modules);
    }

    /**
     * Generates and returns a unique ID for a module.
     *
     * @return A unique short value representing the module ID.
     */
    public short getId() {
        return (short) idIncrementer.getAndIncrement();
    }
}
