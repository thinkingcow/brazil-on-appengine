/*
 * SqlTemplate.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2000-2009 Sun Microsystems, Inc.
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version 
 * 1.0 (the "License"). You may not use this file except in compliance with 
 * the License. A copy of the License is included as the file "license.terms",
 * and also available at http://www.sun.com/
 * 
 * The Original Code is from:
 *    Brazil project web application toolkit release 2.3.
 * The Initial Developer of the Original Code is: suhler.
 * Portions created by suhler are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): cstevens, suhler.
 *
 * Version:  2.13
 * Created by suhler on 00/05/08
 * Last modified by suhler on 09/10/02 14:58:53
 *
 * Version Histories:
 *
 * 2.13 09/10/02-14:58:53 (suhler)
 *   doc formatting
 *
 * 2.12 09/10/02-14:29:44 (suhler)
 *   javadoc formatiing fix
 *
 * 2.11 09/08/18-10:35:26 (suhler)
 *   lint
 *
 * 2.10 09/08/18-10:28:50 (suhler)
 *   redo to support both static and dynamic database connections
 *   D
 *
 * 2.9 09/08/11-15:23:57 (suhler)
 *   allow explicit connection?
 *
 * 2.8 08/07/24-16:49:25 (suhler)
 *   preserve token accumulation state
 *
 * 2.7 05/05/11-14:20:52 (suhler)
 *   - added type=query|system|update to <sql> to run executeQuery(),
 *   execute(), and executeUpdate() respectively
 *   - added timeout=[seconds] to <sql>
 *   - fixed "prefix" to eliminate redundant '.'s in property named
 *
 * 2.6 04/11/30-15:19:42 (suhler)
 *   fixed sccs version string
 *
 * 2.5 04/08/30-09:01:53 (suhler)
 *   "enum" became a reserved word, change to "enumer".
 *
 * 2.4 04/04/28-15:57:13 (suhler)
 *   added attributes for spedifying n/a values and 0 or 1 based indexing
 *
 * 2.3 03/08/21-12:46:28 (suhler)
 *   typo in "rowcount" property
 *
 * 2.2 03/07/07-14:45:16 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 1.11.1.1 03/07/07-14:08:15 (suhler)
 *   use addClosingTag() convenience method
 *
 * 2.1 02/10/01-16:39:11 (suhler)
 *   version change
 *
 * 1.11 01/08/03-18:23:21 (suhler)
 *   remove training  ws from classnames before trying to instantiate
 *
 * 1.10 01/06/04-14:10:15 (suhler)
 *   package move
 *
 * 1.9 00/12/11-13:32:37 (suhler)
 *   add class=props for automatic property extraction
 *
 * 1.8 00/10/05-15:51:58 (cstevens)
 *   PropsTemplate.subst() and PropsTemplate.getProperty() moved to the Format
 *   class.
 *
 * 1.7 00/07/07-17:02:31 (suhler)
 *   remove System.out.println(s)
 *
 * 1.6 00/07/07-15:32:51 (suhler)
 *   doc fixes
 *
 * 1.5 00/07/06-15:49:42 (suhler)
 *   doc update
 *
 * 1.4 00/05/31-13:52:03 (suhler)
 *   name change
 *
 * 1.3 00/05/19-11:50:30 (suhler)
 *   redo of error processing - its still not right though
 *
 * 1.2 00/05/10-10:44:21 (suhler)
 *   doc updates
 *
 * 1.2 00/05/08-17:37:52 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 sql/SqlTemplate.java
 *
 * 1.1 00/05/08-17:37:51 (suhler)
 *   date and time created 00/05/08 17:37:51 by suhler
 *
 */

/* MODIFICATIONS
 * 
 * Modifications to this file are derived, directly or indirectly, from Original Code provided by the
 * Initial Developer and are Copyright 2010-2011 Google Inc.
 * See Changes.txt for a complete list of changes from the original source code.
 */

package sunlabs.brazil.sql;

