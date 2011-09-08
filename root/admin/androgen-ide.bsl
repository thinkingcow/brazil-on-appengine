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

<style>
  /* generic google stuff */
  body,td,a,div,.p{font-family:arial,sans-serif}
  div,td{color:#000000}
  a:link,.w,.w a:link{color:#0000cc}
  a:visited{color:#551a8b}
  a:active{color:#ff0000}
  .t a:link,.t a:active,.t a:visited,.t{color:#ffffff}
  .t{background-color:#3366cc}
  .f,.f:link,.f a:link{color:#6f6f6f}
  .i,.i:link{color:#a90a08}
  .a,.a:link{color:#008000}
  div.n {margin-top: 1ex}
  .n a{font-size: 10pt; color:#000000}
  .n .i{font-size: 10pt; font-weight:bold}
  .b{font-size: 12pt; color:#0000cc; font-weight:bold}
</style>

<if name=url.orig glob=/admin/* >
  <set namespace=local name=admin value=true>
  <if query.nickname>
    <if not name=query.nickname value=nickname>
      <set name=files>
      <set name=datafiles>
    </if>
    <set namespace=local name=nickname value=${query.nickname}>
  </if>
</if>

<if query.save>
  <download property=download name="/androgen/${nickname}/${query.inlinefile}" />
  <if download>
    <addheader
     content-type=application/octet-stream
     content-disposition="attachment; filename=${query.inlinefile}"
    />
    <tossmarkup /><get download><abort />
  <else>
    <set namespace=local name=msg value="No file on server to download: ${query.inlinefile}">
  </if>
</if>

<html>
<head>
<title>Androgen IDE</title>
<table><tr><td>
  <img src=/global/AndrogenIcon_140x140.jpg>
</td><td>
  <h3>Androgen IDE<a style="text-decoration:none" href=#end><sup>*</sup></a></h3>
  <if admin>
   <form style=display:inline>
   <input name=nickname value=${nickname} size=10>
   <input type=submit value="Specify User" style=color:red>
   </form>
  <else>
    userID: <get name=nickname>
  </if>
  (
   <a title="Automatically chart results" href='/chart2.bsl' target=chart>Chart</a> |
   <a title="Show uploaded photos" href='/photo.bsl' target=photos>Photos</a> |
   <a title="Recompute file list" href='?reset=true'>Reload</a> |
   <a title="Ide keyboard shortcuts" href='javascript:help()'>Help</a>|
   <a title="Sample Application snippets" target=_samples href='/androgen-samples/index.html'>Samples</a>|
   <a title="Manage Shared Apps" href=/shared/share.bsl target=share>Published apps</a>
  )
</td></tr></table>

<script>
  function id(s) {
    return document.getElementById(s);
  }

  // convert template markup into html

  function markup(s) {
    var markup = id(s).innerHTML.replace(/[[]/g,"<").replace(/]/g,">");

    // do variable substitutions (this is a hack, it could be more general)
    var name = id("input").value.replace(/[.].*/,"");
    markup = markup.replace(/%N%/g,name);

    return markup;
  }

  // shortcut
  // insert sTemplate into text area

  function template(s, what) {
      if (what && what=="clear")  {
         id("ta").value="";
         id("input").value="new.aml";
      }
      insertAtCursor(id("ta"), markup(s + "Template").trim() + "\n");
      return false;
  }

  // play with tabbing behavior
  // The short-cuts could dispatch to a table of external divs like:
  //  <item id=xxx17 text="whatever" advance=2 len=4 />
  // to drive the shortcut keys
  // We could have others, such as:
  //    go to next '>', or go to next "..." and select it all

  var modified=false;
  function stealtab(text, evt) {
    var c = evt.witch || evt.keyCode;
    if (!modified) {
      modified=true;
      id("modified").innerHTML="!";
    }

    // this should be a table lookup

    if (c == 9) {
      insertTabAsSpaces(text);
      return false;
    }

    if (evt.altKey && c == 84) return insertAtCursor(text, "<tag />", 1, 3); // t
    if (evt.altKey && c == 76) return template("label"); // l
    if (evt.altKey && c == 65) return template("action"); // a
    // if (evt.altKey && c == 66) return template("button"); // b
    if (evt.altKey && c == 80) return template("photo"); // p
    if (evt.altKey && c == 82) return template("reset","clear"); // r
    if (evt.altKey && c == 69) return template("entry"); // e
    if (evt.altKey && c == 32) return nextThing(text); // space
    if (evt.altKey && c == 190) return promoteText(text); // >
    if (evt.altKey && c == 188) return demoteText(text); // <
    if (evt.altKey && c == 67) return checkSyntax(id("ta").selectionStart); // c
    // if (evt.altKey) alert("got: " + c);
    return true;
  }

  // look for the next attribute value
  var matchAttribute = new RegExp(/[a-z]+ *= *("[^"]*")/m);
 
  function nextThing(ta) {
    var start = ta.selectionStart || 0;
    var nl = ta.value.indexOf('\n', start);
    var msg = id("editor");
    if (nl < 0) nl = ta.value.length;
    
    var search = matchAttribute.exec(ta.value.substring(start));
    var next = -1;
    if (!search && start > 0) {	// start from the beginning?
      start = 0;
      search = matchAttribute.exec(ta.value);
    }
    if (search) {
       next = ta.value.indexOf(search[1], start);
       msg.innerHTML = search[1];
    }
    if (next >= 0) {
      ta.selectionStart = next + 1;
      ta.selectionEnd = ta.selectionStart + search[1].length - 2;
    } else {
      // not found
    }
    ta.focus();
    return false;
  }

  // copy selection box into entry field

  function change(select, entry) {
     var sel = id(select);
     var index = sel.selectedIndex || 0;
     if (index > 0) {
       id(entry).value = sel[index].value;
       status("Selected: " + sel[index].value);
     }
  }

  function status(text) {
    id("status").innerHTML=text;
  }

  // confirm deletion

  function verifydelete() {
    return confirm('Delete ' + id('input').value + '?');
  }

  // verify Override with fetch
  function verifyOk() {
    if (modified) {
       return confirm("Existing content has been modified. Really overwrite?");
    } else {
       return true;
    }
  }

  // verify upload type - .aml files need to have at least 1 <
  // csv files should have no <
  
  function verifyupload() {
    var file = id("input").value;
    var hasMarkup = /</.test(id("ta").value);
   
    if (hasMarkup && /.csv$/.test(file)) {
       return confirm("This doesn't look like a data file to me.  Are you sure?");
    }
   
    if (!hasMarkup && /.aml$/.test(file)) {
       return confirm('File has no valid markup.  Are you sure?');
    }
    return true;
  }

  // only apps that start with caps are sharable

  function verifySharing() {
    var file = id("input").value || "";
    var isSharable = /^[A-Z][A-Za-z0-9]*[.]aml$/.test(file);
    // alert("got: " + file + "(" + isSharable + ")");
    if (isSharable) {
      var desc = prompt("Enter Description","");
      if (desc) {
	id("description").value=desc;
	return true;
      } else {
        return false;
      }
    } else {
      alert("Sharable apps names must begin with uppercase letter");
      return false;
    } 
  }

  // this is ugly! (and no longer used)
  function checkSharable(evt) {
    var file = id("input").value || 'xxx';
    var hasMarkup = /<.*>/.test(id("ta").value);
    // var hasMarkup = true;
    if (evt) {
      var charCode = evt.which || evt.keyCode;
      file += String.fromCharCode(charCode);
    }
    var test = file;
    var isSharable = /^[A-Z][A-Za-z0-9]*[.]aml$/.test(file);
    if (hasMarkup) test += " (markup)";
    if (isSharable) test += " (sharable)";
    if (isSharable && hasMarkup) {
      id("sharebuttons").style.display="inline";
    } else {
      id("sharebuttons").style.display="none";
    }
    status(test);
    return true;
  }

  var timer;
  function checkShare() {
    if (timer) clearInterval(timer);
    timer = setInterval("doSharable()", 3000);
  }

  function doSharable() {
    var hasMarkup = /</.test(id("ta").value);
    var fileName = id("input").value || 'xxx';
    var isSharable = /^[A-Z][A-Za-z0-9]*[.]aml$/.test(fileName);
    setSharable(isSharable && hasMarkup);
  }

  var isShared = true;
  function setSharable(share) {
    if (isShared != share) {
      if (share) {
        id("sharebuttons").style.display="inline";
	status("Sharable");
      } else {
        id("sharebuttons").style.display="none";
	status("Not Sharable");
      }
      isShared = share;
    }
  }


  // insert tab into a text area as 1 or 2 spaces

  function insertTabAsSpaces(ta) {
    var start = ta.selectionStart || 0;
    var front = ta.value.substring(0, start);  
    var back = ta.value.substring(start, ta.value.length); 
    var nl = front.lastIndexOf("\n");
    if (nl < 0) nl = 0;
    var spaces = (((start - nl) % 2) > 0) ? "  " : " ";
    start += spaces.length;
    ta.value=front + spaces + back;
    ta.selectionStart = start + spaces.length;
    ta.focus();
  }

  function insertAtCursor(ta, text, advance, len) {
    var start = ta.selectionStart || 0;
    var front = ta.value.substring(0, start);  
    var back = ta.value.substring(start, ta.value.length); 
    var nl = front.lastIndexOf("\n");
    if (nl < 0) nl = 0;
    ta.value=front + text + back;
    if (advance) {
      ta.selectionStart = start + advance;
      ta.selectionEnd = start + advance + len;
      ta.focus();
    } else {
      nextThing(ta);
    }
    return false;
  }

  // indent all selected lines 2 spaces
  function promoteText(ta) {
    var start = ta.selectionStart || 0;
    var end = ta.selectionEnd || start;
    var front = ta.value.substring(0, start);  
    var back = ta.value.substring(end, ta.value.length); 
    var text =  "  " + ta.value.substring(start, end).replace(/\n/g,"\n  ");
    ta.value = front + text + back;
    ta.selectionStart = start;
    ta.selectionEnd = start + text.length;
    ta.focus();
    return false;
  }

  function demoteText(ta) {
    var start = ta.selectionStart || 0;
    var end = ta.selectionEnd || start;
    var front = ta.value.substring(0, start);  
    var back = ta.value.substring(end, ta.value.length); 
    var text =  ta.value.substring(start, end).replace(/(^|\n)  /g,"$1");
    ta.value = front + text + back;
    ta.selectionStart = start;
    ta.selectionEnd = start + text.length;
    ta.focus();
    return false;
  }

  function help() {
    var help = "Shortcut keys\n";
    help += "- ALT-space	find next attribute value\n";
    help += "- ALT-a		insert a button with store action\n";
    help += "- ALT-c		Check syntax starting at selection start\n";
    help += "- ALT-e		insert a text entry\n";
    help += "- ALT-l		insert a label\n";
    help += "- ALT-p		insert a button with photo action\n";
    help += "- ALT-r		reset\n";
    help += "- ALT-t		insert markup tag\n";
    help += "- ALT->		right-shift selected text\n";
    help += "- ALT-<		left-shift selected text\n";
    alert(help);
  }

  // click a button

  function doClick(what, event) {
    var code = event.keyCode
    if (code == 13) {
      id(what).click()
      return false;
    }
    return true;
  }

  // select other markup
  function otherMarkup() {
     var sel = id("other");
     var index = sel.selectedIndex || 0;
     if (index > 0) {
       var value = sel[index].value;
       sel.selectedIndex = 0;
       template(value);
     }
  }

  // Experimental simple syntax checker

  // XXX Warning: '.' doesn't match \n in javascript (grr)
  var syntaxPattern = /(<!--((\n|.)*?)-->|[{}<>\'\"\\\n])/;

  function checkSyntax(begin) {
    begin = begin || 0; // index to begin reporting errors
    var ta = id("ta");
    var line = 0; // current line
    var index = 0; // current char index
    var tokenIndex = 0;	// index to the current line
    var tokens = ta.value.split(syntaxPattern);
    var inQuote = "";
    var haveBs = false;
    // allow us to report better error ranges someday
    var startQuote = 0;
    var startTag = 0;
    // Token stack not in / in quoted string
    var c1 = 0;
    var c2 = 0;
    var c3 = 0;
    for (var i=0; i<tokens.length; i+=4) {
      var token = tokens[i];
      var delim = tokens[i+1] || "";
      tokenIndex = index;
      index += token.length + delim.length;
      if (haveBs && token.length==0) { // we have a \'d delimiter
        haveBs = false;
        continue;
      }
      haveBs = false;
      switch(delim) {
        case "\\": haveBs=true; break;
        case "\n": line++; tokenIndex = index; break;
        case "\"": 
        case "\'":
          if (c1 == 0) break;	// ignore quotes entirely outside of tags
          if (inQuote == delim) {
             inQuote = "";
             if (c2 != 0 && index >= begin) {
               syntaxError("Unbalanced >", line, tokenIndex);
               return false;
             }
             if (c3 != 0) {
               var what = c3 < 0 ? "extra" : "missing";
               syntaxError("Warning: " + what +  " } in quoted string", line, tokenIndex);
               return false;
             }
          } else if (inQuote == "") {
             inQuote = delim;
             startQuote = line;
          }
          break;
        // check for balanced {}'s in quoted strings
        case "{":
        case "}":
          if (inQuote != "") {
            c3 += delim=="{" ? 1 : -1;
          }
          break;
        case "<":
          startTag = line;
          if (inQuote == "") c1++; else c2++;
          break;
        case ">":
          if (inQuote == "") c1--; else c2--;
          break;
        default:
          // skip comment - need to count \n's
          line += delim.split("\n").length - 1;
       }
       if (index < begin) continue;

       if (c1 > 1) {
         syntaxError("Missing >", line, tokenIndex);
         return false;
       }
       if (c2 < 0) {
         syntaxError("Missing quote",  line, tokenIndex);
         return false;
       }
       if (c1 < 0) {
         syntaxError("Extra >",  line, tokenIndex);
         return false;
       }
       if (c1 > 1 || c2 > 1 | c1 < 0 || c2 < 0) {
         syntaxError("Syntax Error", line, tokenIndex);
         return false;
       }
    }
    if (c1 !=0 || c1 != 0 || inQuote != "") {
      syntaxError("Missing > or \" at end of file", line, tokenIndex);
      return false;
    }
    if (begin > 0) {
      alert("Syntax OK from cursor.");
    } else {
      alert("Syntax OK");
    }
    return false;
  }

  function syntaxError(what, line, tokenIndex) {
    alert(what + " near line " + line + "\n(click to highlight)");
    ta.selectionStart = tokenIndex - 1;
    ta.selectionEnd = tokenIndex + 1;
    ta.focus();
  }
</script>
</head>
<body bgcolor=white onload='checkShare()'>

<!-- deal with list of files -->

<if query.reset>
  <set name=files>
  <set name=datafiles>
</if>

<if not name=files glob="*.aml*">
  <search search="/androgen/${nickname}/" glob=*.aml />
  <replace name=search.names all=true match="/androgen/${nickname}/">
  <set name=files value=" ${search.names} " >
  <set namespace=local name=msg value="File list refreshed">
</if>

<if not name=datafiles glob="*.csv*">
  <search search="/androgen/${nickname}/" glob=*.data.csv />
  <replace name=search.names all=true match="/androgen/${nickname}/">
  <set name=datafiles value=" ${search.names} " >
</if>

<if query.delete>
  <if not name=query.inlinefile match="^[a-zA-Z0-9_.-]+$">
    <set namespace=local name=msg value="Invalid file name: ${query.inlinefile}">
  <else>
    <delete name="/androgen/${nickname}/${query.inlinefile}" />
    <div id=del style="display:none"><get name=query.inlinefile> deleted"</div>

    <!-- get around a namespace bug in replace by using a temp var -->
    <set namespace=local name=tmp value="${files}">
    <replace name=tmp match=" ${query.inlinefile} " replace=" " />
    <set name=files value=${tmp}>

    <set namespace=local name=tmp value="${datafiles}">
    <replace name=tmp match=" ${query.inlinefile} " replace=" " />
    <set name=datafiles value=${tmp}>

    <set namespace=local name=msg value="File deleted: ${query.inlinefile}">
  </if>
</if>

<if query.fetch>
  <get query.inlinefile><br>
  <if not name=query.inlinefile match="^[a-zA-Z0-9_.-]+$">
    <set namespace=local name=msg value="Invalid file name: ${query.inlinefile}">
  <else>
    <download
     property=newcontent
     name="/androgen/${nickname}/${query.inlinefile}"
    />
    <if newcontent>
      <set name=query.contents namespace=local value="${newcontent}">
      <set namespace=local name=msg value="file downloaded: ${query.inlinefile}">
    <else>
      <set namespace=local name=msg value="No such file: ${query.inlinefile}">
    </if>
  </if>
</if>

<!-- manage app share publishing -->

<if name=query.shareapp value=publish>
  <sql type=update database=speckle catalog=mercato close=true eval=true>
    insert into sharedApps VALUES(
      '${nickname}',
      '${query.inlinefile}', 
      default,
      '${Sql(query.description)}'
    );
  </sql>
  <if name=sql.error glob=Dupli*>
    <set name=sql.error>
    <sql type=update database=speckle catalog=mercato close=true eval=true>
    update sharedApps set description='${Sql(query.description)}'
      where user='${nickname}' and app='${query.inlinefile}'
    </sql>
  </if>
<!--
  <if name=sql.error>
    <script eval>status("${sql.error}");</script>
  <else>
    <script>status("Shared app is now published");</script>
  </if>
-->
</if>
<if name=query.shareapp value=un-share>
  <sql type=update database=speckle catalog=mercato close=true eval=true>
    delete from sharedApps where user='${nickname}' and app='${query.inlinefile}'; 
  </sql>
</if>

<if query.submittext>
  <if not name=query.inlinefile match="^[a-zA-Z0-9_.-]+$">
    <set namespace=local name=msg value="Invalid file name: ${query.inlinefile}">
  <else>
    <if name=query.submittext glob="*share*">
      <set namespace=local name=pre value="/shared_data" />
      <if name=query.inlinefile glob=*.aml>
        <set namespace=local name=pre value="/shared" />
      </if>
    <else>
      <set namespace=local name=pre value="/androgen/${nickname}" />
    </if>
    <move from=${pre}/${query.inlinefile}.previous to=${pre}/${query.inlinefile}.previous.previous force=true />
    <move from=${pre}/${query.inlinefile} to=${pre}/${query.inlinefile}.previous result=ok force=true />
    <upload name="${pre}/${query.inlinefile}" content="${query.contents}" />
    <set namespace=local name=msg value="${query.inlinefile} uploaded to ${pre}">
    <if name=ok>
      <append name=msg value="<br>(previous version saved as ${query.inlinefile}.previous)">
    </if>
    <if name=query.inlinefile glob="*.aml">
      <if not name=files glob="* ${query.inlinefile} *" >
        <set name=files value="${files}${query.inlinefile} ">
      </if>
    </if>
    <if name=query.inlinefile glob="*.csv">
      <if not name=datafiles glob="* ${query.inlinefile} *" >
        <set name=datafiles value="${datafiles}${query.inlinefile} ">
      </if>
    </if>
  </if>
</if>

<form name=form method=post>
<table border=0><tr><td colspan=2>
 <select id=select name=filename
  onchange='change("select","input")'
  title="Files currently on the server.
Select one make it the current file"
 >
   <option>choose a ".aml" file</option>
   <foreach name=i property=files sort>
     <option><get name=i></option>
   </foreach>
 </select>
 <if datafiles>
   or
   <select id=select2 name=datafilename
    onchange='change("select2","input")'
    title="Data currently on the server.
  Select one make it the current file"
   >
     <option>choose a data file</option>
     <foreach name=i property=datafiles sort>
       <option><get name=i></option>
     </foreach>
   </select>
 </if>
 <input id=input name=inlinefile value=${query.inlinefile}
  title="Current file" placeholder="something.aml"
  onkeydown='return doClick("fetch", event)'
  XXonkeypress='return checkSharable(event)'
 />
 <span id=modified title="content has been modified" style=color:red ></span>
 </td></tr>
 <tr><td align=left>
 <input type=submit name=save value="save locally"
  title="Fetch the current file from the server, and save it on your local computer"
 >
 <input type=submit name=delete value="delete file"
  onclick="return verifydelete()"
  title="Delete the current file from the server"
 />
 </td><td align=right>
 <input type=submit name=fetch id=fetch value="fetch"
  title="Fetch the current file from the server, and display it in the edit box"
  onclick="return verifyOk()"
 >
 <input name=submittext value=upload type=submit
  title="upload the contents of the edit box to the server
using the current file as the file name".
  onclick="return verifyupload()"
 >
 <span id=sharebuttons>
 <input name=shareapp value=publish type=submit
  title="Publish this app (name must begin with uppercase)"
  onclick="return verifySharing()"
 >
<!--
 <input name=shareapp value=un-share type=submit
  title="Un-advertise shared app"
  onclick="return verifySharing()"
 >
-->
 <input type=hidden id=description name=description value="">
 </span>
<if admin>
 <input name=submittext value="upload to shared" type=submit
  style=color:red
  title="upload as shared Androlet or data file".
  onclick="return verifyupload()"
 >
</if>
<button
 title="Check markup for common syntax errors"
 onclick='javascript:checkSyntax();return false'
>Check Syntax</button>
</td></tr></table>

<table border=0><tr><td>
 <textarea wrap=off id=ta name=contents rows=35 cols=80
  title="edit box - this is where you edit your markup"
  spellcheck=false
  onkeydown="return stealtab(this, event)"
 ><get name=query.contents convert=html></textarea>
</form>
</td><td valign=top>
<br>
<br>
<button onclick='return template("reset", "clear")'>new androlet</button><br>
<button onclick='return template("label")'>label</button><br>
<button onclick='return template("entry")'>text entry</button><br>
<button onclick='return template("button")'>button</button><br>
<button onclick='return template("smartbutton")'>smart buttons</button><br>
<button onclick='return template("action")'>store</button><br>
<button onclick='return template("buttonaction")'>button + store</button><br>
<button onclick='return template("photo")'>photo button</button><br>
<button onclick='return template("fusion")'>fusiontables action</button>
<a 
 href=http://tables.googlelabs.com/Home target=fusion
 title="Go to the FusionTables web app to create and manage a data table
  (This opens a new browser window)"
>edit table</a>
<br>
<br>
<select onchange='otherMarkup()' id=other>
  <option title="Some additional Androgen tags">additional markup templates</option>
  <option title="display a quick message to the user">toast</option>
  <option title="vibrate the phone">vibrate</option>
  <option title="Convert text to speech, and play it to the user">speak</option>
  <option title="display a modal dialog box">dialog</option>
  <option title="Schedule a notification sometime in the future">notify</option>
  <option title="Send an email message">email</option>
  <option title="copy a photo to picasa">picasa</option>
  <option title="define a list">list</option>
  <option title="define a list">slider</option>
  <option title="terminate this Androgen activity">finish</option>
  <option title="add Store to a shared data file attribute">storeshared</option>
</select>
<br>
<br>
<button onclick='return nextThing(document.getElementById("ta"))'>next attribute value</button>
<br>

</tr></table>
<span id=status style=color:red
 title="The result of the last action"
><get name=msg default="-"></span>
<span id=editor style="color:green"></span>

<!-- experiment with wierd stuff -->

<div style=display:none id=labelTemplate>
[label
  row=next
  color=grey
  text="label text"
]
</div>
<div style=display:none id=buttonTemplate>
[button
  row=next
  name="button1"
  text="button 1"
  action="button1Action"
]
</div>
<div style=display:none id=entryTemplate>
[entry
  row=next
  name="entryName"
  width=10
]
</div>
<div style=display:none id=actionTemplate>
[action name="storeAction"]
  [store
    file="%N%.data.csv"
    csv="${date()},${time()},text to store"
  ]
[/action]
</div>
<div style=display:none id=buttonactionTemplate>
[button
  row=next
  text="Store"
  action=storeAction
  scale=12
]

[action name=storeAction]
  [store
    file="%N%.data.csv"
    csv="${date()},${time()},text to store"
  ]
[/action]
</div>
<div style=display:none id=resetTemplate>
[view
  text="Title"
  scale=30
  color="green"
  bgColor="white"
  reset=true
]
</div>
<div style=display:none id=photoTemplate>
[button
  row=next scale=30
  text="Take Photo"
  action=photoAction
]

[action name=photoAction]
  [camera prefix="myPhoto" ok=photoUploadOK]
[/action]

[action name=photoUploadOK]
   [store csv="http://<get headers.host>/androgen/${imageName}"]   
   [dialog
     title="Photo uploaded"
     image="${imageName}"
   ]
[/action]
</div>
<div style=display:none id=smartbuttonTemplate>
[button row=next name=button1 text="button1"]
[button row=next name=button2 text="button2"]
[button row=next name=button3 text="button3"]
[button row=next name=button4 text="button4"]

[configure name=button? scale=15 color=gray width=10 action=buttonAction]

[action name=buttonAction]
  [configure name=button? color=gray scale=15 width=10]
  [configure name=${name} color=blue scale=15 width=10]
  [vibrate]
  [!-- Add the desired button action (e.g. store) here --]
  [store
   csv="${userid()},${text}"
  ]
[/action]
</div>
<div style=display:none id=fusionTemplate>
[action name=updateFusionTablesAction]
  [send href=fusiontables.bsl
    command=update
    table="table name"
     user="${userid()}"
     date="${date(%T %D)}"
     ... 

    ok=okAction
    error=errorAction
  ]
[/action]

[action name=okAction]
  [toast text="data recorded"]
[/action]

[action name=errorAction]
  [dialog title='update error' text='${error}']
[/action]
</div>

<!-- other stuff -->
<div style=display:none id=speakTemplate>
[speak
  text="Hello, can you hear me."
]
</div>
<div style=display:none id=vibrateTemplate>
[vibrate]
</div>
<div style=display:none id=toastTemplate>
[toast
  text="Short message displayed to user"
]
</div>
<div style=display:none id=dialogTemplate>
[dialog
  title="dialog title"
  text="text describing dialog box"
  yes="yesAction"
  no="noAction"
]
</div>
<div style=display:none id=finishTemplate>
[!-- This will exit the Androgen activity --]
[finish]
</div>
<div style=display:none id=storesharedTemplate>
  file="/shared_data/%N%.data.csv"
</div>
<div style=display:none id=notifyTemplate>
[!--
  Consult the documentation for more info
  cron examples (from quartz cron documentation):
    "0 15 10 * * ?"	 Fire at 10:15am every day
    "0 10,44 14 ? 3 WED" Fire at 2:10pm and at 2:44pm every Wednesday in March.

 --]
[notify
  name=myNotification
  cron="sec min hour d.o.m. month d.o.w. year"
  action="myNotificationAction"
  title="Example notification"
  text="Description of notification"
  ticker="displayed when the notification is created"
  sound=1
  vibrate=i
]

[!-- sample notification action --]
[action name=myNotificationAction]
  [action name=finished] [finish] [/action]
  [button text=exit action=finished]
  [dialog text="replace with proper action"]
  [notify name=myNotification cancel=true 
  [alarm name=myNotification cancel=true]
[/action]
</div>
<div style=display:none id=emailTemplate>
[!-- This will send an email message--]
[action name=emailAction]
  [send href=email.bsl
    to="recipient"
    subject="email subject"
    body="the message"
    ok=emailOk
  ]
[/action]
[action name=emailOk]
  [toast text="email sent"]
[/action]
</div>
<div style=display:none id=picasaTemplate>
[!-- take a photo and upload it to the server --]
[action name=photoAction]
  [camera prefix="myPhoto" ok=photoUploadOK]
[/action]

[!-- copy the photo to a picasa album --]
[action name=photoUploadOK]
  [send href=picasa.bsl
   command=upload
   image=${imageName}
   album="androgen"
   description="describe photo"
   ok=picasaOkAction
   error=picasaErrorAction
   delete=false
  ]
[/action]
[action name=picasaOkAction]
  [toast text="photo ${href} uploaded to picasa"]
[/action]
[action name=picasaErrorAction]
  [dialog title=error text=${error}]
[/action]
</div>
<div style=display:none id=listTemplate>
[list row=next  name=myList color=black text="the list prompt" action=myListAction]
  [item owner=myList text="This is item one" value="1" 
  [item owner=myList text="This is item two" value="2" 
  [!-- add additional list items here --]

[!-- optional action: the currently selected list item is ${myList.text} or ${myList.value} --]
[action name=myListAction]
  [toast text="selected item ${value}: ${text}"]
[/action]
</div>
<div style=display:none id=sliderTemplate>
[slider row=next name=myScale min=0 max=99 minWidth=150 changed=myScaleAction]
[label name=myScaleValue width=2]
[action name=myScaleAction]
  [configure name=myScaleValue text=${value}]
[/action]
</div>
<br>
<a name=end>
<if not name=admin>
  <a href=admin/androgen-ide.bsl>admin version</a>
<else>
  <a href=ls.bsl>File viewer</a>
</if>
<p>
*
(Immature Development Environment)
<p>
Someday this could be a markup editor to make it easy to generate
valid Androgen markup, either by providing markup templates, drag-n-drop
components or both. For now, its just a (mostly) dumb plain text entry.
<hr>
</a>
</body>
</html>
