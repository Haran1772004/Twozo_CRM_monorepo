package com.tlc.crm.core.holder;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.validator.TlcModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the {@link AbstractConfigManager} of all sources.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ConfigManagerHolder {

    private final Map<Source, AbstractConfigManager<? extends TlcModel>> configManagerMap;

    private ConfigManagerHolder() {
        this.configManagerMap = new HashMap<>();

        loadManagers();
    }

    private static class Instance {
        private static final ConfigManagerHolder INSTANCE = new ConfigManagerHolder();
    }

    /**
     * Gets the instance of {@link ConfigManagerHolder}.
     *
     * @return {@link ConfigManagerHolder} instance.
     */
    public static ConfigManagerHolder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Loads the {@link AbstractConfigManager} of respective sources.
     */
    private void loadManagers() {
        configManagerMap.putAll(Map.of(Source.COMPANY, CompanyManager.getInstance(),
                Source.CONTACT, ContactManager.getInstance(), Source.DEAL, DealManager.getInstance(),
                Source.PRODUCT, ProductManager.getInstance(), Source.NOTE, SourceNoteManager.getInstance(),
                Source.ACTIVITY, SourceActivityManager.getInstance()));
    }

    /**
     * Gets the {@link AbstractConfigManager} for the source provided.
     *
     * @param source Refers the {@link Source}.
     * @return The {@link AbstractConfigManager} of given source.
     */
    public AbstractConfigManager<? extends TlcModel> getManager(final Source source) {
        return configManagerMap.get(source);
    }
}