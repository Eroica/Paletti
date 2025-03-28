#include <iostream>
#include <fstream>
#include <string>
#include <vector>

#include <android/log.h>
#include <jni.h>

#include "allheaders.h"

#define LOG_TAG "Paletti"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT int JNICALL
Java_app_paletti_lib_Leptonica_posterize(
	JNIEnv *env,
	jobject /* this */,
	jint max_count,
	jboolean is_black_white,
	jobjectArray file_paths) {
	int len = env->GetArrayLength(file_paths);
	std::vector<std::string> native_paths;
	native_paths.reserve(len);

	for (int i = 0; i < len; i++) {
		jstring jstr = (jstring)(env->GetObjectArrayElement(file_paths, i));
		if (!jstr) {
		    continue;
		}

		const char* charBuffer = env->GetStringUTFChars(jstr, nullptr);
		native_paths.emplace_back(charBuffer);
		env->ReleaseStringUTFChars(jstr, charBuffer);
		env->DeleteLocalRef(jstr);
	}

	if (native_paths.size() < 3) {
		LOGE("Error: Expected at least 3 file paths, got %d", len);
		return -1;
	}

	PIX *pixs = pixRead(native_paths[0].c_str());
	if (!pixs) {
		LOGE("Error: Failed to read input image %s", native_paths[0].c_str());
		return -1;
	}

	PIX *pixc = nullptr;
	if (is_black_white) {
		PIX *tmp = pixConvertRGBToLuminance(pixs);
		PIX *tmp2 = pixConvert8To32(tmp);
		pixc = pixMedianCutQuantGeneral(tmp2, 0, 8, (int)max_count, 0, 0, 0);

		if (!pixc) {
			LOGE("Error: pixMedianCutQuantGeneral failed");
			pixDestroy(&tmp2);
			pixDestroy(&tmp);
			pixDestroy(&pixs);
			return -1;
		}

		pixDestroy(&tmp2);
		pixDestroy(&tmp);
	} else {
		pixc = pixMedianCutQuantGeneral(pixs, 0, 8, (int)max_count, 0, 0, 0);
		if (!pixc) {
			LOGE("Error: pixMedianCutQuantGeneral failed");
			pixDestroy(&pixs);
			return -1;
		}
	}

	PIXCMAP *colormap = pixGetColormap(pixc);
	int count = pixcmapGetCount(colormap);

	std::ofstream colors(native_paths[2]);
	if (!colors.is_open()) {
		LOGE("Error: Failed to open color output file %s", native_paths[2].c_str());
	} else {
		for (int i = 0; i < count; i++) {
			int r, g, b;
			pixcmapGetColor(colormap, i, &r, &g, &b);
			if (is_black_white && (r != g || r != b)) continue;
			colors << r << "," << g << "," << b << std::endl;
		}
		colors.close();
	}

	if (pixWrite(native_paths[1].c_str(), pixc, 1) != 0) {
		LOGE("Error: Failed to write image file %s", native_paths[1].c_str());
	}

	pixDestroy(&pixc);
	pixDestroy(&pixs);
	return 0;
}
