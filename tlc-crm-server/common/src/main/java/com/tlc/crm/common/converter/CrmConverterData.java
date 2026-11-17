package com.tlc.crm.common.converter;

import com.tlc.commons.converter.model.ConverterData;
import com.tlc.crm.common.iam.api.model.CrmUser;

import java.util.Locale;

/**
 * CRM-specific extension of {@link ConverterData} that provides access to the
 * authenticated {@link CrmUser} during model conversion.
 *
 * @param <T> the type of data to be converted
 *
 * @author Sathish
 * @version 1.0
 */
public class CrmConverterData<T> extends ConverterData<T> {

    private final CrmUser user;

    /**
     * Creates an instance with default action and data flags.
     *
     * @param user   the authenticated CRM user
     * @param locale the locale for conversion
     * @param data   the data to be converted
     */
    public CrmConverterData(final CrmUser user, final Locale locale, final T data) {
        super(user.orgId(), user.id(), locale, data);
        this.user = user;
    }

    /**
     * Creates an instance with explicit action and data flags.
     *
     * @param user           the authenticated CRM user
     * @param locale         the locale for conversion
     * @param isUpdateAction {@code true} if this is an update operation
     * @param isPartialData  {@code true} if the data is partial
     * @param data           the data to be converted
     */
    public CrmConverterData(final CrmUser user, final Locale locale, final boolean isUpdateAction,
                            final boolean isPartialData, final T data) {
        super(user.orgId(), user.id(), locale, isUpdateAction, isPartialData, data);
        this.user = user;
    }

    /**
     * Returns the authenticated CRM user associated with this conversion context.
     *
     * @return the {@link CrmUser}
     */
    public CrmUser getUser() {
        return user;
    }
}
