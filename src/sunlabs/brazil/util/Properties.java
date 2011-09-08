package sunlabs.brazil.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;


/**
 * Version of java.util.properties that has the proper genericized parent.
 * (e.g. extends HashTable<String, String> instead of Hashtable<Object, Object>)
 * and delgates to java.util.Properties for load/store et. al.
 * @author suhler@google.com (Stephen Uhler)
 *
 */
public class Properties extends Hashtable<String, String> {
    protected Properties defaults;
    
    public Properties() {
      this(null);
    }
    
    public Properties(Properties defaults) {
      this.defaults = defaults;
    }
    
    public String getProperty(String key) {
      String value = get(key);
      if (value == null && defaults != null) {
        value = defaults.getProperty(key);
      }
      return value;
    }
    
    Enumeration<String> propertyNames() {
      if (defaults == null) {
        return keys();
      } else {
        return fillIn().keys();
      }
    }
    
    public String getProperty(String key, String defaultValue) {
      String value = getProperty(key);
      return value == null ? defaultValue : value;
    }
    
    public void list(PrintStream out) {
      convertToProps().list(out);
    }
    
    public void list(PrintWriter out) {
      convertToProps().list(out);
    }
     
    public void load(InputStream in) throws IOException {
      java.util.Properties props = new java.util.Properties();
      props.load(in);
      copyFromProps(props);
    }
    
    public void loadFromXML(InputStream in) throws IOException {
      java.util.Properties props = new java.util.Properties();
      props.loadFromXML(in);
      copyFromProps(props);
    }
    
    public void storeToXML(OutputStream os, String comment) throws IOException {
      convertToProps().storeToXML(os, comment);
    }
    
    public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
      convertToProps().storeToXML(os, comment, encoding);
    }
    
    @Deprecated
    public void save(OutputStream out, String comments) {
      convertToProps().save(out, comments);
    }
    
    public String setProperty(String key, String value) {
      return put(key, value);
    }
    
    public void store(OutputStream out, String comments) throws IOException {
      convertToProps().store(out, comments);
    }
    
    /**
     * Copy entries from a properties object, string-ifying them as needed.
     * @param props  A java.util.Properties object
     */
    private void copyFromProps(java.util.Properties props) {
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        put((String) entry.getKey(), (String) entry.getValue());
      }
    }

    /**
     *Create a map with the union of all the properties.
     */
    
    private Hashtable<String, String> fillIn() {
      Hashtable<String, String> flatten = new Hashtable<String, String>();
      fillIn(flatten);
      return flatten;
    }
    
    private void fillIn(Map<String, String> map) {
      if (defaults != null) {
        defaults.fillIn(map);
      }
      map.putAll(this);
    }
    
    /**
     * Populate a java.util.properties object for store...
     */
    
    private java.util.Properties convertToProps() {
      java.util.Properties props = new java.util.Properties();
      convertToProps(props);
      return props;
    }
    
    private void convertToProps(java.util.Properties props) {
      if (defaults != null) {
        defaults.convertToProps(props);
      }
      props.putAll(this);
    }
}