package mrl.mosCommunication.message.type;

import mrl.mosCommunication.entities.NOP;
import mrl.mosCommunication.message.Message;
import mrl.mosCommunication.message.property.SendType;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Oct 22, 2010
 * Time: 8:40:09 AM
 */

/**
 * this is a fake message type that use to understand there is no message after it in message byte array.
 * you can see usage of this type in {@link mrl.mosCommunication.message.MessageParser}
 * <br/><br/><b>Note:</b> in {@link mrl.mosCommunication.message.type.MessageTypes} NOP must be the first item.
 */
public class NOPMessage extends AbstractMessage<NOP> {

    public NOPMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(1);
    }

    public NOPMessage() {
        super();
        setDefaultSayTTL(1);
        setSayTTL();
        createProperties();
    }

    public NOPMessage(NOP nop) {
        setDefaultSayTTL(1);
        setSayTTL();
    }

    @Override
    public NOP read(int sendTime) {
        return null;
    }

    @Override
    public void setFields(NOP nop) {}

    @Override
    protected void createProperties() {
        //there is no property for this type of abstractMessageClass
    }

    @Override
    protected void setSendTypes() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void setReceivers() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void setChannelConditions() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.NOP);
    }
}
