package com.tlc.crm.core.template;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.internal.formatter.ValueFormatter;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.models.type.DateRange;
import com.tlc.crm.entity.models.type.DateTimeRange;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.TimeRange;
import com.tlc.crm.entity.models.type.Range;

import com.tlc.commons.util.TypeChecker;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.Time;
import com.tlc.notification.model.template.PlaceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the services of email placeholder value.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class PlaceHolderValueInterpreter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceHolderValueInterpreter.class);

    private static final String TO = "TO";
    private static final String DATE_FORMAT_PATTERN_MM_DD_YYYY = "MM/dd/yyyy";
    private static final String DATE_FORMAT_PATTERN_DD_MM_YYYY = "dd/MM/yyyy";
    private static final String TIME_FORMAT_PATTERN = "hh:mm:ss a";
    private static final String DATE_TIME_RANGE_FORMAT = "%s, %s %s %s, %s";
    private static final String BLANK_SPACE = " ";
    private static final List<String> PRIMARY_FIELDS = List.of("emails", "phones");

    private final Map<Class<?>, Map<String, Field>> entityReflectionFields;

    private final PlaceHolderFormatter placeHolderFormatter;

    private PlaceHolderValueInterpreter() {
        placeHolderFormatter = PlaceHolderFormatter.getInstance();
        entityReflectionFields = new HashMap<>();

        registerEntityReflectionFields(Deal.class);
        registerEntityReflectionFields(Contact.class);
        registerEntityReflectionFields(Company.class);
    }

    private static class Instance {
        private static final PlaceHolderValueInterpreter INSTANCE = new PlaceHolderValueInterpreter();
    }

    /**
     * Gets instance of {@link PlaceHolderValueInterpreter}.
     *
     * @return {@link PlaceHolderValueInterpreter} instance
     */
    public static PlaceHolderValueInterpreter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Resolves the provided field info and entity and places the appropriate placeHolder value.
     * </p>
     *
     * @param placeHolder  Refers to {@link PlaceHolder}
     * @param fieldInfo    Refers to {@link FieldInfo}
     * @param entity       Refers to {@link Entity}
     * @param timeZone     Represents the user timezone
     * @param entityFields Map of field names to Field objects for the entity class hierarchy
     */
    public void resolve(final PlaceHolder placeHolder, final FieldInfo fieldInfo,
                        final Entity entity, final TimeZone timeZone, final Map<String, Field> entityFields) {
        if (Objects.nonNull(entity)) {

            if (Objects.nonNull(fieldInfo)) {
                final long fieldId = fieldInfo.id();

                if (fieldInfo.isPersistent()) {
                    final Object data = entity.getValueHolder().get(fieldId);

                    placeHolder.setValue(Objects.isNull(data) ? null : placeHolderFormatter.format(fieldInfo, data, timeZone));
                } else {
                    final String fieldName = fieldInfo.aliasName();

                    if (PRIMARY_FIELDS.contains(fieldInfo.aliasName())) {
                        resolvePrimaryFields(placeHolder, fieldInfo.aliasName(), entity, entityFields);
                    } else {
                        final Object value = getFieldValueFromEntity(fieldName, entity, entityFields);

                        placeHolder.setValue(value == null ? null : placeHolderFormatter.format(fieldInfo, value, timeZone));
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Resolves a placeholder field against the root CRM entity, including related contact or company records
     * when the field belongs to another source.
     * </p>
     *
     * @param placeHolder Refers to {@link PlaceHolder}
     * @param fieldInfo   Refers to {@link FieldInfo}
     * @param rootEntity  Root entity (deal, contact, or company) from the template context
     * @param timeZone    Logged-in user timezone
     */
    public void resolve(final PlaceHolder placeHolder, final FieldInfo fieldInfo, final Entity rootEntity, final TimeZone timeZone) {
        if (Objects.isNull(rootEntity) || Objects.isNull(fieldInfo)) {
            return;
        }
        final Entity model = resolveModelForField(rootEntity, fieldInfo);

        if (Objects.isNull(model)) {
            return;
        }
        resolve(placeHolder, fieldInfo, model, timeZone, loadEntityFields(model.getClass()));
    }

    /**
     * <p>
     * Returns the entity instance that owns the field value (e.g. deal's primary contact or linked company).
     * </p>
     *
     * @param entity    Root entity from the template context
     * @param fieldInfo Field metadata including source
     * @return Target entity for value lookup, or {@code null} when the related record is missing
     */
    public Entity resolveModelForField(final Entity entity, final FieldInfo fieldInfo) {
        if (Objects.isNull(entity) || Objects.isNull(fieldInfo) || Objects.isNull(fieldInfo.fieldSource())) {
            return entity;
        }
        final String entityType = entity.getEntityType();
        final String fieldEntityType = fieldInfo.fieldSource().getName();

        return switch (entityType) {
            case Contact.ENTITY_TYPE -> {
                final Contact contact = (Contact) entity;

                yield switch (fieldEntityType) {
                    case Company.ENTITY_TYPE -> contact.getCompany();
                    default -> contact;
                };
            }
            case Deal.ENTITY_TYPE -> {
                final Deal deal = (Deal) entity;

                yield switch (fieldEntityType) {
                    case Contact.ENTITY_TYPE -> deal.getPrimaryContact();
                    case Company.ENTITY_TYPE -> deal.getCompany();
                    default -> deal;
                };
            }
            default -> entity;
        };
    }

    /**
     * <p>
     * Loads and caches the reflection fields for the given entity class.
     * The fields are collected from the class and its parent classes so callers
     * can resolve fields without repeatedly walking the class hierarchy.
     * </p>
     *
     * @param entityClass Entity class whose fields need to be loaded
     * @return Field map keyed by field name
     */
    public Map<String, Field> loadEntityFields(final Class<?> entityClass) {
        return entityReflectionFields.computeIfAbsent(entityClass, this::buildEntityFields);
    }

    /**
     * <p>
     * Registers and caches the reflection fields for the given entity class.
     * </p>
     *
     * @param entityClass Entity class whose fields need to be registered
     */
    private void registerEntityReflectionFields(final Class<?> entityClass) {
        entityReflectionFields.put(entityClass, buildEntityFields(entityClass));
    }

    /**
     * Builds a reflection field map by collecting fields from the given entity
     * class and its parent classes.
     *
     * @param entityClass Entity class whose fields need to be collected
     * @return Field map keyed by field name
     */
    private Map<String, Field> buildEntityFields(final Class<?> entityClass) {
        final Map<String, Field> allFields = new HashMap<>();
        Class<?> current = entityClass;

        while (Objects.nonNull(current)) {

            for (final Field field : current.getDeclaredFields()) {

                if (allFields.putIfAbsent(field.getName(), field) == null) {
                    field.setAccessible(true);
                }
            }
            current = current.getSuperclass();
        }
        return allFields;
    }

    /**
     * <p>
     * Resolves the placeholder value for primary fields based on the given field name. If the entity is not null,
     * this method maps the field to its corresponding primary value and sets it on the placeholder.
     * </p>
     *
     * @param placeHolder  the placeholder to set the resolved value into
     * @param fieldName    the logical field name to resolve
     * @param entity       the entity from which the field value should be retrieved
     * @param entityFields the map of field names to Field objects for the entity class hierarchy
     */
    private void resolvePrimaryFields(final PlaceHolder placeHolder, final String fieldName, final Entity entity, final Map<String, Field> entityFields) {

        if (Objects.nonNull(entity)) {
            final StoreType storeType = switch (fieldName) {
                case "emails" -> StoreType.EMAIL;
                case "phones" -> StoreType.PHONE;
                default -> null;
            };

            if (Objects.nonNull(storeType)) {
                final Object value = getFieldValueFromEntity(getPrimaryField(fieldName), entity, entityFields);

                placeHolder.setValue(Objects.isNull(value) ? null : placeHolderFormatter.format(storeType, value, null));
            }
        }
    }

    /**
     * <p>
     * Retrieves the value of the specified field from the given entity.
     * </p>
     *
     * @param fieldName    the name of the field to retrieve
     * @param entity       the entity instance from which the field value is extracted
     * @param entityFields a map containing field names and their corresponding Field objects for the entity class hierarchy
     * @return the value of the field if found and accessible, otherwise null
     */
    private Object getFieldValueFromEntity(final String fieldName, final Entity entity, final Map<String, Field> entityFields) {
        if (Objects.isNull(entity) || Objects.isNull(fieldName)) return null;

        final Field field = entityFields.get(fieldName);

        if (Objects.isNull(field)) {
            LOGGER.debug("Email Template Placeholder: Field '{}' could not be found in the entity class '{}' or any of its superclasses",
                    fieldName, entity.getClass().getName());

            return null;
        }

        try {
            return field.get(entity);
        } catch (Exception e) {
            LOGGER.debug("Email Template Placeholder: Unable to access the field '{}' in entity class '{}': {}", fieldName, entity.getClass().getName(), e.getMessage());

            return null;
        }
    }

    /**
     * <p>
     * Gets the primary field for respective given field name.
     * </p>
     *
     * @param fieldName Represents the field name
     * @return the primary field names
     */
    private String getPrimaryField(final String fieldName) {
        return switch (fieldName) {
            case "emails" -> "primaryEmail";
            case "phones" -> "primaryPhone";
            default -> null;
        };
    }

    /**
     * Formatter that implements {@link ValueFormatter} inorder to obtain the required value for {@link PlaceHolder} field.
     *
     * @author AswiniN
     * @version 1.0
     */
    private static class PlaceHolderFormatter implements ValueFormatter<FieldInfo, Object, Object> {

        private final BaseCurrencyService baseCurrencyService;

        private PlaceHolderFormatter() {
            baseCurrencyService = BaseCurrencyService.getInstance();
        }

        private static class Instance {
            private static final PlaceHolderFormatter INSTANCE = new PlaceHolderFormatter();
        }

        /**
         * Gets the instance of {@link ValueFormatter}
         *
         * @return {@link ValueFormatter} instance
         */
        public static PlaceHolderFormatter getInstance() {
            return Instance.INSTANCE;
        }

        /**
         * Transforms the value based on the {@link StoreType} as required for {@link PlaceHolder}
         *
         * @param fieldInfo Refers to {@link FieldInfo}
         * @param value     To be formatted
         * @return the formatted value
         */
        @Override
        public Object format(final long orgId, final FieldInfo fieldInfo, final Object value) {
            final StoreType storeType = fieldInfo.storeType();

            return format(storeType, value, null);
        }

        /**
         * Transforms the value based on the {@link StoreType} as required for {@link PlaceHolder}
         *
         * @param fieldInfo Refers to {@link FieldInfo}
         * @param value     To be formatted
         * @param timeZone  Represents the user timezone
         * @return the formatted value
         */
        public Object format(final FieldInfo fieldInfo, final Object value, final TimeZone timeZone) {
            final StoreType storeType = fieldInfo.storeType();

            return format(storeType, value, timeZone);
        }

        /**
         * Formats the provided value based on the given {@link StoreType}.
         *
         * @param storeType Represents the {@link StoreType}
         * @param value     to be formatted
         * @param timeZone  Represents the user timezone
         * @return the formatted value
         */
        public Object format(final StoreType storeType, final Object value, final TimeZone timeZone) {
            if (Objects.isNull(value)) {
                return null;
            }

            return switch (storeType) {

                case MULTI_SELECT -> {
                    if (value instanceof final Collection<?> multiSelectValues) {
                        final Collection<String> options = new ArrayList<>();

                        for (final Object multiSelectValue : multiSelectValues) {

                            if (multiSelectValue instanceof final Option option) {
                                options.add(option.getOption());
                            }
                        }
                        yield String.join(", ", options);
                    }
                    yield null;
                }
                case SINGLE_STRING, TEXT -> TypeChecker.isString(value) ? value : String.valueOf(value);
                case SINGLE_NUMBER -> String.valueOf(value);
                case DATE, TIME, DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE -> formatRangeValues(value, timeZone);
                case MONETARY, DEAL_VALUE -> value instanceof Monetary monetary ? getMonetaryValue(monetary) : null;
                case ADDRESS -> value instanceof final Address address ? getAddress(address) : null;
                case COMPANY, COMPANY_ONBOARD -> (value instanceof final Company company) ? company.getName() : null;
                case CONTACT, CONTACT_ONBOARD ->
                        (value instanceof final Contact contact) ? contact.getFirstName() : null;
                case OWNER -> ((CrmUser) value).getIamId();
                case EMAIL, EMAILS -> {
                    if (value instanceof final Email email) {
                        yield email.getValue();
                    } else if (value instanceof Collection<?> collection) {
                        if (!collection.isEmpty()) {
                            yield collection.stream().findFirst().get() instanceof Email email ? email.getValue() : null;
                        }
                    }
                    yield null;
                }
                case PHONE, PHONES -> {
                    if (value instanceof final Phone phone) {
                        yield phone.getNumber();
                    } else if (value instanceof Collection<?> collection) {
                        if (!collection.isEmpty()) {
                            yield collection.stream().findFirst().get() instanceof Phone phone ? phone.getNumber() : null;
                        }
                    }
                    yield null;
                }
                case DROPDOWN -> {
                    if (value instanceof Pipeline pipeline) {
                        yield pipeline.getName();
                    } else if (value instanceof PipelineStage stage) {
                        yield stage.getName();
                    } else if (value instanceof LifeCycleStage lifeCycleStage) {
                        yield lifeCycleStage.getName();
                    } else if (value instanceof LifeCycleStatus lifeCycleStatus) {
                        yield lifeCycleStatus.getName();
                    }
                    else if (value instanceof Option option) {
                        yield option.getOption();
                    }
                    yield null;
                }
                case TERRITORY -> ((Territory) value).getName();
                default -> null;
            };
        }

        /**
         * <p>
         * Formats the provided range field value
         * </p>
         *
         * @param value    Represents the value
         * @param timeZone Represents the user timezone
         * @return the resolved range values
         */
        private Object formatRangeValues(final Object value, final TimeZone timeZone) {
            final BiFunction<DateFormat, Range<?>, Object> formatterFunction = (formatter, range) ->
                    String.join(
                            BLANK_SPACE, formatter.format(range.getFrom().getTimeStamp()),
                            TO, formatter.format(range.getTo().getTimeStamp())
                    );

            if (value instanceof Date date) {
                return getDateFormat(DATE_FORMAT_PATTERN_DD_MM_YYYY, timeZone).format(date.getTimeStamp());
            } else if (value instanceof Time time) {
                return getDateFormat(TIME_FORMAT_PATTERN, timeZone).format(time.getTimeStamp());
            } else if (value instanceof DateRange range) {
                return formatterFunction.apply(getDateFormat(DATE_FORMAT_PATTERN_DD_MM_YYYY, timeZone), range);
            } else if (value instanceof TimeRange range) {
                return formatterFunction.apply(getDateFormat(TIME_FORMAT_PATTERN, timeZone), range);
            } else if (value instanceof DateTimeRange range) {
                final DateFormat dateFormatter = getDateFormat(DATE_FORMAT_PATTERN_MM_DD_YYYY, timeZone);
                final DateFormat timeFormatter = getDateFormat(TIME_FORMAT_PATTERN, timeZone);
                final long from = range.getFrom().getTimeStamp();
                final long to = range.getTo().getTimeStamp();

                return String.format(
                        DATE_TIME_RANGE_FORMAT, dateFormatter.format(from), timeFormatter.format(from),
                        TO, dateFormatter.format(to), timeFormatter.format(to)
                );
            }

            return null;
        }

        /**
         * <p>
         * Formats the address field provided
         * </p>
         *
         * @param address Represents the {@link Address}
         * @return the required address to be placed in placeholder value
         */
        private String getAddress(final Address address) {
            final Country country = address.getCountry();

            return Stream.of(address.getAddressLine1(), address.getAddressLine2(), address.getCity(), address.getState(),
                    Objects.isNull(country) ? null : country.getName(), address.getZipcode())
                    .filter(Objects::nonNull).map(String::trim).collect(Collectors.joining(BLANK_SPACE));
        }

        /**
         * <p>
         * Fetches the monetary value in a format with currency and value
         * </p>
         *
         * @param monetary Represents the {@link Monetary}
         * @return the required monetary to be placed in placeholder value
         */
        private String getMonetaryValue(final Monetary monetary) {
            return String.format("%s%f", monetary.getCurrency().getSymbol(),
                    baseCurrencyService.applyPrecision(monetary.orgId(), monetary.getValue()));
        }

        /**
         * Parses the pattern provided and results the corresponding date format.
         *
         * @param pattern  Represents the pattern.
         * @param timeZone Represents the user timezone.
         * @return {@link DateFormat} of given patten.
         */
        private DateFormat getDateFormat(final String pattern, final TimeZone timeZone) {
            final SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

            if (Objects.nonNull(timeZone)) {
                dateFormat.setTimeZone(timeZone);
            }

            return dateFormat;
        }
    }
}