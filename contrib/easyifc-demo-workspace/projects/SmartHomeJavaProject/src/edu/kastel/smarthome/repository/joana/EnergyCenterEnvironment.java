package edu.kastel.smarthome.repository.joana;

import edu.kastel.smarthome.repository.components.EnergyCenter;
import edu.kastel.smarthome.repository.interfaces.ApplianceControlling;
import edu.kastel.smarthome.repository.interfaces.SmartMeterReading;
import edu.kit.joana.ui.annotations.EntryPoint;
import edu.kit.joana.ui.annotations.MayFlow;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;


public class EnergyCenterEnvironment {
	static int INPUT = 42;
		
	static class SwitchOnLamp {
		@Source(level="application customer", lineNumber = 16, columnNumber = 2)
		int id;
		
		@Sink(level="application customer", lineNumber = 19, columnNumber = 2)
		int result;
		
		public SwitchOnLamp() {
			this.id = INPUT;
		}
	}
	
	static class SetCategory {
		@Source(level="customer", lineNumber = 28, columnNumber = 2)
		int category;

		public SetCategory() {
			this.category = INPUT;
		}
	}

	static class GetConsumption {
		@Source(level="customer", lineNumber = 37, columnNumber = 2)
		int category;
		
		@Sink(level="customer", lineNumber = 40, columnNumber = 2)
		int result;
		
		public GetConsumption() {
			this.category = INPUT;
		}
	}
	
	static class GetEnergyConsumption {
		@Sink(level="customer provider", lineNumber = 49, columnNumber = 2)
		int result;
	}

//	@EntryPoint(
//	adversaries = { "customer", "provider", "application" }
//)
	@EntryPoint(
			levels  = { "customer", "provider", "application",
			            "customer provider", "application customer", "application provider",
			          },
			lattice = { @MayFlow(from="customer provider",    to="provider"),
			            @MayFlow(from="customer provider",    to="customer"),
			            @MayFlow(from="application customer", to="customer"),
			            @MayFlow(from="application customer", to="application"),
			            @MayFlow(from="application provider", to="application"),
			            @MayFlow(from="application provider", to="provider")
			          }
	)
	public static void main(String[] args) {
		final SmartMeterReading smartMeterReading = new RandomReading();
		final ApplianceControlling applianceControlling = new RandomControlling();
		
		
		final EnergyCenter energyCenter = new EnergyCenter(smartMeterReading);
		energyCenter.addAppliance(applianceControlling);
		
		while(true) {
			
			SwitchOnLamp callToSwitchOnLamp = new SwitchOnLamp();
			callToSwitchOnLamp.result =
					energyCenter.switchOnLamp(callToSwitchOnLamp.id);
			
			GetEnergyConsumption callToGetEnergyConsumption = new GetEnergyConsumption();
			callToGetEnergyConsumption.result = energyCenter.getEnergyConsumption();
		}
	}
	
	static class RandomReading implements SmartMeterReading {
		class GetEnergyCount {
			@Source(level="customer provider", lineNumber = 89, columnNumber = 3)
			private final int result;

			public GetEnergyCount() {
				this.result = INPUT;
			}
		}

		class GetEnergyCountI {
			@Sink(level="customer", lineNumber = 98, columnNumber = 3)
			private final int category;
			
			@Source(level="customer", lineNumber = 101, columnNumber = 3)
			private final int result;

			public GetEnergyCountI(int category) {
				this.category = category;
				this.result = INPUT;
			}
		}
		

		static class SetCategoryI {
			@Sink(level="customer", lineNumber = 112, columnNumber = 3)
			private final int category;
			
			public SetCategoryI(int category) {
				this.category = category;
			}
		}
		
		@Override
		public int getEnergyCount() {
			GetEnergyCount callToGetConsumptionData = new GetEnergyCount();
			return callToGetConsumptionData.result;
		}
		
		@Override
		public int getEnergyCount(int category) {
			GetEnergyCountI callToGetConsumptionData = new GetEnergyCountI(category);
			return callToGetConsumptionData.result;
		}

		@Override
		public void setCategory(int category) {
			@SuppressWarnings("unused")
			SetCategoryI callToSetCategoryI = new SetCategoryI(category);
		}
	}
	
	static class RandomControlling implements ApplianceControlling {
		class SwitchOn {
			@Source(level = "application customer", lineNumber = 141, columnNumber = 3)
			private int result;
			
			public SwitchOn() {
				this.result = INPUT; 
			}
		}
		
		class SwitchOff {
			@Source(level = "application customer", lineNumber = 150, columnNumber = 3)
			private int result;
			
			public SwitchOff() {
				this.result = INPUT;
			}
		}
		
		class GetState {
			@Source(level = "application customer", lineNumber = 159, columnNumber = 3)
			private int result;
			
			public GetState() {
				this.result = INPUT;
			}
		}
		@Override
		public int switchOn() {
			SwitchOn callToSwitchOn = new SwitchOn();
			return callToSwitchOn.result;
		}
		
		@Override
		public int switchOff() {
			SwitchOff callToSwitchOff = new SwitchOff();
			return callToSwitchOff.result;
		}
		
		@Override
		public int getState() {
			GetState callToGetState = new GetState();
			return callToGetState.result;
		}
	}
	
 
}
