package mrl.firebrigade;

import javolution.util.FastSet;
import mrl.common.ConvexHull;
import mrl.common.ConvexHull_Rubbish;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.common.clustering.*;
import mrl.common.comparator.ConstantComparators;
import mrl.viewer.layers.MrlConvexHullLayer;
import mrl.viewer.layers.MrlTargetPointsLayer;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: 1/26/12
 * Time: 3:27 PM
 */
public class MrlFireBrigadeDirectionManager {
    private static final double TARGET_DISTANCE_THRESHOLD = ClusterManager.CLUSTER_RANGE_THRESHOLD;
    MrlFireBrigadeWorld world;
    Point mapCenter;
    int DIRECTION_THRESHOLD = 10000;
    double lastDegree = 0;
    private int theta = 30;
    math.geom2d.Point2D targetPoint;
    math.geom2d.Point2D lastTargetPoint;

    List<MrlBuilding> highValueBuildings;//added by Sajjad

    public MrlFireBrigadeDirectionManager(MrlFireBrigadeWorld world) {
        this.world = world;
        mapCenter = new Point(world.getCenterOfMap().getX(), world.getCenterOfMap().getY());
        //highValueBuildings = new ArrayList<MrlBuilding>();
    }

    public Point findBestDirectionPoint(FireCluster fireCluster, List<FireCluster> fireClusters) {
        math.geom2d.Point2D fireClusterCenter = new math.geom2d.Point2D(fireCluster.getCenter());
        math.geom2d.Point2D[] points = new math.geom2d.Point2D[4];
        points[0] = new math.geom2d.Point2D(0, 0);
        points[1] = new math.geom2d.Point2D(0, world.getMapHeight());
        points[2] = new math.geom2d.Point2D(world.getMapWidth(), 0);
        points[3] = new math.geom2d.Point2D(world.getMapWidth(), world.getMapHeight());
        double maxDistance = Double.MIN_VALUE;
        math.geom2d.Point2D tempTargetPoint = null;
        for (math.geom2d.Point2D point : points) {
            double distance = Util.distance(point, fireClusterCenter);
            if (distance > maxDistance) {
                maxDistance = distance;
                tempTargetPoint = point;
            }
        }

        targetPoint = new math.geom2d.Point2D(tempTargetPoint.getX(), tempTargetPoint.getY());

        List<FireCluster> nearClusters = new ArrayList<FireCluster>();
        for (FireCluster next : fireClusters) {
            if (next.equals(fireCluster)) {
                continue;
            }
            if (Util.distance(next.getConvexHullObject().getConvexPolygon(), fireCluster.getConvexHullObject().getConvexPolygon()) < ClusterManager.CLUSTER_RANGE_THRESHOLD * 3) {
                nearClusters.add(next);
            }
        }
        double degree = 0;
        while (degree < 360) {
            if (!isThereClusterInDirectionOf(targetPoint, fireCluster, nearClusters)) {
                if (fireCluster.hasBuildingInDirectionOf(new Point((int) targetPoint.getX(), (int) targetPoint.getY()), true, false)) {
                    break;
                }
            }
            targetPoint = targetPoint.rotate(fireClusterCenter, Math.toRadians((world.getSelf().getID().getValue() % 2) == 0 ? theta : -theta));
            degree += theta;
        }
        /*if (degree >= 360) {
            return null;
        }*/
        return new Point((int) targetPoint.getX(), (int) targetPoint.getY());

    }

    private boolean isThereClusterInDirectionOf(math.geom2d.Point2D targetPoint, FireCluster sourceCluster, List<FireCluster> nearClusters) {
        rescuecore2.misc.geometry.Line2D line = new rescuecore2.misc.geometry.Line2D(targetPoint.getX(), targetPoint.getY(), sourceCluster.getCenter().getX(), sourceCluster.getCenter().getY());
        for (FireCluster fireCluster : nearClusters) {
            if (Util.hasIntersectionLines(fireCluster.getConvexHullObject().getConvexPolygon(), line)) {
                return true;
            }
        }
        return false;
    }

