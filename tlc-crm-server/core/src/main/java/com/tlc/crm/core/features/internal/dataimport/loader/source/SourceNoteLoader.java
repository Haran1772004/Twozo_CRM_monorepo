package com.tlc.crm.core.features.internal.dataimport.loader.source;

import com.tlc.crm.core.features.internal.dataimport.builder.model.ModelBuilder;
import com.tlc.crm.core.features.internal.dataimport.builder.model.SourceNoteBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.AbstractModelLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.loader.source.model.SourceNote;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.entity.fields.source.Source;

import java.util.HashMap;

/**
 * Loader to load the {@link SourceNote} models into the {@link SourceModelContainer}
 *
 * @author sundar
 * @version 1.0
 */
public final class SourceNoteLoader extends AbstractModelLoader<SourceNote> {

    private final SourceNoteBuilder sourceNoteBuilder;

    private SourceNoteLoader() {
        super(Source.NOTE);
        this.sourceNoteBuilder = SourceNoteBuilder.getInstance();
    }

    private static final class Instance {
        private static final SourceNoteLoader INSTANCE = new SourceNoteLoader();
    }

    /**
     * Gets the instance of {@link SourceNoteLoader}
     *
     * @return {@link SourceNoteLoader} instance
     */
    public static SourceNoteLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SourceNoteBuilder}
     */
    @Override
    protected ModelBuilder<SourceNote> getModelBuilder() {
        return sourceNoteBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder   Which holds the data to be converted to {@link SourceNote} and load into the {@link SourceModelContainer}
     * @param errorTracker Tracks the errors that arise while loading the {@link SourceNote} into the {@link SourceModelContainer}
     */
    @Override
    public void load(final DataHolder dataHolder, final ErrorTracker errorTracker) {
        loadModels(dataHolder, dataHolder.sourceRecordContainer().getMergedIndicesWithinTheSheet(source), new HashMap<>(), errorTracker);
    }
}
