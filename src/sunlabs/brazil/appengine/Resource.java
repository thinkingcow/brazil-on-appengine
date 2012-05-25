// Experiment with virtual "file" objects to store in the datastore

package sunlabs.brazil.appengine;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.apphosting.api.ApiProxy.CapabilityDisabledException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;


/**
 * Represent a "file" as a persistent object.
 * @author suhler@google.com
 */

@PersistenceCapable
public class Resource {
  // local cache of objects
  private static Hashtable<String, Resource> cache = new Hashtable<String, Resource>();
  // use memcached if available
  private static MemcacheService memcacheService = null;
  // use JDO if available
  private static PersistenceManagerFactory pmfi = null;

  static {
    try {
      Class.forName("javax.jdo.JDOHelper");  // XX does this actually do anything?
      pmfi = JDOHelper.getPersistenceManagerFactory("transactions-optional");
    } catch (ClassNotFoundException e) {
      System.err.println("Warning: JDO not found, persistence disabled");
    }
    try {
      Class.forName("com.google.appengine.api.memcache.MemcacheServiceFactory");
      memcacheService = MemcacheServiceFactory.getMemcacheService();
    } catch (ClassNotFoundException e) {
      System.err.println("Warning: memcached not found, using local cache only");
    }
  }

  @PrimaryKey
  @Persistent private String path;    // the absolute path in the "virtual" file system
  @Persistent private String type;    // the mime type, if known
  @Persistent private long lastMod;   // last modified time, ms
  @Persistent private Blob data;      // the file data
  @Persistent private int version;    // arbitrary version # (optional)

  /**
   * Create a "virtual file" to persist
   * @param path    the name of the file
   * @param data    the file contents
   */
  public Resource(String path, byte[] data) {
    this(path, "application/octet-stream", System.currentTimeMillis(), 0, data);
  }

  /**
   * Create a "Virtual file".
   * @param path    the name of the file
   * @param type    The file mime type (if applicable)
   * @param lastMod The last modified time of the file
   * @param version A version number
   * @param data    The file contents
   */
  public Resource(String path, String type, long lastMod, int version, byte[] data) {
    this.path = path;
    this.type = type;
    this.lastMod = lastMod;
    this.data = new Blob(data);
    this.version = version;
  }

  public String getPath() {
    return path;
  }

  // use with caution.

  void setPath(String path) {
    this.path = path;
  }

  public long getLastMod() {
    return lastMod;
  }

  public byte[] getData() {
    return data.getBytes();
  }

  public int getVersion() {
    return version;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return path + " (" + lastMod + ")";
  }

  /**
   * This helper is the "Serializable" version of a Resource.
   * @author suhler@google.com
   *
   */

  static class Helper implements Serializable {
    private String path;    // the absolute path in the "virtual" file system
    private String type;    // the mime type, if known
    private long lastMod;   // last modified time, ms
    private byte[] data;    // the file data
    private int version;    // arbitrary version # (optional)

    public Helper(Resource r) {
      this.path = r.getPath();
      this.type = r.getType();
      this.lastMod = r.getLastMod();
      this.data = r.getData();
      this.version = r.getVersion();
    }

    public Resource asResource() {
      return new Resource(path, type, lastMod, version, data);
    }
  }

  /**
   * put a resource into either the memcached or local cache.
   * @param resource
   */

  static void put(Resource resource) {
    if (memcacheService != null) {
      memcacheService.put(resource.getPath(), new Helper(resource));
    } else {
      cache.put(resource.getPath(), resource);
    }
  }

  static Resource get(String path) {
    Resource result = null;
    if (memcacheService != null) {
      Helper h = (Helper) memcacheService.get(path);
      if (h != null) {
        result = h.asResource();
      }
    } else {
      result = cache.get(path);
    }
    return result;
  }

  /**
   * Remember a resource.
   * @param resource
   */
  public static void save(Resource resource) {
    synchronized (resource) {
      put(resource);

      // I'd like to do this in the background (write back, not write-thru)
      // but I haven't got around to it yet.
      if (pmfi != null) {
        PersistenceManager pm = pmfi.getPersistenceManager();
        Transaction tx = pm.currentTransaction();
        try {
          System.err.println("Persist... " + resource);
          tx.begin();
          pm.makePersistent(resource);
          tx.commit();
          System.err.println("done " + resource);
        } catch (CapabilityDisabledException e) {
          // Datastore is read-only, degrade gracefully
        } catch (Exception e) {
          removeFromCache(resource.path);
          e.printStackTrace();
          if (tx.isActive()) {
            tx.rollback();
          }
        } finally {
          pm.close();
        }
      }
    }
  }

