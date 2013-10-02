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
 * Time: 5:09:06 PM
 */
public class BurningBuildingMessage extends Message {
    public BurningBuildingMessage() {
        super();
    }

    public BurningBuildingMessage(int buildingIdIndex, int fieriness, int temperature) {
        this();
        setPropertyValue(PropertyName.FierinessAndIndex, getIndexAndFieriness(buildingIdIndex, fieriness));
        setPropertyValue(PropertyName.Temperature, getTempVal(temperature));
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.FierinessAndIndex, 2));
        properties.add(new MessageProperty(PropertyName.Temperature, 1));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.BurningBuilding;
    }

    @Override
    public Priority getPriority() {
        return Priority.VeryHigh;
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

    @Override
    public String toString() {
        String s = " P: ";
        s += " buildingIdIndex = " + getAreaIdIndex() + " , fieriness = "+getFieriness() + " , temperature = "+getTemperature();
        return s;
    }

    private int getIndexAndFieriness(int index, int fieriness) {
        int value;
        int f;
        switch (fieriness) {
            case 1:
            case 2:
            case 3:
                f = fieriness;
                break;
            case 8:
                f = 0;
                break;
            default:
                throw new RuntimeException("Firyness eshtebahi baraye BurningBuildingMessage : " + fieriness);
        }
        value = (f << 14);
        value = (value | index);

        return value;

    }

    private int getTempVal(int temperature) {
        if (temperature > 900) {
            temperature = 900;
        }
        return (int) Math.round((double) temperature / 3.529411);
    }

    public int getAreaIdIndex() {
        int value = getPropertyValue(PropertyName.FierinessAndIndex);

        return (value & 0x3fff);
    }

    public int getFieriness() {
        int value = getPropertyValue(PropertyName.FierinessAndIndex);
        int f = (value >> 14);
        if (f == 0)
            return 8;
        return f;
    }

    public int getTemperature() {
        return (int) Math.round(getPropertyValue(PropertyName.Temperature) * 3.529411);
    }

    //////////////////////////////////////////////
//    public static int temperature(int value) {
//        return (int) (value * 3.516);
//    }
//
//    public static int temp(int temperature) {
//        if (temperature > 900) {
//            temperature = 900;
//        }
//        return (int) Math.round((double) temperature / 3.516);
//    }
//
//    private static int indexAndFieriness(int index, int fieriness) {
//        int value;
//        int f;
//        switch (fieriness) {
//            case 1:
//            case 2:
//            case 3:
//                f = fieriness;
//                break;
//            case 8:
//                f = 0;
//                break;
//            default:
//                throw new RuntimeException("Firyness eshtebahi baraye BurningBuildingMessage : " + fieriness);
//        }
//        value = (f << 14);
//        value = (value | index);
//
//        return value;
//
//    }
//
//    public static int areaIdIndex(int value) {
//
//        return (value & 0x3fff);
//    }
//
//    public static int fieriness(int value) {
//
//        int f = (value >> 14);
//        if (f == 0)
//            return 8;
//        return f;
//    }
//
//
//    public static void main(String[] args) {
//        int value;
////        int temperature=795;
////        int fieriness=8;
//
////        value = fierinessAndTemperature(fieriness, temperature);
////        System.out.println("v = "+value);
//        for (int f = 1; f < 4; f++) {
//            for (int t = 0; t < 901; t++) {
//                for (int i = 0; i < 10000; i++) {
//                    value = indexAndFieriness(i, f);
//
//                    int temp = temp(t);
//                    if (value > 65536 || temp > 256) {
//                        throw new RuntimeException("f=" + f + " t=" + t + " i:" + i+"  value="+value+"  temp="+temp);
//                    }
//                    if (f != fieriness(value)) {
//                        throw new RuntimeException("f=" + f + " t=" + t + " ff:" + fieriness(value) + " tt:" + temperature(temp));
//                    }
//                    if (Math.abs(t - temperature(temp)) > 4) {
//                        throw new RuntimeException("f=" + f + " t=" + t + " ff:" + fieriness(value) + " tt:" + temperature(temp));
//                    }
//                    if (areaIdIndex(value) != i) {
//                        throw new RuntimeException("f=" + f + " t=" + t + " ff:" + fieriness(value) + " tt:" + temperature(temp));
//                    }
//
//
//                }
//            }
//        }
//
//
////        System.out.println("f:" +fieriness(-2));
////        System.out.println("t:" +temperature(-2));
//    }
}
