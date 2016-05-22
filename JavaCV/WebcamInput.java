import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;


public class WebcamInput extends Thread {
	
	public static void main(String[] args) {

		Thread thread = new WebcamInput();
		thread.start();
		
		JOptionPane pane = new JOptionPane("Close?", JOptionPane.INFORMATION_MESSAGE);
		JDialog dialog = pane.createDialog(null, "");
		dialog.setModal(false);
		
		final Object obj = new Object();
		
		dialog.addComponentListener(new ComponentAdapter() {
			
			public void componentHidden(ComponentEvent e) {
				
				synchronized (obj) {

					obj.notify();
				}
			}
		});
		
		dialog.setVisible(true);
		
		try {

			synchronized (obj) {
				
				obj.wait();
			}
		}
		catch (InterruptedException e1) {

			e1.printStackTrace();

		}

		thread.interrupt();
		
		dialog.dispose();
	}
	
	@Override
	public void run() {
		
		FrameGrabber grabber = new OpenCVFrameGrabber("");

		try {
			
			grabber.start();
		}
		catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			
			e.printStackTrace();
			return;
		}
		
		PictureShower pictureShower = new PictureShower(1080, 720);
		pictureShower.start();
		
		JSlider thresholdBar = new JSlider(SwingConstants.HORIZONTAL, 0, 300, 150);
		thresholdBar.setMajorTickSpacing(75);
		thresholdBar.setMinorTickSpacing(25);
		thresholdBar.setPaintTicks(true);
		thresholdBar.setPaintLabels(true);
		thresholdBar.setPreferredSize(null);
	
		JSlider ratioBar = new JSlider(SwingConstants.HORIZONTAL, 200, 300, 300);
		Hashtable<Object, Object> labelTable = new Hashtable<Object, Object>();
		for (int i = 200; i <= 300; i += 20) {
			
			labelTable.put(i, new JLabel("" + i / 100.0));
		}
		ratioBar.setLabelTable(labelTable);
		ratioBar.setMinorTickSpacing(5);
		ratioBar.setMajorTickSpacing(20);
		ratioBar.setPaintTicks(true);
		ratioBar.setPaintLabels(true);
		ratioBar.setPreferredSize(null);
		
		JFrame userControl = new JFrame();
		userControl.setLayout(new BoxLayout(userControl.getContentPane(), BoxLayout.PAGE_AXIS));
		userControl.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		userControl.add(thresholdBar);
		userControl.add(ratioBar);
		userControl.pack();
		userControl.setVisible(true);
		
		try {
			while (!isInterrupted()) {

				Mat mat = new OpenCVFrameConverter.ToMat().convert(grabber.grab());
				
				//long time = System.currentTimeMillis();
				
				Mat gray = new Mat(mat.size());
				opencv_imgproc.cvtColor(mat, gray, opencv_imgproc.COLOR_BGR2GRAY);

				int threshold = thresholdBar.getValue();
				opencv_imgproc.Canny(gray, gray, threshold, threshold * (ratioBar.getValue() / 100.0));
				
				/*
				 * Suggestion:
				MatVector contours = new MatVector();
				Mat hierarchy = new Mat();
				opencv_imgproc.findContours(gray, contours, hierarchy, opencv_imgproc.RETR_TREE, opencv_imgproc.CV_CHAIN_APPROX_SIMPLE);
				 */
				
				pictureShower.update(gray);
			}//end while
		}//end try
		catch (org.bytedeco.javacv.FrameGrabber.Exception e) {

			e.printStackTrace();
		}

		System.out.println("Exiting...");
		
		try {
			grabber.stop();
		} catch (org.bytedeco.javacv.FrameGrabber.Exception e) {

			e.printStackTrace();
		}

		pictureShower.interrupt();
		
		Thread.interrupted();
		userControl.dispose();
		
		try {
			
			pictureShower.join();
		}
		catch (InterruptedException e) {
			
			//e.printStackTrace();
		}
		
		System.out.println("Goodbye");
	}
}