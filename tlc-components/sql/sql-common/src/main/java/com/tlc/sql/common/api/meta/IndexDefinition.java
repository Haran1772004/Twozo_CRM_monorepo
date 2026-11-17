package com.tlc.sql.common.api.meta;

import java.util.List;
import java.util.Objects;


/**
 * Represents an index definition for both OLTP and OLAP databases.
 * OLTP uses only name and columns. OLAP additionally uses type and false-positive-probability.
 *
 * @author Abishek
 * @version 1.1
 */
public class IndexDefinition
{
    private final String name;
    private final List<String> columns;
    private final String type;
    private final String falsePositiveProbability;
    private final String granularity;

    public IndexDefinition(String name, List<String> columns)
    {
        this(name, columns, null, null, null);
    }

    public IndexDefinition(String name, List<String> columns, String type, String falsePositiveProbability, String granularity)
    {
        this.name = Objects.requireNonNull(name);
        this.columns = List.copyOf(columns);
        this.type = type;
        this.falsePositiveProbability = falsePositiveProbability;
        this.granularity = granularity;
    }

    public String getName()
    {
        return name;
    }

    public List<String> getColumns()
    {
        return columns;
    }

    public String getType()
    {
        return type;
    }

    public String getFalsePositiveProbability()
    {
        return falsePositiveProbability;
    }

    public String getGranularity()
    {
        return granularity;
    }

    /**
     * Returns true if this index has OLAP-specific configuration (type, probability, etc.)
     */
    public boolean isOlapIndex()
    {
        return Objects.nonNull(type);
    }
}
