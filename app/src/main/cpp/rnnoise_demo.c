//
// Created by Betul on 4/26/2019.
//


#include <stdlib.h>
#include <assert.h>
#include <jni.h>
#include <string.h>
#include <pthread.h>
#include <sys/types.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>// for log message
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include <math.h>
#include <stdio.h>
#include <zconf.h>
#include "rnnoise.h"
#define LOG_TAG "TESTTTTTTT"
#define FRAME_SIZE 480




JNIEXPORT jdouble JNICALL
Java_com_zen_noisyai_activity_MainActivity_rnnoise_1demo(JNIEnv *env, jclass type,jstring inputFName, jstring outputFName) {
    ///Bizim Algoritma Karesi => "Exit Point => NOISY : 4059.437211 ||| CLEAR : 3909.596673 ||| PERCENTAGE : 96.308835"
    ///Bizim Algoritma        => "Exit Point => NOISY : 4501.080858 ||| CLEAR : 4060.160638 ||| PERCENTAGE : 90.204126"
    ///Noise alan rms değeri =>  "Exit Point => NOISY : 17737.315180 ||| CLEAR : 0.000000 ||| PERCENTAGE : 0.000000"
    ///Clear alan rms değeri =>  "Exit Point => NOISY : 17690.985579 ||| CLEAR : 0.000000 ||| PERCENTAGE : 99.738801"
    ///Clear new  rms değeri =>  "Exit Point => NOISY : 15779.416292 ||| CLEAR : 0.000000 ||| PERCENTAGE : 89.194670"

    ///Audacity Algoritması   => "Exit Point => NOISY : 17478.195631 ||| CLEAR : 12282.904722 ||| PERCENTAGE : 70.275588"

    const char *inputFile = (*env)->GetStringUTFChars(env, inputFName, NULL);
    const char *outputFile = (*env)->GetStringUTFChars(env, outputFName, NULL);
    assert(NULL != inputFile);

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", inputFile);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", outputFile);


    double SUM_NOISY_SOUND = 0;
    double SUM_CLEAR_SOUND = 0;
    double COUNTER = 0;


    int i;
    int first = 1;
    float x[FRAME_SIZE];
    FILE *f1, *fout;
    DenoiseState *st;
    st = rnnoise_create();
    f1 = fopen(inputFile, "r");
    fout = fopen(outputFile, "w");

    while (1) {
        short tmp[FRAME_SIZE];

        if (!first){
            fread(tmp, sizeof(short), FRAME_SIZE, f1);

            for (i = 0; i < FRAME_SIZE; i++){
                x[i] = tmp[i];
                SUM_NOISY_SOUND += x[i]*x[i];
                //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Tmp[%d] : %d", i, tmp[i]);
            }
            rnnoise_process_frame(st, x, x);
            for (i = 0; i < FRAME_SIZE; i++){
                //SUM_CLEAR_SOUND += (tmp[i]-x[i]) * (tmp[i]-x[i]);
                SUM_CLEAR_SOUND += x[i]*x[i];
                COUNTER++;

                tmp[i] = x[i];
            }

            fwrite(tmp, sizeof(short), FRAME_SIZE, fout);
        }
        else{
            fread(tmp, sizeof(short), 22, f1);
            fwrite(tmp, sizeof(short), 22, fout);

            /*
            putc(0x52, fout);putc(0x49, fout);putc(0x46, fout);putc(0x46, fout);putc(0x2c, fout);putc(0xd9, fout);putc(0x5b, fout);putc(0x00, fout);putc(0x57, fout);putc(0x41, fout);putc(0x56, fout);putc(0x45, fout);
            putc(0x66, fout);putc(0x6d, fout);putc(0x74, fout);putc(0x20, fout);putc(0x10, fout);putc(0x00, fout);putc(0x00, fout);putc(0x00, fout);putc(0x01, fout);putc(0x00, fout);putc(0x02, fout);putc(0x00, fout);
            putc(0x380, fout);putc(0xbb, fout);putc(0x00, fout);putc(0x00, fout);putc(0x00, fout);putc(0xee, fout);putc(0x02, fout);putc(0x00, fout);putc(0x04, fout);putc(0x00, fout);putc(0x10, fout);putc(0x00, fout);
            putc(0x64, fout);putc(0x61, fout);putc(0x74, fout);putc(0x61, fout);putc(0x08, fout);putc(0xd9, fout);putc(0x5b, fout);putc(0x00, fout);*/
        }

        if (feof(f1)) break;
        first = 0;
    }
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "End While");
    rnnoise_destroy(st);
    fclose(f1);
    fclose(fout);


    SUM_NOISY_SOUND = sqrt(SUM_NOISY_SOUND/COUNTER);
    SUM_CLEAR_SOUND = sqrt(SUM_CLEAR_SOUND/COUNTER);
    double PERCENTAGE = (SUM_CLEAR_SOUND/SUM_NOISY_SOUND)*100;

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Exit Point => NOISY : %lf ||| CLEAR : %lf ||| PERCENTAGE : %lf", SUM_NOISY_SOUND, SUM_CLEAR_SOUND, PERCENTAGE);

    return PERCENTAGE;

}


