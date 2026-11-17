package com.tlc.web.client.event;

/**
 * Describes an event
 *
 * @param name Name of the event
 * @param data Data to be published
 * @param <T>  Type of data to be published
 * @author Selvakumar
 * @version 1.0
 */
public record Event<T>(String name, T data) {
}