    public Point findFarthestPointOfMap(FireCluster fireCluster) {
        math.geom2d.Point2D fireClusterCenter = new math.geom2d.Point2D(fireCluster.getCenter());
        List<rescuecore2.misc.geometry.Point2D> points = new ArrayList<rescuecore2.misc.geometry.Point2D>();
        points.add(new rescuecore2.misc.geometry.Point2D(0, 0));
        points.add(new rescuecore2.misc.geometry.Point2D(0, world.getMapHeight()));
        points.add(new rescuecore2.misc.geometry.Point2D(world.getMapWidth(), 0));
        points.add(new rescuecore2.misc.geometry.Point2D(world.getMapWidth(), world.getMapHeight()));


        rescuecore2.misc.geometry.Point2D farthestPointOfMap = Util.findFarthestPoint(fireCluster.getConvexHullObject().getConvexPolygon(), points);

        targetPoint = new math.geom2d.Point2D(farthestPointOfMap.getX(), farthestPointOfMap.getY());

        double degree = 0;
        while (degree < 360) {
            if (fireCluster.hasBuildingInDirectionOf(new Point((int) targetPoint.getX(), (int) targetPoint.getY()), true, false)) {
                break;
            }
            targetPoint = targetPoint.rotate(fireClusterCenter, Math.toRadians((world.getSelf().getID().getValue() % 2) == 0 ? theta : -theta));
            degree += theta;
        }
        return new Point((int) targetPoint.getX(), (int) targetPoint.getY());

    }

    public Point findBestValueTarget(FireCluster fireCluster, List<CivilianCluster> civilianClusters) {
        SortedSet<Pair<Point, Double>> sortedTargetPoints = new TreeSet<Pair<Point, Double>>(ConstantComparators.TARGET_POINT_VALUE_COMPARATOR);
        Polygon convexPoly = fireCluster.getConvexHullObject().getConvexPolygon();
        if (civilianClusters != null && !civilianClusters.isEmpty()) {
            for (CivilianCluster civilianCluster : civilianClusters) {
                Point convexPoint = new Point((int) convexPoly.getBounds().getCenterX(), (int) convexPoly.getBounds().getCenterY());
                double radiusLength = Math.sqrt(Math.pow(convexPoly.getBounds().getHeight(), 2) + Math.pow(convexPoly.getBounds().getWidth(), 2)) / 2;
                Point[] helperPoints = getPerpendicularPoints(civilianCluster.getCenter(), convexPoint, radiusLength);
                if (fireCluster.hasBuildingInDirectionOf(civilianCluster.getCenter(), true, true)) {
                    sortedTargetPoints.add(new Pair<Point, Double>(civilianCluster.getCenter(), civilianCluster.getValue() / (Util.distance(civilianCluster.getConvexHullObject().getConvexPolygon(), fireCluster.getConvexHullObject().getConvexPolygon()) / MRLConstants.MEAN_VELOCITY_OF_MOVING)));
                    for (Point point : helperPoints) {
                        if (fireCluster.hasBuildingInDirectionOf(point, true, true)) {
                            sortedTargetPoints.add(new Pair<Point, Double>(point, civilianCluster.getValue() * 0.01 / (Util.distance(fireCluster.getConvexHullObject().getConvexPolygon(), point) / MRLConstants.MEAN_VELOCITY_OF_MOVING)));
                        }
                    }
                } else {
                    sortedTargetPoints.add(new Pair<Point, Double>(civilianCluster.getCenter(), civilianCluster.getValue() * 0.01));
                    for (Point point : helperPoints) {
                        if (fireCluster.hasBuildingInDirectionOf(point, true, true)) {
                            sortedTargetPoints.add(new Pair<Point, Double>(point, civilianCluster.getValue() * 0.1 / (Util.distance(fireCluster.getConvexHullObject().getConvexPolygon(), point) / MRLConstants.MEAN_VELOCITY_OF_MOVING)));
                        }
                    }
                }
            }
        } else {
            sortedTargetPoints.add(new Pair<Point, Double>(mapCenter, 100d));
        }

        if (MRLConstants.LAUNCH_VIEWER) {
            MrlTargetPointsLayer.TARGET_POINTS.put(world.getSelf().getID(), new ArrayList<Pair<Point, Double>>(sortedTargetPoints));
        }

        return sortedTargetPoints.first().first();
    }

