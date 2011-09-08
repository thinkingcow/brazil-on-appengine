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
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.MatchString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Skeleton Handler for uploading files using multipart/form-data.
 * as resources.
 * Properties:
 * <dl class=props>
 * <dt>prefix, suffix, glob, match
 * <dd>Specify the URL that triggers this handler.
 * <dt>namespace
 * <dd>The virtual "filesytem" this goes into (defaults to "Brazil")
 * <dt>pattern
 * <dd>The pattern that will determine the name of the file being stored.
 * The pattern will undergo ${..} substitutions, using anything in Request.props, augmented
 * by any name/value pairs found in the form.  In particular, ${filename} will be the name of the
 * file specify the user.
 * <dt>query
 * <dd>If set, all non-file form names and values will be set in the request properties, prefixed
 * by this string (e.g query=query.)
 * <dt>prefix.count
 * <dt>prefix.[count].path
 * <dt>prefix.[count].file
 * <dt>prefix.uploads
 * <dd>
 * These properties are set for uploaded files to: The number of files uploaded,
 * the path name used for file "count" (0 indexed), the "filename" specified in the form, and the
 * count of uploads (e.g. 0 1 2 ...).
 * <p>
 * NOTES:<br>
 * The request is changed to GET, if any file has been uploaded.
 * (See {@link MatchString}).
 * </dl>
 *
 * @author              Stephen Uhler
 */

public class MultipartUploadResourceHandler implements Handler {
  MatchString isMine;            // check for matching url
  String pattern;                // pattern for computing filename
  String namespace;              // our resources namespace
  String queryPrefix;            // if set, use as prefix to all "query" properties

  @Override
  public boolean
  init(Server server, String prefix) {
    isMine = new MatchString(prefix, server.getProps());
    namespace = server.getProps().getProperty(prefix + "namespace","brazil");
    pattern = server.getProps().getProperty(prefix + "pattern", "${filename}");
    queryPrefix = server.getProps().getProperty(prefix + "query");
    return true;
  }

  /**
   * Make sure this is one of our requests.
   * If OK, save file to proper spot.
   * @throws IOException 
   */

  @Override
  public boolean
  respond(Request request) throws IOException {
    String type = request.getHeaders().get("content-type");
    if (!isMine.match(request.getUrl()) || type==null ||
        !type.startsWith("multipart/form-data") || !request.getMethod().equals("POST")) {
      System.out.println("Push skipping...");
      return false;
    }

    //screen out bad requests

    if (request.getPostData() == null) {
      request.sendError(400, "No content to upload");
      return true;
    }

    if (request.getHeaders().get("Content-Range") != null) {
      request.sendError(501, "Can't handle partial puts");
      return true;
    }
    if (processData(request) > 0) {
      request.setMethod("GET");
    }
    return false;  // allow template to do something with this
  }

  /**
   * process the data - this doesn't currently do anything useful.
   * Need to:
   *  Resource resource = new Resource(namespace + name, content);
   *  Resource.save(resource);
   *
   */

