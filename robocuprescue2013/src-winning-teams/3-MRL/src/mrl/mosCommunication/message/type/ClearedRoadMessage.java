package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.ClearedRoad;
import mrl.mosCommunication.message.IDConverter;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 1:03 PM
 * Author: Mostafa Movahedi
 */
public class ClearedRoadMessage extends AbstractMessage<ClearedRoad> {
    int roadIndex;

    public ClearedRoadMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(1);
    }

    public ClearedRoadMessage(ClearedRoad clearedRoad) {
        super(clearedRoad);
        setDefaultSayTTL(1);
        setSayTTL();
    }

    public ClearedRoadMessage() {
        super();
        setDefaultSayTTL(1);
        setSayTTL();
        createProperties();
    }

    @Override
    public ClearedRoad read(int sendTime) {
        return new ClearedRoad(IDConverter.getRoadID(propertyValues.get(PropertyTypes.RoadIndex)),sendTime);
    }

    @Override
    public void setFields(ClearedRoad clearedRoad) {
        this.roadIndex = IDConverter.getRoadKey(clearedRoad.getRoadID());
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.RoadIndex, new RoadIndexProperty(roadIndex));
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
        setMessageType(MessageTypes.ClearedRoad);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }
}
