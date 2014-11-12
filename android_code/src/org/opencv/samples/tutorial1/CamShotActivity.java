package org.opencv.samples.tutorial1;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class CamShotActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;
    
    private boolean              isCaptured = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CamShotActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_cam_shot);

        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("Toggle Native/Java camera");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String toastMesage = new String();
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemSwitchCamera) {
            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
            mIsJavaCamera = !mIsJavaCamera;

            if (mIsJavaCamera) {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
                toastMesage = "Java Camera";
            } else {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);
                toastMesage = "Native Camera";
            }

            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableView();
            Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
            toast.show();
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	
    	if (isCaptured)
    	{
    		isCaptured = false;
    		
    		String fileSaved = saveFrame2Storage(inputFrame.rgba());
    		if (fileSaved != null)
    		{
    			//Toast.makeText(this, fileSaved + " saved", Toast.LENGTH_SHORT).show();
    			
    			//mOpenCvCameraView.disableView();
    			
    			//back to the main activity
    			Intent i = new Intent(CamShotActivity.this, Tutorial1Activity.class);
    			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    			
    			i.putExtra("image chosen", fileSaved);
    			startActivity(i);
    			finish();
    		}
    		//else
    			//Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
    		
    		
    	}
    	
        return inputFrame.rgba();
    }
    
    /** Called when the user touches the button */
    public void captureFrame(View view) {
    	Log.i(TAG,"captureFrame event");
        //set flag to tell the capturer for a shot
    	isCaptured = true;
    }
    
    String saveFrame2Storage (Mat mat) {
    	Mat mIntermediateMat = new Mat();
 	   	Imgproc.cvtColor(mat, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

 	   
 	   
 	   	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
 	   	String currentDateandTime = sdf.format(new Date());
 	   	String fileName = Environment.getExternalStorageDirectory().getPath() +
                             "/sample_picture_" + currentDateandTime + ".png";
 	   
 	           
 	   	Boolean bool = Highgui.imwrite(fileName, mIntermediateMat);

 	   	if (bool)
 	   	{
 	   		Log.i(TAG, "SUCCESS writing image to external storage");
 	   		//Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
 	   		return fileName;
 	   	}
 	   	else
 	   	{
 	   		Log.i(TAG, "Fail writing image to external storage");
 	   		//Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
 	   		return null;
 	   	}
    }
}
