/*
 * MiscTemplate.java
 *
 * Brazil project web application toolkit,
 * export version: 2.3 
 * Copyright (c) 2005-2009 Sun Microsystems, Inc.
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
 * Version:  1.29
 * Created by suhler on 05/06/13
 * Last modified by suhler on 09/12/27 10:39:55
 *
 * Version Histories:
 *
 * 1.29 09/12/27-10:39:55 (suhler)
 *   add "javascript" and "hidetags" converters, which
 *   protext "<" ">", and properly escape javascript respecively
 *
 * 1.28 09/09/30-16:10:23 (suhler)
 *   doc updates
 *
 * 1.27 09/07/01-11:43:36 (suhler)
 *   add the "hidetags" conversion, to escape < and > only
 *
 * 1.26 09/06/30-09:37:23 (suhler)
 *   add "min" and "max" to <squence>
 *
 * 1.25 09/06/03-10:54:23 (suhler)
 *   add <subst> tag for ${...} substitutions
 *
 * 1.24 09/04/22-11:45:52 (suhler)
 *   kill errant closing tag
 *
 * 1.23 08/12/15-13:19:07 (suhler)
 *   add tag_replace
 *
 * 1.22 08/09/11-10:14:31 (suhler)
 *   undo accidental removal of 2 lines
 *
 * 1.21 08/09/11-10:10:46 (suhler)
 *   use the proper namespace for Append
 *   .
 *
 * 1.20 08/07/24-16:51:01 (suhler)
 *   preserve token accumulation state
 *
 * 1.19 08/06/11-08:44:49 (suhler)
 *   add "strip" option to remove characters from a string
 *
 * 1.18 08/02/19-11:12:26 (suhler)
 *   remove the "convert=" option from <stringop...> and replace it with
 *   an extensible plugin conversion mechanism
 *
 * 1.17 08/02/11-13:52:04 (suhler)
 *   add <stringop escape=xxx> to put \'s infront of certain characters
 *
 * 1.16 06/12/04-08:10:09 (suhler)
 *   add format= to <expr>
 *   .
 *
 * 1.15 06/11/13-11:50:29 (suhler)
 *   allow <inline esc=true|false> to turn on/off \X substitutions
 *
 * 1.14 06/08/01-14:10:34 (suhler)
 *   removed comment describing just fixed bug
 *   .
 *
 * 1.13 06/08/01-14:08:46 (suhler)
 *   Tell the SetTemplate if we created a previously empty but remembered
 *   namespace (yuk)
 *
 * 1.12 06/07/31-15:00:51 (suhler)
 *   Added note about mapping variables to imported but not yet created namespaces.
 *   This is hard to fix 'cause the information is kept in an instance variable
 *   this class can't get to; it will require better refactoring of the SetTemplate.
 *   This has always been a mis-feature of <mapnames>
 *
 * 1.11 06/07/31-14:44:25 (suhler)
 *   added remove=true option to mapnames
 *
 * 1.10 06/05/26-17:14:18 (suhler)
 *   doc fixes. add "append" to <inline>
 *
 * 1.9 06/01/20-10:31:50 (suhler)
 *   add random
 *
 * 1.8 05/12/22-16:40:11 (suhler)
 *   -add <map> example
 *   - add stringlength
 *   - allow sequences to have 0 or 1 elements
 *   - make "name" optional in <expr> and <inline>
 *
 * 1.7 05/08/18-17:30:48 (suhler)
 *   Added <mapnames> to do mappings of values between namespaces
 *   added "namespace" parameter to <stringop>
 *
 * 1.6 05/08/17-15:09:06 (suhler)
 *   Added <stringop> to perform miscellaneous string manipulation functions
 *
 * 1.5 05/07/13-10:18:33 (suhler)
 *   doc improvements
 *
 * 1.4 05/06/15-13:24:08 (suhler)
 *   added "<sequence>" to generate a sequence of values
 *
 * 1.3 05/06/13-18:16:28 (suhler)
 *   fixed inline atEnd
 *
 * 1.2 05/06/13-16:20:04 (suhler)
 *   - added <inline>
 *   - doc cleanups
 *
 * 1.2 05/06/13-13:46:08 (Codemgr)
 *   SunPro Code Manager data about conflicts, renames, etc...
 *   Name history : 1 0 sunlabs/MiscTemplate.java
 *
 * 1.1 05/06/13-13:46:07 (suhler)
 *   date and time created 05/06/13 13:46:07 by suhler
 *
 */

package sunlabs.brazil.sunlabs;

