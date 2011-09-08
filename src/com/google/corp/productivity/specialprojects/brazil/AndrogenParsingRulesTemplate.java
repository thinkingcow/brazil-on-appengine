// Copyright 2010-2011 Google Inc. All Rights Reserved.

package com.google.corp.productivity.specialprojects.brazil;

import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.Format;
import sunlabs.brazil.util.LexHTML;

/**
 * Change the Lexor to allow '>'s inside of quoted attribute values.
 * Rules:
 * - " or ' may be used to quote strings
 * - \X turns off special bahavior for ', "", and >
 * Bugs: Although this finds the '>' according to the new rules, there is no
 * garrantee the attribute values will be parsed properly.
 * @author suhler@google.com (Stephen Uhler)
 *
 */

public class AndrogenParsingRulesTemplate extends Template {
  
  @Override
  public boolean init(RewriteContext hr) {
    hr.addClosingTag("androgen");
    hr.lex = new Lex(hr.lex.rest());
    return true;
  }
  
  public void tag_androgen(RewriteContext hr) {
    debug(hr);
    boolean eval = hr.isTrue("eval");
    boolean was = hr.accumulate(false);
    hr.nextToken();
    String script = hr.getBody();
    if (eval) {
        script =  Format.subst(hr.request.getProps(), script, true);
    }
    hr.append(script);
    hr.nextToken(); // skip over closing tag
    hr.accumulate(was);
  }
  
  public void tag_slash_androgen(RewriteContext hr) {
    hr.killToken();
  }
  
  /**
   * Version of our lexor that admits >'s in attribute values
   * @author suhler@google.com (Stephen Uhler)
   */
  static class Lex extends LexHTML {
    Lex(String s) {
      super(s);
    }
    
    @Override
    protected int findClose(int start) {
      int end = findGt(start);
      // System.err.println("[" + str.substring(start, end) + "]");
      return end;
    }
    
    /**
     * Look for the closing > in a tag.  Allow
     * >'s in quoted strings and protected by \'s.
     */
    final static char NO_QUOTE = 0;
    private int findGt(int start) {
      int inQuote = NO_QUOTE;
      boolean inEsc = false;
      for (int i = start; i < strEnd; i++) {
        int c = str.charAt(i);
        if (c == '\\') {
          inEsc = !inEsc;
          continue;
        }
        if (c == '>' && inQuote == NO_QUOTE && !inEsc) {
          return i;
        } else if (c == inQuote && !inEsc) {
          inQuote = NO_QUOTE;
        } else if ((c == '\"' || c == '\'') && !inEsc && inQuote == NO_QUOTE) {
          inQuote = c;
        }
        inEsc = false;
      }
      return -1;
    }
  }
}