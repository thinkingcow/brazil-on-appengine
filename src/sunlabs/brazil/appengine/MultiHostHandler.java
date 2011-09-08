/*Copyright 2011 Google Inc.
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
 * MultiHostHandler.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3
 * Copyright (c) 1999-2007 Sun Microsystems, Inc.
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is included as the file "license.terms",
 * and also available at http://www.sun.com/
 *
 * The Original Code is from:
 *    Brazil project web application toolkit release 2.3.
 * The Initial Developer of the Original Code is: cstevens.
 * Portions created by cstevens are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 */

package sunlabs.brazil.appengine;

import sunlabs.brazil.properties.PropertiesList;
import sunlabs.brazil.server.Handler;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.Glob;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * (Inspired by the sunlabs.brazil.handler.MultiHostHandler.)<br>
 * Allows the user to handle a set
 * of host names that are all running on the same IP address.  This
 * handler looks at the http "Host" header and redispatches the request to the
 * appropriate sub-server.
 * <p>
 * Only the main server is actually listening to the port on the specified IP
 * address.  The sub-servers are not running in separate threads.  Indeed,
 * they are not "running" at all.  They exist merely as a convenient bag to
 * hold each of the server-specific configuration parameters.
 * <p>
 * Properties:
 * <dl class=props>
 * <dt>namespace<dd>The root of the virtual filesystem (defaults to "brazil")
 * <dt>initialized<dd>This is set in Server.props() once the hosts configuration is read in
 * from resources.  Clearing this property will cause the hosts configuration to be re-initialized
 * on the next request.
 * <dt>init<dd>The name of the virtual file (resource) used to configure the sub-servers.  This
 * file in in java.Properties formant, and has the form:
 * <pre>
 *   subservers=a b c....
 *   a.pattern=[glob pattern to match an incoming hostname]
 *   a.handler=[Name of the handler token (or class name) to run for this sub-server]
 *   a.config=[WS delimited list of virtual files to use to configure this server]
 * </pre>
 * </dl>
 */
public class MultiHostHandler implements Handler {
  String prefix;
  String namespace;
  Server mainServer;

  private Hosts servers;

  @Override
  public boolean
  init(Server server, String prefix) {
    this.prefix = prefix;
    this.mainServer = server;
    this.namespace = server.getProps().getProperty(prefix + "namespace", "brazil");
    servers = null;
    return true;
  }

  /**
   * Read in the config file and initialize the server configurations.
   */
  private void init() {
    String initialized = mainServer.getProps().getProperty(prefix + "initialized");
    System.err.println(prefix + "initialized=(" + initialized + ")"); 
    if (servers == null || !Format.isTrue(initialized)) {
      String savedHostsName = mainServer.getProps().getProperty(prefix + "init", "/" +
            prefix + ".props");
      servers = new Hosts(mainServer, namespace, savedHostsName);
      mainServer.getProps().put(prefix + "initialized", "true");
    } else {
      System.err.println("Not Initializing servers");
    }
  }

  /**
   * Responds to an HTTP request by examining the "Host:" request header
   * and dispatching to the main handler of the server that handles
   * that virtual host.  If the "Host:" request header was not specified,
   * or named a virtual host that was not initialized in <code>init</code>
   * from the list of virtual hosts, this method returns without
   * handling the request. Port numbers are not used for host matching.
   *
   * @param	request
   *		The HTTP request to be forwarded to one of the sub-servers.
   *
   * @return	<code>true</code> if the sub-server handled the message,
   *		<code>false</code> if it did not.  <code>false</code> is
   *		also returned if the "Host:" was unspecified or unknown.
   */

  @Override
  public boolean
  respond(Request request) throws IOException {
    String host = request.getRequestHeader("Host");
    Server orig = request.server;
    if (host == null) {
      return false;
    }

    // insure only one dispatch on a host?
    if (Format.isTrue(request.getProps().getProperty(prefix + "dispatched"))) {
      request.log(Server.LOG_DIAGNOSTIC, prefix, "Ignoring recursive call...");
      return false;
    }

    int idx = host.indexOf(":");
    if (idx > 0) {
      host = host.substring(0, idx);
    }
    System.err.println("    Root is:" + request.getServerProps().getProperty("root"));
    init();
    System.err.println("    Root is:" + request.getServerProps().getProperty("root"));
    Server server = servers.lookupServer(host);
    request.log(Server.LOG_DIAGNOSTIC, prefix, host + "? SubHosts: " + servers);
    if (server != null) {
      System.err.println("New Root is:" + server.getProps().getProperty("root"));
      request.log(Server.LOG_DIAGNOSTIC, prefix, "replacing server object " + mainServer +
                " with " + server);
      request.getProps().setProperty(prefix + "dispatched", "true");
      server.requestCount++;
      request.server = server;     
      
      // Adjust the properties (this might not be quite right)
      
      PropertiesList pl = new PropertiesList(request.getProps().getWrapped());
      request.log(Server.LOG_DIAGNOSTIC, prefix, "Existing request properties:");
      request.setServerProps(new PropertiesList(server.getProps()));
      request.setProps(pl);
      request.getProps().addBefore(request.getServerProps());
      
      request.log(Server.LOG_DIAGNOSTIC, prefix, "Dispatching host=" + host + " to subserver");
      boolean result = server.handler.respond(request);
      request.server = orig;
      return result;
    } else {
      System.err.println("    Root is:" + request.getServerProps().getProperty("root"));
      
    }
    request.log(Server.LOG_DIAGNOSTIC, prefix,
            host + ": no matching sub-server");
    return false;
  }

