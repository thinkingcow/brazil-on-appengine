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
import sunlabs.brazil.template.TemplateRunner;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.MatchString;
import sunlabs.brazil.util.http.HttpUtil;

import java.io.IOException;
import java.util.Properties;

/**
 * This is a replacement for the TemplateHandler that uses JDO resources instead of
 * files.  The additional properties supported are:
 * <dl class=props>
 * <dt>namespace
 * <dd>The namespace to use for the virtual filesystem (defaults to "brazil")
 * <dt>headerTemplate
 * <dd>The resource name, if any whose contents will always be prepended to each template
 * <dt>footerTemplate
 * <dd>The resource name, if any whose contents will always be appended to each template
 * </dl>
 * Templates should be in the default server encoding
 * @author suhler@google.com
 *
 */

public class CachedTemplateHandler implements Handler {
  private String namespace;
  private String headerTemplate;  // prepend all markup with this everytime
  private String footerTemplate;  // add this markup to the end of everything
  private String sessionProperty; // where to find the session name
  private MatchString isMine;     // check for matching url
  private boolean modified;       // if set, emit last-modified header
  private TemplateRunner runner;  // The template object for our class

  @Override
  public boolean
  init(Server server, String prefix) {
    Properties props = server.getProps();
    isMine = new MatchString(prefix, props);
    namespace = props.getProperty(prefix + "namespace", "brazil");
    headerTemplate = props.getProperty(prefix + "headerTemplate");
    footerTemplate = props.getProperty(prefix + "footerTemplate");
    sessionProperty = props.getProperty(prefix + "session", "SessionID");
    modified = Format.isTrue(props.getProperty(prefix + "modified"));

    String str = props.getProperty(prefix + "templates");
    if (str == null) {
      server.log(Server.LOG_ERROR, prefix, "no \"templates\" property specified");
      return false;
    }

    try {
      runner = new TemplateRunner(server, prefix, str);
    } catch (ClassCastException e) {
      server.log(Server.LOG_ERROR, e.getMessage(), "not a Template");
      return false;
    } catch (ClassNotFoundException e) {
      server.log(Server.LOG_ERROR, e.getMessage(), "unknown class");
      return false;
    }
    return true;
  }

  @Override
  public boolean
  respond(Request request) throws IOException {
    String url = request.getUrl();
    Properties props = request.getProps();
    String prefix = isMine.prefix();

    if (url.endsWith("/")) {
      url += props.getProperty(prefix + "default" + "", "index.html");
    }
    if (!isMine.match(url)) {
      return false;
    }

    int index = url.lastIndexOf('.');
    if (index < 0) {
      request.log(Server.LOG_INFORMATIONAL, prefix, "no suffix suffix for: " + url);
      return false;
    }

    String type = FileHandler.getMimeType(url, props, prefix);
    if (type == null) {
      request.log(Server.LOG_INFORMATIONAL, prefix, "unknown type for: " + url);
      return false;
    }

    // make sure this is a text file!

    if (!type.toLowerCase().startsWith("text/")) {
      request.log(Server.LOG_INFORMATIONAL, prefix, "Not a text type: " + type);
      return false;
    }

    request.log(Server.LOG_DIAGNOSTIC, prefix, "finding root for (" + prefix +
        ") [" + props.getProperty(prefix + FileHandler.ROOT, "n/a") + "][" +
        props.getProperty(FileHandler.ROOT, "n/a") + "]");
    // request.getServerProps().save(System.err, "ServerProps");
    // request.getProps().save(System.err, "RequestProps");
    String root = props.getProperty(prefix + FileHandler.ROOT,
        props.getProperty(FileHandler.ROOT, "")).trim();
    if (root.equals(".") || root.equals("/")) { // XXX we have no concept of current directory
      root = "";
    }
    request.log(Server.LOG_DIAGNOSTIC, prefix, root + "+" + url);
    url = root + url;

    Resource resource = Resource.load(namespace + url);
    if (resource == null) {
      request.log(Server.LOG_DIAGNOSTIC, prefix, "Not found: " + url);
      return false;
    }
    String template = fetchTemplateData(headerTemplate) + new String(resource.getData()) +
        fetchTemplateData(footerTemplate);
    String session = props.getProperty(sessionProperty, "common");
    String result = runner.process(request, template, session);
    request.log(Server.LOG_DIAGNOSTIC + 1, prefix, "{" + template + "}\n{" +  result + "}");
    if (result != null && !Format.isTrue(props.getProperty(prefix + "alwaysCancel"))) {
      FileHandler.setModified(request.getProps(), resource.getLastMod());
      if (modified) {
        request.addHeader("Last-Modified", HttpUtil.formatTime(resource.getLastMod()));
      }
      request.sendResponse(result, type);
      return true;
    } else {
      request.log(Server.LOG_INFORMATIONAL, prefix, runner.getError());
      return false;
    }
  }

  /**
   * Return the contents of a "template" as a string
   * @return Template contents, or ""
   */
  private String fetchTemplateData(String templateName) {
    if (templateName != null) {
      Resource template = Resource.load(namespace + templateName);
      if (template != null) {
        return new String(template.getData());
      }
    }
    return "";
  }
}
