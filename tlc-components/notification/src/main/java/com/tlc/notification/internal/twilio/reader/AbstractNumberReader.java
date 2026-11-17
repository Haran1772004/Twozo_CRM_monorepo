package com.tlc.notification.internal.twilio.reader;

import com.tlc.notification.model.call.PageDetails;
import com.twilio.base.Page;
import com.twilio.base.Reader;
import com.twilio.base.Resource;
import com.twilio.base.ResourceSet;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.Domains;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Abstract implementation of the {@link NumberReader} interface to fetch available location details
 * from the Twilio provider.
 * <p>
 * This class provides reusable methods for handling paginated API responses and loading resource details.
 * Concrete implementations should extend this class and implement the required logic for fetching
 * specific location-based details such as available states, localities, and numbers.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see NumberReader
 */
public abstract class AbstractNumberReader implements NumberReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNumberReader.class);

    protected final TwilioRestClient twilioRestClient;

    public AbstractNumberReader(final TwilioRestClient twilioRestClient) {
        this.twilioRestClient = twilioRestClient;
    }

    /**
     * Retrieves the URL of the next page of API results, if available.
     *
     * @param numberPage The current page of results.
     * @return The next page URL as a string, or {@code null} if there is no next page.
     */
    protected final String getNextPageUrl(final Page<?> numberPage) {
        return numberPage.hasNextPage() ? numberPage.getNextPageUrl(Domains.NUMBERS.toString()) : null;
    }

    /**
     * Loads resource details from a Twilio API response and manages pagination.
     * <p>
     * This method handles retrieving resource details from Twilio's API, either as a full dataset
     * (if no pagination is required) or using paginated requests.
     * </p>
     *
     * @param <T>                   The type of resource being fetched, extending {@link Resource}.
     * @param country               Represents the name of the country.
     * @param pageDetails           The pagination details, including limit and next page URL.
     * @param numberReader          The Twilio {@link Reader} instance used for fetching data.
     * @param resourceDetailsLoader A consumer that processes each retrieved resource.
     * @param nextPageUrlLoader     A consumer that stores the next page URL for pagination.
     */
    protected final <T extends Resource> void loadResourceDetails(final String country, final PageDetails pageDetails,
                                                                  final Reader<T> numberReader,
                                                                  final Consumer<T> resourceDetailsLoader,
                                                                  final Consumer<String> nextPageUrlLoader) {
        final Integer pageLimit = pageDetails.pageLimit();

        try {
            if (Objects.isNull(pageLimit)) {
                final ResourceSet<T> resourceSet = numberReader.read(twilioRestClient);

                resourceSet.forEach(resourceDetailsLoader);
            } else {
                final String nextPageUrl = pageDetails.pageUrl();

                numberReader.pageSize(pageLimit);
                final Page<T> page = Objects.isNull(nextPageUrl) ? numberReader.firstPage(twilioRestClient)
                        : numberReader.getPage(nextPageUrl, twilioRestClient);

                page.getRecords().forEach(resourceDetailsLoader);

                if (page.hasNextPage()) {
                    nextPageUrlLoader.accept(page.getNextPageUrl(Domains.NUMBERS.toString()));
                }
            }
        } catch (final Exception exception) {
            LOGGER.info("Voice Call: Phone Numbers is unavailable for the country - {}", country);
        }
    }
}
