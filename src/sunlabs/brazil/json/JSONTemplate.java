/*
 * JSONTemplate.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2008-2009 Sun Microsystems, Inc.
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
 * The Initial Developer of the Original Code is: suhler.
 * Portions created by suhler are Copyright (C) Sun Microsystems, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): suhler.
 *
 * Version:  1.10
 * Created by suhler on 08/09/17
 * Last modified by suhler on 09/09/04 11:17:21
 *
 * Version Histories:
 *
 * 1.10 09/09/04-11:17:21 (suhler)
 *   doc fixes, lint
 *
 * 1.9 09/07/13-14:17:35 (suhler)
 *   doc fixes
 *
 * 1.8 09/07/13-13:54:05 (suhler)
 *   add the capability to "merge" json objects
 *
 * 1.7 09/07/13-12:04:09 (suhler)
 *   - deprecate <json ...> incorporating its functionality into
 *   <item prefix=xxx>
 *
 * 1.6 09/07/08-07:47:38 (suhler)
 *   doc fixes
 *
 * 1.5 09/07/06-08:38:09 (suhler)
 *   added inline values for <item>
 *
 * 1.4 09/06/12-13:27:02 (suhler)
 *   - convert properties -> json recursively
 *   - add a json -> properties converter as well
 *
 * 1.3 08/09/19-13:59:30 (suhler)
 *   both JSON styles work: need to deal with errors properly
 *
 * 1.2 08/09/19-10:31:19 (suhler)
 *   mess up the vm,
 *
 * 1.2 70/01/01-00:00:02 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 json/JSONTemplate.java
 *
 * 1.1 08/09/17-15:23:39 (suhler)
 *   date and time created 08/09/17 15:23:39 by suhler
 *
 */

/* MODIFICATIONS
 * 
 * Modifications to this file are derived, directly or indirectly, from Original Code provided by the
 * Initial Developer and are Copyright 2010-2012 Google Inc.
 * See Changes.txt for a complete list of changes from the original source code.
 */

package sunlabs.brazil.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.Format;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Template for generating (or extracting) JSON format data.
 * <dt>&lt;json2props src="json text" prepend="..." [delim="..."]&gt;
 * <dd> Flatten JSON (supplied in "src") into a set of properties prefixed
 * by "prepend".  This is the inverse of the
 *  <code>&lt;jsonitem, prefix="..."&gt;</code>
 * tag (if the delim is ".", the default).
 * <p>
 * The &lt;json2props&gt; tag can be used to flatten a json object into
 * a set of properties, which can be manipulated as a set of name/value
 * pairs using the
 * standard mechanisms, then turned back into a JSON object again.
 * </dl>
 * <p>
 * The following set of templates can be used to generate a JSON object via
 * an *ML representation.
 * <dl>
 * <dt>&lt;jsonarray&gt; ... &lt;/jsonarray&gt;
 * <dd>Create a JSON array
 * <dt>&lt;jsonobject&gt; ... &lt;/jsonobject&gt;
 * <dd>create a JSON object
 * <dt>&lt;jsonitem value=xxxx /&gt;
 * <dt>&lt;jsonitem valueinline /&gt; ... &lt;/jsonitem&gt;
 * <dd>Add an element onto a JSON array
 * <dt>&lt;jsonitem name=xxx value=xxxx /&gt;
 * <dt>&lt;jsonitem name=xxx&gt; ... &lt;/jsonitem&gt;
 * <dt>&lt;jsonitem name=xxx valueinline /&gt; ... &lt;/jsonitem&gt;
 * <dd>Add an entry into a JSON object.
 * <dt>&lt;jsonitem prepend=xxx /&gt;
 * <dt>&lt;jsonitem name=xxx prepend=xxx /&gt;
 * <dd>Add items onto a JSON array or object from a set of Properties.
 * See the discussion under &lt;jsonitem&gt; for more specifics.
 * </dl>
 * <p>
 * All the markup in the current template is replaced
 * with the JSON generated by the "jsonobject", "jsonarray" and "jsonitem" tags.
 * (except when "jsonextract" is used).
 * <p>
 * Additional tags.
 * <dl>
 * <dt>jsonextract
 * <dd>Extract the JSON into a property.
 * <dt>jsonp
 * <dd>wrap the resulting JSON in a Javascript function call (e.g. JSONP).
 * </dl>
 * <ul>
 * <li> Error handling is currently incomplete, the code tries to
 * to guess what is intended in the face of errors.
 * </ul>
 */

