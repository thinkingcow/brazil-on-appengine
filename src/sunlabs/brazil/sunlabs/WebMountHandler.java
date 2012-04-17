/*
 * WebMountHandler.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2008-2009 Sun Microsystems, Inc.
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
 * Created by suhler on 08/12/15
 * Last modified by suhler on 09/06/30 10:29:39
 *
 * Version Histories:
 *
 * 1.11 09/06/30-10:29:39 (suhler)
 *   add noErrorReturn
 *
 * 1.10 09/06/03-10:50:18 (suhler)
 *   add "rootPath" to set all cookies paths to "/"
 *   added timeout on server requests
 *
 * 1.9 09/05/12-10:34:01 (suhler)
 *   allow dynamic reconfiguration
 *
 * 1.8 09/05/11-16:23:13 (suhler)
 *   better diagnostics
 *
 * 1.7 09/04/22-11:45:35 (suhler)
 *   added cookie rewriting
 *
 * 1.6 09/02/12-10:15:09 (suhler)
 *   - add getheaders param to add response headers to request
 *   - add doStyle to enable URL rewrites in inline styles
 *
 * 1.5 09/01/30-16:38:12 (suhler)
 *   changes for new HttpRequest
 *   .
 *
 * 1.4 09/01/30-08:50:19 (suhler)
 *   redo for new HttpRequest
 *
 * 1.3 09/01/16-16:30:34 (suhler)
 *   Replaced MapPage with MapHtmlUrl, and added url rewriting in stylesheets
 *
 * 1.2 09/01/08-09:29:51 (suhler)
 *   about to redo using the NAWS http stack
 *
 * 1.2 70/01/01-00:00:02 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 sunlabs/WebMountHandler.java
 *
 * 1.1 08/12/15-13:20:58 (suhler)
 *   date and time created 08/12/15 13:20:58 by suhler
 *
 */

package sunlabs.brazil.sunlabs;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.StringMap;
import sunlabs.brazil.util.http.HttpInputStream;
import sunlabs.brazil.util.http.MapHtmlUrl;
import sunlabs.brazil.util.http.MimeHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Handler for implementing "mounted" web sites.
 * This causes another web site (or sites) to "appear" inside our document root.
 *
 * All of the appropriate links in HTML documents from the mounted
 * site are rewritten, so they appear to be local references.
 * <p>
 * Properties:
 * <dl class=props>
 * <dt>proxy:port
 * <dd>If specified, use the indicated proxy. If ":port" is left off, 
 * port 80 is assumed.
 * <dt>getheaders<dd>if specified, all http headers returned from the
 * origin server are set in the request properties prefixed by the value
 * of "getheaders".
 * <dt>doStyle<dd>If true, look for (and rewrite) background URL's
 * defined in inline <code>style</code> attributes. (e.g.
 * <code>&lt;div style='background: url("/someimage.gif")'&gt;).
 * but requires the examination of every HTML tag.  Using background images
 * in style attributes in normally considered poor style.
 * <dt>debug<dd>If true, then all mapped URL's are emitted to stderr.
 * <dt>debugHeaders<dd>If true, then HTTP headers are emitted to stderr.
 * <dt>headers
 * <dd>A list of tokens that represent additonal http headers to 
 * add to this request.  For each token the the pair:
 * <i>token</i>.name and <i>token</i>.value are used.  For example:
 * <pre>
 *  headers=auth other
 *  auth.name=Authorization
 *  auth.value=Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
 *  other.name=X-foo
 *  other.value=bar
 * </pre>
 * would cause the headers:
 * <pre>
 * Authorization: QWxhZGRpbjpvcGVuIHNlc2FtZQ==
 * X-foo: bar
 * </pre>
 * to be added to the request.
 * <dt>mappings<dd>A list of tokens specifying which hosts are to be
 * mapped. (defaults to "[prefix].map").
 * <dt>[map].remote<dd>The remote website fetch
 * <dt>[map].local<dd>The specified URL on the local machine to use
 * Both "local" and "remote" may have ${..} constructs with are resolved
 * relative to the server properties object.
 * <dt>mapRefer<dd>If true the http "refer" header is mapped from the
 *     <i>local</i> namespace to the <i>remote</i> namespace
 * <dt>local, remote, fragment
 * <dd>These properties are added to the request and indicate the
 *     mapping from "local+fragment" to "remote+fragment".
 * <dt>configurationChanged
 * <dd>Normally the configuration options are read and processed once at
 * startup.  However, if <code>configurationChanged</code> is set, this 
 * handler will re-read it's configuration options, then <i>un-set</i>
 * <code>configurationChanged</code>.  This allows mounts to be modified
 * dynamically. (This is an experimental feature).
 * <dt>rootpath=true|false
 * <dd>If true, all cookied paths are set to "/", otherwise they
 * are translated relative to the mount point.
 * <dt>timelimit=[seconds]
 * <dd>Set the maximum time the server has to respond to this request.  Use
 * '0' for no timelimit.
 * <dt>noErrorReturn
 * <dd>If true, then if the proxy request fails, the response method
 * returns "false", and places the reason for failure in the "errorCode"
 * and "errorMsg" request properties.
 * Otherwise, and error response is generated. The default is (erroneously)
 * false for historical reasons.
 * </dl>
 * <p>
 * Example:
 * <pre>
 * mappings = a b ...
 * a.remote=http(s)://www.something.com/test/
 * a.local=http(s)://localhost:2345/something/
 * </pre>
 * A request for:
 * <code>/something/foo.html</code>on localhost:2345
 * will cause the page from:
 * <code>http://www.something.com/test/foo.html</code>
 * to be fetched.  In addition, any URL's in the returned document
 * that start with <code>http://www.something.com/test/</code> are
 * modified to start with <code>http://localhost:2345/something/</code>
 * instead.  This also applies for both <code>cookie</code> and 
 * <code>location</code> HTTP headers.
 * <p>
 * Notes:
 * <ul>
 * <li>Make sure both "local" and "remote" end in "/".
 * <li>If "remote" doesn't start at the document root, then
 * URL references in the mounted doc root that start with "../" don't remap
 * properly.
 * <li>I haven't added cookie domain rewriting yet.
 * <li>The original "host" header is preserved as the header
 * <code>X-host-orig</code> when requesting a page from the remote site.
 * <li>If multiple mappings are requested, references to any remote
 * site from another remote site are rewritten so the request is resolved
 * locally, then remapped.  Mappings are processed in order.
 * </ul>
 *
 * @author      Stephen Uhler
 * @version	2.8, 08/03/17
 */