  public int processData(Request request) {
    MapProperties mp = new MapProperties(request.getProps());
    Split s = new Split(request.getPostData());
    int uploadCount = 0;
    StringBuffer sb = new StringBuffer();
    while (s.nextPart()) {
      String name = s.name();
      String filename = s.fileName();
      
      if (filename==null) {
        String value = s.content();
        mp.addItem(name, value);  
        if (queryPrefix != null) {
          request.getProps().put(queryPrefix + name, value);
        }
      } else if (s.length() > 0){ 
        mp.addItem("filename", filename);
        String path = Format.subst(mp, pattern);
        request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), pattern + " => " + path);
        Resource resource = new Resource(namespace + path, s.contentBytes());
        Resource.save(resource);
        sb.append(uploadCount).append(" ");
        request.getProps().put(isMine.prefix() + uploadCount + ".path", path);
        request.getProps().put(isMine.prefix() + uploadCount + ".file", filename);
        
        uploadCount++;
      } else {
        request.log(Server.LOG_DIAGNOSTIC, isMine.prefix(), "empty file, skipping: " + filename);
      }
      request.getProps().put(isMine.prefix() + "uploads", "" + sb.toString());
    }
    request.getProps().put(isMine.prefix() + "count", "" + uploadCount);
    return uploadCount;
  }
  
  /**
   * Add stuff in front of an existing Properties
   * This is for Format.subst, and is not a complete implementation.
   */

  static class MapProperties extends Properties {
      Properties realProps;
      Map<String, String> map;

      public MapProperties(Properties props) {
          realProps = props;
          this.map = new HashMap<String, String>();
      }

      public void addItem(String name, String value) {
          map.put(name, value);
      }

      @Override
      public String getProperty(String key, String dflt) {
          String result = map.get(key);

          if (result == null) {
              result = realProps.getProperty(key, dflt);
          }
          return result;
      }

      @Override
      public String getProperty(String key) {
          return getProperty(key, null);
      }
  }

  

  /**
   * Split multipart data into its constituent pieces.  Use byte[] so we can
   * handle (potentially) large amounts of binary data.
   * This acts as an iterator, stepping through the parts, extracting the appropriate
   * info for each part.
   */

  public static class Split {
    byte[] bytes;
    int bndryEnd;   // end of the initial boundary line
    int partStart;  // start index of this part
    int partEnd;    // index to the end of this part
    int contentStart;       // start of the content

    /**
     * create a new multipart form thingy
     */

    public Split(byte[] bytes) {
      partEnd = 0;
      this.bytes = bytes;
      bndryEnd = indexOf(bytes, 0, bytes.length, "\r\n");
      partStart=0;
      contentStart=0;
    }

    /**
     * Return true if there is a next part
     */

    public boolean
    nextPart() {
      partStart = partEnd + bndryEnd+2;
      if (partStart >= bytes.length) {
        return false;
      }
      partEnd = indexOf(bytes, partStart, bytes.length, bytes, 0, bndryEnd);
      if (partEnd < 0) {
        return false;
      }
      partEnd -=2; // back over \r\n
      contentStart = indexOf(bytes, partStart, bytes.length, "\r\n\r\n") + 4;
      return true;
    }

    /**
     * Get the content as a string
     */

    public String
    content() {
      return new String(bytes, contentStart, partEnd-contentStart);
    }
    
    public byte[] contentBytes() {
      byte[] result = new byte[length()];
      System.arraycopy(bytes, contentStart, result, 0, length());
      return result;
    }

    /**
     * Return the content length
     */

    public int
    length() {
      return partEnd - contentStart;
    }

    /**
     * return the index into the start of the data for this part
     */

    public int
    start() {
      return contentStart;
    }

    /**
     * Return the header as a string
     */

    public String header() {
      return (new String(bytes, partStart, contentStart-partStart));
    }

    /**
     * get the part name
     */

    public String name() {
      return extractKey(" name=\"");
    }

    /**
     * get the part filename
     */

    public String fileName() {
      return extractKey(" filename=\"");
    }
    
    /**
     * Extract a key out of the multi-part "mini header".  This assumes
     * a very specific format (that seems to work for Chrome/FF/Safari).
     */

    private String extractKey(String key) {
      int start = indexOf(bytes, partStart, contentStart, key) + key.length();
      int end = indexOf(bytes, start, contentStart, "\"");
      if (start>=key.length() && end >=0) {
        return (new String(bytes, start, end-start));
      } else {
        return null;
      }
    }
  }

  /**
   * Find the index of dst in src or -1 if not found.
   * This is the byte array equivalent to string.indexOf()
   */

  public static int
  indexOf(byte[] src, int srcStart, int srcEnd, byte[] dst, int dstStart, int dstEnd) {
    int len = dstEnd - dstStart;    // len of to look for
    srcEnd -= len;
    for (;srcStart < srcEnd; srcStart++) {
      boolean ok=true;
      for(int i=0; ok && i<len;i++) {
        if (dst[i+dstStart] != src[srcStart + i]) {
          ok=false;
        }
      }
      if (ok) {
        return srcStart;
      }
    }
    return -1;
  }

  public static int
  indexOf(byte[] src, int srcStart, int srcEnd, String dst) {
    return (indexOf(src, srcStart, srcEnd, dst.getBytes(), 0, dst.length()));
  }
}