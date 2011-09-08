/*
 * UrlMapperHandler.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2000-2008 Sun Microsystems, Inc.
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
 * Version:  2.8
 * Created by suhler on 00/02/14
 * Last modified by suhler on 08/12/19 11:14:55
 *
 * Version Histories:
 *
 * 2.8 08/12/19-11:14:55 (suhler)
 *   make inner class static
 *
 * 2.7 08/06/24-10:45:19 (suhler)
 *   do more useful ${} substitutions in replace ment strings.
 *   .
 *
 * 2.6 07/03/26-10:28:38 (suhler)
 *   - add remappinging arbitrary http request header (instead of just a url)
 *   - enable access to all http headers for ${} substitutions
 *
 * 2.5 07/03/21-15:57:50 (suhler)
 *   add "target" property to URL rewriter, so it can rewrite arbitrary HTTP
 *   headers.
 *
 * 2.4 05/12/21-10:26:19 (suhler)
 *   better diagnostics
 *
 * 2.3 04/11/03-08:29:49 (suhler)
 *   url.orig is now set in Request.java
 *
 * 2.2 04/10/26-11:28:15 (suhler)
 *   bug fix - make sure "export" ends in '.'.
 *
 * 2.1 02/10/01-16:36:36 (suhler)
 *   version change
 *
 * 1.20 02/02/04-14:33:04 (suhler)
 *   Make "host" available to mapper to allow better support for
 *   "wildcard" websites (e.g. <anything>.foo.com -> same machine)
 *
 * 1.19 01/11/28-13:08:37 (suhler)
 *   add ${user-agent} for "source" options, add docs
 *
 * 1.18 01/08/14-16:38:11 (suhler)
 *   doc lint
 *
 * 1.17 01/03/15-10:48:22 (suhler)
 *   set "url.orig" property if the url is modified
 *
 * 1.16 00/12/18-11:24:49 (suhler)
 *   allow ${...} substitutions in replacement strings
 *   .
 *
 * 1.15 00/12/11-13:27:36 (suhler)
 *   add class=props for automatic property extraction
 *
 * 1.14 00/12/08-16:46:49 (suhler)
 *   doc fixes
 *   .
 *
 * 1.13 00/11/06-10:47:56 (suhler)
 *   added source option
 *
 * 1.12 00/10/05-15:51:34 (cstevens)
 *   PropsTemplate.subst() and PropsTemplate.getProperty() moved to the Format
 *   class.
 *
 * 1.11 00/10/05-11:08:00 (suhler)
 *   sub-matches of the url can be saved in the request properties for later use.
 *   This will be more useful when the properties macro facility is integrated in.
 *   .
 *
 * 1.10 00/08/16-12:11:57 (suhler)
 *   changed Format to PRopsTemplate.getPRoperty
 *   .
 *
 * 1.9 00/06/20-08:56:02 (suhler)
 *   doc fix
 *
 * 1.8 00/06/05-11:03:08 (suhler)
 *   doc fixes
 *
 * 1.7 00/05/24-11:25:58 (suhler)
 *   add docs
 *
 * 1.6 00/05/19-11:48:59 (suhler)
 *   doc clean-ups
 *
 * 1.5 00/05/12-16:19:05 (suhler)
 *   added missong boundary check
 *
 * 1.4 00/04/20-11:50:33 (cstevens)
 *   copyright.
 *
 * 1.3 00/04/17-16:32:38 (cstevens)
 *   Parameter subst on url match.
 *
 * 1.2 00/02/27-19:51:15 (suhler)
 *   fixed typos
 *
 * 1.2 00/02/14-15:00:03 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 handlers/UrlMapperHandler.java
 *
 * 1.1 00/02/14-15:00:02 (suhler)
 *   date and time created 00/02/14 15:00:02 by suhler
 *
 */

/* MODIFICATIONS
 * 
 * Modifications to this file are derived, directly or indirectly, from Original Code provided by the
 * Initial Developer and are Copyright 2010-2011 Google Inc.
 * See Changes.txt for a complete list of changes from the original source code.
 */

package sunlabs.brazil.handler;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.regexp.Regexp;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

