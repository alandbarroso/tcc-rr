package mrl.communication.messages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Jan 17, 2011
 * Time: 5:12:38 PM
 */
public class ExtinguishedBuildingMessage extends Message {
    public ExtinguishedBuildingMessage() {
        super();
    }

    public ExtinguishedBuildingMessage(int buildingIdIndex, int fieriness) {
        this();
        setPropertyValue(PropertyName.FierinessAndIndex, getIndexAndFieriness(buildingIdIndex, fieriness));
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.FierinessAndIndex, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.ExtinguishedBuilding;
    }

    @Override
    public Priority getPriority() {
        return Priority.High;
    }

    @Override
    public Receivers getReceivers(Human self) {
        return Receivers.All;
    }

    @Override
    public TypeOfSend getSendType() {
        return TypeOfSend.VoiceAndRadio;
    }

    @Override
    public int getInitialTTL() {
        return 20;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof ExtinguishedBuildingMessage) && !(message instanceof BurningBuildingMessage)) {
            return false;
        } else if (message instanceof ExtinguishedBuildingMessage) {
            return (getAreaIdIndex() == ((ExtinguishedBuildingMessage) message).getAreaIdIndex());
        } else {
            return (getAreaIdIndex() == ((BurningBuildingMessage) message).getAreaIdIndex());
        }
    }

    private int getIndexAndFieriness(int index, int fieriness) {
        int value;
        int f;
        switch (fieriness) {
            case 4:
            case 5:
            case 6:
            case 7:
                f = fieriness - 4;
                break;
            default:
                throw new RuntimeException("Firyness eshtebahi baraye ExtinguishMessage : " + fieriness);
        }
        value = (f << 14);
        value = (value | index);

        return value;

    }

    public int getFieriness() {
        int value = getPropertyValue(PropertyName.FierinessAndIndex);

        return (value >> 14) + 4;
    }

    public int getAreaIdIndex() {
        int value = getPropertyValue(PropertyName.FierinessAndIndex);

        return (value & 0x3fff);
    }

}
