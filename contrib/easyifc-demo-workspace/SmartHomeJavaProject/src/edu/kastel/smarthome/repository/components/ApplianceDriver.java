package edu.kastel.smarthome.repository.components;

import edu.kastel.smarthome.repository.interfaces.ApplianceControlling;
import edu.kastel.smarthome.repository.interfaces.RelaisControlling;

public class ApplianceDriver implements ApplianceControlling {
	@SuppressWarnings("unused")
	private RelaisControlling relaisControlling;
	
	public ApplianceDriver(RelaisControlling relaisControlling) {
		this.relaisControlling = relaisControlling;
	}

	@Override
	public int switchOn() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int switchOff() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getState() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
