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

import sunlabs.brazil.server.Server;
import sunlabs.brazil.template.MacroTemplate;
import sunlabs.brazil.template.RewriteContext;

import java.io.IOException;

/**
 * Macro template that ignores ${..} substitutions between <action>..</action> tags.
 * @author suhler@google.com 
 */

public class AndrogenMacroTemplate extends MacroTemplate {
  private int count = 0;
  
  @Override
  public boolean init(RewriteContext hr) {
    count = 0;
    hr.request.getProps().put(hr.prefix + "subst", "true");
    return super.init(hr);
  }
  
  public void tag_action(RewriteContext hr) {
    hr.substAttributeValues();
    if (!hr.isSingleton()) {
      count++;
    }
  }
  
  /**
   * Reset the global macro table.
   * "src" is the absolute path to use to repopulate the table
   */
  
  public void tag_resetglobalmacros(RewriteContext hr) {
    hr.killToken();
    initial.clear();
    loadInitial(hr, hr.get("init", "init.macros"));
    hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix,
          "Reset initial macros: " + initial);
  }
 
  /**
   * @param hr  
   */
  public void tag_slash_action(RewriteContext hr) {
    count--;
  }
  
  /**
   * Fetch the initial macros from jdo instead of from the filesystem
   */
  @Override
  protected String getInitialSrc(RewriteContext hr, String init) throws IOException {
    String namespace = hr.get("namespace", "brazil");
    Resource resource = Resource.load(namespace + init);
    if (resource == null) {
      hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, "missing: " + init);
      throw new IOException("Missing initial macros: " + namespace + init);
    }
    String result = new String(resource.getData());
    hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, init + " (" + result + ")");
    return result;
  }
  
  // Templates are discovered by introspection in "this" class only
  
  @Override
  public void
  tag_definemacro(RewriteContext hr) {
    super.tag_definemacro(hr);
  }

  @Override
  public void
  defaultTag(RewriteContext hr) {
    String tag = hr.getTag();
    if (!macroTable.containsKey(tag) && count > 0) {
      hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, "nosub in " + hr.getTag());
      return;
    }
    super.defaultTag(hr);
  }
}