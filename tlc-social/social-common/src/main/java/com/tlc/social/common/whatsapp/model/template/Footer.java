package com.tlc.social.common.whatsapp.model.template;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A model which represents the footer component of the template
 *
 * @author Maharaja S
 * @version 1.0
 */
public class Footer extends Component {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.footer.not.found")
    @Size(max = 60, groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.footer.length.exceeded")
    private String text;

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
        return Type.FOOTER;
    }
}
