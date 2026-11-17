package com.tlc.crm.core.features.internal.dataimport.tracker;

import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.tracker.impl.ErrorTrackerImpl;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;
import java.util.Locale;

/**
 * Provides services to track the errors throughout the completion of the import process
 *
 * @author sundar
 * @version 1.0
 */
public interface ErrorTracker {

    /**
     * Creates an {@link ErrorTracker}
     *
     * @param importId Represents the import ID
     * @return {@link ErrorTracker}
     */
    static ErrorTracker create(final Long importId) {
        return new ErrorTrackerImpl(Locale.getDefault(), importId);
    }

    /**
     * Creates an {@link ErrorTracker}
     *
     * @param locale Represents {@link Locale}
     * @return {@link ErrorTracker}
     */
    static ErrorTracker create(final Locale locale, final long importId) {
        return new ErrorTrackerImpl(locale, importId);
    }

    /**
     * Gets the import ID
     *
     * @return the import ID
     */
    long getImportId();

    /**
     * Tracks the error that have occurred at the specified index
     *
     * @param index Represents the index where the error occurred
     * @param error Represents the error message
     */
    void track(final int index, final String error);

    /**
     * Tracks the error that have occurred at the specified index
     *
     * @param index  Represents the index where the error occurred
     * @param errors Represents the error message
     */
    void track(final int index, final Collection<String> errors);

    /**
     * Checks whether the tracker contains any errors or not
     *
     * @return true if the tracker contains any errors otherwise false
     */
    boolean hasErrors();

    /**
     * Retrieves the error message contained at the specified index. Multiple errors are delimited by commas
     *
     * @param index The index that holds the error(s)
     * @return The error message
     */
    String getMessage(final int index);

    /**
     * Fetches the {@link Collection} of indices that hold errors
     *
     * @return A {@link Collection} of indices containing errors
     */
    Collection<Integer> getInvalidIndices();

    /**
     * <p>
     * Tracks the error that have occurred at the specified index
     * </p>
     *
     * @param index  Represents the index where the error occurred
     * @param source Represents the {@link Source}
     * @param error  Represents the error message
     */
    void track(final int index, final Source source, final String error);

    /**
     * <p>
     * Tracks the error that have occurred at the specified index
     * </p>
     *
     * @param index          Represents the index where the error occurred
     * @param throwable      Represents the {@link Throwable} that has occurred
     * @param defaultMessage Represents the default error message should be added
     */
    void track(final int index, final Throwable throwable, final String defaultMessage);

    /**
     * Invalidates the indices associated with the existing invalid indices
     *
     * @param sourceModelContainer is a repository that encompasses all the model values extracted from the source record container Which contains the {@link Collection} of source models grouped by each row index
     */
    void invalidateIndices(final SourceModelContainer sourceModelContainer);
}
