/*Copyright 2012 Google Inc.
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

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.MatchString;

import java.io.IOException;

/**
 * Look for JSON in post data, and extract into a String property "json".
 * Options will be added as needed. KISS for now.
 * @author suhler@google.com (Stephen Uhler)
 */

public class ExtractJsonHandler implements Handler {
  MatchString isMine;            // check for matching url

  @Override
  public boolean init(Server server, String prefix) {
    isMine = new MatchString(prefix, server.getProps());
    return true;
  }

  /**
   * If there is post data that looks like JSON, make it available
   * <dl class=props>
   * <dt>name<dd>Where to put the result (defaults to "json") in the local namespace
   * <dt>type<dd>Content type must contain this substring (defaults to "json")
   * </dl>
   */
  @Override
  public boolean respond(Request request) throws IOException {
    if (!isMine.match(request.getUrl())) {
      return false;
    }
	String resultName = request.getProps().getProperty(isMine.prefix() + "name", "json");
	String match = request.getProps().getProperty(isMine.prefix() + "type", "json");
	
    byte[] post = request.getPostData();
    String type = request.getHeaders().get("content-type");
    if (post != null && type != null && type.toLowerCase().indexOf(match) >= 0) {
      request.getProps().put(resultName, new String(post, "UTF8"));
    }
    return false;
  }
}