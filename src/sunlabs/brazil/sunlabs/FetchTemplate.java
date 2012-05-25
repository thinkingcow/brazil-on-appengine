/*
 * FetchTemplate.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2004-2009 Sun Microsystems, Inc.
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
 * Contributor(s): suhler.
 *
 * Version:  1.11
 * Created by suhler on 04/04/02
 * Last modified by suhler on 09/01/30 16:37:20
 *
 * Version Histories:
 *
 * 1.11 09/01/30-16:37:20 (suhler)
 *   make deprecated
 *
 * 1.10 08/06/20-08:27:08 (suhler)
 *   document
 *   body attribute
 *
 * 1.9 08/06/09-16:12:30 (suhler)
 *   add "method" and "body" options to allow arbitrary http methods, and associate message bodies
 *   with any method
 *
 * 1.8 08/06/02-12:11:23 (suhler)
 *   added "trustHost" to trust ssl hosts whose identity we can't verify
 *
 * 1.7 08/02/21-10:00:07 (suhler)
 *   Add additional tag attributes:
 *   - nofollow: don't follow redirects
 *   - addclientheaders: pass client HTTP headers through
 *   - addheaders: add additional http headers
 *   .
 *
 * 1.6 08/02/20-13:07:40 (suhler)
 *   add "nofollow" attribute, Server.version
 *
 * 1.5 06/08/02-13:45:54 (suhler)
 *   changed name to FetchTemplate, removed some debugging
 *   .
 *
 * 1.4 05/05/19-13:05:41 (suhler)
 *   only print stack trace if log > 3
 *
 * 1.3 04/05/24-15:24:15 (suhler)
 *   doc fixes
 *
 * 1.2 04/04/28-14:52:55 (suhler)
 *   doc cleanup
 *
 * 1.2 04/04/02-16:23:15 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 2 1 sunlabs/FetchTemplate.java
 *   Name history : 1 0 sunlabs/IncludeTemplate.java
 *
 * 1.1 04/04/02-16:23:14 (suhler)
 *   date and time created 04/04/02 16:23:14 by suhler
 *
 */

/* MODIFICATIONS
 * 
 * Modifications to this file are derived, directly or indirectly, from Original Code provided
 * by the Initial Developer and are Copyright 2010-2012 Google Inc.
 * See Changes.txt for a complete list of changes from the original source code.
 */

package sunlabs.brazil.sunlabs;

