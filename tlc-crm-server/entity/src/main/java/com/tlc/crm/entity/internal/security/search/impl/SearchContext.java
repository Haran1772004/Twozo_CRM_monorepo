package com.tlc.crm.entity.internal.security.search.impl;

import com.tlc.search.model.FilterClause;
import com.tlc.search.service.ReadableDocumentService;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Wrapper to hold info pertaining to search action.
 * </p>
 *
 * @param start        Start count of the doc.
 * @param limit   Total docs to be fetched.
 * @param index        Index's name.
 * @param filterClause {@link FilterClause}.
 * @param docService   {@link ReadableDocumentService}.
 * @author Ajay
 * @version 1.0.
 */
record SearchContext(int start, int limit, int totalCount, @NotNull String index, @NotNull FilterClause filterClause,
                     @NotNull ReadableDocumentService docService) {
}
