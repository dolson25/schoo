package client.visible.indexingWindow;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.font.*;
import java.awt.event.*;

import javax.imageio.*;
import javax.swing.*;

import client.notVisible.BatchState;
import client.notVisible.BatchStateListener;

import java.util.*;
import java.io.*;
import java.net.URL;


@SuppressWarnings("serial")
public class ImageComponent extends JComponent implements BatchStateListener{
	
	private Color color;
	private BufferedImage indexImage;
	private int w_translateX;
	private int w_translateY;
	private double scale;
	private boolean dragging;
	private int w_dragStartX;
	private int w_dragStartY;
	private int w_dragStartTranslateX;
	private int w_dragStartTranslateY;
	private AffineTransform dragTransform;
	private ArrayList<DrawingShape> shapes;
	private BatchState bs;
	
	private ImageComponent ic;
	
	public ImageComponent(BatchState bss) {
		
		bs = bss;
		bs.addListener(this);
		ic = this;
		
		color = new Color(110, 222, 200, 166);
		
		w_translateX = 0;
		w_translateY = 0;
		scale = bs.getZoomLevel();
		
		initDrag();

		shapes = new ArrayList<DrawingShape>();
		
		shapes.add(new DrawingRect(new Rectangle2D.Double(0, 0, 0, 0), new Color(111, 111, 0, 0)));
		shapes.add(new DrawingRect(new Rectangle2D.Double(0, 0, 0, 0), new Color(111, 111, 0, 0)));

		this.setBackground(new Color(111, 111, 111));
		this.setPreferredSize(new Dimension(700, 700));
		this.setMinimumSize(new Dimension(100, 100));
		this.setMaximumSize(new Dimension(1000, 1000));
		
		this.addMouseListener(mouseAdapter);
		this.addMouseMotionListener(mouseAdapter);
		this.addComponentListener(componentAdapter);
		this.addMouseWheelListener(wheelListener);
	}
	
