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

/*
 * Upload a resource to an in memory cache.
 */

package sunlabs.brazil.appengine;

import sunlabs.brazil.server.FileHandler;
import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.MatchString;
import sunlabs.brazil.util.http.HttpUtil;

import java.io.IOException;

/**
 * Upload a file/template to an in-memory cache.
 * (See the CachedTemplateHandler for details)
 * @author suhler@google.com
 *
 * <dl class=props>
 * <dt>prefix, suffix, glob, match...
 * <dd>See MatchString().  "Prefix must be specified - it is removed from
 * the URL to be retrieved  (e.g a file uploaded as "[prefix]/foo" will be stored as "/foo").
 * <dt>namespace
 * <dd>Which JDO namespace to persist resources to.
 * <dt>put
 * <dd>If True, then reqire PUT method, otherwise require POST
 * <dt>allowNamespace
 * <dd>If True, then the namespace may be overridden by the specified query parameter
 * "namespace"
 * </dl>
 * <p>
 * If the http method is "DELETE", the resource is deleted (unless the X-cacheonly http
 * header is present, in which case only the local cached version is removed).
 * <p>
 * Otherwise, if there is a content body, it is uploaded.
 */
public class UploadResourceHandler implements Handler  {
  MatchString isMine;
  String namespace;
  boolean allowNamespace;
  boolean requirePut;
  boolean allowGet;

  @Override
  public boolean init(Server server, String prefix) {
    namespace = server.getProps().getProperty(prefix + "namespace", "brazil");
    isMine = new MatchString(prefix, server.getProps());
    requirePut = Format.isTrue(server.getProps().getProperty(prefix + "put"));
    allowGet = Format.isTrue(server.getProps().getProperty(prefix + "get")); 
    allowNamespace = Format.isTrue(server.getProps().getProperty(prefix + "allowNamespace"));
    return true;
  }

  /**
   * Upload, Append, download or Delete a "resource".
   * - the GET method downloads the resource
   * - POST or PUT uploads the resource, depending on the configuration
   * - the DELETE method deletes the resource
   *   (setting X-cacheonly only removes the resource from the cache)
   * - if the http header "X-append" is true the new content is appended to the resource
   * @throws IOException  
   */

  @Override
  public boolean respond(Request request) throws IOException {
    if (!isMine.match(request.getUrl())) {
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "Skipping request, nonmatching url");
      return false;
    }
    String prefix = request.getProps().getProperty(isMine.prefix() + "prefix");
    String name = request.getUrl().substring(prefix.length()); // property name
    
    // Can we overload the namespace here?
    String overRide;
    if (allowNamespace && (overRide = request.getQueryData(null).get("namespace")) != null) {
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "Changing namespace: " + namespace +
              " to " + overRide);
      namespace = overRide;
    }
    
    // Fetch a path - stolen from the DownloadResourceHandler
    
    if (allowGet && request.getMethod().equals("GET")) {
      Resource resource = Resource.load(namespace + name);
      if (resource == null) {
        request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(),"No template " + name);
        return false;
      }
      String type = FileHandler.getMimeType(name, request.getProps(), isMine.prefix());
      if (type == null) {
        type = "application/octet-stream";
      }
      byte[] buf = resource.getData();
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), " returning cached: (" + buf.length + ")");
      request.addHeader("Last-Modified", HttpUtil.formatTime(resource.getLastMod()));
      request.sendResponse(buf, type);
      return true;
    }

    // Delete a path
    
    if (request.getMethod().equals("DELETE")) {
      boolean result = true;
      if (request.getHeaders().get("X-cacheonly")!= null) {
        Resource.clear(namespace + name);
        request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "Clearing cache for: " + name);
      } else {
        request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "Deleting: " + name);
        result = Resource.delete(namespace + name);
      }
      if (result) {
        request.sendResponse("Delete suceeded", "text/plain", 200);
      } else {
        request.sendResponse("Delete failed", "text/plain", 404);
      }
      return result;
    }
    
    // Upload a resource.  We require POST (PUT would be better, put harder to use with
    // the existing Android client HTTP stack)
 
    if (!requiredMethod(request) || request.getPostData() == null) {
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(),
          "Skipping request, no or post data or incorrect method");
      return false;
    }

    // Experimental "append" support
    
    byte[] content;
    Resource existingResource;
    
    if (Format.isTrue(request.getHeaders().get("X-append")) && 
        (existingResource = Resource.load(namespace + name)) != null) {
      byte[] existing = existingResource.getData();
      byte[] uploaded = request.getPostData();
      content = new byte[existing.length + uploaded.length];
      System.arraycopy(existing, 0, content, 0, existing.length);
      System.arraycopy(uploaded, 0, content, existing.length, uploaded.length);
    } else {
      content = request.getPostData();
    }
    
    Resource resource = new Resource(namespace + name, content);
    Resource.save(resource);
    // request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "uploaded: " + name + " as: [" + new String(content, "ISO-8859-1") + "]");
    request.addHeader("location", request.serverUrl());
    request.sendResponse("upload of (" + name + ") succeeded", "text/plain", 201);
    return true;
  }
  
  private boolean requiredMethod(Request request) {
    return request.getMethod().equals(requirePut ? "PUT" : "POST");
  }
}
