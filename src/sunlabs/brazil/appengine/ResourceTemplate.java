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

import sunlabs.brazil.appengine.Resource.ResourceInfo;
import sunlabs.brazil.server.FileHandler;
import sunlabs.brazil.session.SessionManager;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.Glob;
import sunlabs.brazil.util.regexp.Regexp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;


/**
 * Template to deal with appengine resources.
 * @author suhler@google.com (Stephen Uhler)
 *
 */
public class ResourceTemplate extends Template {
  
  /**
   * Upload a file from query data. 
   * Be careful how this tag is used:
   * until there is some config option to "protect" the filesystem, it needs to
   * be done with surrounding markup.
   * <upload name="pathname" content="contents to upload" [namespace="brazil" shouldAppend=false] />
   * - name     The name of the file to upload
   * - content  The content to upload it as
   * - restrict=/must matchfile path/    The config setting permit a file to be uploaded.
   *   ${..} performed.
   * - allow=/must match url/   If true skip the restrict test
   * 
   * XXX The access controls should be generalized, and implemented uniformly!
   */
  
  public void tag_upload(RewriteContext hr) {
    String root = hr.get("namespace","brazil");
    String name= hr.get("name");
    String content = hr.get("content");
    
    // these shouldn't be recomputed each time! XXX
    String restrictPath = hr.request.getProps().getProperty(hr.prefix + "restrict");
    String allowUrl =  hr.request.getProps().getProperty(hr.prefix + "allow");
    boolean shouldAppend = hr.isTrue("append");
    
    debug(hr);
    hr.killToken();
    
    if (allowUrl != null) {
      Regexp re = new Regexp(allowUrl);
      String url = hr.request.getProps().getProperty("url.orig");
      if (re.match(url) != null) {
        System.err.print(
                "magic url: " + url + " matches " + allowUrl + ": skipping security check");
        restrictPath = null;
      } else {
        System.err.println("no magic url: " + url + " != " + allowUrl);
      }
    }
    
    if (restrictPath != null) {
      restrictPath = Format.subst(hr.request.getProps(), restrictPath);
      Regexp re = new Regexp(restrictPath);
      if (re.match(name) == null) {
        System.err.print("invalid url: [" + restrictPath + "] for [" + name + "]");
        debug(hr, "url denied");
        return;
      }
    }
    
    if (name != null && content != null && content.length() > 0) {
      Resource resource;
      byte[] data = content.getBytes();
      if (shouldAppend && (resource = Resource.get(root + name)) != null) {
        byte combined[] = new byte[resource.getData().length + data.length];
        System.arraycopy(resource.getData(), 0, combined, 0, resource.getData().length);
        System.arraycopy(data, 0, combined, resource.getData().length, data.length);
        resource = new Resource(root + name, combined);
      } else {
        resource = new Resource(root + name, data);
      }
      System.err.println("saving to " + resource.getPath() + ": (" + new String(resource.getData()) + ")");
      Resource.save(resource);
    } else {
      debug(hr, "Missing name and/or content");
    }
  }
  
  /**
   * Download a resource into a property.  This only works for file whose mime types are text/...
   * - namespace        which namespace to use
   * - name             the name of the resource
   * - property         Where to put the contents (defaults to "download")
   * - max              The max size to accept (defaults to 10000? (not implemented)
   */
  
  public void tag_download(RewriteContext hr) {
    String root = hr.get("namespace","brazil");
    String name= hr.get("name");
    String property = hr.get("property", "download");
    
    debug(hr);
    hr.killToken();
    
    if (name == null) {
      debug(hr, "Missing name");
      return;
    }
    
    String type = FileHandler.getMimeType(name, hr.request.getProps(), hr.prefix);
    if (type == null || !type.toLowerCase().startsWith("text")) {
      hr.request.getProps().put(hr.prefix + "error", "Invalid file type: " + type);
      debug(hr, "Invalid file type");
      return;
    }
    
    Resource resource = Resource.load(root + name);
    if (resource == null) {
      hr.request.getProps().put(hr.prefix + "error", "No such file");
      debug(hr, "No resource: (" + root + name + ")");
      return;
    }
    byte[] data = resource.getData();
    debug(hr, "setting " + property + " to " + data.length + "bytes");
    hr.request.getProps().put(property, new String(data));
  }
  
  /**
   * Delete a resource.
   * @param hr
   */
  public void tag_delete(RewriteContext hr) {
    String root = hr.get("namespace","brazil");
    String name= hr.get("name");
    debug(hr);
    hr.killToken();

    if (name != null) {
      Resource.delete(root + name);
    }
  }
  
  /**
   * Toss all markup generated to this point.
   * (This doesn't belong here.  It needs to move to the MiscTemplate.)
   */
  public void tag_tossmarkup(RewriteContext hr) {
    hr.killToken();
    hr.sb = new StringBuffer();
  }
  
