package com.tlc.commons.template.processor;

import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.template.model.Section;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Processor responsible for updating the order of sections within a template.
 *
 * <p>This processor rearranges the HTML sections based on the order provided by
 * {@link #getReorderedSection()}. The sections are moved within the document body
 * while preserving their existing content and structure.
 *
 * <p>Concrete implementations must provide the required section ordering details.
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class SectionOrderUpdater implements Processor<String> {

    private final HtmlParser htmlParser;

    public SectionOrderUpdater() {
        htmlParser = HtmlParser.getInstance();
    }

    /**
     * Processes the given HTML content and updates the section order.
     *
     * <p>The method parses the HTML content, identifies sections using their IDs,
     * and appends them to the document body according to the configured order.
     *
     * @param htmlString HTML content whose section order needs to be updated.
     * @return Updated HTML content with reordered sections.
     */
    @Override
    public String process(final String htmlString) {
        final HtmlDocument document = htmlParser.parse(htmlString);
        final Optional<HtmlElement> optionalBody = Optional.ofNullable(getBaseSection())
                .flatMap(baseSection -> document.getElementById(baseSection.id()))
                .or(() -> Optional.ofNullable(document.body()));

        if (optionalBody.isPresent()) {
            final HtmlElement body = optionalBody.get();

            for (final Section section : getReorderedSection()) {
                final HtmlElement element = body.getElementById(section.id());

                if (Objects.nonNull(element)) {
                    body.appendChild(element);
                }
            }
        }

        return document.html();
    }

    /**
     * Returns the execution order of this processor.
     *
     * <p>This processor is configured to execute at the default processing order.
     *
     * @return Processing order value.
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * Provides the sections in the required display order.
     *
     * <p>The returned list determines the sequence in which sections are arranged
     * within the generated HTML output.
     *
     * @return List of sections representing the required order.
     */
    protected abstract List<Section> getReorderedSection();

    /**
     * Returns the base section associated with this implementation.
     *
     * @return the base {@link Section} instance
     */
    protected abstract Section getBaseSection();
}