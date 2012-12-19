/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jgraph.JGraph;

public final class Export {
	private Export() { }

    public static void export(JGraph graph, File file) throws IOException {
        double dWidth = graph.getWidth();
        double dHeight = graph.getHeight();
        int width = (int) dWidth;
        int height = (int) dHeight;
        BufferedImage img = createImage(graph, width, height);
        for(int tx = img.getMinTileX(); tx <= img.getMinTileX() + img.getNumXTiles() - 1; tx++) {
            for(int ty = img.getMinTileY(); ty <= img.getMinTileY() + img.getNumYTiles() - 1; ty++) {
                WritableRaster raster = img.getWritableTile(tx, ty);
                int x = raster.getMinX();
                int y = raster.getMinY();
                WritableRaster childRaster =
                        raster.createWritableTranslatedChild(0, 0);
                ColorModel colorModel = img.getColorModel();
                BufferedImage tile = new BufferedImage(colorModel, childRaster,
                        colorModel.isAlphaPremultiplied(), null);
                Graphics2D graphics = tile.createGraphics();
                graphics.translate(-x, -y);
                graph.print(graphics);
            }
        }
        
        ImageIO.write(img, "png", file);
        
    }

    /**
     * Creates a tiled image from the specified JGraph component.
     * @param component a JGraph component
     * @param width the width of the image
     * @param height the height of the image
     * @return a tiled image
     */
    private static BufferedImage createImage(JGraph component, int width, int height) {
    	return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
}
