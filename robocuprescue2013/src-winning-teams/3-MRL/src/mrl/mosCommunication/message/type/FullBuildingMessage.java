package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.FullBuilding;
import mrl.mosCommunication.message.IDConverter;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 2:12 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class FullBuildingMessage extends AbstractMessage<FullBuilding> {
    int buildingIndex;
    int buildingPriority;

    public FullBuildingMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(30);
    }

    public FullBuildingMessage(FullBuilding fullBuilding) {
        super(fullBuilding);
        setDefaultSayTTL(30);
        setSayTTL();
    }

    public FullBuildingMessage() {
        super();
        setDefaultSayTTL(30);
        setSayTTL();
        createProperties();
    }

    @Override
    public FullBuilding read(int sendTime) {
        EntityID buildingID = IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex));
        return new FullBuilding(buildingID, propertyValues.get(PropertyTypes.BuildingPriority),sendTime);
    }

    @Override
    protected void setFields(FullBuilding fullBuilding) {
        this.buildingIndex = IDConverter.getBuildingKey(fullBuilding.getBuildingID());
        this.buildingPriority = fullBuilding.getBuildingPriority();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
        properties.put(PropertyTypes.BuildingPriority, new BuildingPriority(buildingPriority));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
        sendTypes.add(SendType.Emergency);
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
        setMessageType(MessageTypes.FullBuilding);
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
