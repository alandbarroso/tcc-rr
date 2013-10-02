package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.EmptyBuilding;
import mrl.mosCommunication.message.IDConverter;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 1:14 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class EmptyBuildingMessage extends AbstractMessage<EmptyBuilding> {
    private int buildingIndex;

    public EmptyBuildingMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(20);
    }

    public EmptyBuildingMessage(EmptyBuilding emptyBuilding) {
        super(emptyBuilding);
        setDefaultSayTTL(20);
        setSayTTL();
    }

    public EmptyBuildingMessage() {
        super();
        setDefaultSayTTL(20);
        setSayTTL();
        createProperties();
    }

    @Override
    public EmptyBuilding read(int sendTime) {
        return new EmptyBuilding(IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex)),sendTime);
    }

    @Override
    public void setFields(EmptyBuilding emptyBuilding) {
        this.buildingIndex = IDConverter.getBuildingKey(emptyBuilding.getBuildingID());
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
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
        channelConditions.add(ChannelCondition.Low);
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.EmptyBuilding);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }


    @Override
    public int hashCode() {
        return getPropertyValues().get(PropertyTypes.BuildingIndex);
    }
}
