package com.tlc.crm.entity.filter.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.fields.FilterFields;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * <p>
 * Manages filter related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Dropdown(name = "filterList", fieldAlias = "filters")
public class FilterList extends AbstractDropdownAction {

    private static final I18nKey FAVORITES = new I18nKey("i18n.filter.category.favorites");
    private static final I18nKey CREATED_BY_ME = new I18nKey("i18n.filter.category.own");
    private static final I18nKey SHARED_WITH_ME = new I18nKey("i18n.filter.category.shared");
    private static final String FORMAT_SYMBOL = "%s";

    private final FilterManager filterManager;

    public FilterList() {
        filterManager = FilterManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested date interval values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final Long userId = dropdownRequest.getUserId();
        final JsonObject data = dropdownRequest.data();
        final DropdownLimit limit = dropdownRequest.getLimit();
        final I18nResolutionAdapter resolver = dropdownRequest.getI18NResolutionAdapter();
        final Collection<Filter> filters = filterManager.getOrgFilters(orgId, userId, data.getLong(FilterFields.SOURCE_ID.getName()),
                limit, Filter.Category.COMMON);
        final OptionContainer optionContainer = new OptionContainer();

        if (Objects.nonNull(filters) && !filters.isEmpty()) {
            final JsonArray defaultFilters = Json.array();
            final JsonArray myFilters = Json.array();
            final JsonArray sharedFilters = Json.array();
            final JsonArray favoriteFilters = Json.array();
            final String sizeKey = FilterFields.SIZE.getName();
            final String titleKey = FilterFields.TITLE.getName();
            final String optionsKey = FilterFields.OPTIONS.getName();
            final BiConsumer<JsonObject, JsonArray> filterAdder = (filterJson, filterArray) -> filterArray.put(filterJson);
            final BiConsumer<I18nKey, JsonArray> optionsAdder = (title, filterArray) -> {
                final int filterCount = filterArray.size();

                if (filterCount > 0) {
                    final JsonObject filterJson = Json.object();

                    if (Objects.nonNull(title)) {
                        filterJson.put(titleKey, resolver.get(title));
                    }
                    filterJson.put(sizeKey, filterCount);
                    filterJson.put(optionsKey, filterArray);
                    optionContainer.addOption(filterJson);
                }
            };

            for (final Filter filter : filters) {
                final JsonObject filterJson = convertFilterIntoJson(userId, filter, resolver);
                final CrmUser creator = filter.getCreator();

                if (filter.isFavorite()) {
                    favoriteFilters.put(filterJson);
                }
                filterAdder.accept(filterJson, filter.isDefault() ? defaultFilters : Objects.nonNull(creator)
                        && Objects.equals(userId, creator.id()) ? myFilters : sharedFilters);
            }
            optionsAdder.accept(FAVORITES, favoriteFilters);
            optionsAdder.accept(null, defaultFilters);
            optionsAdder.accept(CREATED_BY_ME, myFilters);
            optionsAdder.accept(SHARED_WITH_ME, sharedFilters);
        }

        return optionContainer;
    }

    /**
     * <p>
     * Converts the {@link Filter} into key-value pair.
     * </p>
     *
     * @param userId   Represents the user id.
     * @param filter   Which holds the values of filter.
     * @param resolver Represents the {@link I18nResolutionAdapter}.
     * @return Converted filter.
     */
    private JsonObject convertFilterIntoJson(final Long userId, final Filter filter, final I18nResolutionAdapter resolver) {
        final JsonObject filterJson = Json.object();
        final Filter.Permissions permissions = filter.getPermissions();
        final CrmUser creator = filter.getCreator();
        final String name = filter.getName();
        final boolean isDefault = filter.isDefault();
        final String filterName = isDefault ? resolver.get(name) : name;

        filterJson.put(FilterFields.ID.getName(), filter.id());
        filterJson.put(FilterFields.NAME.getName(), filterName);
        filterJson.put(FilterFields.IS_FAVORITE.getName(), filter.isFavorite());
        filterJson.put(FilterFields.IS_DEFAULT.getName(), isDefault);
        filterJson.put(FilterFields.IS_ACTIVE.getName(), filter.isRecentlyApplied());

        if (Objects.isNull(permissions) || (Objects.nonNull(creator) && Objects.equals(userId, creator.id()))) {
            filterJson.put(FilterFields.IS_EDITABLE.getName(), true);
            filterJson.put(FilterFields.IS_DELETABLE.getName(), true);
            filterJson.put(FilterFields.IS_SHAREABLE.getName(), true);
        } else {
            filterJson.put(FilterFields.IS_EDITABLE.getName(), permissions.isEditable());
            filterJson.put(FilterFields.IS_DELETABLE.getName(), permissions.isDeletable());
            filterJson.put(FilterFields.IS_SHAREABLE.getName(), permissions.isShareable());
        }

        return filterJson;
    }
}
