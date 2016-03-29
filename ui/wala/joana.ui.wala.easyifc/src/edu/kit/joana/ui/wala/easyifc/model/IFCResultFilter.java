package edu.kit.joana.ui.wala.easyifc.model;

import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SLeak;

public interface IFCResultFilter {

	public static enum LeakType { DIRECT, INDIRECT, EXCEPTION }
	
	public boolean isOk(final LeakType type, final SLeak leak);

	public static IFCResultFilter DEFAULT = new IFCResultFilter() {
		
		@Override
		public boolean isOk(final LeakType type, final SLeak leak) {
			return true;
		}
	};
	
}
