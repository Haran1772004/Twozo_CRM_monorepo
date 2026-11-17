package com.tlc.crm.common.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.service.FieldPropertiesService;
import com.tlc.crm.common.service.DateTimeFieldsResolver;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Converts the {@link DateTime} field in {@link TlcModel}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class DateTimeFieldsResolverImpl implements DateTimeFieldsResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateTimeFieldsResolverImpl.class);

    private final FieldPropertiesService fieldPropertiesService;

    private static class Instance {
        private static final DateTimeFieldsResolverImpl INSTANCE = new DateTimeFieldsResolverImpl();
    }

    private DateTimeFieldsResolverImpl() {
        fieldPropertiesService = FieldPropertiesService.getInstance();
    }

    /**
     * <p>
     * Gets the {@link DateTimeFieldsResolverImpl}.
     * </p>
     *
     * @return {@link DateTimeFieldsResolverImpl}.
     */
    public static DateTimeFieldsResolverImpl getInstance() {
        return DateTimeFieldsResolverImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Parses the {@link DateTime} fields.
     * </p>
     *
     * @param model {@link TlcModel}.
     */
    @Override
    public void resolve(final TlcModel model) {
        resolve(model, fieldPropertiesService.getFieldsByType(model.getClass(), DateTime.class));
    }

    /**
     * <p>
     * Parses the {@link DateTime} fields for given fields.
     * </p>
     *
     * @param model      {@link TlcModel}.
     * @param fieldNames Field names in the model.
     */
    @Override
    public void resolve(final TlcModel model, final String... fieldNames) {
        resolve(model, fieldPropertiesService.getFieldsByTypeAndName(model.getClass(), DateTime.class, Arrays.stream(fieldNames).toList()));
    }

    /**
     * <p>
     * Resolves the {@link DateTime} fields in a model.
     * </p>
     *
     * @param model  {@link TlcModel}
     * @param fields Collection of {@link Field}.
     */
    private void resolve(final TlcModel model, final Collection<Field> fields) {
        final TemporalService temporalService = TemporalServiceProvider.get();
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();

        for (final Field field : fields) {

            try {
                field.setAccessible(true);
                final DateTime dateTime = (DateTime) field.get(model);

                if (Objects.nonNull(dateTime)) {
                    field.set(model, new DateTime(temporalService.convertUtcToLocalTime(dateTime.getTimeStamp(), timezoneInfo), timezoneInfo));
                }
            } catch (IllegalAccessException exception) {
                LOGGER.error("DateTime field conversion failed, Message: {}, Org ID: {}, User ID: {}", exception.getMessage(), model.orgId(), crmUser.id());
                LOGGER.debug("DateTime field conversion Stack : ", exception);
                throw ErrorCode.get(CommonErrorCodes.TIMEZONE_CONVERSION_FAILED, "i18n.common.timezone.conversion.failed");
            } finally {
                field.setAccessible(false);
            }
        }
    }
}
