package mrl.mosCommunication.message;

import mrl.world.MrlWorld;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ISL
 * Date: 2/23/11
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
//this class indexes objects of the model to reduce ID bit num

//Notice: this class has some problems with indexing Civilians and Blockades
public class IDConverter {
    private static HashMap<Integer, EntityID> buildings = new HashMap<Integer, EntityID>();
    private static HashMap<Integer, EntityID> roads = new HashMap<Integer, EntityID>();
    private static HashMap<Integer, EntityID> blockades = new HashMap<Integer, EntityID>();
    private static HashMap<Integer, EntityID> ambulanceTeams = new HashMap<Integer, EntityID>();
    private static HashMap<Integer, EntityID> fireBrigades = new HashMap<Integer, EntityID>();
    private static HashMap<Integer, EntityID> policeForces = new HashMap<Integer, EntityID>();
    private static HashMap<Integer, EntityID> civilians = new HashMap<Integer, EntityID>();


    private static int buildingsBitSize;
    private static int roadsBitSize;
    private static int blockadesBitSize;
    private static int ambulanceTeamsBitSize;
    private static int fireBrigadesBitSize;
    private static int policeForcesBitSize;
    private static int civiliansBitSize;

    /////////////Get ID with key////////////////
    public static EntityID getBuildingID(int key) {
        return buildings.get(key);
    }

    public static EntityID getRoadID(int key) {
        return roads.get(key);
    }

    public static EntityID getBlockadeID(int key) {
        return blockades.get(key);
    }

    public static EntityID getAmbulanceTeamID(int key) {
        return ambulanceTeams.get(key);
    }

    public static EntityID getAFireBrigadeID(int key) {
        return fireBrigades.get(key);
    }

    public static EntityID getPoliceForceID(int key) {
        return policeForces.get(key);
    }

    public static EntityID getCivilianID(int key) {
        return civilians.get(key);
    }

    /////////////////////////////////////////////////
    ///////////////Get key with ID///////////////////
    public static int getBuildingKey(EntityID buildingID) {
        for (int i = 0; i < buildings.size(); i++) {
            if (buildings.get(i).equals(buildingID)) {
                return i;
            }
        }
        return -1;
    }

    public static int getRoadKey(EntityID roadID) {
        for (int i = 0; i < roads.size(); i++) {
            if (roads.get(i).equals(roadID)) {
                return i;
            }
        }
        return -1;
    }

    public static int getBlockadeKey(EntityID blockadeID) {
        for (int i = 0; i < blockades.size(); i++) {
            if (blockades.get(i).equals(blockadeID)) {
                return i;
            }
        }
        return -1;
    }

    public static int getAmbulanceTeamKey(EntityID ambulanceTeamID) {
        for (int i = 0; i < ambulanceTeams.size(); i++) {
            if (ambulanceTeams.get(i).equals(ambulanceTeamID)) {
                return i;
            }
        }
        return -1;
    }

    public static int getFireBrigadeKey(EntityID fireBrigadeID) {
        for (int i = 0; i < fireBrigades.size(); i++) {
            if (fireBrigades.get(i).equals(fireBrigadeID)) {
                return i;
            }
        }
        return -1;
    }

    public static int getPoliceForceKey(EntityID policeForceID) {
        for (int i = 0; i < policeForces.size(); i++) {
            if (policeForces.get(i).equals(policeForceID)) {
                return i;
            }
        }
        return -1;
    }

    public static int getCivilianKey(EntityID civilianID) {
        for (int i = 0; i < civilians.size(); i++) {
            if (civilians.get(i).equals(civilianID)) {
                return i;
            }
        }
        return -1;
    }

    /////////////////////////////////////////////////////////
    //////////////////////Converts///////////////////////////
    public void convertBuildings(Collection<StandardEntity> buildings) {
        int i = 0;
        this.getBuildings().clear();
        for (StandardEntity next : buildings) {
            this.getBuildings().put(i++, next.getID());
        }
        calculateBuildingsBitSize();
    }

    public void convertRoads(List<StandardEntity> roads) {
        int i = 0;
        this.getRoads().clear();
        for (StandardEntity next : roads) {
            this.getRoads().put(i++, next.getID());
        }
        calculateRoadsBitSize();
    }

    public void convertBlockades(List<Blockade> blockades) {
        int i = 0;
        this.getBlockades().clear();
        for (Blockade next : blockades) {
            this.getBlockades().put(i++, next.getID());
        }
        calculateBlockadesBitSize();
    }

    public void convertAmbulanceTeams(List<AmbulanceTeam> ambulanceTeams) {
        int i = 0;
        this.getAmbulanceTeams().clear();
        for (AmbulanceTeam next : ambulanceTeams) {
            this.getAmbulanceTeams().put(i++, next.getID());
        }
        calculateAmbulanceTeamsBitSize();
    }

    public void convertFireBrigades(List<FireBrigade> fireBrigades) {
        int i = 0;
        this.getFireBrigades().clear();
        for (FireBrigade next : fireBrigades) {
            this.getFireBrigades().put(i++, next.getID());
        }
        calculateFireBrigadesBitSize();
    }

    public void convertPoliceForces(List<PoliceForce> policeForces) {
        int i = 0;
        this.getPoliceForces().clear();
        for (PoliceForce next : policeForces) {
            this.getPoliceForces().put(i++, next.getID());
        }
        calculatePoliceForcesBitSize();
    }

