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

import sunlabs.brazil.server.FileHandler;
import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.MatchString;
import sunlabs.brazil.util.http.HttpUtil;

import java.io.IOException;

/**
 *Download a file from the in-memory cache.
 * (See the CachedTemplateHandler for details)
 * @author suhler@google.com
 *
 */
public class DownloadResourceHandler implements Handler {
  MatchString isMine;
  String namespace;

  @Override
  public boolean init(Server server, String prefix) {
    isMine = new MatchString(prefix, server.getProps());
    namespace = server.getProps().getProperty(prefix + "namespace", "brazil");
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean respond(Request request) throws IOException {
    
    // XXX temporary to trigger searching.  This belongs elsewhere
    if (request.getUrl().startsWith("/search/")) {
      String search = request.getUrl().substring(8);
      request.log(Server.LOG_DIAGNOSTIC, "SEARCH TEST", search);
      Resource.search(search);
      return false;
    }
    
    if (!isMine.match(request.getUrl())) {
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "Skipping request, nonmatching url");
      return false;
    }
    
    // XXX Use file suffix for type instead of intrinsic type (for now)
    String type = FileHandler.getMimeType(request.getUrl(), request.getProps(), isMine.prefix());
    request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(),"type: " + type);
    if (type == null) {
      return false;
    }
    String root = request.getProps().getProperty(isMine.prefix() + "root",
            request.getProps().getProperty("root", ""));
    if (root.equals(".") || root.equals("/")) {
      root = "";
    }
    Resource resource = Resource.load(namespace + root + request.getUrl());
    if (resource == null) {
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(),"No template " + namespace + " + " +
              root + " + " + request.getUrl());
      return false;
    }
    byte[] buf = resource.getData();
    request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), " returning cached: (" + buf.length + ")");
    request.addHeader("Last-Modified", HttpUtil.formatTime(resource.getLastMod()));
    request.sendResponse(buf, type);
    return true;
  }
}