public class WebMountHandler implements Handler {
  String prefix;	// our name
  String getheaders;	// prefix for target response headers
  MapHtmlUrl rewriter;	// our url rewriter
  Vector<Pair> map = new Vector<Pair>();	// pairs of local,remote
  boolean noErrorReturn=false;  // if true.
  boolean debugHeaders;	// emit all http headers for debugging

  /**
   * Remember the mounts
   */

  public boolean
  init(Server server, String serverPrefix) {
    this.prefix = serverPrefix;
    setup(server.getProps());
    noErrorReturn=Format.isTrue(server.getProps().getProperty(serverPrefix +
    "noErrorReturn"));
    server.log(Server.LOG_DIAGNOSTIC, serverPrefix, 
            "Maps: " + map);
    return true;
  }

  /**
   * Set up the maps.
   */

  void
  setup(Properties props) {
    StringTokenizer st= new StringTokenizer(
            props.getProperty(prefix + "mappings",prefix + ".map"));
    boolean debug = Format.isTrue( props.getProperty(prefix + "debug"));
    debugHeaders = Format.isTrue(
            props.getProperty(prefix + "debugHeaders"));
    boolean doStyle = Format.isTrue( props.getProperty(prefix + "doStyle"));
    getheaders = props.getProperty(prefix + "getheaders");
    while (st.hasMoreTokens()) {
      String name = st.nextToken();
      String local = Format.subst(props,
              props.getProperty(name + ".local"));
      String remote = Format.subst(props,
              props.getProperty(name + ".remote"));
      if (remote==null || local == null) {
        continue;
      }
      map.addElement(new Pair(local,remote));
    }
    rewriter = new MapHtmlUrl(new Mapper(map,debug), doStyle);
  }

  /**
   * If this is one of "our" url's, fetch the document from
   * the destination server, and return it as if it was local.
   */

