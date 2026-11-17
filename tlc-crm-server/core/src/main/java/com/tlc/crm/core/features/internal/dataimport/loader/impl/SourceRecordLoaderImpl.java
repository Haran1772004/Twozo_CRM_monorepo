package com.tlc.crm.core.features.internal.dataimport.loader.impl;

import com.tlc.attachment.model.Record;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.service.UserPreferenceHolderService;
import com.tlc.crm.core.features.internal.dataimport.ImportFieldService;
import com.tlc.crm.core.features.internal.dataimport.SourceFieldHeaders;
import com.tlc.crm.core.features.internal.dataimport.builder.SourceRecordBuilder;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.SourceRecordLoader;
import com.tlc.crm.core.features.internal.dataimport.model.SourceRecordBuildRequest;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the implementation for {@link SourceRecordLoader} to loads the data rows into the {@link SourceRecordContainer}
 *
 * @author sundar
 * @version 1.0
 * @see SourceRecordLoader
 */
public class SourceRecordLoaderImpl implements SourceRecordLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceRecordLoaderImpl.class);

    private final ImportFieldService importFieldService;
    private final UserPreferenceHolderService userPreferenceHolderService;

    private SourceRecordLoaderImpl() {
        importFieldService = ImportFieldService.getInstance();
        userPreferenceHolderService = UserPreferenceHolderService.get();
    }

    private static class Instance {
        private static final SourceRecordLoader SOURCE_RECORD_LOADER = new SourceRecordLoaderImpl();
    }

    /**
     * Gets the instance of {@link SourceRecordLoader}
     *
     * @return {@link SourceRecordLoader} instance
     */
    public static SourceRecordLoader getInstance() {
        return Instance.SOURCE_RECORD_LOADER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                        Represents the organisation ID
     * @param sourceRecordContainer        To be loaded with the Excel data
     * @param recordHolder                 Which contains the Excel data
     * @param sourceFieldHeadersCollection Which contains the mapping of Excel columns and Entity fields
     * @param errorTracker                 Tracks the errors that arise during formatting and loading the data into the {@link SourceRecordContainer}
     */
    @Override
    public void load(final Long orgId, final SourceRecordContainer sourceRecordContainer, final RecordHolder recordHolder,
                     final Collection<SourceFieldHeaders> sourceFieldHeadersCollection, final ErrorTracker errorTracker) {
        final SourceRecordBuilder sourceRecordBuilder = SourceRecordBuilder.create(sourceRecordContainer,
                getSourceFields(orgId), sourceFieldHeadersCollection, errorTracker);
        final long startTime = System.currentTimeMillis();
        final long importId = errorTracker.getImportId();
        final UserRegionalPreference userRegionalPreference = userPreferenceHolderService.get(orgId, LoggedInUserInfo.getUser().id())
                .get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();

        for (final Map.Entry<Integer, Record> valueEntry : recordHolder.getRecords().entrySet()) {
            final Integer index = valueEntry.getKey();

            LOGGER.debug("Data import: Source record is loading. Import ID: {}, Index: {}", importId, index);
            sourceRecordContainer.put(index, sourceRecordBuilder.build(new SourceRecordBuildRequest(orgId, valueEntry.getValue(), timezoneInfo)));
            LOGGER.debug("Data import: Source record loaded. Import ID: {}, Index: {}", importId, index);
        }
        LOGGER.info("Data import: JSON representation of Excel row data generated in {} milliseconds, Import ID: {}, Org ID: {}", System.currentTimeMillis() - startTime, importId, orgId);
    }

    /**
     * Gets the source fields of organisation
     *
     * @param orgId Represents the organisation ID
     * @return {@link Map} that contains the import field grouped by sources and field ID
     */
    private Map<Source, Map<Long, FieldInfo>> getSourceFields(final Long orgId) {
        final Map<Source, Map<Long, FieldInfo>> sourceFields = new EnumMap<>(Source.class);

        for (final Source source : Source.values()) {
            final Collection<FieldInfo> importFields = importFieldService.getImportableSourceFields(orgId, source);
            final Map<Long, FieldInfo> importFieldMap = sourceFields.computeIfAbsent(source, sourceKey -> new HashMap<>());

            for (final FieldInfo field : importFields) {
                importFieldMap.put(field.id(), field);
            }
        }
        return sourceFields;
    }
}