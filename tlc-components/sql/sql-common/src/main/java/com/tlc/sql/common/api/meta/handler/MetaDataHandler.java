package com.tlc.sql.common.api.meta.handler;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.internal.parser.PatternResolver;
import com.tlc.sql.common.internal.parser.XmlToDCConverter;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Abishek
 * @version 1.0
 */
public interface MetaDataHandler<C extends ColumnDefinition, T extends TableDefinition<C>> {

    void loadMetaData(File file);

    void loadMetaData(Map<String, T> tableDefinitions);

    void populateMetaData(File file);

    void populateMetaData(Map<String, T> tableDefinitions);

    void unloadMetaData(Set<String> tables);

    Map<String, T> fetchTableDefinitions(File file);

    Map<String, T> fetchTableDefinitions(InputStream inputStream);

    void populateData(List<File> files, Map<String, String> dynamicProp);

    DataContainer fetchDataContainer(List<File> files, PatternResolver patternResolver, Map<String, String> dynamicProp, XmlToDCConverter.Type type);
}
