//package mrl.common.BenchMark;
//
//import mrl.world.MrlWorld;
////import mysql.*;
//import rescuecore2.standard.entities.Building;
//import rescuecore2.standard.entities.Civilian;
//import rescuecore2.standard.entities.StandardEntity;
//import rescuecore2.worldmodel.EntityID;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
///**
// * @author Mahdi
// */
//public class CivilianLogManager extends RescueSimLogManager {
//    Set<Civilian> civilianFounded;
//    Map<Civilian, Integer> heardCivilians;
//
//    public CivilianLogManager(MrlWorld mrlWorld) {
//        super(mrlWorld);
//        civilianFounded = new HashSet<Civilian>();
//        heardCivilians = new HashMap<Civilian, Integer>();
//        String civilian_log_fields = "civ_id:int.10," +//5
//                "heard_time:mediumint.8," +//6
//                "found_time:mediumint.8," +//7
//                "position:int.10," +//8
//                "in_building:tinyint.1,";//9
//        headerFields.addAll(convertToFields(civilian_log_fields));
//        Table table = new Table("mrl_civilian_log_VC1", headerFields);
//        logger = new MySQLLogger(mySQL, table);
//        prepare();
//    }
//
//    @Override
//    public void execute() {
//        Civilian civilian;
//        for (StandardEntity civEntity : world.getCivilians()) {
//            civilian = (Civilian) civEntity;
//            if (world.getChanges().contains(civEntity.getID())) {
//                if (!civilianFounded.contains(civilian)) {
//                    if (civilian.isPositionDefined()) {
//                        civilianFounded.add(civilian);
//                        int heard_time = -1;
//                        if (heardCivilians.containsKey(civilian)) {
//                            heard_time = heardCivilians.get(civilian);
//                        }
//                        log(makeRow(civilian, heard_time, world.getTime()));
//                    } else {
//
//                    }
//                }
//            } else if (world.getHeardCivilians().contains(civilian.getID())) {
//                heardCivilians.put(civilian, world.getTime());
//                log(makeRow(civilian, world.getTime(), -1));
//            }
//        }
//    }
//
//    private Row makeRow(Civilian civilian, int heard_time, int found_time) {
//        Row row = new Row();
//        addBaseColumns(row);
//        Field field = new IntegerField(headerFields.get(5), civilian.getID().getValue());
//        row.addField(field);
//        field = new IntegerField(headerFields.get(6), heard_time);
//        row.addField(field);
//        field = new IntegerField(headerFields.get(7), found_time);
//        row.addField(field);
//        EntityID position = civilian.getPosition();
//        if (position != null) {
//            field = new IntegerField(headerFields.get(8), position.getValue());
//            row.addField(field);
//            field = new IntegerField(headerFields.get(9), world.getEntity(position) instanceof Building ? 1 : 0);
//            row.addField(field);
//        }
//        return row;
//    }
//}
