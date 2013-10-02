/*
package mrl.common.BenchMark;

import mrl.world.MrlWorld;
//import mysql.*;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

*/
/**
 * @author Mahdi
 *//*

public class TotalCivilianLogManager extends RescueSimLogManager {
    private static Set<StandardEntity> TOTAL_CIVILIANS;
    private static int LAST_CYCLE_EXECUTE = 0;

    static {
        TOTAL_CIVILIANS = new HashSet<StandardEntity>();
    }

    public TotalCivilianLogManager(MrlWorld mrlWorld) {
        super(mrlWorld);
        String totalCivilianLogFields = "in_building:mediumint.8," +//5
                "buried:mediumint.8," +//6
                "healthy:mediumint.8," +//7
                "undefined_position:mediumint.8," +//8
                "total:mediumint.8," +//9
                "team:varchar.10";//10
        headerFields.addAll(convertToFields(totalCivilianLogFields));
        Table table = new Table("total_civilian_log", headerFields, headerFields.get(0));
        logger = new MySQLLogger(mySQL, table);
//        prepare();
    }

    @Override
    public void execute() {
        if (world.getTime() <= LAST_CYCLE_EXECUTE) {
            return;
        }
        LAST_CYCLE_EXECUTE = world.getTime();
        TOTAL_CIVILIANS.addAll(world.getCivilians());
        int totalCivilian = TOTAL_CIVILIANS.size();
        int inBuildings = 0, buriedCount = 0, healthy = 0, undefinedPositions = 0;
        Civilian civilian;
        try {
            for (StandardEntity civEntity : TOTAL_CIVILIANS) {
                civilian = (Civilian) civEntity;
                if (civilian.isPositionDefined()) {
                    StandardEntity position = world.getEntity(civilian.getPosition());
                    if (position instanceof Building && !(position instanceof Refuge)) {
                        inBuildings++;
                    }
                    if (civilian.isBuriednessDefined() && civilian.getBuriedness() == 0) {
                        healthy++;
                    } else {
                        buriedCount++;
                    }
                } else {
                    undefinedPositions++;
                }
            }

        } catch (ConcurrentModificationException ignored) {
        }
        log(makeRow(inBuildings, buriedCount, healthy, undefinedPositions, totalCivilian));
    }

    private Row makeRow(int inBuildings, int buriedCount, int healthy, int undefinedPositions, int totalCivilian) {
        Row row = new Row();
        addBaseColumns(row);
        Field field = new IntegerField(headerFields.get(5), inBuildings);
        row.addField(field);
        field = new IntegerField(headerFields.get(6), buriedCount);
        row.addField(field);
        field = new IntegerField(headerFields.get(7), healthy);
        row.addField(field);
        field = new IntegerField(headerFields.get(8), undefinedPositions);
        row.addField(field);
        field = new IntegerField(headerFields.get(9), totalCivilian);
        row.addField(field);
        field = new StringField(headerFields.get(10), TEAM_NAME);
        row.addField(field);
        return row;
    }


}
*/
