package com.tlc.crm.entity.list.model;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * A model that represents the {@link Statistics} details
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class Statistics {

    private final Map<Type, Integer> stats;

    public Statistics() {
        stats = new HashMap<>();
    }

    public void addStatistic(final Type type, final Integer value) {
        stats.put(type, value);
    }

    public Map<Type, Integer> getStats() {
        return stats;
    }

    public enum Type {

        WITH_OPEN_DEALS(1, "i18n.list.summary.type.withOpenDeals.placeholder", (short) 2),
        WITHOUT_DEALS(2, "i18n.list.summary.type.withOutDeals.placeholder", (short) 3),
        WITH_WON_DEALS(3, "i18n.list.summary.type.withWonDeals.placeholder", (short) 4),
        UN_TOUCHED(3, "i18n.list.summary.type.unTouched", (short) 4),
        WON_DEALS(4, "i18n.list.summary.type.wonDeals.placeholder", (short) 3),
        LOST_DEALS(5, "i18n.list.summary.type.lostDeals.placeholder", (short) 4),
        OPEN_DEALS(6, "i18n.list.summary.type.openDeals.placeholder", (short) 2),
        UPCOMING(7, "i18n.activity.status.upcoming", (short) 2),
        COMPLETED(9, "i18n.activity.status.completed", (short) 3),
        OVERDUE(8, "i18n.activity.status.overdue", (short) 4),
        ;

        private final int id;
        private final String displayName;
        private final Short order;

        Type(final int id, final String displayName, final Short order) {
            this.id = id;
            this.displayName = displayName;
            this.order = order;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Short getOrder() {
            return order;
        }
    }
}
