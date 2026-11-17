package com.tlc.commons.template.processor;

import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.template.model.Section;
import com.tlc.commons.template.model.TemplateWrapper;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Processor responsible for removing unwanted sections from a template.
 *
 * <p>This processor identifies sections configured by their IDs and removes the
 * corresponding HTML elements from the template content before further processing.
 *
 * <p>Concrete implementations must provide the sections that need to be removed
 * through {@link #getSections()}.
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class PreSectionRemover implements Processor<TemplateWrapper> {

    private final HtmlParser htmlParser;

    public PreSectionRemover() {
        htmlParser = HtmlParser.getInstance();
    }

    /**
     * Processes the given template and removes configured sections.
     *
     * <p>The method parses the template HTML, finds matching elements using their
     * section IDs, removes them from the document, and updates the template with
     * the modified HTML content.
     *
     * @param templateWrapper Template wrapper containing the HTML content to process.
     * @return Updated template wrapper after removing configured sections.
     */
    @Override
    public TemplateWrapper process(final TemplateWrapper templateWrapper) {
        final Collection<Section> sections = getSections();

        if (Objects.nonNull(sections) && !sections.isEmpty()) {
            final HtmlDocument document = htmlParser.parse(templateWrapper.getHtmlString());

            for (final Section section : sections) {
                final Optional<HtmlElement> elementToBeRemoved = document.getElementById(section.id());

                elementToBeRemoved.ifPresent(HtmlElement::remove);
            }

            return templateWrapper.updateHtml(document.html());
        }

        return templateWrapper;
    }

    /**
     * Returns the execution order of this processor.
     *
     * <p>This processor uses the default execution order in the processing chain.
     *
     * @return Processing order value.
     */
    @Override
    public int getOrder() {
        return 1;
    }

    /**
     * Provides the sections that should be removed from the template.
     *
     * <p>Implementations should return the collection of section definitions
     * whose corresponding HTML elements need to be removed.
     *
     * @return Collection of sections to remove.
     */
    protected abstract Collection<Section> getSections();
}