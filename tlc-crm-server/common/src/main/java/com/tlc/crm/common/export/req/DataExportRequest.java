package com.tlc.crm.common.export.req;

import com.tlc.commons.json.JsonObject;

import java.util.Locale;

/**
 * A model that holds the data export request parameters
 *
 * @author Selvakumar
 * @version 1.0
 */
public record DataExportRequest(Long exportId, String name, String type, long orgId, long userId,
                                JsonObject parameters, Locale locale) {
}
