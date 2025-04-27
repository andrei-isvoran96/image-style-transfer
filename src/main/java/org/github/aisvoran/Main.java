package org.github.aisvoran;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
	private static BufferedImage contentImg = null;
	private static BufferedImage styleImg = null;

	// Calculate Euclidean distance between two RGB colors
	private static double colorDistance(int rgb1, int rgb2) {
		int r1 = (rgb1 >> 16) & 0xFF;
		int g1 = (rgb1 >> 8) & 0xFF;
		int b1 = rgb1 & 0xFF;
		int r2 = (rgb2 >> 16) & 0xFF;
		int g2 = (rgb2 >> 8) & 0xFF;
		int b2 = rgb2 & 0xFF;
		return Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));
	}

	private static int[] buildPalette(BufferedImage img, int paletteSize) {
		int[] palette = new int[paletteSize];
		int w = img.getWidth();
		int h = img.getHeight();
		int grid = (int)Math.sqrt(paletteSize);
		int idx = 0;
		for (int gy = 0; gy < grid; gy++) {
			for (int gx = 0; gx < grid; gx++) {
				int x = gx * w / grid + w / (2 * grid);
				int y = gy * h / grid + h / (2 * grid);
				if (x >= w) x = w - 1;
				if (y >= h) y = h - 1;
				palette[idx++] = img.getRGB(x, y);
				if (idx >= paletteSize) break;
			}
			if (idx >= paletteSize) break;
		}
		return palette;
	}

	private static int findClosestColor(int rgb, int[] palette) {
		double minDist = Double.MAX_VALUE;
		int best = palette[0];
		for (int c : palette) {
			double dist = colorDistance(rgb, c);
			if (dist < minDist) {
				minDist = dist;
				best = c;
			}
		}
		return best;
	}

	private static void styleTransfer(BufferedImage contentImg, BufferedImage styleImg, File outputFile) throws IOException {
		int width = contentImg.getWidth();
		int height = contentImg.getHeight();
		BufferedImage outputImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] palette = buildPalette(styleImg, 32);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int contentRGB = contentImg.getRGB(x, y);
				int bestRGB = findClosestColor(contentRGB, palette);
				outputImg.setRGB(x, y, bestRGB);
			}
		}
		ImageIO.write(outputImg, "jpg", outputFile);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Image Style Transfer (Drag & Drop)");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(800, 700);
			frame.setLayout(new BorderLayout());

			JPanel imagesPanel = new JPanel(new GridLayout(1, 2, 10, 10));
			JLabel contentLabel = new JLabel("Drop Content Image Here", SwingConstants.CENTER);
			JLabel styleLabel = new JLabel("Drop Style Image Here", SwingConstants.CENTER);
			contentLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
			styleLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
			contentLabel.setPreferredSize(new Dimension(350, 350));
			styleLabel.setPreferredSize(new Dimension(350, 350));
			imagesPanel.add(contentLabel);
			imagesPanel.add(styleLabel);

			// Output image label
			JLabel outputLabel = new JLabel("Output will appear here", SwingConstants.CENTER);
			outputLabel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
			outputLabel.setPreferredSize(new Dimension(350, 350));

			JPanel outputPanel = new JPanel(new BorderLayout());
			outputPanel.add(outputLabel, BorderLayout.CENTER);
			outputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
			centerPanel.add(imagesPanel);
			centerPanel.add(outputPanel);

			// Drag and drop support
			new DropTarget(contentLabel, new DropTargetAdapter() {
				public void drop(DropTargetDropEvent dtde) {
					try {
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						java.util.List<File> droppedFiles = (java.util.List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						if (!droppedFiles.isEmpty()) {
							File file = droppedFiles.get(0);
							BufferedImage img = ImageIO.read(file);
							if (img == null) {
								JOptionPane.showMessageDialog(frame, "Failed to load image: Unsupported or corrupted file.");
								return;
							}
							contentImg = img;
							contentLabel.setIcon(new ImageIcon(contentImg.getScaledInstance(350, 350, Image.SCALE_SMOOTH)));
							contentLabel.setText("");
						}
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(frame, "Failed to load image: " + ex.getMessage());
					}
				}
			});
			new DropTarget(styleLabel, new DropTargetAdapter() {
				public void drop(DropTargetDropEvent dtde) {
					try {
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						java.util.List<File> droppedFiles = (java.util.List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
						if (!droppedFiles.isEmpty()) {
							File file = droppedFiles.get(0);
							BufferedImage img = ImageIO.read(file);
							if (img == null) {
								JOptionPane.showMessageDialog(frame, "Failed to load image: Unsupported or corrupted file.");
								return;
							}
							styleImg = img;
							styleLabel.setIcon(new ImageIcon(styleImg.getScaledInstance(350, 350, Image.SCALE_SMOOTH)));
							styleLabel.setText("");
						}
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(frame, "Failed to load image: " + ex.getMessage());
					}
				}
			});

			JButton transferButton = new JButton("Transfer Style and Save as output.jpg");
			transferButton.addActionListener(e -> {
				if (contentImg == null || styleImg == null) {
					JOptionPane.showMessageDialog(frame, "Please drop both images first.");
					return;
				}
				// Resize style image if needed
				BufferedImage styleImgResized = styleImg;
				if (contentImg.getWidth() != styleImg.getWidth() || contentImg.getHeight() != styleImg.getHeight()) {
					Image tmp = styleImg.getScaledInstance(contentImg.getWidth(), contentImg.getHeight(), Image.SCALE_SMOOTH);
					BufferedImage resized = new BufferedImage(contentImg.getWidth(), contentImg.getHeight(), BufferedImage.TYPE_INT_RGB);
					Graphics2D g2d = resized.createGraphics();
					g2d.drawImage(tmp, 0, 0, null);
					g2d.dispose();
					styleImgResized = resized;
				}
				transferButton.setEnabled(false);
				transferButton.setText("Processing...");
				BufferedImage finalStyleImgResized = styleImgResized;
				SwingWorker<Void, Void> worker = new SwingWorker<>() {
					@Override
					protected Void doInBackground() {
						try {
							styleTransfer(contentImg, finalStyleImgResized, new File("output.jpg"));
						} catch (IOException ex) {
							JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
						}
						return null;
					}
					@Override
					protected void done() {
						transferButton.setEnabled(true);
						transferButton.setText("Transfer Style and Save as output.jpg");
						// Load and display output image
						try {
							BufferedImage outputImg = ImageIO.read(new File("output.jpg"));
							if (outputImg != null) {
								outputLabel.setIcon(new ImageIcon(outputImg.getScaledInstance(350, 350, Image.SCALE_SMOOTH)));
								outputLabel.setText("");
							} else {
								outputLabel.setIcon(null);
								outputLabel.setText("Failed to load output image");
							}
						} catch (IOException ex) {
							outputLabel.setIcon(null);
							outputLabel.setText("Failed to load output image");
						}
						JOptionPane.showMessageDialog(frame, "Style transfer complete! Saved as output.jpg");
					}
				};
				worker.execute();
			});

			frame.add(centerPanel, BorderLayout.CENTER);
			frame.add(transferButton, BorderLayout.SOUTH);
			frame.setVisible(true);
		});
	}
}