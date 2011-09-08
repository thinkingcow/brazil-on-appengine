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

package com.google.corp.productivity.specialprojects.brazil.gdata;

import com.google.gdata.client.GoogleService;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaByteArraySource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.ExifTags;
import com.google.gdata.data.photos.GphotoAccess;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.data.photos.impl.ExifTag;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import sunlabs.brazil.appengine.Resource;
import sunlabs.brazil.server.FileHandler;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Experiment with gdata apis.  To start with, this is to integrate
 * androgen photos with Picasa.
 * - Create an album
 * - List all albums
 * - List all photos in an album
 * - Download a photo (picasa to appengine)
 * - Upload a photo (app engine to picasa)
 * Tags:
 *   <getauthtoken user=xxx pass=xxx [service=xxx name=xxx]>
 *   <album command=create name=xxx [description=xxx protected=true|false>
 *   <album command=list [prepend=xxx]>
 *   <photo command=upload name= [album= title= description= >
 *   <photo command=list [album="albumid"]>
 *   <photo command=download ...>
 *   <photo command=edit ...>
 * Notes:
 *    All tags take either "token", or "user, pass" attributes
 *    All tags take "prepend" and all results will be of the form prepend.xxx
 *    Tags take "max" and "start (1 indexed) to limit the number of items returned (if appropriate)
 * @author suhler@google.com (Stephen Uhler)
 *
 */

/* XXX API NOTES
 *   https://picasaweb.google.com/data/feed/api/user/[userID]/albumid/[albumID]?kind=...
 *   https://picasaweb.google.com/data/feed/api/user/[userID]/album/[albumName]?kind=...
 *   https://picasaweb.google.com/data/feed/api/user/[userID]/albumid/[albumID]/photoid/[photoID]?kind=...
 *   https://picasaweb.google.com/data/feed/api/user/[userID]/album/[albumName]/photoid/[photoID]?kind=...
 *   kind=param1,param2 ...
 *     params:  album photo comment tag user
 *  Addtional query parameters:
 *     max-results=nnn
 *     start-index=1
 *  imgmax=94,110,128,200,220,288,320,400,512,576,640,720,800,912,1024,1152,1280,1440,1600
 *  imgmax=d (original uploaded sizes)
 */
public class PicasaTemplate extends Template {

  private static final String AUTHENTICATE_HEADER = "www-authenticate";
  private static final String ERROR = "error";
  private static final String PICASA_FEED_URL =
      "https://picasaweb.google.com/data/feed/api/user/";
  /**
   * Our application identifier for gData
   */
  private static final String APP_STRING = "goog-androgen-0.1";

  /**
   * &lt;getauthtoken user=&quot;ldap&quot; pass=&quot;password&quot; [service=&quot;lh2&quot;]&gt;
   * Get an authentication token for a clientlogin service.  This is used by a browser.  Android
   * uses a different mechanism (e.g. an auth header).
   *
   * user:  the user@domain
   * pass:  The password
   * service:  The service name.
   *
   * The result will be placed in "name", or token.[service] and be "unauthenticated"
   * if authentication failed.
   * @param hr
   */

  public void tag_getauthtoken(RewriteContext hr) {
    hr.killToken();
    String service = hr.get("service", "lh2");
    GoogleService gs = new GoogleService(service, APP_STRING);
    String token;
    try {
      token = gs.getAuthToken(hr.get("user"), hr.get("pass"), null, null, service, APP_STRING);
    } catch (AuthenticationException e) {
      token = "unauthenticated";
      addErrorValue(hr, e);
    } catch (NullPointerException e) {
      token = "unauthenticated";
      addErrorValue(hr, e);
    }
    hr.getNamespaceProperties().put(hr.get("name", "token" + "." + service), token);
  }

  /**
   * For now, just try to create or list an album
   * <album token= | user=, pass=  prepend=...  command=...  ...>
   *   command=create   name=xxx [description=xxx]
   *   command=list
   * @param hr
   */

  public void
  tag_album(RewriteContext hr) {
    debug(hr);
    hr.killToken();
    PicasawebService pws;
    try {
      pws = getPicasaService(hr);
    } catch (AuthenticationException e) {
      System.err.println("Can't get authorization " + e);
      e.printStackTrace();
      return;
    }

    String command = hr.get("command");
    if ("create".equals(command)) {
      album_create(pws, hr);
    } else if ("list".equals(command)) {
      album_list(pws, hr);
    }
    return;
  }

  public void
  tag_photo(RewriteContext hr) {
    debug(hr);
    hr.killToken();
    PicasawebService pws;
    try {
      pws = getPicasaService(hr);
    } catch (AuthenticationException e) {
      e.printStackTrace();
      return;
    }

    String command = hr.get("command");
    if ("upload".equals(command)) {
      photo_upload(pws, hr);
    } else if ("list".equals(command)) {
      photo_list(pws, hr);
    }
    return;
  }

  private void photo_list(PicasawebService pws, RewriteContext hr) {
    String album = hr.get("album", "default");
    try {
      URL url = Pattern.matches("^\\d*$", album) ? getFeedUrl(hr, "albumid", album) :
        getFeedUrl(hr, "album", album);
      AlbumFeed feed = pws.getFeed(url, AlbumFeed.class);
      int count = 0;
      int photoSize = 0;
      try {
        photoSize = Integer.parseInt(hr.get("size", "0"));
      } catch (NumberFormatException e) { /* empty */ }

      for(PhotoEntry photo : feed.getPhotoEntries()) {
        count++;
        addValue(hr, count, "id", extractId(photo.getId()));
        addValue(hr, count, "description", photo.getDescription().getPlainText());
        addValue(hr, count, "location", photo.getGeoLocation());
        addValue(hr, count, "height", photo.getHeight());
        addValue(hr, count, "width", photo.getWidth());
        addValue(hr, count, "size", photo.getSize());
        addValue(hr, count, "rotation", photo.getRotation());
        addValue(hr, count, "url", photo.getHtmlLink().getHref());
        addValue(hr, count, "self", fixPhotoUrl(photo.getMediaThumbnails().get(0).getUrl(),
                photoSize));
        addValue(hr, count, "full", fixPhotoUrl(photo.getMediaThumbnails().get(0).getUrl(), 0));


        ExifTags tags = photo.getExifTags();
        for (ExifTag tag : tags.getExifTags()) {
          addValue(hr, count, "exif." + tag.getName(), tags.getExifTagValue(tag.getName()));
        }
      }
      addValue(hr, -1, "photos","" +  count);
    } catch (MalformedURLException e) {
      addErrorValue(hr, e);
    } catch (IOException e) {
      addErrorValue(hr, e);
    } catch (ServiceException e) {
      addErrorValue(hr, e);
    }
  }

  /*
   * Copy a photo from a resource to picassa
   * Return the "id" of the photo if uploaded sucessfully
   * <photo command=upload [album="album|albumid"] name="/resource/name.jpg" [title=... description=...]>
   *        delete=true|false
   */
  private void photo_upload(PicasawebService pws, RewriteContext hr) {
    String album = hr.get("album", "default");
    String photoName = hr.get("name");
    URL url;
    try {
      url = Pattern.matches("^\\d*$", album) ? getFeedUrl(hr, "albumid", album) :
        getFeedUrl(hr, "album", album);
    } catch (MalformedURLException e) {
      addErrorValue(hr, e);
      return;
    }
    String type = FileHandler.getMimeType(photoName, hr.request.getProps(), hr.prefix);
    if (type == null || (!type.startsWith("image/") && !type.startsWith("video/"))) {
      debug(hr, "Invalid or no file type");
      addValue(hr, -1, ERROR, "invalid file type for: " + photoName);
      return;
    }

    /*
     * Fetch data from a resource, or from the request, if we have a "post"
     */
    
    Resource resource;
    String namespace = hr.get("namespace", "brazil");
    if (hr.request.getMethod().equals("POST") && hr.request.getPostData().length > 0) {
      resource = new Resource(photoName, hr.request.getPostData());
      namespace=null;
    } else {
      resource = Resource.load(namespace + photoName);
      if (resource == null) {
        addValue(hr, -1, ERROR, "No such file: " + photoName);
        debug(hr, "No such file");
        return;
      }
    }

    // See if the album exists, create otherwise.

    boolean shouldCreateAlbum = false;
    if (hr.get("userID", "default").equals("default") && !Pattern.matches("^\\d*$", album)) {
      try {
        pws.getFeed(url, UserFeed.class);
      } catch (IOException e) {
        addErrorValue(hr, e);
      } catch (ServiceException e) {
        shouldCreateAlbum = true;
      }
    }

    // XXX We could pass in description and album type params here, if needed

    if (shouldCreateAlbum) {
      try {
        AlbumEntry createAlbum = createAlbum(pws, album, "auto-created via photo upload", true);
        addValue(hr, -1, "albumId", createAlbum.getId());
        addValue(hr, -1, "albumcreated", "true");
      } catch (IOException e) {
        addErrorValue(hr, e);
      } catch (ServiceException e) {
        addErrorValue(hr, e);
      }
    }

    PhotoEntry photo = new PhotoEntry();
    photo.setTitle(new PlainTextConstruct(hr.get("title", "title")));
    photo.setDescription(new PlainTextConstruct(hr.get("description", "description")));
    photo.setClient(APP_STRING);
    MediaByteArraySource media = new MediaByteArraySource(resource.getData(), type);
    photo.setMediaSource(media);
    PhotoEntry returnedPhoto;
    try {
      returnedPhoto = pws.insert(url, photo);
      addValue(hr, -1, "id", returnedPhoto.getId());
      addValue(hr, -1, "href", fixPhotoUrl(returnedPhoto.getMediaThumbnails().get(0).getUrl(), 0));
      if (hr.isTrue("delete") && namespace != null) {
        Resource.delete(namespace + photoName);
      }
    } catch (IOException e) {
      addErrorValue(hr, e);
    } catch (ServiceException e) {
      addErrorValue(hr, e);
    }
  }

  /**
   * @param pws
   * @param hr
   */
  private void album_list(PicasawebService pws, RewriteContext hr) {
    try {
      URL url = getFeedUrl(hr, "album", null);
      UserFeed feed = pws.getFeed(url, UserFeed.class);
      System.err.println("album feed: " + feed + " " + feed.getDescription().getPlainText());
      int count = 0;
      for (AlbumEntry album : feed.getAlbumEntries()) {
        count++;
        addValue(hr, count, "id", extractId(album.getId()));
        addValue(hr, count, "access", extractId(album.getAccess()));
        addValue(hr, count, "name", album.getName());
        addValue(hr, count, "title", album.getTitle().getPlainText());
        addValue(hr, count, "count", album.getPhotosUsed());
        addValue(hr, count, "description", album.getDescription().getPlainText());
        addValue(hr, count, "url", album.getHtmlLink().getHref());
      }
      addValue(hr, -1, "albums", "" +  count);

    } catch (MalformedURLException e) {
      addErrorValue(hr, e);
    } catch (IOException e) {
      addErrorValue(hr, e);
    } catch (ServiceException e) {
      addErrorValue(hr, e);
    } catch (Exception e) {
      addErrorValue(hr, e);
    }
  }

  /**
   * Extract the id from a photo or album complete identifier
   * @param id
   */
  private String extractId(String id) {
    return id.substring(id.lastIndexOf("/") + 1);
  }

  /**
   * Treat empty strings as nulls
   */
  @SuppressWarnings("unused")
  private String nullEmpty(String s) {
    if ("".equals(s)) {
      return null;
    } else {
      return s;
    }
  }

  private static final Pattern PHOTO_PATTERN = Pattern.compile("^(.*)(/s[0-9]+)(/[^/]*)$");

  /**
   * Adjust the photo URL to the specified size.
   * This uses an undocumented api, assuming url's of the form:
   *   https://something.googleblah.com/stuff/morestuff/.../s100/name.jpg
   * where the "s100" represents the size of the photo to be referenced.
   * @param url     The "thumbnail" url returned from the feed
   * @param size    the nominal size of the image (in pixels) or 0 for the "natural" size
   * @returns        The url with the size parameter modified
   */

  private String fixPhotoUrl(String url, int size) {
    if (size < 0 || size > 1600) {
      throw new IllegalArgumentException("Photos must be between 0 and 1600 pixels");
    }
    String replace = size > 0 ? "/s" + size : "";
    return PHOTO_PATTERN.matcher(url).replaceFirst("$1" + replace + "$3");
  }

  // this is dumb (e.g. n^2) - make a helper class
  private void addValue(RewriteContext hr, int count, String name, Object value) {
    if (value != null) {
      String index = count >= 0 ? "." +  count  : "";
      String key = hr.get("prepend", "results") + index + "." +  name;
      hr.getNamespaceProperties().put(key, value.toString());
      // System.err.println("add: " + key + "=" + value);
    }
  }

  /**
   * An error occurred, add error values to result.
   */
  private void addErrorValue(RewriteContext hr, Exception e) {
    System.err.println("Argh! " + e.getClass().getName() + ":" + e.getMessage());
    Properties props = hr.getNamespaceProperties();
    String prepend = hr.get("prepend", "results");
    props.put(prepend + "." + ERROR, e.getMessage());
    props.put(prepend + ".errorClass", e.getClass().getName());

    // we need to trap authentication failures and pass the info back to Androgen, so
    // a new auth token can be obtained.

    if (e instanceof ServiceException) {
      ServiceException se = (ServiceException) e;
      System.err.println("ex: " + se.getCodeName() + "/" + se.getInternalReason() + "/" + se.getDebugInfo());
      List<String> authHeaders = ((ServiceException) e).getHttpHeader(AUTHENTICATE_HEADER);
      if (authHeaders != null) {
        System.err.println("Authentication failure: " + authHeaders);
        hr.request.setStatus(401);
        for (String header : authHeaders) {
          hr.request.addHeader(AUTHENTICATE_HEADER, header);
        }
      }
    }
  }

  private void album_create(PicasawebService pws, RewriteContext hr) {
    try {
      AlbumEntry insertedEntry = createAlbum(pws,
          hr.get("name", "album"), hr.get("description"), hr.isTrue("protected"));
      System.err.println(insertedEntry.getId());
      addValue(hr, -1, "id", insertedEntry.getId());
    } catch (IOException e) {
      addErrorValue(hr, e);
    } catch (ServiceException e) {
      addErrorValue(hr, e);
    }
  }

  /**
   * Create a public photo album helper
   */
  private static AlbumEntry createAlbum(PicasawebService pws, String name, String desc,
          boolean isProtected) throws IOException, ServiceException {
    AlbumEntry album = new AlbumEntry();
    album.setTitle(new PlainTextConstruct(name));
    album.setAccess(isProtected ? GphotoAccess.Value.PROTECTED : GphotoAccess.Value.PUBLIC);
    if (desc != null) {
      album.setDescription(new PlainTextConstruct(desc));
    }
    return pws.insert(new URL(PICASA_FEED_URL + "default/"), album);
  }

  private PicasawebService getPicasaService(RewriteContext hr) throws AuthenticationException {
    PicasawebService pws = new PicasawebService(APP_STRING);
    String token = hr.get("token");
    if (token == null) {
      token = extractHttpToken(hr);
      System.err.println("Extracted token:" + token);
    }
    if (token != null) {
      pws.setUserToken(token);
    } else {
      String user = hr.get("user");
      String pass = hr.get("pass");
      if (user != null && pass != null) {
        pws.setUserCredentials(hr.get("user"), hr.get("pass"));
      } else {
        throw new AuthenticationException("No auth token to extract and user and/or password missing");
      }
    }
    System.err.println(hr.getBody() + " " + pws.getServiceVersion());
    return pws;
  }

  /**
   * Extract the auth token from the http auth header provided by the
   * android account manager,
   */
  private String extractHttpToken(RewriteContext hr) {
    String authHeader = hr.request.getHeaders().get("authorization");
    if (authHeader != null && authHeader.startsWith("GoogleLogin auth=")) {
      return authHeader.substring(authHeader.indexOf("=") + 1);
    } else {
      System.err.println("Missing GoogleLogin token");
      return null;
    }
  }

  // Make a picasa feed url - but just what I need
  private  URL getFeedUrl(RewriteContext hr, String path1, String path2) throws MalformedURLException {
    // "https://picasaweb.google.com/data/feed/api/user/default/
    String base = PICASA_FEED_URL + hr.get("userID", "default") + "/";
    String delim = "?";
    if (path1 != null && path2 != null) {
      base += path1 + "/" + path2 + "/";
    }
    else if (path1 != null) {
      base += "?kind=" + path1;
      delim = "&";
    }
    String max = hr.get("max");
    if (max != null) {
      base += delim + "max-results=" + max;
      delim = "&";
    }
    String start = hr.get("start");
    if (start != null) {
      base += delim + "start=" + start;
    }
    System.err.println("URL: " + base);
    return new URL(base);
  }
}