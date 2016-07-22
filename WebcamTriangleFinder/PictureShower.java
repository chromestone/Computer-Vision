import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

/**
 * Wraps the CanvasFrame in an another thread for greater flexibility.
 * Especially useful for threads that depend on "interrupt" to function properly,
 * but need to update the CanvasFrame
 * 
 * @author derekzhang
 *
 */
public class PictureShower extends Thread {

	private CanvasFrame canvas;
	private OpenCVFrameConverter.ToMat frameConverter;
	private final Object lock;
	private Mat mat;
	
	//private final Object waitLock;
	
	public PictureShower(int width, int height) {
		
		canvas = new CanvasFrame("Webcam", 1);
		canvas.setCanvasSize(width, height);
		canvas.setDefaultCloseOperation(CanvasFrame.DO_NOTHING_ON_CLOSE);
		frameConverter = new OpenCVFrameConverter.ToMat();
		lock = new Object();
		mat = null;
		
		//this.waitLock = waitLock;
	}
	
	public void update(Mat mat) {
		
		synchronized (lock) {
			
			this.mat = mat;
			
			lock.notify();
		}
	}
	
	@Override
	public void run() {
		
		try {
			
			while (!isInterrupted()) {
				
//				synchronized (waitLock) {
//					
//					waitLock.wait();
//				}
				
				
				synchronized (lock) {
					
					lock.wait();
					
					if (mat != null) {

						canvas.showImage(frameConverter.convert(mat));
						mat = null;
					}
				}
			}
		}
		catch (InterruptedException e) {
			
			System.out.println("Exiting GUI...");
		}
		
		Thread.interrupted();
		canvas.dispose();
	}
	
}
