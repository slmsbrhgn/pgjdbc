package org.postgresql.jdbc1;

// IMPORTANT NOTE: This file implements the JDBC 1 version of the driver.
// If you make any modifications to this file, you must make sure that the
// changes are also made (if relevent) to the related JDBC 2 class in the
// org.postgresql.jdbc2 package.

import java.sql.*;

import org.postgresql.util.PSQLException;

/**
 * A Statement object is used for executing a static SQL statement and
 * obtaining the results produced by it.
 *
 * <p>Only one ResultSet per Statement can be open at any point in time.
 * Therefore, if the reading of one ResultSet is interleaved with the
 * reading of another, each must have been generated by different
 * Statements.  All statement execute methods implicitly close a
 * statement's current ResultSet if an open one exists.
 *
 * @see java.sql.Statement
 * @see ResultSet
 */
public class Statement implements java.sql.Statement
{
    Connection connection;		// The connection who created us
    java.sql.ResultSet result = null;	// The current results
    SQLWarning warnings = null;	// The warnings chain.
    int timeout = 0;		// The timeout for a query (not used)
    boolean escapeProcessing = true;// escape processing flag
    int maxrows=0;

	/**
	 * Constructor for a Statement.  It simply sets the connection
	 * that created us.
	 *
	 * @param c the Connection instantation that creates us
	 */
	public Statement (Connection c)
	{
		connection = c;
	}

	/**
	 * Execute a SQL statement that retruns a single ResultSet
	 *
	 * @param sql typically a static SQL SELECT statement
	 * @return a ResulSet that contains the data produced by the query
	 * @exception SQLException if a database access error occurs
	 */
	public java.sql.ResultSet executeQuery(String sql) throws SQLException
	{
		this.execute(sql);
		while (result != null && !((org.postgresql.ResultSet)result).reallyResultSet())
			result = ((org.postgresql.ResultSet)result).getNext();
		if (result == null)
			throw new PSQLException("postgresql.stat.noresult");
		return result;
	}

	/**
	 * Execute a SQL INSERT, UPDATE or DELETE statement.  In addition
	 * SQL statements that return nothing such as SQL DDL statements
	 * can be executed
	 *
	 * @param sql a SQL statement
	 * @return either a row count, or 0 for SQL commands
	 * @exception SQLException if a database access error occurs
	 */
	public int executeUpdate(String sql) throws SQLException
	{
		this.execute(sql);
		if (((org.postgresql.ResultSet)result).reallyResultSet())
			throw new PSQLException("postgresql.stat.result");
		return this.getUpdateCount();
	}

	/**
	 * In many cases, it is desirable to immediately release a
	 * Statement's database and JDBC resources instead of waiting
	 * for this to happen when it is automatically closed.  The
	 * close method provides this immediate release.
	 *
	 * <p><B>Note:</B> A Statement is automatically closed when it is
	 * garbage collected.  When a Statement is closed, its current
	 * ResultSet, if one exists, is also closed.
	 *
	 * @exception SQLException if a database access error occurs (why?)
	 */
	public void close() throws SQLException
	{
          // Force the ResultSet to close
          java.sql.ResultSet rs = getResultSet();
          if(rs!=null)
            rs.close();

          // Disasociate it from us (For Garbage Collection)
          result = null;
	}

	/**
	 * The maxFieldSize limit (in bytes) is the maximum amount of
	 * data returned for any column value; it only applies to
	 * BINARY, VARBINARY, LONGVARBINARY, CHAR, VARCHAR and LONGVARCHAR
	 * columns.  If the limit is exceeded, the excess data is silently
	 * discarded.
	 *
	 * @return the current max column size limit; zero means unlimited
	 * @exception SQLException if a database access error occurs
	 */
	public int getMaxFieldSize() throws SQLException
	{
		return 8192;		// We cannot change this
	}

	/**
	 * Sets the maxFieldSize - NOT! - We throw an SQLException just
	 * to inform them to stop doing this.
	 *
	 * @param max the new max column size limit; zero means unlimited
	 * @exception SQLException if a database access error occurs
	 */
	public void setMaxFieldSize(int max) throws SQLException
	{
		throw new PSQLException("postgresql.stat.maxfieldsize");
	}

	/**
	 * The maxRows limit is set to limit the number of rows that
	 * any ResultSet can contain.  If the limit is exceeded, the
	 * excess rows are silently dropped.
	 *
	 * @return the current maximum row limit; zero means unlimited
	 * @exception SQLException if a database access error occurs
	 */
	public int getMaxRows() throws SQLException
	{
		return maxrows;
	}

	/**
	 * Set the maximum number of rows
	 *
	 * @param max the new max rows limit; zero means unlimited
	 * @exception SQLException if a database access error occurs
	 * @see getMaxRows
	 */
	public void setMaxRows(int max) throws SQLException
	{
	  maxrows = max;
	}

	/**
	 * If escape scanning is on (the default), the driver will do escape
	 * substitution before sending the SQL to the database.
	 *
	 * @param enable true to enable; false to disable
	 * @exception SQLException if a database access error occurs
	 */
	public void setEscapeProcessing(boolean enable) throws SQLException
	{
		escapeProcessing = enable;
	}

	/**
	 * The queryTimeout limit is the number of seconds the driver
	 * will wait for a Statement to execute.  If the limit is
	 * exceeded, a SQLException is thrown.
	 *
	 * @return the current query timeout limit in seconds; 0 = unlimited
	 * @exception SQLException if a database access error occurs
	 */
	public int getQueryTimeout() throws SQLException
	{
		return timeout;
	}

