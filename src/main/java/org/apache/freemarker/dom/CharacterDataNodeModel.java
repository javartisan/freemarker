/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
package org.apache.freemarker.dom;

import org.apache.freemarker.core.model.TemplateScalarModel;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;

class CharacterDataNodeModel extends NodeModel implements TemplateScalarModel {
    
    public CharacterDataNodeModel(CharacterData text) {
        super(text);
    }
    
    @Override
    public String getAsString() {
        return ((org.w3c.dom.CharacterData) node).getData();
    }
    
    @Override
    public String getNodeName() {
        return (node instanceof Comment) ? "@comment" : "@text";
    }
    
    @Override
    public boolean isEmpty() {
        return true;
    }
}