/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import edu.kit.joana.api.IFCType;

public class SelectIFCTypeAction extends Action implements IMenuCreator {

	private Menu menu;
	private IFCType selected = IFCType.RLSOD;
	private final static String TXT_LSOD 	= "   LSOD    ";
	private final static String TXT_RLSOD 	= "  RLSOD   ";
	private final static String TXT_iRLSOD 	= "  iRLSOD  ";
	
	public SelectIFCTypeAction() {
		super(TXT_RLSOD, Action.AS_DROP_DOWN_MENU);
		this.setDescription("Select security criterion for multithreaded programs.");
		this.setId("joana.ui.wala.easyifc.selectIFCTypeAction");
		setMenuCreator(this);
	}

	public IFCType getSelectedIFCtype() {
		return selected;
	}
	
	@Override
	public void dispose() {
		if (menu != null) {
			menu.dispose();
			menu = null;
		}
	}

	@Override
	public Menu getMenu(final Control parent) {
		if (menu != null) {
			menu.dispose();
		}
		
		menu = new Menu(parent);
		
		final MenuItem iLSOD = new MenuItem(menu, Action.AS_RADIO_BUTTON);
		iLSOD.setText(IFCType.LSOD.toString());
		iLSOD.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(final SelectionEvent e) {
				selected = IFCType.LSOD;
				setText(TXT_LSOD);
			}
			
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				selected = IFCType.LSOD;
				setText(TXT_LSOD);
			}
		});
		final MenuItem iRLSOD = new MenuItem(menu, Action.AS_RADIO_BUTTON);
		iRLSOD.setText(IFCType.RLSOD.toString());
		iRLSOD.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(final SelectionEvent e) {
				selected = IFCType.RLSOD;
				setText(TXT_RLSOD);
			}
			
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				selected = IFCType.RLSOD;
				setText(TXT_RLSOD);
			}
		});
		final MenuItem iIRLSOD = new MenuItem(menu, Action.AS_RADIO_BUTTON);
		iIRLSOD.setText(IFCType.iRLSOD.toString());
		iIRLSOD.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(final SelectionEvent e) {
				selected = IFCType.iRLSOD;
				setText(TXT_iRLSOD);
			}
			
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
				selected = IFCType.iRLSOD;
				setText(TXT_iRLSOD);
			}
		});
		
		menu.setDefaultItem(iRLSOD);
		selected = IFCType.RLSOD;
		
		return menu;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		return null;
	}

}