	public void downloadBatch(String url){
		shapes.clear();
		indexImage = null;;
		try {
			indexImage= ImageIO.read(new URL(url));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int imageHeight = indexImage.getHeight();
		int imageWidth = indexImage.getWidth();

		this.setPreferredSize(new Dimension(imageWidth,imageHeight + 50));

		shapes.add(new DrawingImage(indexImage, new Rectangle2D.Double(0, 0, 
				indexImage.getWidth(null), indexImage.getHeight(null))));
		
		bs.setCellSelectedRow(0);
		bs.setCellSelectedColumn(0);
										
		if(!bs.isHighlights())
			color = new Color(0,0,0,0);
		shapes.add(new DrawingRect(new Rectangle2D.Double(
				bs.getBatchInfo().getFields().get(bs.getCellSelectedColumn()).getXCoordinate(),
				bs.getBatchInfo().getFirstYCoordinate() 
						+ bs.getCellSelectedRow() * bs.getBatchInfo().getRecordHeight(),
				bs.getBatchInfo().getFields().get(bs.getCellSelectedColumn()).getWidth(),
				bs.getBatchInfo().getRecordHeight()), 
				color));
		
		if(bs.isImageInverted())
		{
			bs.setImageInverted(!bs.isImageInverted());
			imageInvert();
		}
		
		repaint();
	}
	private void initDrag() {
		dragging = false;
		w_dragStartX = 0;
		w_dragStartY = 0;
		w_dragStartTranslateX = 0;
		w_dragStartTranslateY = 0;
		dragTransform = null;
	}
	
	public void setScale(double newScale) {
		scale = newScale;
		this.repaint();
	}
	
	public void setTranslation(int w_newTranslateX, int w_newTranslateY) {
		w_translateX = w_newTranslateX;
		w_translateY = w_newTranslateY;
		this.repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {

		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;
		drawBackground(g2);
		
		g2.translate(this.getWidth() / 2,this.getHeight() / 2);
		g2.scale(scale, scale);
		g2.translate(-this.getWidth() / 2 + w_translateX, 
				-this.getHeight() / 2 + w_translateY);

		drawShapes(g2);
	}
	
	private void drawBackground(Graphics2D g2) {
		g2.setColor(getBackground());
		g2.fillRect(0,  0, getWidth(), getHeight());
	}

	private void drawShapes(Graphics2D g2) {
		for (DrawingShape shape : shapes) {
			shape.draw(g2);
		}
	}
	
	////////////////
	//Listener Stuff
	////////////////
	
	public void selectedCellChanged(int row, int col)
	{
		shapes.remove(1);
		
		shapes.add(new DrawingRect(new Rectangle2D.Double(
				bs.getBatchInfo().getFields().get(bs.getCellSelectedColumn()).getXCoordinate(),
				bs.getBatchInfo().getFirstYCoordinate() + bs.getBatchInfo().getRecordHeight()
					* (bs.getCellSelectedRow()),
				
				//same
				bs.getBatchInfo().getFields().get(bs.getCellSelectedColumn()).getWidth(),
				//same
				bs.getBatchInfo().getRecordHeight()), 
				color));
		
		this.repaint();
	}
	public void valueChanged(int x, int y, String newValue)
	{
		
	}
	public void highlightsToggled()
	{
		if(color.getRed() == 0)
		{
			bs.setHighlights(true);
			color = new Color(110, 222, 200, 166);
		}
		else
		{
			bs.setHighlights(false);
			color = new Color(0,0,0,0);
		}

		repaint();
	}
	public void zoomChanged(double ratio)
	{
		scale = scale * ratio;
		if(scale > 4)
			scale = 4;

		if(scale < .25)
			scale = .25;
		
		bs.setZoomLevel(scale);
		repaint();
	}
	public void downloadBatch()
	{
		
	}
	public void submitBatch()
	{
		
	}
	public void imageInvert()
	{
		RescaleOp op = new RescaleOp(-1.0f, 255f, null);
		indexImage = op.filter( indexImage, null);
		
		bs.setImageInverted(!bs.isImageInverted());
		
		repaint();
	}
	
	private MouseAdapter mouseAdapter = new MouseAdapter() {

		@Override
		public void mouseClicked(MouseEvent e){
			
			ic.requestFocusInWindow();
			
			int d_X = e.getX();
			int d_Y = e.getY();
			
			AffineTransform transform = new AffineTransform();

			transform.translate(ImageComponent.this.getWidth() / 2,
					ImageComponent.this.getHeight() / 2);
			transform.scale(scale, scale);
				transform.translate(-ImageComponent.this.getWidth() / 2 + w_translateX, 
						-ImageComponent.this.getHeight() / 2 + w_translateY);
			
			Point2D d_Pt = new Point2D.Double(d_X, d_Y);
			Point2D w_Pt = new Point2D.Double();
			try
			{
				transform.inverseTransform(d_Pt, w_Pt);
			}
			catch (NoninvertibleTransformException ex) {
				return;
			}
			int w_X = (int)w_Pt.getX();
			int w_Y = (int)w_Pt.getY();
			Graphics2D g2 = (Graphics2D)getGraphics();
			
			if (!shapes.get(1).contains(g2, w_X, w_Y) & isCell(w_X,w_Y))
			{
				getNewXCoor(w_X);
				getNewYCoor(w_Y);

				bs.selectedCellChanged(bs.getCellSelectedRow(), bs.getCellSelectedColumn());
			}

		}
		
		public boolean isCell(int w_X, int w_Y){
			if(w_X < bs.getBatchInfo().getFields().get(0).getXCoordinate())
				return false;
			if(w_X >  bs.getBatchInfo().getFields().get(bs.getBatchInfo().getFields().size()-1).getXCoordinate()
					+ bs.getBatchInfo().getFields().get(bs.getBatchInfo().getFields().size()-1
							).getWidth())
				return false;
			if(w_Y < bs.getBatchInfo().getFirstYCoordinate())
				return false;
			if(w_Y > bs.getBatchInfo().getFirstYCoordinate() +
					bs.getBatchInfo().getRecordHeight() * bs.getBatchInfo().getNumRecords())
				return false;
			return true;
		}
		
		public void getNewXCoor(int w_X){
			
			boolean notDone = true;
			int fieldIndex = 0;
			int newXCoor = bs.getBatchInfo().getFields().get(0).getXCoordinate();
			while(notDone){
				if(w_X > newXCoor + bs.getBatchInfo().getFields().get(fieldIndex).getWidth())
				{
					newXCoor += bs.getBatchInfo().getFields().get(fieldIndex).getWidth();
					fieldIndex++;
				}
				else
					notDone = false;
			}
			
			bs.setCellSelectedColumn(fieldIndex);
		}
		
		public void getNewYCoor(int w_Y){
			
			boolean notDone = true;
			int recordIndex = 0;

			int newYCoor = bs.getBatchInfo().getFirstYCoordinate();
			while(notDone){
				if(w_Y > newYCoor + bs.getBatchInfo().getRecordHeight())
				{
					newYCoor += bs.getBatchInfo().getRecordHeight();
					recordIndex++;
				}
				else
					notDone = false;
			}
			
			bs.setCellSelectedRow(recordIndex);
		}
	
		
		@Override
		public void mousePressed(MouseEvent e) {
			int d_X = e.getX();
			int d_Y = e.getY();
			
			AffineTransform transform = new AffineTransform();

			transform.translate(ImageComponent.this.getWidth() / 2,
					ImageComponent.this.getHeight() / 2);
			transform.scale(scale, scale);
			transform.translate(-ImageComponent.this.getWidth() / 2 + w_translateX, 
					-ImageComponent.this.getHeight() / 2 + w_translateY);
			
			Point2D d_Pt = new Point2D.Double(d_X, d_Y);
			Point2D w_Pt = new Point2D.Double();
			try
			{
				transform.inverseTransform(d_Pt, w_Pt);
			}
			catch (NoninvertibleTransformException ex) {
				return;
			}
			int w_X = (int)w_Pt.getX();
			int w_Y = (int)w_Pt.getY();
			
			boolean hitShape = false;
			
			Graphics2D g2 = (Graphics2D)getGraphics();
			for (DrawingShape shape : shapes) {
				if (shape.contains(g2, w_X, w_Y)) {
					hitShape = true;
					break;
				}
			}
			
			if (hitShape) {
				dragging = true;		
				w_dragStartX = w_X;
				w_dragStartY = w_Y;		
				w_dragStartTranslateX = w_translateX;
				w_dragStartTranslateY = w_translateY;
				dragTransform = transform;
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {		
			if (dragging) {
				int d_X = e.getX();
				int d_Y = e.getY();
				
				Point2D d_Pt = new Point2D.Double(d_X, d_Y);
				Point2D w_Pt = new Point2D.Double();
				try
				{
					dragTransform.inverseTransform(d_Pt, w_Pt);
				}
				catch (NoninvertibleTransformException ex) {
					return;
				}
				int w_X = (int)w_Pt.getX();
				int w_Y = (int)w_Pt.getY();
				
				int w_deltaX = w_X - w_dragStartX;
				int w_deltaY = w_Y - w_dragStartY;
				
				w_translateX = w_dragStartTranslateX + w_deltaX;
				w_translateY = w_dragStartTranslateY + w_deltaY;
				
				repaint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			initDrag();
		}
	};
	
	private MouseWheelListener wheelListener = new MouseWheelListener(){

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if(e.getWheelRotation()<0)
			{
				scale = scale * 1.1;
				if(scale > 4)
					scale = 4;
				bs.setZoomLevel(scale);
			}
			else
			{
				scale = scale * 0.9;
				if(scale < .25)
					scale = .25;
				bs.setZoomLevel(scale);
			}
			repaint();
			
		}
		
	};
	
	private ComponentAdapter componentAdapter = new ComponentAdapter() {

		@Override
		public void componentHidden(ComponentEvent e) {
			return;
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			return;
		}

		@Override
		public void componentResized(ComponentEvent e) {
			return;
		}

		@Override
		public void componentShown(ComponentEvent e) {
			return;
		}	
	};

	
	/////////////////
	// Drawing Shape
	/////////////////
	
	
	interface DrawingShape {
		boolean contains(Graphics2D g2, double x, double y);
		void draw(Graphics2D g2);
		Rectangle2D getBounds(Graphics2D g2);
	}


	class DrawingRect implements DrawingShape {

		private Rectangle2D rect;
		
		public DrawingRect(Rectangle2D rect, Color color) {
			this.rect = rect;
		}

		@Override
		public boolean contains(Graphics2D g2, double x, double y) {
			return rect.contains(x, y);
		}

		@Override
		public void draw(Graphics2D g2) {
			g2.setColor(color);
			g2.fill(rect);
		}
		
		@Override
		public Rectangle2D getBounds(Graphics2D g2) {
			return rect.getBounds2D();
		}
	}


	class DrawingImage implements DrawingShape {

		private Rectangle2D rect;
		
		public DrawingImage(Image image, Rectangle2D rect) {
			this.rect = rect;
		}

		@Override
		public boolean contains(Graphics2D g2, double x, double y) {
			return rect.contains(x, y);
		}

		@Override
		public void draw(Graphics2D g2) {
			g2.drawImage(indexImage, (int)rect.getMinX(), (int)rect.getMinY(), (int)rect.getMaxX(), (int)rect.getMaxY(),
							0,0, indexImage.getWidth(null), indexImage.getHeight(null), null);
		}	
		
		@Override
		public Rectangle2D getBounds(Graphics2D g2) {
			return rect.getBounds2D();
		}
	}
	

}
