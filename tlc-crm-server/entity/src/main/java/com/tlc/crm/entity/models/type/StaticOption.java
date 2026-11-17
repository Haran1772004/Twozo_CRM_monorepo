package com.tlc.crm.entity.models.type;

import com.tlc.i18n.I18nResolver;

/**
 * <p>
 * Model that represents the static dropdown option
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class StaticOption extends Option {

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public boolean isStaticOption() {
        return true;
    }

    public boolean isDraggable() {
        return false;
    }

    public String getOptionName(final I18nResolver i18nResolver) {
        return i18nResolver.get(getOption());
    }
}
