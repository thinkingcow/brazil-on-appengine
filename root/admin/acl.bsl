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

<!-- manage ACL's -->

<html>
<head>
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

<title>Androgen Access List</title>
</head>
<body bgcolor=white>
<table><tr><td>
  <img src=/images/AndrogenIcon_140x140.jpg>
</td><td>
  <h3>Access Control list</h3>
</td></tr></table>

<if query.contents>
  <upload name="/admin/acl.props" content="${query.contents}">
  <h2>ACL list updated</h2>
  <refreshacls />
</if>

<set namespace=server name=mime.props value=text/plain>
<download property=download name="/admin/acl.props">
<form method=post>
 <textarea wrap=off name=contents rows=35 cols=80
  title="ACL table goes here"
  spellcheck=false
 ><get name=download></textarea>
 <br>
 <input type=submit value="Update ACL">
</form>
