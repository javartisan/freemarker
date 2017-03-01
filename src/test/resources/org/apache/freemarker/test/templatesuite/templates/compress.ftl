<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<html>
<head>
<title>FreeMarker: Compress Test</title>
</head>
<body>

<#assign utility={'standardCompress': "org.apache.freemarker.core.util.StandardCompress"?new()}>
<p>A simple test follows:</p>

<p>${message}</p>

<#compress>

  <p>This is the same message,  using the &quot;compress&quot; tag:</p>


<p>${message}</p>
</#compress>

<@utility.standardCompress buffer_size=8>

  <p>This is the same message,  using the &quot;StandardCompress&quot; transform model:</p>


<p>${message}</p>
</@>

<@utility.standardCompress single_line=true>

<p>This 
   multi-line message 
     should 
be compressed 
   to a single line.</p></@>

<p>An example where the first character is not whitespace but the second character is:</p>
<p><#compress>x y</#compress></p>

<p>The end.</p>
</body>
</html>
