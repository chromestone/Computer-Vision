import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.*;

public class Tutorial {

	public static void main(String[] args) {
		
		//writes the output of the method
		opencv_imgcodecs.imwrite("t1.jpg", detectTriangleAndDrawPoints(opencv_imgcodecs.imread("triangle.jpg")));
	}
	
	public static Mat detectTriangleAndDrawPoints(Mat src) {
		
		//the image that is returned
		Mat output = src.clone();
		//IplImage out = new IplImage(output);//uncomment this to access specific points
		
		//creates empty image to hold the gray scaled image
		Mat gray = new Mat(src.size());
		opencv_imgproc.cvtColor(src, gray, opencv_imgproc.CV_BGR2GRAY);
		
		//creates empty image to hold the thresholded image
		Mat thresh = new Mat(gray.size());
		opencv_imgproc.threshold(gray, thresh, 127, 255, opencv_imgproc.THRESH_BINARY_INV);
		
		//stores the contours (outline of the shape)
		MatVector contours = new MatVector();
		opencv_imgproc.findContours(thresh.clone(), contours, opencv_imgproc.CV_RETR_EXTERNAL, opencv_imgproc.CV_CHAIN_APPROX_SIMPLE);
		
		//loops through all possible outline candidates
		for (long i = 0; i < contours.size(); i++) {
			
			//stores points of the polygon
			Mat approx = new Mat();
			opencv_imgproc.approxPolyDP(contours.get(i), approx, opencv_imgproc.arcLength(contours.get(i), true) * .01, true);
			
			//checks if polygon is valid
			if (Math.abs(opencv_imgproc.contourArea(contours.get(i))) < 1000 || !opencv_imgproc.isContourConvex(approx))//15426
				continue;
			
			//triangle
			if (approx.rows() == 3 && approx.cols() == 1) {
				
				//draws contour
				opencv_imgproc.drawContours(output, contours, (int) i, Scalar.BLUE);
				
				//makes image compatible for "point getting"
				IplImage approxImg = new IplImage(approx);
					
				for (int r = 0; r < approx.rows(); r++) {

					//contains the points of the polygon
					CvScalar pointScalar = opencv_core.cvGet2D(approxImg, r, 0);
					//draws point
					opencv_imgproc.circle(output, new Point((int) pointScalar.get(0), (int) pointScalar.get(1)), 5, Scalar.RED);

					//Change specific pixels of (output) image
					//uncomment IplImage out (top of the method)
					//due to both images being the "same" pointer, modifying the IplImage, also modifies the Mat
					/*
					int x = (int) pointScalar.get(0);
					int y = (int) pointScalar.get(1);
					CvScalar pixelScalar = opencv_core.cvGet2D(out, y, x);//reversed because that's how arrays work
					int red = 0, g = 255, b = 0;
					//BGR
					pixelScalar.setVal(0, b);
					pixelScalar.setVal(1, g);
					pixelScalar.setVal(2, red);
					opencv_core.cvSet2D(out, y, x, pixelScalar);
					*/
				}
			}
		}
		
		return output;
	}
}
