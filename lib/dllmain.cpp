#include "pch.h"

#include <filesystem>
#include <fstream>
#include <string>

#include <dwmapi.h>
#include <jni.h>
#include <Windows.h>
#include <windowsx.h>

using std::exception;
using std::filesystem::path;
using std::string;
using namespace sqlite;

string targetWndName = "Paletti";
bool isDarkMode = false;

/** 
 * Adapted from:
 * dgellow, 2022-01-18
 * https://stackoverflow.com/a/70753913
 * CC BY-SA 3.0
 * https://creativecommons.org/licenses/by-sa/3.0/
 */
bool is_light_theme() {
	auto buffer = std::vector<char>(4);
	auto cbData = static_cast<DWORD>(buffer.size() * sizeof(char));
	auto res = RegGetValueW(
		HKEY_CURRENT_USER,
		L"Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
		L"AppsUseLightTheme",
		RRF_RT_REG_DWORD,
		nullptr,
		buffer.data(),
		&cbData
	);

	if (res != ERROR_SUCCESS) {
		return true;
	}

	auto i = int(buffer[3] << 24 | buffer[2] << 16 | buffer[1] << 8 | buffer[0]);

	return i == 1;
}

const char* DB_STATEMENTS[] = {
	"SELECT value FROM environment WHERE name=?;",
	"SELECT count, is_black_white, is_dither, source FROM image WHERE id=?;",
	"DELETE FROM color WHERE image_id=?;",
	"INSERT INTO color (rgb, image_id) VALUES (?, ?);"
};

extern "C" JNIEXPORT int JNICALL
Java_app_paletti_lib_Leptonica_posterize2(
	JNIEnv * env,
	jobject /* this */,
	jint image_id,
	jstring db_path
) {
	const char* native_db_path = env->GetStringUTFChars(db_path, 0);

	try {
		database db(native_db_path);
		env->ReleaseStringUTFChars(db_path, native_db_path);

		path PalettiCache;
		db << DB_STATEMENTS[0] << "cache"
			>> [&](string cache) {
			PalettiCache = path(cache);
		};

		int max_count;
		bool is_black_white;
		bool is_dither;
		path PixSource;
		db << DB_STATEMENTS[1] << image_id
			>> [&](int t_count, bool t_is_black_white, bool t_is_dither, string t_source) {
			max_count = t_count;
			is_black_white = t_is_black_white;
			is_dither = t_is_dither;
			PixSource = path(t_source);
		};

		PIX* pixs = pixRead(PixSource.generic_string().c_str());
		if (pixs == nullptr) {
			return -1;
		}

		PIX* pixc;
		if (is_black_white) {
			PIX* tmp = pixConvertRGBToLuminance(pixs);
			PIX* tmp2 = pixConvert8To32(tmp);
			pixc = pixMedianCutQuantGeneral(tmp2, is_dither, 8, max_count, 0, 0, 0);
			pixDestroy(&tmp2);
			pixDestroy(&tmp);
		}
		else {
			pixc = pixMedianCutQuantGeneral(pixs, is_dither, 8, max_count, 0, 0, 0);
		}
		if (pixc == nullptr) {
			pixDestroy(&pixs);
			return -1;
		}

		db << DB_STATEMENTS[2] << image_id;

		PIXCMAP* colormap = pixGetColormap(pixc);
		const int count = pixcmapGetCount(colormap);
		for (int i = 0; i < count; i++) {
			int rgb, g, b;
			pixcmapGetColor(colormap, i, &rgb, &g, &b);
			if (is_black_white && (rgb != g || rgb != b)) {
				continue;
			}
			rgb = (rgb << 8) + g;
			rgb = (rgb << 8) + b;
			db << DB_STATEMENTS[3] << rgb << image_id;
		}

		const path OutPix = PalettiCache / path(std::to_string(image_id) + ".png");
		pixWrite(OutPix.string().c_str(), pixc, 1);
		pixDestroy(&pixc);
		pixDestroy(&pixs);
	}
	catch (exception& e) {
		return -1;
	}

	return 0;
}

extern "C" JNIEXPORT int JNICALL
Java_app_paletti_lib_Leptonica_posterize(
	JNIEnv * env,
	jobject /* this */,
	jint max_count,
	jboolean is_black_white,
	jobjectArray file_paths
) {
	string native_paths[3];
	int len = env->GetArrayLength(file_paths);
	for (int i = 0; i < len; i++) {
		jstring jstr = (jstring)(env->GetObjectArrayElement(file_paths, i));
		const jsize strLen = env->GetStringUTFLength(jstr);
		const char* charBuffer = env->GetStringUTFChars(jstr, (jboolean*)0);
		string str(charBuffer, strLen);
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
	} else {
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

void subclassWindow(HWND hWnd) {
	int trueValue = 0x01;
	int falseValue = 0x00;
	int micaValue = 0x38;
	int windowValue = 0x2;

	auto ok = DwmSetWindowAttribute(hWnd, 38, &windowValue, sizeof(int));

	if (!SUCCEEDED(ok)) {
		DwmSetWindowAttribute(hWnd, 1029, &trueValue, sizeof(int));
	}

	if (isDarkMode) {
		DwmSetWindowAttribute(hWnd, 20, &trueValue, sizeof(int));
	}
}

BOOL CALLBACK EnumWindowsProc(HWND hWnd, LPARAM lParam) {
	char String[255];

	if (!hWnd || !IsWindowVisible(hWnd) || !SendMessage(hWnd, WM_GETTEXT, sizeof(String), (LPARAM)String)) {
		return TRUE;
	}

	char pszClassName[64];
	GetClassName(hWnd, pszClassName, 64);
	if (_stricmp(pszClassName, "shell_traywnd") && _stricmp(pszClassName, "progman")) {
		char windowTitle[64];
		GetWindowText(hWnd, windowTitle, 64);
		if (!targetWndName.compare(windowTitle)) {
			subclassWindow(hWnd);
			return FALSE;
		}
	}

	return 1;
}

extern "C" JNIEXPORT void JNICALL
Java_app_paletti_lib_Windows_subclass(
	JNIEnv * env,
	jobject,
	jstring title,
	jboolean isSetDarkMode
) {
	isDarkMode = (bool)isSetDarkMode;

	jboolean isCopy;
	const char* convertedValue = (env)->GetStringUTFChars(title, &isCopy);
	string str = convertedValue;
	targetWndName = str;

	EnumWindows(EnumWindowsProc, 0);

	if (isCopy == JNI_TRUE) {
		env->ReleaseStringUTFChars(title, convertedValue);
	}
}

extern "C" JNIEXPORT bool JNICALL
Java_app_paletti_lib_Windows_isdarkmode(JNIEnv * env, jobject) {
	return !is_light_theme();
}
