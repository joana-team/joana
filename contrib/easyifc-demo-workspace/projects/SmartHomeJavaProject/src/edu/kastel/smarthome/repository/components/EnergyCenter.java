package edu.kastel.smarthome.repository.components;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kastel.smarthome.repository.interfaces.ApplianceControlling;
import edu.kastel.smarthome.repository.interfaces.ConsumptionMonitoring;
import edu.kastel.smarthome.repository.interfaces.EnergyControlling;
import edu.kastel.smarthome.repository.interfaces.SmartMeterReading;

public class EnergyCenter implements EnergyControlling, ConsumptionMonitoring {
	
	final int AWAY = 0;
	private SmartMeterReading smartMeterReading;
	
	private Map<Integer,ApplianceControlling> appliances = new HashMap<Integer, ApplianceControlling>();
	private Set<ApplianceControlling> active = new HashSet<ApplianceControlling>();
	
	public EnergyCenter(SmartMeterReading smartMeterReading) {
		this.smartMeterReading = smartMeterReading;
	}

	@Override
	public int switchOnLamp(int id) {
		ApplianceControlling appliance = appliances.get(id);
		if (appliance == null) return -1;
		return appliance.switchOn();
	}

	@Override
	public int getConsumption(int category) {
		return smartMeterReading.getEnergyCount(category);
	}
	
	@Override
	public void setCategory(int category) {
		smartMeterReading.setCategory(category);
		
	}
	
	@Override
	public int getEnergyConsumption() {
		return smartMeterReading.getEnergyCount(AWAY);
		//return smartMeterReading.getEnergyCount();
	}
	
	public void internal() {
		for (ApplianceControlling appliance : appliances.values() ) {
			if (appliance.getState() > 0) {
				active.add(appliance);
			} else {
				active.remove(appliance);
			}
		}
		
	}
	
	public int addAppliance(ApplianceControlling appliance) {
		int highestId = -1;
		for (Integer id : appliances.keySet()) {
			highestId = Math.max(highestId, id);
		}
		final int newId = highestId + 1;
		appliances.put(newId, appliance);
		
		if (appliance.getState() > 0) {
			active.add(appliance);
		} else {
			active.remove(appliance);
		}
		return newId;
	}

}
