/*
 * DebugTemplate.java
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
 * Contributor(s): suhler.
 *
 * Version:  2.4
 * Created by suhler on 00/11/17
 * Last modified by suhler on 09/04/22 11:49:02
 *
 * Version Histories:
 *
 * 2.4 09/04/22-11:49:02 (suhler)
 *   add output options to debugTemplate
 *
 * 2.3 03/07/15-09:11:19 (suhler)
 *   Doc fixes for pdf version of the manual
 *
 * 2.2 02/11/05-15:09:37 (suhler)
 *   Merged changes between child workspace "/home/suhler/brazil/naws" and
 *   parent workspace "/net/mack.eng/export/ws/brazil/naws".
 *
 * 2.1 02/10/01-16:36:50 (suhler)
 *   version change
 *
 * 1.5.1.1 02/09/20-10:54:09 (suhler)
 *   allow debug=0 to turn off debugging
 *
 * 1.5 02/07/24-10:46:01 (suhler)
 *   doc updates
 *
 * 1.4 00/12/11-13:31:09 (suhler)
 *   doc typo
 *
 * 1.3 00/11/20-13:21:58 (suhler)
 *   doc fixes
 *
 * 1.2 00/11/19-10:30:31 (suhler)
 *   only print if debug is on
 *
 * 1.2 00/11/17-09:32:55 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 handlers/templates/DebugTemplate.java
 *
 * 1.1 00/11/17-09:32:54 (suhler)
 *   date and time created 00/11/17 09:32:54 by suhler
 *
 */

/* MODIFICATIONS
 * 
 * Modifications to this file are derived, directly or indirectly, from Original Code provided by the
 * Initial Developer and are Copyright 2010-2011 Google Inc.
 * See Changes.txt for a complete list of changes from the original source code.
 */

package sunlabs.brazil.template;

import sunlabs.brazil.util.Format;

import java.io.Serializable;

/**
 * Template class for printing stuff to stderr (for template debugging).
 * This class is used by the TemplateHandler.
 * <p>
 * A new HTML tag,  
 * <code>&lt;debug&gt;</code> is defined.  Any text between the
 * <code>&lt;debug</code> and <code>&gt;</code> is printed on stderr,
 *  along with the
 * session id and the url.  Variable substitutions of the form
 * ${...} are performed on the text.
 * <p>
 * The attribute "output" can be used to direct the output to either
 * the console (output="console"), inline (output="inline"), 
 * or both (output="both") which is the default.  The "output" attribute
 * may be set as a configuration option as well. The Attribute "limit" may
 * be uses to limit the maximum length of the output (in characters).
 * <p>
 * The property <code>debug</code> must be present for this template
 * to function.  Otherwise, all <code>debug</code> tags are removed.
 * <dl class=props>
 * <dt><code>debug</code>
 * <dd> If this configuration parameter is true, debugging is enabled.
 * </dl>
 *
 * @author		Stephen Uhler
 * @version		%V% 09/04/22
 */

public class DebugTemplate extends Template implements Serializable {
    private static final String DEBUG = "debug";
    transient boolean shouldDebug;

    @Override
    public boolean
    init(RewriteContext hr) {
	String dbg = hr.request.getProps().getProperty(hr.prefix + DEBUG);
	shouldDebug = Format.isTrue(dbg);
	return true;
    }

    public void
    tag_debug(RewriteContext hr)
    {
	if (shouldDebug) {
	    String stuff = hr.getArgs();
	    if (stuff != null) {
		String map = Format.subst(hr.request.getProps(), stuff);
		String output = hr.get("output", "both");
		int max = Format.stringToInt(hr.get("limit"), 0);
		if (max > 0) {
		    map = map.substring(0, max);
		}
		if (output.equals("both") || output.equals("console")) {
		    System.err.println("Debug " + hr.sessionId + ":" +
		          hr.request.getUrl() + " " + map);
		}
		if (output.equals("both") || output.equals("inline")) {
		    hr.append("\n<!--DEBUG " + stuff + "\n    " +map + "-->\n");
		}
	    }
	}
	hr.killToken();
    }
}
