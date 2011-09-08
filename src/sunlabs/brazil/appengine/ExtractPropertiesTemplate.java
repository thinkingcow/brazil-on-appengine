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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Extract a properties formatted String into a set of properties, or extract a set of properties
 * into a properties formatted String.
 * 
 * Samples usage:
 *   <extractProperties name=content prefix=x>
 *   <foreach name=i glob=x.*>
 *      ... do something with the values ....
 *   </foreach>
 *   <genProperties name=content prefix=x>
 * @author suhler@google.com
 *
 */
public class ExtractPropertiesTemplate extends Template {
  
  /**
   * Extract a Properties formatted string into the request properties
   * <extractproperties name=propname | value="a=b ..." prefix="...">
   * @param hr
   */
  public void tag_extractproperties (RewriteContext hr) {
    debug(hr);
    hr.killToken();
    String name=hr.get("name");
    String value;
    if (name == null) {
      value = hr.get("value", "");
    } else {
      value = hr.request.getProps().getProperty(name, "");
    }
    String prefix=hr.get("prefix", "");
    
    ByteArrayInputStream bais = new ByteArrayInputStream(value.getBytes());
    if (prefix.equals("")) {
      try {
        hr.request.getProps().load(bais);
      } catch (IOException e) {}
      return;
    }
    if (!prefix.endsWith("."))  {
      prefix += ".";
    }
    
    Properties props = new Properties();
    try {
      props.load(bais);
      Properties set = hr.request.getProps();
      Enumeration<?> names = props.propertyNames();
	  while (names.hasMoreElements()) {
		String key = (String) names.nextElement();
		set.put(prefix + key, props.getProperty(key));
	  }
    } catch (IOException e) {}
  }

  /**
   * Generate a properties format text string from a properties prefix.  This
   * is the inverse of extractProperties.
   */
  @SuppressWarnings("deprecation")
  public void tag_genproperties(RewriteContext hr) {
    String prefix=hr.get("prefix");
    if (prefix == null) {
      debug(hr, "Prefix required");
      return;
    }
    String name = hr.get("name", prefix);
    
    Properties props = new Properties();
    Properties src = hr.request.getProps();
    if (!prefix.endsWith(".")) {
      prefix += ".";
    }
    int len = prefix.length();
    for (Entry<Object, Object>  item: src.entrySet()) {
      String key = (String) item.getKey();
      if (key.startsWith(prefix)) {
        props.put(key.substring(len), item.getValue());
      }
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    props.save(baos, "Generated from " + prefix);
    src.put(name, baos.toString());
  }
}