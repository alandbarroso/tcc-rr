package chichiCreator.chichiViewer;

import chichiCreator.chichiViewer.chichiLayers.ChichiBuildingLayer;
import chichiCreator.chichiViewer.chichiLayers.ChichiRoadLayer;
import rescuecore2.standard.view.AnimatedHumanLayer;
import rescuecore2.standard.view.AreaNeighboursLayer;
import rescuecore2.standard.view.StandardWorldModelViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 10, 2010
 * Time: 5:31:02 PM
 */
public class ChichiAnimatedWorldModelViewer extends /*Mrl*/StandardWorldModelViewer {
    private static final int FRAME_COUNT = 10;
    private static final int ANIMATION_TIME = 750;
    private static final int FRAME_DELAY = ANIMATION_TIME / FRAME_COUNT;

    private AnimatedHumanLayer humans;
    private final Object lock = new Object();
    private boolean done;

    /**
     * Construct an animated world model chichiViewer.
     */
    public ChichiAnimatedWorldModelViewer() {
        super();
        Timer timer = new Timer(FRAME_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (lock) {
                    if (done) {
                        return;
                    }
                    done = true;
                    if (humans.nextFrame()) {
                        done = false;
                        repaint();
                    }
                }
            }
        });
        timer.setRepeats(true);
        timer.start();
    }

    @Override
    public String getViewerName() {
        return "Animated world model chichiViewer";
    }

    @Override
    public void addDefaultLayers() {
        addLayer(new ChichiBuildingLayer());
        addLayer(new ChichiRoadLayer());

        AreaNeighboursLayer neighboursLayer = new AreaNeighboursLayer();
        neighboursLayer.setVisible(false);
        addLayer(neighboursLayer);

        humans = new AnimatedHumanLayer();
        humans.setVisible(false);
        addLayer(humans);

    }

}