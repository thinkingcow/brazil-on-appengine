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
  This is the test app for fusiontables integration with androgen.
  It Maps XML like androgen commands into query parameters and then
  into fusiontables queries.

  Map "androgen" api's to "fusiontables" apis.  Example:
  <send href=[here] command=update table=[table name] ok=action error=action
   col1=value1 ... coln = valuen>
  <sent href=[here] query="raw query" ok="action to run on ok" error=action>
  <send href=[here] command=create table=[table name] columns="a:type b:type ..."
-->

<!-- common macros.  Inline for now to facilitate debugging -->

<!--
  Post a query request to fusiontables
    input
      query:	The non-encoded query string (required)
      results:  (default=results)  property to place results into
    output
      results	The raw data from the fusiontables server
      hdrs.*	The http headers from fusiontables
-->
    
<definemacro name=fusionquery global=true>
  <set namespace=local name=convertedquery value="${query}">
  <stringop name=convertedquery convert=url>
  <fetch
   method=POST addclientheaders=true
   body="sql=\${convertedquery}"
   href=https://www.google.com/fusiontables/api/query
   name=${results#results} getheaders=hdrs
  >
  <if name=hdrs.www-authenticate>
    <addheader status=401 www-authenticate="\${hdrs.www-authenticate}">
    <abort>
  </if>
</definemacro>

<!--
  Run androgen markup based on fusionquery results.
    input
      ok	name of action to run for positive result
      error	name of action to run for negative result
      results	(implicit property) see <fusiontables> above
-->

<definemacro name=fusionresult global=true>
  <stringop name=results convert=html>
  <if name=hdrs.content-type match=text/plain>
    <run action="${ok#ok}" text='\${results}'>
  <else>
    <run action="${error#error}" error='${why} \${results}'>
  </if>
</definemacro>

<if name=query.fusionDebug>
  <dialog title="Fusiontables debugging enabled" text="${headers.query}">
</if>

<!--
   Sample raw sql query
   query="raw fusiontables query
   ok=action to run on each result
-->

<if name=query.query>
  <fusionquery query="${query.query}">
  <if query.fusionDebug><dialog title="query=(${query.query})" text="${results}"></if>
  <if not name=hdrs.content-type match=text/plain>
    <run action="${query.error#error}" error='${why} ${results}'>
    <abort>
  </if>
  <extractcsv name=results headers=true namespace=local>
  <sequence name=rows count=${results.rows}>
  <foreach name=i property=rows>
    <if name=i value=${results.rows}><break></if>
    <set namespace=local name=p>
    <foreach name=p glob=results.*.${i}>
       <append name=p value="${p.name.1}='${p.value}'" delim=" ">
    </foreach>
    <inline>
      <run action=${query.ok#ok} count=${i} ${p} >
    </inline>
    <run action=${query.done#done}>
  </foreach>
  <abort>
</if>

<!--
  Create a table (string values for now?)
  <send href=[here] command=create table=[table name] columns="a b ...">
-->
  
<if name=query.command value=create>
  <foreach name=i property=query.columns>
    <append name=build value="'${i}:STRING'" delim=",">
  </foreach>
  <inline name=query eval=true>
    create ${query.table} (${build})
  </inline>
  <if query.fusionDebug>
    <dialog title="fusiontables create" text="${query}">
  </if>
  <fusionquery query="${query}">
  <fusionresult
   ok=${query.ok}
   error=${query.error}
   why="creating ${query.table}"
  >
  <abort>
</if>

<!-- 
  Cache table names
  tables.table id.[name] = [number]
-->

<set name=tables>
<if not name=tables>
  <fusionquery query="show tables" results=tables>
  <if not name=hdrs.content-type match=text/plain>
    <run 
     action="${error#error}"
     error='Unable to fetch table names: ${hdrs.status} ${results}'>
    <abort>
  </if>
  <extractcsv name=tables headers=true key="name" namespace=${SessionID}>
  <set name=tables value=true>
  <if query.fusionDebug>
    <toast text="fetching table names">
  </if>
</if>

<!--
  Simple table data for pie charts
  <send... command=piedata table=name col=name run=name>
  generate: labels,values for run command
-->

<if name=query.command value=piedata>
  <set namespace=local name=id value="${tables.table id.${query.table}}">
  <if not name=id>
    <dialog title=error text="Invalid table name: ${query.table}">
    <abort>
  </if>

  <foreach name=col property=query.col delim=",">
    <set name=results>
    <set name=labels>
    <set name=values>
    <inline name=query eval=true>
      select ${col},COUNT() from ${id} GROUP BY ${col}
    </inline>
    <fusionquery query="${query}">
    <if not name=hdrs.content-type match=text/plain>
      <run action="${error#error}" error='${results}'>
      <abort>
    </if>
    <extractcsv name=results headers=true key="${col}">
    <foreach name=i glob=results.*.* sort>
     <append name=labels value=${i.name.2} delim="${query.delim#,}">
     <append name=values value=${i.value} delim=",">
    </foreach>
    <run action=${query.run#pie} labels="${labels}" values="${values}" col=${col}>
  </foreach>

  <abort>
</if>

<!-- cache table description: describe.${id} = col1,col2,... -->

<if query.table>
  <set namespace=local name=id value="${tables.table id.${query.table}}">
  <if not name=id>
    <dialog title=error text="Invalid table name: ${query.table}">
    <abort>
  </if>
  <set name=describe.${id}>
  <if not name=describe.${id}>
    <if query.fusionDebug>
      <toast text="fetching table ${id} name">
    </if>
    <fusionquery query="describe ${id}" results=description>
    <if not name=hdrs.content-type match=text/plain>
      <run 
       action="${error#error}"
       error='Unable to fetch schema for table ${id}: ${results}'>
      <abort>
    </if>
    <extractcsv name=description headers=true key="id">
    <foreach name=i glob=description.name.* sort numeric>
      <append name=gather value=${i.value} delim=",">
    </foreach>
    <set name=describe.${id} value=${gather}>
  </if>
</if>

<!--
  update a table:
  insert into [table_id] (pcolumn_name],[column_name]...) values ([value],[value], ...)
-->

<if name=query.command value=update>
  <foreach name=i property=describe.${id} delim=",">
    <append name=values value="'${query.${i}}'" delim=",">
  </foreach>
  <inline name=query eval=true>
  insert into ${id} (${describe.${id}}) values (${values})
  </inline>
  <if query.fusionDebug>
    <dialog title="fusiontables raw query" text="${query}">
  </if>
  <fusionquery query="${query}">
  <fusionresult ok=${query.ok} error=${query.error} why="updating ${id}">
 <abort>
</if>