public class JSONTemplate extends Template {

  private static final String ARRAY_COUNT_SUFFIX = "_Count";
  private static final String ARRAY_INDECES_SUFFIX = "_Indeces";
  Vector<String> items = new Vector<String>(); // list of items to JSONify
  Stack<Object> stack = new Stack<Object>();   // Json Stack
  // Stack stack = new MyStack();   // Json Stack
  private String functionName = null;    // possible JSONP wrapper

  @Override
  public boolean init(RewriteContext hr) {
    items.setSize(0);
    stack.setSize(0);
    return super.init(hr);
  }

  /**
   * This tag is used to map a set of properties to a
   * JSON object.  If more than one tag is present, a JSON array will be produced.
   * <br>Deprecated - use the "prefix" attribute of the "item" tag instead.
   */

  public void
  tag_json(RewriteContext hr) {
    String pre = hr.get("prefix");
    hr.killToken();
    if (pre != null) {
      items.addElement(pre);
    }
    return;
  }

  public void
  tag_jsonobject(RewriteContext hr) {
    hr.killToken();
    stack.push(new JSONObject());
  }

  public void
  tag_slash_jsonobject(RewriteContext hr) {
    hr.killToken();
    if (!(stack.peek() instanceof JSONObject)) {
      oops(hr, "need </object>");
    }
    popHelper(hr);
  }

  public void
  tag_jsonarray(RewriteContext hr) {
    hr.killToken();
    stack.push(new JSONArray());
  }

  public void
  tag_slash_jsonarray(RewriteContext hr) {
    hr.killToken();
    if (!(stack.peek() instanceof JSONArray)) {
      oops(hr, "need </array>");
    }
    popHelper(hr);
  }

  /**
   * If this is a singleton, then the name must be defined if in the
   * context of an "object", and "value" must be defined in in the
   * context of an "array".
   * <p>
   * Otherwise, "name" must be defined, and there should be either
   * an &lt;jsonarray&gt;..&lt;/jsonarray&gt; or
   * &lt;jsonobject&gt;..&lt;/jsonobject&gt; tag pair before the
   * enclosing &lt;/jsonitem&gt;.
   * <p>
   * if "valueinline" is defined, then all the markup until the
   * matching closing &lt;/jsonitem&gt; is taken as the value.  The
   * additional boolean attributes "trim" and "eval" can be used
   * to trim whitespace from, and evaluate ${...} constructs from
   * the value respectively.
   *
   * If "prefix" is defined instead of "value" or "valueinline"
   * then the value of the object (or array element if name is missing
   * and the current context is an array), the object value is generated
   * implicitly from the current request properties of the described
   * prefix. An optional attribute "delim" (which default to ".") is
   * used to allow the generation of nested values.
   * <p>
   * If the name attribute is missing with "prefix",
   * and the current context is an
   * &lt;jsonobject&gt;, then the JSON object implied by the "prefix" is
   * merged into the existing object. So, if the property is defined:
   * <pre>
   * foo.I=am here
   * </pre>
   * Then The markup:
   * <pre>
   * &lt;jsonobject&gt;
   *   &lt;jsonitem name="test" value="ing" /&gt;
   *   &lt;jsonitem name=other prefix=foo /&gt;
   * &lt;/jsonobject&gt;
   * </pre>
   * produces:
   * <pre>
   *{
   *  "other": {"I": "am here"},
   *  "test": "ing"
   *}
   * </pre>
   * Whereas without the "name" attribute the markup
   * <pre>
   * &lt;jsonobject&gt;
   *   &lt;jsonitem name="test" value="ing" /&gt;
   *   &lt;jsonitem prefix=foo /&gt;
   * &lt;/jsonobject&gt;
   * </pre>
   * will produce:
   * <pre>
   *{
   *  "I": "am here",
   *  "test": "ing"
   *}
   * </pre>
   * <p>
   * Each property whose name is <code>[prefix].a.b.  ... n</code> is
   * created as
   * a node in the resulting json object.  Any objects whose entire entries
   * consist of "0, 1, ... n" are converted into an array.
   * Similarly, the values
   * "true", "false", and "null" are treated as JSON booleans (or null).
   * numbers are converted into JSON integers.
   * <br>[Note: it is not possible to distinquish booleans, nulls and
   * integers from
   * their string equivalents: Properties only deal with strings.]
   * <p>
   * For example, if the following properties are defined:
   * <pre>
   *  foo.a=hi
   *  foo.b.0=nothing
   *  foo.b.1=something
   *  foo.number=27
   *  foo.ok=true
   * </pre>
   * Then the markup:
   * &lt;jsonobject&gt;
   *   &lt;jsonitem name=implicit prefix="foo" /&gt;
   * &lt;/jsonobject&gt;
   * will produce the output:
   * <pre>
   *{"implicit": {
   *  "a": "hi",
   *  "b": [
   *    "nothing",
   *    "something"
   *  ],
   *  "number": 27,
   *  "ok": true
   *}}
   * </pre>
   */

