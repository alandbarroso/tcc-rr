package worldGraph;

import geometry.Point;

public class Blockade extends Shape {
	public rescuecore2.standard.entities.Blockade modelBlockade = null;
	public Blockade(rescuecore2.standard.entities.Blockade blockade) {
		this.modelBlockade = blockade;

		for (int i = 0; i < blockade.getApexes().length; i += 2)
			points.add(new Point(blockade.getApexes()[i],
					blockade.getApexes()[i + 1]));
		if (blockade.getApexes().length > 0)
			points.add(new Point(blockade.getApexes()[0],
					blockade.getApexes()[1]));
		updateCenterPoint();
	}
}