	/**
	 * Sets the queryTimeout limit
	 *
	 * @param seconds - the new query timeout limit in seconds
	 * @exception SQLException if a database access error occurs
	 */
	public void setQueryTimeout(int seconds) throws SQLException
	{
		timeout = seconds;
	}

	/**
	 * Cancel can be used by one thread to cancel a statement that
	 * is being executed by another thread.  However, PostgreSQL is
	 * a sync. sort of thing, so this really has no meaning - we
	 * define it as a no-op (i.e. you can't cancel, but there is no
	 * error if you try.)
	 *
	 * 6.4 introduced a cancel operation, but we have not implemented it
	 * yet. Sometime before 6.5, this method will be implemented.
	 *
	 * @exception SQLException only because thats the spec.
	 */
	public void cancel() throws SQLException
	{
		// No-op
	}

	/**
	 * The first warning reported by calls on this Statement is
	 * returned.  A Statement's execute methods clear its SQLWarning
	 * chain.  Subsequent Statement warnings will be chained to this
	 * SQLWarning.
	 *
	 * <p>The Warning chain is automatically cleared each time a statement
	 * is (re)executed.
	 *
	 * <p><B>Note:</B>  If you are processing a ResultSet then any warnings
	 * associated with ResultSet reads will be chained on the ResultSet
	 * object.
	 *
	 * @return the first SQLWarning on null
	 * @exception SQLException if a database access error occurs
	 */
	public SQLWarning getWarnings() throws SQLException
	{
		return warnings;
	}

	/**
	 * After this call, getWarnings returns null until a new warning
	 * is reported for this Statement.
	 *
	 * @exception SQLException if a database access error occurs (why?)
	 */
	public void clearWarnings() throws SQLException
	{
		warnings = null;
	}

	/**
	 * setCursorName defines the SQL cursor name that will be used by
	 * subsequent execute methods.  This name can then be used in SQL
	 * positioned update/delete statements to identify the current row
	 * in the ResultSet generated by this statement.  If a database
	 * doesn't support positioned update/delete, this method is a
	 * no-op.
	 *
	 * <p><B>Note:</B> By definition, positioned update/delete execution
	 * must be done by a different Statement than the one which
	 * generated the ResultSet being used for positioning.  Also, cursor
	 * names must be unique within a Connection.
	 *
	 * <p>We throw an additional constriction.  There can only be one
	 * cursor active at any one time.
	 *
	 * @param name the new cursor name
	 * @exception SQLException if a database access error occurs
	 */
	public void setCursorName(String name) throws SQLException
	{
		connection.setCursorName(name);
	}

	/**
	 * Execute a SQL statement that may return multiple results. We
	 * don't have to worry about this since we do not support multiple
	 * ResultSets.   You can use getResultSet or getUpdateCount to
	 * retrieve the result.
	 *
	 * @param sql any SQL statement
	 * @return true if the next result is a ResulSet, false if it is
	 * 	an update count or there are no more results
	 * @exception SQLException if a database access error occurs
	 */
	public boolean execute(String sql) throws SQLException
	{
          if(escapeProcessing)
            sql=connection.EscapeSQL(sql);
          result = connection.ExecSQL(sql);
          return (result != null && ((org.postgresql.ResultSet)result).reallyResultSet());
	}

	/**
	 * getResultSet returns the current result as a ResultSet.  It
	 * should only be called once per result.
	 *
	 * @return the current result set; null if there are no more
	 * @exception SQLException if a database access error occurs (why?)
	 */
	public java.sql.ResultSet getResultSet() throws SQLException
	{
          if (result != null && ((org.postgresql.ResultSet)result).reallyResultSet())
            return result;
          return null;
	}

	/**
	 * getUpdateCount returns the current result as an update count,
	 * if the result is a ResultSet or there are no more results, -1
	 * is returned.  It should only be called once per result.
	 *
	 * @return the current result as an update count.
	 * @exception SQLException if a database access error occurs
	 */
	public int getUpdateCount() throws SQLException
	{
		if (result == null) 		return -1;
		if (((org.postgresql.ResultSet)result).reallyResultSet())	return -1;
		return ((org.postgresql.ResultSet)result).getResultCount();
	}

	/**
	 * getMoreResults moves to a Statement's next result.  If it returns
	 * true, this result is a ResulSet.
	 *
	 * @return true if the next ResultSet is valid
	 * @exception SQLException if a database access error occurs
	 */
	public boolean getMoreResults() throws SQLException
	{
		result = ((org.postgresql.ResultSet)result).getNext();
		return (result != null && ((org.postgresql.ResultSet)result).reallyResultSet());
	}

   /**
    * Returns the status message from the current Result.<p>
    * This is used internally by the driver.
    *
    * @return status message from backend
    */
   public String getResultStatusString()
   {
     if(result == null)
       return null;
     return ((org.postgresql.ResultSet)result).getStatusString();
   }

    /**
     * New in 7.1: Returns the Last inserted oid. This should be used, rather
     * than the old method using getResultSet, which for executeUpdate returns
     * null.
     * @return OID of last insert
     */
    public int getInsertedOID() throws SQLException
    {
      if(result!=null)
        return ((org.postgresql.ResultSet)result).getInsertedOID();
      return 0;
    }

}
