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

import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.session.SessionManager;
import sunlabs.brazil.util.Glob;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Session Manager that keeps "properties" objects as JDO persistent objects.
 * Non properties objects use the default implementation.
 * Should use memcached too, but one thing at a time.
 * @author suhler@google.com
 */

// XXX this needs a rethink - 'tis broken

public class ResourceSessionManager extends SessionManager implements Handler, Runnable {
  String match; // ala PropertiesCacheManager, for now
  
  @Override
  public boolean init(Server server, String prefix) {
   SessionManager.setSessionManager(this);
   match = server.getProps().getProperty(prefix + "match", "*\\?*save=true");
   /* no shutdown hooks for app engine
   if (Format.isTrue(server.getProps().getProperty(prefix + "saveOnExit"))) {
     Runtime.getRuntime().addShutdownHook(new Shutdown(this));
   }
   */
   return true;
  }

  /**
   * Use the same strategy as the PropertiesCacheManager for persisting state.
   * XXX needs to be improved to specify a specific object to save.
   * @throws IOException  
   */
  
  @Override
  public boolean respond(Request request) throws IOException {
    String check;
    if (request.getQuery().equals("")) {
        check = request.getUrl();
    } else {
        check = request.getUrl() + "?" + request.getQuery();
    }
    if (Glob.match(match, check)) {
        save();
    }

    return false;
  }
  
  // save out every properties thing
  
  @SuppressWarnings("unchecked")
  public void save() {
    Enumeration e = sessions.keys();
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      Object value = sessions.get(key);
      if (value instanceof Properties) {
        putObj((String) key, (Properties) value);
      }
    }
  }

  /**
   * Get the session object out of the in memory hashtable.
   * If it doesn't exist, try loading it from the underlying store,
   * then put it in the hashtable.  This doesn't guarantee cache
   * coherency across multiple instances.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected Object
  getObj(Object session, Object ident) {
    Object result = super.getObj(session, ident);
    // if (result != null && !(result instanceof Properties)) {
    if (result != null) {
      System.err.println("Non-properties session returned");
      return result;
    }
    String key = makeKey(session, ident);
    Resource resource = Resource.load(key);
    System.err.println("Getting: [" + key + "] " + resource);
    if (resource != null) {
      Properties p = new Properties();
      ByteArrayInputStream bi = new ByteArrayInputStream(resource.getData());
      try {
        p.load(bi);
        System.err.println("Loading from persistent store: " + session + "/" + ident + " " + p);
        // XXX broken ???
        // super.putObj(session, ident, p);
        sessions.put(key, p);
        bi.close();
        return p;
      } catch (IOException e) {
        e.printStackTrace();
      }
    } 
   return null;
  }
  
  // XXX need to do Saveable too!
  
  /**
   * Copy the in-memory session object (if its a properties) into 
   * the underlying persistent store.
   */
  @Override
  protected void
  putObj(Object session, Object ident, Object value) {
    if (value instanceof Properties) {
      putObj(makeKey(session, ident), (Properties) value);
    }
    super.putObj(session, ident, value);
  }
  
  // XXX this does a transaction per instance, and should be improved
  private void putObj(String key, Properties p) {
    System.err.println("Persisting: [" + key + "] (" + p + ")");
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    try {
      p.store(bo, key);
      Resource.save(new Resource(key,bo.toByteArray()));
      Resource.removeFromCache(key);
      bo.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Shutdown hook - save all sessions
   */
  @Override
  public void run() {
    save();
  } 
}