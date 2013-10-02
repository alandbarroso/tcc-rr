package mrl.common.BenchMark.messaging;

import mrl.communication.Packet;
import mrl.mosCommunication.entities.MessageEntity;
import mrl.mosCommunication.message.type.AbstractMessage;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Mahdi
 */
public class PacketLogProperty {
//    private Packet packet;
    private EntityID sender;
    private CommunicationType communicationType;
    private TransmitType transmitType;
    private MessageEntity message;

//    public PacketLogProperty(Packet packet, EntityID sender, CommunicationType comType, TransmitType transmitType) {
//        this.packet = packet;
//        this.sender = sender;
//        this.communicationType = comType;
//        this.transmitType = transmitType;
//    }

    public PacketLogProperty(MessageEntity message, CommunicationType communicationType, TransmitType transmitType) {
                this.message = message;
        this.sender = message.getSender();
        this.communicationType = communicationType;
        this.transmitType = transmitType;
    }

    public MessageEntity getMessage() {
        return message;
    }

    public EntityID getSender() {
        return sender;
    }

    public CommunicationType getCommunicationType() {
        return communicationType;
    }

    public TransmitType getTransmitType() {
        return transmitType;
    }
}

