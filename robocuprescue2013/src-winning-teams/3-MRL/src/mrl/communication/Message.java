/*
 * This file is part of RoboAKUT Project.
 *
 * RoboAKUT Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RoboAKUT Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RoboAKUT Project.  If not, see <http://www.gnu.org/licenses/>
 *
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mrl.communication;

import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * @author murat
 *         edited by Mostafa Shabani.
 *         Date: Jan 3, 2011
 *         Time: 4:08:19 PM
 */
public abstract class Message {
    private List<MessageProperty> properties;
    private int messageByteArraySize = 0;

    protected abstract List<MessageProperty> getMessageProperties();

    public abstract MessageType getType();

    public abstract Priority getPriority();

    public abstract Receivers getReceivers(Human self);

    public abstract TypeOfSend getSendType();

    public abstract int getInitialTTL();

    public abstract boolean equals(Message message);

    public Message() {
        List<MessageProperty> properties = getMessageProperties();

        this.properties = new ArrayList<MessageProperty>();
        this.properties.addAll(properties);

        for (MessageProperty property : properties) {
            messageByteArraySize += property.getByteNeeded();
        }
    }

    public void setPropertyValue(PropertyName propertyName, int value) {
        for (MessageProperty property : properties) {
            if (property.getPropertyName().equals(propertyName)) {
                property.setValue(value);
            }
        }
    }

    public List<MessageProperty> getProperties() {
        return properties;
    }

    public Integer getPropertyValue(PropertyName propertyName) {
        for (MessageProperty property : properties) {
            if (property.getPropertyName().equals(propertyName)) {
                return property.getValue();
            }
        }
        return null;
    }

    public int getMessageByteArraySize() {
        return messageByteArraySize;
    }

    @Override
    public String toString() {
        String s = " P: ";
        for (MessageProperty p : properties) {
            s += p.getPropertyName() + "=" + p.getValue() + " ";
        }
        return s;
    }
}
