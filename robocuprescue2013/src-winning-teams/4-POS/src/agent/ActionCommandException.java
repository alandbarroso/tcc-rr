package agent;

import rescuecore2.standard.messages.StandardMessageURN;
import rescuecore2.worldmodel.EntityID;

public class ActionCommandException extends Exception {
	private static final long serialVersionUID = 6774634733349626281L;

	public EntityID lastClearedTarget = null;
	private StandardMessageURN action;
	public ActionCommandException(StandardMessageURN action) { 
		super(); 
		this.action = action;
	}

	public StandardMessageURN getAction() {
		return action;
	}
}
