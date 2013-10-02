/*
package mrl.common.BenchMark.messaging;

import Manager.LogManager;
import mrl.communication.Packet;
import mrl.mosCommunication.entities.MessageEntity;
import mrl.mosCommunication.message.property.SendType;
import mrl.mosCommunication.message.type.AbstractMessage;
import mrl.world.MrlWorld;
import mysql.HeaderField;
import mysql.MySQL;
import mysql.Row;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

*/
/**
 * @author Mahdi
 *//*

public abstract class MessagingLogManager extends LogManager {
    protected MrlWorld world;
    protected List<HeaderField> headerFields;
    protected static MySQL mySQL = new MySQL();
    protected List<MessageLogProperty> propertyList;

    static {
        mySQL = new MySQL("localhost","root","254136");
        mySQL.connect("RescueSim");
    }

    protected MessagingLogManager(MrlWorld mrlWorld) {
        this.world = mrlWorld;
        this.headerFields = Collections.synchronizedList(new ArrayList<HeaderField>());
        this.propertyList = Collections.synchronizedList(new ArrayList<MessageLogProperty>());

        HeaderField id = new HeaderField("id", "INT NOT NULL AUTO_INCREMENT", -1);
        headerFields.add(id);
    }

    protected void addBaseColumns(Row row) {

    }

//    public void appendProperties(Collection<AbstractMessage> messages,int time, CommunicationType communicationType, TransmitType transmitType) {
//        for (AbstractMessage abstractMessageClass : messages) {
//            appendProperty(abstractMessageClass,time, communicationType, transmitType);
//        }
//    }

    public void appendProperty(AbstractMessage message,int time, SendType sendType, TransmitType transmitType) {
        appendProperty(new MessageLogProperty(message,time, sendType, transmitType));
    }

    public void appendProperty(MessageLogProperty packetLogProperty) {
        propertyList.add(packetLogProperty);
    }

}
*/
