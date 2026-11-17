package com.tlc.pk.export;

import com.tlc.commons.code.ErrorCode;

import com.tlc.pk.export.options.ExportOptions;
import com.tlc.pk.internal.ProductPackageErrorCodes;

import lombok.NonNull;
import lombok.extern.java.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for implementing the {@link Exporter} interface.  This
 * class provides a default implementation of the {@link #doExport(ExportOptions)} (ExportOptions)}
 * method and handles some common setup, leaving the actual export logic to be
 * implemented by concrete subclasses.  This class also enforces a consistent
 * approach to option handling.
 *
 * @param <O> The type of export options used by this exporter.  Must be a subtype
 *            of {@link ExportOptions}.
 */
@Log
public abstract class AbstractExporter<O extends ExportOptions> implements Exporter<O>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AbstractExporter.class );

    /**
     * {@inheritDoc}
     * @param exportOptions The export options to use for the export. Must not be null.
     * @throws ExportException
     */
    @Override
    public final void doExport( final @NonNull O exportOptions ) throws ExportException
    {
        try
        {
            validateAndDoPreExportActions();
            export( exportOptions );
            doPostExportActions();
        }
        catch( Exception exception )
        {
            LOGGER.error( "File Export Service: Failed to export action, Reason:", exception );
            throw ErrorCode.get( ProductPackageErrorCodes.EXPORT_ACTION_FAILED,
                         "i18n.analytics.config.export.action.failed" );
        }
    }

    /**
     * Validates the current object or data and performs necessary pre-export actions.
     * This method calls the {@link #validate()} method to validate the object
     * and the {@link #doPreExportActions()} method to perform any necessary pre-export operations.
     *
     * This method ensures that all necessary validation and setup is done before any export action is initiated.
     */
    private void validateAndDoPreExportActions()
    {
        validate();
        doPreExportActions();
    }

    /**
     * Validates the current object or data.
     * <p>
     * This is an abstract method, and must be implemented by subclasses to provide
     * the specific validation logic required for the object before any operations (like export) are performed.
     * </p>
     * <p>
     * The validation may include checks for missing fields, correct formats, or business logic-specific validation
     * that ensures the object is in a valid state for further processing.
     * </p>
     */
    protected abstract void validate();

    /**
     * Performs necessary actions that should take place before the export operation.
     * <p>
     * Subclasses can override this method to implement their own pre-export actions as needed.
     * </p>
     * <p>
     * The default implementation does nothing, but it can be customized as required.
     * </p>
     */
    protected void doPreExportActions()
    {}

    /**
     * Performs the core logic for exporting the data using the provided export options.
     * <p>
     * This is an abstract method that must be implemented by subclasses to define the actual
     * export logic. The implementation should handle writing the data to the target destination,
     * such as saving to a file, sending to a remote server, or another destination.
     * </p>
     *
     * <p>
     * The method is called after the validation and pre-export actions are performed, ensuring
     * that the data is valid and ready for export. The export process itself will be customized
     * in the subclass implementation of this method.
     * </p>
     *
     * @param exportOptions The export options to use for the export. Must not be {@code null}.
     *                      These options typically contain configuration or parameters for
     *                      the export operation.
     * @throws ExportException If an error occurs during the export process, such as a failure
     *                         to write data to the target destination, invalid export options,
     *                         or any other issue that prevents the export from completing successfully.
     */
    protected abstract void export( final @NonNull O exportOptions ) throws ExportException;

    /**
     * Performs necessary actions that should take place after the export operation.
     * <p>
     * Subclasses can override this method to implement their own post-export actions as needed.
     * </p>
     * <p>
     * The default implementation does nothing, but it can be customized as required.
     * </p>
     */
    protected void doPostExportActions()
    {}

}
