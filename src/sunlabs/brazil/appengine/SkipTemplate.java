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
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;

import java.util.Enumeration;


/**
 * Skip all ${} processing for android builder tags inside of <action>...</action> pairs
 * @author suhler@google.com 
 */

public class SkipTemplate extends Template {
  int count=0;
  
  @Override
  public boolean init(RewriteContext hr) {
    count=0;
    return super.init(hr);
  }
  public void tag_action(RewriteContext hr) {
    if (!hr.isSingleton()) {
      count++;
    }
  }
 
  public void tag_slash_action(@SuppressWarnings("unused") RewriteContext hr) {
    count--;
  }

  @SuppressWarnings("unchecked")
  public void
  defaultTag(RewriteContext hr) {
    if (count>0) {
      hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, "nosub in " + hr.getTag());
      return;
    }
    Enumeration e = hr.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      hr.put(key, hr.get(key));
    }
  }
}