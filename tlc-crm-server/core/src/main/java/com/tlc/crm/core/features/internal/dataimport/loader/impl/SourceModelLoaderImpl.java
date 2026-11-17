package com.tlc.crm.core.features.internal.dataimport.loader.impl;

import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.core.features.internal.dataimport.loader.SourceModelLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.source.CompanyLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.source.ContactLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.source.DealLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.source.ProductLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.source.SourceActivityLoader;
import com.tlc.crm.core.features.internal.dataimport.loader.source.SourceNoteLoader;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.entity.fields.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Provides the implementations of {@link SourceModelLoader} services
 *
 * @author sundar
 * @version 1.0
 * @see SourceModelLoader
 */
public class SourceModelLoaderImpl implements SourceModelLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceModelLoaderImpl.class);

    private final CompanyLoader companyLoader;
    private final ContactLoader contactLoader;
    private final DealLoader dealLoader;
    private final ProductLoader productLoader;
    private final SourceActivityLoader sourceActivityLoader;
    private final SourceNoteLoader sourceNoteLoader;

    private SourceModelLoaderImpl() {
        this.companyLoader = CompanyLoader.getInstance();
        this.contactLoader = ContactLoader.getInstance();
        this.dealLoader = DealLoader.getInstance();
        this.productLoader = ProductLoader.getInstance();
        this.sourceActivityLoader = SourceActivityLoader.getInstance();
        this.sourceNoteLoader = SourceNoteLoader.getInstance();
    }

    /**
     * Contains the {@link SourceModelLoader} instance
     */
    private static class Instance {
        private static final SourceModelLoader SOURCE_MODEL_LOADER = new SourceModelLoaderImpl();
    }

    /**
     * Gets the instance of {@link SourceModelLoader}
     *
     * @return {@link SourceModelLoader}
     */
    public static SourceModelLoader getInstance() {
        return Instance.SOURCE_MODEL_LOADER;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder   Which holds the data to be converted to {@link com.tlc.validator.TlcModel} and load into the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param errorTracker Tracks the errors that arise while loading the data into the {@link SourceModelContainer}
     */
    @Override
    public void load(final DataHolder dataHolder, final ErrorTracker errorTracker) {
        final Collection<Source> sources = dataHolder.sourceRecordContainer().sources();

        if (!sources.isEmpty()) {
            final long startTime = System.currentTimeMillis();

            for (final Source source : sources) {

                switch (source) {
                    case COMPANY -> companyLoader.load(dataHolder, errorTracker);
                    case CONTACT -> contactLoader.load(dataHolder, errorTracker);
                    case DEAL -> dealLoader.load(dataHolder, errorTracker);
                    case PRODUCT -> productLoader.load(dataHolder, errorTracker);
                    case ACTIVITY -> sourceActivityLoader.load(dataHolder, errorTracker);
                    case NOTE -> sourceNoteLoader.load(dataHolder, errorTracker);
                }
            }
            LOGGER.info("Data import: Conversion of JSON-formatted Excel sheet rows to relevant models took {} milliseconds, Import ID: {}, Org ID: {}", System.currentTimeMillis() - startTime, dataHolder.sourceModelContainer().getImportId(), dataHolder.orgId());
        }
    }

}
