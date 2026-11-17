package com.tlc.crm.core.features.internal.dataimport.loader;

import com.tlc.attachment.model.Record;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.crm.core.features.internal.dataimport.SourceFieldHeaders;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.impl.SourceRecordLoaderImpl;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;

import java.util.Collection;

/**
 * Loads the Excel row data contained within the {@link RecordHolder} into the {@link SourceRecordContainer}
 *
 * @author sundar
 * @version 1.0
 */
public interface SourceRecordLoader {

    /**
     * Gets the instance of {@link SourceRecordLoader}
     *
     * @return {@link SourceRecordLoader} instance
     */
    static SourceRecordLoader getInstance() {
        return SourceRecordLoaderImpl.getInstance();
    }

    /**
     * Loads the {@link Record} into the {@link SourceRecordContainer} from {@link RecordHolder} by parsing those to appropriate source JSON
     *
     * @param orgId                        Represents the organisation ID
     * @param sourceRecordContainer        To be loaded with the Excel data
     * @param recordHolder                 Which contains the Excel data
     * @param sourceFieldHeadersCollection Which contains the mapping of Excel columns and Entity fields
     * @param errorTracker                 Tracks the errors that arise during formatting and loading the data into the {@link SourceRecordContainer}
     */
    void load(final Long orgId, final SourceRecordContainer sourceRecordContainer, final RecordHolder recordHolder,
              final Collection<SourceFieldHeaders> sourceFieldHeadersCollection, final ErrorTracker errorTracker);
}
