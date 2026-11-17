package com.tlc.crm.core.features.internal.dataimport.builder;

import com.tlc.attachment.model.Record;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.FieldHeader;
import com.tlc.crm.core.features.internal.dataimport.SourceFieldHeaders;
import com.tlc.crm.core.features.internal.dataimport.builder.impl.SourceRecordBuilderImpl;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.model.SourceRecordBuildRequest;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Collection;
import java.util.Map;

/**
 * Construct the {@link SourceRecord} that contains the data from an Excel row based on the sources
 *
 * @author sundar
 * @version 1.0
 */
public interface SourceRecordBuilder {

    /**
     * Creates the {@link SourceRecordBuilder} to build the {@link SourceRecord} using the {@link Record}
     *
     * @param sourceRecordContainer        is a repository that encompasses all the JSON values extracted from the source
     * @param sourceFields                 Which contains the collection of import fields grouped by the sources and IDs
     * @param sourceFieldHeadersCollection Which contains the collection of {@link FieldHeader} grouped by the sources
     * @param errorTracker                 Tracks the errors that arise during formatting and loading the data into the {@link SourceRecordContainer}
     * @return {@link SourceRecordBuilder} to build the {@link SourceRecord} using the {@link Record}
     */
    static SourceRecordBuilder create(final SourceRecordContainer sourceRecordContainer, final Map<Source, Map<Long, FieldInfo>> sourceFields,
                                      final Collection<SourceFieldHeaders> sourceFieldHeadersCollection, final ErrorTracker errorTracker) {
        return new SourceRecordBuilderImpl(sourceRecordContainer, sourceFields, sourceFieldHeadersCollection, errorTracker);
    }

    /**
     * Builds the {@link SourceRecord} with field value mapping and {@link Record}
     *
     * @param sourceRecordBuildRequest Represents a request model to build {@link SourceRecord}
     * @return {@link SourceRecord} that contains the collection of {@link JsonObject} grouped by sources
     */
    SourceRecord build(final SourceRecordBuildRequest sourceRecordBuildRequest);
}
