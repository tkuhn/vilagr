package ch.tkuhn.vilagr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

// This class is not yet connected to the main Vilagr class
public class GraphDrawer {

	private int size;
	private float offset = 0.0f;
	private float scale = 1.0f;
	private boolean yAxisBottomUp = false;
	private float edgeAlpha = 0.01f;
	private int nodeSize = 4;
	private float[][] edgeMap;
	private BufferedImage image;
	private Graphics graphics;

	public GraphDrawer(int size) {
		this.size = size;
		edgeMap = new float[size][size];
	}

	// Proper sequence:
	// - set...
	// - recordEdge*
	// - finishEdgeDrawing
	// - drawNode*
	// - getImage

	public void finishEdgeDrawing() {
		if (edgeMap == null) {
			throw new RuntimeException("Edge drawing already finished");
		}
		image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		graphics = image.getGraphics();
		for (int x = 0 ; x < size ; x++) {
			for (int y = 0 ; y < size ; y++) {
				int p = 0;
				float v = edgeMap[x][y];
				if (v > 0) p = (int) (edgeMap[x][y] * 123 + 1);
				image.setRGB(x, y, 0xffffff - p * 0x010101);
			}
		}
		edgeMap = null;
	}

	public void recordEdge(int x1, int y1, int x2, int y2) {
		if (edgeMap == null) {
			throw new RuntimeException("Cannot record edges after edge drawing is finished");
		}
		if (Math.abs(x2 - x1) > Math.abs(y2 - y1)) {
			if (x1 > x2) {
				int tx1 = x1;
				x1 = x2;
				x2 = tx1;
				int ty1 = y1;
				y1 = y2;
				y2 = ty1;
			}
			for (int x = x1 ; x <= x2 ; x++) {
				recordEdgePixel(x, (int) ( ((float) (x - x1) / (x2 - x1)) * (y2 - y1) + y1 ) );
			}
		} else {
			if (y1 > y2) {
				int tx1 = x1;
				x1 = x2;
				x2 = tx1;
				int ty1 = y1;
				y1 = y2;
				y2 = ty1;
			}
			for (int y = y1 ; y <= y2 ; y++) {
				recordEdgePixel((int) ( ((float) (y - y1) / (y2 - y1)) * (x2 - x1) + x1 ), y);
			}
		}
	}

	public void recordEdge(float preX1, float preY1, float preX2, float preY2) {
		int x1 = (int) ( (preX1+offset) * scale );
		int y1 = (int) ( (preY1+offset) * scale );
		int x2 = (int) ( (preX2+offset) * scale );
		int y2 = (int) ( (preY2+offset) * scale );
		if (yAxisBottomUp) {
			y1 = size - y1;
			y2 = size - y2;
		}
		recordEdge(x1, y1, x2, y2);
	}

	private void recordEdgePixel(int x, int y) {
		if (x < 0 || y < 0 || x >= size || y >= size) return;
		float v = edgeMap[x][y];
		edgeMap[x][y] = (float) (v + edgeAlpha - v * edgeAlpha);
	}

	public void drawNode(int x, int y, Color color) {
		if (edgeMap != null) {
			throw new RuntimeException("Edge drawing is not yet finished");
		}
		graphics.setColor(color);
		int hs = (int) (nodeSize/2.0);
		for (int px = -hs ; px <= hs ; px++) {
			for (int py = -hs ; py <= hs ; py++) {
				if (px*px + py*py > hs*hs) continue;
				graphics.fillRect(x + px, y + py, 1, 1);
			}
		}
	}

	public void drawNode(float preX, float preY, Color color) {
		int x = (int) ( (preX+offset) * scale );
		int y = (int) ( (preY+offset) * scale );
		if (yAxisBottomUp) {
			y = size - y;
		}
		drawNode(x, y, color);
	}

	public void setEdgeAlpha(float edgeAlpha) {
		this.edgeAlpha = edgeAlpha;
	}

	public void setNodeSize(int nodeSize) {
		this.nodeSize = nodeSize;
	}

	public void setTransformation(float offset, float scale, boolean yAxisBottomUp) {
		this.offset = offset;
		this.scale = scale;
		this.yAxisBottomUp = yAxisBottomUp;
	}

	public BufferedImage getImage() {
		return image;
	}

}
