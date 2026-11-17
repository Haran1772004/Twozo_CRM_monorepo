package com.tlc.crm.entity.models.menu;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.SettingsErrorCodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Holds the home menu configs.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum HomeMenuMetrics {

    DEAL_CLOSING_THIS_WEEK(1, "i18n.menu.home.metrics.deal.closing.week.placeholder", Source.DEAL),
    MY_OPEN_DEALS(2, "i18n.menu.home.metrics.open.deals.placeholder", Source.DEAL),
    NEW_CONTACTS(3, "i18n.menu.home.metrics.new.contacts.placeholder", Source.CONTACT),
    RECENT_CONTACTS(4, "i18n.menu.home.metrics.recent.contacts.placeholder", Source.CONTACT);

    private static final Collection<Integer> IDS;

    private final int id;
    private final String displayName;
    private final Source source;

    static {
        IDS = Arrays.stream(HomeMenuMetrics.values()).map(HomeMenuMetrics::getId).collect(Collectors.toUnmodifiableSet());
    }

    HomeMenuMetrics(final int id, final String displayName, final Source source) {
        this.id = id;
        this.displayName = displayName;
        this.source = source;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getId() {
        return id;
    }

    /**
     * Gets the {@code HomeMenuMetrics} by its ID.
     *
     * @param id Refers the metrics ID.
     * @return The {@code HomeMenuMetrics}.
     */
    public static HomeMenuMetrics getById(final short id) {
        return switch (id) {
            case 1 -> DEAL_CLOSING_THIS_WEEK;
            case 2 -> MY_OPEN_DEALS;
            case 3 -> NEW_CONTACTS;
            case 4 -> RECENT_CONTACTS;
            default -> throw ErrorCode.get(SettingsErrorCodes.MENU_CONFIG_NOT_FOUND);
        };
    }

    /**
     * Gets the ids of the metrics.
     *
     * @return The ids of the metrics.
     */
    public static Collection<Integer> getIds() {
        return IDS;
    }

    /**
     * Gets the source of the metrics.
     *
     * @return The source of the metrics.
     */
    public Source getSource() {
        return source;
    }
}