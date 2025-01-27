package org.fluentjdbc;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.time.Instant;

/**
 * Interface for consistent query operations in a fluent way
 */
public interface DatabaseQueryBuilder<T extends DatabaseQueryBuilder<T>> extends DatabaseQueriable<T> {

    /**
     * If you haven't called {@link #orderBy}, the results of {@link DatabaseListableQueryBuilder#list}
     * will be unpredicable. Call <code>unordered()</code> if you are okay with this.
     */
    DatabaseListableQueryBuilder unordered();

    /**
     * Adds an <code>order by</code> clause to the query. Needed in order to list results
     * in a predicatable order.
     */
    DatabaseListableQueryBuilder orderBy(String orderByClause);

    /**
     * If the query returns no rows, returns `null`, if exactly one row is returned, maps it and return it,
     * if more than one is returned, throws `IllegalStateException`
     *
     * @param connection Database connection
     * @param mapper Function object to map a single returned row to a object
     * @return the mapped row if one row is returned, null otherwise
     * @throws IllegalStateException if more than one row was matched the the query
     *
     */
    <OBJECT> OBJECT singleObject(Connection connection, DatabaseTable.RowMapper<OBJECT> mapper);

    /**
     * Returns a string from the specified column name
     *
     * @param connection Database connection
     * @return the mapped row if one row is returned, null otherwise
     * @throws IllegalStateException if more than one row was matched the the query
     */
    @Nullable
    default String singleString(Connection connection, String fieldName) {
        return singleObject(connection, row -> row.getString(fieldName));
    }

    /**
     * Returns a long from the specified column name
     *
     * @param connection Database connection
     * @return the mapped row if one row is returned, null otherwise
     * @throws IllegalStateException if more than one row was matched the the query
     */
    @Nullable
    default Number singleLong(Connection connection, final String fieldName) {
        return singleObject(connection, (DatabaseTable.RowMapper<Number>) row -> row.getLong(fieldName));
    }

    /**
     * Returns an instant from the specified column name
     *
     * @param connection Database connection
     * @return the mapped row if one row is returned, null otherwise
     * @throws IllegalStateException if more than one row was matched the the query
     */
    @Nullable
    default Instant singleInstant(Connection connection, final String fieldName) {
        return singleObject(connection, row -> row.getInstant(fieldName));
    }

}
