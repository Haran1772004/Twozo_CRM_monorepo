package com.tlc.components.starter.conf;

import com.tlc.commons.util.ConfHolder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Responsible for loading the necessary configuration settings
 * essential for initializing the application.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "tlc")
public final class ApplicationConfLoader {

    private final ConfHolder confHolder;

    private ApplicationConfLoader() {
        this.confHolder = ConfHolder.getInstance();
    }

    public void setConfig(final Map<String, Map<String, String>> config) {
        confHolder.loadGroupedConfigs(config);
    }
}
