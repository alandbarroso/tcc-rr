package mrl.world.object;

import mrl.util.PolygonUtil;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Blockade;

import java.awt.*;
import java.util.ArrayList;

/**
 * created by: Mahdi Taherian
 * User: mrl
 * Date: 5/18/12
 * Time: 11:44 AM
 */
public class MrlBlockade {
    private Polygon blockade;
    private Blockade parent;
    private MrlRoad position;
    private Polygon transformedPolygon;
    private java.util.List<MrlEdge> blockedEdges;
    private int repairCost;
    private BlockadeValue value;
    public MrlBlockade(MrlRoad road, Blockade blockade, Polygon polygon) {
        initialize(road, blockade, polygon);
    }

    private void initialize(MrlRoad road, Blockade blockade, Polygon polygon) {
        this.parent = blockade;
        this.blockade = polygon;
        blockedEdges = new ArrayList<MrlEdge>();
        position = road;
        repairCost = blockade.getRepairCost();
    }

    public void create(Polygon polygon) {
        blockade = polygon;
    }

    public void createTransformedPolygon(ScreenTransform t) {
        int xs[] = new int[blockade.npoints];
        int ys[] = new int[blockade.npoints];
        for (int i = 0; i < blockade.npoints; i++) {
            xs[i] = t.xToScreen(blockade.xpoints[i]);
            ys[i] = t.yToScreen(blockade.ypoints[i]);
        }
        transformedPolygon = new Polygon(xs, ys, blockade.npoints);
    }

    public Polygon getPolygon() {
        return blockade;
    }

    public Polygon getTransformedPolygon() {
        return transformedPolygon;
    }

    public Pair<MrlBlockade, MrlBlockade> split(Line2D lineSegment) {
        Pair<Polygon, Polygon> splitPolygon = PolygonUtil.split(parent.getApexes(), lineSegment);
        MrlBlockade block1 = new MrlBlockade(position, parent, splitPolygon.first());
        MrlBlockade block2 = new MrlBlockade(position, parent, splitPolygon.second());
        return new Pair<MrlBlockade, MrlBlockade>(block1, block2);
    }

    public Pair<MrlBlockade, MrlBlockade> split(Point point1, Point point2) {
        Point2D startPoint = new Point2D(point1.getX(), point1.getY());
        Point2D endPoint = new Point2D(point2.getX(), point2.getY());
        return split(new Line2D(startPoint, endPoint));
    }

    public java.util.List<MrlEdge> getBlockedEdges() {
        return blockedEdges;
    }

    public void addBlockedEdges(MrlEdge mrlEdge) {
        if (!blockedEdges.contains(mrlEdge))
            blockedEdges.add(mrlEdge);
    }

    public Pair<MrlBlockade, MrlBlockade> split(MrlEdge edge) {
        return split(edge.getLine());
    }

    public MrlRoad getPosition() {
        return position;
    }

    public Blockade getParent() {
        return parent;
    }

    public int getRepairCost() {
        return repairCost;
    }

    public BlockadeValue getValue() {
        return value;
    }

    public void setValue(BlockadeValue value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        MrlBlockade otherBlockade;
        if (obj instanceof MrlBlockade) {
            otherBlockade = (MrlBlockade) obj;
            return getPolygon().equals(otherBlockade.getPolygon());
        }
        return false;
    }
}
