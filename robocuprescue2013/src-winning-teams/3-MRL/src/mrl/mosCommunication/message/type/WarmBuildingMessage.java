package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.WarmBuilding;
import mrl.mosCommunication.message.IDConverter;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/23/13
 * Time: 4:13 PM
 *
 * @Author: Mostafa Movahedi
 */
public class WarmBuildingMessage extends AbstractMessage<WarmBuilding> {
    int buildingIndex;

    public WarmBuildingMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(20);
    }

    public WarmBuildingMessage(WarmBuilding warmBuilding) {
        super(warmBuilding);
        setDefaultSayTTL(20);
        setSayTTL();
    }

    public WarmBuildingMessage() {
        super();
        setDefaultSayTTL(20);
        setSayTTL();
        createProperties();
    }

    @Override
    public WarmBuilding read(int sendTime) {
        return new WarmBuilding(IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex)),sendTime);
    }

    @Override
    protected void setFields(WarmBuilding warmBuilding) {
        buildingIndex = IDConverter.getBuildingKey(warmBuilding.getBuildingID());
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
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
        setMessageType(MessageTypes.WarmBuilding);
    }
}
