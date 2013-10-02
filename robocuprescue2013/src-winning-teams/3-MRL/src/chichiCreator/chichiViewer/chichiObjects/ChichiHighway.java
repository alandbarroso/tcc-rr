package chichiCreator.chichiViewer.chichiObjects;

import rescuecore2.standard.entities.Road;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Mostafa
 * Date: Mar 29, 2011
 * Time: 9:43:05 PM
 */
public class ChichiHighway extends ArrayList<Road> {
    int id;
    public Color color;

    public ChichiHighway(int id) {
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

}
