package com.tlc.crm.core.features.internal.dataimport;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

/**
 * Holds a field with its related source
 *
 * @param field         Represents the main field of a source
 * @param relatedSource Represents the source related to the field
 * @author sundar
 * @version 1.0
 */
public record FieldSourcePair(FieldInfo field, Source relatedSource) {

}
