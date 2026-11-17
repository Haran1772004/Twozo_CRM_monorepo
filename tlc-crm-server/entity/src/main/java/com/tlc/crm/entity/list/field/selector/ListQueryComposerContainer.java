package com.tlc.crm.entity.list.field.selector;

import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.ListQueryComposer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Holds the information about the provider and their additional field selector
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class ListQueryComposerContainer {

    private final Map<Provider, ListQueryComposer> registeredActions;

    private ListQueryComposerContainer() {
        registeredActions = new ConcurrentHashMap<>();
    }

    private static class Instance {
        private static final ListQueryComposerContainer INSTANCE = new ListQueryComposerContainer();
    }

    /**
     * <p>
     * Gets the {@link ListQueryComposerContainer} Instance
     * </p>
     *
     * @return {@link ListQueryComposerContainer}
     */
    public static ListQueryComposerContainer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link ListQueryComposer} for the given provider
     * </p>
     *
     * @param provider Represents the {@link Provider}
     * @return {@link ListQueryComposer}
     */
    public ListQueryComposer get(final Provider provider) {
        return registeredActions.get(provider);
    }

    /**
     * <p>
     * Puts the {@link ListQueryComposer} and their respective {@link Provider}
     * </p>
     *
     * @param provider                 Represents the {@link Provider}
     * @param listQueryComposer Represents the {@link ListQueryComposer}
     */
    public void put(final Provider provider, final ListQueryComposer listQueryComposer) {
        registeredActions.put(provider, listQueryComposer);
    }
}
