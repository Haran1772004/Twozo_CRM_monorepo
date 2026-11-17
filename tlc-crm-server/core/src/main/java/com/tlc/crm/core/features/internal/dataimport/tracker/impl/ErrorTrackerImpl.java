package com.tlc.crm.core.features.internal.dataimport.tracker.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeUtil;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Collection;
import java.util.Collections;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the implementation for the services offered by the {@link ErrorTracker}
 *
 * @author sundar
 * @version 1.0
 */
public class ErrorTrackerImpl implements ErrorTracker {

    private static final String COMMA_SEPARATOR = ", ";
    private final Map<Integer, Collection<String>> errorMessagesMap;
    private final I18nResolver i18nResolver;
    private final long importId;

    public ErrorTrackerImpl(final Locale locale, final Long importId) {
        this.importId = importId;
        this.errorMessagesMap = new ConcurrentHashMap<>();
        this.i18nResolver = I18nServiceProvider.get().getResolver(locale);
    }

    /**
     * {@inheritDoc}
     *
     * @return the import ID
     */
    @Override
    public long getImportId() {
        return importId;
    }

    /**
     * {@inheritDoc}
     *
     * @param index Represents the index where the error occurred
     * @param error Represents the error message
     */
    @Override
    public void track(final int index, final String error) {

        if (Objects.isNull(error)) {
            return;
        }
        errorMessagesMap.computeIfAbsent(index, indexKey -> new HashSet<>()).add(error);
    }

    /**
     * {@inheritDoc}
     *
     * @param index  Represents the index where the error occurred
     * @param errors Represents the error message
     */
    @Override
    public void track(final int index, final Collection<String> errors) {

        if (Objects.isNull(errors) || errors.isEmpty()) {
            return;
        }
        errorMessagesMap.computeIfAbsent(index, indexKey -> new HashSet<>()).addAll(errors);
    }

    /**
     * {@inheritDoc}
     *
     * @return A {@link Collection} of indices containing errors
     */
    @Override
    public Collection<Integer> getInvalidIndices() {
        return errorMessagesMap.isEmpty() ? Collections.emptySet() : errorMessagesMap.keySet();
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the tracker contains any errors otherwise false
     */
    @Override
    public boolean hasErrors() {
        return !errorMessagesMap.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @param index The index that holds the error(s)
     * @return The error message
     */
    @Override
    public String getMessage(final int index) {

        if (errorMessagesMap.containsKey(index)) {
            return String.join(COMMA_SEPARATOR, resolveI18nErrorMessages(errorMessagesMap.get(index)));
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param index  Represents the index where the error occurred
     * @param source Represents the {@link Source}
     * @param error  Represents the error message
     */
    @Override
    public void track(final int index, final Source source, final String error) {
        errorMessagesMap.computeIfAbsent(index, indexKey -> new HashSet<>()).add(String.format("%s-%s", source, i18nResolver.get(error)));
    }

    /**
     * {@inheritDoc}
     *
     * @param index          Represents the index where the error occurred
     * @param throwable      Represents the {@link Throwable} that has occurred
     * @param defaultMessage Represents the default error message should be added
     */
    @Override
    public void track(final int index, final Throwable throwable, final String defaultMessage) {
        final Optional<ErrorCode> optionalErrorCode = ErrorCodeUtil.optErrorCode(throwable);
        final String message = optionalErrorCode.isPresent()
                ? ErrorCodeUtil.extractErrorMessage(optionalErrorCode.get())
                : defaultMessage;

        track(index, message);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceModelContainer is a repository that encompasses all the model values extracted from the source record container Which contains the {@link Collection} of source models grouped by each row index
     */
    @Override
    public void invalidateIndices(final SourceModelContainer sourceModelContainer) {
        final Collection<Source> sources = sourceModelContainer.sources();

        for (final Integer invalidIndex : getInvalidIndices()) {

            for (final Source source : sources) {
                final Collection<Integer> mergedIndices = sourceModelContainer.getMergedIndices(source, invalidIndex);

                if (Objects.nonNull(mergedIndices)) {
                    final Collection<String> errors = errorMessagesMap.get(invalidIndex);

                    errors.add("i18n.import.merge.record.skipped.reason");
                    mergedIndices.forEach(mergedIndex -> track(mergedIndex, Collections.unmodifiableCollection(errors)));
                }
                final Collection<Integer> updatedIndices = sourceModelContainer.getUpdatedIndices(source, invalidIndex);

                if (Objects.nonNull(updatedIndices)) {
                    final Collection<String> errors = errorMessagesMap.get(invalidIndex);

                    errors.add("i18n.import.update.record.skipped.reason");
                    updatedIndices.forEach(mergedIndex -> track(mergedIndex, Collections.unmodifiableCollection(errors)));
                }
            }
        }
    }

    /**
     * <p>
     * Resolves i18n messages for a collection of errors
     * </p>
     *
     * @param errors Represents error messages
     * @return The collection of error messages
     */
    private Collection<String> resolveI18nErrorMessages(final Collection<String> errors) {
        final Collection<String> errorMessages = new HashSet<>();

        for (final String error : errors) {
            errorMessages.add(i18nResolver.get(error));
        }

        return errorMessages;
    }
}
