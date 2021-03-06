package com.devicehive.model.eventbus;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Event bus subscription object. Represents subscriptions for all types of events - notification, commands and command updates.
 */
public class Subscription implements Portable, Serializable {

    public static final int FACTORY_ID = 1;
    public static final int CLASS_ID = 5;

    private String type;
    private String entityId;
    private String name;

    public Subscription() {

    }
    /**
     * @param type - type of event to subscribe to (notification, command, command update)
     * @param entityId - entityId of event to subscribe to (id of device, command)
     */
    public Subscription(String type, String entityId) {
        this.type = type;
        this.entityId = entityId;
    }

    /**
     * @param type - type of event to subscribe to (notification, command, command update)
     * @param entityId - entityId of event to subscribe to (id of device, command)
     * @param name - specific event name to subscribe to (notification name, command name)
     */
    public Subscription(String type, String entityId, String name) {
        this.type = type;
        this.entityId = entityId;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subscription)) return false;
        Subscription that = (Subscription) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(entityId, that.entityId) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, entityId, name);
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "type='" + type + '\'' +
                ", entityId='" + entityId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public int getFactoryId() {
        return FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        writer.writeUTF("type", type);
        writer.writeUTF("entityId", entityId);
        writer.writeUTF("name", name);
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        type = reader.readUTF("type");
        entityId = reader.readUTF("entityId");
        name = reader.readUTF("name");
    }
}
