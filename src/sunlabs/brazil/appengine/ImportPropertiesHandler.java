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

import sunlabs.brazil.handler.UrlMapperHandler;
import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.MatchString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Import a properties file from the "filesystem" based
 * on the value of some other property.  This is (currently)
 * intended to be used to change properties based on the incoming host name.
 * The "properties" file is read, cached and chained to each request.  The
 * property [prefix].[resource] is set to initialized.  Clearing the resource will cause the
 * next request to re-read the resource.  This handler is appropriate for a small number of
 * resources (e.g. <100's).
 * <dl class=props>
 * <dt>prefix, suffix, glob, match<dd>What triggers this handler
 * <dt>namespace<dd>Which virtual filesystem (defaults to "brazil")
 * <dt>resource<dd>The properties format file to chain to the request.
 *     It should be an absolute path (e.g. begin with /) and contain ${...}
 *     pattern(s) that get evaluated on each request.
 * <dt>initialized.xxx<dd>Set in the server namespace when a resource is cached.
 *     Clearing this will cause the next request to reload the resource
 *</dl>
 * 
 * @author suhler@google.com
 *
 */
public class ImportPropertiesHandler implements Handler {
  Server server;
  MatchString isMine;
  String namespace;
  Hashtable<String, Properties> cache = new Hashtable<String, Properties>();
  String resourceName; 

  @Override
  public boolean init(Server server, String prefix) {
    this.server = server;
    isMine = new MatchString(prefix, server.getProps());
    namespace = server.getProps().getProperty(prefix + "namespace", "brazil");
    resourceName = server.getProps().getProperty(prefix + "resource");
    return resourceName != null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean respond(Request request) throws IOException {
    
    if (!isMine.match(request.getUrl())) {
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "Skipping request, nonmatching url");
      return false;
    }
    
    // XXX this is hacky
    
    UrlMapperHandler.MapProperties map = 
      new UrlMapperHandler.MapProperties(request.getProps(), request.getHeaders());
    map.put("method", request.getMethod());
    map.put("url", request.getUrl());
    
    request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "can subst: " + map + " & " + request.getHeaders());
    String key = Format.subst(map, resourceName);
    request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), " Considering property file: " + key);
    
    String name = isMine.prefix() + "initialized." + key;
    if (server.getProps().getProperty(name) == null) {
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), " removing from cache");
      cache.remove(key);
    }
    
    Properties chainProps = cache.get(key);
    if (chainProps != null) {
      request.addSharedProps(chainProps);
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), " chaining from cache");
      return false;
    }
    
    Resource resource = Resource.load(namespace + key);
    if (resource == null) {
      request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(),"No file " + key);
      server.getProps().put(name, "missing");
      return false;
    }
    
    ByteArrayInputStream bais = new ByteArrayInputStream(resource.getData());
    Properties p = new Properties();
    p.load(bais);
    cache.put(key, p);
    server.getProps().put(name, "initialized");
    request.addSharedProps(chainProps);
    request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), " Reload and chain:" + p);
    return false;
  }
}
