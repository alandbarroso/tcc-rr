package graphics;

import geometry.Line;
import geometry.Point;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import javax.swing.JApplet;

import worldGraph.Area;
import worldGraph.Enterance;

public class MainViewer extends JApplet implements MouseListener,
		MouseMotionListener, MouseWheelListener {
	private static final long serialVersionUID = 1009953074472074523L;

	private Graphics g = null;
	private Area area = new Area();
	private ArrayList<Point> circles = new ArrayList<Point>();
	public static ArrayList<Point> myCircles = new ArrayList<Point>();
	public static ArrayList<Line> myLines = new ArrayList<Line>();
	private final int RAD = 500;
	// private float zoom = 6f;
	// private int diffX = -30600;
	// private int diffY = 18861;

	private int zoom = 13;
	private int diffX = -6725;
	private int diffY = 12137;

	public void init() {
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);

		// Point[] areaPoints = { new Point(188190, 116207),
		// new Point(187134, 115603), new Point(186119, 114791),
		// new Point(187979, 116467), new Point(188190, 116207) };
		Point[] areaPoints = { new Point(91847, 162565),
				new Point(93975, 159926), new Point(96444, 156862),
				new Point(98143, 154754), new Point(100361, 152003),
				new Point(101299, 150840), new Point(108713, 157560),
				new Point(108382, 157899), new Point(100831, 167377),
				new Point(92781, 161423), new Point(91847, 162565) };
		for (Point point : areaPoints)
			area.points.add(point);

		area.updateCenterPoint();

		int index = 9;
//		myLines.add(new Line(area.getInnerPoint(index, index + 1, RAD / 2),
//				area.getInnerPoint(index + 1, index, RAD / 2)));
		Enterance enterance = new Enterance(0, index, area, RAD);
		enterance.createPoints();
		// enterance.updateAvaiablePoints();
		// area.enterances.add(enterance);
		// circles.addAll(enterance.avaialablePoints);
//		myLines.add(new Line(areaPoints[index], areaPoints[index + 1]));
	}

	public void paint(Graphics g) {
		this.g = g;
		g.clearRect(0, 0, getWidth(), getHeight());

		drawPoints(area.points);
		g.setColor(Color.BLACK);
		for (Point p : circles)
			g.drawOval(diffX + p.getX() - RAD, diffY + getHeight()
					- (p.getY() + RAD), RAD * 2, RAD * 2);
		g.setColor(Color.RED);
		for (Point p : myCircles)
			g.drawOval((p.getX() - RAD), getHeight() - (p.getY() + RAD),
					RAD * 2, RAD * 2);
		for (Line line : myLines)
			drawLine(line.getFirstPoint(), line.getSecondPoint());
	}

	public void drawPoints(ArrayList<Point> points) {
		for (int i = 1; i < points.size(); i++)
			drawLine(points.get(i - 1), points.get(i));
	}

	public void drawLine(Point first, Point second) {
		drawLine(first.getX(), first.getY(), second.getX(), second.getY());
	}

	public void drawLine(int x1, int y1, int x2, int y2) {
		g.drawLine(diffX + (int) (x1 / zoom), diffY + getHeight()
				- (int) (y1 / zoom), diffX + (int) (x2 / zoom), diffY
				+ getHeight() - (int) (y2 / zoom));
	}

	public void mouseClicked(MouseEvent e) {
		System.out.println("Here");
		repaint();
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
		lastX = -1;
		lastY = -1;
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		float mult = 10;
		if (zoom < 150)
			mult = 5;
		if (zoom < 100)
			mult = 2;
		if (zoom < 50)
			mult = 1;
		if (mult < 20)
			mult = 0.5f;
		zoom += e.getWheelRotation() * mult;
		repaint();
	}

	int lastX = -1, lastY = -1;

	public void mouseDragged(MouseEvent e) {
		if (lastX != -1) {
			diffX += e.getX() - lastX;
			diffY += e.getY() - lastY;
		}
		lastX = e.getX();
		lastY = e.getY();
		System.out.println("DiffX: " + diffX + " " + "DiffY: " + diffY);
		System.out.println("Zomm: " + zoom);
		repaint();
	}

	public void mouseMoved(MouseEvent e) {
	}
}
