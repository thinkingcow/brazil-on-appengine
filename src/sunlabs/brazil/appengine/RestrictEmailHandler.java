/*Copyright 2010-2011 Google Inc.
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
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.Glob;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Handler to grant/deny access using a table of authorized users.
 * - Supports dynamic tables of additional users
 * - Supports static patterns of "default" users
 * <p>
 * If an "id" isn't in the table, and doesn't match the pattern, it is denied.
 * <dl class=props>
 * <dt>id
 * <dd>request property to match against allow/deny patterns. Defaults to "email"
 * Id no request property exists, it is set to "bogus".
 * <dt>patterns
 * <dd>a white space delimited list of glob patterns, checked in order that are used
 * to allow or deny an "id" to this site.   If the pattern in prefixed with "!" it is
 * a "deny" pattern, otherwise a match is allowed.
 * <dt>resource
 * <dd>The name of the appengine resource containing a java properties format file
 * of specific "id"'s that are to be allowed (id=true) or denied (id=false).  The
 * resource file is checked first.  If an "id" isn't in the table, then the "patterns" 
 * list is consulted.
 * <dt>instance
 * <dt>An arbitrary token that is used to associate this handler instance, with the corrosponding
 * template instance that can dynamically update the table.  The default is the handler prefix
 * (which should work be fine in most circumstances).
 * 
 * @author suhler@google.com
 */
public class RestrictEmailHandler extends Template implements Handler {
  private String prefix;        // handler prefix
  private String namespace;     // which filesystem to use
  private String idProperty;    // request property to match on (defaults to email)
  private String patterns;      // list of glob patterns matching privilaged users
  private String resourceName;      // name of properties format file containing id matches
  private Properties accessList;
  
  // XXX we need a better way for our template to find our handler instance
  private static HashMap<String,RestrictEmailHandler> registry = 
      new HashMap<String, RestrictEmailHandler>();
  
  @Override
  public boolean init(Server server, String prefix) {
    this.prefix = prefix;
    namespace = server.getProps().getProperty(prefix + "namespace", "brazil");
    idProperty = server.getProps().getProperty(prefix + "id", "email");
    patterns = server.getProps().getProperty(prefix + "patterns", "");
    resourceName = server.getProps().getProperty(prefix + "resource");
    server.log(Server.LOG_DIAGNOSTIC, prefix, "patterns: " + patterns);
    propsFromResource();
    String instance = server.getProps().getProperty(prefix + "instance", prefix);
    registry.put(instance, this);
    return true;
  }

  /**
   * Check for an "id" in the list to accept/deny.  Otherwise, check each
   * glob pattern.
   */
  @Override
  public boolean respond(Request request) throws IOException {
    String check = request.getProps().getProperty(idProperty, "bogus");
    
    String token = accessList.getProperty(check);
    if (Format.isTrue(token)) {
      request.log(Server.LOG_DIAGNOSTIC, prefix, "Allowed (list): " + check);
      return false;
    }
    if (Format.isFalse(token)) {
      request.sendError(403, "Access denied", check + " in list");
      return true;
    }
    
    request.log(Server.LOG_DIAGNOSTIC, prefix, "Checking (" + check  + ") in: " + patterns);
    StringTokenizer st = new StringTokenizer(patterns);
    while(st.hasMoreElements()) {
      String glob = st.nextToken();
      if (glob.startsWith("!") && Glob.match(glob.substring(1), check)) {
        request.sendError(403, "Access denied", "matches " + glob);
        return true;
      } else if (Glob.match(glob, check)) {
        request.log(Server.LOG_DIAGNOSTIC, prefix, "Allowed: " + check + " matches " + glob);
        return false;
      }
    }
    request.sendError(403, "Access denied", "no rule");
    return true;
  }
  
  /**
   * Find our hander instance and refresh the ACL file
   * &lt;refreshacls instance=&quot;handler-prefix&quot; /&gt;
   * - The property "lastmod" is set to the last mod time of the acl resource, or 0 if there
   *   was an error.
   * - The property "count" is set the the number of entries in the acl file
   * @param hr
   */
  
  public void tag_refreshacls(RewriteContext hr) {
    debug(hr);
    hr.killToken();
    RestrictEmailHandler handler = registry.get(hr.get("instance", hr.prefix));
    long lastMod = 0;
    if (handler != null) {
      lastMod = handler.propsFromResource();
      hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, "Reloading ACL's");
      hr.request.getProps().put(hr.prefix + "count", "" + handler.aclCount());
    } else {
      hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, "Can't find handler instance");
    }
    hr.request.getProps().put(hr.prefix + "lastmod", "" + lastMod);
  }
  
  private long propsFromResource() {
    Properties props = new Properties();
    Resource resource = Resource.load(namespace + resourceName);
    long lastMod = 0;
    if (resource != null) {
      ByteArrayInputStream bais = new ByteArrayInputStream(resource.getData());
      try {
        props.load(bais);
        bais.close();
        lastMod = resource.getLastMod();
      } catch (IOException e) { /* can't happen */ }
    }
    accessList = props;
    return lastMod;
  }
  
  private int aclCount() {
    return  accessList.size();
  }
}