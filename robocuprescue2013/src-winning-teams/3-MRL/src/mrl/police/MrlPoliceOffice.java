package mrl.police;

import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.communication.Packet;
import mrl.platoon.MrlCentre;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.EnumSet;

/**
 * A sample centre agent.
 */
public class MrlPoliceOffice extends MrlCentre {
    @Override
    public String toString() {
        return "MRL PoliceOffice ID: " + this.getID().getValue();
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        super.think(time, changed, heard);
    }

    @Override
    public void act() throws CommandException, TimeOutException {

    }

    @Override
    public void processMessage(Packet packet, EntityID sender) {

    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.POLICE_OFFICE);
    }
}