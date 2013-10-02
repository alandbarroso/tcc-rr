package agent;

import java.util.EnumSet;
import rescuecore2.standard.entities.AmbulanceCentre;
import rescuecore2.standard.entities.StandardEntityURN;

public class AmbulanceCentreAgent extends Agent<AmbulanceCentre> {

	protected void decide() throws ActionCommandException {
		rest();
	}

	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.AMBULANCE_CENTRE);
	}
}
