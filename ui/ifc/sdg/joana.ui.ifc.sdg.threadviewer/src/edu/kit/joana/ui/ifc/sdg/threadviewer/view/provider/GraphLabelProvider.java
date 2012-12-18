/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.provider;

import java.util.Collection;
import java.util.HashSet;


import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.IConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IEntityStyleProvider;
import org.eclipse.zest.core.widgets.ZestStyles;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ui.ifc.sdg.threadviewer.model.SDGWrapper;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.Activator;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ThreadViewer;


public class GraphLabelProvider implements ILabelProvider, IConnectionStyleProvider, IEntityStyleProvider {

	// Font colors
	private static final Color BLACK = new Color(Display.getDefault(), 0, 0, 0);

	// Node colors
	private static final Color HIGHLIGHT_NODE = new Color(Display.getDefault(), 255, 232, 10);

	private static final Color DARK_BLUE = new Color(Display.getDefault(), 80, 126, 188);
	private static final Color BLUE = new Color(Display.getDefault(), 120, 163, 205);
	private static final Color LIGHT_BLUE = new Color(Display.getDefault(), 179, 200, 227);
	private static final Color VERY_LIGHT_BLUE = new Color(Display.getDefault(), 224, 234, 248);

	private static final Color DARK_GREEN = new Color(Display.getDefault(), 110, 168, 46);
	private static final Color GREEN = new Color(Display.getDefault(), 125, 186, 60);
	private static final Color LIGHT_GREEN = new Color(Display.getDefault(), 143, 205, 75);
	private static final Color VERY_LIGHT_GREEN = new Color(Display.getDefault(), 201, 231, 167);

	private static final Color DARK_ORANGE = new Color(Display.getDefault(), 255, 102, 0);
	private static final Color ORANGE = new Color(Display.getDefault(), 255, 129, 40);
	private static final Color LIGHT_ORANGE = new Color(Display.getDefault(), 255, 156, 80);
	private static final Color VERY_LIGHT_ORANGE = new Color(Display.getDefault(), 255, 210, 179);

	private static final Color DARK_YELLOW = new Color(Display.getDefault(), 255, 255, 11);
	private static final Color YELLOW = new Color(Display.getDefault(), 255, 255, 46);
	private static final Color LIGHT_YELLOW = new Color(Display.getDefault(), 255, 255, 81);
	private static final Color VERY_LIGHT_YELLOW = new Color(Display.getDefault(), 255, 255, 151);

	private static final Color[][] COLORS =
		new Color[][] { {DARK_BLUE, BLUE, LIGHT_BLUE, VERY_LIGHT_BLUE},
						{DARK_GREEN, GREEN, LIGHT_GREEN, VERY_LIGHT_GREEN},
						{DARK_ORANGE, ORANGE, LIGHT_ORANGE, VERY_LIGHT_ORANGE},
						{DARK_YELLOW, YELLOW, LIGHT_YELLOW, VERY_LIGHT_YELLOW} };

	// Edge colors
	private static final Color DARK_RED = new Color(Display.getDefault(), 192, 0, 0);

	private static final Color DARK_GRAY = new Color(Display.getDefault(), 60, 60, 60);
	private static final Color GRAY = new Color(Display.getDefault(), 110, 110, 110);
	private static final Color LIGHT_GRAY = new Color(Display.getDefault(), 190, 190, 190);

	// Saves current selection and current thread
	private Object selection = null;
	private ThreadInstance currentThread = null;

	public GraphLabelProvider() {
		super();
	}


	/* Label and image */

