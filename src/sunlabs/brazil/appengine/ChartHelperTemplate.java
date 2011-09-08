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

import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.ClockScan;
import sunlabs.brazil.util.Glob;

import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * Helper for turning numeric data into Google chart format.
 * This will expand as the need arises.
 * See: http://code.google.com/apis/chart/docs/data_formats.html
 * @author suhler@google.com
 *
 */
public class ChartHelperTemplate extends Template {
  public final static String map = 
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.-";
  private final static char[] charMap = map.toCharArray();
  
  /**
   * <vis
   *   namespace=[namespace for experiment]
   *   key=[name of variable to extract data for]
   *   from=[starting time (ms since epoch)]
   *   to=[ending time]
   *   />
   *   returns:
   *     key.[xy]min    The minimum value found
   *     key.[xy]max    The max value found
   *     key.[xy]       The vis chart data
   *     key.count      The number of items
   *     
   *     This implies a format of a namespace which is:
   *     [key].[timestamp]=[numeric value]
   *     
   *   - Gather the data values
   *   - Sort by timestamp
   *   - generate string results
   */
  @SuppressWarnings("unchecked")
  public void tag_vis(RewriteContext hr) {
    debug(hr);
    String key = hr.get("key");
    
    if (key==null) {
      debug(hr, "Missing key");
      return;
    }
    String pattern = key + ".*";
    long from = when(hr.get("from"));
    long to = when(hr.get("to"));
    
    double minY =  Double.MAX_VALUE;
    double maxY = -Double.MAX_VALUE;
    
    Vector<Pair> values = new Vector<Pair>();
    Properties props = hr.getNamespaceProperties();
    if (props == null) {
      debug(hr, "no data");
      return;
    }
    props.list(System.err);

    // collect the data
    
    Enumeration names = props.propertyNames();
    String[] subMatch = new String[1];
    while (names.hasMoreElements()) {
      String value = (String) names.nextElement();
      if (!Glob.match(pattern, value, subMatch)) {
        // System.err.println("Skipping" + pattern + "," + value);
        continue;
      }
      try {
        long x = Long.parseLong(subMatch[0]);
        if (x < from || (x > to && to != 0)) {
          System.err.println("Out of range: " + from + "<" + x + "<" + to);
          continue;
        }
        double y = Double.parseDouble(props.getProperty(value));
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
        values.add(new Pair(x, y));
      } catch (NumberFormatException e) {
        debug(hr, "oops: " + e);
        // ignore invalid data
      }
    }
   
    // compute the values
    
    Collections.sort(values);
    // System.err.println("max=" + max + " min=" + min);
    // System.err.println(values);
    long minX = values.firstElement().time;
    long maxX = values.lastElement().time;
    // hr.append("range: " + minX + "," + maxX + "<br>");
    double scaleX = 4095 / (double) (maxX - minX);
    double scaleY = 4095 / (maxY - minY);
    hr.request.getProps().put(key + ".xmin", "" + minX);
    hr.request.getProps().put(key + ".xmax", "" + maxX);
    hr.request.getProps().put(key + ".ymin", "" + minY);
    hr.request.getProps().put(key + ".ymax", "" + maxY);
    hr.request.getProps().put(key + ".count", "" + values.size());

    StringBuffer compressedX = new StringBuffer();
    StringBuffer compressedY = new StringBuffer();
    for (Pair p : values) {
      indexToString(compressedX,(int) (scaleX * (p.time - minX)));
      indexToString(compressedY,(int) (scaleY * (p.value - minY)));
    }
    hr.request.getProps().put(key + ".x", compressedX.toString());
    hr.request.getProps().put(key + ".y", compressedY.toString());
  }
  
  void indexToString(StringBuffer sb, int index) {
    sb.append(charMap[(index >> 6) & 0x3F]).append(charMap[index & 0x3F]);
  }
  
  /**
   * Turn a date string into ms since epoch timestamp.
   * @param dateString  Something that looks like a date and or time
   * @return 0 if invalid date
   */
  
  private long when(String dateString) {
    if (dateString == null) {
      return 0L;
    }
    Date date = ClockScan.GetDate(dateString, null, null);
    if (date != null) {
      return date.getTime();
    } else {
      return 0L;
    }
  }
  
  static class Pair implements Comparable<Pair> {
    public long time;
    public double value;
    
    public Pair(long time, double value) {
      this.time = time;
      this.value = value;
    }

    @Override
    public int compareTo(Pair pair) {
      long diff = time - pair.time;
      return (diff != 0) ? (diff > 0 ? 1 : -1) : 0;
    }
    
    @Override
    public String toString() {
      return time + "," + value;
    }
  }
}