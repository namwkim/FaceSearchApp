/*
 * face-recognition.c
 *
 *  Created on: Jul 16, 2014
 *      Author: namwkim85
 */

#include "opencv/cv.h"
#include "opencv/highgui.h"
#include "math.h"
#include "float.h"
#include "face-recognition.h"
#include "helper.h"
#include <stdio.h>

#define UCHAR unsigned char
#define CV_GET(img,y,x) CV_IMAGE_ELEM((img), UCHAR, (y), (x))
#define CV_GET_B(img,y,x) CV_IMAGE_ELEM((img), UCHAR, (y), (x) * 3 + 0)
#define CV_GET_G(img,y,x) CV_IMAGE_ELEM((img), UCHAR, (y), (x) * 3 + 1)
#define CV_GET_R(img,y,x) CV_IMAGE_ELEM((img), UCHAR, (y), (x) * 3 + 2)


IplImage* CalcLBP(IplImage* src, int radius, int neighbors, int uniform_on){
	if (src->nChannels!=1 || src->depth!=IPL_DEPTH_8U){
		return NULL;
	}
	int i,j,n;
	//allocate memory for the result
	CvSize size = cvGetSize(src);
	IplImage* dst = cvCreateImage(cvSize(size.width-2*radius, size.height-2*radius), IPL_DEPTH_8U, 1);

	//initialize to zeros
	cvSetZero(dst);

	for (n=0; n<neighbors; n++){
		//sample points
		float x = (float) (radius*cos(2.0*CV_PI*n/(float)neighbors));
		float y = (float) (-radius*sin(2.0*CV_PI*n/(float)neighbors));

		//relative indices
		int fx = floor(x);
		int fy = floor(y);
		int cx = ceil(x);
		int cy = ceil(y);

		//fractional part
		float ty = y-fy;
		float tx = x-fx;

		//set interpolation weights
        float w1 = (1 - tx) * (1 - ty);
        float w2 =      tx  * (1 - ty);
        float w3 = (1 - tx) *      ty;
        float w4 =      tx  *      ty;

        // iterate through your data
        for (i=radius; i<(src->height-radius); i++){
        	for (j=radius; j<(src->width-radius); j++){
        		UCHAR V1 = CV_GET(src, i+fy, j+fx);
        		UCHAR V2 = CV_GET(src, i+fy, j+cx);
        		UCHAR V3 = CV_GET(src, i+cy, j+fx);
        		UCHAR V4 = CV_GET(src, i+cy, j+cx);

        		UCHAR newV = w1*V1 + w2*V2 + w3*V3 + w4*V4;

        		int pos = (i-radius)*dst->widthStep + (j-radius);
        		dst->imageData[pos] += (newV>src->imageData[i*src->widthStep + j])<<n;
        	}
        }
	}

	// convert to uniform patterns
	if (uniform_on){
		for (i=radius; i<(src->height-radius); i++){
			for (j=radius; j<(src->width-radius); j++){
				int pos = (i-radius)*dst->widthStep + (j-radius);
				dst->imageData[pos] = ConvertToUniform(dst->imageData[pos], neighbors);
				//printf("val : %d\n", dst->imageData[pos]);
			}
		}
	}
	return dst;
}
int ConvertToUniform(int num, int P){

	int total = 0, uniform = 0;
	int prev = (num&(1<<(P-1)))>>(P-1);
	int curr = num&1;
	total += abs(curr-prev);
	prev = curr;
	uniform = curr;
	for (int i=1; i<P; i++){
		curr = (num & (1<<i))>>i;
		total += abs(curr-prev);
		prev = curr;
		uniform += curr;
	}
	return total<=2? uniform : P+1;
}
CvHistogram* CalcHistogram(IplImage* src, int minVal, int maxVal, int normed){
	// Establish the number of bins.
	int histSize = maxVal-minVal+1;

	float range[] = { minVal, maxVal+1 };
	float* histRange = { range };

	CvHistogram* result = cvCreateHist(1, &histSize, CV_HIST_ARRAY, &histRange, 1);
	cvCalcHist(&src, result, 0, NULL);

	if (normed){
		cvNormalizeHist(result, 1.0);
	}
	return result;
}
//hists and child histograms have to be freed later on
CvMat* CalcSpatialHistogram(IplImage* src, int numPatterns, int grid_x, int grid_y){
	if (src==NULL) return NULL;
	// calculate LBP patch size
	int width = src->width/grid_x;
	int height = src->height/grid_y;

	// allocate memory for the spatial histogram
	CvMat* matrix = cvCreateMat(grid_x*grid_y,numPatterns, CV_32FC1);
	cvSetZero(matrix);
	//CvHistogram** hists = (CvHistogram**)malloc(grid_x*grid_y*sizeof(CvHistogram*));

	int histIdx = 0;

	for (int i=0; i<grid_y; i++){
		for (int j=0; j<grid_x; j++){
			IplImage* sub_img = CreateSubImg(src, cvRect(i*width,j*height,width,height));
			if (sub_img==NULL){
				fprintf(stderr, "Failed to Create a sub image file\n");
			}
			CvHistogram* cell_hist = CalcHistogram(sub_img, 0, (numPatterns-1), 1);
			CvMatND* histmat = (CvMatND*)(cell_hist->bins);
			if (histmat->dims!=1){//has to be 1 dim
				fprintf(stderr, "histmat->dims has to be 1!\n");
				return NULL;
			}
			if (histmat->dim[0].size != numPatterns){//has to be equal
				fprintf(stderr, "histmat->dim[0].size has to be equal to numPatterns!\n");
				return NULL;
			}
			//integrate this regional histogram into the full histogram
			for (int k=0; k<histmat->dim[0].size; k++){
				matrix->data.fl[histIdx*matrix->cols + k] = histmat->data.fl[k];
			}

			//hists[histIdx] = cell_hist;
			histIdx++;

			//release sub image and histogram
			cvReleaseImage(&sub_img);
		}
	}
	return matrix;
}

float CompareHistograms(CvMat* hist1, CvMat* hist2, const float* weight){
	if (hist1->rows!=hist2->rows || hist1->cols!=hist2->cols){
		return FLT_MAX;
	}
	float totalDist = 0.0f;
	for (int i=0; i<hist1->rows; i++){
		float subTotal = 0.0f;
		for (int j=0; j<hist1->cols; j++){
			int pos = i*hist1->cols + j;
			float a = hist1->data.fl[pos];
			float b = hist2->data.fl[pos];
			float c = a-b;
			if (fabs(a+b) > FLT_EPSILON)
				subTotal += (1.0f/2.0f)*((c*c)/(a+b));
		}
		totalDist += (weight==NULL? subTotal : subTotal*weight[i]);
	}
	return totalDist;
}

