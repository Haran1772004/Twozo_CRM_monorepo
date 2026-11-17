package com.tlc.crm.core.notification.email.bulk.api.model.metrics;

import java.util.Collection;

/**
 * Holds the information of bulk mail metrics, including their contact details and metrics types.
 *
 * @param data  Represents the data that contains either the contact name or the email address.
 * @param types Represents the {@link Collection} of metrics {@link Metrics.MetricsType}
 * @author KowsalyaSP
 * @version 1.0
 */
public record MetricsInsight(String data, Collection<Metrics.MetricsType> types) {
}
