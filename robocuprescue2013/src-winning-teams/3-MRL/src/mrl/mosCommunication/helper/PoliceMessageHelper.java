package mrl.mosCommunication.helper;

import mrl.mosCommunication.entities.ClearedPath;
import mrl.mosCommunication.entities.ClearedRoad;
import mrl.mosCommunication.entities.MessageEntity;
import mrl.mosCommunication.entities.RescuedCivilian;
import mrl.mosCommunication.message.MessageManager;
import mrl.mosCommunication.message.type.ClearedPathMessage;
import mrl.mosCommunication.message.type.ClearedRoadMessage;
import mrl.mosCommunication.message.type.RescuedCivilianMessage;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.MrlPoliceForceWorld;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 4:35 PM
 * Author: Mostafa Movahedi
 */
public class PoliceMessageHelper extends PlatoonMessageHelper {
    MrlPoliceForceWorld world;

    public PoliceMessageHelper(MrlPoliceForceWorld world, MrlPlatoonAgent platoonAgent, MessageManager messageManager) {
        super(world, platoonAgent, messageManager);
        this.world = world;
    }

    public void sendClearedRoadMessage(EntityID roadID) {
        messageManager.addMessage(new ClearedRoadMessage(new ClearedRoad(roadID,world.getTime())));
    }

    public void sendClearedPathMessage(EntityID pathID) {
        messageManager.addMessage(new ClearedPathMessage(new ClearedPath(pathID,world.getTime())));
    }

    public void processMessage(MessageEntity messageEntity) {

    }
}
