package com.tlc.mcp.internal.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class McpPromptRegistry {

    private static final Comparator<String> VERSION_ORDER = Comparator.comparingInt(Integer::parseInt);

    private final ConcurrentHashMap<String, NavigableMap<String, PromptRegistration>> prompts =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String[]>> bundleKeys = new ConcurrentHashMap<>();

    public synchronized void register(String bundleName, PromptRegistration reg) {
        prompts.computeIfAbsent(reg.name(), k -> new TreeMap<>(VERSION_ORDER))
               .put(reg.version(), reg);
        bundleKeys.computeIfAbsent(bundleName, k -> new ArrayList<>())
                  .add(new String[]{reg.name(), reg.version()});
    }

    public Optional<PromptRegistration> resolve(String name) {
        final NavigableMap<String, PromptRegistration> versions = prompts.get(name);
        if (versions == null || versions.isEmpty()) return Optional.empty();
        return Optional.of(versions.lastEntry().getValue());
    }

    public List<PromptRegistration> listAll() {
        final List<PromptRegistration> result = new ArrayList<>();
        for (NavigableMap<String, PromptRegistration> versions : prompts.values()) {
            if (!versions.isEmpty()) {
                result.add(versions.lastEntry().getValue());
            }
        }
        return result;
    }

    public synchronized void unregisterBundle(String bundleName) {
        final List<String[]> keys = bundleKeys.remove(bundleName);
        if (keys == null) return;
        for (String[] pair : keys) {
            final NavigableMap<String, PromptRegistration> map = prompts.get(pair[0]);
            if (map != null) {
                map.remove(pair[1]);
                if (map.isEmpty()) prompts.remove(pair[0]);
            }
        }
    }
}