/**
 * Handler for mapping URL's or HTTP headers, or redirecting URLs
 * based on the contents of the current HTTP request.
 * Matches URL's (or arbitrary request properties) against a regexp pattern.
 * If there is a match, the URL
 * (or specified HTTP header)
 * is rewritten or the URL is redirected.
 * <p>
 * Properties:
 * <dl class=props>
 * <dt>match	<dd>The regexp to match a url.  May contain constructs
 *		of the form ${xxx}, which are replaced by the value of
 *		<code>request.props</code> for the key <i>xxx</i>
 *      If <code> match </code> begins with "!", then the sense of the match is inverted
 *      (The ! is not considered to be part of the re).
 * <dt>replace  <dd>The url to replace it with.  This may contain both
 *		regular expression sub-patterns, such as "\1", or
 *		variables of the form ${..} which are replaced with
 *		the equivalent augmented request properties. The
 *		request properties are augmented by looking first for
 *		the special variables:
 *		"method", "url", "protocol", "query", "serverUrl", "hostname",
 * 		"hostport".
 *		then in the HTTP headers, then in the request properties.
 *		The special variables "hostname" and "hostport" are derived
 *		from the http "host" header.
 * <dt>export   <dd>If set, use this as a properties prefix, and set 
 *		    request properties for each sub-expression in "match".
 *                  (E.g. [export]1 [export]2 ...).
 * <dt>redirect <dd>If set, the request is redirected instead of being rewritten
 * <dt>ignoreCase <dd>If set, the case of the expression is ignored.
 * <dt>source
 * <dd>
 * If set, then this string is used instead of the
 * url as the <i>source</i> of the match. Variable substitution
 * using ${xxx} is performed on <i>source</i>, which, if unset, 
 * defaults to "${url}".  If set, ${} substitutions 
 * are taken from the special variables (see "replace", above), followed
 * by any http request headers, followed by names in the current Request
 * object.
 * The <i>source</i> property
 * is obtained at init time, but evaluated (for ${...}) at every request.
 * <p>
 * As an example, the configuration:<br><code>
 * prefix.source=${user-agent}!${url}<br>
 * prefix.match=Lynx.*!(.*)<br>
 * prefix.replace=/text\\1<br></code>
 * could cause all browsers with "Lynx" in their user agent header
 * to the "text" sub-directory.
 * <dt>target</dt>
 * <dd>By default, this handler modifies the request URL.  If target
 * is specified, it names an HTTP header to be replaced instead of
 * the URL.  The "target" is ignored if "redirect" is specified, and
 * a new header is created if the "target" header doesn't already exist.
 * </dl>
 *
 * @author      Stephen Uhler
 * @version     2.8, 08/12/19
 */

public class UrlMapperHandler implements Handler {
    final static String MATCH = "match";
    final static String SOURCE = "source";
    final static String REPLACE = "replace";
    final static String REDIRECT = "redirect";
    final static String NOCASE = "ignoreCase";
    final static String EXPORT = "export";
    final static String TARGET = "target";

    Regexp re = null;		// the expression to match
    String replace;		// the replacement string
    String export;		// the prefix for exported submatches
    String source;		// the source for the match
    String prefix;		// our properties prefix
    String target;		// the replacement target (e.g. the URL)
    boolean redirect = false;	// True to redirect instead of rewrite
    boolean invert = false;     // invert sense of match
    Server server;

    @Override
    public boolean
    init(Server serverArg, String prefixArg) {
	this.server=serverArg;
	this.prefix = prefixArg;
	boolean noCase = (server.getProps().getProperty(prefixArg + NOCASE) != null);
	redirect = (server.getProps().getProperty(prefixArg + REDIRECT) != null);
	replace = server.getProps().getProperty(prefixArg + REPLACE);
	export = server.getProps().getProperty(prefixArg + EXPORT);
	source = server.getProps().getProperty(prefixArg + SOURCE);
	target = server.getProps().getProperty(prefixArg + TARGET);

	if (replace == null) {
	    server.log(Server.LOG_WARNING, prefixArg, "missing: " + REPLACE);
	    return false;
	}
	String match = server.getProps().getProperty(prefixArg + MATCH);
	if (match == null) {
	    server.log(Server.LOG_WARNING, prefixArg, "missing: " + MATCH);
	    return false;
	}
	if (match.startsWith("!")) {
	  match = match.substring(1);
	  invert = true;
	}
	match = Format.subst(server.getProps(), match);
	try {
	    re = new Regexp(match, noCase);
	} catch (Exception e) {
	    server.log(Server.LOG_WARNING, prefixArg, "Bad expression:" + e);
	}
	return (re != null);
    }

