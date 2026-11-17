package com.tlc.crm.core.features.internal.dataimport.builder.model;

import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines abstract functionalities for constructing the {@link T} model specific to each {@link Source}
 *
 * @param <T> Represents the model for the respective {@link Source}
 * @author sundar
 * @version 1.0
 * @see ModelBuilder
 */
abstract class AbstractModelBuilder<T extends TlcModel> implements ModelBuilder<T> {

    /**
     * Fetches the related {@link Source} model from the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer}
     *
     * @param index                Represents the index
     * @param sourceModelContainer is a repository that encompasses all the model values extracted from the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer} is a repository that encompasses all the model values extracted from the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer}
     * @param source               Represents the source
     * @param relatedSource        Represents the related source to be fetched
     * @return {@link TlcModel} which represents the related source model of provided source
     */
    Optional<TlcModel> getRelatedSourceModel(final int index, final SourceModelContainer sourceModelContainer,
                                             final Source source, final Source relatedSource) {
        final Collection<Integer> mergedIndices = sourceModelContainer.getMergedIndices(source, index);
        final Collection<Integer> updatedIndices = sourceModelContainer.getUpdatedIndices(source, index);

        if (Objects.nonNull(mergedIndices)) {
            return fetchLatestModel(mergedIndices, relatedSource, sourceModelContainer);
        } else if (Objects.nonNull(updatedIndices)) {
            return fetchLatestModel(updatedIndices, relatedSource, sourceModelContainer);
        } else {
            final Integer parentIndex = sourceModelContainer.getParentIndex(index, source);

            if (Objects.nonNull(parentIndex)) {
                final Collection<Integer> mergedIndicesOfParentIndex = sourceModelContainer.getMergedIndices(source, parentIndex);

                if (Objects.nonNull(mergedIndicesOfParentIndex)) {
                    return fetchLatestModel(mergedIndicesOfParentIndex, relatedSource, sourceModelContainer);
                } else {
                    final Collection<Integer> updatedIndicesOfParentIndex = sourceModelContainer.getUpdatedIndices(source, parentIndex);

                    if (Objects.nonNull(updatedIndicesOfParentIndex)) {
                        return fetchLatestModel(updatedIndicesOfParentIndex, relatedSource, sourceModelContainer);
                    }
                }
            } else {
                return Optional.ofNullable(sourceModelContainer.getModel(index, relatedSource));
            }
        }
        return Optional.empty();
    }

    /**
     * Fetches the latest model from the {@link SourceModelContainer} identified using the maximum index
     *
     * @param mergedIndices        A {@link Collection} of merged indices that referred from a main index
     * @param relatedSource        Represents the related source
     * @param sourceModelContainer is a repository that encompasses all the model values extracted from the {@link com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer} is a repository that encompasses all the model values extracted from the source
     * @return the latest {@link TlcModel} of related source
     */
    private Optional<TlcModel> fetchLatestModel(final Collection<Integer> mergedIndices, final Source relatedSource,
                                      final SourceModelContainer sourceModelContainer) {
        final Collection<Integer> indexes = mergedIndices.stream().sorted(Comparator.reverseOrder()).toList();

        for (final int index : indexes) {
            final TlcModel model = sourceModelContainer.getModel(index, relatedSource);

            if (Objects.nonNull(model)) {
                return Optional.of(model);
            }
        }
        return Optional.empty();
    }
}
