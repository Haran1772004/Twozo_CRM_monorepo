package com.tlc.crm.entity.service;

import com.tlc.commons.service.registry.auto.service.AutoService;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import java.util.Collection;

/**
 * <p>
 * Pertains to Activity status icon for entities.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface ActivityStatusIconService extends AutoService<FieldKeys> {

    /**
     * <p>
     * Gets the activity status icon type.
     * </p>
     *
     * @param orgId        An organization's id.
     * @param entityId     An entity's id.
     * @param source       {@link Source}.
     * @param timezoneInfo {@link TimezoneInfo}.
     * @return the activity icon type.
     */
    String getIconTypeName(final Long orgId, final Long entityId, final Source source, final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Gets the activity icon type related to the deal.
     * </p>
     *
     * @param activities   Represents the collection of {@link Activity}
     * @param timezoneInfo Represents the {@link TimezoneInfo}
     * @return the activity icon type related to the deal.
     */
    String getIconTypeName(final Collection<Activity> activities, final TimezoneInfo timezoneInfo);
}