JNIEXPORT jdouble JNICALL
Java_com_zen_noisyai_activity_ListActivity_rnnoise_1demo(JNIEnv *env, jclass type,jstring inputFName, jstring rootFName, jstring outputFName) {

    const char *inputFile = (*env)->GetStringUTFChars(env, inputFName, NULL);
    const char *rootFile = (*env)->GetStringUTFChars(env, rootFName, NULL);
    const char *outputFile = (*env)->GetStringUTFChars(env, outputFName, NULL);
    assert(NULL != inputFile);

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", inputFile);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", rootFile);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", outputFile);


    double SUM_NOISY_SOUND = 0;
    double SUM_CLEAR_SOUND = 0;
    double COUNTER = 0;


    int i;
    int first = 1;
    float x[FRAME_SIZE];
    float y[FRAME_SIZE];
    FILE *f1, *froot, *fout;
    DenoiseState *st;
    st = rnnoise_create();
    f1 = fopen(inputFile, "r");
    froot = fopen(rootFile, "r");
    fout = fopen(outputFile, "w");

    while (1) {
        short tmp[FRAME_SIZE];
        short ROOT_MINUS_CLEAN[FRAME_SIZE];

        if (!first){
            fread(tmp, sizeof(short), FRAME_SIZE, f1);
            fread(ROOT_MINUS_CLEAN, sizeof(short), FRAME_SIZE, froot);

            for (i = 0; i < FRAME_SIZE; i++){
                x[i] = tmp[i];
                y[i] =  ROOT_MINUS_CLEAN[i];
                //SUM_NOISY_SOUND += tmp[i]*tmp[i];
                SUM_NOISY_SOUND += y[i]*y[i];
                //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Tmp[%d] : %d", i, tmp[i]);
            }
            rnnoise_process_frame(st, x, x);
            for (i = 0; i < FRAME_SIZE; i++){
                //SUM_CLEAR_SOUND += (ROOT_MINUS_CLEAN[i]-x[i]) * (ROOT_MINUS_CLEAN[i]-x[i]);
                SUM_CLEAR_SOUND += x[i]*x[i];
                COUNTER++;
                tmp[i] = x[i];
            }

            fwrite(tmp, sizeof(short), FRAME_SIZE, fout);
        }
        else{
            fread(tmp, sizeof(short), 22, f1);
            fread(ROOT_MINUS_CLEAN, sizeof(short), 22, froot);
            fwrite(tmp, sizeof(short), 22, fout);


            /*
            putc(0x52, fout);putc(0x49, fout);putc(0x46, fout);putc(0x46, fout);putc(0x2c, fout);putc(0xd9, fout);putc(0x5b, fout);putc(0x00, fout);putc(0x57, fout);putc(0x41, fout);putc(0x56, fout);putc(0x45, fout);
            putc(0x66, fout);putc(0x6d, fout);putc(0x74, fout);putc(0x20, fout);putc(0x10, fout);putc(0x00, fout);putc(0x00, fout);putc(0x00, fout);putc(0x01, fout);putc(0x00, fout);putc(0x02, fout);putc(0x00, fout);
            putc(0x380, fout);putc(0xbb, fout);putc(0x00, fout);putc(0x00, fout);putc(0x00, fout);putc(0xee, fout);putc(0x02, fout);putc(0x00, fout);putc(0x04, fout);putc(0x00, fout);putc(0x10, fout);putc(0x00, fout);
            putc(0x64, fout);putc(0x61, fout);putc(0x74, fout);putc(0x61, fout);putc(0x08, fout);putc(0xd9, fout);putc(0x5b, fout);putc(0x00, fout);*/
        }

        if (feof(f1)) break;
        first = 0;
    }
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "End While");
    rnnoise_destroy(st);
    fclose(f1);
    fclose(fout);


    SUM_NOISY_SOUND = sqrt(SUM_NOISY_SOUND/COUNTER);
    SUM_CLEAR_SOUND = sqrt(SUM_CLEAR_SOUND/COUNTER);
    double PERCENTAGE = (SUM_CLEAR_SOUND/SUM_NOISY_SOUND)*100;

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Exit Point => NOISY : %lf ||| CLEAR : %lf ||| PERCENTAGE : %lf", SUM_NOISY_SOUND, SUM_CLEAR_SOUND, PERCENTAGE);

    return PERCENTAGE;

}



