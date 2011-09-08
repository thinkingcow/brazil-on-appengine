/*
 * DelayHandler.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2003-2006 Sun Microsystems, Inc.
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
 * Version:  1.5
 * Created by suhler on 03/08/28
 * Last modified by suhler on 06/11/13 15:05:34
 *
 * Version Histories:
 *
 * 1.5 06/11/13-15:05:34 (suhler)
 *   move MatchString to package "util" from "handler"
 *
 * 1.4 06/08/07-16:36:48 (suhler)
 *   change delay parameters to "ms" and "sec" and fix the docs.
 *
 * 1.3 04/05/24-15:24:09 (suhler)
 *   doc fixes
 *
 * 1.2 04/04/28-14:52:38 (suhler)
 *   ??
 *
 * 1.2 03/08/28-14:16:08 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 sunlabs/DelayHandler.java
 *
 * 1.1 03/08/28-14:16:07 (suhler)
 *   date and time created 03/08/28 14:16:07 by suhler
 *
 */

/* MODIFICATIONS
 * 
 * Modifications to this file are derived, directly or indirectly, from Original Code provided by the
 * Initial Developer and are Copyright 2010-2011 Google Inc.
 * See Changes.txt for a complete list of changes from the original source code.
 */

package sunlabs.brazil.sunlabs;

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.MatchString;

import java.io.IOException;

/**
 * Handler or template for adding a delay into a response.
 * <p>
 * Properties:
 * <dl class=props>
 * <dt>prefix, suffix, match, glob<dd>Specify which url's to process.
 * <dt>delay<dd>The delay, in ms (defaults to 1000).
 * </dl>
 * Template:
 * &lt;delay sec=nnn ms=nnn /&gt;
 * <p>
 * Delay the specified amount of seconds plus msec - either or both
 * may be specified.
 */

public class DelayHandler extends Template implements Handler {
  MatchString isMine;            // check for matching url

  @Override
  public boolean
  init(Server server, String prefix) {
    isMine = new MatchString(prefix, server.getProps());
    return true;
  }

  /**
   * Delay before servicing this request.
   * <pre>&lt;delay sec=nnn ms=nnn&gt;</pre>
   * @throws IOException 
   */

  @Override
  public boolean
  respond(Request request) throws IOException {
    if (isMine.match(request.getUrl())) {
      String sec=request.getProps().getProperty(isMine.prefix() + "sec", "0");
      String ms=request.getProps().getProperty(isMine.prefix() + "ms", "0");
      delay(sec, ms);
    }
    return false;
  }

  /**
   * Pause for "ms" ms and/or "sec" seconds.
   */

  public void
  tag_delay(RewriteContext hr) {
    debug(hr);
    hr.killToken();
    delay(hr.get("sec", "0"), hr.get("ms", "0"));
  }

  void delay(String sec, String ms) {
    try {
      int delay = Integer.decode(ms).intValue() +
      1000 * Integer.decode(sec).intValue();
      Thread.sleep(delay);
    } catch (Exception e) {
    }
  }
}