    public void setCenterBasedDirection(ConvexObject convexObject) {
        getDirectionBorder(mapCenter, convexObject);
    }

    public List<MrlBuilding> getBuildingInDirectionWorldCentre(ConvexObject convexObject) {
        List<MrlBuilding> buildingsToExtinguish = new ArrayList<MrlBuilding>();
        Set<Line2D> lines = getDirectionBorder(mapCenter, convexObject);
        if (MRLConstants.LAUNCH_VIEWER) {
            convexObject.CONVEX_INTERSECT_LINES = lines;
        }
        if (lines != null) {
            Polygon directionPolygon = getDirectionPolygon(lines, convexObject.getConvexPolygon());
            if (MRLConstants.LAUNCH_VIEWER) {
                convexObject.DIRECTION_POLYGON = directionPolygon;
            }
            if (directionPolygon != null) {
                Point2D point;
                for (StandardEntity se : world.getBuildings()) {
                    point = new Point(se.getLocation(world).first(), se.getLocation(world).second());
                    if (directionPolygon.contains(point)) {
                        buildingsToExtinguish.add(world.getMrlBuilding(se.getID()));
                    }
                }
            }
        }
        highValueBuildings = buildingsToExtinguish;
        if (MRLConstants.LAUNCH_VIEWER) {
            MrlConvexHullLayer.HIGH_VALUE_BUILDINGS.put(world.getSelfHuman().getID(), highValueBuildings);
        }
        return buildingsToExtinguish;
    }


    public List<MrlBuilding> getBuildingInDirectionCivilianCluster(Point CivilianClusterCentre, ConvexObject convexObject) {
        List<MrlBuilding> buildingsToExtinguish = new ArrayList<MrlBuilding>();
        Set<Line2D> lines = getDirectionBorder(CivilianClusterCentre, convexObject);
        if (MRLConstants.LAUNCH_VIEWER) {
            convexObject.CONVEX_INTERSECT_LINES = lines;
        }
        if (lines != null) {
            Polygon directionPolygon = getDirectionPolygon(lines, convexObject.getConvexPolygon());
            if (MRLConstants.LAUNCH_VIEWER) {
                convexObject.DIRECTION_POLYGON = directionPolygon;
            }
            if (directionPolygon != null) {
                Point2D point;
                for (StandardEntity se : world.getBuildings()) {
                    point = new Point(se.getLocation(world).first(), se.getLocation(world).second());
                    if (directionPolygon.contains(point)) {
                        buildingsToExtinguish.add(world.getMrlBuilding(se.getID()));
                    }
                }
            }
        }
        if (MRLConstants.LAUNCH_VIEWER) {
            MrlConvexHullLayer.HIGH_VALUE_BUILDINGS.put(world.getSelfHuman().getID(), highValueBuildings);
        }
        highValueBuildings = buildingsToExtinguish;
        return buildingsToExtinguish;
    }

    public List<MrlBuilding> getBuildingInLimitDirectionCivilianCluster(Point CivilianClusterCentre, ConvexObject convexObject) {
        List<MrlBuilding> buildingsToExtinguish = new ArrayList<MrlBuilding>();

        Set<Line2D> lines = getLimitDirectionBorder(CivilianClusterCentre, convexObject);
        if (MRLConstants.LAUNCH_VIEWER) {
            convexObject.CONVEX_INTERSECT_LINES = lines;
        }

        if (lines != null) {
            Polygon directionPolygon = getDirectionPolygon(lines, convexObject.getConvexPolygon());
            if (MRLConstants.LAUNCH_VIEWER) {
                convexObject.DIRECTION_POLYGON = directionPolygon;
            }
            if (directionPolygon != null) {
                Point2D point;
                for (StandardEntity se : world.getBuildings()) {
                    point = new Point(se.getLocation(world).first(), se.getLocation(world).second());
                    if (directionPolygon.contains(point)) {
                        buildingsToExtinguish.add(world.getMrlBuilding(se.getID()));
                    }
                }
            }
        }
        if (MRLConstants.LAUNCH_VIEWER) {
            MrlConvexHullLayer.HIGH_VALUE_BUILDINGS.put(world.getSelfHuman().getID(), highValueBuildings);
        }
        highValueBuildings = buildingsToExtinguish;

        return buildingsToExtinguish;
    }

