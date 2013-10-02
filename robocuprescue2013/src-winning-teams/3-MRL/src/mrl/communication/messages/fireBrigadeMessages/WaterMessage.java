package mrl.communication.messages.fireBrigadeMessages;

import mrl.communication.Message;
import mrl.communication.MessageType;
import mrl.communication.property.*;
import rescuecore2.standard.entities.Human;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: 5/26/11
 * Time: 3:14 PM
 */
public class WaterMessage extends Message {
    public WaterMessage() {
        super();
    }

    public WaterMessage(int buildingIdIndex, int water) {
        this();
        setPropertyValue(PropertyName.AreaIdIndex, buildingIdIndex);
        setPropertyValue(PropertyName.Water, (int) Math.round((double) water / 58.8235));//for 15000 water power
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.AreaIdIndex, 2));
        properties.add(new MessageProperty(PropertyName.Water, 1));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.Water;
    }

    @Override
    public Priority getPriority() {
        return Priority.High;
    }

    @Override
    public Receivers getReceivers(Human self) {
        return Receivers.FireBrigade;
    }

    @Override
    public TypeOfSend getSendType() {
        return TypeOfSend.OnlyRadio;
    }

    @Override
    public int getInitialTTL() {
        return 0;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof WaterMessage)) {
            return false;
        }
        return (getAreaIdIndex() == ((WaterMessage) message).getAreaIdIndex());
    }

    public int getAreaIdIndex() {
        return getPropertyValue(PropertyName.AreaIdIndex);
    }

    public int getWaterValue() {
        return (int) Math.round(getPropertyValue(PropertyName.Water) * 58.8235);
    }

//    public static void main(String[] args) {
//        int a= (int) Math.round(15000.0/58.8235);
//        System.out.println("a= "+a);
//        int b = (int) Math.round(a*58.8235);
//        System.out.println(b);
//    }
}
