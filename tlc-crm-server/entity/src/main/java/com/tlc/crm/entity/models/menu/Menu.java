package com.tlc.crm.entity.models.menu;

import com.tlc.crm.icon.model.Icon;
import com.tlc.i18n.I18nKey;

import java.io.Serializable;
import java.util.Collection;

/**
 * <p>
 * A model that represents {@link Menu}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class Menu implements Serializable {

    private long id;
    private Long orgId;
    private String name;
    private I18nKey displayName;
    private Icon icon;
    private Config config;
    private Collection<Menu> subMenus;
    private Long sourceId;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public Config getConfig() {
        return config;
    }

    public Icon getIcon() {
        return icon;
    }

    public Collection<Menu> getSubMenus() {
        return subMenus;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setDisplayName(final I18nKey displayName) {
        this.displayName = displayName;
    }

    public void setConfig(final Config config) {
        this.config = config;
    }

    public void setIcon(final Icon icon) {
        this.icon = icon;
    }

    public void setSubMenus(final Collection<Menu> subMenus) {
        this.subMenus = subMenus;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(final Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    /**
     * <p>
     * A model that represents {@link Config}
     * </p>
     *
     * @author Kavinila E
     * @version 1.0
     */
    public static class Config implements Serializable {

        private Long id;
        private int order;
        private Collection<Integer> metricsIds;
        private String metrics;

        public Long getId() {
            return id;
        }

        public int getOrder() {
            return order;
        }

        public void setId(final Long id) {
            this.id = id;
        }

        public void setOrder(final int order) {
            this.order = order;
        }

        public Collection<Integer> getMetricsIds() {
            return metricsIds;
        }

        public void setMetricsIds(final Collection<Integer> metricsIds) {
            this.metricsIds = metricsIds;
        }

        public String getMetrics() {
            return metrics;
        }

        public void setMetrics(final String metrics) {
            this.metrics = metrics;
        }

    }
}
