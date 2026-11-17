package com.tlc.crm.entity.service.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.StaticOption;
import com.tlc.crm.entity.resolver.OptionResolver;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements the {@link OptionService} provides the functionality for accessing the {@link Option}
 *
 * @author Kishore J
 * @version 1.0
 * @see OptionService
 */
public class OptionServiceImpl implements OptionService {

    private final OptionResolver optionResolver;
    private final FieldService fieldService;

    private OptionServiceImpl() {
        optionResolver = OptionResolver.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    private final static class Instance {
        private static final OptionServiceImpl INSTANCE = new OptionServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link OptionServiceImpl} instance
     * </p>
     *
     * @return The {@link OptionServiceImpl}
     */
    public static OptionServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @param id      Represents the option id
     * @return The {@link Option}
     */
    @Override
    public Option get(final long orgId, final long fieldId, final Long id) {
        final Collection<Option> options = optionResolver.get(orgId, fieldId);

        return Objects.isNull(options) || options.isEmpty() ? null : options.stream().filter(option -> option.id().equals(id))
                .findFirst().orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param fieldInfo  Represents the {@link FieldInfo}
     * @param optionName Represents the option name
     * @return The {@link Option}
     */
    @Override
    public Option getOption(final long orgId, final FieldInfo fieldInfo, final String optionName) {
        validateFieldInfo(fieldInfo);
        final Collection<Option> options = optionResolver.get(orgId, fieldInfo.id());

        return (Objects.isNull(options) || options.isEmpty()) ? null : options.stream()
                .filter(option -> option.getOption().equals(optionName)).findFirst().orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param fieldInfo    Represents the {@link FieldInfo}
     * @param criteria     Which configure the criteria
     * @param i18nResolver Refers to the resolver
     * @param order        Represents ascending or descending order
     * @return The collection of {@link Option}
     */
    @Override
    public Collection<Option> getOptions(final long orgId, final FieldInfo fieldInfo, final DropdownCriteria criteria,
                                         final I18nResolver i18nResolver, final String order) {
        validateFieldInfo(fieldInfo);
        final Collection<Option> options = optionResolver.get(orgId, fieldInfo.id());
        final Function<Option, String> resolveStaticOption = option -> ((StaticOption) option).getOptionName(i18nResolver);
        final Consumer<Option> setValue = option -> {
            if (!option.isEditable()) {
                option.setOption(resolveStaticOption.apply(option));
            }
        };
        final Function<Option, String> getValue = option -> {
            final String data = option.getOption();

            if (option.isEditable()) {
                return data;
            } else {
                final String staticOption = resolveStaticOption.apply(option);

                option.setOption(staticOption);
                return staticOption;
            }
        };

        if (Objects.nonNull(options) && !options.isEmpty()) {
            final Stream<Option> optionStream = AbstractDropdownAction.applyCriteria(options, criteria, List.of(getValue),
                    Optional.of(setValue)).stream();

            return optionStream.sorted(FieldKeys.ASC.getName().equals(order)
                    ? Comparator.comparing(Option::getOrder) : Comparator.comparing(Option::getOrder)
                    .reversed()).toList();
        }

        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @param id      Represents the option id
     * @return true If the option is exists, otherwise it will return false
     */
    @Override
    public boolean exists(final long orgId, final long fieldId, final Long id) {
        final Collection<Option> options = optionResolver.get(orgId, fieldId);

        return Objects.nonNull(options) && !options.isEmpty() && options.stream().anyMatch(option -> option.id().equals(id));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents  an organization id
     * @param fieldId   Represents the field id
     * @param optionIds Which contains a collection of option ids
     * @return true If the options exist, otherwise it will return false
     */
    @Override
    public boolean exists(final long orgId, final long fieldId, final Collection<Long> optionIds, final boolean checkEditable) {
        final Collection<Option> options = optionResolver.get(orgId, fieldId);

        return Objects.nonNull(options) && !options.isEmpty() && options.stream()
                .filter(option -> {
                    if (checkEditable) {
                        return option.isEditable() && optionIds.contains(option.id());
                    } else {
                        return optionIds.contains(option.id());
                    }
                }).count() == optionIds.size();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @param name    Represents the option name
     * @return true If the option is exists, otherwise it will return false
     */
    @Override
    public boolean exists(final long orgId, final long fieldId, final String name) {
        final Collection<Option> options = optionResolver.get(orgId, fieldId);

        return Objects.nonNull(options) && !options.isEmpty() && options.stream().anyMatch(option -> option.getOption().equals(name));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param source         Represents the source
     * @param fieldAliasName Represents the field alias name
     * @param optionNames    Which contains a collection of option names
     * @return The collection of {@link Option}
     */
    @Override
    public Collection<Option> getOptions(final long orgId, final String source, final String fieldAliasName,
                                         final Collection<String> optionNames) {
        final Map<String, FieldInfo> fieldMap = fieldService.groupDefaultFieldsByAliasName(source, fieldAliasName);

        return Objects.isNull(fieldMap) || fieldMap.isEmpty() ? Collections.emptyList() : getOptions(orgId, fieldMap.get(fieldAliasName), optionNames);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param fieldInfo   Represents the field info
     * @param optionNames Which contains a collection of option names
     * @return The collection of {@link Option}
     */
    @Override
    public Collection<Option> getOptions(final long orgId, final FieldInfo fieldInfo, final Collection<String> optionNames) {
        final Collection<Option> options = optionResolver.get(orgId, fieldInfo.id());

        return Objects.isNull(options) || options.isEmpty() ? Collections.emptyList() : options.stream()
                .filter(option -> optionNames.contains(option.getOption())).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param fieldInfo Represents {@link FieldInfo}
     * @return The collection of {@link Option}
     */
    @Override
    public Collection<Option> getDefaultOptions(final long orgId, final FieldInfo fieldInfo) {
        validateFieldInfo(fieldInfo);
        final Collection<Option> options = optionResolver.get(orgId, fieldInfo.id());

        return Objects.isNull(options) || options.isEmpty() ? Collections.emptySet() :
                options.stream().filter(Option::isDefault).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an org id
     * @param fieldInfo Represents the {@link FieldInfo}
     * @return The {@link Option}
     */
    @Override
    public Option getDefaultOption(final long orgId, final FieldInfo fieldInfo) {
        validateFieldInfo(fieldInfo);
        final Collection<Option> options = getDefaultOptions(orgId, fieldInfo);

        return Objects.isNull(options) || options.isEmpty() ? null : options.stream().findFirst().orElse(null);
    }

    /**
     * <p>
     * Retrieves the default {@link Option} for the given organization and {@link FieldInfo}.
     * If no default option is present, the first available option from the collection is returned.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param fieldInfo Represents {@link FieldInfo}
     * @return The default {@link Option} if present; otherwise the first available {@link Option};
     * or {@code null} if no options exist
     */
    public Option getDefaultOrFindFirst(final long orgId, final FieldInfo fieldInfo) {
        validateFieldInfo(fieldInfo);
        final Collection<Option> options = get(orgId, fieldInfo);

        if (Objects.isNull(options) || options.isEmpty()) {
            return null;
        }
        Option first = null;

        for (final Option option : options) {

            if (Objects.isNull(first)) {
                first = option;
            }

            if (option.isDefault()) {
                return option;
            }
        }

        return first;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents organization id
     * @param fieldId Represents the field id
     * @param ids     Which contains collection of option ids
     * @return true If the option ids exist, otherwise it will return false
     */
    @Override
    public boolean checkNonEditableOptionsForIds(final long orgId, final long fieldId, final Collection<Long> ids) {
        final Collection<Option> options = optionResolver.get(orgId, fieldId);

        return Objects.nonNull(options) && !options.isEmpty() && options.stream().filter(option ->
                !option.isEditable()).anyMatch(option -> ids.contains(option.id()));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param fieldInfo Represents {@link FieldInfo}
     * @return The collection of {@link Option}
     */
    @Override
    public Collection<Option> get(final long orgId, final FieldInfo fieldInfo) {
        validateFieldInfo(fieldInfo);
        final Collection<Option> options = optionResolver.get(orgId, fieldInfo.id());

        return Objects.isNull(options) ? Collections.emptyList() : options.stream().sorted(Comparator.comparing(Option::getOrder)).toList();
    }

    /**
     * <p>
     * Validates the configuration of a given {@link FieldInfo}
     * </p>
     *
     * @param fieldInfo Represents the {@link FieldInfo}
     */
    private void validateFieldInfo(final FieldInfo fieldInfo) {
        final StoreType storeType = fieldInfo.storeType();

        if (StoreType.DROPDOWN != storeType && StoreType.MULTI_SELECT != storeType) {
            throw ErrorCode.get(EntityErrorCodes.FIELD_IS_NOT_A_DROPDOWN, "i18n.entity.field.not.dropdown");
        }
        final PresetConfig presetConfig = fieldInfo.presetConfig();
        final DropdownType dropdownType = presetConfig.dropdownType();

        if (dropdownType == null) {
            throw ErrorCode.get(EntityErrorCodes.INVALID_DROPDOWN_TYPE, "i18n.entity.invalid.dropdown.type");
        } else if (DropdownType.HIDDEN_OPTION == dropdownType) {
            throw ErrorCode.get(EntityErrorCodes.UNSUPPORTED_DROPDOWN_TYPE, "i18n.entity.dropdown.type.unsupported");
        }
    }
}
