package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.ClearedPath;
import mrl.mosCommunication.message.IDConverter;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/23/13
 * Time: 3:54 PM
 * @Author: Mostafa Movahedi
 */
public class ClearedPathMessage extends AbstractMessage<ClearedPath> {
    int pathIndex;
    public ClearedPathMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(1);
    }

    public ClearedPathMessage(ClearedPath clearedPath) {
        super(clearedPath);
        setDefaultSayTTL(1);
        setSayTTL();
    }

    public ClearedPathMessage() {
        super();
        setDefaultSayTTL(1);
        setSayTTL();
        createProperties();
    }

    @Override
    public ClearedPath read(int sendTime) {
        return new ClearedPath(IDConverter.getRoadID(propertyValues.get(PropertyTypes.RoadIndex)),sendTime);
    }

    @Override
    protected void setFields(ClearedPath clearedPath) {
        pathIndex = IDConverter.getRoadKey(clearedPath.getPathID());
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.RoadIndex, new RoadIndexProperty(pathIndex));
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
        setMessageType(MessageTypes.ClearedPath);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }
}
