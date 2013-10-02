package mrl.firebrigade.simulator;

import javolution.util.FastMap;
import mrl.common.MRLConstants;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.object.MrlBuilding;
import mrl.world.object.mrlZoneEntity.MrlZone;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.GaussianGenerator;
import rescuecore2.standard.entities.Refuge;

import java.util.List;
import java.util.Map;

public class Simulator {
    private MrlFireBrigadeWorld world;
//    private Map<Integer, Double> oldZoneTemperatureMap = new FastMap<Integer, Double>();
//    private Map<Integer, Double> newZoneTemperatureMap = new FastMap<Integer, Double>();

    public static float GAMMA = 0.2f;
    public static float WATER_COEFFICIENT = 20f;
    private static final float AIR_TO_BUILDING_COEFFICIENT = 45f;
    NumberGenerator<Double> burnRate = new GaussianGenerator(0.15, 0.02, new java.util.Random(23));


    public Simulator(MrlFireBrigadeWorld world) {
        this.world = world;

//        for (MrlZone zone : world.getZones()) {
//            oldZoneTemperatureMap.put(zone.getId(), 0d);
//            newZoneTemperatureMap.put(zone.getId(), 0d);
//        }
    }

    /**
     * this method update building fuel and energy.
     * and get new fieriness and temperature like main fireSimulator.
     */
    public void update() {
        burn();
        cool();
        exchangeBuilding();
        cool();
    }

    private void burn() {
        double burnRate = this.burnRate.nextValue();
        for (MrlBuilding b : world.getMrlBuildings()) {
//            if (b.getSelfBuilding().getID().equals(MrlPlatoonAgent.CHECK_ID2)) {
//                System.out.print("");
//            }
            if (b.getEstimatedTemperature() >= b.getIgnitionPoint() && b.isFlammable() && b.getFuel() > 0) {
                float consumed = b.getConsume(burnRate);
                if (consumed > b.getFuel()) {
                    consumed = b.getFuel();
                }
                b.setEnergy(b.getEnergy() + consumed);
//                energyHistory.registerBurn(b, consumed);
                b.setFuel(b.getFuel() - consumed);
                b.setPrevBurned(consumed);
            } else {
                b.setPrevBurned(0f);
            }
            if (MRLConstants.LAUNCH_VIEWER) {
//                MrlViewer.TEMP_BUILDING_MAP.put(b.getSelfBuilding().getID(), b);
            }
        }
    }

    private void exchangeBuilding() {
        for (MrlBuilding b : world.getMrlBuildings()) {
            exchangeWithAir(b);
        }
//        oldZoneTemperatureMap = newZoneTemperatureMap;

//        double sumdt = 0;
        Map<MrlBuilding, Double> radiation = new FastMap<MrlBuilding, Double>();
        for (MrlBuilding b : world.getMrlBuildings()) {
            double radEn = b.getRadiationEnergy();
            radiation.put(b, radEn);
        }
        for (MrlBuilding b : world.getMrlBuildings()) {
            if (b.getSelfBuilding().getID().equals(MrlPlatoonAgent.CHECK_ID2)) {
                System.out.print("");
            }
            double radEn = radiation.get(b);
            if (world.isCommunicationLess() || world.isCommunicationMedium() || world.isCommunicationLow()) {
                radEn /= 2.311738394;
            }
            List<MrlBuilding> bs = b.getConnectedBuilding();
            List<Float> vs = b.getConnectedValues();

            for (int c = 0; c < vs.size(); c++) {
                if (bs.get(c).getSelfBuilding().getID().equals(MrlPlatoonAgent.CHECK_ID2)) {
                    System.out.print("");
                }
                double oldEnergy = bs.get(c).getEnergy();
                double connectionValue = vs.get(c);
                double a = radEn * connectionValue;
                double sum = oldEnergy + a;
                bs.get(c).setEnergy(sum);
//                energyHistory.registerRadiationGain(bs[c], a);
            }
            b.setEnergy(b.getEnergy() - radEn);
//            energyHistory.registerRadiationLoss(b, -radEn);
        }
    }

