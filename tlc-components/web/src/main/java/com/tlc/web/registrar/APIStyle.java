package com.tlc.web.registrar;

/**
 * Represents the different API communication protocols supported by the system.
 * Each protocol defines a specific style of client-server interaction and data exchange.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum APIStyle {

    /**
     * Rest API standard wrapped for our own components.
     */
    INTERNAL,

    /**
     * Resource-based API style built over HTTP using standard methods such as GET, POST, PUT, and DELETE
     */
    REST,
    ;
}
