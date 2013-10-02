package mrl.mosCommunication.message;

import mrl.mosCommunication.message.type.AbstractMessage;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Sep 24, 2010
 * Time: 6:52:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageComparator implements Comparator<AbstractMessage> {

    @Override
    public int compare(AbstractMessage a, AbstractMessage b) {
        if(a.getPriority() > b.getPriority())
            return 1;
        if(a.getPriority() == b.getPriority())
            return 0;
        else
            return -1;
    }
}