import sunlabs.brazil.server.Server;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.Format;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Sample Template class for running SQL queries via jdbc and
 * placing the results into the request properties for further processing.
 * <p>
 * Foreach session, a connection is made to one or more sql database via jdbc.
 * Session reconnection is attempted if the server connection breaks.
 * An SQL query is issued, with the results populating the request properties.
 * <p>
 * There are two ways to use this template (and they may be used together): static and 
 * dynamic.  For static configurations, the jdbc URL's and additional properties are defined
 * in the configuration file, and database connections are represented by "tokens".  For the
 * dynamic configurations, the jdbc URL's are passed as attributes to the sql tag.
 * <p>
 * Configuration options:
 * <dl class=props>
 * <dt>drivers
 * <dd>A list of java classes that will be used as JDBC drivers for the
 * database connectors.  At least one driver should be provided.
 * <dt>databases
 * <dd>A list of tokens that represent databases that may be accessed by this template.
 * For each token, the properties:
 * <ul>
 * <li><code>token.url</code>
 * Specifies a JDBC URL
 * to use to connect to the database. 
 * <li><code>token.sqlPrefix</code>
 * Contains a list of tokens that represent
 * additional information that should be provided for a connection, in the
 * form of one or more "token.name", "token.value" pairs.
 * <li><code>token.user</code>, <code>token.passwd</code>
 * The user and password associated with this connection.
 * If "user" is specified, then the tokens in "sqlPrefix" are ignored.
 * </ul>
 * </dl>
 * For a discussion of how the tag attributes and how the results results map to properties,
 * {@link #tag_sql see below}.
 *
 * @author		Stephen Uhler
 * @version		2.13
 */
public class SqlTemplate extends Template {

  Hashtable<String, ConInfo> connections = new Hashtable<String, ConInfo>();	// our database connections
  boolean initialized = false;

  /**
   * Gather up all the databases and their credentials.
   */
  @Override
  public boolean init(RewriteContext hr) {
    super.init(hr);
    hr.addClosingTag("sql");
    if (initialized) {
      return (true);
    }
    initialized = true;
    hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix,
            "One time initialization");
    Properties props = hr.request.getProps();
    StringTokenizer st;

    // load any required drivers

    String drivers = hr.get("drivers", null);

    // (look at "driver" for backward compatibility)
    if (drivers == null) {
      drivers = hr.get("driver", null);
    }

    if (drivers != null) {
      st = new StringTokenizer(drivers);
      while (st.hasMoreTokens()) {
        try {
          String cls = st.nextToken();
          Class.forName(cls);
          hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix,
                  "loading driver: (" + cls + ")");

        } catch (ClassNotFoundException e) {
          hr.request.log(Server.LOG_WARNING, hr.prefix, e.getMessage());
          continue;
        }
      }
    } else {
      hr.request.log(Server.LOG_WARNING, hr.prefix,
              "No database drivers specified!");
    }

    // Create the static database connections

    st = new StringTokenizer(hr.get("databases", hr.prefix));
    while (st.hasMoreTokens()) {
      String token = trimDot(st.nextToken());
      String url = props.getProperty(token + ".url");
      String user = props.getProperty(token + ".user");
      String passwd = props.getProperty(token + ".passwd");
      Properties sqlProps = null;	// extra properties for connection

      if (url == null) {
        hr.request.log(Server.LOG_WARNING, token,
                "missing url parameter");
        continue;
      }

      /*
       * Get the extra properties passed to each sql connection
       */

      String pre = props.getProperty(token + ".sqlPrefix");
      if (pre != null) {
        Enumeration enumer = props.propertyNames();
        int len = pre.length();
        sqlProps = new Properties();
        while (enumer.hasMoreElements()) {
          String key = (String) enumer.nextElement();
          if (key.startsWith(pre)) {
            sqlProps.put(key.substring(len),
                    props.getProperty(key));
          }
        }
      }

      // stash the connection info.

      ConInfo con;
      if (user != null) {
        con = new ConInfo(url, user, passwd);
      } else {
        con = new ConInfo(url, sqlProps);
      }
      hr.request.log(Server.LOG_DIAGNOSTIC, token,
              "Got sql connection for " + url + ": " + con);
      connections.put(token, con);
      // System.out.println("connections: " + con);
    }
    return (true);
  }

  /**
   * Replace the SQL query with the appropriate request properties.
   * Look for the following parameters:
   * <i>(NOTE - This interface is preliminary, and subject to change)</i>.
   * <dl>
   * <dt>debug	<dd>Include diagnostics in html comments
   * <dt>prefix	<dd>prefix to prepend to all results.
   *			    Defaults to the database token
   * <dt>database	<dd>the database to use.  This is either one of the database
   *			tokens used with the "databases" configuration
   *			parameter, or a fully qualified jdbc url (e.g.
   *			"jdbc:..." to use for this database.
   *			if the jdbc url form is used, the driver must have already
   *			been specified, and no optional parameters can be
   *			provided for the connection.
   * <dt>max		<dd>The max # of rows returned (default=100)
   * <dt>catalog    <dd>The name of the catalog (e.g. database) to set, if any
   * <dt>na		<dd>Value to reurn for NULL.  Defaults to "n/a"
   * <dt>type		<dd>The type of SQL command, one of "query", "system",
   *			or "update".  these values map to the JDBC calls
   *			executeQuery(), execute() and executeUpdate()
   *			respectively. Defaults to "query".
   * <dt>timeout	<dd>The number of seconds to wait for the query
   *			to finish.  Defaults to "0": wait forever
   * <dt>eval		<dd>If present, do ${...} to entire query. (see
   *			{@link sunlabs.brazil.util.Format#getProperty getProperty}).
   * <dt>zeroIndex	<dd>if true, row counts start at 0, not 1
   * <dt>index	<dd>If present, use column 1 as part of the name.
   *			    Otherwise, an index name is invented.
   * <dt>noTable	<dd>If true, the table name is not encoded as
   *			part of the result
   * <dt>close	<dd>If true, the database copnnection will be closed after the query conpletes.
   * </dl>
   * For all queries, the following properties (with the prefix prepended)
   * are set:
   * <dl>
   * <dt>columncount		<dd>The number of columns returned
   * <dt>rowcount		<dd>The number of rows returned
   * </dl>
   * Foreach entry in the resultant table, its property is:
   * <code>${prefix}.${table_name}.${columname}.${key}</code>.  If
   * the <code>index</code> parameter is set, the key is the value of
   * the first column returned. Otherwise the key is the row number,
   * and the additional property <code>${prefix}.rows</code> contains a
   * list of all the row numbers returned.
   */
  public void tag_sql(RewriteContext hr) {
    debug = hr.isTrue("debug");
    boolean eval = hr.isTrue("eval");
    String type = hr.get("type", "query");
    boolean useIndex = hr.isTrue("index");
    boolean zeroIndex = hr.isTrue("zeroIndex");
    Properties props = hr.request.getProps();
    String na = hr.get("na", "n/a");
    String database = hr.get("database", hr.prefix);
    boolean shouldClose = hr.isTrue("close");
    boolean noTable = hr.isTrue("notable");
    String user = hr.get("user");
    String passwd = hr.get("passwd");
    String catalog = hr.get("catalog");

    String pre = hr.get("prefix");
    if (pre == null) {
      pre = (database != null && !database.startsWith("jdbc:")) ? hr.prefix : database;
    }
    debug(hr);

    boolean was = hr.accumulate(false);
    hr.nextToken();
    String query = hr.getBody();
    hr.accumulate(was);
    hr.nextToken();	// eat the </sql>

    if (!pre.equals("") && pre.endsWith(".") == false) {
      pre += ".";
    }

    ConInfo conInfo = connections.get(database);
    if (conInfo == null && database.startsWith("jdbc:")) {
      if (user != null) {
        conInfo = new ConInfo(database, user, passwd);
      } else {
        conInfo = new ConInfo(database, null);
      }
      connections.put(database, conInfo);
      hr.request.log(Server.LOG_DIAGNOSTIC, pre,
              "new dynamic sql connection " + conInfo);
    }
    if (conInfo == null) {
      String msg = "database undefined: (" + database + ")";
      debug(hr, msg);
      props.put(pre + "error", msg);
      return;
    }
    int max = Format.stringToInt(hr.get("max", null), 100);
    int timeout = Format.stringToInt(hr.get("timeout", "0"), 0);

    if (eval) {
      query = Format.subst(props, query);
    }
    debug(hr, query);

    Statement stmt = null;
    ResultSet result = null;
    int updateCount = -1;
    ResultSetMetaData meta = null;

    /*
     * Now run the query, stuffing the results into the properties.
     * This is pretty stupid right now.  The first column is used as
     * the "index" if useIndex is set.  Otherwise a counter is used.
     */

    try {
      if (catalog != null) {
        conInfo.getConnection().setCatalog(catalog);
        hr.request.log(Server.LOG_DIAGNOSTIC, "Setting catalog: " + catalog);
      }
      stmt = doSQL(conInfo.getConnection(), query, type, timeout);
      result = stmt.getResultSet();
      if (result != null) {
        meta = result.getMetaData();
      }
      updateCount = stmt.getUpdateCount();
      props.put(pre + "updateCount", "" + updateCount);
      if (meta == null || result == null) {
        return;
      }

      int rows = 0;
      int count = meta.getColumnCount();
      StringBuffer list = null;
      props.put(pre + "columncount", "" + count);

      // column names

      StringBuffer columns = new StringBuffer();
      String delim = "";
      for (int i = 0; i < count; i++) {
        String cn = na;
        try {
          cn = meta.getColumnName(i);
        } catch (SQLException e) {
          System.err.println("Can't get column name: " + e);
        }
        props.put(pre + "columnName." + (i + 1), cn);
        columns.append(delim).append(i + 1);
        delim = " ";
      }
      props.put(pre + "columns", columns.toString());


      if (!useIndex) {
        list = new StringBuffer();
      }
      while (result.next() && rows++ < max) {
        String first;	// name of property row
        if (useIndex) {
          first = result.getString(1);
        } else {
          first = "" + (zeroIndex ? rows - 1 : rows);
          list.append(first).append(" ");
        }
        for (int i = (useIndex ? 2 : 1); i <= count; i++) {
          String name = deriveName(pre, meta, first, i, noTable);
          String value = result.getString(i);
          if (value == null) {
            value = na;
          }
          props.put(name, value);
          // debug(hr,name + "=" + value);
        }
      }
      if (list != null) {
        props.put(pre + "rows", list.toString());
      }
      props.put(pre + "rowcount", "" + rows);
    } catch (SQLException e) {
      e.printStackTrace(); // temporary
      props.put(pre + "error", e.getMessage());
      props.put(pre + "state", e.getSQLState());
      debug(hr, "Failed: " + e.getMessage() + "/" + e.getSQLState());
      props.put(pre + "rowcount", "0");
    } catch (Exception e) {
      e.printStackTrace(); // temporary
      props.put(pre + "error", e.getMessage());
    }

    if (shouldClose) {
      conInfo.close();
      debug(hr, "connection closed");
    }
  }

  public Statement doSQL(Connection con, String query, String type, int timeout)
          throws SQLException {
    Statement stmt = con.createStatement();
    if (timeout > 0) {
      stmt.setQueryTimeout(timeout);
    }
    System.err.println("doSql Running:" + type + " (" + query + ")");
    if (type.equals("query")) {
      stmt.executeQuery(query); // returns result set
    } else if (type.equals("update")) {
      stmt.executeUpdate(query); // returns count
    } else if (type.equals("system")) {
      stmt.execute(query); // true -> resultset, false ->update count
    }
    System.err.println("doSql Done!");
    return stmt;
  }

  /**
   * Convenience method for deriving props names
   */
  String deriveName(String prefix, ResultSetMetaData meta, String suffix,
          int i, boolean noTable) {
    String table = "";
    String column = "";
    if (!noTable) {
      try {
        table = meta.getTableName(i);
      } catch (SQLException e) {
      }
    }
    try {
      column = meta.getColumnName(i);
    } catch (SQLException e) {
    }
    if (!column.equals("")) {
      column += ".";
    }
    if (!table.equals("")) {
      table += ".";
    }
    String result = prefix + table + column + suffix;
    return result;
  }

  public void tag_slash_sql(RewriteContext hr) {
    hr.killToken();
  }

  String trimDot(String s) {
    return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
  }

  /**
   * Stuff needed to create an SQL connection.
   */
  static class ConInfo {

    String url;	// the url for the connection
    Properties props;	// the extra properties for the connection
    Connection con;		// the resultant connection
    String user, passwd;	// name and password (if required)

    ConInfo(String url, Properties props) {
      this.url = url;
      this.props = props;
      this.con = null;
      user = null;
      passwd = null;
    }

    ConInfo(String url, String user, String passwd) {
      this.url = url;
      this.props = null;
      this.con = null;
      this.user = user;
      this.passwd = passwd;
    }

    /**
     * Connect to the database
     */
    Connection getConnection() throws SQLException {
      if (con == null || !con.isValid(2)) {
        try {
          System.err.println("Connecting to ...: " + url);
          if (user != null) {
            con = DriverManager.getConnection(url, user, passwd);
          } else {
            con = DriverManager.getConnection(url, props);
          }
          System.err.println("Connection created: " + con);
        } catch (SQLException e) {
          throw (e);
        } catch (Exception e) {
          System.err.println("Exception in SQL setup: " + e);
          throw new SQLException("SQL setup error: " + e);
        }
      } else {
        System.err.println("Resusing: " + con);
      }
      return con;
    }

    public void close() {
      try {
        con.close();
        con = null;
      } catch (SQLException e) {
      }
    }

    @Override
    public String toString() {
      String result = url;
      if (user != null) {
        result += " " + user;
      }
      if (props != null) {
        result += " " + props;
      }
      
      if (con != null) {
        result += " Con:" + con;
      }
      return result;
    }
  }
  
  // temporary
  
  @Override
  protected void
  debug(RewriteContext hr, String msg) {
    super.debug(hr, msg);
    System.err.println("SQL: " + msg);
  }
}
