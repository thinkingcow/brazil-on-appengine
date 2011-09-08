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

import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;

import java.util.StringTokenizer;

/**
 * Sift a token up/down in a list. Q&D version
 * @author suhler@google.com
 *
 * <sift name="property" sift="up|down|delete" item="text of matching item">
 */

public class SiftTemplate extends Template {
  private static final int NONE = -1;

  public void tag_sift (RewriteContext hr) {
    debug(hr);
    hr.killToken();
    
    String name = hr.get("name");
    if (name == null) {
      debug(hr, "Missing name");
      return;
    }
    
    String sift = hr.get("sift");
    if (sift == null) {
      debug(hr, "missing sift=up|down|delete");
      return;
    }
    
    String item = hr.get("item");
    if (item == null)    {
      debug(hr, "missing item");
      return;
    }
    
    String delim = hr.get("delim", " \t");
    StringTokenizer st =
        new StringTokenizer(hr.request.getProps().getProperty(name, ""), delim);
    String[] items = new String[st.countTokens()];
    int match = NONE;
    String value;
    for (int i = 0; st.hasMoreTokens(); i++) {
      items[i] = (value = st.nextToken());
      if (match == NONE && value.equals(item)) {
        match = i;
      }
    }
    
    if (match == NONE) {
      debug(hr, "No matching item");
      return;
    }
    
    if (sift.equals("up") && match > 0)  {
      items = swap(items, match, match - 1);
    } else if (sift.equals("down") && match + 1 < items.length) {
      items = swap(items, match, match + 1);
    } else if (sift.equals("delete")) {
      items = delete(items, match);
    } else {
      debug(hr, "Invalid operation");
      return;
    }
    
    StringBuffer sb = new StringBuffer();
    if (items.length > 0) {
      sb.append(items[0]);
    }
    for (int i = 1; i < items.length; i++) {
      sb.append(delim.substring(0, 1)).append(items[i]);
    }
    hr.request.getProps().put(name, sb.toString());
  }
  
  private String[] swap(String[] items, int x, int y) {
    String tmp = items[x];
    items[x] = items[y];
    items[y] = tmp;
    return items;
  }
  
  private String[] delete(String[] items, int x) {
    String[] result = new String[items.length - 1];
    for(int i = 0, j = 0; i < result.length; i++, j++) {
      result[i] = items[j];
      if (x == j) {
        i--;
      }
    }
    return result;
  }
}
