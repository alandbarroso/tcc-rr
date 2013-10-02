package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.BuriedAgent;
import mrl.mosCommunication.message.IDConverter;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/17/13
 * Time: 4:57 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class BuriedAgentMessage extends AbstractMessage<BuriedAgent> {

    int buildingIndex;
    int hp;
    int buriedness;
    int damage;

    public BuriedAgentMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(1);
    }

    public BuriedAgentMessage(BuriedAgent buriedAgent) {
        super(buriedAgent);
        setDefaultSayTTL(1);
        setSayTTL();
    }

    public BuriedAgentMessage() {
        super();
        setDefaultSayTTL(1);
        setSayTTL();
        createProperties();
    }

    @Override
    public BuriedAgent read(int sendTime) {
        BuriedAgent buriedAgent = new BuriedAgent();
        buriedAgent.setBuildingID(IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex)));
        buriedAgent.setHp(propertyValues.get(PropertyTypes.HealthPoint));
        buriedAgent.setDamage(propertyValues.get(PropertyTypes.Damage));
        buriedAgent.setBuriedness(propertyValues.get(PropertyTypes.Buriedness));
        buriedAgent.setSendTime(sendTime);
        return buriedAgent;
    }

    @Override
    public void setFields(BuriedAgent buriedAgent) {
        buildingIndex = IDConverter.getBuildingKey(buriedAgent.getBuildingID());
        hp = buriedAgent.getHp();
        damage = buriedAgent.getDamage();
        buriedness = buriedAgent.getBuriedness();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
        properties.put(PropertyTypes.HealthPoint, new HealthPointProperty(hp));
        properties.put(PropertyTypes.Damage, new DamageProperty(damage));
        properties.put(PropertyTypes.Buriedness, new BuildingIndexProperty(buriedness));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
        sendTypes.add(SendType.Speak);
    }

    @Override
    protected void setReceivers() {
        receivers.add(Receiver.FireBrigade);
        receivers.add(Receiver.PoliceForce);
        receivers.add(Receiver.FireBrigade);
    }

    @Override
    protected void setChannelConditions() {
        channelConditions.add(ChannelCondition.High);
        channelConditions.add(ChannelCondition.Medium);
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.BuriedAgent);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }

    @Override
    public int hashCode() {
        return sender == null ? -1 : sender.getValue();
    }
}