    private void exchangeWithAir(MrlBuilding b) {
// Give/take heat to/from air cells

        double oldTemperature = b.getEstimatedTemperature();
        double oldEnergy = b.getEnergy();
//        double cellCover = b.getCellCover();
//        double cellTemperature = oldZoneTemperatureMap.get(b.getZoneId());
//
//
//        double temperatureDelta = cellTemperature - oldTemperature;
//        double energyDelta = temperatureDelta * AIR_TO_BUILDING_COEFFICIENT * b.getSelfBuilding().getGroundArea();
//        double newCellTemperature = newZoneTemperatureMap.get(b.getZoneId()) - (energyDelta / Math.sqrt(cellCover));
//        if (newCellTemperature >= 0) {
//            newZoneTemperatureMap.put(b.getZoneId(), newCellTemperature);
//        } else {
//            newZoneTemperatureMap.put(b.getZoneId(), 0d);
//        }
//
//        if (oldEnergy + energyDelta >= 0) {
//            b.setEnergy(oldEnergy + energyDelta);
//        } else {
//            b.setEnergy(0);
//        }

        if (oldTemperature > 100) {
            b.setEnergy(oldEnergy - (oldEnergy * 0.042));
        }
    }

    private void cool() {
        for (MrlBuilding building : world.getMrlBuildings()) {
            waterCooling(building);
        }
    }

    private void waterCooling(MrlBuilding b) {
        double lWATER_COEFFICIENT = (b.getEstimatedFieryness() > 0 && b.getEstimatedFieryness() < 4 ? WATER_COEFFICIENT : WATER_COEFFICIENT * GAMMA);
        if (b.getWaterQuantity() > 0) {
            double dE = b.getEstimatedTemperature() * b.getCapacity();
            if (dE <= 0) {
                return;
            }
            double effect = b.getWaterQuantity() * lWATER_COEFFICIENT;
            int consumed = b.getWaterQuantity();
            if (effect > dE) {
                double pc = 1 - ((effect - dE) / effect);
                effect *= pc;
                consumed *= pc;
            }
            b.setWaterQuantity(b.getWaterQuantity() - consumed);
            b.setEnergy(b.getEnergy() - effect);
        }
    }


    /**
     * **********************************************************************************
     * this method simulation temporary fire simulator for get data for next cycles.
     * ya'ni in method vase pishbini ha va estefade dar select fire zone mibashad.
     *
     * @param zoneList: zone haei ke mikhaim rooshun shabih sazi anjam bedim.
     */
    public void tempSimulation(List<MrlZone> zoneList) {
        simulationBurn(zoneList);
        simulationExchangeBuilding(zoneList);
    }

    private void simulationBurn(List<MrlZone> zoneList) {
        double bRate=burnRate.nextValue();
        for (MrlZone zone : zoneList) {

            for (MrlBuilding b : zone) {
                if (!(b.getSelfBuilding() instanceof Refuge) && (b.getTempEstimatedTemperature() >= b.getIgnitionPoint() || b.isTempFlammable()) && b.getTempFuel() > 0) {
                    float consumed = b.getTempConsume(bRate);
                    if (consumed > b.getTempFuel()) {
                        consumed = b.getTempFuel();
                    }
                    b.setTempEnergy(b.getTempEnergy() + consumed);
                    b.setTempFuel(b.getTempFuel() - consumed);
                    b.setTempPrevBurned(consumed);
                } else {
                    b.setTempPrevBurned(0f);
                }
//                MrlViewer.TEMP_BUILDING_MAP.put(b.getSelfBuilding().getID(), b);
            }
        }
    }

    private void simulationExchangeBuilding(List<MrlZone> zoneList) {
        for (MrlZone zone : zoneList) {
            for (MrlBuilding b : zone) {
                simulationExchangeWithAir(b);
            }
        }

        for (MrlZone zone : zoneList) {
            for (MrlBuilding b : zone) {
                if ((b.getSelfBuilding() instanceof Refuge)) {
                    continue;
                }
                double radEn = b.getTempRadiationEnergy();
                List<MrlBuilding> bs = b.getConnectedBuilding();
                List<Float> vs = b.getConnectedValues();

                for (int c = 0; c < vs.size(); c++) {
                    double oldEnergy = bs.get(c).getTempEnergy();
                    double connectionValue = vs.get(c);
                    double a = radEn * connectionValue;
                    double sum = oldEnergy + a;
                    bs.get(c).setTempEnergy(sum);
                }
                b.setTempEnergy(b.getTempEnergy() - radEn);
            }
        }
    }

    private void simulationExchangeWithAir(MrlBuilding b) {
        // Give/take heat to/from air cells
        double oldEnergy = b.getTempEnergy();
        double energyDelta = (b.getSelfBuilding().getGroundArea() * 45 * (b.getSelfBuilding().getGroundArea() / 2500f));
        double val = oldEnergy - energyDelta;
        if (val < 0) {
            val = 0;
        }
        b.setTempEnergy(val);
    }

}
