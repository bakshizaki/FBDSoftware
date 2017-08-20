package edu.buffalo.eguru;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
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
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
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
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.buffalo.eguru.ForcePoint.EntityDirection;
import edu.buffalo.eguru.ForcePoint.EntityProperty;
import edu.buffalo.eguru.ForcePoint.EntityType;

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
	int MODE_DRAW_FP = 5;
	int MODE_DEL_FP = 6;
	int MODE_DRAW_FORCE = 7;
	int MODE_TEST_FORCE = 8;

	int current_mode = MODE_NONE;
	private Image scissor = null;
	private Image forcePointImage = null;
	int cutCount = 1;

	boolean isFBDDefined = false;
	boolean isFBDAnswered = false;
	boolean isForcePointSelected = false;

	JButton deleteCuts, deleteAll, restartFBD;

	ArrayList<ZPoint> cutsList = new ArrayList<ZPoint>();

	// list of forcepoints
	ArrayList<ForcePoint> fpList = new ArrayList<ForcePoint>();

	// list of which contains data for all the forces, their angles, moments
	// etc.
	ArrayList<ForcePoint> forceDataList = new ArrayList<ForcePoint>();

	// list of correct forces loaded from file
	ArrayList<ForcePoint> correctForceDataList = new ArrayList<ForcePoint>();

	Point fbdStart;
	Point fbdRecent;
	ZPoint firstPoint, secondPoint;
	ForcePoint firstForcePoint, secondForcePoint;

	ArrayList<Line2D> lineList = new ArrayList<Line2D>();
	ArrayList<Line2D> temporaryLineList = new ArrayList<Line2D>();
	ArrayList<Line2D> answerLineList = new ArrayList<Line2D>();

	BufferedImage subImage;

	int arrowLineLength = 50;
	int arrowHeaderSize = 5;
	int arrowLenght = arrowLineLength + arrowHeaderSize;

	JToolBar forceToolbar = null;

	// static final Color KNOWN_FORCE_COLOR = new Color(0, 153, 51);
	// static final Color UNKNOWN_FORCE_COLOR = Color.RED;
	static final Color DRAWING_COLOR = Color.BLACK;
	static final Color CORRECT_COLOR = new Color(0, 153, 51);
	static final Color INCORRECT_COLOR = Color.RED;
	Color currentColor = null;

	ForcePoint.EntityType currentType = null;
	ForcePoint.EntityProperty currentProperty = null;
	ForcePoint.EntityDirection currentDirection = null;

	// Force Toolbar radiobuttons
	JRadioButton selectForce;
	JRadioButton selectMoment;
	JRadioButton selectProperyKnown;
	JRadioButton selectProperyUnknown;
	JRadioButton selectDirectionCW;
	JRadioButton selectDirectionCCW;
	JButton undoButton;
	JButton submitForces;

	int ANGLE_TOLERENCE = 5;

	BufferedImage undoImage;
	int UNDO_LIMIT = 10;
	SizedStack<BufferedImage> undoImageStack = new SizedStack<BufferedImage>(10);

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
			g.drawImage(forcePointImage, 100, 100, null);
			originalImage = deepCopy(canvasImage);

			imageLabel = new JLabel(new ImageIcon(canvasImage));
			JScrollPane imageScroll = new JScrollPane(imageView);
			imageView.add(imageLabel);
			gui.add(imageScroll, BorderLayout.CENTER);

			gui.add(getToolBar(), BorderLayout.NORTH);

			statusLabel = new JLabel("Mode: None");
			gui.add(statusLabel, BorderLayout.SOUTH);
			forceToolbar = getForceToolBar();
			gui.add(forceToolbar, BorderLayout.EAST);

		}
		return gui;
	}

	JToolBar getToolBar() {
		JToolBar tb = new JToolBar();
		tb.setFloatable(false);

		// delete cuts button
		deleteCuts = new JButton("Delete");
		deleteCuts.setMnemonic('d');
		deleteCuts.setToolTipText("Delete Selected Cuts");
		deleteCuts.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (current_mode == MODE_SELECTION) {
					for (Iterator<ZPoint> iterator = cutsList.iterator(); iterator.hasNext();) {
						ZPoint p = iterator.next();
						if (p.isSelected) {
							Graphics2D g = canvasImage.createGraphics();
							g.drawImage(originalImage, p.x - 15, p.y - 25, p.x + 16, p.y + 16, p.x - 15, p.y - 25,
									p.x + 16, p.y + 16, null);
							g.dispose();
							iterator.remove();

						}

					}
					imageLabel.repaint();
				} else if (current_mode == MODE_DEL_FP) {
					for (Iterator<ForcePoint> iterator = fpList.iterator(); iterator.hasNext();) {
						ForcePoint p = iterator.next();
						if (p.isSelected) {
							Graphics2D g = canvasImage.createGraphics();
							g.drawImage(originalImage, p.x - 15, p.y - 25, p.x + 16, p.y + 16, p.x - 15, p.y - 25,
									p.x + 16, p.y + 16, null);
							g.dispose();
							iterator.remove();

						}

					}
					imageLabel.repaint();
				}
			}

		});

		// delete all cuts

		deleteAll = new JButton("Delete All");
		deleteAll.setMnemonic('l');
		deleteAll.setToolTipText("Delete All Cuts");
		deleteAll.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (current_mode == MODE_SELECTION) {
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
				} else if (current_mode == MODE_DEL_FP) {
					for (Iterator<ForcePoint> iterator = fpList.iterator(); iterator.hasNext();) {
						ForcePoint p = iterator.next();
						Graphics2D g = canvasImage.createGraphics();
						g.drawImage(originalImage, p.x - 15, p.y - 25, p.x + 16, p.y + 16, p.x - 15, p.y - 25, p.x + 16,
								p.y + 16, null);
						g.dispose();
						iterator.remove();
					}

					imageLabel.repaint();
				}
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

	JToolBar getForceToolBar() {
		JToolBar tb = new JToolBar(JToolBar.VERTICAL);
		tb.setFloatable(false);

		selectForce = new JRadioButton("Force");
		selectForce.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentType = EntityType.FORCE;
				selectDirectionCW.setEnabled(false);
				selectDirectionCCW.setEnabled(false);
			}
		});

		selectMoment = new JRadioButton("Moment");
		selectMoment.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentType = EntityType.MOMENT;
				selectDirectionCW.setEnabled(true);
				selectDirectionCCW.setEnabled(true);
			}
		});

		selectProperyKnown = new JRadioButton("Known");
		selectProperyKnown.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentProperty = EntityProperty.KNOWN;

			}
		});

		selectProperyUnknown = new JRadioButton("Unknown");
		selectProperyUnknown.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentProperty = EntityProperty.UNKNOWN;
			}
		});

		selectDirectionCW = new JRadioButton("Clockwise");
		selectDirectionCW.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentDirection = EntityDirection.CLOCKWISE;

			}
		});

		selectDirectionCCW = new JRadioButton("Anticlockwise");
		selectDirectionCCW.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				currentDirection = EntityDirection.ANTICLOCKWISE;

			}
		});

		undoButton = new JButton("Undo");
		undoButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				undo();

			}
		});

		submitForces = new JButton("Submit");
		submitForces.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				// there are 2 list, correctForceDataList (A) contains all
				// correct answers, forceDataList (B) contains answer entered by
				// user
				// To check correctness we have to check every correct
				// ForcePoint from A is in B, and every correct ForcePoint frmo
				// B is in A.
				
				submitForces.setEnabled(false);
				undoButton.setEnabled(false);

				boolean forceFinalAnswer = true;
				
				drawOriginal();
				for(ForcePoint fp: fpList) {
					displayForcePoint(fp.x, fp.y);
				}

				for (ForcePoint p : correctForceDataList) {
					if (p.isCorrect()) {
						if (!forceListContains(forceDataList, p)) {
							forceFinalAnswer = false;
							currentColor = INCORRECT_COLOR;
							displayCircle(p.x, p.y);
						}
							
					}
				}
				for (ForcePoint p : forceDataList) {
					if (p.isCorrect()) {
						if (!forceListContains(correctForceDataList, p)) {
							forceFinalAnswer = false;
							currentColor = INCORRECT_COLOR;
						} else {
							currentColor = CORRECT_COLOR;
						}
						if (p.type == EntityType.FORCE)
							displayArrow(p.x, p.y, p.angle, p.property);
						else
							displayMoment(p.x, p.y, p.direction, p.property);
					}
				}

				 forceDataList.clear();

				if (forceFinalAnswer == true)
					statusLabel.setText("Force Correct");
				else
					statusLabel.setText("Force Incorrect");

			}

			private boolean forceListContains(ArrayList<ForcePoint> forceList, ForcePoint p) {
				if (p.type == EntityType.FORCE) {
					return forceListContainsForce(forceList, p);
				} else if (p.type == EntityType.MOMENT) {
					return forceListContainsMoment(forceList, p);
				}
				return false;
			}

			private boolean forceListContainsMoment(ArrayList<ForcePoint> forceList, ForcePoint p) {
				for (ForcePoint listPoint : forceList) {
					if (p.x == listPoint.x && p.y == listPoint.y && listPoint.isCorrect == true
							&& p.type == listPoint.type && p.getProperty() == listPoint.getProperty()
							&& p.getDirection() == listPoint.getDirection())
						return true;
				}
				return false;
			}

			private boolean forceListContainsForce(ArrayList<ForcePoint> forceList, ForcePoint p) {
				for (ForcePoint listPoint : forceList) {
					if (p.x == listPoint.x && p.y == listPoint.y && listPoint.isCorrect == true
							&& p.type == listPoint.type && p.getProperty() == listPoint.getProperty()
							&& equalWithTolerance(p.getAngle(), listPoint.getAngle()))
						return true;
				}
				return false;
			}

			private boolean equalWithTolerance(int angle1, int angle2) {
				int upperTolerance = angle2 + ANGLE_TOLERENCE;
				int lowerTolerance = angle2 - ANGLE_TOLERENCE;

				if (lowerTolerance < 0) {
					lowerTolerance = 360 + lowerTolerance;
					if ((angle1 >= 0 && angle1 <= upperTolerance) || (angle1 >= lowerTolerance && angle1 <= 359))
						return true;
					else
						return false;
				}
				if (upperTolerance > 359) {
					upperTolerance = upperTolerance - 360;
					if ((angle1 >= 0 && angle1 <= upperTolerance) || (angle1 >= lowerTolerance && angle1 <= 359))
						return true;
					else
						return false;
				} else {
					if ((angle1 <= angle2 + ANGLE_TOLERENCE) && (angle1 >= angle2 - ANGLE_TOLERENCE))
						return true;
					else
						return false;
				}
			}

		});

		ButtonGroup bg2 = new ButtonGroup();
		bg2.add(selectForce);
		bg2.add(selectMoment);

		ButtonGroup bg = new ButtonGroup();
		bg.add(selectProperyKnown);
		bg.add(selectProperyUnknown);

		ButtonGroup bg3 = new ButtonGroup();
		bg3.add(selectDirectionCW);
		bg3.add(selectDirectionCCW);

		tb.add(selectForce);
		tb.add(selectMoment);
		tb.addSeparator();
		tb.add(selectProperyKnown);
		tb.add(selectProperyUnknown);
		tb.addSeparator();
		tb.add(selectDirectionCW);
		tb.add(selectDirectionCCW);
		tb.addSeparator();
		tb.add(undoButton);
		tb.add(submitForces);
		tb.setVisible(false);
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
						// bi = imageResizing(bi, imageWidth, imageHeight);
						undoImageStack.push(deepCopy(bi));
						setImage(bi);
						cutCount = 1;
						cutsList.clear();
						fpList.clear();
						forceDataList.clear();
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
					drawOriginal();
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

		JMenuItem saveForceData = new JMenuItem("Save Force Data");
		saveForceData.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				StringBuilder sb = new StringBuilder();
				sb.append("Forces:\n");
				for (ForcePoint fp : forceDataList) {
					// FORMAT: x , y , is_correct , EntityType, EntityProperty,
					// EntityDirection, angle
					String typeString = (fp.type == EntityType.FORCE) ? "Force" : "Moment";
					String propertyString = (fp.property == EntityProperty.KNOWN) ? "Known" : "Unknown";
					String directionString = (fp.direction == EntityDirection.CLOCKWISE) ? "Clockwise"
							: "Anticlockwise";
					sb.append(fp.x + "," + fp.y + "," + fp.isCorrect() + "," + typeString + "," + propertyString + ","
							+ directionString + "," + fp.angle + "\n");
				}

				// add the points are incorrect i.e. no force moment attached to
				// it
				for (ForcePoint fp : fpList) {
					if (fp.isCorrect() == false) {
						String typeString = (fp.type == EntityType.FORCE) ? "Force" : "Moment";
						String propertyString = (fp.property == EntityProperty.KNOWN) ? "Known" : "Unknown";
						String directionString = (fp.direction == EntityDirection.CLOCKWISE) ? "Clockwise"
								: "Anticlockwise";
						sb.append(fp.x + "," + fp.y + "," + fp.isCorrect() + "," + typeString + "," + propertyString
								+ "," + directionString + "," + fp.angle + "\n");
					}
				}

				sb.append("End Forces");
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

				// System.out.println(sb.toString());

			}
		});

		JMenuItem loadForceData = new JMenuItem("Load Force Data");
		loadForceData.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				int result = chooser.showOpenDialog(gui);
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					ArrayList<String> fileText = readTextFile(f);
					ArrayList<String> forcesString = getForcesFromText(fileText);
					drawOriginal();
					fpList.clear();
					correctForceDataList.clear();
					forceDataList.clear();
					currentColor = DRAWING_COLOR;
					for (int i = 0; i < forcesString.size(); i++) {
						ForcePoint fp = getFpFromString(forcesString.get(i));
						correctForceDataList.add(fp);
						if (!fpListContainsLocation(fpList, fp)) {
							fpList.add(new ForcePoint(fp.getLocation()));
						}
					}

					for (ForcePoint fp : correctForceDataList) {
						displayForcePoint(fp.x, fp.y);
						if (fp.isCorrect == true) {
							if (fp.type == EntityType.FORCE)
								displayArrow(fp.x, fp.y, fp.angle, fp.property);
							else if (fp.type == EntityType.MOMENT)
								displayMoment(fp.x, fp.y, fp.direction, fp.property);
						}
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
				System.exit(0);

			}
		});

		file.add(openItem);
		file.add(saveFBDData);
		file.add(loadFBDData);
		file.add(saveForceData);
		file.add(loadForceData);
		file.add(exitItem);

		return file;

	}

	boolean fpListContainsLocation(ArrayList<ForcePoint> fpList, ForcePoint fp) {
		boolean retValue = false;
		for (ForcePoint p : fpList) {
			if (p.x == fp.x && p.y == fp.y) {
				retValue = true;
			}
		}
		return retValue;
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
				forceToolbar.setVisible(false);

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
				forceToolbar.setVisible(false);

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
				forceToolbar.setVisible(false);

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
				forceToolbar.setVisible(false);

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
				forceToolbar.setVisible(false);

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

		JMenuItem drawFP = new JMenuItem("Draw Force Point");
		drawFP.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				current_mode = MODE_DRAW_FP;
				statusLabel.setText("Mode: Drawing Force Point");
				deleteCuts.setEnabled(false);
				deleteAll.setEnabled(false);
				restartFBD.setEnabled(false);
				forceToolbar.setVisible(false);

				// drawOriginal();
				// drawForcePoints(fpList);

				// remove all mouselisteners first
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

				imageLabel.addMouseListener(new fpDrawingModeListener());

			}
		});

		JMenuItem deleteFP = new JMenuItem("Delete Force Point");
		deleteFP.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				current_mode = MODE_DEL_FP;
				statusLabel.setText("Mode: Delete Force Point");
				deleteCuts.setEnabled(true);
				deleteAll.setEnabled(true);
				restartFBD.setEnabled(false);
				forceToolbar.setVisible(false);

				drawOriginal();
				drawForcePoints(fpList);

				// remove all mouselisteners first
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

				imageLabel.addMouseListener(new FPDeleteModeListener());

			}
		});

		JMenuItem drawForces = new JMenuItem("Draw Forces");
		drawForces.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				current_mode = MODE_DRAW_FORCE;
				statusLabel.setText("Mode: Define Forces and Moments");
				deleteCuts.setEnabled(false);
				deleteAll.setEnabled(false);
				restartFBD.setEnabled(false);

				clearFBDDefinigVariables();
				 drawOriginal();
				 drawForcePoints(fpList);

				// setup defauls of forceToolbar
				selectForce.setSelected(true);
				selectMoment.setSelected(false);
				selectProperyKnown.setSelected(true);
				selectProperyUnknown.setSelected(false);
				selectDirectionCW.setSelected(true);
				selectDirectionCCW.setSelected(false);
				selectDirectionCW.setEnabled(false);
				selectDirectionCCW.setEnabled(false);
				submitForces.setVisible(false);

				currentType = EntityType.FORCE;
				currentProperty = EntityProperty.KNOWN;
				currentDirection = EntityDirection.CLOCKWISE;

				forceToolbar.setVisible(true);

				// SET FORCE COLOR
				currentColor = DRAWING_COLOR;

				// isFBDAnswered = false;
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

				imageLabel.addMouseListener(new DrawEntityListener());
				imageLabel.addMouseMotionListener(new DrawForcesMotionListener());

			}
		});

		JMenuItem forceTestMode = new JMenuItem("Test Mode");
		forceTestMode.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				current_mode = MODE_TEST_FORCE;
				statusLabel.setText("Mode: Test Mode Forces/Moments");
				deleteCuts.setEnabled(false);
				deleteAll.setEnabled(false);
				restartFBD.setEnabled(false);

				clearFBDDefinigVariables();
				drawOriginal();
				drawForcePoints(fpList);

				// setup defauls of forceToolbar
				selectForce.setSelected(true);
				selectMoment.setSelected(false);
				selectProperyKnown.setSelected(true);
				selectProperyUnknown.setSelected(false);
				selectDirectionCW.setSelected(true);
				selectDirectionCCW.setSelected(false);
				selectDirectionCW.setEnabled(false);
				selectDirectionCCW.setEnabled(false);
				submitForces.setVisible(true);
				submitForces.setEnabled(true);
				undoButton.setEnabled(true);

				currentType = EntityType.FORCE;
				currentProperty = EntityProperty.KNOWN;
				currentDirection = EntityDirection.CLOCKWISE;

				forceToolbar.setVisible(true);

				// SET FORCE COLOR
				currentColor = DRAWING_COLOR;

				if (forceDataList.size() > 0) {
					correctForceDataList = new ArrayList<ForcePoint>(forceDataList);
					forceDataList.clear();
				}

				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}

				imageLabel.addMouseListener(new DrawEntityListener());
				imageLabel.addMouseMotionListener(new DrawForcesMotionListener());

			}
		});

		force.add(drawFP);
		force.add(deleteFP);
		force.add(drawForces);
		force.add(forceTestMode);

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
		forcePointImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/x-30.png"));

		frame = new JFrame("FBD Software");
		frame.setSize(1100, 600);
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

	class fpDrawingModeListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			drawCut(e.getPoint());

			super.mouseClicked(e);
		}

		public void drawCut(Point point) {

			Graphics2D g = canvasImage.createGraphics();
			g.setColor(Color.black);

			g.drawImage(forcePointImage, point.x - 15, point.y - 15, null);
			g.dispose();
			imageLabel.repaint();
			fpList.add(new ForcePoint(point));

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

	class FPDeleteModeListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			Point clickedPoint = e.getPoint();
			for (ForcePoint p : fpList) {
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
						g.drawImage(forcePointImage, p.x - 15, p.y - 15, null);
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
						// System.out.println("Answered");
						// for (Line2D l : answerLineList) {
						// System.out.println((int) l.getX1() + "," + (int)
						// l.getY1() + "|" + (int) l.getX2() + ","
						// + (int) l.getY2());
						// }
						// System.out.println("Stored");
						// for (Line2D l : lineList) {
						// System.out.println((int) l.getX1() + "," + (int)
						// l.getY1() + "|" + (int) l.getX2() + ","
						// + (int) l.getY2());
						// }

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

	class DrawEntityListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (currentType == EntityType.FORCE) {
				drawForce(e);
			} else if (currentType == EntityType.MOMENT) {
				drawMoment(e);
			}

			super.mouseClicked(e);
		}

	}

	void drawForce(MouseEvent e) {
		Point clickedPoint = e.getPoint();
		if (isForcePointSelected == false) {

			for (ForcePoint p : fpList) {
				Rectangle clickThresholdRectangle = new Rectangle(p.x - 15, p.y - 15, 30, 30);
				if (clickThresholdRectangle.contains(clickedPoint)) {
					isForcePointSelected = true;
					firstForcePoint = p;

					// Graphics2D g = canvasImage.createGraphics();
					// g.setColor(Color.green);
					// g.drawRect(p.x - 15, p.y - 15, 30, 30);
					// g.dispose();
					// imageLabel.repaint();

					subImage = deepCopy(canvasImage);
					undoImageStack.push(deepCopy(canvasImage));
					int x1 = (p.x - arrowLenght) < 0 ? 0 : (p.x - arrowLenght);
					int y1 = (p.y - arrowLenght) < 0 ? 0 : (p.y - arrowLenght);
					int x2 = (p.x + arrowLenght) > canvasImage.getWidth() ? canvasImage.getWidth()
							: (p.x + arrowLenght);
					int y2 = (p.y + arrowLenght) > canvasImage.getHeight() ? canvasImage.getHeight()
							: (p.y + arrowLenght);
					int width = x2 - x1;
					int height = y2 - y1;

					// subImage = subImage.getSubimage(p.x - arrowLenght, p.y -
					// arrowLenght, arrowLenght * 2,
					// arrowLenght * 2);

					subImage = subImage.getSubimage(x1, y1, width, height);

				}
			}
		} else {
			isForcePointSelected = false;
			secondForcePoint = new ForcePoint(e.getPoint());
			double distance = firstForcePoint.distance(secondForcePoint);
			double ratio = arrowLineLength / distance;
			double new_X = ((1 - ratio) * firstForcePoint.getX()) + ratio * secondForcePoint.getX();
			double new_Y = ((1 - ratio) * firstForcePoint.getY()) + ratio * secondForcePoint.getY();
			ForcePoint newPoint = new ForcePoint((int) new_X, (int) new_Y);
			Graphics2D g = canvasImage.createGraphics();
			// g.drawImage(subImage, (int) firstForcePoint.getX() - arrowLenght,
			// (int) firstForcePoint.getY() - arrowLenght, (int)
			// firstForcePoint.getX() + arrowLenght,
			// (int) firstForcePoint.getY() + arrowLenght, 0, 0, arrowLenght *
			// 2, arrowLenght * 2, null);

			ForcePoint p = firstForcePoint;
			int x1 = (p.x - arrowLenght) < 0 ? 0 : (p.x - arrowLenght);
			int y1 = (p.y - arrowLenght) < 0 ? 0 : (p.y - arrowLenght);
			int x2 = (p.x + arrowLenght) > canvasImage.getWidth() ? canvasImage.getWidth() : (p.x + arrowLenght);
			int y2 = (p.y + arrowLenght) > canvasImage.getHeight() ? canvasImage.getHeight() : (p.y + arrowLenght);

			g.drawImage(subImage, x1, y1, x2, y2, 0, 0, subImage.getWidth(), subImage.getHeight(), null);
			g.dispose();
			imageLabel.repaint();

			drawArrow(firstForcePoint, newPoint);

			// mark the point as correct
			firstForcePoint.setCorrect(true);

			ForcePoint pointToAdd = new ForcePoint(firstForcePoint.x, firstForcePoint.y);
			pointToAdd.setCorrect(true);
			pointToAdd.setType(EntityType.FORCE);
			pointToAdd.setProperty(
					currentProperty == EntityProperty.KNOWN ? EntityProperty.KNOWN : EntityProperty.UNKNOWN);
			// calculate angle
			double angleRad = Math.atan2((new_Y - firstForcePoint.getY()) * -1, new_X - firstForcePoint.getX());
			double angleDeg = Math.toDegrees(angleRad);
			angleDeg = (angleDeg + 360) % 360;
			pointToAdd.setAngle((int) angleDeg);

			forceDataList.add(pointToAdd);
		}
	}

	void drawMoment(MouseEvent e) {
		Point clickedPoint = e.getPoint();
		Color arcColor = null;
		for (ForcePoint p : fpList) {
			Rectangle clickThresholdRectangle = new Rectangle(p.x - 15, p.y - 15, 30, 30);
			if (clickThresholdRectangle.contains(clickedPoint)) {

				undoImageStack.push(deepCopy(canvasImage));
				Graphics2D g = canvasImage.createGraphics();
				int arcRadius = arrowLenght / 2;
				arcColor = currentColor;
				if (currentProperty == EntityProperty.KNOWN) {
					Stroke dark = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
					g.setStroke(dark);
				}

				else if (currentProperty == EntityProperty.UNKNOWN) {
					Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
							new float[] { 4 }, 0);
					g.setStroke(dashed);
				}

				g.setColor(arcColor);

				if (currentDirection == EntityDirection.CLOCKWISE) {
					g.drawArc(p.x - arcRadius, p.y - arcRadius, 2 * arcRadius, 2 * arcRadius, 300, 120);

					Point arcArrowStartPoint = new Point((int) (arcRadius * Math.cos(Math.toRadians(315))),
							(int) (-1 * arcRadius * Math.sin(Math.toRadians(315))));
					arcArrowStartPoint = new Point(p.x + arcArrowStartPoint.x, p.y + arcArrowStartPoint.y);
					Point arcArrowEndPoint = new Point((int) (arcRadius * Math.cos(Math.toRadians(300))),
							(int) (-1 * arcRadius * Math.sin(Math.toRadians(300))));
					arcArrowEndPoint = new Point(p.x + arcArrowEndPoint.x, p.y + arcArrowEndPoint.y);
					drawArrow(arcArrowStartPoint, arcArrowEndPoint);

				} else if (currentDirection == EntityDirection.ANTICLOCKWISE) {
					g.drawArc(p.x - arcRadius, p.y - arcRadius, 2 * arcRadius, 2 * arcRadius, 120, 120);

					Point arcArrowStartPoint = new Point((int) (arcRadius * Math.cos(Math.toRadians(135))),
							(int) (-1 * arcRadius * Math.sin(Math.toRadians(135))));
					arcArrowStartPoint = new Point(p.x + arcArrowStartPoint.x, p.y + arcArrowStartPoint.y);
					Point arcArrowEndPoint = new Point((int) (arcRadius * Math.cos(Math.toRadians(120))),
							(int) (-1 * arcRadius * Math.sin(Math.toRadians(120))));
					arcArrowEndPoint = new Point(p.x + arcArrowEndPoint.x, p.y + arcArrowEndPoint.y);
					drawArrow(arcArrowStartPoint, arcArrowEndPoint);

				}

				g.dispose();
				imageLabel.repaint();

				// mark p as correct
				p.setCorrect(true);
				ForcePoint pointToAdd = new ForcePoint(p.x, p.y);
				pointToAdd.setCorrect(true);
				pointToAdd.setType(EntityType.MOMENT);
				pointToAdd.setProperty(
						currentProperty == EntityProperty.KNOWN ? EntityProperty.KNOWN : EntityProperty.UNKNOWN);
				pointToAdd.setDirection(currentDirection == EntityDirection.CLOCKWISE ? EntityDirection.CLOCKWISE
						: EntityDirection.ANTICLOCKWISE);

				forceDataList.add(pointToAdd);

			}
		}
	}

	class DrawForcesMotionListener implements MouseMotionListener {

		@Override
		public void mouseDragged(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (isForcePointSelected == true) {
				secondForcePoint = new ForcePoint(e.getPoint());
				double distance = firstForcePoint.distance(secondForcePoint);
				if (distance == 0)
					return;
				double ratio = arrowLineLength / distance;
				double new_X = ((1 - ratio) * firstForcePoint.getX()) + ratio * secondForcePoint.getX();
				double new_Y = ((1 - ratio) * firstForcePoint.getY()) + ratio * secondForcePoint.getY();
				ForcePoint newPoint = new ForcePoint((int) new_X, (int) new_Y);
				double angleRad = Math.atan2((new_Y - firstForcePoint.getY()) * -1, new_X - firstForcePoint.getX());
				double angleDeg = Math.toDegrees(angleRad);
				angleDeg = (angleDeg + 360) % 360;

				Graphics2D g = canvasImage.createGraphics();
				// g.drawImage(subImage, (int) firstForcePoint.getX() -
				// arrowLenght,
				// (int) firstForcePoint.getY() - arrowLenght, (int)
				// firstForcePoint.getX() + arrowLenght,
				// (int) firstForcePoint.getY() + arrowLenght, 0, 0, arrowLenght
				// * 2, arrowLenght * 2, null);
				ForcePoint p = firstForcePoint;
				int x1 = (p.x - arrowLenght) < 0 ? 0 : (p.x - arrowLenght);
				int y1 = (p.y - arrowLenght) < 0 ? 0 : (p.y - arrowLenght);
				int x2 = (p.x + arrowLenght) > canvasImage.getWidth() ? canvasImage.getWidth() : (p.x + arrowLenght);
				int y2 = (p.y + arrowLenght) > canvasImage.getHeight() ? canvasImage.getHeight() : (p.y + arrowLenght);

				g.drawImage(subImage, x1, y1, x2, y2, 0, 0, subImage.getWidth(), subImage.getHeight(), null);

				g.setColor(Color.BLACK);
				g.drawString(Integer.toString((int) angleDeg) + "°", firstForcePoint.x - 15, firstForcePoint.y - 15);
				g.dispose();
				imageLabel.repaint();
				drawArrow(firstForcePoint, newPoint);

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

	void clearForceData() {
		firstForcePoint = null;
		secondForcePoint = null;
		fpList.clear();
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

	private ArrayList<String> getForcesFromText(ArrayList<String> fileText) {
		ArrayList<String> pointsString = new ArrayList<String>();
		int dataIdx = 0;
		while (!fileText.get(dataIdx).contains("Forces:"))
			dataIdx++;
		dataIdx++;
		while (!fileText.get(dataIdx).contains("End Forces") && dataIdx < fileText.size()) {
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

	private ForcePoint getFpFromString(String str) {
		ForcePoint fp = null;
		String[] strSplit = str.split(",");
		int x = Integer.parseInt(strSplit[0]);
		int y = Integer.parseInt(strSplit[1]);

		boolean isCorrect = false;
		if (strSplit[2].equals("true"))
			isCorrect = true;
		else if (strSplit[2].equals("false"))
			isCorrect = false;

		EntityType type = null;
		if (strSplit[3].equals("Force"))
			type = EntityType.FORCE;
		else if (strSplit[3].equals("Moment"))
			type = EntityType.MOMENT;

		EntityProperty property = null;
		if (strSplit[4].equals("Known"))
			property = EntityProperty.KNOWN;
		else if (strSplit[4].equals("Unknown"))
			property = EntityProperty.UNKNOWN;

		EntityDirection direction = null;
		if (strSplit[5].equals("Clockwise"))
			direction = EntityDirection.CLOCKWISE;
		else if (strSplit[5].equals("Anticlockwise"))
			direction = EntityDirection.ANTICLOCKWISE;

		int angle = Integer.parseInt(strSplit[6]);

		fp = new ForcePoint(x, y);
		fp.setCorrect(isCorrect);
		fp.setType(type);
		fp.setProperty(property);
		fp.setDirection(direction);
		fp.setAngle(angle);
		return fp;
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

	void drawForcePoints(ArrayList<ForcePoint> fpList) {
		Graphics2D g = canvasImage.createGraphics();
		for (ForcePoint p : fpList) {
			if (p.isCorrect()) {
				p.setCorrect(false);
			}
			g.drawImage(forcePointImage, p.x - 15, p.y - 15, null);
			// g.setColor(Color.BLACK);
			// g.drawString(Integer.toString(p.getCutCounter()), p.x - 15, p.y -
			// 15);

		}
		g.dispose();
		imageLabel.repaint();
	}

	private void drawArrow(Point p1, Point p2) {
		int x1 = (int) p1.getX();
		int y1 = (int) p1.getY();
		int x2 = (int) p2.getX();
		int y2 = (int) p2.getY();

		Color arrowColor = null;

		Graphics2D g2d = canvasImage.createGraphics();
		AffineTransform tx = new AffineTransform();
		Line2D.Double line = new Line2D.Double(x1, y1, x2, y2);
		arrowColor = currentColor;
		if (currentProperty == EntityProperty.KNOWN) {
			Stroke dark = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0);
			g2d.setStroke(dark);
		}

		else if (currentProperty == EntityProperty.UNKNOWN) {
			Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 4 }, 0);
			g2d.setStroke(dashed);
		}

		g2d.setColor(arrowColor);
		g2d.drawLine(x1, y1, x2, y2);

		Polygon arrowHead = new Polygon();
		arrowHead.addPoint(0, 5);
		arrowHead.addPoint(-5, -5);
		arrowHead.addPoint(5, -5);

		tx.setToIdentity();
		double angle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1);
		tx.translate(line.x2, line.y2);
		tx.rotate((angle - Math.PI / 2d));

		Graphics2D g = (Graphics2D) g2d.create();
		g.setColor(arrowColor);

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

	public class SizedStack<T> extends Stack<T> {
		private int maxSize;

		public SizedStack(int size) {
			super();
			this.maxSize = size;
		}

		@Override
		public T push(T object) {
			// If the stack is too big, remove elements until it's the right
			// size.
			while (this.size() >= maxSize) {
				this.remove(0);
			}
			return super.push(object);
		}
	}

	private void undo() {
		if (current_mode == MODE_DRAW_FORCE || current_mode == MODE_TEST_FORCE) {

			Graphics2D g = canvasImage.createGraphics();
			if (undoImageStack.isEmpty())
				return;
			undoImage = undoImageStack.pop();
			if (undoImage != null) {
				g.drawImage(undoImage, 0, 0, canvasImage.getWidth(), canvasImage.getHeight(), 0, 0,
						canvasImage.getWidth(), canvasImage.getHeight(), null);
				g.dispose();
				imageLabel.repaint();
				ForcePoint pointToRemove = forceDataList.get(forceDataList.size() - 1);
				forceDataList.remove(forceDataList.size() - 1);
				// if this list does not have this point anymore i.e. all
				// forces/moments related to it are removed, then mark the point
				// as
				// incorrect in fpList
				if (!fpListContainsLocation(forceDataList, pointToRemove)) {
					for (ForcePoint p : fpList) {
						if (p.getLocation().equals(pointToRemove.getLocation())) {
							p.setCorrect(false);
						}
					}
				}
			}
		}
	}

	private void displayMoment(int x, int y, EntityDirection direction, EntityProperty property) {
		Color arcColor = null;
		currentProperty = property;
		currentDirection = direction;
		Point p = new Point(x, y);
		Graphics2D g = canvasImage.createGraphics();
		int arcRadius = arrowLenght / 2;
		arcColor = currentColor;
		if (currentProperty == EntityProperty.KNOWN) {
			Stroke dark = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
			g.setStroke(dark);
		} else if (currentProperty == EntityProperty.UNKNOWN) {
			Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 4 }, 0);
			g.setStroke(dashed);
		}
		g.setColor(arcColor);

		if (currentDirection == EntityDirection.CLOCKWISE) {
			g.drawArc(p.x - arcRadius, p.y - arcRadius, 2 * arcRadius, 2 * arcRadius, 300, 120);

			Point arcArrowStartPoint = new Point((int) (arcRadius * Math.cos(Math.toRadians(315))),
					(int) (-1 * arcRadius * Math.sin(Math.toRadians(315))));
			arcArrowStartPoint = new Point(p.x + arcArrowStartPoint.x, p.y + arcArrowStartPoint.y);
			Point arcArrowEndPoint = new Point((int) (arcRadius * Math.cos(Math.toRadians(300))),
					(int) (-1 * arcRadius * Math.sin(Math.toRadians(300))));
			arcArrowEndPoint = new Point(p.x + arcArrowEndPoint.x, p.y + arcArrowEndPoint.y);
			drawArrow(arcArrowStartPoint, arcArrowEndPoint);

		} else if (currentDirection == EntityDirection.ANTICLOCKWISE) {
			g.drawArc(p.x - arcRadius, p.y - arcRadius, 2 * arcRadius, 2 * arcRadius, 120, 120);

			Point arcArrowStartPoint = new Point((int) (arcRadius * Math.cos(Math.toRadians(135))),
					(int) (-1 * arcRadius * Math.sin(Math.toRadians(135))));
			arcArrowStartPoint = new Point(p.x + arcArrowStartPoint.x, p.y + arcArrowStartPoint.y);
			Point arcArrowEndPoint = new Point((int) (arcRadius * Math.cos(Math.toRadians(120))),
					(int) (-1 * arcRadius * Math.sin(Math.toRadians(120))));
			arcArrowEndPoint = new Point(p.x + arcArrowEndPoint.x, p.y + arcArrowEndPoint.y);
			drawArrow(arcArrowStartPoint, arcArrowEndPoint);

		}

		g.dispose();
		imageLabel.repaint();

	}

	private void displayArrow(int x, int y, int angle, EntityProperty property) {
		Point p1 = new Point(x, y);
		int new_X = (int) (arrowLineLength * Math.cos(Math.toRadians(angle)));
		int new_Y = (int) (-1 * arrowLineLength * Math.sin(Math.toRadians(angle)));
		Point p2 = new Point(x + new_X, y + new_Y);
		currentProperty = property;
		drawArrow(p1, p2);

	}

	private void displayForcePoint(int x, int y) {
		Graphics2D g = canvasImage.createGraphics();
		g.setColor(Color.black);

		g.drawImage(forcePointImage, x - 15, y - 15, null);
		g.dispose();
		imageLabel.repaint();

	}
	
	private void displayCircle(int x, int y) {
		Graphics2D g = canvasImage.createGraphics();
		g.setColor(currentColor);
		g.drawOval(x-15, y-15, 30, 30);
		g.dispose();
		imageLabel.repaint();
		
	}

}
