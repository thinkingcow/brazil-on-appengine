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

<!-- Missing resource template -->

<if name=url.orig value=/androgen/index.aml>
  <addheader content-type=text/plain>
  <dialog cancelText="dismiss" title="Nuthin' Here Yet"
    text="<h2>You have not yet defined a default Androgen application</h2>
       Someday there will be instructions on what to do here.
       <p>
       If you know the name of your app, you can enter it via the menu."
  >
  <abort>
</if>

<if name=url.orig glob=*.aml>
  <addheader content-type=text/plain>
  <dialog cancelText="dismiss" title="404 Application Not Found"
    text="The Androgen-let you requested: <b>${url.orig}</b> isn't here."
  >
  <abort>
</if>

<if name=headers.url glob="*/">
  <addheader location="index.html">
  <abort>
</if>

<title>Missing</title>
<addheader status=404>
Hello <b><get nickname></b>, 
The page you are looking for: <b><get name=url.orig convert=html></b>,
is missing!