    /**
     * get direction final polygon.(this polygon over all buildings to we get it for extinguish)
     *
     * @param lines          lines of directions
     * @param clusterPolygon fire polygon
     * @return a polygon for get buildings
     */
    private Polygon getDirectionPolygon(Set<Line2D> lines, Polygon clusterPolygon) {
        ConvexHull_Rubbish outerConvexHull = new ConvexHull_Rubbish();
        ConvexHull_Rubbish innerConvexHull = new ConvexHull_Rubbish();
        Polygon directionPolygon = new Polygon();
        Point2D clusterCenter = new Point((int) clusterPolygon.getBounds2D().getCenterX(), (int) clusterPolygon.getBounds2D().getCenterY());

        for (Line2D line : lines) {
            Point2D[] point2Ds = getPerpendicularPoints(line.getP2(), line.getP1(), DIRECTION_THRESHOLD);
            if (clusterCenter.distance(point2Ds[0]) >= clusterCenter.distance(point2Ds[1])) {
                outerConvexHull.addPoint(point2Ds[0].getX(), point2Ds[0].getY());
            } else {
                outerConvexHull.addPoint(point2Ds[1].getX(), point2Ds[1].getY());
            }

            point2Ds = getPerpendicularPoints(line.getP1(), line.getP2(), DIRECTION_THRESHOLD);
            if (clusterCenter.distance(point2Ds[0]) >= clusterCenter.distance(point2Ds[1])) {
                outerConvexHull.addPoint(point2Ds[0].getX(), point2Ds[0].getY());
            } else {
                outerConvexHull.addPoint(point2Ds[1].getX(), point2Ds[1].getY());
            }

            outerConvexHull.addPoint(line.getX1(), line.getY1());
            outerConvexHull.addPoint(line.getX2(), line.getY2());
            innerConvexHull.addPoint(line.getX1(), line.getY1());
            innerConvexHull.addPoint(line.getX2(), line.getY2());
        }

        List<Point2D> outerPoints = new ArrayList<Point2D>();
        List<Point2D> outerPoints0 = new ArrayList<Point2D>();
        List<Point2D> innerPoints = new ArrayList<Point2D>();
        List<Point2D> innerPoints0 = new ArrayList<Point2D>();
        double maxDist = 0;
        double dist;
        Point2D prePoint = null;
        Point2D longerPoint1 = null;
        Point2D longerPoint2 = null;

        for (Point2D point : outerConvexHull.getPoints()) {
            if (prePoint == null) {
                prePoint = point;
                continue;
            }

            dist = prePoint.distance(point);
            if (maxDist < dist) {
                maxDist = dist;
                longerPoint1 = prePoint;
                longerPoint2 = point;
            }
            prePoint = point;
        }

        if (longerPoint1 != null && longerPoint2 != null) {
            boolean flag = false;

            for (Point2D point : outerConvexHull.getPoints()) {
                if (longerPoint2.equals(point)) {
                    flag = true;
                }
                if (flag) {
                    outerPoints.add(point);
                } else {
                    outerPoints0.add(point);
                }
            }
            outerPoints.addAll(outerPoints0);


            List<Point> ips = innerConvexHull.getPoints();
            Collections.reverse(ips);
            for (Point2D point : ips) {
                if (longerPoint1.equals(point)) {
                    flag = false;
                }
                if (flag) {
                    innerPoints0.add(point);
                } else {
                    innerPoints.add(point);
                }
            }
            innerPoints.addAll(innerPoints0);

            outerPoints.addAll(innerPoints);

            for (Point2D point : outerPoints) {
                directionPolygon.addPoint((int) point.getX(), (int) point.getY());
            }
        }

        return directionPolygon;
    }

