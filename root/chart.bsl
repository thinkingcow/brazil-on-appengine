<!--
  Copyright 2011 Google Inc. All Rights Reserved.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<!-- Sample pie-chart generator for CSV files using GVIS charts -->

<head>
<title>Androgen Sample Charts</title>
<style>
.big {font-size:120%}
</style>

</head>
<body>
<table><tr><td>
<img height=50% src=/images/AndrogenIcon_140x140.jpg>
</td><td valign=center>
<h2>Pie-O-Matic Chart Generator</h2>
</td></tr>
</table>


<!-- fetch file names to plot -->
<if not name=headers.query>
  <set name=shared>
  <set name=private>
</if>

<if not name=private>
  <search search=/shared_data/ glob="*.csv">
  <foreach name=i glob=search./shared_data/*>
    <append name=shared value=${i.name.1} delim=" " namespace=${SessionID}>
  </foreach>
  <search search=/androgen/${nickname}/ glob="*.csv">
  <foreach name=i glob=search./androgen/${nickname}/*>
    <append name=private value=${i.name.1} delim=" " namespace=${SessionID}>
  </foreach>
</if>

Choose <span title="comma seperated value">CSV</span> file to plot
<form prepend=query. style=display:inline >
  <select name=file>
    <option>... select shared ...</option>
    <foreach name=i sort property=shared><option value="${i}"><get i></option></foreach>
  </select>
  <input type=submit value=chart> -or-
</form>
<form prepend=query. style=display:inline >
  <select name=private>
    <option>... select <get nickname> ...</option>
    <foreach name=i sort property=private><option value="${i}"><get i></option></foreach>
  </select>
  <input type=submit value=chart>
</form>
<br>

<if name=query.private>
  <set namespace=local name=myurl value="/androgen/${query.private}">
<elseif query.file>
  <set namespace=local name=myurl value="/shared_data/${query.file}">
</if>
   
<if myurl>
    <tag>script</tag>formUrl="<get myurl>"</script>
    <!--Load the AJAX API-->
    <script type="text/javascript" src="http://www.google.com/jsapi"></script>
    <script type="text/javascript">
      var counter=1;
      var cache = new Object();

      // Load the Visualization API and the piechart package.
      google.load('visualization', '1', {'packages':['piechart']});
      
      // Set a callback to run when the Google Visualization API is loaded.
      google.setOnLoadCallback(fetchData);

      // fetch my own csv files and convert them into data objects

      function getReq() {                   // get an http request object
        if (window.XMLHttpRequest) {        // Mozilla/firefox
          return new XMLHttpRequest();
        } else {
          throw new Error("bad bad bad!");
        }
      }

      var running=true;
      var rerun;
      function submitUrl(url, callback) {
        var query = "random=" + Math.random();
        for (var i=2; i<arguments.length-1; i+=2) {
          query += "&" + arguments[i] + "=" + escape(arguments[i+1]);
        }
        var req = getReq();
	var callMe = callback;
	var myUrl = url;
	rerun = function() {
          status("checking...");
	  submitUrl(myUrl, callMe);
	}
        req.onreadystatechange = function() {
          if (req.readyState == 4) {
              if (req.status != 200) {
                status("Oops: " + req.statusText);
                return;
              }
              status("ready ");
              if (!(cache[myUrl] && req.responseText.length==cache[myUrl])) {
                cache[myUrl] = req.responseText.length;
                callMe(req);
              }
	      if (running) setTimeout(rerun, 3000);
          }
        }
        req.open("GET", url + "?" + query);
        req.send(null);
        return true;
      }

      function callback(response) {
        status("update " + (counter++) + " " + Date());
	var disp = document.getElementById('data');
	disp.innerHTML=response.responseText;
        disp.scrollTop = disp.scrollHeight;
	var stuff = parseCsv(response.responseText);

        var pies = 0;
	for (var i=0; i<stuff.cols; i++) {
	  var pie = stuffToPie(stuff, i);
	  if (pie) {
            pies++;
	    var title = stuff.header ? stuff['0,' + i] : "Column " + i
	    var data = stuffToData(pie);
	    var chart = new google.visualization.PieChart(document.getElementById('chart_' + i));
            google.visualization.events.addListener(chart, 'onmouseover', genHandler(pie));
            google.visualization.events.addListener(chart, 'onmouseout', clearCurrent);
	    chart.draw(data, {width: 800, height: 300, is3D: true, title: title, fontSize: 24});
	  }
	}
        status("Generating charts for " + pies + " of " + stuff.cols + " data columns", "heading");
      }

      function clearCurrent() {
        status("-", "current");
      }
   
      function genHandler(csvdata) {
         return  function(e) {
             var row = e.row + csvdata.header;
             status(csvdata[row + ",0"] + "= " + csvdata[row + ",1"], "current"); 
          }
      }

      function fetchData() {
        status(formUrl, "misc");
        submitUrl(formUrl, callback);
      }

      function describe(obj) {
        var s = "";
	for (var i in obj) {
	  s += i + "=" + obj[i] + "\n";
	}
	return s;
      }

      // convert our simple data table into a dataFrame
      // Simple scheme for now - fix later

      function stuffToData(stuff) {
	var dataTable  = new google.visualization.DataTable();
	for (var col=0;col<stuff.cols; col++) {
	  var type = isNumeric(stuff,col) ? 'number' : 'string';
          stuff[col + ",type"] = type;
	  dataTable.addColumn(type, stuff.header ? stuff["0," + col] : "" + col);
	}
	for (var row=stuff.header;row<stuff.rows; row++) {
	  var rowData = new Array()
	  for (var col=0; col<stuff.cols; col++) {
	    var value = stuff[row + "," + col];
	    rowData[col] = isNumeric(stuff, col) ? parseFloat(value) : value;
	  }
	  dataTable.addRow(rowData);
	}
	return dataTable;
      }

      // see if every every row is numeric.

      function isNumeric(stuff, col) {
        var cached = stuff[col + ",isNumeric"];
        if (cached) {
	  return cached;
	}
        for (var i = stuff.header; i<stuff.rows; i++) {
	  if (isNaN(parseFloat(stuff[i + "," + col]))) {
	    stuff[col + ",isNumeric"] = false;
	    return false;
	  }
	}
	stuff[col + ",isNumeric"] = true;
	return true;
      }
	

      // Convert column name into col# or null

      function colOf(stuff, name) {
        for (var i=0;i<stuff.cols;i++) {
	  if (stuff["0," + i] == name) return i
	}
	return null;
      }

      // parses CSV into a data table for the vis stuff
      //  Grab a field, turn it into a dataTable
      //  First convert into a literal json object.
      //  We need to look at the values to guess numbers or strings?
      //  If the 1st char starts with '#' then we have a header?
      //  XXX someday "stuff" will be a real object.  Just playn' now

      var reg = /("([^"]*("")?)*"|[^,\n]*)([,\n])/g
      function parseCsv(data) {

	// convert csv data into an "object" table

	var stuff = new Object();
	var row = 0;
	var col = 0;
	var cols;
        var split=data.split(reg);

	stuff.header = data.indexOf("#") == 0 ? 1 : 0;

	for(var i=0; i<split.length-5; i+=5) {
	  var field = split[i+1];
	  if (split[i+2]) {
	    field=fixQuoted(field).trim();
	  }
	  stuff[row + "," + col] = field;
	  if (split[i+4] == ",") {
	    col++;
	  } else {
	    if (row==0) cols=col + 1;
            if (col > 0 || field.length>0) {  // toss lines with no data
	      row++;
            } 
            col=0;
	  }
	}
	if (stuff.header) {
	  stuff["0,0"] = stuff["0,0"].substring(1);
	}
	stuff.rows = row;
	stuff.cols = cols;
	return stuff;
      }

      // remove quotes from csv field

      function fixQuoted(field) {
        return field.replace(/^"|"$/g,'').replace(/""/g,'"'); 
      }

      // Turn a colum of "stuff" into a new "stuff" object suitable for a pie chart
      // or null otherwise.  Use heuristics.

      function stuffToPie(stuff, col) {
        var test = new Object();
	var bins = 0;
        var gtOne = 0;
	for(var i = stuff.header; i<stuff.rows; i++) {
	  var value = stuff[i + ',' + col].trim();
	  if (test[value]) {
            if (test[value]==1) gtOne++;
	    test[value]++;
	  } else {
	    bins++;
	    test[value]=1;
	  }
	}
	if (bins < 2) return null; // not enough bins
	if (gtOne > 10) return null; // too many bins
	if (bins > 10) { // turn 1's into "other"
	  if (!test["other"]) test["other"] = 0;
	  for(var i in test) {
	    if(test[i] == 1) {
	      test.other++;
	      delete test[i];
	    }
	  }
	}
	// turn into "stuff"

	result = new Object();
	result.cols=2;
	result.header=stuff.header;
	var row = stuff.header;
	if (stuff.header) {
	  var header =  stuff['0,' + col];
	  result['0,0'] = header;
	  result['1,0'] = header;
	}
	for (var i in test) {
	  result[row + ",0"] = '"' + i + '"';
	  result[row + ",1"] = test[i];
	  row++;
	}
	result.rows = row;
	// alert(describe(result));
	return result;
      }

      function status(msg, div) {
        div = div || "status"
        document.getElementById(div).innerHTML=msg;
      }

      function toggleVis(id) {
        var d = document.getElementById(id);
        var vis = d.style.display;
        d.style.display = (vis == "none") ? "block" : "none";
        d.scrollTop = d.scrollHeight;
      }

      function toggleRunning(id) {
        if (running) {
          running=false;
          id.innerHTML = "start";
          id.style.color='red';
        } else {
          running = true;
          id.innerHTML = "stop";
          id.style.color='green';
	  rerun();
        }
      }
    </script>

    <span id=heading class=big></span>
    <button onclick='javascript:toggleVis("data")'>Toggle data view</button>
    <button title="toggles auto-refresh" onclick='javascript:toggleRunning(this)'>stop</button>
    <br>
    <pre id=data style=display:none;overflow:auto;width:100%;height:20ex>(No Data)</pre>
    <span id=misc style="color:green"></span>
    <span id=status style="color:orange">status</span>
    <br><span title="mouse over slice to see values here" id=current style="color:blue">-</span>
    <!--Div that will hold the pie chart-->
    <div class=big id="chart_0"></div>
    <div class=big id="chart_1"></div>
    <div class=big id="chart_2"></div>
    <div class=big id="chart_3"></div>
    <div class=big id="chart_4"></div>
    <div class=big id="chart_5"></div>
    <div class=big id="chart_6"></div>
    <div class=big id="chart_7"></div>
    <div class=big id="chart_8"></div>
    <div class=big id="chart_9"></div>
</if>
</body>
