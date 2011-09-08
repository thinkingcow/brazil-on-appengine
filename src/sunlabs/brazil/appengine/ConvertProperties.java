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

// XXX this is broken

package sunlabs.brazil.appengine;

import sunlabs.brazil.properties.PropertiesList;
import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.sunlabs.MiscTemplate;
import sunlabs.brazil.util.MatchString;
import sunlabs.brazil.util.http.HttpUtil;

import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Shortcut for doing simple string conversion of properties as a side effect of "fetching" them.
 * Interpolates variable names of the form:  foo!type by converting the value of "foo" using
 * "type", where type is one of "sql", "javascript", "html", "url", or "hidetags".  See
 * @author suhler@google.com (Stephen Uhler)
 *
 */
public class ConvertProperties implements Handler {
  private MatchString isMine;
  private String seperator; // character that introduces the conversion
  
  public boolean init(Server server, String prefix) {
    isMine = new MatchString(prefix, server.getProps());
    seperator = server.getProps().getProperty(prefix + "seperator", "!");
    return true;
  }

  @Override
  public boolean respond(Request request) throws IOException {
    if (isMine.match(request.getUrl())) {
      PropertiesList pl = new PropertiesList(new ConvertProps(seperator), true);
      pl.addBefore(request.getServerProps());

      System.err.println("Installing ConvertProps: " + seperator + " for: " + request.getUrl());
    }
    return false;
  }
  
  // Define some conversions
  static class ConvertProps extends Properties {
    private String seperator;
    
    ConvertProps(String seperator) {
      this.seperator = seperator;
    }
    
    /**
     * Look for properties of the form foo!bar, and convert "foo" based on the conversion "bar".
     */
    
    @Override
    public String getProperty(String key) {
      String result = super.getProperty(key);
      System.err.println("get:" + key + "=>" + result);
      if (result == null && key.indexOf(seperator) > 0) {
        StringTokenizer st = new StringTokenizer(key, seperator);
        if (st.countTokens() == 2) {
          key = st.nextToken();
          String value = getProperty(key);
          result = convert(value, st.nextToken());
        } else {
          System.err.println("XXX oops");
        }
      }
      return result;
    }
    
    @Override
    public String getProperty(String key, String defaultValue)  {
      String result = getProperty(key);
      return result == null ? defaultValue : result;
    }
    
    public Object get(Object key) {
      if (key instanceof String) {
        return getProperty(key.toString());
      } else {
        return super.get(key);
      }
    }
    // XXX this should be combined with the MiscTemplate
    
    String convert(String value, String type) {
      System.err.println("Converting: " + value + " with: " + type);
      if (value == null) {
        return value;
      }
      if (type.equals("html")) {
        value =  HttpUtil.htmlEncode(value);
      } else if (type.equals("hidetags")) {
        value =  MiscTemplate.escapeTags(value);
      } else if (type.equals("sql")) {
        value =  MiscTemplate.escapeSQL(value);
      } else if (type.equals("javascript")) {
        value =  MiscTemplate.escapeJS(value);
      } else if (type.equals("url")) {
        value =  HttpUtil.urlEncode(value);
      }
      System.out.println("to (" + value + ")");
      return value;
    }
  }
}