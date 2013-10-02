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
 * Time: 5:13:46 PM
 */
public class CivilianSeenMessage extends Message {
    public CivilianSeenMessage() {
        super();
    }

    public CivilianSeenMessage(int id, int buriedness, int damage, int hp, int positionIdIndex, int timeToRefuge) {
        this();
        setPropertyValue(PropertyName.HumanID, id);
        setPropertyValue(PropertyName.Buriedness, buriedness);
        setPropertyValue(PropertyName.Damage, getSendingDamage(damage));
        setPropertyValue(PropertyName.HealthPoint, getHp(hp));
        setPropertyValue(PropertyName.HealthPointAndAreaIdIndex, getHPAndAreaIndex(hp, positionIdIndex));
        setPropertyValue(PropertyName.TimeToRefuge, timeToRefuge);
//        System.out.println("create CivilianSeenMessage : id = "+id+" buriedness = "+buriedness+" damage = "+damage+" hp = "+hp+" posIndex = "+positionIdIndex+"  "+this.toString());
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.HumanID, 4));
        properties.add(new MessageProperty(PropertyName.Buriedness, 1));
        properties.add(new MessageProperty(PropertyName.Damage, 1));
        properties.add(new MessageProperty(PropertyName.HealthPoint, 1));
        properties.add(new MessageProperty(PropertyName.HealthPointAndAreaIdIndex, 2));
        properties.add(new MessageProperty(PropertyName.TimeToRefuge, 1));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.CivilianSeen;
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
        return TypeOfSend.OnlyRadio;
    }

    @Override
    public int getInitialTTL() {
        return 30;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof CivilianSeenMessage)) {
            return false;
        }
        return (getCivilianId() == ((CivilianSeenMessage) message).getCivilianId());
    }

    private int getSendingDamage(int damage) {
        if (damage > 1000) {
            damage = 1000;
        }
        return (int) Math.round((double) damage / 3.921568627);
    }

    private int getHp(int hp) {
        hp = (int) Math.round(hp / 9.775171065);
        hp = (hp >> 2);
        return hp;
    }

    private int getHPAndAreaIndex(int hp, int positionIdIndex) {
        int value;

        hp = (int) Math.round(hp / 9.775171065);
        hp = (hp & 0x03);
        hp = (hp << 14);
        value = (positionIdIndex | hp);

        return value;
    }

    public int getCivilianId() {
        return getPropertyValue(PropertyName.HumanID);
    }

    public int getBuriedness() {
        return getPropertyValue(PropertyName.Buriedness);
    }

    public int getDamage() {
        return (int) Math.round(getPropertyValue(PropertyName.Damage) * 3.921568627);
    }

    public int getHealthPoint() {
        int hp = getPropertyValue(PropertyName.HealthPoint);
        int h = getPropertyValue(PropertyName.HealthPointAndAreaIdIndex);
        h = h & 0xc000;
        h = (h >> 14);
        hp = hp << 2;
        hp = hp | h;
        hp = (int) Math.round(hp * 9.775171065);
        return hp;
    }

    public int getAreaIdIndex() {
        int indexAndHP = getPropertyValue(PropertyName.HealthPointAndAreaIdIndex);
        indexAndHP = indexAndHP & 0x3fff;
        return indexAndHP;
    }

    public int getTimeToRefuge() {
        return getPropertyValue(PropertyName.TimeToRefuge);
    }


    ////////////////////////////////////////////
//    private static int getSendingDamage0(int damage) {
//        if (damage > 1000) {
//            damage = 1000;
//        }
//        return (int) Math.round((double) damage / 3.921568627);
//    }
//
//    private static int getHp0(int hp) {
//        hp = (int) Math.round(hp / 9.775171065);
//        hp = (hp >> 2);
//        return hp;
//    }
//
//    private static int getHPAndAreaIndex0(int hp, int positionIdIndex) {
//        int value;
//
//        hp = (int) Math.round(hp / 9.775171065);
//        hp = (hp & 0x03);
//        hp = (hp << 14);
//        value = (positionIdIndex | hp);
//
//        return value;
//    }
//
//    public static int getDamage0(int d) {
//        return (int) (d * 3.921568627);
//    }
//
//    public static int getHealthPoint0(int hp, int h) {
//        h = h & 0xc000;
//        h = (h >> 14);
//        hp = hp << 2;
//        hp = hp | h;
//        hp = (int) (hp * 9.775171065);
//        return hp;
//    }
//
//    public static int getAreaIdIndex0(int indexAndHP) {
//        indexAndHP = indexAndHP & 0x3fff;
//        return indexAndHP;
//    }
//
//    public static void main(String[] args) {
//
//        for (int hp = 0; hp < 10000; hp++) {
//            for (int index = 0; index < 10000; index++) {
//                int h1 = getHp0(hp);
//                int h2 = getHPAndAreaIndex0(hp, index);
//
//                if (Math.abs(hp - getHealthPoint0(h1, h2)) > 10) {
//                    throw new RuntimeException("hp=" + hp + " hhh:" + getHealthPoint0(h1, h2) + " index=" + index);
//                }
//                if (index != getAreaIdIndex0(h2)) {
//                    throw new RuntimeException("hp=" + hp + " index=" + index + " iii : " + getAreaIdIndex0(h2));
//                }
//            }
//        }
//        for (int damage = 0; damage < 10000; damage++) {
//            int d = getSendingDamage0(damage);
//
//            int ddd = damage;
//            if (damage > 1000) {
//                ddd = 1000;
//            }
//            if (Math.abs(ddd - getDamage0(d)) > 4) {
//                throw new RuntimeException(" damage=" + damage + " ddd : " + getDamage0(d));
//            }
//        }
//
//        System.out.println("DONE");
//    }
}
