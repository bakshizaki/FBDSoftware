package edu.buffalo.eguru;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.sound.sampled.Line;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainPage {

	JFrame frame;
	JPanel gui;
	JLabel imageLabel;
	BufferedImage originalImage, canvasImage;
	JLabel statusLabel;
	int imageWidth = 800;
	int imageHeight = 450;

	int MODE_NONE = 0;
	int MODE_DRAWING = 1;
	int MODE_SELECTION = 2;
	int MODE_FBD = 3;
	int MODE_TEST = 4;

	int current_mode = MODE_NONE;
	private Image scissor = null;
	int cutCount = 1;

	boolean isFBDDefined = false;
	boolean isFBDAnswered = false;
	boolean isForcePointSelected = false;

	JButton deleteCuts, deleteAll, restartFBD;

	ArrayList<ZPoint> cutsList = new ArrayList<ZPoint>();

	Point fbdStart;
	Point fbdRecent;
	ZPoint firstPoint, secondPoint;

	ArrayList<Line2D> lineList = new ArrayList<Line2D>();
	ArrayList<Line2D> temporaryLineList = new ArrayList<Line2D>();
	ArrayList<Line2D> answerLineList = new ArrayList<Line2D>();
	
	BufferedImage subImage;
	
	int arrowLineLength = 50;
	int arrowHeaderSize = 5;
	int arrowLenght = arrowLineLength+arrowHeaderSize;
	

	// Line2D l = new Line2D.Double();

	public JComponent getGui() {
		if (gui == null) {

			gui = new JPanel(new BorderLayout(4, 4));
			gui.setBorder(new EmptyBorder(5, 3, 5, 3));

			setImage(new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB));

			JPanel imageView = new JPanel(new GridBagLayout());
			imageView.setPreferredSize(new Dimension(imageWidth, imageHeight));

			// display white image
			Graphics2D g = this.canvasImage.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
			g.drawImage(scissor, 100, 100, null);
			originalImage = canvasImage;

			imageLabel = new JLabel(new ImageIcon(canvasImage));
			JScrollPane imageScroll = new JScrollPane(imageView);
			imageView.add(imageLabel);
			gui.add(imageScroll, BorderLayout.CENTER);

			gui.add(getToolBar(), BorderLayout.NORTH);

			statusLabel = new JLabel("Mode: None");
			gui.add(statusLabel, BorderLayout.SOUTH);

		}
		return gui;
	}

	JToolBar getToolBar() {
		JToolBar tb = new JToolBar();
		tb.setFloatable(false);

		// delete cuts button
		deleteCuts = new JButton("Delete Cuts");
		deleteCuts.setMnemonic('d');
		deleteCuts.setToolTipText("Delete Selected Cuts");
		deleteCuts.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (Iterator<ZPoint> iterator = cutsList.iterator(); iterator.hasNext();) {
					ZPoint p = iterator.next();
					if (p.isSelected) {
						Graphics2D g = canvasImage.createGraphics();
						g.drawImage(originalImage, p.x - 15, p.y - 25, p.x + 16, p.y + 16, p.x - 15, p.y - 25, p.x + 16,
								p.y + 16, null);
						g.dispose();
						iterator.remove();

					}

				}
				imageLabel.repaint();
			}
		});

		// delete all cuts

		deleteAll = new JButton("Delete All");
		deleteAll.setMnemonic('l');
		deleteAll.setToolTipText("Delete All Cuts");
		deleteAll.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (Iterator<ZPoint> iterator = cutsList.iterator(); iterator.hasNext();) {
					ZPoint p = iterator.next();
					Graphics2D g = canvasImage.createGraphics();
					g.drawImage(originalImage, p.x - 15, p.y - 25, p.x + 16, p.y + 16, p.x - 15, p.y - 25, p.x + 16,
							p.y + 16, null);
					g.dispose();
					iterator.remove();
				}
				cutCount = 1;
				imageLabel.repaint();

			}
		});

		// Start FBD

		restartFBD = new JButton("Restart FBD");
		restartFBD.setMnemonic('r');
		restartFBD.setToolTipText("Restart Free Body Diagram");
		restartFBD.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				drawOriginal();
				drawPoints(cutsList);
				clearFBDDefinigVariables();
				// has to remove all listeners first before addding, adding a
				// listener twice causes problems
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

				imageLabel.addMouseListener(new fbdModeListener());
			}
		});

		deleteCuts.setEnabled(false);
		deleteAll.setEnabled(false);
		restartFBD.setEnabled(false);
		tb.add(deleteCuts);
		tb.addSeparator();
		tb.add(deleteAll);
		tb.addSeparator();
		tb.add(restartFBD);

		return tb;

	}

	JMenu getFileMenu() {
		JMenu file = new JMenu("File");
		file.setMnemonic('f');

		JMenuItem openItem = new JMenuItem("Open Image");
		openItem.setMnemonic('o');
		openItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser ch = new JFileChooser();
				FileNameExtensionFilter ff = new FileNameExtensionFilter("Image files",
						ImageIO.getReaderFileSuffixes());
				ch.setFileFilter(ff);
				int result = ch.showOpenDialog(gui);
				if (result == JFileChooser.APPROVE_OPTION) {
					try {
						BufferedImage bi = ImageIO.read(ch.getSelectedFile());
						bi = imageResizing(bi, imageWidth, imageHeight);
						setImage(bi);
						cutCount = 1;
						cutsList.clear();
						clearFBDData();
					} catch (IOException e1) {
						showError(e1);
						e1.printStackTrace();
					}
				}
			}
		});

		JMenuItem saveFBDData = new JMenuItem("Save FBD Data");
		saveFBDData.setMnemonic('s');
		saveFBDData.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				StringBuilder sb = new StringBuilder();
				sb.append("Points:\n");
				for (ZPoint p : cutsList) {
					sb.append(p.x + "," + p.y + "," + p.isCorrect() + "\n");
				}
				sb.append("End Points\n");
				sb.append("Lines:\n");
				for (Line2D l : lineList) {
					sb.append((int) l.getX1() + "," + (int) l.getY1() + "|" + (int) l.getX2() + "," + (int) l.getY2()
							+ "\n");
				}
				sb.append("End Lines");

				File temp = null;
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("Save As...");
				chooser.setApproveButtonText("Save");
				chooser.setApproveButtonMnemonic(KeyEvent.VK_S);
				chooser.setApproveButtonToolTipText("Click me to save!");

				do {
					if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION)
						return;
					temp = chooser.getSelectedFile();
					if (!temp.exists())
						break;
					if (JOptionPane.showConfirmDialog(frame,
							"<html>" + temp.getPath() + " already exists.<br>Do you want to replace it?<html>",
							"Save As", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
						break;
				} while (true);

				saveFile(temp, sb.toString());

			}
		});

		JMenuItem loadFBDData = new JMenuItem("Load FBD Data");
		loadFBDData.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				JFileChooser chooser = new JFileChooser();
				int result = chooser.showOpenDialog(gui);
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					ArrayList<String> fileText = readTextFile(f);
					ArrayList<String> pointsString = getPointsFromText(fileText);
					clearFBDData();
					cutsList.clear();
					for (int i = 0; i < pointsString.size(); i++) {
						String[] pointsplit = pointsString.get(i).split(",");
						int x = new Integer(pointsplit[0]);
						int y = new Integer(pointsplit[1]);
						displayPoints(i, x, y);
						cutsList.add(new ZPoint(x, y, i + 1));
					}
					cutCount = pointsString.size() + 1;

					ArrayList<String> linesString = getLinesFromText(fileText);
					for (int i = 0; i < linesString.size(); i++) {
						String[] points = linesString.get(i).split("\\|");
						String[] point1 = points[0].split(",");
						int point1x = Integer.parseInt(point1[0]);
						int point1y = Integer.parseInt(point1[1]);

						String[] point2 = points[1].split(",");
						int point2x = Integer.parseInt(point2[0]);
						int point2y = Integer.parseInt(point2[1]);

						Point p1 = new Point(point1x, point1y);
						Point p2 = new Point(point2x, point2y);
						drawLineOnImage(p1, p2);
						lineList.add(new Line2D.Double(p1, p2));
					}
				}

			}
		});

		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.setMnemonic('x');
		exitItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		file.add(openItem);
		file.add(saveFBDData);
		file.add(loadFBDData);
		file.add(exitItem);

		return file;

	}

	JMenu getModesMenu() {
		JMenu modes = new JMenu("Modes");
		modes.setMnemonic('m');

		JMenuItem none = new JMenuItem("None");
		none.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				current_mode = MODE_NONE;
				statusLabel.setText("Mode: None");
				deleteCuts.setEnabled(false);
				deleteAll.setEnabled(false);
				restartFBD.setEnabled(false);

				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

			}
		});

		JMenuItem drawCuts = new JMenuItem("Draw Cuts");
		drawCuts.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				current_mode = MODE_DRAWING;
				statusLabel.setText("Mode: Drawing Cuts");
				deleteCuts.setEnabled(false);
				deleteAll.setEnabled(false);
				restartFBD.setEnabled(false);

				drawOriginal();
				drawPoints(cutsList);

				// remove all mouselisteners first
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

				imageLabel.addMouseListener(new drawingModeListener());

			}
		});

		JMenuItem selectCuts = new JMenuItem("Select Cuts");
		selectCuts.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				current_mode = MODE_SELECTION;
				statusLabel.setText("Mode: Cuts Selection");
				deleteCuts.setEnabled(true);
				deleteAll.setEnabled(true);
				restartFBD.setEnabled(false);

				drawOriginal();
				drawPoints(cutsList);

				// remove all mouselisteners first
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

				imageLabel.addMouseListener(new selectionModeListener());

			}
		});

		JMenuItem defineFBD = new JMenuItem("Define FBD");
		defineFBD.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				current_mode = MODE_FBD;
				statusLabel.setText("Mode: Define FBD");
				deleteCuts.setEnabled(false);
				deleteAll.setEnabled(false);
				restartFBD.setEnabled(true);

				drawOriginal();
				drawPoints(cutsList);
				// clearFBDData();
				clearFBDDefinigVariables();

				isFBDDefined = false;
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}
				imageLabel.addMouseListener(new fbdModeListener());
			}
		});

		JMenuItem testMode = new JMenuItem("Test Mode");
		testMode.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				current_mode = MODE_TEST;
				statusLabel.setText("Mode: Test");
				deleteCuts.setEnabled(false);
				deleteAll.setEnabled(false);
				restartFBD.setEnabled(false);

				// reset stuff
				clearFBDDefinigVariables();
				answerLineList.clear();

				drawOriginal();
				drawPoints(cutsList);

				isFBDAnswered = false;
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

				imageLabel.addMouseListener(new TestModeListener());
			}
		});

		modes.add(none);
		modes.add(drawCuts);
		modes.add(selectCuts);
		modes.add(defineFBD);
		modes.add(testMode);

		return modes;
	}

	JMenu getForceMenu() {
		JMenu force = new JMenu("Forces");

		JMenuItem drawForces = new JMenuItem("Draw Forces");
		drawForces.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				statusLabel.setText("Mode: Forces");
				deleteCuts.setEnabled(false);
				deleteAll.setEnabled(false);
				restartFBD.setEnabled(false);

				clearFBDDefinigVariables();
				drawOriginal();
				drawPoints(cutsList);

				isFBDAnswered = false;
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

				imageLabel.addMouseListener(new DrawForcesListener());
				imageLabel.addMouseMotionListener(new DrawForcesMotionListener());

			}
		});

		force.add(drawForces);

		return force;
	}

	JMenuBar getMenuBar() {
		JMenuBar mb = new JMenuBar();
		mb.add(getFileMenu());
		mb.add(getModesMenu());
		mb.add(getForceMenu());
		return mb;
	}

	private void setImage(BufferedImage image) {
		originalImage = image;
		int w = image.getWidth();
		int h = image.getHeight();
		canvasImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = this.canvasImage.createGraphics();

		g.drawImage(image, 0, 0, null);

		if (this.imageLabel != null) {
			imageLabel.setIcon(new ImageIcon(canvasImage));
		}
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| UnsupportedLookAndFeelException e) {
					e.printStackTrace();
				} // Set Look and Feel of the UI
				MainPage window = new MainPage();
			}
		});
	}

	public MainPage() {
		initialize();
	}

	private void initialize() {
		scissor = Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/scissor-30.png"));

		frame = new JFrame("FBD Software");
		frame.setSize(900, 600);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(getGui());
		frame.setJMenuBar(getMenuBar());
	}

	public static BufferedImage imageResizing(BufferedImage img, int width, int height) {
		BufferedImage bimg = new BufferedImage(width, height, BufferedImage.TRANSLUCENT);
		Graphics2D g2d = (Graphics2D) bimg.createGraphics();
		g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
		g2d.drawImage(img, 0, 0, width, height, null);
		g2d.dispose();
		return bimg;
	}

	private void showError(Throwable t) {
		JOptionPane.showMessageDialog(gui, t.getMessage(), t.toString(), JOptionPane.ERROR_MESSAGE);
	}

	class drawingModeListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			drawCut(e.getPoint());

			super.mouseClicked(e);
		}

		public void drawCut(Point point) {

			Graphics2D g = canvasImage.createGraphics();
			g.setColor(Color.black);

			g.drawImage(scissor, point.x - 15, point.y - 15, null);
			g.drawString(Integer.toString(cutCount), point.x - 15, point.y - 15);
			g.dispose();
			imageLabel.repaint();
			cutsList.add(new ZPoint(point, cutCount));
			cutCount++;

		}

	}

	class selectionModeListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			Point clickedPoint = e.getPoint();
			for (ZPoint p : cutsList) {
				Rectangle clickThresholdRectangle = new Rectangle(p.x - 15, p.y - 15, 30, 30);
				if (clickThresholdRectangle.contains(clickedPoint)) {
					if (!p.isSelected()) {
						Graphics2D g = canvasImage.createGraphics();
						g.setColor(Color.red);
						// g.drawImage(originalImage, p.x-15, p.y-25, p.x+15,
						// p.y+15, p.x-15, p.y-25, p.x+15, p.y+15, null);
						g.drawRect(p.x - 15, p.y - 15, 30, 30);
						g.dispose();
						imageLabel.repaint();
						p.setSelected(true);
					} else {
						Graphics2D g = canvasImage.createGraphics();
						g.drawImage(originalImage, p.x - 15, p.y - 15, p.x + 16, p.y + 16, p.x - 15, p.y - 15, p.x + 16,
								p.y + 16, null);
						g.drawImage(scissor, p.x - 15, p.y - 15, null);
						g.dispose();
						imageLabel.repaint();
						p.setSelected(false);
					}

				}
			}

			super.mouseClicked(e);
		}

	}

	class fbdModeListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			Point clickedPoint = e.getPoint();
			for (ZPoint p : cutsList) {
				Rectangle clickThresholdRectangle = new Rectangle(p.x - 15, p.y - 15, 30, 30);
				if (clickThresholdRectangle.contains(clickedPoint)) {
					if (fbdStart != null && fbdStart == p) {
						isFBDDefined = true;
					}
					if (fbdStart == null) {
						fbdStart = p;
						firstPoint = p;
					}
					Graphics2D g = canvasImage.createGraphics();
					g.setColor(Color.green);
					g.drawRect(p.x - 15, p.y - 15, 30, 30);
					if (fbdRecent != null) {
						secondPoint = p;
						Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
								new float[] { 9 }, 0);
						g.setStroke(dashed);
						g.setColor(Color.black);
						g.drawLine(fbdRecent.x, fbdRecent.y, p.x, p.y);
						temporaryLineList.add(new Line2D.Double(firstPoint, secondPoint));
						firstPoint = secondPoint;
					}
					p.setCorrect(true);
					fbdRecent = p;
					g.dispose();
					imageLabel.repaint();

					if (isFBDDefined) {
						for (MouseListener m : imageLabel.getMouseListeners()) {
							imageLabel.removeMouseListener(m);
						}
						lineList = new ArrayList<Line2D>(temporaryLineList);
						isFBDDefined = false;
					}

				}
			}
			super.mouseClicked(e);
		}

	}

	class TestModeListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			Point clickedPoint = e.getPoint();
			for (ZPoint p : cutsList) {
				Rectangle clickThresholdRectangle = new Rectangle(p.x - 15, p.y - 15, 30, 30);
				if (clickThresholdRectangle.contains(clickedPoint)) {
					if (fbdStart != null && fbdStart == p) {
						isFBDAnswered = true;

					}
					if (fbdStart == null) {
						fbdStart = p;
						firstPoint = p;
					}
					Graphics2D g = canvasImage.createGraphics();
					g.setColor(Color.green);
					g.drawRect(p.x - 15, p.y - 15, 30, 30);
					if (fbdRecent != null) {
						secondPoint = p;
						Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
								new float[] { 9 }, 0);
						g.setStroke(dashed);
						g.setColor(Color.black);
						g.drawLine(fbdRecent.x, fbdRecent.y, p.x, p.y);
						answerLineList.add(new Line2D.Double(firstPoint, secondPoint));
						firstPoint = secondPoint;
					}
					p.setCorrect(true);
					fbdRecent = p;
					g.dispose();
					imageLabel.repaint();

					if (isFBDAnswered) {
						for (MouseListener m : imageLabel.getMouseListeners()) {
							imageLabel.removeMouseListener(m);
						}

						// check if same
//						System.out.println("Answered");
//						for (Line2D l : answerLineList) {
//							System.out.println((int) l.getX1() + "," + (int) l.getY1() + "|" + (int) l.getX2() + ","
//									+ (int) l.getY2());
//						}
//						System.out.println("Stored");
//						for (Line2D l : lineList) {
//							System.out.println((int) l.getX1() + "," + (int) l.getY1() + "|" + (int) l.getX2() + ","
//									+ (int) l.getY2());
//						}

						boolean isFBDSame = true;
						for (Line2D line : answerLineList) {
							if (!isListContainLine(lineList, line)) {
								isFBDSame = false;
							}

						}
						if (isFBDSame == true)
							statusLabel.setText("Mode: Test Answer:Correct");
						else
							statusLabel.setText("Mode: Test Answer:Incorrect");
					}

				}
			}
			super.mouseClicked(e);
		}

	}

	class DrawForcesListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			Point clickedPoint = e.getPoint();
			if (isForcePointSelected == false) {
				
				for (ZPoint p : cutsList) {
					Rectangle clickThresholdRectangle = new Rectangle(p.x - 15, p.y - 15, 30, 30);
					if (clickThresholdRectangle.contains(clickedPoint)) {
						isForcePointSelected = true;
						firstPoint = p;
						
						Graphics2D g = canvasImage.createGraphics();
						g.setColor(Color.green);
						g.drawRect(p.x - 15, p.y - 15, 30, 30);
						g.dispose();
						imageLabel.repaint();
						
						subImage = deepCopy(canvasImage);
						subImage = subImage.getSubimage(p.x - arrowLenght, p.y - arrowLenght, arrowLenght*2, arrowLenght*2);
//						subImage = canvasImage.getSubimage(p.x - 50, p.y - 50, 100, 100);
//						imageLabel.setIcon(new ImageIcon(subImage));

					}
				}
			}
			else {
				isForcePointSelected = false;
				secondPoint = new ZPoint(e.getPoint(), 0);
				double distance = firstPoint.distance(secondPoint);
				double ratio = arrowLineLength / distance;
				double new_X = ((1-ratio)*firstPoint.getX())+ratio * secondPoint.getX();
				double new_Y = ((1-ratio)*firstPoint.getY())+ratio * secondPoint.getY();
				ZPoint newPoint= new ZPoint((int) new_X, (int) new_Y, 0);
				drawArrowHead(firstPoint, newPoint);
//				drawLineOnImage(firstPoint, secondPoint);
			}
			
			super.mouseClicked(e);
		}

	}
	
	class DrawForcesMotionListener implements MouseMotionListener {

		@Override
		public void mouseDragged(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if(isForcePointSelected == true) {
				secondPoint = new ZPoint(e.getPoint(), 0);
				double distance = firstPoint.distance(secondPoint);
				double ratio = arrowLineLength / distance;
				double new_X = ((1-ratio)*firstPoint.getX())+ratio * secondPoint.getX();
				double new_Y = ((1-ratio)*firstPoint.getY())+ratio * secondPoint.getY();
				ZPoint newPoint= new ZPoint((int) new_X, (int) new_Y, 0);
				Graphics2D g = canvasImage.createGraphics();
				g.drawImage(subImage, (int) firstPoint.getX()-arrowLenght, (int) firstPoint.getY()-arrowLenght, (int) firstPoint.getX()+arrowLenght, (int) firstPoint.getY()+arrowLenght, 0, 0, arrowLenght*2, arrowLenght*2, null);
				g.dispose();
				imageLabel.repaint();
				drawArrowHead(firstPoint, newPoint);
				
			}
			
		}
		
	}

	boolean saveFile(File temp, String s) {
		FileWriter fout = null;
		try {
			fout = new FileWriter(temp);
			fout.write(s);
		} catch (IOException ioe) {
			return false;
		} finally {
			try {
				fout.close();
			} catch (IOException excp) {
			}
		}
		return true;
	}

	void clearFBDData() {
		fbdStart = null;
		fbdRecent = null;
		firstPoint = null;
		secondPoint = null;
		lineList.clear();
	}

	void clearFBDDefinigVariables() {
		fbdStart = null;
		fbdRecent = null;
		firstPoint = null;
		secondPoint = null;
		temporaryLineList.clear();
	}

	public ArrayList<String> readTextFile(File f) {
		ArrayList<String> retString = new ArrayList<String>();
		;
		try {
			FileInputStream fstream = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				retString.add(strLine);
			}

			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return retString;
	}

	private void displayPoints(int i, int x, int y) {
		Graphics2D g = canvasImage.createGraphics();
		g.setColor(Color.black);

		g.drawImage(scissor, x - 15, y - 15, null);
		g.drawString(Integer.toString(i + 1), x - 15, y - 15);
		g.dispose();
		imageLabel.repaint();
	}

	private ArrayList<String> getPointsFromText(ArrayList<String> fileText) {
		ArrayList<String> pointsString = new ArrayList<String>();
		int dataIdx = 0;
		while (!fileText.get(dataIdx).contains("Points:"))
			dataIdx++;
		dataIdx++;
		while (!fileText.get(dataIdx).contains("End Points") && dataIdx < fileText.size()) {
			pointsString.add(fileText.get(dataIdx));
			dataIdx++;
		}
		return pointsString;

	}

	private ArrayList<String> getLinesFromText(ArrayList<String> fileText) {
		ArrayList<String> linesString = new ArrayList<String>();
		int dataIdx = 0;
		while (!fileText.get(dataIdx).contains("Lines:"))
			dataIdx++;
		dataIdx++;
		while (!fileText.get(dataIdx).contains("End Lines") && dataIdx < fileText.size()) {
			linesString.add(fileText.get(dataIdx));
			dataIdx++;
		}
		return linesString;

	}

	void drawLineOnImage(Point p1, Point p2) {
		Graphics2D g = canvasImage.createGraphics();
		Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9 }, 0);
		g.setStroke(dashed);
		g.setColor(Color.black);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
		g.dispose();
		imageLabel.repaint();

	}

	boolean isLineEqual(Line2D l1, Line2D l2) {
		if (l1.getP1().equals(l2.getP1()) && l1.getP2().equals(l2.getP2()))
			return true;
		else if (l1.getP1().equals(l2.getP2()) && l1.getP2().equals(l2.getP1()))
			return true;
		else
			return false;
	}

	boolean isListContainLine(ArrayList<Line2D> list, Line2D line) {
		for (Line2D l : list) {
			if (isLineEqual(l, line))
				return true;
		}
		return false;
	}

	void drawOriginal() {
		Graphics2D g = canvasImage.createGraphics();
		g.drawImage(originalImage, 0, 0, canvasImage.getWidth(), canvasImage.getHeight(), 0, 0, canvasImage.getWidth(),
				canvasImage.getHeight(), null);
		g.dispose();
		imageLabel.repaint();
	}

	void drawPoints(ArrayList<ZPoint> cutsList) {
		Graphics2D g = canvasImage.createGraphics();
		for (ZPoint p : cutsList) {
			if (p.isCorrect()) {
				p.setCorrect(false);
			}
			g.drawImage(scissor, p.x - 15, p.y - 15, null);
			g.setColor(Color.BLACK);
			g.drawString(Integer.toString(p.getCutCounter()), p.x - 15, p.y - 15);

		}
		g.dispose();
		imageLabel.repaint();
	}
	
	private void drawArrowHead(Point p1, Point p2) {
		int x1 = (int) p1.getX();
		int y1 = (int) p1.getY();
		int x2 = (int) p2.getX();
		int y2 = (int) p2.getY();
		
		Graphics2D g2d = canvasImage.createGraphics();
		AffineTransform tx = new AffineTransform();
		Line2D.Double line = new Line2D.Double(x1,y1,x2,y2);
		
		g2d.setColor(Color.BLACK);
		g2d.drawLine(x1, y1, x2, y2);

		Polygon arrowHead = new Polygon();
		arrowHead.addPoint( 0,5);
		arrowHead.addPoint( -5, -5);
		arrowHead.addPoint( 5,-5);

	    tx.setToIdentity();
	    double angle = Math.atan2(line.y2-line.y1, line.x2-line.x1);
	    tx.translate(line.x2, line.y2);
	    tx.rotate((angle-Math.PI/2d));  

	    Graphics2D g = (Graphics2D) g2d.create();
	    g.setColor(Color.black);
	    g.setTransform(tx);   
	    g.fill(arrowHead);
	    g.dispose();
	    
	    imageLabel.repaint();
	}
	
	static BufferedImage deepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		}

}