	@Override
	public String getText(Object obj) {
		if (obj instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) obj;
			return SDGWrapper.getInstance().getShortLabel(thread);
		} else if (obj instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) obj;
			return SDGWrapper.getInstance().getShortLabel(region);
		}

		return "";
	}

	@Override
	public Image getImage(Object obj) {
		if (obj instanceof ThreadInstance) {
			return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_THREAD);
		} else if (obj instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) obj;

			if (SDGWrapper.getInstance().isInSourceCode(region)) {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_THREADREGION);
			} else {
				return Activator.getDefault().getImageRegistry().get(Activator.IMAGE_TREEVIEW_THREADREGION_DISABLED);
			}
		}

		return null;
	}


	/* Handle selection */

	public void setCurrentSelection(Object obj) {
		// selection is supposed to be possibly be "null"
		selection = obj;

		if (selection instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) selection;
			currentThread = thread;
		} else if (selection instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) selection;
			currentThread = SDGWrapper.getInstance().getThread(region);
		} else {
			currentThread = null;
		}
	}


	/* Node design */

	@Override
	public Color getForegroundColour(Object entity) {
		if (entity instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) entity;
			if (thread.equals(currentThread)) {
				return BLACK;
			} else {
				return DARK_GRAY;
			}
		} else if (entity instanceof ThreadRegion) {
			if (isAdjacentEdgeSelected(entity)) {
				return BLACK;
			} else if (isAdjacentInterferedRegions(entity)) {
				return BLACK;
			} else if (isCurrentRegion(entity)) {
				return BLACK;
			} else {
				return GRAY;
			}
		}

		return BLACK;
	}


	@Override
	public Color getBackgroundColour(Object entity) {
		if (entity instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) entity;

			if (thread.equals(currentThread)) {
				return COLORS[thread.getId() % 4][0];
			} else {
				return COLORS[thread.getId() % 4][1];
			}
		} else if (entity instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) entity;

			if (isAdjacentEdgeSelected(entity)) {
				return COLORS[region.getThread() % 4][2];
			} else if (isAdjacentInterferedRegions(entity)) {
				return COLORS[region.getThread() % 4][2];
			} else if (isCurrentRegion(entity)) {
				return COLORS[region.getThread() % 4][2];
			} else {
				return COLORS[region.getThread() % 4][3];
			}
		}

		return null;
	}

	@Override
	public Color getBorderColor(Object entity) {
		if (entity instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) entity;

			if (thread.equals(currentThread)) {
				return GRAY;
			}
		} else if (entity instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) entity;

			if (hasHiddenInterferences(region)) {
				return DARK_RED;
			} else if (isAdjacentInterferedRegions(region)) {
				return LIGHT_GRAY;
			} else if (isCurrentRegion(entity)) {
				return LIGHT_GRAY;
			}
		}

		// Entity outside
		return null;
	}

	@Override
	public int getBorderWidth(Object entity) {
		if (entity instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) entity;

			// Highlight selected entity
			if (selection instanceof ThreadInstance) {
				if (thread.equals(selection)) {
					return 1;
				}
			}

			if (thread.equals(currentThread)) {
				return 0;
			}
		} else if (entity instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) entity;

			// Highlight selected entity
			if (selection instanceof ThreadRegion) {
				if (region.equals(selection)) {
					return 1;
				}
			}

			if (hasHiddenInterferences(region)) {
				return 1;
			} else if (isAdjacentInterferedRegions(region)) {
				return 0;
			} else if (isCurrentRegion(entity)) {
				return 0;
			}
		}

		// Entity outside
		return 0;
	}


	@Override
	public Color getBorderHighlightColor(Object entity) {
		if (entity instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) entity;

			if (hasHiddenInterferences(region)) {
				return DARK_RED;
			}
		}

		return DARK_GRAY;
	}

	@Override
	public Color getNodeHighlightColor(Object entity) {
		if (entity instanceof ThreadInstance) {
			return HIGHLIGHT_NODE;
		} else if (entity instanceof ThreadRegion) {
			return HIGHLIGHT_NODE;
		}

		return null;
	}

	@Override
	public IFigure getTooltip(Object entity) {
		return null;
	}

	@Override
	public boolean fisheyeNode(Object entity) {
		return true;
	}


	/* Edge design */

	@Override
	public int getConnectionStyle(Object edge) {
		if (edge.equals(selection)) {
			if (isInterference(edge)) {
				return ZestStyles.CONNECTIONS_DASH;
			} else {
				return ZestStyles.CONNECTIONS_DIRECTED;
			}
		} else if (isCurrentEdge(edge)) {
			return ZestStyles.CONNECTIONS_DIRECTED;
		} else if (isAdjacentInterferences(edge)) {
			return ZestStyles.CONNECTIONS_DASH;
		} else if (isCurrentInterferences(edge)) {
			return ZestStyles.CONNECTIONS_DOT;
		} else if (isInterference(edge)) {
			return ZestStyles.CONNECTIONS_DOT;
		}

		// Control Flow Edge outside selected thread
		return ZestStyles.CONNECTIONS_DIRECTED;
	}


	@Override
	public Color getHighlightColor(Object rel) {
		if (isInterference(rel)) {
			return DARK_RED;
		}

		return GRAY;
	}

	@Override
	public int getLineWidth(Object edge) {
		if (selection instanceof EntityConnectionData && selection.equals(edge)) {
			if (isInterference(edge)) {
				return 2;
			} else {
				return 2;
			}
		} else if (isCurrentEdge(edge)) {
			return 1;
		} else if (isAdjacentInterferences(edge)) {
			return 3;
		} else if (isCurrentInterferences(edge)) {
			return 2;
		} else if (isInterference(edge)) {
			return 1;
		}

		// Control Flow Edge outside selected thread
		return 1;
	}

	@Override
	public Color getColor(Object edge) {
		// Edge belonging to selected thread
		if (isCurrentEdge(edge)) {
			return GRAY;
		} else if (isAdjacentInterferences(edge)) {
			return DARK_RED;
		} else if (isCurrentInterferences(edge)) {
			return DARK_RED;
		} else if (isInterference(edge)) {
			return DARK_RED;
		}

		// Control Flow Edge outside selected thread
		return LIGHT_GRAY;
	}


	/* Miscellaneous */

	@Override
	public void addListener(ILabelProviderListener listener) { }

	@Override
	public void dispose() { }

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) { }


	/* Helper methods */

	private boolean isCurrentRegion(Object entity) {
		boolean isCurrent = false;

		if (currentThread != null) {
			Collection<ThreadRegion> currents = SDGWrapper.getInstance().getRegions(currentThread);

			if (entity instanceof ThreadRegion) {
				if (currents != null) {
					if (currents.contains(entity)) {
						isCurrent = true;
					}
				}
			}
		}

		return isCurrent;
	}

	private boolean isCurrentEdge(Object edge) {
		boolean isCurrent = false;

		if (currentThread != null) {
			if (edge instanceof EntityConnectionData) {
				EntityConnectionData connection = (EntityConnectionData) edge;
				Object source = connection.source;
				Object target = connection.dest;

				if (SDGWrapper.getInstance().getRegions(currentThread).contains(source)
						|| source.equals(currentThread)) {
					Collection<ThreadRegion> targets = getNextRegions(source);
					if (targets.contains(target)) {
						isCurrent = true;
					}
				}
			}
		}

		return isCurrent;
	}

	private boolean isAdjacentEdgeSelected(Object entity) {
		boolean isAdjacent = false;

		if (selection instanceof EntityConnectionData) {
			EntityConnectionData connection = (EntityConnectionData) selection;
			Object tmpSource = connection.source;
			Object tmpTarget = connection.dest;

			if (tmpSource instanceof ThreadRegion && tmpTarget instanceof ThreadRegion) {
				ThreadRegion source = (ThreadRegion) tmpSource;
				ThreadRegion target = (ThreadRegion) tmpTarget;

				if (target.equals(entity) || source.equals(entity)) {
					isAdjacent = true;
				}
			}
		}

		return isAdjacent;
	}

	private boolean isInterference(Object edge) {
		boolean isInterference = false;

		// Check if interference edge
		if (edge instanceof EntityConnectionData) {
			EntityConnectionData connection = (EntityConnectionData) edge;
			Object source = connection.source;
			Object target = connection.dest;

			if (source instanceof ThreadRegion && target instanceof ThreadRegion) {
				ThreadRegion sourceRegion = (ThreadRegion) source;
				ThreadRegion targetRegion = (ThreadRegion) target;
				Collection<ThreadRegion> targets = null;

				if (sourceRegion.getID() < targetRegion.getID()) {
					targets = SDGWrapper.getInstance().getInterferedRegions(sourceRegion);
				} else {
					targets = SDGWrapper.getInstance().getInterferedRegions(targetRegion);
				}

				if (targets != null) {
					if (targets.contains(targetRegion)) {
						isInterference = true;
					}
				}
			}
		}

		return isInterference;
	}

	private boolean isCurrentInterferences(Object edge) {
		boolean isCurrentInterference = false;

		if (currentThread != null) {
			// Check if interference edge
			if (isInterference(edge)) {
				EntityConnectionData connection = (EntityConnectionData) edge;
				ThreadRegion source = (ThreadRegion) connection.source;
				ThreadRegion target = (ThreadRegion) connection.dest;

				if (currentThread.getId() == source.getThread() || currentThread.getId() == target.getThread()) {
					isCurrentInterference = true;
				}
			}
		}

		return isCurrentInterference;
	}

	private boolean isAdjacentInterferences(Object edge) {
		boolean isAdjacent = false;

		if (selection != null) {
			if (selection instanceof ThreadRegion) {
				// Check if interference edge
				if (isInterference(edge)) {
					EntityConnectionData connection = (EntityConnectionData) edge;
					ThreadRegion source = (ThreadRegion) connection.source;
					ThreadRegion target = (ThreadRegion) connection.dest;

					if (selection.equals(source) || selection.equals(target)) {
						isAdjacent = true;
					}
				}
			}
		}

		return isAdjacent;
	}


	private boolean isAdjacentInterferedRegions(Object entity) {
		boolean isAdjacent = false;

		if (selection != null) {
			if (selection instanceof ThreadRegion) {
				if (entity instanceof ThreadRegion) {
					ThreadRegion target = (ThreadRegion) entity;
					Collection<ThreadRegion> targets =
						SDGWrapper.getInstance().getInterferedRegions(selection);

					if (targets != null) {
						if (targets.contains(target)) {
							isAdjacent = true;
						}
					}
				}
			}
		}

		return isAdjacent;
	}

	private boolean hasHiddenInterferences(ThreadRegion source) {
		boolean hasHiddenInterferences = false;

		for (ThreadRegion target : SDGWrapper.getInstance().getInterferedRegions(source)) {
			if (!ThreadViewer.getInstance().isActiveThread(target)) {
				hasHiddenInterferences = true;
				break;
			}
		}

		return hasHiddenInterferences;
	}

	private Collection<ThreadRegion> getNextRegions(Object entity) {
		Collection<ThreadRegion> regions = new HashSet<ThreadRegion>();

		if (ThreadViewer.getInstance().isSourceCodeFilterSet()) {
			if (ThreadViewer.getInstance().isInterferingFilterSet()) {
				if (ThreadViewer.getInstance().isFilterHideInterproceduralEdgesSet()) {
					regions = SDGWrapper.getInstance().getNextSourceCodeInterferingRegions(entity);
				} else {
					regions = SDGWrapper.getInstance().
						getNextSourceCodeInterferingRegionsWithInterprocEdges(entity);
				}
			} else {
				if (ThreadViewer.getInstance().isFilterHideInterproceduralEdgesSet()) {
					regions = SDGWrapper.getInstance().getNextSourceCodeRegions(entity);
				} else {
					regions = SDGWrapper.getInstance().getNextSourceCodeRegionsWithInterprocEdges(entity);
				}
			}
		} else {
			if (ThreadViewer.getInstance().isInterferingFilterSet()) {
				if (ThreadViewer.getInstance().isFilterHideInterproceduralEdgesSet()) {
					regions = SDGWrapper.getInstance().getNextInterferingRegions(entity);
				} else {
					regions = SDGWrapper.getInstance().getNextInterferingRegionsWithInterprocEdges(entity);
				}
			} else {
				if (ThreadViewer.getInstance().isFilterHideInterproceduralEdgesSet()) {
					regions = SDGWrapper.getInstance().getNextRegions(entity);
				} else {
					regions = SDGWrapper.getInstance().getNextRegionsWithInterprocEdges(entity);
				}
			}
		}

//		if (ThreadViewer.getInstance().isSourceCodeFilterSet()) {
//			if (ThreadViewer.getInstance().isInterferingFilterSet()) {
//				regions = memoryNextSourceCodeInterferingRegions.get(source);
//			} else {
//				regions = memoryNextSourceCodeRegions.get(source);
//			}
//		} else {
//			if (ThreadViewer.getInstance().isInterferingFilterSet()) {
//				regions = memoryNextInterferingRegions.get(source);
//			} else {
//				regions = sdgWrapper.getNextRegions(source);
//			}
//		}

		return regions;
	}
}
