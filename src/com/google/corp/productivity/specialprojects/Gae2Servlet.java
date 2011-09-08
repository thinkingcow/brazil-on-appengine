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

package com.google.corp.productivity.specialprojects;

import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.oauth.OAuthService;
import com.google.appengine.api.oauth.OAuthServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.api.utils.SystemProperty.Environment.Value;

import sunlabs.brazil.properties.PropertiesList;
import sunlabs.brazil.server.Request;
import sunlabs.brazil.server.Server;
import sunlabs.brazil.util.http.MimeHeaders;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class Gae2Servlet extends HttpServlet {

  Properties props;
  Server server;
  boolean init=false;   // need to finish up on first request
  UserService userService;
  OAuthService oauthService; 
  
  /**
   * Load in the server config from a WS separated list of resource names defined by
   * the "config" parameter.  The default resource is "configs".  Once the configs are
   * read in, two additional resources are loaded (if available), one containing the
   * appengine App ID, and the final one named: [appID]-[major version number].
   */

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    props = new Properties();
    initProps(props);
    props.put("servlet_name", config.getServletName());

    
    /* XXX just testing
    Provider[] providers = Security.getProviders();
    for(Provider provider : providers) {
      System.err.println(provider.toString());
    }
    Enumeration en = config.getInitParameterNames();
    while(en.hasMoreElements()) {
      String name = (String) en.nextElement();
      System.err.println(name + "=" + config.getInitParameter(name));
    }
    en = config.getServletContext().getAttributeNames();
    while(en.hasMoreElements()) {
      String name = (String) en.nextElement();
      System.err.println(name + "==" + config.getServletContext().getAttribute(name));
    }
    */

    // get initial properties from config file

    String configs = config.getInitParameter("config");
    System.err.println("Config is:" + configs);
    if (configs == null) {
      configs="config";
    }
    StringTokenizer st = new StringTokenizer(configs);
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (!propsFromFile(props, token)) {
        log("invalid config file " + token);
      }
    }
    
    // Load appid specific config file (if it exists)
    
    String appID;
    if (SystemProperty.environment.value() == Value.Development) {
      appID = "local";
    } else {
      appID = SystemProperty.applicationId.get();
    }
    
    if (!propsFromFile(props, appID)) {
      log("missing config file " + appID);
    }
    props.put("appID", appID);
    
    String versionID = SystemProperty.applicationVersion.get();
    props.put("appVersionString", versionID);
    int index = versionID.indexOf(".");
    if (index > 0) {
      versionID = versionID.substring(0, index);
      props.put("appVersion", versionID);
      propsFromFile(props, appID + "-" + versionID);
    }
    
    String handlerName= props.getProperty("handler","sunlabs.brazil.server.FileHandler");
    server = new Server(0, handlerName, props);
    server.setLogLevel(5);
    server.init();  // XXX need to capture status

    userService = UserServiceFactory.getUserService();
    if (props.get("oauth") != null) {
      oauthService = OAuthServiceFactory.getOAuthService();
      log("Using oauth authentication");
    } else {
      oauthService = null;
    }
  }

  @Override
  public void service(HttpServletRequest req, HttpServletResponse res)
  throws IOException {

    System.out.println("Calling service...");
    if (!init) {
      init=true;
      server.setPort(req.getServerPort());
    }

    Request request = new GseServletRequest(server, req, res);
    //request.getProps().put("context_path", req.getContextPath());
    // req.getRemoteHost()  XXX we should stuff this somewhere
    request.getRequest();
    if (server.handler == null) {
      request.sendError(500, "No handler configured to accept this request");
    } else if (server.handler.respond(request) == false) {
      request.sendError(404, null, request.getUrl());
    }
    try {
      request.out.close();
    } catch (IOException e) {}
    server.log(Server.LOG_INFORMATIONAL, null, "request done");
  }

  // just the basics
  void initProps(Properties properties) {
    properties.put("mime.html", "text/html");
    properties.put("mime.txt",  "text/plain");
    properties.put("mime.css",  "text/css");

    properties.put("mime.gif",  "image/gif");
    properties.put("mime.jpg",  "image/jpeg");
    properties.put("mime.png",  "image/png");
    properties.put("mime.ico",  "image/vnd.microsoft.icon");

    properties.put("mime.js",   "application/javascript");
    properties.put("mime.pdf",  "application/pdf");
  }

  // Fill in from a file

  public static boolean propsFromFile(Properties props, String name) {
    boolean ok=false;

    try {
      FileInputStream fin = new FileInputStream(name);
      props.load(fin);
      fin.close();
      ok=true;
    } catch (IOException e) {
      // just return false
    }
    return ok;
  }

  /**
   * We need to catch the headers, and feed them to the servlet.
   * @author suhler@google.com (Stephen Uhler)
   *
   */
  public class GseOutputStream extends Request.HttpOutputStream {

    public GseOutputStream(OutputStream out) {
      super(out);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sendHeaders(Request request) {
      GseServletRequest req = (GseServletRequest)request;
      Enumeration e = request.getResponseHeaders().keys();
      while (e.hasMoreElements()) {
        String name = (String)e.nextElement();
        if (name.toLowerCase().equals("server")) {
          req.res.addHeader("Via", request.getResponseHeaders().get(name));
        } else {
          req.res.addHeader(name, request.getResponseHeaders().get(name));
        }
      }

      /* Need to remap URI's in HTML content
      String type = request.responseHeaders.get("Content-Type");
      if (type != null && type.equals("text/html")) {
      baos = new ByteArrayOutputStream(request.server.bufsize);
      out = new HttpOutputStream(baos);
      contextPath = req.props.getProperty("context_path") + "/";
       */
    }
  }

  /*
   *  Request will be an interface someday.  For now, 
   *  the getRequest() method fills out the request.
   */
  public class GseServletRequest extends Request
  {
    HttpServletRequest req;
    HttpServletResponse res;
    MimeHeaders reqHeaders = new MimeHeaders();
    MimeHeaders resHeaders = new MimeHeaders();

    GseServletRequest(Server server, HttpServletRequest req, HttpServletResponse res)
    throws UnknownHostException, IOException
    {
      super(server,req.getInputStream(),
          new GseOutputStream(new BufferedOutputStream(res.getOutputStream())));
      this.req = req;
      this.res = res;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean getRequest() throws IOException {
      // super.getRequest(); ???

      String origUrl = req.getRequestURI().trim();
      int strip = req.getContextPath().length();
      setUrl(origUrl.substring(strip));
      setConnectionHeader("Connection");


      setMethod(req.getMethod().trim());
      String query = req.getQueryString();
      if (query != null)
        query = query.trim();
      else
        query = "";
      setQuery(query);
      setProtocolVersion(req.getProtocol().trim());
      setServerProtocol(req.getScheme());

      statusCode = 200;
      statusPhrase = "OK";
      setStartMillis(System.currentTimeMillis());
      out.bytesWritten=0;

      log(Server.LOG_LOG, "Request " + requestsLeft + " " + toString());

      if (getProtocolVersion().equals("HTTP/1.0")) {
        setVersion(10);
      } else if (getProtocolVersion().equals("HTTP/1.1")) {
        setVersion(11);
        // Should we turn off chunked transfer encoding?
      } else {
        sendError(505, toString(), null);
        return false;
      }

      Enumeration names = req.getHeaderNames();
      while (names.hasMoreElements()) {
        String name = (String)names.nextElement();
        reqHeaders.put(name, req.getHeader(name));
      }
      setHeaders(reqHeaders);
      setResponseHeaders(resHeaders);
      int len = req.getContentLength();
      if (len>0) {
        byte[] postData = new byte[len];
        in.readFully(postData);
        setPostData(postData);
      } else {
        setPostData(null);
      }
      
      // XXX need to replicate this to swap servers

      PropertiesList pl = new PropertiesList();
      setServerProps(new PropertiesList(server.getProps()));
      setProps(pl);
      getProps().addBefore(getServerProps());

      pl.put("url.servlet", origUrl);
      pl.put("url.orig", getUrl());

      User user = userService.getCurrentUser();
      fillUserProperties(pl, "",  user); 
      try {
        pl.put("loginURL", userService.createLoginURL(req.getRequestURI()));
        pl.put("logoutURL", userService.createLogoutURL(req.getRequestURI()));
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
        pl.put("loginUrlError", e.getMessage());
      }

      fillUserProperties(pl, "oauth.", getOAuthUser());
      return true;
    }
    
    private void fillUserProperties(Properties p, String prefix, User user) {
      if (user == null) {
        return;
      }
      nullPropsHelper(p, prefix, "email", user.getEmail());
      nullPropsHelper(p, prefix, "nickname", user.getNickname());
      nullPropsHelper(p, prefix, "authdomain", user.getAuthDomain());
      nullPropsHelper(p, prefix, "userid", user.getUserId());
    }
    
    private void nullPropsHelper(Properties p, String prefix, String key, String value) {
      if (value != null) {
        p.put(prefix + key, value);
      }
    }
    
    /**
     * Get the current user, either clientlogin or oAuth, depending on
     * the "oauth" server parameter in the config file
     */
    
    private User getOAuthUser() {
      if (oauthService == null) {
        return null;
      }
      try {
        return oauthService.getCurrentUser();
      } catch (OAuthRequestException e) { 
        e.printStackTrace();
        return null;
      }
    }

    /**
     *
     * Sets response code in the servlet response object.
     */  
    @Override
    public void setStatus(int code)
    {
      if (code>0) {
        res.setStatus(code);
        super.setStatus(code);
      }
    }

/*
    @Override
    public String serverUrl() {
      return req.getRequestURL().toString();
    }
*/

    @Override
    public void
    redirect(String url, String body)
    throws IOException
    {
      res.sendRedirect(url);
    }
  }
}