  /**
   * Copy/Move a resource.
   * XXX This should be implemented in Resource to guarantee atomicity.
   * @param hr
   */
  public void tag_move(RewriteContext hr) {
    String root = hr.get("namespace","brazil");
    String from = hr.get("from");
    String to = hr.get("to");
    boolean keep = hr.isTrue("keep"); // copy and not move
    String result = hr.get("result"); // set this var to "true" if succeeded
    boolean force = hr.isTrue("force"); // overrite destination if it already exists
    
    debug(hr);
    hr.killToken();

    if (from != null && to != null) {
      Resource fromResource = Resource.load(root + from);
      Resource toResource = Resource.load(root + to);
      
      if (fromResource != null && (toResource == null || force)) {
        fromResource.setPath(root + to);
        Resource.save(fromResource);
        if (result != null) {
          hr.request.getProps().put(result, "true");
        }
        if (!keep) {
          Resource.delete(root + from);
        }
      }
    }
  }
  
  /** 
   * See if a resource exists
   * name:  The name of the resource
   * result: Where to put the result (true or false) defaults to "name" which is overwritten
   */
  
  public void tag_exists(RewriteContext hr) {
    String root = hr.get("namespace","brazil");
    String name = hr.get("name");
    String result = hr.get("result", name);
    
    if (name != null) {
      Resource resource = Resource.load(root + name);
      hr.request.getProps().put(result, resource == null ? "false" : "true");
    }
  }
  
  /**
   * Search for "files" in the store based on prefixes.
   * This is a poor-man's "ls".  The type and nature of the returned
   * valued is preliminary.
   * <search namespace="brazil" search="/androgen/..." delim=" " prefix="search." restrict="glob" />
   * - namespace:   where in JDO this is stored
   * - search:      The resource prefix (must start with a /)
   * - delim:       Separator for list of returned resources
   * - glob         Glob pattern to restrict results
   * - regexp       Regexp to restrict results
   * Results are returned in:
   *   prefix.count=n
   *   prefix.names=name1[delim]name2[delim]...namen
   *   prefix.[name]=lastmod
   * @param hr
   */
  public void tag_search(RewriteContext hr) {
    String root = hr.get("namespace","brazil");
    String search= hr.get("search"); // search prefix
    String delim = hr.get("delim", " ");
    String prefix = hr.get("prefix", "search."); // results prefix
    String glob = hr.get("glob"); // restrict results to match this
    String match = hr.get("match");  // restrict to regexp
    
    debug(hr);
    hr.killToken();

    if (search == null || !search.startsWith("/")) {
      debug(hr, "Invalid or missing search prefix");
      return;
    }
    
    Regexp re = null;
    if (match != null) {
      try {
        re = new Regexp(match);
      } catch (IllegalArgumentException e) {
        // ignore
      }
    }
    
    int stripNamespace = root.length();
    int count = 0;
    Collection<ResourceInfo> items = Resource.search(root + search);
    StringBuffer names = new StringBuffer();

    for (ResourceInfo i : items) {
      String name = i.name.substring(stripNamespace);
      if ((glob == null || Glob.match(glob, name)) &&
          (re == null || re.match(name) != null)) {
        hr.request.getProps().put(prefix + name, "" + i.lastModTime);
        names.append(name).append(delim);
        count++;
      }
    }
    hr.request.getProps().put(prefix + "names", names.toString());
    hr.request.getProps().put(prefix + "count", "" + count);
  }
  
  /**
   * Load and Store a namespace to the persistent store.
   * This is temporary - use <namespace persist=true> instead
   * 
   * &lt;persist command=load sessionTable="xxxx" namespace="nnnn" [clear=true|false]&gt;
   * @param hr  Std rewrite context
   */
  public void tag_persist(RewriteContext hr) {
    String command = hr.get("command");
    if (command==null)   {
      debug(hr, "Need command=load|store");
      return;
    }

    String sessionTable = hr.get("sessionTable", hr.prefix);
    String root = hr.get("namespace", hr.sessionId);
    Properties p = (Properties) SessionManager.getSession(root,
        sessionTable, null);
    
    String key = "persist/" + sessionTable + ":" + root;
    
    Resource resource;
    if (command.equals("store") && p!= null) {
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      try {
        p.store(bo, key);
      } catch (IOException e) {
        e.printStackTrace();
      }
      debug(hr, "Saving " + key);
      System.err.println("Persisting: " + p);
      resource = new Resource(key, bo.toByteArray());
      Resource.save(resource);
    } else if (command.equals("load") && (resource = Resource.load(key)) != null) {
      ByteArrayInputStream bi = new ByteArrayInputStream(resource.getData());
      if (hr.isTrue("clear") &&  p!=null) {
        p.clear();
      } else if (p==null) {
        p = new Properties();
      }
      try {
        p.load(bi);
      } catch (IOException e) {
        e.printStackTrace();
      }
      SessionManager.put(root, sessionTable, p);
      System.err.println("Restoring " + p);
     debug(hr, "loaded " + key);
    } else {
      debug(hr, "Invalid command");
    }
  }
}