  /**
   * Load a properties file from a resource.
   * This looks in the virtual file system, and if not there, looks in the "jar" file, 
   * then adds the properties it finds to the supplied Properties object.
   * XXX This doesn't belong here
   * @param namespace   Virtual filesystem namespace (not used for static resources)
   * @param name        Name of the resource 
   * @param base    Add properties to this, if it already exists
   * @return        The supplied properties object or null if no properties were found?
   */
  public static Properties getPropsFromResource(String namespace, String name, Properties base) {
      Resource resource = Resource.load(namespace + name);
      if (resource == null) {
        return getPropsFromJar(name, base);
      }
      Properties props = (base != null) ? base : new Properties();
      // XXX temporary debugging
      System.err.println(namespace + name + ": " +
              new String(resource.getData()).replace("\n", ", "));
      ByteArrayInputStream bais = new ByteArrayInputStream(resource.getData());
      try {
        props.load(bais);
        bais.close();
      } catch (IOException e) { }
      return props;
  }

  /**
   * Read a resource as a properties file
   * @param name        Resource name in the jar file
   * @param defaults    default properties object
   * @return            Properties object, or Null if none is available
   */

  private static Properties getPropsFromJar(String name, Properties base) {
    InputStream in = MultiHostHandler.class.getResourceAsStream(name);
    if (in != null) {
      Properties props = (base != null) ? base : new Properties();
      try {
        props.load(in);
        return props;
      } catch (IOException e) {}
    }
    return null;
  }
  
  /**
   * Structure to keep track of servers and their glob patterns.
   */

  static class Host {
    String name;        // token that identifies this server
    String pattern;		// the glob pattern
    String handler;     // The handler name (or token)
    String config;      // name of config file
    Server server;		// the contrived server object (or null)

    Host(String name, String pattern, String handler, String config) {
      this.name = name;
      this.pattern = pattern;
      this.handler = handler;
      this.config = config;
      this.server = null;
    }

    /**
     * return the server associated with this host, creating it as needed
     */
    Server
    getServer(Server mainServer, String namespace) {
      return server != null ? server : createServer(mainServer, namespace);
    }

    /**
     * Create and initialize a sub-server.
     */
    private Server createServer(Server mainServer, String namespace) {
      String mainroot = mainServer.getProps().getProperty("root","");
      Properties props = new Properties();
      props.putAll(mainServer.getProps());
      props.remove("root");
      
      if (mainroot.equals(".")) {
        mainroot = ""; // XXX temporary for old config files
      }
      
      mainServer.log(Server.LOG_DIAGNOSTIC, name, "creating sub-server + (" + mainroot + ")");
      if (config != null) {
        StringTokenizer st = new StringTokenizer(config);
        while (st.hasMoreTokens()) {
          String token = st.nextToken();
          if (getPropsFromResource(namespace, token, props) == null) {
            mainServer.log(Server.LOG_WARNING, name, "No config file: " + token);
          }
        }

        // fix up the root

        String root = (String)props.get("root");
        if (".".equals(root)) {
          root = ""; // XXX temporary for old config files
        }
        if (root != null && !root.startsWith("/") && root.length() > 0) {
          props.put("root", mainroot + "/" + root);
        } else if (root == null) {
          props.put("root", mainroot);
        } else {
          // keep root the same
        }
      }
      mainServer.log(Server.LOG_DIAGNOSTIC, name, "using root: " + props.getProperty("root"));
      
      String prefix = name + ".";
      Server sub = new Server(mainServer.getPort(), handler, props);
      sub.setHostName(props.getProperty(prefix + "host", pattern));
      sub.setPrefix(prefix);
      try {
        String str = props.getProperty(prefix + "log");
        sub.setLogLevel(Integer.decode(str).intValue());
      } catch (Exception e) {
        sub.setLogLevel(mainServer.getLogLevel());
      }
      sub.maxRequests = mainServer.maxRequests;
      sub.setTimeout(mainServer.getTimeout());
      this.server = sub;
      boolean ok = sub.init();
      if (ok) {
        this.server = sub;
        return sub;
      }
      return null;
    }


    @Override
    public String toString() {
      return name + ":" + pattern + " (" + handler + " " + config + ")";
    }
  }

  /**
   * Vector sub-servers to dispatch to
   * name: the token used to identify this server
   * pattern: the glob pattern a host matches to use this server
   * Server:  The sub-server object associated with this token
   *
   */
  static class Hosts extends Vector<Host> {
    Server mainServer;
    String namespace;

    public Hosts(Server mainServer, String namespace, String resourceName) {
      super();
      this.mainServer = mainServer;
      this.namespace = namespace;
      fromProps(resourceName);
    }


    public Server lookupServer(String hostName) {
      for(Host host : this) {
        if (Glob.match(host.pattern, hostName)) {
          return host.getServer(mainServer, namespace);
        }
      }
      return null;
    }

    private void fromProps(String resourceName) {
      Properties props = getPropsFromResource(namespace, resourceName, null);
      if (props == null) {
        mainServer.log(Server.LOG_DIAGNOSTIC, "init", "No resource: " + resourceName);
        return;
      }
      
      StringTokenizer st = new StringTokenizer(props.getProperty("subservers"));
      while (st.hasMoreTokens()) {
        String name = st.nextToken();
        String pattern = props.getProperty(name + ".pattern");
        String handler = props.getProperty(name + ".handler");
        String config = props.getProperty(name + ".config");
        add(new Host(name, pattern, handler, config));
      }
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      for(Host host : this) {
        sb.append(host.toString()).append(", ");
      }
      return sb.toString();
    }
  }
}
