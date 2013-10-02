/*
 * This file is part of RoboAKUT Project.
 *
 * RoboAKUT Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RoboAKUT Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RoboAKUT Project.  If not, see <http://www.gnu.org/licenses/>
 *
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mrl.communication;

import mrl.communication.messages.*;
import mrl.communication.messages.ambulanceMessages.*;
import mrl.communication.messages.fireBrigadeMessages.FBGoToClusterMessage;
import mrl.communication.messages.fireBrigadeMessages.FireBrigadePriorityMessage;
import mrl.communication.messages.fireBrigadeMessages.WaterMessage;
import mrl.communication.messages.policeMessages.PoliceBidMessage;
import mrl.communication.property.MessageProperty;
import mrl.communication.property.Priority;
import mrl.communication.property.PropertyName;
import mrl.communication.property.Receivers;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Human;

/**
 * @author murat
 *         edited by Mostafa Shabani.
 *         Date: Jan 3, 2011
 *         Time: 5:01:32 PM
 */
public enum MessageType {

    PacketHeader(Header.class),
    ChannelScannerMessage(ChannelScannerMessage.class),

    AgentInfo(AgentInfoMessage.class),
    BuriedAgent(BuriedAgentMessage.class),
    CLBuriedAgent(CLBuriedAgentMessage.class),
    StuckAgent(StuckAgentMessage.class),
    CLStuckAgent(CLStuckAgentMessage.class),

    VisitedBuilding(VisitedBuildingMessage.class),
    BurningBuilding(BurningBuildingMessage.class),
    ExtinguishedBuilding(ExtinguishedBuildingMessage.class),

    CivilianSeen(CivilianSeenMessage.class),
    HeardCivilian(HeardCivilianMessage.class),
    RescuedCivilian(RescuedCivilianMessage.class),

    ClearedRoad(ClearedRoadMessage.class),
    ClearedPath(ClearedPathMessage.class),

    TargetToGo(TargetToGoMessage.class),
    Block(BlockedRoadMessage.class),
    ImpassableNode(ImpassableNodeMessage.class),
    StartRescuingCivilianMessage(StartRescuingCivilianMessage.class),
    TransportingCivilianMessage(TransportingCivilianMessage.class),
    ValueFunctionMessage(ValueFunctionMessage.class),
    AmbulanceCivilianBid(AmbulanceCivilianBidMessage.class),
    AmbulanceAgentBid(AmbulanceAgentBidMessage.class),
    AmbulanceLeaderBid(AmbulanceLeaderBidMessage.class),
    CurrentRescuingCivilianMessage(CurrentRescuingCivilianMessage.class),
    LoaderMessage(LoaderMessage.class),
    PoliceBid(PoliceBidMessage.class),
    AmbulanceCivilianTask(AmbulanceCivilianTaskMessage.class),
    AmbulanceAgentTask(AmbulanceAgentTaskMessage.class),
    Water(WaterMessage.class),
    FireBrigadePriority(FireBrigadePriorityMessage.class),
    GoToCluster(FBGoToClusterMessage.class),

    FullBuilding(CLFullBuildingMessage.class),
    EmptyBuilding(EmptyBuildingMessage.class);


    private Class<? extends Message> messageClass_;


    private MessageType(Class<? extends Message> messageClass) {
        messageClass_ = messageClass;
    }

    public Class<? extends Message> getMessageClass() {
        return messageClass_;
    }

    public Priority getPriority() {
        Message message = newInstance();
        if (message != null) {
            return message.getPriority();
        } else {
            return Priority.Low;
        }
    }

    public Receivers getReceivers(Human self) {
        Message message = newInstance();
        if (message != null) {
            return message.getReceivers(self);
        } else {
            return Receivers.All;
        }
    }

    public int getByteArraySize() {
        Message message = newInstance();
        if (message != null) {
            return message.getMessageByteArraySize();
        } else {
            return 8;// 8 is average 
        }
    }

    /**
     * Returns a new message instance of associated type
     *
     * @return message instance
     */
    public Message newInstance() {
        Message message = null;
        try {
            message = messageClass_.newInstance();
        } catch (Exception ex) {
            Logger.error(name(), ex);
        }
        return message;
    }

    /**
     * Returns the messageType instance associated with the given class
     *
     * @param messageClass the class of message
     * @return the message type
     */
    public static MessageType getMessageType(Class<Message> messageClass) {
        for (MessageType type : MessageType.values()) {
            if (type.getMessageClass() == messageClass) {
                return type;
            }
        }
        return null;
    }

    public static MessageType getMessageType(byte[] messageByteArray, int index) {
        int typeIndex = messageByteArray[index];

        return getMessageType(typeIndex);
    }

    public static MessageType getMessageType(int typeIndex) {
        MessageType type;

        if (typeIndex < values().length) {
            type = values()[typeIndex];
        } else {
            type = null;
        }
        return type;
    }

    public static MessageProperty getProperty() {
        MessageProperty property = new MessageProperty(PropertyName.MessageType, 1);
        return property;
    }
}
