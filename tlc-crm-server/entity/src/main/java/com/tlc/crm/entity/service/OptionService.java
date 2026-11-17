package com.tlc.crm.entity.service;

import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.internal.OptionServiceImpl;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;

/**
 * Provides an abstract way to deal with {@link Option}
 *
 * @author Kishore J
 * @version 1.0
 */
public interface OptionService {

    /**
     * <p>
     * Gets the {@link OptionService} instance
     * </p>
     *
     * @return The {@link OptionService}
     */
    static OptionService getInstance() {
        return OptionServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the option for the given org id, field id and option id
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @param id      Represents the option id
     * @return The {@link Option}
     */
    Option get(final long orgId, final long fieldId, final Long id);

    /**
     * <p>
     * Gets the option for the given org id, field info and option name
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param fieldInfo Represents the {@link FieldInfo}
     * @param option    Represents the option name
     * @return The {@link Option}
     */
    Option getOption(final long orgId, final FieldInfo fieldInfo, final String option);

    /**
     * <p>
     * Fetches the {@link Collection} of options for the given organization
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param fieldInfo    Represents the {@link FieldInfo}
     * @param criteria     Which configure the criteria
     * @param i18nResolver Refers to the resolver
     * @param order        Represents ascending or descending order
     * @return The collection of {@link Option}
     */
    Collection<Option> getOptions(final long orgId, final FieldInfo fieldInfo, final DropdownCriteria criteria,
                                  final I18nResolver i18nResolver, final String order);

    /**
     * <p>
     * Fetches a collection of options for the given data
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param fieldInfo   Represents the field info
     * @param optionNames Which contains a collection of option names
     * @return The collection of {@link Option}
     */
    Collection<Option> getOptions(final long orgId, final FieldInfo fieldInfo, final Collection<String> optionNames);

    /**
     * <p>
     * Checks whether the option exists or not for the given org id, field id and option id
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @param id      Represents the option id
     * @return true If the option is exists, otherwise it will return false
     */
    boolean exists(final long orgId, final long fieldId, final Long id);

    /**
     * <p>
     * Checks whether the options exist or not for the given org id, field id and option ids
     * </p>
     *
     * @param orgId     Represents  an organization id
     * @param fieldId   Represents the field id
     * @param optionIds Which contains a collection of option ids
     * @return true If the options exist, otherwise it will return false
     */
    boolean exists(final long orgId, final long fieldId, final Collection<Long> optionIds, final boolean checkEditable);

    /**
     * <p>
     * Checks whether the option exists or not for the given org id, field id and option name
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @param name    Represents the option name
     * @return true If the option is existing, otherwise it will return false
     */
    boolean exists(final long orgId, final long fieldId, final String name);

    /**
     * <p>
     * Fetches a collection of options for the given data
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param source         Represents the source
     * @param fieldAliasName Represents the field alias name
     * @param optionNames    Which contains a collection of option names
     * @return The collection of {@link Option}
     */
    Collection<Option> getOptions(final long orgId, final String source, final String fieldAliasName,
                                  final Collection<String> optionNames);

    /**
     * <p>
     * Retrieves the default options for multi select fields based on the given organization id and {@link FieldInfo}
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param fieldInfo Represents {@link FieldInfo}
     * @return The collection of {@link Option}
     */
    Collection<Option> getDefaultOptions(final long orgId, final FieldInfo fieldInfo);

    /**
     * <p>
     * Retrieves the default option for the dropdown based on the provided organization id and {@link FieldInfo}
     * </p>
     *
     * @param orgId     Represents an org id
     * @param fieldInfo Represents the {@link FieldInfo}
     * @return The {@link Option}
     */
    Option getDefaultOption(final long orgId, final FieldInfo fieldInfo);

    /**
     * <p>
     * Checks if non-editable options are among the specified options ids for a given organization id and field id
     * </p>
     *
     * @param orgId   Represents organization id
     * @param fieldId Represents the field id
     * @param ids     Which contains collection of option ids
     * @return true If the option ids exist, otherwise it will return false
     */
    boolean checkNonEditableOptionsForIds(final long orgId, final long fieldId, final Collection<Long> ids);

    /**
     * <p>
     * Fetches a collection of option based on the provided organization id and {@link FieldInfo}
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param fieldInfo Represents {@link FieldInfo}
     * @return The collection of {@link Option}
     */
    Collection<Option> get(final long orgId, final FieldInfo fieldInfo);

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
    Option getDefaultOrFindFirst(final long orgId, final FieldInfo fieldInfo);
}
