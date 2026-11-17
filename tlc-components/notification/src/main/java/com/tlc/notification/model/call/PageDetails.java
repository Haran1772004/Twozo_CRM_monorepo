package com.tlc.notification.model.call;

/**
 * Represents pagination details for retrieving phone numbers.
 *
 * <p>
 * This record stores the number type being queried, the URL for fetching the next page,
 * and the maximum number of records per page.
 * </p>
 *
 * @param numberType The type of phone number being paginated.
 * @param pageUrl    The URL for the next page of results (nullable).
 * @param pageLimit  The maximum number of results per page.
 * @author Sathish
 * @version 1.0
 */
public record PageDetails(NumberType numberType, String pageUrl, Integer pageLimit) {

    /**
     * Constructs a {@link PageDetails} instance with a number type and page limit,
     * but without a page URL.
     *
     * @param numberType The type of phone number being queried.
     * @param pageLimit  The maximum number of results per page.
     */
    public PageDetails(final NumberType numberType, final Integer pageLimit) {
        this(numberType, null, pageLimit);
    }
}