JNIEXPORT jboolean JNICALL
Java_com_zen_noisyai_activity_MainActivity_combine(JNIEnv *env, jclass type, jstring mOne_, jstring mTwo_) {

    const char *mOne = (*env)->GetStringUTFChars(env, mOne_, 0);
    const char *mTwo = (*env)->GetStringUTFChars(env, mTwo_, 0);

    char mOneExt[] = "input";
    char mTwoExt[] = "street";
    char *mOneFile = malloc(sizeof(char) * 1024);
    char *mTwoFile = malloc(sizeof(char) * 1024);
    char *OutputFile = malloc(sizeof(char) * 1024);
    assert(NULL != mOne); assert(NULL != mTwo);

    memcpy(mOneFile, mOne, strlen(mOne)+1);
    strcat(mOneFile, mOneExt);
    strcat(mOneFile, ".wav");

    memcpy(mTwoFile, mTwo, strlen(mTwo)+1);
    strcat(mTwoFile, mTwoExt);
    strcat(mTwoFile, ".wav");

    memcpy(OutputFile, mOne, strlen(mOne)+1);strcat(OutputFile, mOneExt);
    strcat(OutputFile, "_");strcat(OutputFile, mTwoExt);strcat(OutputFile, ".wav");


    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", mOneFile);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", mTwoFile);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", OutputFile);

    short temp[FRAME_SIZE];

    int i;
    int first = 1;
    float x[FRAME_SIZE];
    FILE *f1, *f2, *fout;
    f1 = fopen(mOneFile, "r");fread(temp, sizeof(short), 22, f1);
    f2 = fopen(mTwoFile, "r");fread(temp, sizeof(short), 22, f2);
    fout = fopen(OutputFile, "w");

    while (1) {
        short mOneTemp[FRAME_SIZE];
        short mTwoTemp[FRAME_SIZE];

        fread(mOneTemp, sizeof(short), FRAME_SIZE, f1);
        fread(mTwoTemp, sizeof(short), FRAME_SIZE, f1);

        if (feof(f1)) break;
        if (feof(f2)){
            fclose(f2);
            f2 = fopen(mTwoFile, "r");
            fread(mTwoTemp, sizeof(short), FRAME_SIZE, f1);
        }
        for (i = 0; i < FRAME_SIZE; i++){
            x[i] = mOneTemp[i] + mTwoTemp[i];
        }

        if (!first){
            fwrite(x, sizeof(short), FRAME_SIZE, fout);
        }
        else{
            putc(0x52, fout);putc(0x49, fout);putc(0x46, fout);putc(0x46, fout);putc(0x2c, fout);putc(0xd9, fout);putc(0x5b, fout);putc(0x00, fout);putc(0x57, fout);putc(0x41, fout);putc(0x56, fout);putc(0x45, fout);
            putc(0x66, fout);putc(0x6d, fout);putc(0x74, fout);putc(0x20, fout);putc(0x10, fout);putc(0x00, fout);putc(0x00, fout);putc(0x00, fout);putc(0x01, fout);putc(0x00, fout);putc(0x02, fout);putc(0x00, fout);
            putc(0x38, fout);putc(0xbb, fout);putc(0x00, fout);putc(0x00, fout);putc(0x00, fout);putc(0xee, fout);putc(0x02, fout);putc(0x00, fout);putc(0x04, fout);putc(0x00, fout);putc(0x10, fout);putc(0x00, fout);
            putc(0x64, fout);putc(0x61, fout);putc(0x74, fout);putc(0x61, fout);putc(0x08, fout);putc(0xd9, fout);putc(0x5b, fout);putc(0x00, fout);
        }
        first = 0;
    }
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "End While");
    fclose(f1);
    fclose(f2);
    fclose(fout);


    (*env)->ReleaseStringUTFChars(env, mOne_, mOne);
    (*env)->ReleaseStringUTFChars(env, mTwo_, mTwo);
}





 /*


    WAV Header
    char *a[] = {"5249", "4646", "2cd9", "5b00", "5741", "5645", "666d", "7420",
    "1000", "0000", "0100", "0200", "80bb", "0000", "00ee", "0200",
    "0400", "1000", "6461", "7461", "08d9", "5b00"};


 const char *filename = (*env)->GetStringUTFChars(env, inputFName, NULL);
    char inputFileExt[] = "6-clear_area_new_output.pcm";
    char outputFileExt[] = "new_output.pcm";
    char *inputFile = malloc(sizeof(char) * 1024);
    char *outputFile = malloc(sizeof(char) * 1024);
    assert(NULL != filename);

    memcpy(inputFile, filename, strlen(filename)+1);
    strcat(inputFile, inputFileExt);

    memcpy(outputFile, filename, strlen(filename)+1);
    strcat(outputFile, outputFileExt);

    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", inputFile);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Entry Point %s", outputFile);


    double SUM_NOISY_SOUND = 0;
    double SUM_CLEAR_SOUND = 0;
    double COUNTER = 0;


    int i;
    int first = 1;
    float x[FRAME_SIZE];
    FILE *f1, *fout;
    DenoiseState *st;
    st = rnnoise_create();
    f1 = fopen(inputFile, "r");
    fout = fopen(outputFile, "w");

    while (1) {
        short tmp[FRAME_SIZE];
        //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "1.While");
        fread(tmp, sizeof(short), FRAME_SIZE, f1);
        //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "2.While");
        if (feof(f1)) break;
        for (i = 0; i < FRAME_SIZE; i++){
            x[i] = tmp[i];
            SUM_NOISY_SOUND += tmp[i]*tmp[i];
            COUNTER++;
            __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Tmp[%d] : %d", i, tmp[i]);
        }

        //Commment starts here
        rnnoise_process_frame(st, x, x);
        for (i = 0; i < FRAME_SIZE; i++){
            //SUM_CLEAR_SOUND += (tmp[i]-x[i]) * (tmp[i]-x[i]);
            SUM_CLEAR_SOUND += x[i]*x[i];
            tmp[i] = x[i];
        }
        if (!first) fwrite(tmp, sizeof(short), FRAME_SIZE, fout);
        first = 0;
        //Commment ends here
}
__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "End While");
rnnoise_destroy(st);
fclose(f1);
fclose(fout);


SUM_NOISY_SOUND = sqrt(SUM_NOISY_SOUND/COUNTER);
SUM_CLEAR_SOUND = sqrt(SUM_CLEAR_SOUND/COUNTER);
double PERCENTAGE = (SUM_CLEAR_SOUND/SUM_NOISY_SOUND)*100;

__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Exit Point => NOISY : %lf ||| CLEAR : %lf ||| PERCENTAGE : %lf", SUM_NOISY_SOUND, SUM_CLEAR_SOUND, PERCENTAGE);


*/








