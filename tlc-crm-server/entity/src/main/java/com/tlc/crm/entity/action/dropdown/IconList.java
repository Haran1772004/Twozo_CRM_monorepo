package com.tlc.crm.entity.action.dropdown;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.converter.IconConverter;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.icon.model.Icon;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Manages {@link Icon} related dropdown actions.
 *
 * @author kowsalyaSP
 * @version 1.0
 */
@StaticDropdown(name = "iconList")
public class IconList extends AbstractDropdownAction {
    private final IconService iconService;
    private final IconConverter iconConverter;

    public IconList() {
        iconService = IconService.getInstance();
        iconConverter = IconConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownLimit limit = dropdownRequest.getLimit();
        final Collection<Icon> icons = applyCriteria(iconService.getIcons(), dropdownRequest.getCriteria(), List.of(Icon::getName), Optional.empty());
        final int start = limit.start();
        final long totalRecords = icons.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<Icon> iconList = applyLimit(icons, start, limit.limit());

            iconList.forEach(icon -> optionContainer.addOption(iconConverter.convert(new ConverterConfig(dropdownRequest.getOrgId(),
                    dropdownRequest.getUserId(), dropdownRequest.locale(), false, null), icon))); //Todo need locale to resolve i18nKey
        }

        return optionContainer;
    }
}
