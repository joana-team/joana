package edu.kastel.smarthome.repository.interfaces;

public interface TabletControlling {
	public int switchOn(int id);
	public int getConsumption(boolean present);
	public void setPresence(boolean present);
}
