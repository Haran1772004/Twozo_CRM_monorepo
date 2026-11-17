package com.tlc.mcp.internal.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class McpToolVersionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpToolVersionRegistry.class);

    private final Map<String, NavigableMap<String, ToolRegistration>> tools = new ConcurrentHashMap<>();
    private final Map<String, ToolRegistration> versionedAliases = new ConcurrentHashMap<>();
    private final Map<String, List<ToolKey>> bundleTools = new ConcurrentHashMap<>();

    private static final Comparator<String> VERSION_ORDER =
            Comparator.comparingInt(v -> {
                try { return Integer.parseInt(v); }
                catch (NumberFormatException e) { return Integer.MAX_VALUE; }
            });

    public void register(String bundleSymbolicName, ToolRegistration registration) {
        tools.computeIfAbsent(registration.name(), k -> new TreeMap<>(VERSION_ORDER))
             .put(registration.version(), registration);

        versionedAliases.put(registration.name() + "_v" + registration.version(), registration);

        bundleTools.computeIfAbsent(bundleSymbolicName, k -> new CopyOnWriteArrayList<>())
                   .add(new ToolKey(registration.name(), registration.version()));

        LOGGER.info("Registered tool '{}' v{} from bundle '{}'",
                registration.name(), registration.version(), bundleSymbolicName);
    }

    public void unregisterBundle(String bundleSymbolicName) {
        final List<ToolKey> keys = bundleTools.remove(bundleSymbolicName);
        if (keys == null) {
            return;
        }
        for (ToolKey key : keys) {
            final NavigableMap<String, ToolRegistration> versions = tools.get(key.name());
            if (versions != null) {
                versions.remove(key.version());
                if (versions.isEmpty()) {
                    tools.remove(key.name());
                }
            }
            versionedAliases.remove(key.name() + "_v" + key.version());
        }
        LOGGER.info("Unregistered {} tool(s) from bundle '{}'", keys.size(), bundleSymbolicName);
    }

    public Optional<ToolRegistration> resolve(String name) {
        final ToolRegistration alias = versionedAliases.get(name);
        if (alias != null) {
            return Optional.of(alias);
        }
        final NavigableMap<String, ToolRegistration> versions = tools.get(name);
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }
        for (ToolRegistration reg : versions.descendingMap().values()) {
            if (!reg.deprecated()) {
                return Optional.of(reg);
            }
        }
        final ToolRegistration fallback = versions.lastEntry().getValue();
        LOGGER.warn("Only deprecated version {} of tool '{}' is available",
                fallback.version(), name);
        return Optional.of(fallback);
    }

    public List<ToolRegistration> listCanonical() {
        final List<ToolRegistration> result = new ArrayList<>();
        for (NavigableMap<String, ToolRegistration> versions : tools.values()) {
            if (versions.isEmpty()) {
                continue;
            }
            boolean found = false;
            for (ToolRegistration reg : versions.descendingMap().values()) {
                if (!reg.deprecated()) {
                    result.add(reg);
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(versions.lastEntry().getValue());
            }
        }
        return result;
    }

    public Map<String, ToolRegistration> listVersionedAliases() {
        return Collections.unmodifiableMap(versionedAliases);
    }

    boolean isEmpty() {
        return tools.isEmpty();
    }
}
