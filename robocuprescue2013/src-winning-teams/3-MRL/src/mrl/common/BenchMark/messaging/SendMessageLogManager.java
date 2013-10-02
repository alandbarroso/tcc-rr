/*
package mrl.common.BenchMark.messaging;

import Base.Log;
import mrl.mosCommunication.message.type.AbstractMessage;
import mrl.world.MrlWorld;
import mysql.*;

import java.util.ArrayList;
import java.util.List;

*/
/**
 * @author Mahdi
 *//*

public class SendMessageLogManager extends MessagingLogManager {
    protected static final String SEND_LOG_TABLE = "send_log";
    */
/**
     * fields which will be add in mysql table with valid data type <br />
     * <b>Note:</b> primary key will added in header fields with name "id" in super class constructor.
     *//*

    private enum Fields {
        sender(new HeaderField("sender", "varchar", 30)),
        sent_cycle(new HeaderField("sent_cycle", "mediumint", 8)),
        packet_type(new HeaderField("packet_type", "varchar", 40)),
        priority(new HeaderField("priority", "varchar", 10)),
        say_speak(new HeaderField("say_speak", "varchar", 20)),
        send_type(new HeaderField("send_type", "varchar", 20)),
        say_ttl(new HeaderField("say_ttl", "int", 4)),
        message_bitSize(new HeaderField("message_bitSize", "int", 4));

        private HeaderField headerField;

        Fields(HeaderField headerField) {
            this.headerField = headerField;
        }
    }

    public SendMessageLogManager(MrlWorld mrlWorld) {
        super(mrlWorld);
        for (Fields field : Fields.values()) {
            headerFields.add(field.headerField);
        }

        Table table = new Table(SEND_LOG_TABLE, headerFields, headerFields.get(0));
        logger = new MySQLLogger(mySQL, table);
        logger.clearLogs();
    }

    @Override
    public void execute() {
//        Packet packet;
        List<Log> rows = new ArrayList<Log>();
        int sentTime;
        String sender;
        String packetType;
        String say_speak;
        String priority;
        String sendType;
        int ttl;
        int messageBitSize;

        //add messages with they properties into separated log
        //each log is one different row in  database table
        AbstractMessage message;
        for (MessageLogProperty property : propertyList) {//iterate in packets
            message = property.getMessage();
//            packet = property.getPacket();
            sender = world.getEntity(property.getSender()).toString();
            say_speak = property.getSendType().toString();
            packetType = message.getMessageType().name();
            sentTime = property.getTime();
            ttl = message.getSayTTL();
            messageBitSize = message.getMessageBitSize(property.getSendType());
            priority = String.valueOf(message.getPriority());
            sendType = "default";
            rows.add(makeRow(sender, sentTime, packetType, priority, say_speak, sendType, ttl, messageBitSize));
        }

        propertyList.clear();

        //now add logs in database
        multipleLog(rows);
    }

    protected Row makeRow(String sender, int sentTime, String packetType, String priority, String say_speak, String sendType, int ttl, int messageBitSize ) {
        Row row = new Row();
        row.setTableName(SEND_LOG_TABLE);
        addBaseColumns(row);
//        row.addField(new IntegerField(headerFields.get(1), senderID));
//        row.addField(new IntegerField(headerFields.get(2), sentTime));
//        row.addField(new StringField(headerFields.get(3), packetType));
//        row.addField(new StringField(headerFields.get(4), priority));
//        row.addField(new StringField(headerFields.get(5), say_speak));
//        row.addField(new StringField(headerFields.get(6), sendType));
        row.addField(new StringField(Fields.sender.headerField, sender));
        row.addField(new IntegerField(Fields.sent_cycle.headerField, sentTime));
        row.addField(new StringField(Fields.packet_type.headerField, packetType));
        row.addField(new StringField(Fields.priority.headerField, priority));
        row.addField(new StringField(Fields.say_speak.headerField, say_speak));
        row.addField(new StringField(Fields.send_type.headerField, sendType));
        row.addField(new IntegerField(Fields.say_ttl.headerField, ttl));
        row.addField(new IntegerField(Fields.message_bitSize.headerField, messageBitSize));
        return row;
    }
}
*/
