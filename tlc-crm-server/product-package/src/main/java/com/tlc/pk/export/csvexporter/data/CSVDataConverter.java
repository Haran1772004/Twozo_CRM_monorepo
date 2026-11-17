package com.tlc.pk.export.csvexporter.data;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.i18n.I18nResolver;
import lombok.NonNull;

import java.util.Map;

/**
 * A generic interface for converting data into a {@code Data} object for CSV export.
 *
 * <p>Implementations of this interface define how input data of type {@code T}
 * is transformed into a structured {@code Data} object, containing headers and
 * rows suitable for CSV generation.</p>
 *
 * @param <T> the type of input data to be converted into CSV format
 * @param <M> the type of modular field (e.g., ReportModularField in implementations)
 */
public interface CSVDataConverter<T, M>
{

    /**
     * Converts the given data into a {@code Data} object containing headers and rows in CSV format.
     *
     * <p>This method processes the input of type {@code T} and transforms it into a {@code Data} object
     * that includes a collection of headers and rows, which can be used for generating CSV files
     * or for further processing.</p>
     *
     * @param orgId             the organization ID associated with the data
     * @param data              the input data of type {@code T} to be converted; the structure depends on the implementation
     * @param userMap           a mapping of user IDs to {@code CrmUser} objects, used for resolving user-related data
     * @param modularFieldMap   a map of modular field data (implementation-specific type M)
     * @param i18nResolver      an instance of {@link I18nResolver} for handling internationalization
     * @return a {@code Data} object containing the extracted headers and rows, where {@code headers} represent
     * column names and {@code rows} represent the data entries
     */
    Data convertToCSVRows(final @NonNull Long orgId,
                          final @NonNull T data,
                          final Map<Long, CrmUser> userMap,
                          final @NonNull Map<Long, M> modularFieldMap,
                          final @NonNull I18nResolver i18nResolver);

}
