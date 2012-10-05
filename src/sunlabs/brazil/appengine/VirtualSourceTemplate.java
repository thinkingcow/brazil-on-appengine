/*Copyright 2012 Google Inc.
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

import sunlabs.brazil.sunlabs.SourceTemplate;
import sunlabs.brazil.template.RewriteContext;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Use a virtual resource instead of a File as a source for the template.
 * the current document. Adapted from sunlabs.brazil.sunlabs.SourceTemplate
 */

public class VirtualSourceTemplate extends SourceTemplate {

  @Override
  public void
  tag_source(RewriteContext hr) {
    super.tag_source(hr);
  }

  /**
   * Fetch a resource from its name.
   * (This is a dumb interface, but it's what we have, for now)
   */

  @Override
  protected ByteArrayOutputStream
  getContent(RewriteContext hr, String src) throws IOException {
    Resource resource = Resource.load("brazil" + src);
    if (resource == null) {
      hr.request.getProps().put(hr.prefix + "error", "No such resource");
      throw new FileNotFoundException(src);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(resource.getData());
    return baos;
  }
}
