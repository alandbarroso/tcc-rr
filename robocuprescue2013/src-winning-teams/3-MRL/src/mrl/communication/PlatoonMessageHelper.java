package mrl.communication;

import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by
 * User: mrl
 * Date: Mar 25, 2011
 * Time: 3:41:37 PM
 */
public class PlatoonMessageHelper {
    protected MrlWorld world;
    protected MessageManager messageManager;
    protected MrlPlatoonAgent platoonAgent;

    public PlatoonMessageHelper(MrlWorld world, MrlPlatoonAgent platoonAgent, MessageManager messageManager) {
        this.world = world;
        this.messageManager = messageManager;
        this.platoonAgent = platoonAgent;
    }


    public void sendTargetToGoMessage(EntityID id) {
//        TargetToGoMessage targetToGoMessage = new TargetToGoMessage(world.getIndexes().getAreaIndex(id));
//        messageManager.addMessageToQueue(targetToGoMessage, MessageType.TargetToGo);
    }
}
