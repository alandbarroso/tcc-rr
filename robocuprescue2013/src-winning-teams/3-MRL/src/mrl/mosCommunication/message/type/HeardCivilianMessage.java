package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.HeardCivilian;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 7:12 PM
 * Author: Mostafa Movahedi
 */
public class HeardCivilianMessage extends AbstractMessage<HeardCivilian> {
    int civilianID;
    int locationX;
    int locationY;

    public HeardCivilianMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(20);
    }

    public HeardCivilianMessage(HeardCivilian heardCivilian) {
        super(heardCivilian);
        setDefaultSayTTL(20);
        setSayTTL();
    }

    public HeardCivilianMessage() {
        super();
        setDefaultSayTTL(20);
        setSayTTL();
        createProperties();
    }

    @Override
    public HeardCivilian read(int sendTime) {
        EntityID civilianID = new EntityID(propertyValues.get(PropertyTypes.HumanID));
        int locationX = propertyValues.get(PropertyTypes.LocationX);
        int locationY = propertyValues.get(PropertyTypes.LocationY);
        return new HeardCivilian(civilianID, locationX, locationY,sendTime);
    }

    @Override
    protected void setFields(HeardCivilian heardCivilian) {
        civilianID = heardCivilian.getCivilianID().getValue();
        locationX = heardCivilian.getLocationX();
        locationY = heardCivilian.getLocationY();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.HumanID, new HumanIDProperty(civilianID));
        properties.put(PropertyTypes.LocationX, new LocationXProperty(locationX));
        properties.put(PropertyTypes.LocationY, new LocationYProperty(locationY));
    }

    @Override
    protected void setSendTypes() {
//        sendTypes.add(SendType.Say); //todo.........
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
        setMessageType(MessageTypes.HeardCivilian);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }
}