import sunlabs.brazil.server.Server;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.http.MimeHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Template class for substituting html pages into an html page.
 * This class is used by the TemplateHandler.  This does not perform
 * traditional server-side include processing, whose normal purpose is to
 * include standard headers and footers.  That functionality is provided
 * instead by including the content into the template, and not the other
 * way around.
 * <p>
 * This include incorporates entire pages from other sites, either directly 
 * as part of the page content, or into a property, for further processing.
 * <p>
 * <b>NOTE:</b> This version depends on the behavior of JDK1.4.  It does not
 * use the Brazil Request class, but the URL class instead.  Thus
 * it has advantages that it automatically follows some redirects, and can
 * handle additional protocols, such as "https" and "ftp", but it does
 * not cache connections, and the proxy may not be set on a per request
 * basis. Attributes:
 * <dl class=attributes>
 * <dt>href<dd>Absolute url to fetch, and insert here
 * <dt>post<dd>Post data if any.  If set, an http POST is issued.
 *   The post data is expected to be in www-url-encoded format.
 *   [DEPRECATED, use "method" and "body" instead]
 * <dt>method<dd>The request method.  If not set, GET is used, unless "body" is supplied, 
 * in which case POST is the default.
 * <dt>body<dd>The message body, if any.  If a message body is supplied, then the content-type
 *   is set to "application/x-www-form-urlencoded" by default.
 * <dt>type<dd>Override the default content type for data to be uploaded.  This is done before
 * "addHeaders" is processed.
 * <dt>encoding<dd>Set the character encoding for the content body (defaults to UTF-8).
 * <dt>alt<dd>Text to insert if URL can't be obtained.
 * <dt>name<dd>The name of the variable to put the result in.
 *   If this is specified, the content is not included in place.
 * <dt>proxy<dd>The proxy:port to use as a proxy (if any).
 *   If specified, it overrides the <code>proxy</code> property, 
 *   in <code>request.props</code>.
 * <dt>timeout<dd>Set the timeout for this request (in sec).  This will be set both
 * the connection timout (for reaching the server), and the I/O Timeout (for retieving the response)
 * so the request may take up to twice this long before it fails.  The default is '0' or no
 * timeout (e.g. wait forever)
 * <dt>getheaders<dd>The name of the variable prefix to use to
 *   extract the http response headers.
 *   If this not specified, no response headers are retrieved.
 *   The result will be properties of the form:
 *   <code>[getheaders].[header_name]=[header_value]</code>.
 *   If multiple entries exist for a particular header name, the values
 *   are combined as per HTTP conventions (e.g. v1, v2, ... vn).
 *   The pseudo header <code>status</code> (and corrosponding <code>statusPhrase</code>
 *   will contain the http status line.  The pseudo headers will be the only headers set if an error
 *   occurs.
 * <dt>nofollow<dd>By default, redirects are followed (at least redirects to
 *   the same protocol).  Setting "nofollow=true" disables following redirects.
 * <dt>addclientheaders<dd>If true, then all non point-to-point http headers
 *   are copied into the request. [Note, this is the opposite default
 *   from the IncludeTemplate(), but it retains better backward compatibility.
 * <dt>addheaders<dd>A white space delimited list of additional header names
 *   do be added to the set of HTTP headers accompanying the request.  The
 *   header values are looked up in the request properties. Example:
 *   <pre>
 *     &lt;set namespace=local name=ha.name value=X-extra &gt;
 *     &lt;set namespace=local name=ha.value value="header value"&gt;
 *     &lt;set namespace=local name=hb.name value=cookie"&gt;
 *     &lt;set namespace=local name=hb.name value="jsessionid=123"&gt;
 *     &lt;fetch .... addheaders="ha hb"&gt;
 *   </pre>
 *   Would cause the following headers to be added to the request:
 *   <pre>
 *     X-extra: header value
 *     cookie: jsessionid=123
 *   </pre>
 *   Headers whose values that are undefined, or the empty string are not set.
 *   [Note, this mechanism for adding headers in different from that used
 *   by the IncludeTemplate(), which is unfortunate].
 *   do be added to the set of HTTP headers accompanying the request.  The
 *   <dt>contenthandlers
 *   <dd>A whitespace list of tokens representing classes that will handle particular content
 *   types.  For each token the property "[token].class" is used to specify the class name.
 *   Handlers are tried in order until one processes the content.  If no handler is specified,
 *   The default handler, whose behavior is described here, is used.
 * </dl>
 * If the request fails, the "error" property will be set with a short message, 
 * prefixed with the template name.
 * <p>
 * Note: This used to be called the IncludeTemplate, but was
 * renamed to avoid confusion with the other IncludeTemplate.
 * This template will be deprecated when the IncludeTemplate completely
 * subsumes this functionality.
 *
 * @author		Stephen Uhler
 * @version		@(#)FetchTemplate.java	1.11
 */

public class FetchTemplate extends Template {
  
  // Keep a map of handlers per template configuration
  private static HashMap<String,List<ContentHandler>> handlerMap = 
      new HashMap<String, List<ContentHandler>>();
  
  /**
   * One time initialization of content handlers
   * XXX Handlers should be passed their names so they can find their properties.
   */
  @Override
  public boolean init(RewriteContext hr) {
    setup(hr);
    return true;
  }
  
  /**
   * Install the content handlers.
   * XXX This class was designed to fetch remote html content to incorporate into
   *     an html page, but is being pushed to deal with non-html content as well.
   *     This is a first-pass attempt to allow pluggable content handlers based on the
   *     return content type.  It has many deficiencies.
   * @param hr
   */
  
  private synchronized void setup(RewriteContext hr) {
    List<ContentHandler> handlers = handlerMap.get(hr.prefix);
    if (handlers == null) {
      handlers = new ArrayList<ContentHandler>();
      StringTokenizer st = new StringTokenizer(hr.getProps("contenthandlers", ""));
      while(st.hasMoreTokens()) {
        String token = st.nextToken();
        try {
          String className = hr.request.getProps().getProperty(token + ".class");
          Class<? extends ContentHandler> cls = Class
              .forName(className)
              .asSubclass(ContentHandler.class);
          ContentHandler handler = cls.newInstance();
          handlers.add(handler);
          hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix,
                  "Installing handler: " + token);
        } catch (Exception e) {
          hr.request.log(Server.LOG_WARNING, hr.prefix,
                  "Can't install handler: " + token + " (" + e + ")");
        }
      }
      handlers.add(new DefaultHandler());
      handlerMap.put(hr.prefix, handlers);
    }
  }

  public void tag_include(RewriteContext hr) {
    String href = hr.get("href");	// the url to fetch
    String alt = hr.get("alt");	    // the result if fetch failed
    String name = hr.get("name");	// the variable to put the result in
    String post = hr.get("post");	// post data (if any) DEPRECATED - use body`
    String body = hr.get("body");	// post data (if any)
    String type = hr.get("type", "application/x-www-form-urlencoded");
    String encoding = hr.get("encoding", "UTF-8");
    String method = hr.get("method", "GET").toUpperCase();	// the method
    boolean isDebug = hr.isTrue("debug");
    boolean follow = !hr.isTrue("nofollow");  // follow redirects
    boolean addClientHeaders = hr.isTrue("addclientheaders");
    String addHeaders = hr.get("addheaders"); // names of headers to add
    String getHeaders = hr.get("getheaders"); // retrieve response headers
    if (alt == null) {
      alt = "";
    }
    
    HttpURLConnection con;
    hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, "Fetching: (" + href + ")");
    try {
      URL target = new URL(href);
      con = (HttpURLConnection) target.openConnection();
      con.setInstanceFollowRedirects(follow);
      con.setUseCaches(false);

      // copy selected headers from client

      if (addClientHeaders) {
        addClientHeaders(hr, con);
      } else {
        con.addRequestProperty("User-agent", "Brazil-Fetch/" + Server.getVersion());
      }

      // add additional http headers

      if (addHeaders != null) {
        StringTokenizer st = new StringTokenizer(addHeaders);
        while (st.hasMoreTokens()) {
          String token = st.nextToken();
          String key = hr.request.getProps().getProperty(token + ".name");
          String value = hr.request.getProps().getProperty(token + ".value");
          if (key != null && value != null) {
            con.addRequestProperty(key, value);
            hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix,
                "Adding http header: " + key + ": " + value);
          }
        }
      }
      
      if (isDebug) {
        debug(hr, href + " Sending: " + con.getRequestProperties());
      }

      if (post != null && body == null) { // for backward compatibility
        body = post;
      }
      if (method.equals("GET") && post != null) {
        method = "POST";
      }
      int timeoutSec = Format.stringToInt(hr.get("timeout"), -1);
      if (timeoutSec >= 0) {
        int mult = hr.isTrue("timeoutInMs") ? 1 : 1000;  // for testing - change timeout to ms
        con.setConnectTimeout(timeoutSec * mult);
        con.setReadTimeout(timeoutSec * mult);
      }
      con.setRequestMethod(method);
      if (body != null && body.length() > 0) {
        if (con.getRequestProperty("content-type") == null) {
          con.setRequestProperty("content-type", type + ";  charset=" + encoding);
        }
        con.setDoOutput(true);
        OutputStream out = con.getOutputStream();
        // XXX should probably deal with an UnsupportedEncoding exception here
        byte[] bodyBytes = body.getBytes(encoding);
        out.write(bodyBytes);
        out.close();
        System.err.println("Sending postdata: " + bodyBytes.length + " (" + body + ")");
        if (isDebug) {
          debug(hr, href + " body: " + body);
        }
      }
     
      long startTime = System.currentTimeMillis();
      con.connect();
      // this must be first to force an IOException if something went wrong.
      // If we getResponseMessage() first, we get a runtime exception instead
      InputStream in = con.getInputStream();
      hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, con.getResponseMessage() + " " + 
              con.getHeaderFields());
      if (getHeaders != null) {
        for (Map.Entry<String, List<String>> entry : con.getHeaderFields().entrySet()) {
          String base = entry.getKey();
          if (base == null) {
            continue;
          }
          String key = getHeaders + "." + base.toLowerCase();
          List<String> values = entry.getValue();
          if (values.size() == 1) {
            hr.request.getProps().put(key, "" + values.get(0));
          } else if (values.size() > 1) {
            String build = values.get(0).toString();
            for (int i = 1; i < values.size(); i++) {
              build += ", " + values.get(i);
            }
            hr.request.getProps().put(key, "" + build);
          }
        }
        hr.request.getProps().put(getHeaders + ".status",
                "" + con.getResponseCode());
        hr.request.getProps().put(getHeaders + ".statusPhrase",
                "" + con.getResponseCode());
      }
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      byte[] buf = new byte[4096];
      int read;
      while ((read = in.read(buf)) != -1) {
        bo.write(buf, 0, read);
      }
      in.close();
      if (getHeaders != null) {
        hr.request.getProps().put(getHeaders + ".timeMillis",
                "" + (System.currentTimeMillis() - startTime));
      }
      
      //  Find a handler to process the content
      
      byte[] data = bo.toByteArray();
      for (ContentHandler handler : handlerMap.get(hr.prefix)) {
        hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, "processing: " + handler);
        if (handler.processContent(hr, con.getContentType(), data)) {
          break;
        }
      }
    } catch (IOException e) {
      System.err.println("Oops: " + e);
      if (hr.server.getLogLevel() > Server.LOG_LOG) {
        e.printStackTrace(System.err);
      }
      if (name != null) {
        hr.request.getProps().put(hr.prefix + name, alt);
      } else {
        hr.append("<!-- " + e + " -->" + alt);
      }
      hr.request.getProps().put(hr.prefix + "error", e.getMessage());
      // invent "plausible" response headers
      // It would be nice if appengine would tell us we timed-out, but it won't
      if (getHeaders != null) {
        hr.request.getProps().put(getHeaders + ".status", "504");
        hr.request.getProps().put(getHeaders + ".statusPhrase", e.getMessage());
      }
    }
    hr.killToken();
  }

  /**
   * @param hr
   * @param con
   */
  private void addClientHeaders(RewriteContext hr, HttpURLConnection con) {
    con.addRequestProperty("Via", "Brazil-Fetch/" + Server.getVersion());
    MimeHeaders map = new MimeHeaders();
    hr.request.getHeaders().copyTo(map);
    removePointToPointHeaders(map, false); // Stolen from HttpRequest
    map.remove("host");
    for (int i = 0; i < map.size(); i++) {
      con.addRequestProperty(map.getKey(i), map.get(i));
    }
  }

  /**
   * Alternate name so we can use both the Include and Fetch
   * templates in the same page.
   */

  public void
  tag_fetch(RewriteContext hr) {
    tag_include(hr);
  }

  static void
  removePointToPointHeaders(MimeHeaders headers, boolean response) {
    headers.remove("Connection");
    headers.remove("Proxy-Connection");
    headers.remove("Keep-Alive");
    headers.remove("Upgrade");

    if (response == false) {
      headers.remove("Proxy-Authorization");
    } else {
      headers.remove("Proxy-Authenticate");
      headers.remove("Public");
      headers.remove("Transfer-Encoding");
    }
  }
  
  /**
   * A content handler for processing HTTP results.  The handler should transform the results into
   * one or more properties, then return "true".  Returning false causes another handler to get a
   * crack at it.
   * @author suhler@google.com (Stephen Uhler)
   *
   */
  
  public interface ContentHandler {
    public boolean processContent(RewriteContext hr, String type, byte[] results)
        throws IOException;
  }
  
  /**
   * The default Content handler - the original behavior
   * @author suhler@google.com (Stephen Uhler)
   *
   */
  static class DefaultHandler implements ContentHandler {
    @Override
    @SuppressWarnings("unused")
    public boolean processContent(RewriteContext hr, String type, byte[] data) {
      String name = hr.get("name");
      String  results = new String(data);
      if (name != null) {
        hr.request.getProps().put(name, results);
      } else {
        hr.append(results);
      }
      return true;
    }
  }
}