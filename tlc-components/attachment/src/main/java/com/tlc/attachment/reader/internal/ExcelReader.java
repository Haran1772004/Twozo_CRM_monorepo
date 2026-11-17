package com.tlc.attachment.reader.internal;

import com.tlc.attachment.internal.status.AttachmentErrorCodes;
import com.tlc.attachment.io.File;
import com.tlc.attachment.reader.DataReader;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.reader.internal.handler.ExcelSheetHandler;
import com.tlc.commons.code.ErrorCode;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * Provides implementations for parsing XLSX and XLS formatted files and storing their contents into a {@link RecordHolder}
 *
 * @author pandiaraj
 * @version 1.0
 * @see AbstractDataReader
 */
public class ExcelReader extends AbstractDataReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelReader.class);

    private static class INSTANCE {
        private static final ExcelReader INSTANCE = new ExcelReader();
    }

    private ExcelReader() {
        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
    }

    /**
     * Gets the {@link ExcelReader} instance
     *
     * @return {@link DataReader}
     */
    public static DataReader getInstance() {
        return INSTANCE.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param file         Represents the Excel file contains the data to read
     * @param recordHolder Represents the {@link RecordHolder} will be loaded with the read data
     * @param indices      Which contains the specific indices to be read
     * @param start        Indicates the starting index from where the file reading should begin
     * @param limit        Indicates the number of records to be read from the file
     */
    @Override
    void process(final File file, final RecordHolder recordHolder, final Collection<Integer> indices, final Integer start,
                 final Integer limit) {

        try (final OPCPackage opcPackage = OPCPackage.open(file.inputStream())) {
            final XSSFReader xssfReader = new XSSFReader(opcPackage);
            final XMLReader xmlReader = XMLHelper.newXMLReader();
            final Iterator<InputStream> sheetsData = xssfReader.getSheetsData();

            if (sheetsData.hasNext()) {

                try (final InputStream sheetStream = sheetsData.next()) {
                    xmlReader.setContentHandler(new XSSFSheetXMLHandler(xssfReader.getStylesTable(),
                            xssfReader.getSharedStringsTable(), Objects.isNull(indices)
                            ? new ExcelSheetHandler(recordHolder, start, limit) : new ExcelSheetHandler(recordHolder, indices), true));

                    final String fileName = file.name();
                    final long readStartedAt = System.currentTimeMillis();

                    try {
                        xmlReader.parse(new InputSource(sheetStream));
                    } catch (ErrorCode errorCode) {
                        LOGGER.info("{} reading has been completed within the specified limit", fileName);
                    }
                    LOGGER.info("{} reading was completed within a time frame of {} milliseconds", fileName, (System.currentTimeMillis() - readStartedAt));
                    LOGGER.info("{} records have been successfully read from {}", recordHolder.totalRecords(), fileName);
                }
            }
        } catch (InvalidFormatException exception) {
            throw ErrorCode.get(AttachmentErrorCodes.UNSUPPORTED_FILE_FORMAT);
        } catch (IOException | OpenXML4JException | ParserConfigurationException | SAXException exception) {
            throw ErrorCode.get(AttachmentErrorCodes.FILE_READ_FAILED);
        }
    }
}