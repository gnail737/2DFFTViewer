package com.gnail737.imageviewerwithstupidlens;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.widget.ImageView;

public class ImageViewer extends Activity {

	static final String CAMERA_PIC_DIR = "/DCIM/browser-photos/";
	static final int FFT_SIZE = 512;
	ImageView iv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        iv = (ImageView) findViewById(R.id.mGLView);

        String imageDir =
            Environment.getExternalStorageDirectory().getAbsolutePath()
            + CAMERA_PIC_DIR;

        Intent i = new Intent(this, ListFiles.class);
        i.putExtra("directory", imageDir);
        startActivityForResult(i,0);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.image_viewer, menu);
		return true;
	}
	
	@Override
    protected void onActivityResult(int requestCode,
            int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0 && resultCode==RESULT_OK) {
            String tmp = data.getExtras().getString("clickedFile");
            Bitmap imageToChange= BitmapFactory.decodeFile(tmp);
            process_image_wFFT(imageToChange);
        }

    }

    void process_pixels(Bitmap image, int [] pixels) {
    	
        int width = image.getWidth();
        int height = image.getHeight();
        
        int x = width>>1;
        int y = height>>1;
        
        int[] pixels1 = new int[(width*height)];
        int[] pixels2 = new int[(width*height)];
        int[] pixels3 = new int[(width*height)];
        int[] pixels4 = new int[(width*height)];
        
        image.getPixels(pixels1, 0, width, 0, 0, width>>1, height>>1);
        image.getPixels(pixels2, 0, width, x, 0, width>>1, height>>1);
        image.getPixels(pixels3, 0, width, 0, y, width>>1, height>>1);
        image.getPixels(pixels4, 0, width, x, y, width>>1, height>>1);
        
        if(image.isMutable()) {
            image.setPixels(pixels4, 0, width, 0, 0, width>>1, height>>1);
            image.setPixels(pixels3, 0, width, x, 0, width>>1, height>>1);
            image.setPixels(pixels2, 0, width, 0, y, width>>1, height>>1);
            image.setPixels(pixels1, 0, width, x, y, width>>1, height>>1);
        }
        //iv.setImageBitmap(image);
    }
    
    void process_image_wFFT(Bitmap image) {
        Bitmap bm = Bitmap.createScaledBitmap(image, FFT_SIZE, FFT_SIZE, false);
        float [][] fft_real = new float[FFT_SIZE][FFT_SIZE];
        float [][] fft_imagine = new float[FFT_SIZE][FFT_SIZE];
        for (int x = 0; x < FFT_SIZE; x++) {
        	for (int y = 0; y<FFT_SIZE; y++) { //Bitmap starts from upper left corner growing downwards
        		int ARGB = bm.getPixel(x, y);
        		//averaging RGB to get grey scale
        		fft_real[y][x] = ((float)((ARGB & 0X00FF0000) >> 16) / 255.0f  + 
        				                    (float)((ARGB & 0X0000FF00) >> 8) / 255.0f +
        				                    (float)((ARGB & 0X000000FF))/255.0f) / 3.0f;
        		fft_imagine[y][x] = 0f;        		
        	}
        }
        
        //now we are ready for do fft
        //first do 1D FFT on each row 
        for (int row = 0; row < FFT_SIZE; row++) {
        	FFTUtil.FFT_Compute_1D(row, fft_real, fft_imagine, 9);
        }
        //then do it for column but first transpose so previous row major become coloum major 
        //this step would worse performance on CPU but essential for good performance on GPU
        FFTUtil.transposeMatrix(fft_real, true);
        FFTUtil.transposeMatrix(fft_imagine, true);
        
        for (int row = 0; row < FFT_SIZE; row++) {
        	FFTUtil.FFT_Compute_1D(row, fft_real, fft_imagine, 9);
        }
        //transpose our matrix back to row major
        FFTUtil.transposeMatrix(fft_real, false);
        FFTUtil.transposeMatrix(fft_imagine, false);
        //convert to amplitude
        float [][] ampMat = FFTUtil.compute_amplitude(fft_real, fft_imagine);
        //convert to decibel
        //float [] dbMat = FFTUtil.compute_decibels()
        //convert to ARGB packed int
        int [] pixelResult = FFTUtil.compute_pixels(ampMat, FFTUtil.maxAPP);
        bm.setPixels(pixelResult, 0, FFT_SIZE, 0, 0, FFT_SIZE, FFT_SIZE);
        //now divide pixelResult to four quadrants and shift DC to center of pixels
        process_pixels(bm, pixelResult);
        
        iv.setImageBitmap(bm);
    }

}
