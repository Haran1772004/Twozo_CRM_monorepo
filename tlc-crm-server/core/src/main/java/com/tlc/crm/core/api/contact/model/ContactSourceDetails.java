package com.tlc.crm.core.api.contact.model;

/**
 * A record that stores information about the source of the contact.
 *
 * @param twozo           The twozo value.
 * @param contactProvider The provider value.
 * @param total           The total value.
 * @author KarthickV
 * @version 1.0
 */
public record ContactSourceDetails(Long twozo, Long contactProvider, Long total) {

}
