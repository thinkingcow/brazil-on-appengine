/* Copyright 2012 Google Inc.
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

import sunlabs.brazil.properties.PropertiesList;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.regexp.Regexp;

import java.util.Properties;

/**
 * Template holder for property name aliasing.
 * This probably belongs in the MiscTemplate.
 * @author suhler@google.com (Stephen Uhler)
 *
 */
public class AliasTemplate extends Template {
  
  /**
   * Specify an alias to refer to a set of variables.  This permits long
   * variable names to be represented more compactly.  Unlike the "map_names"
   * tag in the MiscTemplate which copies the data into new names, this one
   * simply aliases the name.
   * Options:
   * <dl>
   * <dt>from<dd>A regular expression that will match the name of the alias
   * <dt>to<dd>The substituted value of the real variable name to lookup
   * @param hr
   */
  public void tag_alias(RewriteContext hr) {
    String from = hr.get("from");
    String to = hr.get("to");
    
    debug(hr);
    hr.killToken();
    
    if (from == null || to == null) {
      debug(hr, "Missing from or to attribute");
      return;
    }
    try {
      hr.request.addSharedProps(new AliasProps(hr.request.getProps(), from, to));
    } catch (IllegalArgumentException e) {
      debug(hr, e.getMessage());
    }
  }
  
  private static class AliasProps extends Properties {
    private Properties baseProperties;
    private Regexp re;
    private String to;
    
    public AliasProps(PropertiesList props, String from, String to) {
      this.baseProperties = props;
      this.re = new Regexp(from);
      this.to = to;
    }
    
    @Override
    public String getProperty(String key) {
      String map;
      if (re.match(key) != null && !(map = re.subAll(key, to)).equals(key)) {
        System.err.println("alias mapping:" + key + " -> " + map);
        return baseProperties.getProperty(map);
      }
      return null;
    }
  }
}