package sos.base.util.namayangar.sosLayer.fire;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;

import javax.swing.JComponent;

import rescuecore2.misc.Pair;
import sos.base.entities.Building;
import sos.base.util.namayangar.NamayangarUtils;
import sos.base.util.namayangar.misc.gui.ScreenTransform;
import sos.base.util.namayangar.sosLayer.other.SOSAbstractToolsLayer;
import sos.base.util.namayangar.tools.LayerType;
import sos.police_v2.PoliceForceAgent;
import sos.police_v2.base.worldModel.PoliceWorldModel;
import sos.police_v2.state.UpdateClusterFireState;

public class UpdateClusterFireLayer extends SOSAbstractToolsLayer<UpdateClusterFireState> {

	public UpdateClusterFireLayer() {
		super(UpdateClusterFireState.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getZIndex() {
		// TODO Auto-generated method stub
		return 150;
	}

	@Override
	protected void makeEntities() {
		setEntities(((PoliceForceAgent) model().sosAgent()).getState(UpdateClusterFireState.class));
	}

	@Override
	protected Shape render(UpdateClusterFireState entity, Graphics2D g, ScreenTransform transform) {
		Shape shape;
		g.setColor(Color.blue);
		for (Building b : entity.allOuter) {
			shape = NamayangarUtils.transformShape(b, transform);
			NamayangarUtils.drawString("outer", g, transform, b);
			g.draw(shape);
		}
		//		g.setColor(new Color(200, 50, 50));
		for (Building b : entity.listOfTargets) {
			shape = NamayangarUtils.transformShape(b, transform);
			NamayangarUtils.drawString("InList", g, transform, b);
			g.fill(shape);
		}
		g.setColor(new Color(0, 200, 200));
		for (Building b : entity.doneBuilding) {
			NamayangarUtils.drawString("done", g, transform, b);
			shape = NamayangarUtils.transformShape(b, transform);
			g.draw(shape);
		}
		if (entity.target != null) {
			shape = NamayangarUtils.transformShape(entity.target, transform);
			NamayangarUtils.drawString("target", g, transform, entity.target);

			g.fill(shape);

		}

		return null;
	}

	@Override
	public JComponent getGUIComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid() {
		return model() instanceof PoliceWorldModel && ((PoliceForceAgent) model().sosAgent()).getState(UpdateClusterFireState.class) != null;
	}

	@Override
	public ArrayList<Pair<String, String>> sosInspect(UpdateClusterFireState entity) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public LayerType getLayerType() {
		return LayerType.Police;
	}
}
