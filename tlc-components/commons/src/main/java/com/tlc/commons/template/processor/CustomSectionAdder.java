package com.tlc.commons.template.processor;

import com.tlc.commons.parser.html.HtmlDocument;
import com.tlc.commons.parser.html.HtmlElement;
import com.tlc.commons.parser.html.HtmlParser;
import com.tlc.commons.template.model.CustomSectionInfo;
import com.tlc.commons.template.model.TemplateWrapper;
import com.tlc.commons.util.StringUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processor implementation responsible for adding and duplicating custom sections
 * into a template.
 *
 * <p>This processor identifies predefined custom section placeholders in the HTML
 * template, clones the required sections, updates their identifiers and
 * placeholders based on the configured order, and updates the final template
 * content.
 *
 * <p>The generated section IDs and placeholders are suffixed with the configured
 * order value to support multiple instances of the same custom section.
 *
 * <p>Concrete implementations must provide the custom section information through
 * {@link #getSections()}.
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class CustomSectionAdder implements Processor<TemplateWrapper> {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$([a-zA-Z_][a-zA-Z0-9_]*)");

    private final HtmlParser htmlParser;

    public CustomSectionAdder() {
        htmlParser = HtmlParser.getInstance();
    }

    /**
     * Processes the given template by adding configured custom sections.
     *
     * <p>The method parses the template HTML, finds existing custom section
     * elements, clones them based on the configured section information, updates
     * child element identifiers and placeholders, and removes the original
     * section after replacement.
     *
     * @param templateWrapper Template wrapper containing the HTML content to process.
     * @return Updated template wrapper containing the modified HTML content.
     */
    @Override
    public TemplateWrapper process(final TemplateWrapper templateWrapper) {
        final Collection<CustomSectionInfo> customSectionInfos = getSections();

        if (Objects.nonNull(customSectionInfos) && !customSectionInfos.isEmpty()) {
            final HtmlDocument document = htmlParser.parse(templateWrapper.getHtmlString());
            final Collection<HtmlElement> customSectionToBeRemoved = new HashSet<>();

            for (final CustomSectionInfo customSectionInfo : customSectionInfos) {
                final Optional<HtmlElement> optionalCustomSection = document.getElementById(customSectionInfo.section().id());

                if (optionalCustomSection.isEmpty()) {
                    document.append(customSectionInfo.sectionHtml());
                } else {
                    final String orderAsString = customSectionInfo.getOrderAsString();
                    final HtmlElement customSection = optionalCustomSection.get();
                    final HtmlElement clonedElement = customSection.clone();

                    customSectionToBeRemoved.add(customSection);
                    clonedElement.id(customSectionInfo.section().id());

                    updateChildSectionId(orderAsString, clonedElement);

                    String html = clonedElement.html();
                    final Matcher matcher = PLACEHOLDER.matcher(html);

                    while (matcher.find()) {
                        final String placeholder = matcher.group();

                        html = html.replace(placeholder, StringUtil.join(StringUtil.Delimiter.HYPHEN, placeholder, orderAsString));
                    }

                    clonedElement.html(html);
                    customSection.after(clonedElement);
                }
            }

            customSectionToBeRemoved.forEach(HtmlElement::remove);
            return templateWrapper.updateHtml(document.html());
        }

        return templateWrapper;
    }

    /**
     * Returns the execution order of this processor.
     *
     * <p>This processor is configured to run as the first processor in the
     * processing chain.
     *
     * @return Processing order value.
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * Updates the child element IDs by appending the custom section order.
     *
     * <p>This method recursively traverses all child elements and updates their
     * IDs to maintain uniqueness when multiple instances of the same section
     * are added.
     *
     * @param order   Order value to append to the element identifiers.
     * @param element Root element whose child identifiers need to be updated.
     */
    private void updateChildSectionId(final String order, final HtmlElement element) {

        if (element.childrenSize() > 0) {
            for (final HtmlElement section : element.children().getElements()) {
                section.id(StringUtil.join(StringUtil.Delimiter.DOT, section.id(), order));

                updateChildSectionId(order, section);
            }
        }
    }

    /**
     * Provides the custom sections that need to be added or processed.
     *
     * <p>Implementations should return the section metadata required for
     * generating custom sections dynamically.
     *
     * @return Collection of custom section information.
     */
    protected abstract Collection<CustomSectionInfo> getSections();
}