package com.tlc.commons.template.processor;

import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.template.model.Section;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Processor responsible for removing configured sections from HTML templates.
 *
 * <p>This processor parses the provided HTML content, identifies elements whose
 * IDs match the configured section definitions, and removes those elements from
 * the template output.
 *
 * <p>Concrete implementations must provide the sections to remove by implementing
 * {@link #getSections()}.
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class PostSectionRemover implements Processor<String> {

    private final HtmlParser htmlParser;

    public PostSectionRemover() {
        htmlParser = HtmlParser.getInstance();
    }

    /**
     * Removes configured sections from the given HTML content.
     *
     * <p>The method parses the HTML content using Jsoup, finds matching elements
     * by section ID, removes them when present, and returns the updated HTML.
     * If no sections are configured, the original content is returned unchanged.
     *
     * @param htmlString HTML template content to process.
     *
     * @return HTML content after removing configured sections.
     */
    @Override
    public String process(final String htmlString) {
        final Collection<Section> sections = getSections();

        if (Objects.nonNull(sections) && !sections.isEmpty()) {
            final HtmlDocument document = htmlParser.parse(htmlString);

            for (final Section section : sections) {
                final Optional<HtmlElement> elementToBeRemoved = document.getElementById(section.id());

                elementToBeRemoved.ifPresent(HtmlElement::remove);
            }

            return document.html();
        }

        return htmlString;
    }

    /**
     * Returns the execution priority of this processor in the processing chain.
     *
     * <p>A lower order value indicates that this processor executes earlier
     * compared to processors with higher order values.
     *
     * @return Processing order value.
     */
    @Override
    public int getOrder() {
        return 1;
    }

    /**
     * Provides the collection of template sections that should be removed.
     *
     * <p>Each section should contain the identifier of the HTML element that
     * needs to be removed from the template.
     *
     * @return Collection of sections to remove from the template.
     */
    protected abstract Collection<Section> getSections();
}