    /**
     * be dast avordane khate direction be samte markaze chegali map
     *
     * @param densityCenter map density center
     * @param convexHull    fire convex hull
     * @return direction lines
     */
    private Set<Line2D> getDirectionBorder(Point densityCenter, ConvexObject convexHull) {
        Polygon convexPoly = convexHull.getConvexPolygon();
        double radiusLength = Math.sqrt(Math.pow(convexPoly.getBounds().getHeight(), 2) + Math.pow(convexPoly.getBounds().getWidth(), 2));

        Point convexPoint = new Point((int) convexPoly.getBounds().getCenterX(), (int) convexPoly.getBounds().getCenterY());
        Point[] points = getPerpendicularPoints(densityCenter, convexPoint, radiusLength);
        Point point1 = points[0];
        Point point2 = points[1];
        convexHull.CENTER_POINT = densityCenter;
        if (MRLConstants.LAUNCH_VIEWER) {
            MrlConvexHullLayer.CENTER_POINT.put(world.getSelf().getID(), new Pair<Point, ConvexObject>(densityCenter, convexHull));
        }
        convexHull.FIRST_POINT = point1;
        convexHull.SECOND_POINT = point2;
        convexHull.CONVEX_POINT = convexPoint;

        Polygon trianglePoly = new Polygon();
        trianglePoly.addPoint(point1.x, point1.y);
        trianglePoly.addPoint(convexPoint.x, convexPoint.y);
        trianglePoly.addPoint(point2.x, point2.y);

        convexHull.setTrianglePolygon(trianglePoly);
        {//get other side of triangle
            double distance = point1.distance(point2) / 3; //mostafa commented this line
            points = getPerpendicularPoints(point2, point1, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexHull.OTHER_POINT2 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexHull.OTHER_POINT2 = new Point(points[1].x, points[1].y);
            }

            points = getPerpendicularPoints(point1, point2, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexHull.OTHER_POINT1 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexHull.OTHER_POINT1 = new Point(points[1].x, points[1].y);
            }
        }

        try {
            return getIntersections(convexPoly, new Line2D.Double(point1, convexPoint), new Line2D.Double(point2, convexPoint), trianglePoly);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Set<Line2D> getLimitDirectionBorder(Point densityCenter, ConvexObject convexHull) {
        Polygon convexPoly = convexHull.getConvexPolygon();
        //--------------Mostafa-------------
        double distance; //distance of fire convex hull to map center density
        distance = Util.distance(convexHull.getConvexPolygon(), new rescuecore2.misc.geometry.Point2D(densityCenter.getX(), densityCenter.getY()));
        double radiusLength = distance;
        //----------------------------------
//        double radiusLength = Math.sqrt(Math.pow(convexPoly.getBounds().getHeight(), 2) + Math.pow(convexPoly.getBounds().getWidth(), 2));//mostafa commented this line

        Point convexPoint = new Point((int) convexPoly.getBounds().getCenterX(), (int) convexPoly.getBounds().getCenterY());
        Point[] points = getPerpendicularPoints(densityCenter, convexPoint, radiusLength);
        Point point1 = points[0];
        Point point2 = points[1];

        convexHull.CENTER_POINT = densityCenter;
        if (MRLConstants.LAUNCH_VIEWER) {
            MrlConvexHullLayer.CENTER_POINT.put(world.getSelf().getID(), new Pair<Point, ConvexObject>(densityCenter, convexHull));
        }
        convexHull.FIRST_POINT = point1;
        convexHull.SECOND_POINT = point2;
        convexHull.CONVEX_POINT = convexPoint;

        Polygon trianglePoly = new Polygon();
        trianglePoly.addPoint(point1.x, point1.y);
        trianglePoly.addPoint(convexPoint.x, convexPoint.y);
        trianglePoly.addPoint(point2.x, point2.y);

        convexHull.setTrianglePolygon(trianglePoly);
        {//get other side of triangle
            //double distance = point1.distance(point2) / 3; //mostafa commented this line
            points = getPerpendicularPoints(point2, point1, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexHull.OTHER_POINT2 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexHull.OTHER_POINT2 = new Point(points[1].x, points[1].y);
            }

            points = getPerpendicularPoints(point1, point2, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexHull.OTHER_POINT1 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexHull.OTHER_POINT1 = new Point(points[1].x, points[1].y);
            }
        }

        try {
            return getIntersections(convexPoly, new Line2D.Double(point1, convexPoint), new Line2D.Double(point2, convexPoint), trianglePoly);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * bedat avordane do noghte amood bar khate hasele az tdo noghte dade shode be faseleye khaste shode.
     *
     * @param point1       main point
     * @param point2       second point for create line
     * @param radiusLength points distance to line
     * @return two points
     */
    public static Point[] getPerpendicularPoints(Point2D point1, Point2D point2, double radiusLength) {
        double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();

        double m1 = (y1 - y2) / (x1 - x2);
        double m2 = (-1 / m1);
        double a = Math.pow(m2, 2) + 1;
        double b = (-2 * x1) - (2 * Math.pow(m2, 2) * x1);
        double c = (Math.pow(x1, 2) * (Math.pow(m2, 2) + 1)) - Math.pow(radiusLength, 2);

        double x3 = ((-1 * b) + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
        double x4 = ((-1 * b) - Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
        double y3 = (m2 * x3) - (m2 * x1) + y1;
        double y4 = (m2 * x4) - (m2 * x1) + y1;

        Point perpendicular1 = new Point((int) x3, (int) y3);
        Point perpendicular2 = new Point((int) x4, (int) y4);
        return new Point[]{perpendicular1, perpendicular2};
    }

    public static Set<Line2D> getIntersections(Polygon poly, Line2D line1, Line2D line2, Polygon trianglePoly) throws Exception {

        PathIterator polyIt = poly.getPathIterator(null); //Getting an iterator along the polygon path
        double[] coords = new double[6]; //Double array with length 6 needed by iterator
        double[] firstCoords = new double[2]; //First point (needed for closing polygon path)
        double[] lastCoords = new double[2]; //Previously visited point
        Set<Line2D> intersections = new FastSet<Line2D>(); //List to hold found intersections

        polyIt.currentSegment(firstCoords); //Getting the first coordinate pair
        lastCoords[0] = firstCoords[0]; //Priming the previous coordinate pair
        lastCoords[1] = firstCoords[1];
        polyIt.next();

        while (!polyIt.isDone()) {
            int type = polyIt.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_LINETO: {
                    Line2D.Double currentLine = new Line2D.Double(lastCoords[0], lastCoords[1], coords[0], coords[1]);
                    if (trianglePoly.contains(lastCoords[0], lastCoords[1]) || trianglePoly.contains(coords[0], coords[1])) {
                        intersections.add(currentLine);
                    }
                    if (currentLine.intersectsLine(line1)) {
                        intersections.add(currentLine);
                    }
                    if (currentLine.intersectsLine(line2)) {
                        intersections.add(currentLine);
                    }
                    lastCoords[0] = coords[0];
                    lastCoords[1] = coords[1];
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    Line2D.Double currentLine = new Line2D.Double(coords[0], coords[1], firstCoords[0], firstCoords[1]);
                    if (trianglePoly.contains(coords[0], coords[1]) || trianglePoly.contains(firstCoords[0], firstCoords[1])) {
                        intersections.add(currentLine);
                    }
                    if (currentLine.intersectsLine(line1)) {
                        intersections.add(currentLine);
                    }
                    if (currentLine.intersectsLine(line2)) {
                        intersections.add(currentLine);
                    }
                    break;
                }
                default: {
                    throw new Exception("Unsupported PathIterator segment type.");
                }
            }
            polyIt.next();
        }
        return intersections;

    }

    public static Point2D getIntersection(final Line2D.Double line1, final Line2D.Double line2) {
        double x1, y1, x2, y2, x3, y3, x4, y4;
        x1 = line1.x1;
        y1 = line1.y1;
        x2 = line1.x2;
        y2 = line1.y2;
        x3 = line2.x1;
        y3 = line2.y1;
        x4 = line2.x2;
        y4 = line2.y2;
        double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3) * (x1 * y2 - x2 * y1))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
        double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2) * (x3 * y4 - x4 * y3))
                / ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
        return new Point2D.Double(x, y);
    }

    public List<MrlBuilding> getHighValueBuildings() {
        return highValueBuildings;
    }

    public int getTheta() {
        return theta;
    }

    public void setTheta(int theta) {
        this.theta = theta;
    }
}
