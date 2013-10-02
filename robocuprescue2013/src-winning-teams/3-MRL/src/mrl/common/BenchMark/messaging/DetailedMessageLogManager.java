/*
package mrl.common.BenchMark.messaging;

import Base.Log;
import mrl.mosCommunication.message.property.PropertyTypes;
import mrl.mosCommunication.message.property.SendType;
import mrl.mosCommunication.message.type.AbstractMessage;
import mrl.mosCommunication.message.type.MessageTypes;
import mrl.world.MrlWorld;
import mysql.*;
import rescuecore2.misc.Pair;

import java.util.*;

*/
/**
 * @author Mahdi
 *//*

public class DetailedMessageLogManager extends MessagingLogManager {
    private static Map<MessageTypes, Pair<MySQLLogger, List<HeaderField>>> messageTypeLoggerMap = Collections.synchronizedMap(new HashMap<MessageTypes, Pair<MySQLLogger, List<HeaderField>>>());
    private List<MessageLogProperty> properties;
    private static final String SENDER_KEY = "sender";
    private static final String RECEIVER_KEY = "receiver";
    private static final String PRIMARY_KEY = "id";
    private static final String TRANSMIT_KEY = "transmit";
    private static final String COMMUNICATION_KEY = "com";
    private static final String SENT_TIME = "sent_time";
    private static final String RECEIVE_TIME = "receive_time";
    private static final String SAY_TTL = "say_ttl";

    public DetailedMessageLogManager(MrlWorld mrlWorld) {
        super(mrlWorld);
        this.properties = Collections.synchronizedList(new ArrayList<MessageLogProperty>());
    }

    @Override
    public void execute() {
        MySQLLogger messageLogger = null;
        List<Log> logs = new ArrayList<Log>();
        MessageTypes messageType;
        Pair<MySQLLogger, List<HeaderField>> pair;
        Table table;
        List<HeaderField> headerFields;
        AbstractMessage message;

        for (MessageLogProperty property : properties) {
            message = property.getMessage();
            messageType = message.getMessageType();
            if (messageTypeLoggerMap.containsKey(messageType)) {
                pair = messageTypeLoggerMap.get(messageType);
                messageLogger = pair.first();
                headerFields = pair.second();
            } else {
                table = createTable(property);
                messageTypeLoggerMap.put(messageType, new Pair<MySQLLogger, List<HeaderField>>(new MySQLLogger(mySQL, table), table.getHeaders()));
                pair = messageTypeLoggerMap.get(messageType);
                messageLogger = pair.first();
                headerFields = pair.second();
                messageLogger.clearLogs();
            }
            logs.add(makeRow(property, headerFields));
        }

        if (messageLogger != null && !logs.isEmpty()) {
            messageLogger.addMultipleLog(logs);
        }

        this.properties.clear();
    }

    private Row makeRow(MessageLogProperty logProperty, List<HeaderField> headerFields) {
        Row row = new Row();
        AbstractMessage message = logProperty.getMessage();
        row.setTableName(message.getMessageType().name());
        for (HeaderField headerField : headerFields) {
            String name = headerField.getName();
            String value = "";
            if (name.equals(SENDER_KEY)) {
                value = world.getEntity(logProperty.getSender()).toString();
            } else if (name.equals(RECEIVER_KEY)) {
                if (logProperty.getTransmitType().equals(TransmitType.RECEIVE) || logProperty.getTransmitType().equals(TransmitType.EMERGENCY_SEND))
                    value = world.getSelf().toString();
                else
                    value = "-";
            } else if (name.equals(PRIMARY_KEY)) {
                //do nothing
            } else if (name.equals(TRANSMIT_KEY)) {
                value = logProperty.getTransmitType().toString();
            } else if (name.equals(COMMUNICATION_KEY)) {
                //todo not handled yet
                value = logProperty.getSendType().toString();
            } else if (name.equals(SENT_TIME)) {
                value = String.valueOf(logProperty.getMessage().getSendTime(logProperty.getSendType(), logProperty.getTime()));
            } else if (name.equals(RECEIVE_TIME)) {
                value = String.valueOf(logProperty.getTime());
            } else if (name.equals(SAY_TTL)) {
                value = String.valueOf(logProperty.getMessage().getSayTTL());
            */
