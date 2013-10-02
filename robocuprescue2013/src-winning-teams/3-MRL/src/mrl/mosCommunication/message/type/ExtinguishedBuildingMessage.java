package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.ExtinguishedBuilding;
import mrl.mosCommunication.message.IDConverter;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 1:19 PM
 * @Author: Mostafa Movahedi
 */
public class ExtinguishedBuildingMessage extends AbstractMessage<ExtinguishedBuilding> {
    private int buildingIndex;
    private int fieryness;

    public ExtinguishedBuildingMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(20);
    }

    public ExtinguishedBuildingMessage(ExtinguishedBuilding extinguishedBuilding) {
        super(extinguishedBuilding);
        setDefaultSayTTL(20);
        setSayTTL();
    }

    public ExtinguishedBuildingMessage() {
        super();
        setDefaultSayTTL(20);
        setSayTTL();
        createProperties();
    }

    @Override

    public ExtinguishedBuilding read(int sendTime) {
        EntityID buildingID = IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex));
        return new ExtinguishedBuilding(buildingID,propertyValues.get(PropertyTypes.Fieryness) , sendTime);
    }

    @Override
    public void setFields(ExtinguishedBuilding extinguishedBuilding) {
        this.buildingIndex = IDConverter.getBuildingKey(extinguishedBuilding.getBuildingID());
        this.fieryness = extinguishedBuilding.getFieryness();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
        properties.put(PropertyTypes.Fieryness, new FierynessProperty(fieryness));
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
        setMessageType(MessageTypes.ExtinguishedBuilding);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }

    @Override
    public boolean equals(Object message) {
        if (!(message instanceof ExtinguishedBuildingMessage) && !(message instanceof BurningBuildingMessage)) {
            return false;
        } else if (message instanceof ExtinguishedBuildingMessage) {
            return (getPropertyValues().get(PropertyTypes.BuildingIndex).equals(((ExtinguishedBuildingMessage) message).getPropertyValues().get(PropertyTypes.BuildingIndex)));
        } else {
            return (getPropertyValues().get(PropertyTypes.BuildingIndex).equals(((BurningBuildingMessage) message).getPropertyValues().get(PropertyTypes.BuildingIndex)));
        }
    }

    @Override
    public int hashCode() {
        return getPropertyValues().get(PropertyTypes.BuildingIndex);
    }
}
