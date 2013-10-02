package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.Loader;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 3:23 PM
 * Author: Mostafa Movahedi
 */
public class LoaderMessage extends AbstractMessage<Loader> {
    int humanID;

    public LoaderMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(20);
    }

    public LoaderMessage(Loader loader) {
        super(loader);
        setDefaultSayTTL(20);
        setSayTTL();
    }

    public LoaderMessage() {
        super();
        setDefaultSayTTL(20);
        setSayTTL();
        createProperties();
    }

    @Override
    public Loader read(int sendTime) {
        return new Loader(new EntityID(propertyValues.get(PropertyTypes.HumanID)),sendTime);
    }

    @Override
    protected void setFields(Loader loader) {
        this.humanID = loader.getHumanID().getValue();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.HumanID, new HumanIDProperty(humanID));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
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
        setMessageType(MessageTypes.Loader);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }
}
