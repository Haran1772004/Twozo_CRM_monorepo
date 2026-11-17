package com.tlc.attachment.storage.model;

import java.io.InputStream;

/**
 * Holds the object download response.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record ObjectDownloadResponse(InputStream inputStream) {

}
