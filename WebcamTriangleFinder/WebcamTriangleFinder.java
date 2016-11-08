import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.IntBuffer;
import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;


public class WebcamTriangleFinder extends Thread {
	
	//***********************************
	static final boolean CONNECT_TO_ROBOT = true;
	//***********************************

	static final String IP = "localhost";
	static final int PORT_NUMBER = 5005;
	
	static final String FORWARD = "a:-100|";
	static final String BACKWARD = "a:100|";
	static final String LEFT = "lr:-100:0|";
	static final String RIGHT = "lr:0:-100|";
	static final String STOP = "a:0|";
	
	static final short NUM_TRIANGLE_SIDES = 3;
	
	public static void main(String[] args) {

		Thread thread = new WebcamTriangleFinder();
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
		
		Socket socket;
		DataInputStream in;
		DataOutputStream out;
		if (CONNECT_TO_ROBOT) {
			
			try {

				socket = new Socket(IP, PORT_NUMBER);
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
			}
			catch (IOException e) {

				e.printStackTrace();
				return;
			}
		}
		
		FrameGrabber grabber = new OpenCVFrameGrabber("");

		try {
			
			grabber.start();
		}
		catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
			
			e.printStackTrace();
			
			try {
				
				socket.close();
			}
			catch (IOException e1) {
				
				e1.printStackTrace();
			}
			
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
				MatVector contours = new MatVector();
				Mat hierarchy = new Mat();
				opencv_imgproc.findContours(gray, contours, hierarchy, opencv_imgproc.RETR_TREE, opencv_imgproc.CV_CHAIN_APPROX_SIMPLE);
				
				if (hierarchy.arrayData() == null)
					
					continue;
				
				int[] childs = parseHierarchyChildren(hierarchy);

				//IplImage hierachy = new IplImage(hierarchy);

				//Mat dest = mat;
				
				String command = STOP;
				for (int i = 0; i < Integer.MAX_VALUE && i < contours.size(); i++) {
					
					Mat approx = new Mat();
					opencv_imgproc.approxPolyDP(contours.get(i), approx, opencv_imgproc.arcLength(contours.get(i), true) * .01, true);
					
					//checks if polygon is valid
					if (!(Math.abs(opencv_imgproc.contourArea(contours.get(i))) >= 4000 && opencv_imgproc.isContourConvex(approx)))//15426
						
						continue;

					if (approx.rows() >= 5 && approx.cols() >= 1) {
						
						//CvScalar children = opencv_core.cvGet2D(hierachy, 0, i);
						//according to API; hierarchy[i][2] gives child of current contour (i)
						int innerApproxIndex = childs[i]; // (int) children.get(2);
						//System.out.println(innerApproxIndex == childs[i]);
						
						if (innerApproxIndex < 0)
							
							continue;
						
						Mat innerApprox = new Mat();
						opencv_imgproc.approxPolyDP(contours.get(innerApproxIndex), innerApprox, opencv_imgproc.arcLength(contours.get(innerApproxIndex), true) * .01, true);
						
						//checks if polygon is valid
						if (!(Math.abs(opencv_imgproc.contourArea(contours.get(i))) >= 1000 && opencv_imgproc.isContourConvex(innerApprox)))//15426-abitrary # that used to work
							
							continue;

						//if not triangle (inside of circle)
						if (!(innerApprox.rows() == 3 && innerApprox.cols() >= 1)) {
							
							continue;
						}
						
						java.nio.Buffer buffer = innerApprox.createBuffer();
						
						if (!(buffer instanceof IntBuffer))
							
							continue;

						Point[] points = getReversedPoints((IntBuffer) buffer);//new Point[innerApprox.rows()];

						double[] distances = new double[innerApprox.rows()];//distances squared that correspond to points of triangle
						
						for (int r = 0; r < points.length; r++) {
							
							Point prev = points[(r - 1 < 0) ? (points.length - 1) : (r - 1)];
							Point next = points[(r + 1 >= points.length) ? (0) : (r + 1)];
							
							distances[r] = Math.pow(prev.x() - next.x(), 2) + Math.pow(prev.y() - next.y(), 2);
						}
						
						//find shortest side of triangle and point corresponding to it
						double min = distances[0];
						int index = 0;
						for (int r = 1; r < distances.length; r++) {
							
							if (distances[r] < min) {
								
								min = distances[r];
								index = r;
							}
						}
						
						Point curr = points[index];
						//System.out.println(curr.y() + " " + curr.x());
						Point prev = points[(index - 1 < 0) ? (points.length - 1) : (index - 1)];
						Point next = points[(index + 1 >= points.length) ? (0) : (index + 1)];
						
						if (next.x() == prev.x()) {//vertical triangle
							
							//opencv_imgproc.circle(dest, new Point(curr.y(), next.x()), 5, Scalar.GREEN);
							if (curr.x() <= next.x()) {//upside
								
								System.out.println("FORWARD1");
								command = BACKWARD;
							}
							else {//downside	
								
								System.out.println("BACKWARD1");
								command = FORWARD;
							}
						}
						else if (next.y() == prev.y()) {//horizontal triangle
							
							//opencv_imgproc.circle(dest, new Point(next.y(), curr.x()), 5, Scalar.GREEN);
							if (curr.y() <= next.y()) {//leftside
								
								System.out.println("LEFT1");
								command = LEFT;
							}
							else {//rightside
								
								System.out.println("RIGHT1");
								command = RIGHT;
							}
						}
						else {

							//from here on, Cartesian points are used
							double slope = (double) (next.x() - prev.x()) / (next.y() - prev.y());

							//coordinates for the interesection of the altitude line to the shortest side of the triangle
							double x = ((slope / (slope * slope + 1)) * (slope * next.y() - next.x() + curr.y() / slope + curr.x()));
							int y = (int) (slope * (x - next.y()) + next.x());
							
							//Point reference = new Point((int) x, y);
							
							//current points flipped to original
							int xDist = (int) x - curr.y();
							int yDist = y - curr.x();
							
							//if angle is greater than 45 degrees from nearest x axis
							if (Math.abs(yDist) > Math.abs(xDist)) {
								
								if (yDist >= 0) {
									
									System.out.println("FORWARD");
									command = FORWARD;
								}
								else {
									
									System.out.println("BACKWARD");
									command = BACKWARD;
								}
							}
							else {
								
								if (xDist >= 0) {
									
									System.out.println("RIGHT");
									command = RIGHT;
								}
								else {
									
									System.out.println("LEFT");
									command = LEFT;
								}
							}
							
							//opencv_imgproc.circle(dest, reference, 5, Scalar.GREEN);
							
						}
						
						break;//found image
					}
				}//end for
				
				if (CONNECT_TO_ROBOT) {

					try {

						out.writeBytes(command);
						in.readByte();
					}
					catch (IOException e) {

						System.err.println(e.getMessage());
					}
				}//end if

				pictureShower.update(gray);
			}//end while
		}//end try
		catch (org.bytedeco.javacv.FrameGrabber.Exception e) {

			e.printStackTrace();
		}
		//catch (InterruptedException e) {
			
			
		//}

		System.out.println("Exiting...");
		
		if (!socket.isClosed()) {
			
			try {
				socket.close();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
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
	
	private int[] parseHierarchyChildren(Mat hierarchy) {
		
		java.nio.Buffer buffer = hierarchy.createBuffer();
		if (buffer instanceof IntBuffer) {
			
			IntBuffer intBuffer = (IntBuffer) buffer;
			if (intBuffer.limit() < 3)
				
				return null;
			intBuffer.position(2);
			int[] childs = new int[hierarchy.cols()];
			for (int i = 0; i < childs.length && intBuffer.limit() - intBuffer.position() >= 4; i++) {
				
				childs[i] = intBuffer.get();
				intBuffer.position(intBuffer.position() + 3);
			}
			return childs;
		}
		return null;
	}
	
	private Point[] getReversedPoints(IntBuffer buffer) {
		
		Point[] coordinates = new Point[NUM_TRIANGLE_SIDES];
		
		for (int i = 0; i < NUM_TRIANGLE_SIDES && buffer.limit() - buffer.position() >= 2; i++) {
			
			int x = buffer.get();
			int y = buffer.get();
			coordinates[i] = new Point(y, x);
		}
		
		return coordinates;
	}
	
	public static class Point {
		
		private final int x;
		private final int y;
		
		public Point(int x, int y) {
			
			this.x = x;
			this.y = y;
		}
		
		public int x() {
			
			return x;
		}
		
		public int y() {
			
			return y;
		}
	}
	
}