  /**
   * Retrieve a resource.
   * @param path
   * @return    The resource associated with "path" or null
   */

  public static Resource load(String path) {
    return load(path, false);
  }

  public static Resource load(String path, boolean force) {
    synchronized(path) {
      Resource result = null;
      if (!force) {
        result = get(path);
      }

      if (result == null && pmfi != null) {
        PersistenceManager pm = pmfi.getPersistenceManager();
        try {
          result = pm.getObjectById(Resource.class, path);
          put(result);
        } catch(JDOObjectNotFoundException e) {
          System.err.println("Not in cache: " + path + (result==null ? " N/A":" Found"));
        } finally {
          pm.close();
        }
      }
      System.err.println("Fetching resource (" + path + ") " +
              (result != null ? "found" : "missing"));
      return result;
    }
  }

  /**
   * Delete a resource.
   * @param path
   */
  public static boolean delete(String path) {
    boolean removed;
    removed = removeFromCache(path);
    if (pmfi != null) {
      PersistenceManager pm = pmfi.getPersistenceManager();
      Transaction tx = pm.currentTransaction();
      Resource resource = null;
      try {
        resource = pm.getObjectById(Resource.class, path);
      } catch (JDOUserException e) {
        // ignore
      }
      if (resource != null) {
        try {
          tx.begin();
          System.err.println("Deleting: " + resource);
          pm.deletePersistent(resource);
          System.err.println("Deleted");
          tx.commit();
        } catch (JDOUserException e) {
          System.err.println("oops:" + e);
          e.printStackTrace();
        } finally {
          pm.close();
        }
      }
    }
    return removed;
  }

  public static boolean removeFromCache(String path) {
    boolean removed;
    if (memcacheService != null) {
      removed = memcacheService.delete(path);
    } else {
      removed = (cache.remove(path) != null);
    }
    return removed;
  }

  // do searching here

  @SuppressWarnings("unchecked")
  public static Collection<ResourceInfo> search(String prefix) {
    ArrayList<ResourceInfo> results = new ArrayList<ResourceInfo>();
    System.err.println("Starting search for: " + prefix);
    if (pmfi != null) {
      PersistenceManager pm = pmfi.getPersistenceManager();
      String search = "path.startsWith(\"" + prefix + "\")";
      Query query = pm.newQuery(Resource.class, search);
      // query.setKeysOnly(); - needs to be com.google.appengine.api.datastore.Query
      Collection<Resource> col = (Collection<Resource>) query.execute();
      if (col != null) {
        for (Iterator<Resource> i = col.iterator(); i.hasNext ();) {
          results.add(new ResourceInfo(i.next ()));
        }
      }
      query.closeAll();
      System.err.println("Done: " + col.size());
    }
    return results;
  }

  /**
   * Clear the entire local cache.
   * @return The number of items removed
   */

  public static int clearCache() {
    int result = -1;
    if (memcacheService != null) {
      memcacheService.clearAll();
    } else {
      result = cache.size();
      cache.clear();
    }
    return result;
  }

  /**
   * Clear an item from the local cache.
   * @param name  the item to clear, if any
   */
  public static void clear(String name) {
    if (memcacheService != null) {
      memcacheService.delete(name);
    } else {
      cache.remove(name);
    }
  }

  /**
   * Make an immutable copy of resource meta-information
   * @author suhler@google.com (Stephen Uhler)
   *
   */
  public static class ResourceInfo {
    public final String name;
    public final long lastModTime;
    public final int size;

    private ResourceInfo(Resource resource) {
      this.name = resource.getPath();
      this.lastModTime = resource.getLastMod();
      // this.size = resource.getData().length;  // This is expensive, need to rethink
      this.size = -1;
    }

    @Override
    public String toString() {
      return "[" + name + " (" + lastModTime + ")]";
    }
  }
}