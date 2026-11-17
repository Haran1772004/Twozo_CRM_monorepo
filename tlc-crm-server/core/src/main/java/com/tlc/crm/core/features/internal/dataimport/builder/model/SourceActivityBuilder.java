package com.tlc.crm.core.features.internal.dataimport.builder.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;

import com.tlc.crm.entity.features.api.activity.model.converter.ActivityConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.validator.TlcModel;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Builder that can construct the {@link ActivityHolder} for importing
 *
 * @author sundar
 * @version 1.0
 * @see AbstractModelBuilder
 */
public class SourceActivityBuilder extends AbstractModelBuilder<ActivityHolder> {

    private static final Source SOURCE = Source.ACTIVITY;

    private final ActivityConverter activityConverter;

    private SourceActivityBuilder() {
        this.activityConverter = ActivityConverter.getInstance();
    }

    private static final class Instance {
        private static final SourceActivityBuilder SOURCE_ACTIVITY_BUILDER = new SourceActivityBuilder();
    }

    /**
     * Gets the instance of {@link SourceActivityBuilder}
     *
     * @return {@link SourceActivityBuilder}
     */
    public static SourceActivityBuilder getInstance() {
        return Instance.SOURCE_ACTIVITY_BUILDER;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataHolder The holder for the properties used to store the model in the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer}
     * @param index      Represents the index.
     * @return The constructed {@link ActivityHolder} using the data from the DataHolder for importing
     */
    @Override
    public Optional<ActivityHolder> build(final DataHolder dataHolder, final int index) {
        final JsonObject activityJson = dataHolder.sourceRecordContainer().get(index).get(SOURCE);

        if (Objects.nonNull(activityJson)) {
            final ActivityHolder activityHolder = new ActivityHolder();

            resolveNoteValue(activityJson);
            activityHolder.setActivity(activityConverter.buildActivity(new ConverterConfig(dataHolder.orgId(),
                    dataHolder.userId(), dataHolder.locale(), false, activityJson, true), activityJson));
            loadRelatedSourceData(dataHolder, activityHolder, index);

            return Optional.of(activityHolder);
        }
        return Optional.empty();
    }

    /**
     * Loads related source data into the provided data and {@link ActivityHolder} based on the specified index.
     *
     * @param dataHolder     Represents the {@link DataHolder}
     * @param activityHolder Holds the {@link ActivityHolder} fields
     * @param index          Represents the index
     */
    private void loadRelatedSourceData(final DataHolder dataHolder, final ActivityHolder activityHolder, final int index) {
        final SourceModelContainer sourceModelContainer = dataHolder.sourceModelContainer();
        final BiConsumer<Source, Consumer<TlcModel>> relatedSources = (source, modelConsumer) -> {
            final Optional<TlcModel> optionalRelatedSourceModel = getRelatedSourceModel(index, sourceModelContainer, SOURCE, source);

            optionalRelatedSourceModel.ifPresent(modelConsumer);
        };

        for (final Source source : sourceModelContainer.sources()) {
            switch (source) {
                case COMPANY -> relatedSources.accept(source, model -> activityHolder.setCompany((Company) model));
                case CONTACT -> relatedSources.accept(source, model -> activityHolder.addContact((Contact) model));
                case DEAL -> relatedSources.accept(source, model -> activityHolder.setDeal((Deal) model));
            }
        }
    }

    /**
     * <p>
     * Resolves the import note value
     * </p>
     *
     * @param activityJson Represents the activity json
     */
    private void resolveNoteValue(final JsonObject activityJson) {
        final String noteKey = ImportFields.NOTE.getName();

        if (activityJson.containsKey(noteKey)) {
            final String note = activityJson.getString(noteKey);
            final JsonObject noteJson = Json.object();

            noteJson.put(ImportFields.CONTENT.getName(), note);

            activityJson.put(noteKey, noteJson);
        }
    }
}
