package com.tlc.social.common.whatsapp.model.template;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * A model which represents the body component of the template
 *
 * @author Maharaja S
 * @version 1.0
 */
public class Body extends Component {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.body.not.found")
    private String text;
    private boolean hasPlaceholder;

    public boolean hasPlaceholder() {
        return hasPlaceholder;
    }

    public void setHasPlaceholder(final boolean hasPlaceholder) {
        this.hasPlaceholder = hasPlaceholder;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    /**
     * {@inheritDoc}
     *
     * @return the type
     */
    @Override
    public Type getType() {
        return Type.BODY;
    }
}
