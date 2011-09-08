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

<!--
  Temporary admin console
-->
<set namespace=server name=mime.previous value=text/plain>

<head>
<title>Androgen File Lister</title>
<script>
  function renameFile(item) {
    var form = formFor(item)
    var name = prompt("Rename " + form.filename.value + " to:", form.filename.value);
    if (name) {
      form.newname.value=name;
      // form.submit();
      return true;
    }
    return false;
  }

  // find enclosing form
  function formFor(element) {
    while (element.nodeName != "FORM" && element.parentNode) {
      element = element.parentNode;
    }
    return element;
  }

</script>
</head>
<body>
<table><tr><td>
<h2>ADMIN Androgen File Lister/deleter/mover</h2>
<if not name=url.orig glob=/admin/* >
  <h2>Error - This page needs to be password protected</h2>
  <abort>
</if>

<if query.reupload>
  <h2>Uploaded <get query.filepath></h2>
    <upload
      name="${query.filepath}"
      content="${query.content}"
    />
</if>

<if name=query.display>
   <download name="${query.filepath}" />
</if>

<if name=query.delete value=delete>
  <h3>Deleting <get query.filename></h3>
  <delete name="${query.filepath}" />
<elseif name=query.copy>
  <h3>copy <get query.filename> to "shared"</h3>
  <delete name="/shared/${query.filename}" />
  <move from=${query.filepath} to=/shared/${query.filename} keep=true>
  <set namespace=local name=query.common value="common aml files">
<elseif name=query.newname>
  <h3>Moved <get query.filepath> to /androgen/<get query.user>/<get query.newname></h3>
  <move from=${query.filepath} to=/androgen/${query.user}/${query.newname} keep=false>
  <delete name="${query.filepath}" />
</if>

<form prepend=query. method=post>
user id:<input name=user value=""}>
<select name=type>
 <option value="XX">select file type</option>
 <option value=*.aml>aml files</option>
 <option value=*.csv>csv files</option>
 <option value=*>all files</option>
</select>
<input type=submit name=go value=submit>
<br>
<input type=submit name=common value="common aml files">
<input type=submit name=shared value="shared files">
</form>
<h3>Advanced Options</h3>
<form method=post>
path prefix: <input name=any value="/...">
</form>

<if query.any>
  <h3> All files starting with: <get query.any></h3>
  <search search=${query.any}>
  <foreach name=i glob=search./*>
    <date format="%D %T" ms="${i.value}">
    <form style=display:inline method=post>
      <input type=hidden name=filepath value=/${i.name.1}>
      <input type=hidden name=filename value=/${i.name.1}>
      <input type=submit name=delete value=delete>
      <if name=i.name.1 match="aml$|csv$|bsl$|html$|previous$">
	 <input type=submit name=display value="display">
      </if>
    </form>
   <b>/<get i.name.1></b><br>
  </foreach>
  <abort>
</if>

<if name=query.common>
  <h3>Common Androgen Markup Files</h3>
  <search search="/shared/">
  <table>
  <foreach name=i glob=search./shared/* sort>
    <tr>
     <td><get i.name.1></td><td><date format="%D %T" ms="${i.value}"></td>
     <td>
	<form style=display:inline method=post>
	  <input type=hidden name=filepath value=/shared/${i.name.1}>
	  <input type=hidden name=filename value=${i.name.1}>
	  <input type=hidden name=common value="common aml files">
	  <input type=submit name=delete value=delete>
          <if name=i.name.1 match="aml$|csv$|bsl$|html$|previous$">
	    <input type=submit name=display value="display">
          </if>
          <if name=i.name.1 match=".jpg$">
	    <input type=submit name=image value="show image">
          </if>
        </form>
     </td>
    </tr>
  </foreach>
  </table>
</if>

<if name=query.shared>
  <h3>Shared data Files</h3>
  <search search="/shared_data/">
  <table>
  <foreach name=i glob=search./shared_data/* sort>
    <tr>
      <td><get i.name.1></td><td><date format="%D %T" ms="${i.value}"></td>
      <td>
	<form style=display:inline method=post>
	  <input type=hidden name=filepath value=/shared_data/${i.name.1}>
	  <input type=hidden name=filename value=${i.name.1}>
	  <input type=submit name=delete value=delete>
	  <input type=hidden name=shared value="shared files">
          <if name=i.name.1 match="aml$|csv$|bsl$|html$">
	    <input type=submit name=display value="display">
          </if>
          <if name=i.name.1 match=".jpg$">
	    <input type=submit name=image value="show image">
          </if>
        </form>
      </td>
    </tr>
  </foreach>
  </table>
</if>


  <if not name=query.user>
    <h3>No user specified</h3>
  <elseif not name=query.user match="^[a-zA-Z0-9_]+$">
    <h3>Invalid user (<get name=query.user convert=html>)</h3>
  <else>
    <search search="/androgen/${query.user}/" glob="${query.type}" />
    <h3><get query.user> files</h3>
    <table>
    <foreach name=i glob=search./androgen/${query.user}/* sort>
      <tr>
        <td><get i.name.1></td><td><date format="%D %T" ms="${i.value}"></td>
	<td>
	<form style=display:inline method=post>
	  <input type=hidden name=filepath value=/androgen/${query.user}/${i.name.1}>
	  <input type=hidden name=filename value=${i.name.1}>
	  <input type=hidden name=user value=${query.user}>
	  <input type=hidden name=type value=${query.type}>
	  <input type=submit name=delete value=delete>
          <if name=i.name.1 glob=*aml>
	    <input type=submit name=copy value="copy to shared">
          </if>
          <if name=i.name.1 match="aml$|csv$|bsl$|html$|previous$">
	    <input type=submit name=display value="display">
          </if>
          <if name=i.name.1 match=".jpg$">
	    <input type=submit name=image value="show image">
          </if>
          <input type=hidden name=newname value="">
          <input type=submit name=rename value="rename file" onclick='return renameFile(this)'>
	</form>
	</td>
      </tr>
    </foreach>
    </table>
  </if>
</td>

<if name=query.filename glob=*.jpg>
  <td valign=top>
    <h3><get query.filename></h3>
    <if name=query.filepath glob=/androgen*>
      <img src="/androgen/${query.filename}">
    <else>
      <img src="${query.filepath}">
    </if>
  </td>
<elseif name=download>
  <td valign=top>
  <h2><get query.filename></h2>
  <form method=post>
  <textarea rows=30 cols=60 name=content wrap=off><get name=download convert=html></textarea>
  <br>
  <input type=submit value="upload ${query.filename}" name=reupload>
  <input type=hidden name=filepath value=${query.filepath}>
  <input type=hidden name=display value=true>
  <if query.user>
    <input type=hidden name=user value=${query.user}>
  </if>
  <if query.type>
    <input type=hidden name=type value=${query.type}>
  </if>
  </form>
  </td>
</if>

</tr></table>
</body>
