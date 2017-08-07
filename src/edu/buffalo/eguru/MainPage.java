package edu.buffalo.eguru;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
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

	int current_mode = MODE_NONE;
	private Image scissor = null;
	int cutCount = 1;
	
	JButton deleteCuts, deleteAll;

	ArrayList<ZPoint> cutsList = new ArrayList<ZPoint>();

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
				for(Iterator<ZPoint> iterator = cutsList.iterator(); iterator.hasNext();) {
					ZPoint p = iterator.next();					
					if(p.isSelected) {
						Graphics2D g = canvasImage.createGraphics();
						g.drawImage(originalImage, p.x-15, p.y-25, p.x+16, p.y+16, p.x-15, p.y-25, p.x+16, p.y+16, null);
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
				for(Iterator<ZPoint> iterator = cutsList.iterator(); iterator.hasNext();) {
					ZPoint p = iterator.next();					
						Graphics2D g = canvasImage.createGraphics();
						g.drawImage(originalImage, p.x-15, p.y-25, p.x+16, p.y+16, p.x-15, p.y-25, p.x+16, p.y+16, null);
						g.dispose();
						iterator.remove();					
				}
				cutCount = 1;
				imageLabel.repaint();

			}
		});

		// Start FBD

		JButton restartFBD = new JButton("Restart FBD");
		restartFBD.setMnemonic('r');
		restartFBD.setToolTipText("Restart Free Body Diagram");
		restartFBD.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		deleteCuts.setEnabled(false);
		deleteAll.setEnabled(false);
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
				int result = ch.showSaveDialog(gui);
				if (result == JFileChooser.APPROVE_OPTION) {
					try {
						BufferedImage bi = ImageIO.read(ch.getSelectedFile());
						bi = imageResizing(bi, imageWidth, imageHeight);
						setImage(bi);
						cutCount = 1;
						cutsList.clear();
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
				// TODO Auto-generated method stub

			}
		});

		JMenuItem loadFBDData = new JMenuItem("Load FBD Data");
		loadFBDData.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub

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
				
				for (MouseListener m : imageLabel.getMouseListeners()) {
					imageLabel.removeMouseListener(m);
				}
				imageLabel.addMouseListener(new fbdModeListener());
			}
		});

		modes.add(none);
		modes.add(drawCuts);
		modes.add(selectCuts);
		modes.add(defineFBD);

		return modes;
	}

	JMenuBar getMenuBar() {
		JMenuBar mb = new JMenuBar();
		mb.add(getFileMenu());
		mb.add(getModesMenu());
		return mb;
	}

	private void setImage(BufferedImage image) {
		originalImage = image;
		int w = image.getWidth();
		int h = image.getHeight();
		canvasImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = this.canvasImage.createGraphics();

		g.drawImage(image, 0, 0, gui);

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
			// System.out.println("Xcord: " + e.getX() + " Ycord: " + e.getY());
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
					if(!p.isSelected()) {
						Graphics2D g = canvasImage.createGraphics();
						g.setColor(Color.red);
//						g.drawImage(originalImage, p.x-15, p.y-25, p.x+15, p.y+15, p.x-15, p.y-25, p.x+15, p.y+15, null);
						g.drawRect(p.x - 15, p.y - 15, 30, 30);
						g.dispose();
						imageLabel.repaint();
						p.setSelected(true);
					}
					else {
						Graphics2D g = canvasImage.createGraphics();
						g.drawImage(originalImage, p.x-15, p.y-15, p.x+16, p.y+16, p.x-15, p.y-15, p.x+16, p.y+16, null);
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
//			Point clickedPoint = e.getPoint();
//			for (ZPoint p : cutsList) {
//				Rectangle clickThresholdRectangle = new Rectangle(p.x - 15, p.y - 15, 30, 30);
//				if (clickThresholdRectangle.contains(clickedPoint)) {
//					Graphics2D g = canvasImage.createGraphics();
//					g.setColor(Color.red);
//					g.drawRect(p.x - 15, p.y - 15, 30, 30);
//					g.dispose();
//					imageLabel.repaint();
//
//				}
//			}
			super.mouseClicked(e);
		}

	}


}
