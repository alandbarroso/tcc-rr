/*
package mrl.common.BenchMark.messaging;

import Base.Log;
import mrl.mosCommunication.message.type.AbstractMessage;
import mrl.world.MrlWorld;
import mysql.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

*/
/**
 * @author Mahdi
 *//*

public class ReceiveMessageLogManager extends MessagingLogManager {
    protected static final String RECEIVE_LOG_TABLE = "receive_log";

    */
/**
     * fields which will be add in mysql table with valid data type
     *//*

    private enum Fields {
        receiver(new HeaderField("receiver", "varchar", 50)),
        received_cycle(new HeaderField("received_cycle", "mediumint", 8)),
        sender(new HeaderField("sender", "varchar", 50)),
        //        sent_cycle(new HeaderField("sent_cycle", "mediumint", 8)),
        packet_type(new HeaderField("packet_type", "varchar", 40)),
        priority(new HeaderField("priority", "varchar", 10)),
        say_speak(new HeaderField("say_speak", "varchar", 20)),
        send_type(new HeaderField("send_type", "varchar", 20));

        private HeaderField headerField;

        Fields(HeaderField headerField) {
            this.headerField = headerField;
        }
    }

    */
/**
     *
     *//*

    public ReceiveMessageLogManager(MrlWorld mrlWorld) {
        super(mrlWorld);
//        String default_log_field = "receiver:varchar.30," +//1
//                "received_cycle:mediumint.8," +//2
//                "sender:varchar.30," +//3
//                "sent_cycle:mediumint.8," +//4
//                "packet_type:varchar.40," +//5
//                "priority:varchar.10," +//6
//                "say_speak:varchar.6," +//7
//                "send_type:varchar.20";//8
//        headerFields.addAll(convertToFields(default_log_field));

//        headerFields.add(Fields.packet_type.headerField);
        for (Fields field : Fields.values()) {
            headerFields.add(field.headerField);
        }

        //create a table for mysql logger
        Table table = new Table(RECEIVE_LOG_TABLE, headerFields, headerFields.get(0));
        logger = new MySQLLogger(mySQL, table);
        logger.clearLogs();
    }

    */
/**
     * execute logging with packets and other properties added before in property list  {#appendProperty}
     *//*

    @Override
    public void execute() {
//        Packet packet;
        List<Log> rows = new ArrayList<Log>();
        String say_speak;
        String priority;
        String sendType;
        EntityID sender;
        String receiver;
        String packetType;
//        int sentTime;
        int receivedTime;

//        for (PacketLogProperty property : propertyList) {
//            packet = property.getPacket();
//            sender = property.getSender();
//            say_speak = property.getCommunicationType().toString();
//            packetType = packet.getHeader().getPacketType().toString();
//            sentTime = packet.getHeader().getPacketCycle(world.getTime());
//            receiver = world.getSelf().toString();
//            receivedTime = world.getTime();
//            for (Message abstractMessageClass : packet) {
//                priority = abstractMessageClass.getPriority().toString();
//                sendType = abstractMessageClass.getSendType().toString();
//                rows.add(makeRow(receiver, receivedTime, sender.toString(), sentTime, packetType, priority, say_speak, sendType));
//            }
//        }
        AbstractMessage message;
        for (MessageLogProperty property : propertyList) {
//            packet = property.getPacket();
            message = property.getMessage();
            sender = property.getSender();
            say_speak = property.getSendType().toString();
            packetType = message.getMessageType().name();
//            sentTime = property.getTime();
            receiver = world.getSelf().toString();
            receivedTime = property.getTime();//world.getTime();
//            for (Message abstractMessageClass : packet) {
            priority = String.valueOf(message.getPriority());
            sendType = "default";
            rows.add(makeRow(receiver, receivedTime, sender.toString(), packetType, priority, say_speak, sendType));
//            }
        }

        propertyList.clear();

        //now add logs in database
        multipleLog(rows);
    }

    */
/**
     * Make a data row for log it in data table.
     *//*

    protected Row makeRow(String receiver, int receivedTime, String sender, String packetType, String priority, String say_speak, String sendType) {
        Row row = new Row();
        row.setTableName(RECEIVE_LOG_TABLE);
        addBaseColumns(row);
        row.addField(new StringField(Fields.receiver.headerField, receiver));
        row.addField(new IntegerField(Fields.received_cycle.headerField, receivedTime));
        row.addField(new StringField(Fields.sender.headerField, sender));
//        row.addField(new IntegerField(Fields.sent_cycle.headerField, sentTime));
        row.addField(new StringField(Fields.packet_type.headerField, packetType));
        row.addField(new StringField(Fields.priority.headerField, priority));
        row.addField(new StringField(Fields.say_speak.headerField, say_speak));
        row.addField(new StringField(Fields.send_type.headerField, sendType));

        return row;
    }

}
*/
