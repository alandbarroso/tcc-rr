package mrl.common.BenchMark.messaging;

import mrl.mosCommunication.message.property.SendType;
import mrl.mosCommunication.message.type.AbstractMessage;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Mahdi
 */
public class MessageLogProperty {
    private AbstractMessage message;
    private EntityID sender;
    private SendType sendType;
    private TransmitType transmitType;
    private int time;

    public MessageLogProperty(AbstractMessage message, int time, SendType sendType, TransmitType transmitType) {
        this.time = time;
        this.message = message;
        this.sender = message.getSender();
        this.sendType = sendType;
        this.transmitType = transmitType;
    }

    public AbstractMessage getMessage() {
        return message;
    }

    public EntityID getSender() {
        return sender;
    }

    public SendType getSendType() {
        return sendType;
    }

    public TransmitType getTransmitType() {
        return transmitType;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