  public void
  tag_jsonitem(RewriteContext hr) {
    hr.killToken();
    if (stack.size() == 0) {
      stack.push(new JSONObject());
      oops(hr, "inserting implied object");
    }
    String name = hr.get("name");
    String prefix = hr.get("prefix");
    Object parent = stack.peek();
    boolean single = hr.isSingleton();
    Object jv = jsonValue(hr.get("value")); // json value of item

    // use the enclosed markup as the value

    if (hr.isTrue("valueinline") && !single) {
      boolean trim = hr.isTrue("trim");
      boolean eval = hr.isTrue("eval");

      String value = hr.snarfTillClose();
      single = true;
      if (trim) {
        value = value.trim();
      }
      if (eval) {
        jv = jsonValue(Format.subst(hr.request.getProps(), value, true));
      } else {
        jv = jsonValue(value);
      }

      // create json value implied by properties prefix

    } else if (single && prefix != null) {
      try {
        jv = deflatten(hr.request.getProps(), prefix, hr.get("delim","."));
      } catch (JSONException e) {
        debug(hr, "Invalid json: " + e);
      }
    }

    if (!single && name != null) { 
      stack.push(new Item(name));
    } else if (parent instanceof JSONObject && name != null) {
      try {
        ((JSONObject) parent).put(name, jv);
      } catch (JSONException e) {
        oops(hr, "invalid item" + e);
      }
    } else if (parent instanceof JSONArray) {
      ((JSONArray) parent).put(jv);
    } else if (parent instanceof JSONObject && name == null &&
            jv instanceof JSONObject) {
      try {
        merge((JSONObject) parent, (JSONObject) jv);
      } catch (JSONException e) {
        oops(hr, "invalid merge" + e);
      }
    } else {
      oops(hr, "Invalid parent for item");
    }
  }

  /**
   * We finished our item: pop and add to our parent (which must be an object)
   */

  public void
  tag_slash_jsonitem(RewriteContext hr) {
    hr.killToken();
    if (stack.size() == 0) {
      oops(hr, "No matching tag");
      return;
    }
    if (!(stack.peek() instanceof Item)) {
      oops(hr, "need item on stack");
      return;
    }
    Item me = (Item) stack.pop();
    JSONObject parent = (JSONObject) stack.peek();
    if (me.value != null) {
      try {
        parent.put(me.name, me.value);
      } catch (JSONException e) {
        oops(hr, "invalid item" + e);
      }
    }
  }

  /**
   * Wrap JSON output (e.g. JSONP)
   * attribute: function="function name".
   * If no "function" attribute is provided, "command" is used.
   * Specify the empty string ("") to turn off JSONP wrapping
   */
  
