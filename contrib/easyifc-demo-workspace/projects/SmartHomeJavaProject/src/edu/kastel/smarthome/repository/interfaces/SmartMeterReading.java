package edu.kastel.smarthome.repository.interfaces;

public interface SmartMeterReading {
	public int getEnergyCount();
	public int getEnergyCount(int category);
	public void setCategory(int category);
}
