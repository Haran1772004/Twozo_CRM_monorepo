package com.tlc.crm.common.iam.service.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.service.UserComparatorService;
import com.tlc.crm.common.models.SortType;
import com.tlc.l10n.temporal.model.DateTime;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>
 * Represents the {@link UserComparatorServiceImpl} for comparison functionality by {@link CrmUser} fields
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class UserComparatorServiceImpl implements UserComparatorService {

    private static class Instance {
        private static final UserComparatorServiceImpl INSTANCE = new UserComparatorServiceImpl();
    }

    /**
     * <p>
     * Gets the user comparator service instance
     * </p>
     *
     * @return The {@link UserComparatorServiceImpl} instance
     */
    public static UserComparatorServiceImpl getInstance() {
        return Instance.INSTANCE;
    }


    /**
     * {@inheritDoc}
     *
     * @param jsonObject Holds the sort {@link Json} details
     * @param locale     Represents a locale.
     * @return The {@link CrmUser} comparator
     */
    @Override
    public Comparator<CrmUser> getUserComparator(final JsonObject jsonObject, final Locale locale) {
        final SortType ascendingType = SortType.ASCENDING;
        final String field = jsonObject.getString(CrmUserFields.FIELD.getName());
        final SortType type = SortType.getSortType(jsonObject.getString(CrmUserFields.TYPE.getName()));
        final Supplier<Collator> collator = () -> Collator.getInstance(locale);

        return switch (field) {
            case "name" -> {
                final Comparator<CrmUser> comparator = Comparator.comparing(CrmUser::getFirstName, Comparator.nullsLast(collator.get()))
                        .thenComparing(CrmUser::getLastName, Comparator.nullsLast(collator.get()));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "state" -> {
                final Comparator<CrmUser> comparator = Comparator.comparing(CrmUser::getState);

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "email" -> {
                final Comparator<CrmUser> comparator = Comparator.comparing(CrmUser::getEmail, Comparator.nullsLast(collator.get()));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "role" -> {
                final Comparator<CrmUser> comparator = Comparator.comparing(CrmUser::getRole, Comparator.nullsLast(
                                Comparator.comparing(CrmRole::getName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "reportingManager" -> {
                final Comparator<CrmUser> comparator = Comparator.comparing(CrmUser::getReportingManager,
                        Comparator.nullsLast(Comparator.comparing(CrmUser::getFirstName, Comparator.nullsLast(collator.get()))
                                        .thenComparing(CrmUser::getLastName, Comparator.nullsLast(collator.get()))));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            case "lastLogin" -> {
                final Comparator<CrmUser> comparator = Comparator.comparing(crmUser -> {
                    final DateTime lastLoginTime = crmUser.getLastLoginTime();

                    return Objects.isNull(lastLoginTime) ? null : lastLoginTime.getTimeStamp();
                }, Comparator.nullsLast(Comparator.naturalOrder()));

                yield ascendingType.equals(type) ? comparator : comparator.reversed();
            }
            default -> throw ErrorCode.get(IamErrorCodes.FIELD_IS_INVALID, "i18n.common.field.is.invalid");
        };
    }
}
