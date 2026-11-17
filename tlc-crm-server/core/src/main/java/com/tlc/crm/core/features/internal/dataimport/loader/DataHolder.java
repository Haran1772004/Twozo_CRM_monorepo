package com.tlc.crm.core.features.internal.dataimport.loader;

import com.tlc.crm.core.features.internal.dataimport.container.AuditLogContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.container.TriggerModelContainer;

import java.util.Locale;

/**
 * Holds the data ie, required frequently to convert and load the source data into the {@link SourceModelContainer}
 *
 * @param orgId                 Represents the organization ID
 * @param userId                Represents the user ID
 * @param locale                Represents the locale
 * @param sourceRecordContainer Which contains the source JSON data to be converted and loaded into the {@link SourceModelContainer}
 * @param sourceModelContainer  is a repository that encompasses all the model values extracted from the {@link SourceRecordContainer}  To be loaded with the converted model
 * @param triggerModelContainer Is a repository that contains the models that are to be triggered
 * @author sundar
 * @version 1.0
 */
public record DataHolder(Long orgId, Long userId, Locale locale, boolean isMergeDuplicatesEnabled, SourceRecordContainer sourceRecordContainer,
                         SourceModelContainer sourceModelContainer, AuditLogContainer auditLogContainer, TriggerModelContainer triggerModelContainer) {

    public DataHolder(final Long orgId, final Long userId, final Locale locale) {
        this(orgId, userId, locale, false, null, null, AuditLogContainer.create(), TriggerModelContainer.create());
    }
}