  public boolean
  respond(Request request) throws IOException {
    String url = request.serverUrl().toLowerCase() + request.getUrl();

    // Allow the configuration to be changed!
    boolean slashOnly = Format.isTrue(request.getProps().getProperty(
            prefix + "rootpath"));
    boolean changed = Format.isTrue(request.getProps().getProperty(
            prefix + "configurationChanged"));

    int timelimit = Format.stringToInt(request.getProps().getProperty(
            prefix + "timelimit"), 0);

    if (changed) {
      map.clear();
      setup(request.getProps());
      request.getProps().removeProperty(prefix + "configurationChanged");
      request.log(Server.LOG_DIAGNOSTIC, prefix, 
              "New maps: " + map);
    }
    request.log(Server.LOG_DIAGNOSTIC, prefix, 
            "mount? " + url);

    boolean code=false;
    Pair item = null;
    for (int i=0;i<map.size();i++) {
      item = map.elementAt(i);
      if (url.startsWith(item.local)) {
        request.log(Server.LOG_DIAGNOSTIC, prefix, 
                "Using mount: " + item);
        break;
      }
      item=null;
    }

    if (item == null) {
      return code;
    }

    String fragment = url.substring(item.local.length());
    String fetch = item.remote + fragment;

    if (!request.getQuery().equals("")) {
      fetch += "?" + request.getQuery();
    }
    if (debugHeaders) {
      System.err.println("headers from client: \n" +
              request.getHeaders().toString("\n  "));
    }

    removePointToPointHeaders(request.getHeaders(), false);
    request.getHeaders().remove("if-modified-since");  // wrong spot XXX
    request.getHeaders().remove("Accept-Encoding");

    // We need to rewrite the "referrer" string here XXX
    if (Format.isTrue(request.getProps().getProperty(prefix + "mapRefer"))) {
      String referer = request.getHeaders().get("referer");
      System.err.println("Got referer: " + referer + " for: " + item);
    }

    URL urlTarget = new URL(fetch);
    HttpURLConnection con = (HttpURLConnection) urlTarget.openConnection();
    
    con.addRequestProperty("Via", "Brazil-Fetch/" + Server.getVersion());
    con.setInstanceFollowRedirects(false); // we need to rewrite the location headers

    MimeHeaders requestHeaders = new MimeHeaders();
    request.getHeaders().copyTo(requestHeaders);
    removePointToPointHeaders(requestHeaders, false); // Stoke from HttpRequest
    requestHeaders.remove("host");
    System.err.println("Adding headers from browser: " + requestHeaders);
    for (int i = 0; i < requestHeaders.size(); i++) {
      con.addRequestProperty(requestHeaders.getKey(i), requestHeaders.get(i));
    }

    String orig = request.getHeaders().get("host");
    if (orig != null) {
      // target.requestHeaders.remove("host");
      con.addRequestProperty("X-Host-Orig", orig);
    }
    con.addRequestProperty("host", hostPart(item.remote)); // ??
    con.addRequestProperty("Via", "Brazil-proxy/" + Server.getVersion());

    // get proxy (if any) [no longer supported]

    // add additional http headers

    String addHeaders = request.getProps().getProperty(prefix + "headers");
    if (addHeaders != null) {
      StringTokenizer st = new StringTokenizer(addHeaders);
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        String value = request.getProps().getProperty(token);
        if (value != null && !value.equals("")) {
          con.addRequestProperty(token, value);
        }
      }
    }

    try {
      if (request.getPostData() != null) {
        OutputStream out = con.getOutputStream();
        out.write(request.getPostData());
        request.log(Server.LOG_DIAGNOSTIC, prefix, "sending post data");
        out.close();
      }
      con.setReadTimeout(timelimit * 1000);

      request.log(Server.LOG_LOG, prefix, "fetching " + url + "...");
      con.connect();
      request.log(Server.LOG_LOG, prefix, "got " + con.getResponseMessage());
      if (debugHeaders) {
        System.err.println("headers from target: " + con.getHeaderFields());
      }

      Map<String, List<String>> headers = con.getHeaderFields(); // These are the response headers?
      MimeHeaders responseHeaders = new MimeHeaders();

      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        String key = entry.getKey();
        for (String value : entry.getValue()) {
          responseHeaders.add(key, value);
        }
      }
      removePointToPointHeaders(responseHeaders, true);

      /*
       * Fix the domain and path specifiers on set-cookie requests.
       * from thr headers recv'd from the target 
       */

      String prepend=null;	// What to prepend to the cookie path
      for (int i = 0; i < responseHeaders.size(); i++) {
        String key = responseHeaders.getKey(i).toLowerCase();
        if (key.equals("set-cookie")) {
          if (prepend == null) {
            prepend = dirPart(urlPart(item.local));
            System.err.println("cookie path prepend: " + prepend);
          }
          String cookie = responseHeaders.get(i);
          String newCookie = mapCookie(cookie, prepend, slashOnly);
          System.err.println("Mapping cookie: (" +
                  cookie + ") to (" + newCookie + ")");
          responseHeaders.put(i, newCookie);
        }
      }

      responseHeaders.copyTo(request.getResponseHeaders());

      // add all response headers to request

