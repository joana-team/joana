package edu.kit.joana.ui.wala.easyifc.actions;

import org.eclipse.jface.action.Action;


import edu.kit.joana.ui.wala.easyifc.Activator;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer;
import edu.kit.joana.ui.wala.easyifc.views.EasyIFCView;

public class IFCActionSelected extends Action {
	public IFCActionSelected(final EasyIFCView view, final IFCCheckResultConsumer resultConsumer) {
		super();
		this.setText("Check IFC");
		this.setDescription("Check the information flow of the selected entry point.");
		this.setId("joana.ui.wala.easyifc.runIIFCActionSelected");
		this.setImageDescriptor(Activator.getImageDescriptor("icons/run_ifc_action.png"));
	}
	
	@Override
	public void run() {
	}
}