/*else if (name.equals("BuildingIndex")) {
                Object obj = message.getPropertyValues().get(PropertyTypes.BuildingIndex);
                int index = obj!=
                Integer integer = ;
                value = IDConverter.getBuildingID(integer).toString();
            } else if (name.equals("PositionIndex")) {
                EntityID id = null;
                int index = message.getPropertyValues().get(PropertyTypes.PositionType);
                PositionTypes positionType = PositionTypes.indexToEnum(index);
                switch (positionType) {
                    case Building:
                        id = IDConverter.getBuildingID(Integer.valueOf(message.getPropertyValues().get(PropertyTypes.PositionIndex).toString()));
                        break;
                    case Road:
                        id = IDConverter.getRoadID(Integer.valueOf(message.getPropertyValues().get(PropertyTypes.PositionIndex).toString()));
                        break;
                }
                value = (id != null ? id.toString() : "null");
            } *//*

            } else {
                value = String.valueOf(message.getPropertyValues().get(PropertyTypes.valueOf(name)));
//                value = getPropertyValue(abstractMessageClass.getProperties(), name);
            }
            if (value.equals("")) {
                //this abstractMessageClass have no property that initiated in header header fields
                continue;
            }
            row.addField(new StringField(headerField, String.valueOf(value)));
        }

        return row;
    }

    */
/**
     * get abstractMessageClass property value from list of properties
     *
     * @param propertyTypes list of abstractMessageClass property
     * @param propertyName      string name of property name that need to find value of it in abstractMessageClass properties
     * @return return value of property
     *//*

//    private String getPropertyValue(Collection<PropertyTypes> propertyTypes , Collection<Integer> propertyValues, String propertyName) {
//
//        Integer intValue = -1;
//        String value = "";
//        for (PropertyTypes messageProperty : propertyTypes) {
//            propertyName
//
//            if (messageProperty.getPropertyName().toString().equals(propertyName)) {
//                intValue = messageProperty.getValue();
//                break;
//            }
//        }
//
//
//
//
//        for (MessageProperty messageProperty : messageProperties) {
//            if (messageProperty.getPropertyName().toString().equals(propertyName)) {
//                intValue = messageProperty.getValue();
//                break;
//            }
//        }
//        if (intValue >= 0) {
//            PropertyName property = PropertyName.valueOf(propertyName);
//            switch (property) {
//                case HumanID:
//                case AgentIdIndex:
//                    intValue += world.maxID;
//                    value = String.valueOf(intValue);
//                    break;
//                default:
//                    value = String.valueOf(intValue);
//                    break;
//            }
//        } else {
//            //this abstractMessageClass have not such property
//        }
//
//        return value;
//    }

    */
/**
     * create table with message properties
     *
     * @param messageLogProperty a message log property for create table from parameters of it
     * @return new table for create mysql table from it's fields
     *//*

    private Table createTable(MessageLogProperty messageLogProperty) {
        if (messageLogProperty == null) {
            return null;
        }

        AbstractMessage message = messageLogProperty.getMessage();

        List<HeaderField> headerFields = new ArrayList<HeaderField>();
        HeaderField id = new HeaderField(PRIMARY_KEY, "INT NOT NULL AUTO_INCREMENT", -1);
        headerFields.add(id);
        Set<PropertyTypes> propertyTypes = new HashSet<PropertyTypes>(message.getProperties().keySet());
        for (PropertyTypes property : propertyTypes) {
            headerFields.add(new HeaderField(property.name(), "varchar", 50));
        }
        //add abstractMessageClass sender field
        HeaderField senderField = new HeaderField(SENDER_KEY, "varchar", 40);
        headerFields.add(senderField);


        HeaderField receiver = new HeaderField(RECEIVER_KEY, "varchar", 50);
        headerFields.add(receiver);

        HeaderField transmitField = new HeaderField(TRANSMIT_KEY, "varchar", 8);
        headerFields.add(transmitField);

        HeaderField sentCycle = new HeaderField(SENT_TIME, "varchar", 4);
        headerFields.add(sentCycle);

        HeaderField receiveCycle = new HeaderField(RECEIVE_TIME, "varchar", 4);
        headerFields.add(receiveCycle);

        HeaderField ttl = new HeaderField(SAY_TTL, "varchar", 4);
        headerFields.add(ttl);


        return new Table(message.getMessageType().name(), headerFields, id);
    }

    protected void addMessageProperty(AbstractMessage message, int time, SendType sendType, TransmitType transmitType) {
        addMessageProperty(new MessageLogProperty(message, time, sendType, transmitType));
    }

    protected void addMessageProperty(MessageLogProperty messageLogProperty) {
        properties.add(messageLogProperty);
    }

    @Override
    public void appendProperty(AbstractMessage message, int time, SendType sendType, TransmitType transmitType) {
//        for (Message abstractMessageClass : packet) {
        addMessageProperty(new MessageLogProperty(message, time, sendType, transmitType));
//        }
    }

    @Override
    public void appendProperty(MessageLogProperty packetLogProperty) {
        appendProperty(packetLogProperty.getMessage(), packetLogProperty.getTime(), packetLogProperty.getSendType(), packetLogProperty.getTransmitType());
    }
}
*/