      if (getheaders != null) {
        Enumeration<String> keys = responseHeaders.keys();
        while(keys.hasMoreElements()) {
          String key = keys.nextElement();
          String full = getheaders + "." + key;
          if (request.getProps().containsKey(full)) {
            request.getProps().put(full,
                    request.getProps().getProperty(full) + ", " + responseHeaders.get(key));
          } else {
            request.getProps().put(full, responseHeaders.get(key));
          }
        }
        request.getProps().put(getheaders + ".status", "" + con.getResponseCode());
        request.getProps().put(getheaders + ".statusLine",con.getResponseMessage());
      }

      // Add other useful properties for the benefit of downstream
      // Processing 

      request.getProps().put(prefix + "local", item.local);
      request.getProps().put(prefix + "remote", item.remote);
      request.getProps().put(prefix + "fragment", fragment);

      if (debugHeaders) {
        System.err.println("headers to client: \n" +
                request.getResponseHeaders().toString("\n  "));
        System.err.println("Request properties: " + request.getProps());
      }

      HttpInputStream in = new HttpInputStream(con.getInputStream());
      if (shouldFilter(request.getResponseHeaders())) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.copyTo(out);
        System.err.println("Content {" + out.toString() + "}");

        ((Mapper)rewriter.getMap()).setPrefix(urlPart(item.local));
        byte[] content = modifyContent(request, out.toByteArray());