    public void convertCivilians(List<Civilian> civilians) {
        int i = 0;
        this.getCivilians().clear();
        for (Civilian next : civilians) {
            this.getCivilians().put(i++, next.getID());
        }
        calculateCiviliansBitSize();
    }

    public void convertAll(MrlWorld model) {
        Collection<StandardEntity> buildings = model.getBuildings();
        List<StandardEntity> road = new ArrayList<StandardEntity>(model.getRoads());
        List<Blockade> blockades = model.getEntitiesOfType(Blockade.class, StandardEntityURN.BLOCKADE);
        List<AmbulanceTeam> ambulanceTeams = model.getEntitiesOfType(AmbulanceTeam.class, StandardEntityURN.AMBULANCE_TEAM);
        List<FireBrigade> fireBrigades = model.getEntitiesOfType(FireBrigade.class, StandardEntityURN.FIRE_BRIGADE);
        List<PoliceForce> policeForces = model.getEntitiesOfType(PoliceForce.class, StandardEntityURN.POLICE_FORCE);
        List<Civilian> civilians = model.getEntitiesOfType(Civilian.class, StandardEntityURN.CIVILIAN);
        convertBuildings(buildings);
        convertRoads(road);
        convertBlockades(blockades);
        convertAmbulanceTeams(ambulanceTeams);
        convertFireBrigades(fireBrigades);
        convertPoliceForces(policeForces);
        convertCivilians(civilians);
    }

    //////////////////////////////////////////////////////////
    /////////////////Get hash maps////////////////////////////
    public HashMap<Integer, EntityID> getBuildings() {
        return buildings;
    }

    public HashMap<Integer, EntityID> getRoads() {
        return roads;
    }

    public HashMap<Integer, EntityID> getBlockades() {
        return blockades;
    }

    public HashMap<Integer, EntityID> getAmbulanceTeams() {
        return ambulanceTeams;
    }

    public HashMap<Integer, EntityID> getFireBrigades() {
        return fireBrigades;
    }

    public HashMap<Integer, EntityID> getPoliceForces() {
        return policeForces;
    }

    public HashMap<Integer, EntityID> getCivilians() {
        return civilians;
    }

    //////////////////////////////////////////////////////////
    ///////////////////Calculate bit size/////////////////////
    public void calculateBuildingsBitSize() {
        int res = 1;
        if (buildings.size() != 0) {
            int size = buildings.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setBuildingsBitSize(res);
    }

    public void calculateRoadsBitSize() {
        int res = 1;
        if (roads.size() != 0) {
            int size = roads.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setRoadsBitSize(res);
    }

    public void calculateBlockadesBitSize() {
        int res = 1;
        if (blockades.size() != 0) {
            int size = blockades.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setBlockadesBitSize(res);
    }

    public void calculateAmbulanceTeamsBitSize() {
        int res = 1;
        if(ambulanceTeams.size() != 0) {
            int size = ambulanceTeams.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setAmbulanceTeamsBitSize(res);
    }

    public void calculateFireBrigadesBitSize() {
        int res = 1;
        if (fireBrigades.size() != 0) {
            int size = fireBrigades.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setFireBrigadesBitSize(res);
    }

    public void calculatePoliceForcesBitSize() {
        int res = 1;
        if (policeForces.size() != 0) {
            int size = policeForces.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setPoliceForcesBitSize(res);
    }

    public void calculateCiviliansBitSize() {
        int res = 1;
        if (civilians.size() != 0) {
            int size = civilians.size() - 1;
            while ((size >>= 1) > 0)
                res++;
        }
        setCiviliansBitSize(res);
    }

    ///////////////////////////////////////////////////////////
    ////////////////////Gets and sets for bit sizes////////////
    public static int getBuildingsBitSize() {
        return buildingsBitSize;
    }

    public static void setBuildingsBitSize(int buildingsBitSize) {
        IDConverter.buildingsBitSize = buildingsBitSize;
    }

    public static int getBlockadesBitSize() {
        return blockadesBitSize;
    }

    public static void setBlockadesBitSize(int blockadesBitSize) {
        IDConverter.blockadesBitSize = blockadesBitSize;
    }

    public static int getRoadsBitSize() {
        return roadsBitSize;
    }

    public static void setRoadsBitSize(int roadsBitSize) {
        IDConverter.roadsBitSize = roadsBitSize;
    }

    public static int getAmbulanceTeamsBitSize() {
        return ambulanceTeamsBitSize;
    }

    public static void setAmbulanceTeamsBitSize(int ambulanceTeamsBitSize) {
        IDConverter.ambulanceTeamsBitSize = ambulanceTeamsBitSize;
    }

    public static int getFireBrigadesBitSize() {
        return fireBrigadesBitSize;
    }

    public static void setFireBrigadesBitSize(int fireBrigadesBitSize) {
        IDConverter.fireBrigadesBitSize = fireBrigadesBitSize;
    }

    public static int getPoliceForcesBitSize() {
        return policeForcesBitSize;
    }

    public static void setPoliceForcesBitSize(int policeForcesBitSize) {
        IDConverter.policeForcesBitSize = policeForcesBitSize;
    }

    public static int getCiviliansBitSize() {
        return civiliansBitSize;
    }

    public static void setCiviliansBitSize(int civiliansBitSize) {
        IDConverter.civiliansBitSize = civiliansBitSize;
    }
}
