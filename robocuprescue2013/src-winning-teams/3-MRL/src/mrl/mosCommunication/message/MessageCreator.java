package mrl.mosCommunication.message;

import mrl.common.BenchMark.messaging.CommunicationType;
import mrl.common.BenchMark.messaging.MessageLogProperty;
//import mrl.common.BenchMark.messaging.MessagingLogManager;
import mrl.common.BenchMark.messaging.TransmitType;
import mrl.common.MRLConstants;
import mrl.mosCommunication.message.property.SendType;
import mrl.mosCommunication.message.type.AbstractMessage;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Aug 26, 2010
 * Time: 11:53:50 AM
 * To change this template use File | Settings | File Templates.
 */


public class MessageCreator {
    protected Message msg;
    public List<AbstractMessage> messages = new ArrayList<AbstractMessage>();
    public MessageCreator(int len){
        msg = new Message(len);
    }

    public void create(List<AbstractMessage> messages/*, MessagingLogManager sendMessageLogManager, MessagingLogManager detailedMessageLogManager*/, int time, SendType sendType, TransmitType transmitType) {
        if (messages != null)
            this.messages = messages;
        Collections.sort(this.messages, new MessageComparator());
        for (AbstractMessage message : this.messages) {
            if (!message.write(msg, sendType)) {
                System.out.println(message.getMessageType().name() + " : Message write Error");
            } else {
                if (MRLConstants.MYSQL_DEBUG_MESSAGING) {
//                    MessageLogProperty logProperty = new MessageLogProperty(message, time, sendType, transmitType);
//                    sendMessageLogManager.appendProperty(logProperty);
//                    detailedMessageLogManager.appendProperty(logProperty);
                }
            }
        }
    }

    public Message getMessage(){
        return msg;
    }
}
