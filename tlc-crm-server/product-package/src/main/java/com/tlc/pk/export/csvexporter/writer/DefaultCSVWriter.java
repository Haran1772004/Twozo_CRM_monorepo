package com.tlc.pk.export.csvexporter.writer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.pk.export.csvexporter.data.Cell;
import com.tlc.pk.export.csvexporter.data.Data;
import com.tlc.pk.export.csvexporter.strategy.CSVStrategy;
import com.tlc.pk.internal.ProductPackageErrorCodes;

import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import java.nio.charset.StandardCharsets;
/**
 * Default implementation of {@link CSVWriter} for writing CSV data.
 *
 * <p>This class extends {@link CSVWriter} and provides a concrete implementation
 * for writing data in CSV format. It formats the data based on the configured
 * {@link CSVWriteOptions} and writes the output to the specified {@link OutputStream}.</p>
 */
@Log
class DefaultCSVWriter implements CSVWriter
{
    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultCSVWriter.class );

    private @NotNull CSVStrategy strategy;
    private static int index = -1;

    @Getter(AccessLevel.PROTECTED)
    private final @NotNull CSVWriteOptions writeOpts;

    DefaultCSVWriter( final @NonNull CSVWriteOptions writeOpts )
    {
        this.writeOpts = writeOpts;
    }

    /**
     * Escapes special characters in a given string to ensure proper CSV formatting.
     * If the string contains the enclosure character, it is doubled to preserve correctness.
     * If the string contains any CSV special characters (delimiter, enclosure, line terminator, or tab),
     * it is enclosed within the enclosure character.
     *
     * @param string       the original string to be processed
     * @param strategy  the CSV strategy defining enclosure, delimiter, line terminator, and tab handling
     * @return the escaped string, properly formatted for CSV output
     */
    private static String findAndEscapeChar( @NonNull String string,
                                             final @NonNull CSVStrategy strategy)
    {
        final String enclosedBy = String.valueOf( strategy.getEnclosedBy() );
        final String doubleEnclosedBy = strategy.getDoubleEnclosedBy();

        if( string.contains( enclosedBy ) )
        {
            string = string.replaceAll( enclosedBy, doubleEnclosedBy );
        }

        if( string.contains( "\n" ) )
        {
            string = string.replace("\n", strategy.getEscapedLinesTerminatedBy() );
        }

        if( string.contains( "\t" ) )
        {
            string = string.replace("\t", strategy.getEscapedTabDelimiter() );
        }

        if( string.contains( String.valueOf( strategy.getDelimiter() ) ) || string.contains( enclosedBy ) ) {

            string = enclosedBy + string + enclosedBy;
        }

        return string;
    }

    /**
     * Writes the data rows to the output stream in CSV format.
     *
     * <p>This method iterates over the {@code Row} collection in the {@code Data} object,
     * processing each {@code Cell<?>} within the rows. Each row is formatted according to
     * the defined {@code CSVStrategy} and written to the configured {@code OutputStream}.</p>
     *
     * <p>Implementations should handle proper delimiter placement, value escaping, and
     * line termination based on the provided CSV strategy.</p>
     *
     * @throws Exception if an error occurs while writing the rows to the output stream.
     */
    protected void writeRows() throws Exception
    {
        final boolean includeHeaders = writeOpts.isIncludeHeaders();

        @Cleanup final Writer writer = new OutputStreamWriter( writeOpts.getOStream(), StandardCharsets.UTF_8 );

        final Data data = writeOpts.getData();

        final Function<Cell<?>, String> cellStringFunction = data.getStringConverter();
        this.strategy = writeOpts.getCsvStrategy();
        final Optional<List<String>> headersData = data.getHeaders();

        if( includeHeaders && headersData.isPresent() )
        {
            final List<String> headers = headersData.get();
            final int totalHeaders = headers.size();
            final String lastHeader = headers.get( totalHeaders - 1);

            CodeBlockExecutor.forEachSafely( headers,
                                             header ->
                                             {
                                                 this.writeString( writer, String.valueOf( header ) );

                                                 if( !lastHeader.equals( header ) )
                                                 {
                                                     writer.write( strategy.getDelimiter() );
                                                 }
                                             });

            writer.write( strategy.getLinesTerminatedBy() );
        }

        CodeBlockExecutor.forEachSafely( data.getRows(), row ->
        {
            final Collection<Cell<?>> cells = row.getCells();
            final int totalCells = cells.size();
            final List<Cell<?>> cellList = cells.stream()
                                                .toList();
            final String lastCell =  cellStringFunction.apply( cellList.get( totalCells - 1 ) );
            final int lastPosition = totalCells -1;

            CodeBlockExecutor.forEachSafely( cells,
                                             cell ->
                                             {
                                                 index++;
                                                 final String cellString = cellStringFunction.apply( cell );
                                                 this.writeString( writer, cellString );

                                                 if( !lastCell.equals( cellString ) || !( lastPosition == index ) )
                                                 {
                                                     writer.write( strategy.getDelimiter() );
                                                 }
                                             } );
            writer.write( strategy.getLinesTerminatedBy() );
            index = -1;
        } );

        writer.flush();
    }

    /**
     * Writes a formatted string to the provided writer, applying necessary escaping and delimiters.
     *
     * <p>This method first escapes special characters in the input string using
     * {@code handleCellValueEscape}, then writes the processed string to the
     * given {@code Writer}. It also appends the delimiter and line termination
     * sequence as defined in the {@code CSVStrategy}.</p>
     *
     * @param writer the writer to which the formatted string is written (must not be {@code null})
     * @param string the string to be written after escaping (must not be {@code null})
     * @throws IOException if an I/O error occurs while writing to the writer
     */
    private void writeString( final @NonNull Writer writer,
                              @NonNull String string) throws IOException
    {
        string = handleCellValueEscape( string );
        writer.write( string );
    }

    /**
     * Escapes special characters in a cell value based on the CSV strategy.
     *
     * <p>This method ensures that the given string is properly escaped by handling occurrences
     * of the delimiter and enclosure characters as defined in the {@code CSVStrategy}.
     * It calls {@code findAndEscapeChar} to process these characters and returns the
     * escaped string.</p>
     *
     * @param string the input string representing the cell value (must not be {@code null})
     * @return the escaped string, ensuring proper CSV formatting
     */
    private String handleCellValueEscape( @NonNull String string )
    {
        return findAndEscapeChar( string, strategy );
    }

    /**
     * {@inheritDoc}
     * @throws CSVWriteException
     */
    @Override
    public void writeCSV() throws CSVWriteException
    {
        try
        {
            writeRows();
        }
        catch( Exception exception)
        {
            LOGGER.error( "File Export Service: Failed to writing CSV file, Reason: ", exception );
            throw ErrorCode.get( ProductPackageErrorCodes.FILE_WRITE_ACTION_FAILED );
        }
    }
}
