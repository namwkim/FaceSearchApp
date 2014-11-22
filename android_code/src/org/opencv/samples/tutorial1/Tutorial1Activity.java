package org.opencv.samples.tutorial1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Tutorial1Activity extends Activity /*implements CvCameraViewListener2 */ {
	
	private class ImageAdapter extends BaseAdapter {
		private Context context;
		private final String[] imageNames;
	 
		public ImageAdapter(Context context, String[] imageNames) {
			this.context = context;
			this.imageNames = imageNames;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	 
			LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	 
			View gridView;
	 
			if (convertView == null) {
	 
				gridView = new View(context);
	 
				// get layout from mobile.xml
				gridView = inflater.inflate(R.layout.mobile, null);
	
			} else {
				gridView = (View) convertView;
			}
			
			// set value into textview
			TextView textView = (TextView) gridView
					.findViewById(R.id.grid_item_label);
			textView.setText(imageNames[position]);
 
			// set image based on selected text
			ImageView imageView = (ImageView) gridView
					.findViewById(R.id.grid_item_image);
 
			String imageName = imageNames[position];
 
			Bitmap inputBitmap = BitmapFactory.decodeFile(imageName);

			imageView.setImageBitmap(inputBitmap);
	
			return gridView;
		}
	 
		@Override
		public int getCount() {
			return imageNames.length;
		}
	 
		@Override
		public Object getItem(int position) {
			//return null;
			return imageNames[position];
		}
	 
		@Override
		public long getItemId(int position) {
			return position;
		}

	}
	
    private static final String TAG = "OCVSample::Activity";
    
    private TextView textViewImageName;
    private TextView textViewInfo;
    private TextView textViewImageFound;
    
    private Button btnInputImage;
    private Button btnSearch;
    private Button btnTakePhoto;
    
    private ImageView imgInputImage;
    private ImageView imgFoundImage;
    private GridView gridFoundImages;
    
    String inputImageFileName = "";
    
    int jNumFaceResults = 20;
	String[] jOutputFileNames = new String[jNumFaceResults];
	float[] jOutputScores = new float[jNumFaceResults];
	
	String[] jCategories = {"faces", "faces2"};
	
	String[] imageNames;
	
	ImageAdapter adapter;

    //jni function - prototype
    public native int searchFace(String jSourceFile, String jDBFolder, String[] jCategories, int jNumFaceResults, String[] jOutputFileNames, float[] jOutputScores);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                	//load opencv and FaceSearcher
                    Log.i(TAG, "OpenCV loaded successfully");
                 // Load native library after(!) OpenCV initialization
                    System.loadLibrary("FaceSearcher");
                    
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Tutorial1Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial1_surface_view);
        
        
        //get all controls/components
        
        textViewImageName = (TextView)findViewById(R.id.textViewImageName);
        textViewInfo = (TextView)findViewById(R.id.textViewInfo);
        textViewImageFound = (TextView)findViewById(R.id.textViewImageFound);
        
        btnInputImage = (Button)findViewById(R.id.btnInputImage);
        btnSearch = (Button)findViewById(R.id.btnSearch);
        btnTakePhoto = (Button)findViewById(R.id.btnTakePhoto);
        
        imgInputImage = (ImageView)findViewById(R.id.imgInputImage);
        imgFoundImage = (ImageView)findViewById(R.id.imgFoundImage);
        gridFoundImages = (GridView)findViewById(R.id.gridFoundImages);
        
            
        //adapter = new ImageAdapter(this, imageNames);
        //gridFoundImages.setBackgroundColor(0x9999FF);
        gridFoundImages.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				/*
				Toast.makeText(
				   getApplicationContext(),
				   ((TextView) v.findViewById(R.id.grid_item_label))
				   .getText(), Toast.LENGTH_SHORT).show();*/
				
				Toast.makeText(
						   getApplicationContext(),
						   imageNames[position] + ", score: " + String.valueOf(jOutputScores[position]), Toast.LENGTH_SHORT).show();
				
				textViewImageFound.setText(imageNames[position] + ", score: " + String.valueOf(jOutputScores[position]));
 
				Bitmap inputBitmap = BitmapFactory.decodeFile(imageNames[position]);

				imgFoundImage.setImageBitmap(inputBitmap);
			}
		});
        
        String fileName = getIntent().getStringExtra("image chosen");
        if((fileName != null) && (!fileName.equalsIgnoreCase(""))) {
        	//Display the chosen image
        	File imgFile = new  File(fileName);

        	if(imgFile.exists()){

        	    Bitmap inputBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

        	    imgInputImage.setImageBitmap(inputBitmap);
        		
        		//Drawable d = Drawable.createFromPath(fileName);
        		//imgInputImage.setImageDrawable(d);
        	    
        	    //set Text - image Name
        		
        		//Bitmap bitmap = decodeFile(imgFile);
                //bitmap = Bitmap.createScaledBitmap(bitmap,100, 100, true);
                //imgInputImage.setImageBitmap(bitmap);
        	    
        	    textViewImageName.setText(fileName);
        	    inputImageFileName = fileName;

        	}

        }
        
    }

    @Override
    public void onPause()
    {
        super.onPause();
        
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //reload OpenCV lib again
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        return true;
    }

    
    
    /** Called when the user touches the button */
    public void selectInputImage(View view) {
        // Do testSearch in response to button click
    	//new DoFaceSearch().execute("");
    	
    	Intent intent = new Intent(Tutorial1Activity.this, ExplorerActivity.class);
        startActivity(intent);
        finish();
    }
    
    /** Called when the user touches the button */
    public void takeCameraPhoto(View view) {
        // Do testSearch in response to button click
    	//new DoFaceSearch().execute("");
    	
    	Intent intent = new Intent(Tutorial1Activity.this, CamShotActivity.class);
        startActivity(intent);
        finish();
    }
    
    /** Called when the user touches the button */
    public void runSearch(View view) {
        // Do testSearch in response to button click
    	if((inputImageFileName != null) && (!inputImageFileName.equalsIgnoreCase("")))
    	{
    		String fileNameWithoutExtension = inputImageFileName.substring(0, inputImageFileName.lastIndexOf('.'));
    		
    		DoFaceSearch asyncTask = new DoFaceSearch();
    		asyncTask.delegate = this;
    		
    		asyncTask.execute(fileNameWithoutExtension);
    	}
    	else
    	{
    		new AlertDialog.Builder(this)
			.setIcon(R.drawable.icon)
			.setTitle("Please choose an input image!!!")
			.setPositiveButton("OK", null).show();	
    	}
    	
    }
    
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);              
            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE=70;
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale++;
            }

            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (Exception e) {}
        return null;
    }
    
    private class DoFaceSearch extends AsyncTask<String, Void, String> {

    	public Tutorial1Activity delegate = null;
    	
        @Override
        protected String doInBackground(String... params) {
        	//run testSearch with dbfolder and imagefileName and parameters
        	//
        	Log.i(TAG, "INPUT FILE: "+ params[0]);
        	int retVal = searchFace(params[0], "/storage/emulated/0/sample/db", jCategories, jNumFaceResults, jOutputFileNames, jOutputScores);
            //return value 0: failed; 1: successful
        	return ((retVal!=0) ? "Failed" : "Successful");
        }

        @Override
        protected void onPostExecute(String result) {
            
        	//return result after testSearch done
        	//TextView textViewInfo =(TextView)findViewById(R.id.textViewInfo); 
        	textViewInfo.setText(result);
        	
        	//enable "Run TestSearch" button
        	btnSearch.setEnabled(true); 
        	btnInputImage.setEnabled(true); 
            btnTakePhoto.setEnabled(true);
        	
        	//display gridview
            for (int i=0; i<jOutputFileNames.length; i++){
                if (jOutputFileNames[i].length() > 0 && jOutputFileNames[i].charAt(jOutputFileNames[i].length()-1)=='3') {
                	jOutputFileNames[i] = jOutputFileNames[i].substring(0, jOutputFileNames[i].length()-1);
                }            	
            }
            imageNames = jOutputFileNames;
            
            if (imageNames.length >= 0)
            {
            	textViewImageFound.setText("Found " + String.valueOf(imageNames.length) + " results");
            	imgFoundImage.setImageResource(R.drawable.icon);
            }
            
            if (delegate != null)
            	adapter = new ImageAdapter(delegate, imageNames);
     
            gridFoundImages.setAdapter(adapter);
            gridFoundImages.setEnabled(true);
            
        }

        @Override
        protected void onPreExecute() {
        	//set running before doing testSearch
        	//TextView textViewInfo =(TextView)findViewById(R.id.textViewInfo); 
        	textViewInfo.setText("Running...");
        	
        	//disable "Run TestSearch" button
        	btnSearch.setEnabled(false); 
        	btnInputImage.setEnabled(false); 
            btnTakePhoto.setEnabled(false);
            
            gridFoundImages.setEnabled(false);
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
    
}