import sunlabs.brazil.server.Server;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.Calculator;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.Glob;
import sunlabs.brazil.util.http.HttpUtil;
import sunlabs.brazil.util.regexp.Regexp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * Template for misc string manipulation functions.
 * <dl>
 * <dt>&lt;append name="..." value="..." [delim=".." namespace="..." prepend=true|false]&gt;
 * <dd>Append a value to an existing variable, using an (optional) delimiter
 *     to separate values. If prepend is true, the value is prepended instead.
 * <dt>&lt;push name="..." value="..." namespace="..."&gt;
 * <dd>Push a variable value on a stack, and replace the value with a new one.
 * <dt>&lt;pop name="..." namespace="..."&gt;
 * <dd>Pop a previously pushed value from the stack.
 * <dt>&lt;increment name="..." [incr=n namespace="..."]&gt;
 * <dd>Increment the value of a variable.
 * <dt>&lt;sequence [name=".."] count=".." [start=".." incr=".." delim=".." min=".." max=".."]&gt;
 * <dd>Generate a sequence of values.
 * <dt>&lt;expr value="..." [name="..." namespace="..." format="..."]&gt;
 * <dd>Do an arithmetic expression evaluation on "value".
 * <dt>&lt;inline name="..." [eval="true|false]&gt; ... &lt;/inline&gt;
 * <dd>Place all the text between the "inline" and "/inline" tags
 *     into the variable indicated by "name".
 * <dt>&lt;eval markup="..." [atEnd=true|false]&gt;
 * <dd>Evaluate a variable as if the value was in-line markup.
 * If "atEnd" is specified, the markup is interpolated at the end of the file.
 * <dt> &lt;stringop name="..." [newname="..." range="start,end"
 *      trim="left|right|both"
 *	case="upper|lower|title"
 *      strip="re_char_class"
 *      convert="url|html|..."
 * &gt;
 * <dd>Perform misc. string functions on the value of a named variable.
 * <dt>&lt;mapnames src=src_pattern dst=dst_pattern 
 *	[namespace=dest_namespace]
 *	[range="start,end", trim="left,right", case="upper|lower|title"
 *	 convert=url|html]&gt;
 * <dd> Map a set of variables from one namespace to another, and optionally
 * change the names and process the values.
 * The "src" parameter is a glob pattern that is expected to match
 * the names of one of more properties.  The "dst" parameter (which
 * defaults to the name actually matched) is the new name of the
 * variable that is copied into the specified "namespace" (which defaults
 * to "local").  ${...} substitutions are performed on the value of "dst"
 * separately for each matched variable.  The variables ${1}, ${2} ...
 * ${i} ... ${9} take on the value of the "i"th wildcard match, as
 * in Glob.match().  ${0} refers to the actual name of the matched
 * variable, and ${mapCount} is the index of the current variable
 * being mapped, starting with "1".
 * <p>
 * If "remove" is specified, then the source variables are removed.
 * <p>
 * All the conversion options provided by &lt;stringop&gt; are also available.
 * <p>
 * Example:<br>
 * <pre>
 * &lt;mapnames src=query.pref_* dst="pref_${1}" namepace=${SessionID}.pref&gt;
 * </pre>
 * Will take the query parameters from the current form that refer to 
 * user preferences, and save them in a preferences namespace associated
 * with the current session.
 * <dt>&lt;stringlength name="xxx" value="${...}"&gt;
 * <dd>Compute the length of the string specified in "value" and place it
 * in the property "name".
 * <dt>&lt;random name=xxx [count=xxx start=xxx type=int|hex|alpha&gt;
 * <dd>Generate a random value. if "type=int", the default, then count
 * (defaults to 2) specifies the range of integers from which the value
 * is taken, and "start" (defaults to 1) specifies the minimum value.
 * <p>
 * When "type=hex", then "start" is taken to be a hex value (or 0 if start
 * is not a valid hex value).
 * <p> When "type=alpha", the "start" must contain only a-z, and the result
 * is the alpabetic result, in radix 26.
 * <dt>&lt;subst name=result value="... ${...} .." [recurse|backslashes]&gt;
 * <dd>Do ${..} substitutions to a value.
 * <dt>&lt;replace name=... match= ... [replace=... newname= nocase=true|false all=true|false]&gt;
 * Do regular expression substitutions.  This is a subset of the &lt;extract&gt; template
 * in the SetTemplate class.
 * If replace is not specified, the "match" is deleted, if "newname" is supplied, it is
 * set with the replaced value, otherwise "name" is modified.
 * </dl>
 */

public class MiscTemplate extends Template {
  static Hashtable<String, Convert> converters = null;	/* loadable converters */

  /**
   * Append a value to a property, with an optional delimeter.
   * &lt;append name="..." value="..." [delim="..." namespace="..."]&gt;
   * If "name" is undefined or empty, it is set to "value".
   * The result is placed in the local namespace, unless the "namespace"
   * is used to override it.  Use namespace=${SessionID} to put the result
   * into the current session.
   * The current value is looked up in the request properties, and (if not
   * found), the namespace specified by the "namespace" parameter, if any.
   * Use the ListTemplate for more generic list manipulation.
   */

  public void tag_append(RewriteContext hr) {
    String name = hr.get("name");
    String value = hr.get("value");
    String delim = hr.get("delim", "");

    debug(hr);
    hr.killToken();

    if (name == null || value == null) {
      debug(hr, "Need name and value!");
      return;
    }

    Properties props = hr.getNamespaceProperties();
    String current = hr.request.getProps().getProperty(name);
    if (current == null) {
      current = props.getProperty(name);
    }
    if (current == null || current.length() == 0) {
      props.put(name, value);
    } else if (hr.isTrue("prepend")) {
      props.put(name, value + delim + current);
    } else {
      props.put(name, current + delim + value);
    }
  }

  static Regexp re = new Regexp("(.*[^0-9-])(-?[0-9]*)");
  /**
   * Increment the value of a variable.
   * &lt;incr name=variable [incr=n namespace="..."]&gt;
   * -If undefined or empty, assume it was '0'.<br>
   * -If the value was non numeric, append or modify any numeric
   *  suffix.<br>
   * The result is placed in the local namespace, unless the "namespace"
   * is used to override it.  Use namespace=${SessionID} to put the result
   * into the current session.
   */

  public void tag_increment(RewriteContext hr) {
    String name = hr.get("name");

    debug(hr);
    hr.killToken();

    if (name == null) {
      debug(hr, "need name!");
      return;
    }
    String current = hr.request.getProps().getProperty(name);
    Properties props = hr.getNamespaceProperties();
    props.put(name, incr(current, hr.get("incr")));
  }

  /**
   * Increment a value by some amount, or "1" if amount
   * is invalid
   */

  String incr(String current, String amount) {
    int incr = 1;	// increment amount
    int val = 0;	// value to increment

    try {
      incr = Integer.decode(amount).intValue();
    } catch (Exception e) {
      // ignore
    }

    // no value, set to increment

    if (current == null || current.trim().equals("")) {
      return "" + incr;
    }

    // It's numeric, do the increment

    try {
      val = Integer.decode(current).intValue();
      return "" + (val + incr);
    } catch (Exception e) {
      // ignore
    }

    // look for a trailing number to increment

    String[] subs = new String[3];
    re.match(current, subs);

    try {
      val = Integer.decode(subs[2]).intValue();
    } catch (Exception e) {
      // ignore
    }
    val += incr;
    if (val != 0) {
      return subs[1] + val;
    } else {
      return subs[1];
    }
  }

  /**
   * Generate a (mostly) numeric sequence.
   * &lt;sequence name="..." count="..."
   *   [start="..." incr="..." delim=".." min=".." max=".."]&gt;
   * <dl>
   * <dt>name
   * <dd>Where to put the result.  Defaults to the local namespace, 
   * but may be overridden with "namespace=".
   * <dt>count
   * <dd>the number of values to generate
   * <dt>start
   * <dd>The start value.  Defaults to "1".  If non numeric, 
   * any trailing digits are incremented, if present, or appended
   * otherwise.
   * <dd>The increment value.  Defaults to "1" if the value is not
   * an integer.
   * <dt>start
   * <dd>The starting value
   * <dt>delim
   * <dd>The delimiter between values.  defaults to " ".
   * <dt>min
   * <dt>max
   * <dd>If supplied this limits "count" to be at least "min" and
   * no more than "max".
   * </dl>
   */

  public void tag_sequence(RewriteContext hr) {
    String name = hr.get("name");
    String delim = hr.get("delim", " ");
    String start = hr.get("start");
    String incr = hr.get("incr");
    int min = Format.stringToInt(hr.get("min"), -1);
    int max = Format.stringToInt(hr.get("max"), -1);
    int count = Format.stringToInt(hr.get("count"), -1);


    debug(hr);
    hr.killToken();

    if (min >= 0 && min > count) {
      count = min;
    }

    if (max >= 1 && max >= min && count > max) {
      count = max;
    }

    if (count < 0 ) {
      debug(hr, "need count ge 0");
      return;
    }
    StringBuffer sb = new StringBuffer();
    if (start == null) {
      start = "1";
    }
    for (int i=0; i < count; i++) {
      sb.append(i == 0 ? "" : delim).append(start);
      start = incr(start, incr);
    }
    if (name == null) {
      hr.append(sb.toString());
    } else {
      hr.getNamespaceProperties().put(name, sb.toString());
    }
  }

  /**
   * Set a variable to the result of an arithmetic expression
   * &lt;expr [name=nnn] value="expr" [format="..." ] [namespace="..."]&gt;
   * sets name to "NaN" if the expression was invalid.
   * The result is placed in the local namespace, unless the "namespace"
   * is used to override it.  Use namespace=${SessionID} to put the result
   * into the current session.
   * <p>
   * If "name" is omitted, the result is placed in the document.
   * If "format" is used, it is java.text.DecimalFormat format specifier.
   * Briefly:
   * <dl>
   * <dt>$<dd>currency
   * <dt>%<dd>percent
   * <dt>.<dd>decimal point
   * <dt>,<dd>group separator, as in: 1,000
   * <dt>#<dd>a digit, if required, blank otherwise
   * <dd>0<dd>a digit, if required, 0 otherwise.
   * </dl>
   * Example: <code>format="$#,##0.00"</code> would be typical for
   * expressing monetary values.  In addition, "truncate=tru" can be use
   * to force truncation instead of rounding.
   */

  public void tag_expr(RewriteContext hr) {
    String name = hr.get("name");
    String value = hr.get("value");
    String format = hr.get("format");
    debug(hr);
    hr.killToken();

    Calculator calc = new Calculator(hr.request.getProps());
    String result = "NaN";
    try {
      result = calc.getValue(value);
      if (format != null) {
        DecimalFormat decimalFormat = new DecimalFormat(format);
        if (hr.isTrue("truncate")) {
          decimalFormat.setRoundingMode(RoundingMode.FLOOR);
        }
        result=decimalFormat.format(Double.valueOf(result).doubleValue());
      }
    } catch (ArithmeticException e) {
      debug(hr, "Invalid expression: " + e);
    } catch (NumberFormatException e) {
      debug(hr, "Invalid expression " + e);
    } catch (IllegalArgumentException e) {
      debug(hr, "Invalid format " + e);
    }
    if (name == null) {
      hr.append(result);
    } else {
      hr.getNamespaceProperties().put(name, result);
    }
  }

  /**
   * Evaluate the contents of a variable as a template, as if
   * it was "inline" here.
   * &lt;eval markup="..." [atEnd=true|false]&gt;
   */

  public void tag_eval(RewriteContext hr) {
    String markup = hr.get("markup");
    boolean atEnd = hr.isTrue("atEnd");
    debug(hr);
    hr.killToken();

    if (markup == null) {
      debug(hr, "need markup!");
      return;
    }
    String rest = hr.lex.rest();
    if (rest != null) {
      hr.lex.replace(atEnd ? rest + markup : markup + rest);
    } else {
      hr.lex.replace(markup);
    }
  }

  /**
   * Do ${...} substitutions in the current context.
   * <ul>
   * <li>&lt;subst name=foo value="..." [recurse|backslashes]&gt;
   * <li>name: the name of the property to contain the substituted results
   *     if not provided, the results are emitted inline.
   * <li>value: The value to substitute
   * <li>namespace: namespace to put the result in (defaults to "local").
   * <li>recurse:   If true, recurse until no ${..}'s remain.
   * <li>backslashes: If true do '\' substututions as well.
   * <li>params: If true all attributes are made available for
   * substitution
   * </ul>
   * This should be part of the SetTemplate.
   */

  public void tag_subst(RewriteContext hr) {
    String name = hr.get("name");
    String value = hr.get("value", "");
    boolean recurse = hr.isTrue("recurse");
    boolean backslashes = hr.isTrue("backslashes");
    boolean params = hr.isTrue("params");
    String result = null;
    int max = 10;	// arbitrary

    debug(hr);
    hr.killToken();
    Properties props = params ? new RewriteContext.AttributeProps(hr) : 
      hr.request.getProps();

    while(max-- > 0) {
      result = Format.subst(props, value, !backslashes);
      if (!recurse || result.equals(value)) {
        break;
      }
      value = result;
    }

    debug(hr, "mapping:" + value + "=>" + result);
    if (name == null) {
      hr.append(result);
    } else {
      hr.getNamespaceProperties().put(name, result);
    }
  }

  // value stacks

  Stacks stack = new Stacks(); // Should use the session manager

  @Override
  public boolean init(RewriteContext hr) {
    hr.addClosingTag("inline");
    stack.populate(hr.request.getProps());
    return super.init(hr);
  }

  /**
   * Treat a variable as a stack, and push a value onto it.
   * &lt;push name="..." value="..." [clear=true|false namespace="..."]&gt;
   * For more generic list manipulation, use the ListTemplate.
   * <p>
   * The result is placed in the local namespace, unless the "namespace"
   * is used to override it.  Use namespace=${SessionID} to put the result
   * into the current session.
   */

  public void tag_push(RewriteContext hr) {
    String name = hr.get("name");
    String value = hr.get("value");
    debug(hr);
    hr.killToken();

    if (name == null || value == null) {
      debug(hr, "need name and value!");
      return;
    }

    if (stack != null && hr.isTrue("clear")) {
      stack.clear(name);
    }
    String old = hr.request.getProps().getProperty(name);
    if (old != null) {
      stack.push(name, old);
    }
    // Properties props = hr.getNamespaceProperties();
    hr.request.getProps().put(name, value);
  }

  /**
   * Treat a variable as a stack, and pop a value from it.
   * &lt;pop name="..." [clear=true|false namespace="..."]&gt;
   * The result is placed in the local namespace, unless the "namespace"
   * is used to override it.  Use namespace=${SessionID} to put the result
   * into the current session.
   */

  public void tag_pop(RewriteContext hr) {
    String name = hr.get("name");
    debug(hr);
    hr.killToken();

    if (name == null) {
      debug(hr, "need name!");
      return;
    }
    Properties props = hr.getNamespaceProperties();
    String result = stack.pop(name);
    if (result == null) {
      props.remove(name);
    } else {
      props.put(name, result);
    }
    if (hr.isTrue("clear")) {
      stack.clear(name);
    }
  }

  /**
   * Set a variable to all the markup 'till the /inline tag.
   * &lt;inline name="..." [eval=true|false] [append=true|false]&gt;
   * ...
   * &lt;/inline&gt;
   * <p>
   * If "eval" is true, then ${..} substitutions are performed before
   * assigning the markup the the named variable. If "esc" is also true, 
   * the \X sequences will be replaced as well.
   * If "append" is true and "name" is specified,
   *  then the markup is appended to the current contents of "name".
   * If no "name" is specified, the markup is output as-is, after
   * ${..} substitutions (e.g. eval=true is implied).
   * <li>params: If true (and eval is true) all attributes are made available for
   * substitution
   */

  public void tag_inline(RewriteContext hr) {
    String name = hr.get("name");
    boolean eval = hr.isTrue("eval");
    boolean append =  hr.isTrue("append");
    boolean noesc = !hr.isTrue("esc");
    boolean params = hr.isTrue("params");

    debug(hr);
    hr.killToken();

    if (name == null) {
      eval = true;
    }
    boolean was = hr.accumulate(false);
    hr.nextToken();
    String body = hr.getBody().trim();
    hr.accumulate(was);
    hr.nextToken();	// eat the </inline>
    hr.killToken();

    if (eval) {
      body = Format.subst(params ? new RewriteContext.AttributeProps(hr) :
        hr.request.getProps(), body, noesc);
    }
    if (name == null) {
      hr.append(body);
    } else {
      Properties props = hr.getNamespaceProperties();
      String result = append ? props.getProperty(name, "") + body : body;
      props.put(name, result);
      debug(hr, body);
    }
  }

  /**
   * String manipulation functions.
   * Any of the following options are supported.  If more than one
   * of "range", "trim", "case", "strip", or "convert" is specified, they
   * are performed in the order listed below.
   * The value named by the "name" attribute is modified, and the
   * result is placed in the local namespace, using the same name,
   * unless "newname" is specified, in which case "newname" is used instead.
   * <p>
   * The "namespace" attribute can be used to alter the namespace
   * to put the result into, in which case the "sessionTable" configuration
   * parameter can be used to alter the namespace class, which defaults
   * to the template handler's (or filter's) prefix.
   * <dl>
   * <dt>&lt;stringop name=xxx range=x,y
   *		[newname=xxx namespace=xxx]&gt;<br>
   * <dt>&lt;stringop name=xxx range=x 
   *		[newname=xxx namespace=xxx]&gt;<br>
   * <dd> Do a substring from character position 'x' (starting at 0)
   *      up to but not including character position 'y'.  Negative
   *      values count from the end of the string. If no "y" is specified, 
   *      then the end of the string is assumed.
   * <dt>&lt;stringop name=xxx trim=left|right|both 
   *		[newname=xxx namespace=xxx]&gt;
   * <dd> Trim whitespace from the string. "trim=true" is equivalent
   *      to "both".
   * <dt>&lt;stringop name=xxx case=lower|upper|title 
   *		[newname=xxx namespace=xxx]&gt;
   * <dd>Do case conversions. "title" causes the entire string to
   * <dt>&lt;stringop name=xxx strip=list 
   *		[newname=xxx namespace=xxx]&gt;
   * <dd>Remove all the characters from the string contained in "list",
   * which is a regular expression character class. The characters
   * '^', '-', or ']' are treated special in regular expression character
   * classes. See  {@link sunlabs.brazil.util.regexp.Regexp}.
   * <dt>&lt;stringop name=xxx convert=html|url|javascript|hidetags|sql|other
   *		[newname=xxx namespace=xxx]&gt;
   * <dd>The string is converted exactly the same as the "convert"
   * option of the "get" tag in the SetTemplate() for "html" or "url".  
   * In addition "hidetags" can be used to escape '&gt;' and '&lt;' only
   * with &amp;lt; and &amp;gt; respectively, and "javascript"
   * can be used to put '\'s in front of (") and (\) to allow
   * variables to be used as properly escaped javascript strings.
   * <p>
   * For any other string, a String converter is dynamically loaded
   * to do the conversion, by looking first for a "other.class" attribute, 
   * then an "other.class" property of this template.  That class is
   * expected to implemnt the "Convert" interface.  Class loading is 
   * done on first reference to a converter.
   * </dl>
   */

  public void tag_stringop(RewriteContext hr) {
    String name = hr.get("name");
    debug(hr);
    hr.killToken();
    if (name == null) {
      debug(hr, "name not specified");
      return;
    }
    String value = hr.request.getProps().getProperty(name);
    if (value == null) {
      debug(hr, "No value for: " + name);
      return;
    }

    value = trim(value, hr.get("trim"));
    value = range(hr, value, hr.get("range"));
    value = recase(value, hr.get("case"));
    value = strip(value, hr.get("strip"));
    value = convert(hr, hr.get("convert"), value);
    String result = hr.get("newname");
    if (result == null) {
      result = name;
    }
    Properties props = hr.getNamespaceProperties();
    props.put(result, value);
  }

  /**
   * Compute string length from "value" , return in "name"
   */

  public void tag_stringlength(RewriteContext hr) {
    String name = hr.get("name");
    String value = hr.get("value");
    debug(hr);
    hr.killToken();
    if (name==null || value == null) {
      debug(hr, "Name and value attributed required");
      return;
    }
    hr.request.getProps().put(name, "" + value.length());
  }

  /**
   * Trim the string "value".
   * How is "left" "right" "both" or "true".
   */

  String trim(String value, String how) {
    if (how == null) {
      return value;
    }
    if ("left".equals(how)) {
      int i;
      for(i = 0 ; i < value.length(); i++) {
        if (!Character.isWhitespace(value.charAt(i))) {
          break;
        }
      }
      return value.substring(i);
    } else if ("right".equals(how)) {
      int i;
      for(i = value.length()-1; i >= 0; i--) {
        if (!Character.isWhitespace(value.charAt(i))) {
          break;
        }
      }
      return value.substring(0,i+1);
    } else if ("both".equals(how) || Format.isTrue(how)) {
      return value.trim();
    } else {
      return value;
    }
  }

  /**
   * Change the case of a String
   */

  String recase(String value, String how) {
    if (how == null) {
      return value;
    }
    if (how.startsWith("to")) {
      how = how.substring(2);
    }
    if (how.equals("lower")) {
      return value.toLowerCase();
    } else if (how.equals("upper")) {
      return value.toUpperCase();
    } else if (how.equals("title")) {
      return titleCase(value);
    } else {
      return value;
    }
  }

  /**
   * Remove all characters from the supplied list.
   * @param value		The value to strip
   * @param chars		The regexp character class to strip
   */

  String strip(String value, String chars) {
    if (chars == null) {
      return value;
    }
    return  new Regexp("[" + chars + "]").subAll(value, "");
  }

  /**
   * Do title casing. must not be null.
   */

  String titleCase(String value) {
    boolean startWord = true;
    char result[] = new char[value.length()];
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      boolean space = Character.isWhitespace(c);
      if (space) {
        startWord = true;
        result[i] = c;
      } else if (startWord) {
        result[i] = Character.toTitleCase(c);
        startWord = false;
      } else {
        result[i] = Character.toLowerCase(c);
      }
    }
    return new String(result);
  }

  /**
   * Do a substring:
   * x,y	from 'x' to 'y'
   * x,-y	from 'x' to end+'y'
   * x	from 'x' to end
   * -x	from end+x to end
   */

  String range(RewriteContext hr, String value, String how) {
    try {
      return range(value, how);
    } catch (NumberFormatException e) {
      debug(hr, "Invalid range: " + how);
      return value;
    }
  }
  
  public static String range(String value, String how) throws NumberFormatException {
    if (how == null) {
      return value;
    }
    int from = 0;
    int to = 0;
    int len = value.length();
    int indx = how.indexOf(",");
    if (indx > 0) {
      from = Integer.decode(how.substring(0,indx)).intValue();
      to = Integer.decode(how.substring(indx+1)).intValue();
    } else {
      from = Integer.decode(how).intValue();
      to = len;
    }

    if (to > len) {
      to = len;
    } else if (to < 0) {
      to += len;
    } else if (to == 0) {
      return value;
    }

    if (from < 0) {
      from += len;
    } else if (from >= to) {
      from = to;
    }
    if (from < 0) {
      from = 0;
    }
    return value.substring(from, to);
  }

  /**
   * Do web related conversions.
   * how is "url" or "html" or dynamically loadable thingy.
   * XXX we should convert the built-ins to be dynamically loaded as well
   */

  String convert(RewriteContext hr, String how, String value) {
    if ("html".equals(how)) {
      return HttpUtil.htmlEncode(value);
    } else if ("hidetags".equals(how)) {
      return escapeTags(value);
    } else if ("sql".equals(how)) {
      return escapeSQL(value);
    } else if ("javascript".equals(how)) {
      return escapeJS(value);
    } else if ("url".equals(how)) {
      return HttpUtil.urlEncode(value);
    } else if (how != null) {
      Convert cvt = getConverter(hr, how);
      if (cvt != null) {
        value = cvt.convert(hr, how, value);
      }
    }
    return value;
  }

  /**
   * Escape '&lt;' and '&gt;' only.
   */
  public static String
  escapeTags(String s) {
    StringBuffer sb = new StringBuffer();
    StringTokenizer st = new StringTokenizer(s, "<>", true);
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (token.equals(">")) {
        sb.append("&gt;");
      } else if (token.equals("<")) {
        sb.append("&lt;");
      } else {
        sb.append(token);
      }
    }
    return sb.toString();
  }

  /**
   * Protect string for javascript
   */
  public static String escapeJS(String s) {
    StringBuffer sb = new StringBuffer();
    StringTokenizer st = new StringTokenizer(s, "\"\\", true);
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (token.equals("\\") || token.equals("\"")) {
        sb.append("\\");
      }
      sb.append(token);
    }
    return sb.toString();
  }
  
  /**
   * Protect string for SQL (double the ')
   */
  
  public static String escapeSQL(String s) {
    StringBuffer sb = new StringBuffer();
    StringTokenizer st = new StringTokenizer(s, "'", true);
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (token.equals("'")) {
        sb.append("'");
      }
      sb.append(token);
    }
    return sb.toString();
  }
  
  /**
   *  Map a set of properties from one namespace to another, 
   *  and (optionally) change their names and values.
   *  &lt;mapnames src=glob-pattern namespace=dst_space
   *            dst=dst-pattern [stringops options] [remove=true|false]&gt;
   */

  public void tag_mapnames(RewriteContext hr) {
    String src = hr.get("src");
    String dst = hr.get("dst", false);  // don't substitute yet
    boolean shouldProcess = 
      hr.get("trim") != null ||
      hr.get("range") != null ||
      hr.get("convert") != null ||
      hr.get("strip") != null  ||
      hr.get("case") != null;
    boolean delSrc = hr.isTrue("remove");
    debug(hr);
    hr.killToken();

    if (src==null) {
      debug(hr, "src not specified");
      return;
    }

    Enumeration<String> names = hr.request.getProps().propertyNames(src);
    Properties dstProps = hr.getNamespaceProperties();
    int count = 0;
    boolean didDst = false;
    while(names.hasMoreElements()) {
      String name = names.nextElement();
      Properties props = new GlobProperties(hr.request.getProps(), src, name,
              ("" + ++count)); 
      String newName;
      if (dst != null) {
        newName = Format.subst(props, dst); 
      } else if (delSrc) {
        hr.request.getProps().removeProperty(name);
        continue;
      } else {
        newName = name;
      }	
      String value = hr.request.getProps().getProperty(name);
      if (shouldProcess) {
        value = trim(value, hr.get("trim"));
        value = range(hr, value, hr.get("range"));
        value = recase(value, hr.get("case"));
        value = strip(value, hr.get("strip"));
        value = convert(hr, hr.get("convert"), value);
      }
      dstProps.put(newName, value);
      didDst=true;
      if (delSrc) {
        hr.request.getProps().removeProperty(name);
      }
    }

    /*
     * If we copied data into a namespace that used to be
     * empty but is now populated, we have to tell the 
     * SetTemplate to actually add it to the chain.
     * There should be a better way of template classes operating
     * on the same page to share state than this XXX.
     */

    if (didDst) {
      Template t = hr.templateFromTag("import");
      if (t != null && t instanceof sunlabs.brazil.template.SetTemplate) {
        ((sunlabs.brazil.template.SetTemplate) t).doImport(hr);
      }
    }

    debug(hr, count + " names mapped");
  }

  /**
   * Generate a random value from "start" with "count" possibilities.
   * If "hex", then generate hex values.
   * If "alpha" generate alphanumeric values
   */

  static Random rand = new Random();
  public void tag_random(RewriteContext hr) {
    String name = hr.get("name");
    String type = hr.get("type", "int"); // "int", "hex" or "alpha"

    int count = 2;
    try {
      count = Integer.decode(hr.get("count")).intValue();
    } catch (Exception e) {
      // ignore
    }
    int value = Math.abs(rand.nextInt() % count);
    String result;

    if (type.equals("hex")) {
      int start = 0;
      try {
        start = Integer.parseInt(hr.get("start", "0").toLowerCase(), 16);
      } catch (Exception e) {}
      result = Integer.toString(start+value, 16);
    } else if (type.equals("alpha")) {
      result = toAlpha(value + fromAlpha(hr.get("start", "a")));
    } else {
      int start = 1;
      try {
        start = Integer.decode(hr.get("start")).intValue();
      } catch (Exception e) {}
      result = "" + (start + value);
    }
    debug(hr);
    hr.killToken();
    if (name == null) {
      hr.append(result);
    } else {
      hr.getNamespaceProperties().put(name, result);
    }
  }

  /**
   * Regular expression replacement.  This is a subset of
   * the &lt;extract&gt; tag in the BSLTemplate
   * Usage:
   *  &lt;replace name=... match= ... replace=... [newname=... nocase=true|false all=true|false&gt;
   */

  public void tag_replace(RewriteContext hr) {
    Regexp replaceRe = null;
    String result;
    String name = hr.get("name");
    String newname = hr.get("newname", name);
    String replace = hr.get("replace", "");

    debug(hr);
    hr.killToken();
    if (name == null) {
      debug(hr, "missing name");
      return;
    }
    try {
      replaceRe = new Regexp(hr.get("match"), hr.isTrue("nocase"));
    } catch (IllegalArgumentException e) {
      debug(hr, "Invalid match param");
      return;
    }
    String value = hr.request.getProps().getProperty(name);
    if (hr.isTrue("all")) {
      result = replaceRe.subAll(value, replace);
    } else {
      result = replaceRe.sub(value, replace);
    }
    if (result == null) {
      hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix,
              "No match for: " + hr.get("match"));
      result = value;
    }
    hr.getNamespaceProperties().put(newname, result);
  }

  /**
   * Convert a positive integer to alphabetic.
   */

  static final String abc = "abcdefghijklmnopqrstuvwxyz";
  public static String toAlpha(int i) {
    char result = abc.charAt(i%26);
    if (i > 25) {
      return toAlpha(i/26) + result;
    } else {
      return "" + result;
    }
  }

  /**
   * Convert an alpha-only string to an integer. no error checking!
   */

  public static int fromAlpha(String s) {
    s = s.toLowerCase();
    int result = 0;
    for (int i = 0; i < s.length(); i++) {
      int diff = (0xff&s.charAt(i)) - (0xff&'a');
      if (diff < 0 || diff > 25) diff=0;
      result = (result * 26) + diff;
    }
    return result;
  }

  /**
   * Special version of a properties that uses the sub expresons
   * of the supplied glob pattern and name to define the keys 1-9.
   * The value "0" takes the supplied default.
   */

  public static class GlobProperties extends Properties {
    Properties realProps;
    String dflt;
    String name;
    String subStr[] = new String[9];

    public GlobProperties(Properties props, String pattern, String name,
            String dflt) {
      realProps = props;
      this.dflt = dflt;
      this.name = name;
      Glob.match(pattern, name, subStr);
    }
    @Override
    public String getProperty(String key) {
      int index;
      if (key.equals("0")) {
        return name;
      } else if (key.equals("mapCount")) {
        return dflt;
      } else if (key.length() == 1 &&
              (index = "123456789".indexOf(key)) >= 0) {
        String result = subStr[index];
        return result == null ? "" : result;
      } else {
        return realProps.getProperty(key);
      }
    }
  }

  /**
   * Keep a table of stacks for values.
   * When push/pop are fixed to integrate with the session manager,
   * this will save stack values between server restarts.
   */

  static class Stacks /* XXX implements PropertiesCacheManager.Saveable */{
    Hashtable<String, Stack<String>> table=null;	// where to keep the stack of values

    Stacks() {}

    void init() {
      if (table == null) {
        table = new Hashtable<String, Stack<String>>(13);
      }
    }

    /**
     * Push a value onto the stack
     */

    void push(String name, String value) {
      init();
      Stack<String> current = table.get(name);
      if (current == null) {
        current = new Stack<String>();
        table.put(name, current);
      }
      current.push(value);
    }


    /**
     * Pop a value from the stack.
     * Return null if nothing to pop
     */

    String pop(String name) {
      Stack<String> current;
      if (table == null || (current = table.get(name)) == null) {
        return null;
      }
      String result = current.pop();
      if (current.empty()) {
        table.remove(name);
      }
      return result;
    }

    void clear(String name) {
      table.remove(name);
    }

    /**
     * Populate a dictionary with the current values of the stacks
     */

    void populate(Dictionary dict) {
      if (table == null) {
        return;
      }
      for(Enumeration<String> e = table.keys(); e.hasMoreElements();) {
        String name = e.nextElement();
        String value =  table.get(name).peek();
        dict.put(name, value);
      }
    }

    // the saveable interface
    static final String DELIM = "#";  // never used in names

    /**
     * load up the stacks
     */

    public void load(InputStream in) throws IOException {
      Properties p = new Properties();
      p.load(in);
      StringTokenizer names = new StringTokenizer(
              p.getProperty(DELIM + "names",DELIM));
      StringTokenizer counts = new StringTokenizer(
              p.getProperty(DELIM + "counts",DELIM));
      while (names.hasMoreTokens()) {
        String name = names.nextToken();
        int count = 0;
        try {
          count = Integer.decode(counts.nextToken()).intValue();
        } catch (Exception e) {}
        for(int i=count-1;i>=0;i--) {
          String value = p.getProperty(name + DELIM + i);
          push(name, value);
        }
      }
    }

    /**
     * Turn our table into a properties file.
     * XXX this required well behaved names (e.g. no DELIM's)
     * @throws IOException 
     */

    @SuppressWarnings("deprecation")
    public void save(OutputStream out, String header) throws IOException {
      if (isEmpty()) {
        return;
      }
      String delim = "";
      StringBuffer names = new StringBuffer();
      StringBuffer counts = new StringBuffer();
      Properties p = new Properties();
      for(Enumeration<String> e = table.keys(); e.hasMoreElements(); ) {
        String name =  e.nextElement();
        Stack<String> stack = table.get(name);
        names.append(delim).append(name);
        counts.append(delim).append(stack.size());
        for (int i=0; i < stack.size(); i++) {
          p.put(name + delim + i, stack.elementAt(i));
        }
        delim = DELIM;
      }
      p.put(DELIM + "names", names.toString());
      p.put(DELIM + "counts", counts.toString());
      p.save(out, header);
    }

    public boolean isEmpty() {
      return (table != null && table.size() > 0);
    }
  }

  /**
   * Register a string converter.
   * Look for "name".class, first as a tag attribute, 
   * then as prefix.name.class.
   */

  Convert getConverter(RewriteContext hr, String name) {
    if (converters == null) {
      converters = new Hashtable<String, Convert>();
    }
    if (converters.containsKey(name)) {
      return converters.get(name);
    } else {
      Convert cvt = null;
      try {
        Class<? extends Convert> cls = 
            Class.forName(hr.get(name + ".class", null)).asSubclass(Convert.class);
        cvt = cls.newInstance();
        converters.put(name, cvt);
        hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix,
                "Installing converter: " + name);
      } catch (Exception e) {
        hr.request.log(Server.LOG_WARNING, hr.prefix,
                "Can't install converter: " + name + " (" + e + ")");
        // e.printStackTrace();
        // This keeps us from retrying the class load
        cvt = new NullConverter();
      }
      return cvt;
    }
  }

  /**
   * Make a null converter.
   */

  static class NullConverter implements Convert {
    @Override
    public String convert(RewriteContext hr, String name, String value) {
      hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix,
              "Ignoring converter: " + name);
      return value;
    }
  }

  /**
   * Interface for dynamically loaded String converters.
   * &lt;stringop convert=xxx ...&gt; is used to look up a converter
   * whose name matches "xxx". and the convert interface is called.
   */

  public interface Convert {
    /**
     * Convert a string to a string:
     * @param hr	Rewrite context at the &lt;stringop&gt; tag.
     * @param name		The converter name
     * @param value	The converted string
     */
    public String convert(RewriteContext hr, String name, String value);
  }
}