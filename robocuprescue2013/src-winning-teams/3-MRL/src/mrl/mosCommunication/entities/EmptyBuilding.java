package mrl.mosCommunication.entities;

import mrl.mosCommunication.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 1:14 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class EmptyBuilding extends MessageEntity {
    EntityID buildingID;

    public EmptyBuilding(EntityID buildingID, int sendTime) {
        super(sendTime);
        this.buildingID = buildingID;
    }

    public EntityID getBuildingID() {
        return buildingID;
    }

    public void setBuildingID(EntityID buildingID) {
        this.buildingID = buildingID;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.EmptyBuilding);
    }
}
