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

<!-- advanced file manipulation -->
<title>Hard-core Resource Management</title>
<h1>This is advanced stuff - Tread lightly</h1>

<if name=query.delete>
  <h3>Deleting <get query.delete></h3>
  <delete name="${query.delete}" />
<elseif name=query.copy>
  <h3>copy <get query.from> to <get query.to></h3>
  <move from=${query.from} to=${query.to} keep=true>
</if>

<form prepend=query. >
  Search any prefix:<input name=search value="/...">
  <input type=submit value=search>
</form>

<form prepend=query. method=post>
  Delete file:<input name=delete value="/...">
  <input type=submit value=delete>
</form>

<form prepend=query. method=post>
  Copy file:
  <input name=from value="/..."> to: <input name=to value="/...">
  <input type=submit name=copy value="copy">
</form>

<form  method=POST enctype=multipart/form-data>
  File Uploader<br>
  Directory to upload to:
  <input name=uploadpath>
  Select file: <input type=file name=filename><br>
  <input type=submit name=upload value=upload>
</form>

<if name=query.search>
  <h3> All files starting with: <get query.search></h3>
  <search search=${query.search}>
  <foreach name=i glob=search./* sort>
    <b>/<get i.name.1></b>
    <date format="%D %T" ms="${i.value}">
    <br>
  </foreach>
</if>
