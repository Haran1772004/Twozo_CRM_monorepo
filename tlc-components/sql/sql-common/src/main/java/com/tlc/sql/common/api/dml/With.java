package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.internal.dml.WithImpl;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a SQL WITH (Common Table Expression) clause that combines
 * one or more {@link DerivedTable}s with a final {@link SelectQuery}.
 */
public interface With {

    /**
     * Returns the collection of derived tables (CTEs) defined in this WITH clause.
     *
     * @return the collection of derived tables
     */
    Collection<DerivedTable> getDerivedTables();

    /**
     * Returns the main select query that references the derived tables.
     *
     * @return the select query
     */
    SelectQuery getSelectQuery();

    /**
     * Builder for constructing {@link With} instances.
     */
    class Builder {

        private SelectQuery selectQuery;
        private List<DerivedTable> derivedTables;

        private Builder() {
        }

        /**
         * Creates a new {@link Builder} instance.
         *
         * @return a new builder
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         * Sets the main select query for the WITH clause.
         *
         * @param selectQuery the select query that references the derived tables
         * @return this builder for method chaining
         */
        public Builder selectQuery(final SelectQuery selectQuery) {
            this.selectQuery = Objects.requireNonNull(selectQuery);
            return this;
        }

        /**
         * Adds a single derived table (CTE) to the WITH clause.
         *
         * @param derivedTable the derived table to add
         * @return this builder for method chaining
         */
        public Builder addDerivedTable(final DerivedTable derivedTable) {
            this.derivedTables = Objects.isNull(derivedTables) ? new ArrayList<>() : derivedTables;
            derivedTables.add(Objects.requireNonNull(derivedTable));

            return this;
        }

        /**
         * Adds multiple derived tables (CTEs) to the WITH clause.
         *
         * @param derivedTables the derived tables to add
         * @return this builder for method chaining
         */
        public Builder addDerivedTable(final DerivedTable... derivedTables) {
            getDerivedTables().addAll(Arrays.asList(derivedTables));
            return this;
        }

        /**
         * Adds a collection of derived tables (CTEs) to the WITH clause.
         *
         * @param derivedTables the collection of derived tables to add
         * @return this builder for method chaining
         */
        public Builder addDerivedTable(Collection<DerivedTable> derivedTables) {
            getDerivedTables().addAll(derivedTables);
            return this;
        }

        /**
         * Builds and returns the {@link With} instance.
         *
         * @return the constructed {@link With} instance
         * @throws com.tlc.commons.code.ErrorCode if the select query is null or no derived tables are provided
         */
        public With build() {
            if (Objects.isNull(selectQuery)) {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_SELECT_QUERY);
            }

            if (Objects.isNull(derivedTables) || derivedTables.isEmpty()) {
                throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_DERIVED_TABLES);
            }

            return new WithImpl(derivedTables, selectQuery);
        }

        /**
         * Returns the derived tables list, initializing it if necessary.
         *
         * @return the list of derived tables
         */
        private List<DerivedTable> getDerivedTables() {
            return this.derivedTables = Objects.isNull(this.derivedTables) ? new ArrayList<>() : this.derivedTables;
        }
    }
}