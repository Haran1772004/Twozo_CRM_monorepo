package com.tlc.pk.export.options;

import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * Provides a base implementation of the {@code ExportOptions} interface.
 *
 * <p>This abstract class serves as a foundation for defining common export settings
 * and behaviors. Subclasses can extend this class to provide specific implementations
 * for different export formats such as CSV or PDF.</p>
 *
 * <p>By implementing {@code ExportOptions}, this class ensures that all export options
 * follow a consistent structure while allowing flexibility for customization.</p>
 */
@SuperBuilder
@Accessors(fluent = true)
public class AbstractExportOptions implements ExportOptions
{

    private final int exportType;

    @Override
    public final int getExportType()
    {
        return exportType;
    }

}