  public void tag_jsonp(RewriteContext hr) {
    functionName = hr.get("function", "command");
    hr.killToken();
  }
  
  /**
   * Extract the current json into a property as a string, optionally resetting it.
   * Attributes:
   * <dl>
   * <dt>name<dd>Where to stuff the json (defaults to "json")
   * <dt>noreset<dd>true|false: Reset the current json? (defaults to false)
   * <dt>namespace<dd>Where to put the result (defaults to "local")
   * </dl>
   */
  
  public void tag_jsonextract(RewriteContext hr) {
    String name = hr.get("name", "json");
    boolean noReset = hr.isTrue("noreset");
    
    hr.killToken();
    if (stack.size() == 1) {
      String result = toString(stack.pop(), 2);
      hr.getNamespaceProperties().put(name, result);
      if (!noReset) {
        items.setSize(0);
        stack.setSize(0);
      }
    }
  }
  
  /**
   * Turn a JSON object into a set of properties.
   * Attributes:
   * <dl>
   * <dt>src     <dd>The json String
   * <dt>property<dd>The variable containing the json string (takes precidence)
   * <dt>delim   <dd>The flattening delimiter (defaults to '.')
   * <dt>prepend <dd>The prefix to use for all properies, defaults to the template prefix
   * </dl>
   * (This is not the proper way to do this - see the list template)
   */

  public void tag_json2props(RewriteContext hr) {
    String src = hr.get("src");
    String property = hr.get("property");
    String prepend = hr.get("prepend", hr.prefix);
    String delim = hr.get("delim", ".");

    if (prepend.endsWith(delim)) {
      prepend = prepend.substring(0, prepend.length() - delim.length());
    }
    
    if (property != null) {
      src = hr.request.getProps().getProperty(property, "").trim();
    } else if (src != null) {
      src = src.trim();
    } else {
      debug(hr, "No json supplied!");
      return;
    }

    Properties props = hr.getNamespaceProperties();
    Object j=null;

    hr.killToken();

    try {
      if (src.startsWith("{")) {
        j = new JSONObject(src);
      } else if (src.startsWith("[")) {
        j = new JSONArray(src);
      } else {
        throw new JSONException("JSON must start with '{' or '['");
      }
    } catch (JSONException e) {
      debug(hr, "Invalid json: " + e);
    }

    try {
      flatten(prepend, delim, props, j);
    } catch (JSONException e) {
      debug(hr, "Invalid json: " + e);
    }
  }

  /**
 .    * Recursively extract a json object into a set of properties
   * @param prefix	The prefix for this property name
   * @param delim	The delimiter to use between levels
   * @param p		The properties object to store the flattened tree into
   * @param obj	The object to flatten
   */

  public static void
  flatten(String prefix, String delim, Properties p, Object obj) throws JSONException {
    if (obj instanceof JSONObject) {
      JSONObject jo = (JSONObject) obj;
      String items[] = JSONObject.getNames(jo);
      for (int i = 0; i < items.length; i++) {
        flatten(prefix + delim + items[i], delim, p, jo.get(items[i]));
      }
    } else if (obj instanceof JSONArray) {
      JSONArray ja = (JSONArray) obj;
      for (int i = 0; i < ja.length(); i++) {
        flatten(prefix + delim + i, delim, p, ja.get(i));
      }
      // add an array count
      p.put(prefix + ARRAY_COUNT_SUFFIX, "" + ja.length());
      p.put(prefix + ARRAY_INDECES_SUFFIX, genIndeces(ja.length()));
    } else {
      p.put(prefix, obj == null ? "null" : obj.toString());
    }
  }
  
