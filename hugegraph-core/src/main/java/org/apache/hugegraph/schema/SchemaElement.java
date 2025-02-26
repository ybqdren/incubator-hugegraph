/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.schema;

import java.util.Collections;
import java.util.Map;

import org.apache.hugegraph.backend.id.Id;
import org.apache.hugegraph.backend.id.IdGenerator;
import org.apache.tinkerpop.gremlin.structure.Graph;

import org.apache.hugegraph.HugeException;
import org.apache.hugegraph.HugeGraph;
import org.apache.hugegraph.type.Nameable;
import org.apache.hugegraph.type.Typeable;
import org.apache.hugegraph.type.define.SchemaStatus;
import org.apache.hugegraph.util.E;
import com.google.common.base.Objects;

public abstract class SchemaElement implements Nameable, Typeable,
                                               Cloneable {

    public static final int MAX_PRIMITIVE_SYS_ID = 32;
    public static final int NEXT_PRIMITIVE_SYS_ID = 8;

    // ABS of system schema id must be below MAX_PRIMITIVE_SYS_ID
    protected static final int VL_IL_ID = -1;
    protected static final int EL_IL_ID = -2;
    protected static final int PKN_IL_ID = -3;
    protected static final int VLN_IL_ID = -4;
    protected static final int ELN_IL_ID = -5;
    protected static final int ILN_IL_ID = -6;
    protected static final int OLAP_VL_ID = -7;

    public static final Id NONE_ID = IdGenerator.ZERO;

    public static final String UNDEF = "~undefined";

    protected final HugeGraph graph;

    private final Id id;
    private final String name;
    private final Userdata userdata;
    private SchemaStatus status;

    public SchemaElement(final HugeGraph graph, Id id, String name) {
        E.checkArgumentNotNull(id, "SchemaElement id can't be null");
        E.checkArgumentNotNull(name, "SchemaElement name can't be null");
        this.graph = graph;
        this.id = id;
        this.name = name;
        this.userdata = new Userdata();
        this.status = SchemaStatus.CREATED;
    }

    public HugeGraph graph() {
        E.checkState(this.graph != null,
                     "Graph is null of schema '%s'", this.name);
        return this.graph;
    }

    public Id id() {
        return this.id;
    }

    public long longId() {
        return this.id.asLong();
    }

    @Override
    public String name() {
        return this.name;
    }

    public Map<String, Object> userdata() {
        return Collections.unmodifiableMap(this.userdata);
    }

    public void userdata(String key, Object value) {
        E.checkArgumentNotNull(key, "userdata key");
        E.checkArgumentNotNull(value, "userdata value");
        this.userdata.put(key, value);
    }

    public void userdata(Userdata userdata) {
        this.userdata.putAll(userdata);
    }

    public void removeUserdata(String key) {
        E.checkArgumentNotNull(key, "The userdata key can't be null");
        this.userdata.remove(key);
    }

    public void removeUserdata(Userdata userdata) {
        for (String key : userdata.keySet()) {
            this.userdata.remove(key);
        }
    }

    public SchemaStatus status() {
        return this.status;
    }

    public void status(SchemaStatus status) {
        this.status = status;
    }

    public boolean system() {
        return this.longId() < 0L;
    }

    public boolean primitive() {
        long id = this.longId();
        return -MAX_PRIMITIVE_SYS_ID <= id && id < 0L;
    }

    public boolean hidden() {
        return Graph.Hidden.isHidden(this.name());
    }

    public SchemaElement copy() {
        try {
            return (SchemaElement) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new HugeException("Failed to clone schema", e);
        }
    }

    public boolean hasSameContent(SchemaElement other) {
        return Objects.equal(this.name(), other.name()) &&
               Objects.equal(this.userdata(), other.userdata());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SchemaElement)) {
            return false;
        }

        SchemaElement other = (SchemaElement) obj;
        return this.type() == other.type() && this.id.equals(other.id());
    }

    @Override
    public int hashCode() {
        return this.type().hashCode() ^  this.id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s(id=%s)", this.name, this.id);
    }

    public static int schemaId(Id id) {
        long l = id.asLong();
        // Currently we limit the schema id to within 4 bytes
        E.checkArgument(Integer.MIN_VALUE <= l && l <= Integer.MAX_VALUE,
                        "Schema id is out of bound: %s", l);
        return (int) l;
    }

    public static class TaskWithSchema {

        private SchemaElement schemaElement;
        private Id task;

        public TaskWithSchema(SchemaElement schemaElement, Id task) {
            E.checkNotNull(schemaElement, "schema element");
            this.schemaElement = schemaElement;
            this.task = task;
        }

        public void propertyKey(PropertyKey propertyKey) {
            E.checkNotNull(propertyKey, "property key");
            this.schemaElement = propertyKey;
        }

        public void indexLabel(IndexLabel indexLabel) {
            E.checkNotNull(indexLabel, "index label");
            this.schemaElement = indexLabel;
        }

        public PropertyKey propertyKey() {
            E.checkState(this.schemaElement instanceof PropertyKey,
                         "Expect property key, but actual schema type is " +
                         "'%s'", this.schemaElement.getClass());
            return (PropertyKey) this.schemaElement;
        }

        public IndexLabel indexLabel() {
            E.checkState(this.schemaElement instanceof IndexLabel,
                         "Expect index label, but actual schema type is " +
                         "'%s'", this.schemaElement.getClass());
            return (IndexLabel) this.schemaElement;
        }

        public SchemaElement schemaElement() {
            return this.schemaElement;
        }

        public Id task() {
            return this.task;
        }
    }
}
