package com.tlc.web.undertow;

/**
 * Enum representing the different modes of receiving data during the request handling process.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
enum RequestReadMode {

    /**
     * Represents the request processing continues without waiting for the entire data to be received.
     */
    LAZY_READ,

    /**
     * Represents the request processing waits for data to be fully received before proceeding.
     */
    EAGER_READ;
}
