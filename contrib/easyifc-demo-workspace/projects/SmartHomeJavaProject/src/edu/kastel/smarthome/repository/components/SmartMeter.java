package edu.kastel.smarthome.repository.components;

import edu.kastel.smarthome.repository.interfaces.MeterReading;
import edu.kastel.smarthome.repository.interfaces.SmartMeterReading;

public class SmartMeter implements SmartMeterReading {
	@SuppressWarnings("unused")
	private MeterReading meterReading;

	public SmartMeter(MeterReading meterReading) {
		this.meterReading = meterReading;
	}

	@Override
	public int getEnergyCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getEnergyCount(int category) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void setCategory(int category) {
		// TODO Auto-generated method stub
		
	}
	
}
