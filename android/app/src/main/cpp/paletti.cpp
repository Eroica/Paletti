#include <iostream>
#include <fstream>
#include <string>

#include <android/log.h>
#include <jni.h>

#include "allheaders.h"

#define  LOG_TAG    "Paletti"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define APPNAME "Paletti"

extern "C" JNIEXPORT int JNICALL
Java_app_paletti_lib_Leptonica_posterize(
	JNIEnv * env,
	jobject /* this */,
	jint max_count,
	jboolean is_black_white,
	jobjectArray file_paths) {
	std::string native_paths[3];
	int len = env->GetArrayLength(file_paths);
	for (int i = 0; i < len; i++) {
		jstring jstr = (jstring)(env->GetObjectArrayElement(file_paths, i));
		const jsize strLen = env->GetStringUTFLength(jstr);
		const char* charBuffer = env->GetStringUTFChars(jstr, (jboolean*)0);
		std::string str(charBuffer, strLen);
		native_paths[i] = str;
		env->ReleaseStringUTFChars(jstr, charBuffer);
		env->DeleteLocalRef(jstr);
	}

	PIX* pixs = pixRead(native_paths[0].c_str());
	if (pixs == nullptr) {
		return -1;
	}

	PIX* pixc;
	if (is_black_white) {
		PIX* tmp = pixConvertRGBToLuminance(pixs);
		PIX* tmp2 = pixConvert8To32(tmp);
		pixc = pixMedianCutQuantGeneral(tmp2, 0, 8, (int)max_count, 0, 0, 0);
		pixDestroy(&tmp2);
		pixDestroy(&tmp);
	}
	else {
		pixc = pixMedianCutQuantGeneral(pixs, 0, 8, (int)max_count, 0, 0, 0);
	}
	if (pixc == nullptr) {
		pixDestroy(&pixs);
		return -1;
	}

	PIXCMAP* colormap = pixGetColormap(pixc);
	int count = pixcmapGetCount(colormap);

	std::ofstream colors;
	colors.open(native_paths[2].c_str());
	for (int i = 0; i < count; i++) {
		int r, g, b;
		pixcmapGetColor(colormap, i, &r, &g, &b);
		if (is_black_white && (r != g || r != b)) {
			continue;
		}
		colors << r << "," << g << "," << b << std::endl;
	}
	colors.close();

	pixWrite(native_paths[1].c_str(), pixc, 1);
	pixDestroy(&pixc);
	pixDestroy(&pixs);
	return 0;
}
