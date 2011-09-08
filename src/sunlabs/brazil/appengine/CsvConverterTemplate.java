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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvConverterTemplate extends Template {
  
  /**
   * Extract a csv format file into a set of properties.  By default
   * all the extracted data are named as follows:
   *  prefix.[column].[row]
   *  prefix.columns=[column count]
   *  prefix.rows=[row count]
   * The number of elements on the first row is used to determine the number of
   * columns.
   *  
   * Attributes
   *   name     The name of the property containing the csv data
   *   prefix   The prefix to prepend the properties to (defaults to name)
   *   headers=true  
   *            Use header names for [column] instead of 0,1,2 ...
   *   key=[column name or 0 indexed column number
   *            Use the specified column name as a "key" and use its value instead
   *            of 0, 1, 2 .... for [row].  If the column name doesn't exit, key is ignored.
   *            If key is not unique, only the last row with a given key value is available  
   * Experimental
   *   chart="col, ... ,col"  
   *            Return google chart visualization data for this column.
   *            prefix.vis.[col].min, prefix.vis.[col].max prefix.vis.[col].data 
   *   labels="col, ... ,col"
   *            Return google chart label data for this col
   *            prefix.vis.[col].labels=val1|val2 ...
   * @param hr
   */
  public void tag_extractcsv(RewriteContext hr) {
    String name=hr.get("name");
    String prefix=hr.get("prefix", name);
    boolean useHeader = hr.isTrue("headers");
    String key = hr.get("key");
    
    debug(hr);
    hr.killToken();
    
    if (name == null) {
      debug(hr, "Missing name attribute");
      return;
    }
    String content = hr.request.getProps().getProperty(name);
    
    if (content == null) {
      debug(hr, "no content");
      return;
    }
    
    if (!prefix.endsWith(".")) {
      prefix += "." ;
    }
    
    Properties props = hr.getNamespaceProperties();
    Table table = new Table(content);
    VisHelper vis = new VisHelper(hr.get("chart"), hr.get("labels"));
    // hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, table.toString());
    
    int keyColumn = getKeyColumn(key, table);

    if (table.columns > 0) {
      int index = 0;
      for (String entry : table.entries) {
        int col = index % table.columns;
        int row = index / table.columns;
        index++;
        if (entry == null || col == keyColumn || (useHeader && row == 0)) {
          continue;
        }
        
        String columnName = useHeader ? table.entries.get(col) : "" + col;
        columnName = columnName.trim();  // common user error
        String rowName = keyColumn < 0 ?
                "" + row : table.entries.get(row * table.columns + keyColumn);
        String entryName = prefix + columnName + "." + rowName;
        props.put(entryName, entry);
        vis.add(columnName, entry);
      }
      props.put(prefix + "rows", "" + table.entries.size() / table.columns);
      props.put(prefix + "columns", "" + table.columns);
      vis.done(props, prefix + "vis.");
    }
  }

  /**
   * @param key
   * @param table
   * @return The key column number, or -1 if there is none
   */
  private int getKeyColumn(String key, Table table) {
    int keyColumn = -1;
    for (int i=0; key != null && i<table.columns; i++) {
      if (key.equals(table.entries.get(i))) {
        keyColumn = i;
        break;
      }
    }
    if (keyColumn == -1 && key != null) {
      try {
        keyColumn = Integer.parseInt(key);
        if (keyColumn >= table.columns) {
          keyColumn = -1;
        }
      } catch(NumberFormatException e) {
        // ignore
      }
    }
    return keyColumn;
  }
  
  /**
   * Match a properly quoted CSV field and the trailing delimiter:
   *  foo                            - text not containing " or "," or "\n"
   *  "quoted foo"                   - anything else
   *  "contains a "" character"      - protect "'s by doubling them
   *  Blank lines are ignored
   */
  public static final Pattern FieldPattern =
    Pattern.compile("(\"([^\"]*(\"\")?)*\"|[^,\n]*)(,|\n+)");
  
  /**
   * Break a properly formatted CSV table into fields
   * @author suhler@google.com (Stephen Uhler)
   *
   */
  private static class Table {
    ArrayList<String> entries;
    int columns = 0;       // csv table size; 
    
    public Table(String contents) {
      entries = new ArrayList<String>();
      parse(contents.trim() + "\n");
    }

    /**
     * Parse  csv table.  These are always properly formed.
     * Otherwise the results are undefined
     */
    private void parse(String contents) {
      int col = 0;
      Matcher matcher = FieldPattern.matcher(contents);
      while (matcher.find()) {
        col++;
        entries.add(deQuote(matcher.group(1)));
        if (!matcher.group(4).equals(",") && columns == 0) {
          columns = col;
        }
      }
    }
    
    @Override
    public String toString() {
      return columns + ": " + entries;
    }
    
    /**
     * Remove quoting from a CSV entry
     */
    private String deQuote(String s) {
      if (s.startsWith("\"")) {
        s = s.substring(1, s.length() - 1);
      }
      return s.replaceAll("\"\"", "\"");
    }
  }
  
  /**
   * Create chart data for a set of named columns
   * @author suhler@google.com (Stephen Uhler)
   *
   */
  static class VisHelper {
    HashMap<String, ChartData> data;
    HashMap<String, StringBuffer> labels;
    /**
     * Comma separated list of column names (or numbers) to generate chart data for
     * @param chart list of columns to compute chart data for
     * @param label list of columns to compute label data for
     */
    public VisHelper(String chart, String label) {
      data = new HashMap<String, ChartData>();
      if (chart != null) {
        StringTokenizer st = new StringTokenizer(chart, ",");
        while(st.hasMoreTokens()) {
          data.put(st.nextToken(), new ChartData());
        }
      }
      
      labels = new HashMap<String, StringBuffer>();
      if (label != null) {
        StringTokenizer st = new StringTokenizer(label, ",");
        while(st.hasMoreTokens()) {
          labels.put(st.nextToken(), new StringBuffer());
        }
      }
    }
    
    /**
     * Potentially Add a value to chart data or label
     */
    public void add(String columnName, String value) {
      ChartData chart;
      if ((chart = data.get(columnName)) != null) {
        chart.addValue(value);
      }
      StringBuffer sb;
      if ((sb = labels.get(columnName)) != null) {
        sb.append(sb.length() > 0 ? "|" : "").append(value.replace("|", ","));
      }
    }
    
    /**
     * dump the results into the properties object
     */
    public void done(Properties props, String prefix) {
      for (Entry<String, ChartData>  entry : data.entrySet()) {
        props.put(prefix + entry.getKey() + ".min", "" + entry.getValue().getMin());
        props.put(prefix + entry.getKey() + ".max", "" + entry.getValue().getMax());
        props.put(prefix + entry.getKey() + ".count", "" + entry.getValue().getCount());
        props.put(prefix + entry.getKey() + ".data", entry.getValue().getEncodedData());
      }

      for (Entry<String, StringBuffer> entry : labels.entrySet()) {
        props.put(prefix + entry.getKey() + ".labels", entry.getValue().toString());
      }
    }
  }

  /**
   * Turn a series of numeric values into extended chart data.
   * See: http://code.google.com/apis/chart/docs/data_formats.html#extended
   * Useage:
   *   addValue(), addValue() ....  Add values to the set
   *   getMin(), getMax(), getEncodedData() ... retrieve results
   * @author suhler@google.com (Stephen Uhler)
   *
   */
  public static class ChartData {
    private final static String map = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.-";
    private final static char[] charMap = map.toCharArray();
    
    private ArrayList<Double> values;
    private double min, max;
    private boolean init = false;
    
    public ChartData() {
      values = new ArrayList<Double>();
    }
    
    public void addValue(String s) {
      try {
        Double d = Double.valueOf(s);
        insert(d);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    
    public double getMin() {
       return min;
    }
    
    public double getMax() {
      return max;
    }
    
    public int getCount() {
      return values.size();
    }
    
    public String getEncodedData() {
      StringBuffer sb = new StringBuffer();
      for (double value : values) {
        try {
          int scaled = (int) Math.round(4095 * (value - min) / (max - min));
          encodeValue(sb, scaled);
        } catch (ArithmeticException e) {
          e.printStackTrace();
          sb.append("__");
        }
      }
      return sb.toString();
    }
    
    public void reset() {
      values.clear();
      init = false;
    }
    
    private void insert(Double value) {
      if (!init) {
        min = max = value;
        init = true;
      } else {
        min = Math.min(min, value);
        max = Math.max(max, value);
      }
      values.add(value);
    }

    /**
     * Encode a 0-4096 value into a google vis chart representation
     */
    private void encodeValue(StringBuffer sb, int index) {
      sb.append(charMap[(index >> 6) & 0x3F]).append(charMap[index & 0x3F]);
    }
  }
}