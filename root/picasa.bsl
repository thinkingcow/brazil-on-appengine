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
  Routines that map from androgen to picasa.
  Added as needed.  This is called via androgen
-->

<!--
 Upload a photo from appengine to picasa
   *command=upload
   *image="path of photo on appengine server"
   album="album name or id"
   title="title of photo"
   description="description of photo"
   ok="action to run if upload is ok"
   error="action to run if upload failed"
   delete=true|false
-->

<if name=query.command value=upload>
  <photo prepend=upload
   command=upload
   title="${query.title#${query.image}}"
   description="${query.description}"
   name=/androgen/${nickname}/${query.image}
   delete="${query.delete#false}"
   album=${query.album#androgen}
   userID=${query.userID#default}
  >
  <if name=upload.id>
    <run action=${query.ok#ok}
     id="${upload.id}" href="${upload.href}" newalbum="${upload.created#false}"
    >
  <else>
    <dialog title=error text=${upload.error}>
    <run action=${query.error#error} error="${upload.error}">
    <addheader status=401>
  </if>
  <abort>
</if>

<!-- list albums -->

<if name=query.command value=albumlist>
  <album command=list>
  <if name=results.errorClass glob=*ServiceForbidden*>
    <addheader status=401>
    <abort>
  </if>
  <if query.before>
    <run action=${query.before} albums=${results.albums}>
  </if>
  <sequence name=album count=${results.albums}>
  <foreach name=i property=album>
    <run 
     action=${query.action#action} 
     id="${results.${i}.id}"
     access="${results.${i}.access}"
     photo="${results.${i}.name}"
     title="${results.${i}.title}"
     count="${results.${i}.count}"
     description="${results.${i}.description}"
    >  
  </foreach>
  <if query.after>
    <run action=${query.after} albums=${results.albums}>
  </if>
  <abort>
</if>

<!-- List photo information -->

<if name=query.command value=photolist>
  <photo command=list album=${query.id} size=${query.size#240}>
  <if name=results.errorClass glob=*ServiceForbidden*>
    <addheader status=401>
    <abort>
  </if>
  <if query.before>
    <run action=${query.before} albums=${results.photos}>
  </if>
  <sequence name=photo count=${results.photos}>
  <foreach name=i property=photo>
    <run 
     action=${query.action#action} 
     size="${results.${i}.size}"
     width="${results.${i}.width}"
     height="${results.${i}.height}"
     id="${results.${i}.id}"
     description="${results.${i}.description}"
     picasaUrl="${results.${i}.url}"
     link="${results.${i}.self}"
     rotation="${results.${i}.rotation}"
    >  
  </foreach>
  <if query.after>
    <run action=${query.after} albums=${results.photos}>
  </if>
  <abort>
</if>
<dialog title=oops text="Invalid query">
