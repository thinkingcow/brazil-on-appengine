/*
 * MapHtmlUrl.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2009 Sun Microsystems, Inc.
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
 * Version:  1.4
 * Created by suhler on 09/01/16
 * Last modified by suhler on 09/02/12 14:44:22
 *
 * Version Histories:
 *
 * 1.4 09/02/12-14:44:22 (suhler)
 *   doc fixed
 *
 * 1.3 09/01/16-16:31:05 (suhler)
 *   add getMap()
 *
 * 1.2 09/01/16-15:03:01 (suhler)
 *   de static-ify
 *   .
 *
 * 1.2 70/01/01-00:00:02 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 sunlabs/MapHtmlUrl.java
 *
 * 1.1 09/01/16-13:59:27 (suhler)
 *   date and time created 09/01/16 13:59:27 by suhler
 *
 */

/* MODIFICATIONS
 * 
 * Modifications to this file are derived, directly or indirectly, from Original Code provided by the
 * Initial Developer and are Copyright 2010-2011 Google Inc.
 * See Changes.txt for a complete list of changes from the original source code.
 */

package sunlabs.brazil.util.http;

import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.HtmlRewriter;
import sunlabs.brazil.util.regexp.Regexp;
import sunlabs.brazil.util.regexp.Regsub;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * Utility class to systematically rewrite links inside of html pages and/or
 * stylesheets. 
 * The "convertHtml()" method finds all URL's in the supplied HTML document, 
 * including embedded stylesheets and style attributes, and calls "mapURL"
 * for each URL, allowing the URL to be rewritten.
 * (This is intended to replace the MapPage class someday).
 *
 * @author      Stephen Uhler
 * @version	@(#)MapHtmlUrl.java	1.4
 */

public class MapHtmlUrl {
    Hashtable<String, String> tagMap;	// tag attributes to map
    Map map;		// Our url rewriter
    boolean doStyle;	// Map urls in inline style tags

    /**
     * Create a url mapper with the supplied url rewriter.
     * @param map	A class that maps URL's to URL's.
     * @param doStyle	There can be URL's inside of inline style
     *			attributes.  It's not common, and potentially
     *			expensive to look for them, so you pick.
     */

    public MapHtmlUrl(Map map, boolean doStyle) {
	this.map = map;
	this.doStyle = doStyle;
	tagMap = init(null);
    }

    /**
     * Add a tag/attribute pair to be mapped.
     * @param name	An html tag
     * @param attr	The attribute that is a URL.
     */

    public void addTag(String name, String attr) {
	doTag(name, attr);
    }

    /**
     * Remove a tag for mapping
     * @param name	The html tag that shouldn't be examined
     */

    public void delTag(String name) {
	doTag(name, null);
    }

    /**
     * Return our mapper instance
     */

    public Map getMap() {
	return map;
    }

    /**
     * Create a hashtable containing all the standard tag/attribute
     * pairs in HTML whose values are URL's.
     */

    public static Hashtable<String, String> init(Hashtable<String, String> h) {
	if (h==null) {
	    h = new Hashtable<String, String>(23);
	}
    	h = new Hashtable<String, String>(19);
        h.put("a","href");
        h.put("applet","codebase");
        h.put("area","href");
        h.put("base","href");
        h.put("body","background");
        h.put("embed","src"); 
        h.put("form","action");
        h.put("frame","src");
        h.put("img","src");
        h.put("layer","src");
        h.put("link","href");
        h.put("object","codebase");
        // h.put("param","value"); // Not sure about this one
        h.put("td","background");
        h.put("th","background");
        h.put("input","src");	// image input items?
        h.put("script","src");	// ???
	return h;
    }

    /**
     * Add or remove a tag from the rewrite list
     */

    void doTag(String name, String attribute) {
        if (attribute == null) {
	    tagMap.remove(name.toLowerCase());
	} else {
	    tagMap.put(name.toLowerCase(), attribute.toLowerCase());
	}
    }

    /**
     * Rewrite all the url's in this document.  This is accomplished
     * by iterating through the document and replacing all URL's with
     * the result of mapUrl() from our Map instance.
     *
     * @param html	The HTML to be processed.
     * @return		The same HTML, will all URL's rewritten.
     */

    public String convertHtml(String html) {
      HtmlRewriter hr = new HtmlRewriter(html);
      while (hr.nextTag()) {
        String tag = hr.getTag().toLowerCase();
        if (tag.equals("style")) {
          hr.nextToken();
          hr.append(convertCSS(hr.getBody()));
        }

        if (doStyle) {
          String style = hr.get("style");
          if (style != null) {
            hr.put("style",  convertCSS(style));
          }
        }

        String param = tagMap.get(tag);
        if (param == null) {
          continue;
        }
        String value = hr.get(param);
        if (value == null) {
          continue;
        }
        String fixed = map!= null ? map.mapUrl(value, false): null;
        if (fixed != null) {
          hr.put(param, fixed);
        }
      }
      return hr.toString();
    }

    // handle the usual suspects only for now
    static Regexp CssRe = new Regexp("url[(]([^)]+)[)]");

    /**
     * Rewrite URL's in style sheets.
     * This works via repeated calls to mapUrl().
     * (This implementation is preliminary).
     * <p>
     * Look for url(href) in a stylesheet value. "href" may
     * be delimited by (") or ('), and surrounding whitespace is ignored.
     */

    public String convertCSS(String style) {
	Regsub sub = new Regsub(CssRe, style);
	StringBuffer sb = new StringBuffer();

	while(sub.nextMatch()) {
	    sb.append(sub.skipped());
	    String rpl = map.mapUrl(
		    Format.deQuote(sub.submatch(1).trim()), true);
	    if (rpl != null) {
		sb.append("url(").append(rpl).append(")");
	    } else {
		sb.append(sub.matched());
	    }
	}
	sb.append(sub.rest());
	return sb.toString();
    }

    /**
     * Every URL calls this to map the string.
     */

    public interface Map {

	/**
	 * Map a url.
	 * @param src	The original URL
	 * @param isStyle	"true" if this is a URL in a stylesheet
	 * @return	The converted string, or "null" if there is no change.
	 */
	public String mapUrl(String src, boolean isStyle);
    }

    /** test implementation of Map. */
    
    static class TestMap implements Map {
        @Override
        public String mapUrl(String url, boolean isStyle) {
	    return "[" + url + "]";
	}
    }

    /**
     * test this stuff out, sort of.
     * Usage: MapHtmlUrl markup<br>
     * - read markup from "markup" file
     * - write markup to stdout
     * - surround all url's with []
     */

    public static void main(String argv[]) throws IOException {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	HttpInputStream in = new HttpInputStream(new FileInputStream(argv[0]));
	in.copyTo(out);
	String src = out.toString();
	MapHtmlUrl map = new MapHtmlUrl(new TestMap(), true);
	String dst = map.convertHtml(src);
	System.out.println(dst);
    }
}
