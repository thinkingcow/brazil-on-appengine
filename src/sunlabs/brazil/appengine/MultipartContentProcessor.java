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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import sunlabs.brazil.appengine.MultipartUploadResourceHandler.Split;
import sunlabs.brazil.sunlabs.FetchTemplate.ContentHandler;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.util.StringMap;
import sunlabs.brazil.util.http.HttpInputStream;
import sunlabs.brazil.util.http.MimeHeaders;

/**
 * Do special processing for multipart output.
 * If the sub-type is "application/http", do even more special stuff.
 * <p>
 * If the "name" parameter is defined and the result is "multipart", then foreach part "n"
 * the properties 'name."n".content' and 'name."n".header.key=value' as well as
 * 'name.count' (the number of parts) and 'name.items' (0 1 2 ... n).
 * <p>
 * In addition, if the sub-part type is "application/http", the part-headers are added to the
 * headers for that part, and not included in the content.
 * <p>
 * This is intended for sites that support http batching.
 * @author suhler@google.com (Stephen Uhler)
 */

public class MultipartContentProcessor implements ContentHandler {
  
  @Override
  public boolean processContent(RewriteContext hr, String type, byte[] data) throws IOException {
    String name = hr.get("name");
    if (type != null && type.startsWith("multipart/") && name != null) {
      Properties props = hr.request.getProps();
      Split parts = new Split(data);
      int partCount = 0;
      StringBuffer partList = new StringBuffer();
      MimeHeaders partHeaders = new MimeHeaders();
      while (parts.nextPart()) {
        partHeaders.clear();
        partHeaders.read(fromBytes(parts.headerBytes()));
        if ("application/http".equalsIgnoreCase(partHeaders.get("content-type"))) {
          HttpInputStream hi = fromBytes(parts.contentBytes());
          props.put(name + "." + partCount + ".status", hi.readLine());
          partHeaders.read(hi);
          ByteArrayOutputStream co = new ByteArrayOutputStream();
          hi.copyTo(co);
          props.put(name + "." + partCount + ".content", co.toString());
        } else {
          props.put(name + "." + partCount + ".content", parts.content());
        }
        populate(props, name + "." + partCount + ".header.", partHeaders);
        partList.append(partCount).append(" ");
        partCount++;
      }
     props.put(name + ".items", partList.toString().trim());
     props.put(name + ".count", "" + partCount);
     return true;
    }
    return false;
  }
  
  
  /**
   * Turn a byte array in to an http input stream
   * @param data
   */
  private HttpInputStream fromBytes(byte[] data) {
    return new HttpInputStream(new ByteArrayInputStream(data));
  }
  
  @SuppressWarnings("rawtypes")
  private void populate(Properties props, String prefix, StringMap map) {
    Enumeration keys = map.keys();
    Enumeration values = map.elements();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      String value = (String) values.nextElement();
      props.put(prefix + key, value);
    }
  }
}