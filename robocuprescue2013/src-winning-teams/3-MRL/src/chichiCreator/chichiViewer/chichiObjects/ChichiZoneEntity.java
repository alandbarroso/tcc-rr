package chichiCreator.chichiViewer.chichiObjects;

import rescuecore2.standard.entities.Building;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Mostafa Shabani.
 * Date: Feb 21, 2011
 * Time: 11:27:04 AM
 */
public class ChichiZoneEntity extends ArrayList<Building> {
    protected int id;
    public Color color;
    //    private ArrayList<ChichiZoneEntity> neighbors = new ArrayList<ChichiZoneEntity>();
    private ArrayList<Integer> neighbors = new ArrayList<Integer>();

    public ChichiZoneEntity(int id) {
        this.id = id;
        refreshColor();
    }

    public int getId() {
        return id;
    }

    public void refreshColor() {
        Random random;
        long randomValue = (long) (System.nanoTime() * Math.pow(id + 1, 2) + 31);
        random = new Random(randomValue);
        color = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
        while ((color.getBlue() < 5 && color.getGreen() < 5 && color.getRed() < 5)
                || (color.getBlue() + 5 < Color.MAGENTA.getBlue()
                && color.getBlue() - 5 > Color.MAGENTA.getBlue()
                && color.getGreen() + 5 < Color.MAGENTA.getGreen()
                && color.getGreen() - 5 > Color.MAGENTA.getGreen()
                && color.getRed() + 5 < Color.MAGENTA.getRed()
                && color.getRed() - 5 > Color.MAGENTA.getRed())) {
            color = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
        }
    }

    public ArrayList<Integer> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(Integer neighbor) {
        if (!neighbors.contains(neighbor)) {
            this.neighbors.add(neighbor);
        }
    }
}