    /**
     * If this request matches the expression, rewrite it.
     */

    @Override
    public boolean
    respond(Request request) throws IOException {
	String formatted;
	Properties extras = getExtras(request);
	if (source != null) {
	    formatted = Format.subst(extras, source);
	} else {
	    formatted = request.getUrl();
	}
	request.log(Server.LOG_DIAGNOSTIC, prefix, "Matching url " + formatted);
	String sub = re.sub(formatted, replace);
	if ((sub == null) ^ invert) {
	    return false;
	}
	if (sub == null) {
	  sub = replace;
	}
	sub = Format.subst(extras, sub);
	if (export != null) {
	    if (!export.endsWith(".")) {
		export += ".";
	    }
	    String[] parts = new String[re.subspecs()];
	    re.match(request.getUrl(), parts);
	    for (int i=0; i<parts.length; i++) {
		request.getProps().put(export + i, parts[i]);
	    }
	}

	request.log(Server.LOG_DIAGNOSTIC, prefix, "Mapping " +
		formatted + " => " + sub);
	if (redirect) {
	    request.redirect(sub,null);
	    return true;
	} else if (target != null) {
	    request.log(Server.LOG_DIAGNOSTIC, prefix, "Setting header: " +
		target + " => " + sub);
	    request.getHeaders().put(target, sub);
	} else {
	    request.setUrl(sub);
	}
	return false;
    }

    /**
     * Add extra properties ontpo the request for ${} substitutions
     */

    Properties getExtras(Request request) {
	MapProperties map = new MapProperties(request.getProps(),
		request.getHeaders());
	map.addItem("method", request.getMethod());
	map.addItem("url", request.getUrl());
	map.addItem("protocol", request.getProtocol());
	map.addItem("query", request.getQuery());
	map.addItem("serverUrl", request.serverUrl());

	// XXX stolen from SetTemplate - should consolidate

	String host = request.getHeaders().get("host");
	if (host == null) {
	    host = server.getHostName();
	}
	if (host != null) {
	    int indx = host.indexOf(":");
	    if (indx < 0) {
		map.addItem("hostname", host);
		if (server.getProtocol().equals("http")) {
		    map.addItem( "hostport","80");
		}
		if (server.getProtocol().equals("https")) {
		    map.addItem("hostport","443");
		}
	    } else {
		map.addItem("hostname", host.substring(0,indx));
		map.addItem("hostport", host.substring(indx+1));
	    }
	}
	return map;
    }

    /**
     * Look in a dictionary first, then the provided properties.
     * XXX There are lots of little classes like this sprinkled
     * throught the code.  They should be consolidated. This is
     * for Format.subst, and is not a complete implementation.
     */

    public static class MapProperties extends Properties {
	Properties realProps;
	Dictionary<String, String> dict;
	Properties extra = null;

	public MapProperties(Properties props, Dictionary<String, String> dict) {
	    realProps = props;
	    this.dict = dict;
	}

	public void addItem(String name, String value) {
	    if (extra == null) {
		extra = new Properties();
	    }
	    extra.put(name, value);
	    // System.out.println("adding: " + name + "=(" + value + ")");
	}

	@Override
  public String getProperty(String key, String dflt) {
	    String result = null;

	    if (extra != null) {
		result = extra.getProperty(key);
	    }
	    if (result == null) {
		result = dict.get(key);
	    }
	    if (result == null) {
		result = realProps.getProperty(key, dflt);
	    }
	    return result;
	}

	@Override
  public String getProperty(String key) {
	    return getProperty(key, null);
	}
    
    @Override
    public String toString() {
      return realProps + "|" + dict + ((extra == null) ? "" : " | " + extra);
    }
  }
}
