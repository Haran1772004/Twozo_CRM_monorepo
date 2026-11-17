package com.tlc.mcp.internal.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class McpResourceRegistry {

    private final ConcurrentHashMap<String, ResourceRegistration> resources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> bundleUris = new ConcurrentHashMap<>();

    public void register(String bundleName, ResourceRegistration reg) {
        resources.put(reg.uri(), reg);
        bundleUris.computeIfAbsent(bundleName, k -> ConcurrentHashMap.newKeySet()).add(reg.uri());
    }

    public Optional<ResourceRegistration> get(String uri) {
        return Optional.ofNullable(resources.get(uri));
    }

    public List<ResourceRegistration> list() {
        return new ArrayList<>(resources.values());
    }

    public void unregisterBundle(String bundleName) {
        final Set<String> uris = bundleUris.remove(bundleName);
        if (uris != null) {
            uris.forEach(resources::remove);
        }
    }
}
