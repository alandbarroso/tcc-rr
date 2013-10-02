/*
package mrl.common.BenchMark;

import Manager.LogManager;
import mrl.ambulance.MrlAmbulanceTeam;
import mrl.firebrigade.MrlFireBrigade;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.MrlPoliceForce;
import mrl.world.MrlWorld;
import mysql.*;

import java.util.ArrayList;
import java.util.List;

*/
/**
 * @author Mahdi
 *//*

public abstract class RescueSimLogManager extends LogManager {
    protected MrlWorld world;
    protected List<HeaderField> headerFields;
    protected static final String MAP_NAME = "mexico1";
    protected static final String TEAM_NAME = "mrl";
    protected MySQL mySQL;

    protected RescueSimLogManager(MrlWorld mrlWorld) {
        this.world = mrlWorld;
        mySQL = new MySQL();
        mySQL.connect("RescueSim");
        headerFields = new ArrayList<HeaderField>();
        HeaderField id = new HeaderField("id", "INT NOT NULL AUTO_INCREMENT", -1);
        headerFields.add(id);
        String default_log_field = "map:varchar.15," +//1
                "agent_type:varchar.3," +//2
                "agent_id:int.10," +//3
                "cycle:mediumint.8";//4
        headerFields.addAll(convertToFields(default_log_field));
    }

    protected void addBaseColumns(Row row) {
        String myType = "";
        MrlPlatoonAgent platoonAgent = world.getPlatoonAgent();
        if (platoonAgent instanceof MrlPoliceForce) {
            myType = "pf";
        } else if (platoonAgent instanceof MrlFireBrigade) {
            myType = "fb";
        } else if (platoonAgent instanceof MrlAmbulanceTeam) {
            myType = "at";
        }

        Field field = new StringField(headerFields.get(1), MAP_NAME);
        row.addField(field);
        field = new StringField(headerFields.get(2), myType);
        row.addField(field);
        field = new IntegerField(headerFields.get(3), world.getPlatoonAgent().getID().getValue());
        row.addField(field);
        field = new IntegerField(headerFields.get(4), world.getTime());
        row.addField(field);
    }

}
*/
