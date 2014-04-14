package com.gnail737.imageviewerwithstupidlens;

import java.util.Arrays;

public class FFTUtil {
    public static float maxAPP;
    private static float [][] tempMat;
    static final int FFT_SIZE = 512;
    static {
    	tempMat = new float[FFT_SIZE][FFT_SIZE];
    }
	//given a M (M<32) bits number(0 - 2^M) revert bits in place
	private static int reverseInPlace(int input, int m) 
	{
		int out = 0, scrape = 0, k;
		for (k=m; k>=1; k--) {
			//right shift till see the last bit
			scrape =(input)>>(k-1) & 0x00000001;
			//out is assembled bit by bit from last to first
		    out += ((scrape == 0) ?  0 : (1<<(m-k)));
		}

		return out;
	}
	
	//FFT_Compute_1D computes 1D FFT along rowNumth row of 2D Matrix, assume Matrix stored in Row-Major, from top to bottom
	public static void FFT_Compute_1D(int rowNum, float [][] realMat, float [][] imagineMat, int m) 
	{
		//length of our array is 2^m
		int len = 0x00000001 << (m);
		int k=0, j=0, temp, magicMask;
		//this scrambleIndex array serves two purposes, first it stores initial scrambled index,
		//second after computation starts it stores intermediate weight for each node during stage loop
		int [] scrambleIndex = new int[len];
		//temp variable used for storing intermediate results
		float [] realTemp = new float[len];
		float [] imgTemp = new float[len];

		if (m >= 32) {
			System.out.format("We don't support bit length greater than 31");
			return;
		}
		
		for (k=0; k<len; k++) {
			temp = reverseInPlace(k, m); 
			scrambleIndex[k] =  temp;
		}
		//compute output for each stage, intermediate result stored in place
		//j is the stage number, k is the index to sample location
		
		//stage zero is a bit special cannot be written in general loop
		//the following loop just for stage 0
		for (k=0; k<len; k++) {
			realTemp[k] = realMat[rowNum][scrambleIndex[k]];
			imgTemp[k] = imagineMat[rowNum][scrambleIndex[k]];
		}
		//use memcpy to speed up
		//realMat[rowNum] = Arrays.copyOf(realTemp, len);
		//imagineMat[rowNum] = Arrays.copyOf(imgTemp, len);
		System.arraycopy(realTemp, 0, realMat[rowNum], 0, len);
		System.arraycopy(imgTemp, 0, imagineMat[rowNum], 0, len);
		//now start from stage 1 til finish
		for (j=1; j<=m; j++) {
			//first sum then apply weight
			magicMask = 0x00000001<<(j-1);
			//compute each sample location in freq domain
			for (k=0; k<len; k++) {
				float sign = ((int)1 - (int)((k & magicMask) / magicMask) * (int)2);
				//adding counter part of sum
				realTemp[k] = sign * realMat[rowNum][k] + realMat[rowNum][k^magicMask];
				imgTemp[k] = sign * imagineMat[rowNum][k] + imagineMat[rowNum][k^magicMask];

				if (((k&(magicMask<<1))) != 0) {
					//apply weight by left shift then right shift, we can zero out j leftmost bits
					multTwiddleFactor(k, realTemp, imgTemp, (k&((magicMask<<1)-1)), (magicMask<<2)); 
				}
			}
			//copy result over finish this stage
			//realMat[rowNum] = Arrays.copyOf(realTemp, len);
			//imagineMat[rowNum] = Arrays.copyOf(imgTemp, len);
			System.arraycopy(realTemp, 0, realMat[rowNum], 0, len);
			System.arraycopy(imgTemp, 0, imagineMat[rowNum], 0, len);
		}
	}
	//given row major square matrix a transpose it to column major format optionally scale our matrix
	public static void transposeMatrix(float [] [] origMat, boolean scaleOrNo) {
		//float [][] outMat = new float[origMat.length][origMat.length]; 
		for (int i=0; i< origMat.length; i++) {
			System.arraycopy(origMat[i], 0, tempMat[i], 0, origMat.length);
		}
		for (int row=0; row<origMat.length; row++) {
			for (int col=0; col<origMat.length; col++) {
				if (scaleOrNo) {
					origMat[col][row] = tempMat[row][col] / (float)origMat.length;
				} else {
					origMat[col][row] = tempMat[row][col];
				}
			}
		}
		return;
		
	}

	private static void multTwiddleFactor(int index, float [] real, float [] img, int pos, int sum) {
		
		float cosArg = (float) Math.cos(-2.0f * Math.PI * pos / (float)sum);
		float sinArg = (float) Math.sin(-2.0f * Math.PI * pos / (float)sum);
		
		float r = real[index]*cosArg - img[index]*sinArg;
		float i = real[index]*sinArg + img[index]*cosArg;
		
		real[index] = r;
		img[index] = i;
	}
    
	public static float[][] compute_amplitude(float [][] real, float [][] imaginary) {
		float [][] amp = new float[real.length][real.length]; 
		float maxAmp = 0;
		for (int row=0; row<amp.length; row++) {
			for (int col=0; col<amp.length; col++) {
				amp[row][col] = (float)Math.hypot(real[row][col], imaginary[row][col]);
				if (amp[row][col] > maxAmp) {
					maxAmp = amp[row][col];
				}
			}
		}	
		maxAPP = maxAmp;
		return amp;
	}

	public static int[] compute_pixels(float[][] ampMat, float maxVal) {
		int [] ARGB = new int[ampMat.length * ampMat.length];
		
		for (int x=0; x<ampMat.length; x++){
			for (int y=0; y<ampMat.length; y++) {
				int Red = (int)(ampMat[y][x] * 256.0f);
				Red &= 0x00ffffff;
				if (Red > 255)  Red = 255;
				//int Red = 0x000000ff;
				ARGB[y*ampMat.length + x] = (int)((int)0xff000000 | (Red<<16) | (Red<<8) | Red);
				//ARGB[y*ampMat.length + x] = 0xffffffff;
			}
		}
		return ARGB;
	}

}
