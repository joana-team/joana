package edu.kastel.smarthome.repository.interfaces;

public interface EnergyControlling {
	public int switchOnLamp(int id);
	public int getConsumption(int category);
	public void setCategory(int category);
}
