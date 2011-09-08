/*Copyright 2011 Google Inc.
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 */

package sunlabs.brazil.appengine;

import sunlabs.brazil.server.ChainHandler;
import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.MatchString;
import sunlabs.brazil.util.regexp.Regexp;

import java.io.IOException;


/**
 * Choose between two handlers to run based on a Url matching a regular expression
 * <dl class=props>
 * <dt>prefix, suffix, glob, match<dd>See MatchString.
 * <dt>if<dd>A regular expression that matches a Url (required)
 * <dt>nocase<dd>If true, the regular expression is case insensitive
 * <dt>then<dd>The name of the handler (or handler class) to run if the URl matches
 * <dt>else<dd>The name of the handler (or handler class) to run if the URl does not matche
 * </dl>
 * Either "then" or "else" (or both) must be specified.
 * @author suhler@google.com (Stephen Uhler)
 *
 */

public class IfHandler implements Handler {
  private MatchString isMine;            // check for matching url
  private Regexp re;
  private Handler thenHandler = null;
  private Handler elseHandler = null;
  
  @Override
  public boolean init(Server server, String prefix) {
	isMine = new MatchString(prefix, server.getProps());
	try {
	  re = new Regexp(server.getProps().getProperty(prefix + "if"), 
    	    Format.isTrue(server.getProps().getProperty(prefix + "nocase")));
	} catch (IllegalArgumentException e) {
	  return false;
	}
	String handlerName = server.getProps().getProperty(prefix + "then");
	if (handlerName != null) {
	  thenHandler = ChainHandler.initHandler(server, prefix, handlerName);
	}
	handlerName = server.getProps().getProperty(prefix + "else");
	if (handlerName != null) {
      elseHandler = ChainHandler.initHandler(server, prefix, handlerName);
	}
	
    return thenHandler != null || elseHandler != null;
  }

  @Override
  public boolean respond(Request request) throws IOException {
	if (!isMine.match(request.getUrl())) {
	  return false;
	}
	if (re.match(request.getUrl()) != null) {
	  return thenHandler != null ? thenHandler.respond(request) : false;
	} else {
	  return elseHandler != null ? elseHandler.respond(request) : false;
	}
  }
}