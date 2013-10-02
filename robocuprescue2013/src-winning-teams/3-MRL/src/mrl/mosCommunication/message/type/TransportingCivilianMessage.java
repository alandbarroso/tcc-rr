package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.TransportingCivilian;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 3:43 PM
 * Author: Mostafa Movahedi
 */
public class TransportingCivilianMessage extends AbstractMessage<TransportingCivilian> {
    int humanID;

    public TransportingCivilianMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(20);
    }

    public TransportingCivilianMessage(TransportingCivilian transportingCivilian) {
        super(transportingCivilian);
        setDefaultSayTTL(20);
        setSayTTL();
    }

    public TransportingCivilianMessage() {
        super();
        setDefaultSayTTL(20);
        setSayTTL();
        createProperties();
    }

    @Override
    public TransportingCivilian read(int sendTime) {
        return new TransportingCivilian(new EntityID(propertyValues.get(PropertyTypes.HumanID)),sendTime);
    }

    @Override
    protected void setFields(TransportingCivilian transportingCivilian) {
        this.humanID = transportingCivilian.getHumanID().getValue();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.HumanID, new HumanIDProperty(humanID));
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
        setMessageType(MessageTypes.TransportingCivilian);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }
}