        if (content == null) {	// This is wrong!!
          request.log(Server.LOG_DIAGNOSTIC,
          "  null content, returning false");
          return false;
        } else {
          // String type = request.getResponseHeaders().get("Content-Type");
          request.setStatus(con.getResponseCode());
          request.sendResponse(content, null);
        }
      } else {
        request.log(Server.LOG_DIAGNOSTIC, prefix, "Not filtering");
        request.sendResponse(in, con.getContentLength(), null, con.getResponseCode());
      }
      in.close();
      code = true;
    } catch (InterruptedIOException e) {
      /*
       * Read timeout while reading from the remote side.  We use a
       * read timeout in case the target never responds.
       */
      code = doError(e, request, 504, "Timeout / No response");
    } catch (UnknownHostException e) {
      code = doError(e, request, 503, fetch +  " Not reachable");
    } catch (ConnectException e) {
      code = doError(e, request, 500, "Connection refused");
    } catch (IOException e) {
      code = doError(e, request, 500, "Error retrieving response: " + e);
    }
    return code;
  }

  /**
   * Strip the hostname from a URL
   */
  String urlPart(String url) {
    return url.substring(url.indexOf("/", 8));
  }

  /**
   * Return only the hostname
   */

  String hostPart(String url) {
    System.err.println("Got url: " + url);
    return url.substring(url.indexOf("/")+2, url.indexOf("/", 8));
  }

  /**
   * Return the directory part - no trailing slash.
   */

  String dirPart(String url) {
    return url.substring(0, url.lastIndexOf("/"));
  }

  /*
   * Either send an error response, set the properties
   */

  boolean doError(Exception e, Request request, int code, String msg) {
    request.log(Server.LOG_DIAGNOSTIC, prefix, "failed: " + e);
    if (noErrorReturn) {
      request.getProps().put(prefix + "errorCode", "" + code);
      request.getProps().put(prefix + "errorMsg", "" + msg);
      request.getProps().put(prefix + "errorDetail", e.getMessage());
      return false;
    } else {
      request.sendError(code, msg);
      return true;
    }
  }

  /**
   * See if the content needs to be filtered.
   * Return "true" if "modifyContent" should be called
   * @param headers	Vector of mime headers for data to proxy
   */

  protected boolean shouldFilter(MimeHeaders headers) {
    String type = headers.get("Content-Type");
    String location = headers.get("location");
    boolean result =  (location != null || 
            (type != null && type.indexOf("text/html") >= 0) ||
            (type != null && type.indexOf("text/css") >= 0));
    return result;
  }

  /**
   * Rewrite the links in an html or css file so they resolve correctly
   */

  public byte[] modifyContent(Request request, byte[] content) {
    byte[] result;

    String location = request.getResponseHeaders().get("location");
    if (location != null) {
      System.err.println("Got location: " + location);
      request.setStatus(302);	// XXX should copy target status
      String fixed = rewriter.getMap().mapUrl(location, false);
      fixed = mapLocation(fixed == null ? location: fixed);
      System.err.println("New location: " + location); 
      if (!fixed.equals(location)) { // XXX this step should be optional
        request.log(Server.LOG_DIAGNOSTIC, prefix, 
                "Mapping location: " + location + "->" + fixed);
        request.getResponseHeaders().put("Location", fixed);
      }
    }
    String type = request.getResponseHeaders().get("content-type");
    if (type==null) {
      // System.err.println("WebMount/modifyContent: No type info!");
      return content;
    }
    if (type.indexOf("text/css") >= 0) {
      result = rewriter.convertCSS(new String(content)).getBytes();
    } else {
      result = rewriter.convertHtml(new String(content)).getBytes();
    }
    request.getResponseHeaders().put("Content-Length", result.length);
    return result;
  }

  /**
   * Handler the location header as a special case, converting all
   * map "remote" prefixes in to "local" ones, including all query parameters
   * @param location
   */
  String mapLocation(String location) {
    for(Pair pair : map) {
      location = replace(location, pair.remote, pair.local);
    }
    return location;
  }

  private static String replace(String src, String from, String to) {
    Pattern pattern = Pattern.compile(Pattern.quote(from));
    return pattern.matcher(src).replaceAll(to);
  }

  /*
   * We need to fix the path and domain.
   * // XXX stolen from GenericProxyHandler - need to redo
   */

  String mapCookie(String line, String prefix1, boolean slash) {
    Cookie cookie = new Cookie(line);
    String path = cookie.get("path");
    if (slash) {
      cookie.put("path", "/");
    } else if (path == null) {
      cookie.put("path", prefix1);
    } else {
      cookie.put("path", prefix1 + path);
    }
    // cookie.put("path", "/");
    cookie.remove("domain"); // XXX broken!
    return cookie.toString();
  }

  /** A simple map: a pair of strings. */

  static class Pair {
    public String local;
    public String remote;

    Pair(String local, String remote) {
      this.local = local;
      this.remote = remote;
    }

    @Override
    public String toString() {
      return local + "->" + remote;
    }
  }

  /**
   * Define our URL converter.
   */

  static class Mapper implements MapHtmlUrl.Map {
    Vector<Pair> map;		// the set of site mappings
    String prefix = null;
    boolean debug;

    public Mapper(Vector<Pair> map, boolean debug) {
      this.map = map;
      this.debug = debug;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    /**
     * Convert the URL:
     * 1) Starts with '/' - prepend the local mount point
     * 2) Starts with a "remote" entry, replace with local one
     */
    public String mapUrl(String fix, boolean isStyle) {
      String result = null;
      if (fix.startsWith("/") && prefix != null) {
        result = prefix + fix.substring(1);
        if (debug) {
          System.err.println("Map Rel: " + fix + "=>" + result + 
                  (isStyle ? "  CSS" : ""));
        }
        return result;
      }

      if (!fix.startsWith("https://") && !fix.startsWith("http://")) {
        return null;
      }

      /*
       * Look for a match in our map table
       */

      for (int i=0;i<map.size();i++) {
        Pair item = map.elementAt(i);
        if (fix.startsWith(item.remote)) {
          result = item.local + fix.substring(item.remote.length());
          if (debug) {
            System.err.println("Map Abs: " + fix + "=>" + result + 
                    (isStyle ? "  CSS" : ""));
          }
          return result;
        }
      }
      return null;
    }
  }

  static void
  removePointToPointHeaders(MimeHeaders headers, boolean isResponse) {
    headers.remove("Connection");
    headers.remove("Proxy-Connection");
    headers.remove("Keep-Alive");
    headers.remove("Upgrade");

    if (isResponse == false) {
      headers.remove("Proxy-Authorization");
    } else {
      headers.remove("Proxy-Authenticate");
      headers.remove("Public");
      headers.remove("Transfer-Encoding");
    }
  }

  public static class Cookie {
    StringMap map;  // cookie parameters

    /**
     * Turn cookie parameters into a String Map
     * @param cookie        The cookie value
     */

    public Cookie(String cookie) {
      StringTokenizer st = new StringTokenizer(cookie, ";");
      map = new StringMap();
      map.put("value", st.nextToken().trim());
      while (st.hasMoreTokens()) {
        String param = st.nextToken();
        int index = param.indexOf('=');
        if (index > 0) {
          map.put(param.substring(0,index).trim().toLowerCase(),
                  param.substring(index+1).trim());
        } else {
          map.put(param.trim(),"");
        }
      }
    }

    public String get(String key) {
      return map.get(key);
    }

    public void put(String key, String value) {
      map.put(key, value);
    }

    public void remove(String key) {
      map.remove(key);
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer(map.get("value"));
      for (int i = 1; i < map.size(); i++) {
        sb.append("; ").append(map.getKey(i));
        sb.append("=").append(map.get(i));
      }
      return sb.toString();
    }
  }
}
