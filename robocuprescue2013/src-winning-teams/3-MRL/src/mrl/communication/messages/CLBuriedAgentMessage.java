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
 * Time: 6:20:08 PM
 */
public class CLBuriedAgentMessage extends Message {
    public CLBuriedAgentMessage() {
        super();
    }

    public CLBuriedAgentMessage(int hp, int buriedness, int damage, int positionIdIndex, int agentIdIndex) {
        this();
        setPropertyValue(PropertyName.HumanID, agentIdIndex);
        setPropertyValue(PropertyName.Buriedness, buriedness);
        setPropertyValue(PropertyName.Damage, getSendingDamage(damage));
        setPropertyValue(PropertyName.HealthPoint, getHp(hp));
        setPropertyValue(PropertyName.HealthPointAndAreaIdIndex, getHPAndAreaIndex(hp, positionIdIndex));
    }

    @Override
    protected List<MessageProperty> getMessageProperties() {
        List<MessageProperty> properties = new ArrayList<MessageProperty>();
        properties.add(new MessageProperty(PropertyName.HumanID, 1));
        properties.add(new MessageProperty(PropertyName.Buriedness, 1));
        properties.add(new MessageProperty(PropertyName.Damage, 1));
        properties.add(new MessageProperty(PropertyName.HealthPoint, 1));
        properties.add(new MessageProperty(PropertyName.HealthPointAndAreaIdIndex, 2));
        return properties;
    }

    @Override
    public MessageType getType() {
        return MessageType.CLBuriedAgent;
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
        return TypeOfSend.OnlyVoice;
    }

    @Override
    public int getInitialTTL() {
        return 30;
    }

    @Override
    public boolean equals(Message message) {
        if (!(message instanceof CLBuriedAgentMessage)) {
            return false;
        }
        return (getAgentIdIndex() == ((CLBuriedAgentMessage) message).getAgentIdIndex());
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

    public int getAgentIdIndex() {
        return getPropertyValue(PropertyName.HumanID);
    }

    public int getBuriedness() {
        return getPropertyValue(PropertyName.Buriedness);
    }

    public int getDamage() {
        return (int) (getPropertyValue(PropertyName.Damage) * 3.921568627);
    }

    public int getHealthPoint() {
        int hp = getPropertyValue(PropertyName.HealthPoint);
        int h = getPropertyValue(PropertyName.HealthPointAndAreaIdIndex);
        h = h & 0xc000;
        h = (h >> 14);
        hp = hp << 2;
        hp = hp | h;
        hp = (int) (hp * 9.775171065);
        return hp;
    }

    public int getAreaIdIndex() {
        int indexAndHP = getPropertyValue(PropertyName.HealthPointAndAreaIdIndex);
        indexAndHP = indexAndHP & 0x3fff;
        return indexAndHP;
    }
}
