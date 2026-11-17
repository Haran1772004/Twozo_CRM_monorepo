package com.tlc.crm.core.features.internal.dataimport.model;

import com.tlc.attachment.model.Record;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;

/**
 * A model which contains the data to build the {@link SourceRecord}
 *
 * @param orgId        Represents the organization id
 * @param record       Represents the{@link Record} which contains sheet row data
 * @param timezoneInfo Represents the timezone info of the user
 * @author Maharaja S
 * @version 1.0
 */
public record SourceRecordBuildRequest(long orgId, Record record, TimezoneInfo timezoneInfo) {}