  /**
   * Generate a list of array indeces
   */
  private static String genIndeces(int n) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < n; i++) {
      if (i > 0) {
        sb.append(" ");
      }
      sb.append(i);
    }
    return sb.toString();
  }

  /**
   * Merge two json objects.
   * If the same name exists in both objects, use the 2nd one.
   * This could be smarter, if required.
   */

  public static JSONObject
  merge(JSONObject jo1, JSONObject jo2) throws JSONException {
    String names[] = JSONObject.getNames(jo2);
    for(int i=0; names != null && i < names.length; i++) {
      jo1.put(names[i], jo2.get(names[i]));
    }
    return jo1;
  }

  /**
   * Turn a JSON object consising of entirely numerical
   * indeces into an array object. Return it or the JSONArray that'll replace it.
   * When turning a flattened tree into JSON using the current scheme, it isn't
   * possible to distinguish object containing only numerical indeces from array,
   * so we de-flatten using only objects, then we can assume all objects with
   * "array" indeces are arrays, and convert them appropriately.
   */

  static Object toArray(JSONObject jo) throws JSONException {
    if (jo.length() == 0) {
      return jo;
    }
    for (int i = 0; i < jo.length(); i++) {
      if (!jo.has("" + i)) {
        return jo;
      }
    }

    // turn me into an array, and return it

    JSONArray ja = new JSONArray();
    for (int i = 0;i < jo.length(); i++) {
      ja.put(jo.get("" + i));
    }
    return ja;
  }

  /**
   * Recursively turn any array like objects into arrays.
   */

  public static Object simplify(Object o) throws JSONException {
    if (o instanceof JSONObject) {
      JSONObject jo = (JSONObject) o;
      String names[] = JSONObject.getNames(jo);
      for(int i = 0; names != null && i < names.length; i++) {
        jo.put(names[i], simplify(jo.get(names[i])));
      }
      o = toArray(jo);
    } else if (o instanceof JSONArray) {
      JSONArray ja = (JSONArray) o;
      for (int i=0; i < ja.length(); i++) {
        ja.put(i, simplify(ja.get(i)));
      }
    }
    return o;
  }

  /**
   * Add a nested property for a JSON object, creating the
   * intermediate objects as needed.
   * @param jo	The json object to add a node to
   * @param st	The tokenized property name (consumed)
   * @param value	The String value to add (or number or boolean)
   */

  static JSONObject
  add(JSONObject jo, StringTokenizer st, String value) throws JSONException {
    String name = st.nextToken();
    if (st.hasMoreTokens()) {
      JSONObject node;
      try {
        node = (JSONObject) jo.get(name);
      } catch (JSONException e) {
        node = new JSONObject();
      }
      jo.put(name, add(node, st, value));
    } else {
      jo.put(name, jsonValue(value));
    }
    return jo;
  }

  /**
   * Return a value as an object representing its type.
   * @return	null, Boolean, Integer, or String.
   */

  static Object jsonValue(String value) {
    Object result;
    if (value == null || value.equals("null")) {
      result = null;
    } else if (value.equals("true") || value.equals("false")) {
      result = new Boolean(value);
    } else {
      try {
        result = Integer.decode(value);
      } catch (NumberFormatException e) {
        result = value;
      }
    }
    return result;
  }

  /**
   * Un-Flatten a subset of a properties object into JSON object.
   * @param p		The flattened name/value pairs
   * @param prefix	The prefix all names must match
   * @param delim	The property delimiter
   * @return		A JSONObject or JSONArray
   */

  public static
  Object deflatten(Properties p, String prefix, String delim) throws JSONException {
    @SuppressWarnings("rawtypes")
    Enumeration names = p.propertyNames();
    JSONObject jo= new JSONObject();
    if (!prefix.endsWith(delim)) {
      prefix += delim;
    }
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      if (name.startsWith(prefix)) {
        String value = p.getProperty(name);
        add(jo, new StringTokenizer(
                name.substring(prefix.length()), delim), value);
      }
    }
    return simplify(jo);
  }

  /*
   * An object or array can be inserted into either an item or an array.
   * Pop, and insert us into our parent
   */

  void
  popHelper(RewriteContext hr) {
    if (stack.size() == 1) {
      return;	// we are the root;
    }
    Object me = stack.pop();
    Object parent = stack.peek();
    if (parent instanceof Item) {
      ((Item) parent).put(me);
    } else if (parent instanceof JSONArray) {
      JSONArray a = (JSONArray) parent;
      a.put(me);
    } else {
      oops(hr, "need item or array as parent");
    }
  }

  /*
   * We are done with this array or object.
   * - we are the value associated with a string to be added to our parent
   * - we are the root object
   * - we have already been added as an array element
   * - something went wrong
   */

  @Override
  public boolean
  done(RewriteContext hr) {

    boolean idt = (hr.request.getProps().getProperty(hr.prefix + "indent") != null);
    boolean show = hr.request.getProps().getProperty(hr.prefix + "show") != null;
    String type = hr.request.getProps().getProperty(hr.prefix + "type");

    if (stack.size() == 1) {
      hr.reset();	// toss any existing markup
      String result = toString(stack.pop(), (idt ? 2 : 0));
      if (functionName != null && functionName.length() > 0) {
        hr.append(functionName).append("(").append(result).append(");");
      } else {
        hr.append(result);
      }
      if (type != null) {
        hr.request.addHeader("content-type", type);
      }
      if (show) {
        System.err.println(result);
      }
      return super.done(hr);
    }

    if (stack.size() > 1) {
      oops(hr, "Non terminated markup");
      if (show) {
        System.err.println("parse error: " + toString(stack.pop(), 2));
      }
      return super.done(hr);
    }

    // Now deal with the <json> tag (Deprecated - to be removed)

    if (items.isEmpty()) {
      return super.done(hr);
    }

    if (items.isEmpty()) {
      return true;  // XXX temp
    }

    JSONArray ja = null;    // JSON array
    Object jo = null;       // JSON object

    /* replace all the output with JSON */

    hr.reset();	// toss any existing markup
    if (items.size() > 1) {
      ja = new JSONArray();
    }
    for(int i = 0;i < items.size(); i++) {
      String name = items.elementAt(i);

      try {
        jo = deflatten(hr.request.getProps(), name, ".");
      } catch (JSONException e) {}
      if (ja != null && jo != null) {
        ja.put(jo);
      }
    }
    try {
      if (ja != null) {
        hr.append(ja.toString(idt ? 2 : 0));
      } else if (jo instanceof JSONObject) {
        hr.append(((JSONObject) jo).toString(idt ? 2 : 0));
      } else {
        hr.append(((JSONArray) jo).toString(idt ? 2 : 0));
      }
    } catch (JSONException e) {}

    if (type != null) {
      hr.request.addHeader("content-type", type);
    }
    return super.done(hr);
  }

  String toString(Object o, int indent) {
    String value = null;
    try {
      if (o instanceof JSONObject) {
        value = ((JSONObject) o).toString(indent);
      } else if (o instanceof JSONArray) {
        value = ((JSONArray) o).toString(indent);
      } else {
        value = o.toString();
      }
    } catch (JSONException e) {
      value = e.toString();
    }
    return value;
  }

  /*
   * parse error, emit message somehow
   */

  void oops(RewriteContext hr, String message) {
    System.out.println("Parse error at " + hr.getTag() + ": " +
            message);
    System.out.println("- Stack size: " + stack.size());
    if (stack.size() > 0) {
      Object o = stack.peek();
      System.out.println("- Stack top " + o.getClass() + ": " + o.toString());
    }
  }

  /**
   * Class to represent an complex item:
   * name=[object] or [name=[array]
   */

  static class Item {
    public String name;
    public Object value;

    Item(String name) {
      this.name = name;
      this.value = null;
    }

    public void put(Object o) {
      this.value = o;
    }

    @Override
    public String toString() {
      return "item(" + name + "," +
      (value == null ? "null" : value.toString()) + ")";
    }
  }

  /**
   * For debugging only
   */

  @SuppressWarnings("unchecked")
  static class MyStack extends Stack<Object> {

    @Override
    public Object push(Object o) {
      System.out.println(size() + "  Push " + o.getClass() + ": " + o);
      return super.push(o);
    }

    @Override
    public Object peek() {
      Object o = super.peek();
      System.out.println(size() + " Peek " + o.getClass() + ": " + o);
      return o;
    }

    @Override
    public Object pop() {
      Object o = super.pop();
      System.out.println(size() + "  Pop " + o.getClass() + ": " + o);
      return o;
    }
  }
}