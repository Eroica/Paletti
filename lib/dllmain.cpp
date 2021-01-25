#include "pch.h"

#include <filesystem>
#include <fstream>
#include <string>

#define APPNAME "Paletti"
#define PALETTI_DB "Paletti.db"

using std::filesystem::path;

const char* DB_STATEMENTS[] = {
	"SELECT value FROM environment WHERE name=?;"
	"SELECT count, is_black_white, source FROM image WHERE id=?;",
	"INSERT INTO color (rgb, image_id) VALUES (?, ?);"
};

extern "C" JNIEXPORT int JNICALL
Java_app_paletti_lib_Leptonica_posterize2(
	JNIEnv * env,
	jobject /* this */,
	jint image_id,
	jstring db_path) {

	sqlite3* db;
	char* zErrMsg = 0;
	int rc;
	const char* native_db_path = env->GetStringUTFChars(db_path, 0);

	rc = sqlite3_open(native_db_path, &db);
	if (rc) {
		printf("Can't open database: %s\n", sqlite3_errmsg(db));
		return -1;
	}
	env->ReleaseStringUTFChars(db_path, native_db_path);

	path PalettiCache;
	sqlite3_stmt* stmt_SelectCache;
	rc = sqlite3_prepare_v2(db, DB_STATEMENTS[0], -1, &stmt_SelectCache, nullptr);
	sqlite3_bind_text(stmt_SelectCache, 1, "cache", -1, SQLITE_STATIC);
	rc = sqlite3_step(stmt_SelectCache);
	if (rc == SQLITE_ROW) {
		PalettiCache = path(reinterpret_cast<const char *>(sqlite3_column_text(stmt_SelectCache, 0)));
	}
	sqlite3_finalize(stmt_SelectCache);
	
	sqlite3_stmt* stmt_SelectImage;
	rc = sqlite3_prepare_v2(db, "SELECT count, is_black_white, source FROM image WHERE id=?", -1, &stmt_SelectImage, nullptr);
	sqlite3_bind_int(stmt_SelectImage, 1, image_id);
	rc = sqlite3_step(stmt_SelectImage);
	int max_count = sqlite3_column_int(stmt_SelectImage, 0);
	bool is_black_white = sqlite3_column_int(stmt_SelectImage, 1);
	path PixSource(reinterpret_cast<const char*>(sqlite3_column_text(stmt_SelectImage, 2)));
	sqlite3_finalize(stmt_SelectImage);

	PIX* pixs = pixRead(PixSource.generic_string().c_str());
	if (pixs == NULL) {
		sqlite3_close(db);
		return -1;
	}

	PIX* pixc;
	if (is_black_white) {
		PIX* tmp = pixConvertRGBToLuminance(pixs);
		PIX* tmp2 = pixConvert8To32(tmp);
		pixc = pixMedianCutQuantGeneral(tmp2, 0, 8, max_count, 0, 0, 0);
		pixDestroy(&tmp2);
		pixDestroy(&tmp);
	}
	else {
		pixc = pixMedianCutQuantGeneral(pixs, 0, 8, max_count, 0, 0, 0);
	}
	if (pixc == NULL) {
		pixDestroy(&pixs);
		sqlite3_close(db);
		return -1;
	}

	PIXCMAP* colormap = pixGetColormap(pixc);
	int count = pixcmapGetCount(colormap);

	sqlite3_stmt* stmt_DeleteColors;
	rc = sqlite3_prepare_v2(db, "DELETE FROM color WHERE image_id=?;", -1, &stmt_DeleteColors, nullptr);
	sqlite3_bind_int(stmt_DeleteColors, 1, image_id);
	sqlite3_step(stmt_DeleteColors);
	sqlite3_finalize(stmt_DeleteColors);

	sqlite3_stmt* stmt_InsertColors;
	rc = sqlite3_prepare_v2(db, "INSERT INTO color (rgb, image_id) VALUES (?, ?);", -1, &stmt_InsertColors, nullptr);

	for (int i = 0; i < count; i++) {
		int rgb, g, b;
		pixcmapGetColor(colormap, i, &rgb, &g, &b);
		if (is_black_white && (rgb != g || rgb != b)) {
			continue;
		}
		rgb = (rgb << 8) + g;
		rgb = (rgb << 8) + b;
		sqlite3_bind_int(stmt_InsertColors, 1, rgb);
		sqlite3_bind_int(stmt_InsertColors, 2, image_id);
		sqlite3_step(stmt_InsertColors);
		sqlite3_reset(stmt_InsertColors);
	}
	sqlite3_finalize(stmt_InsertColors);

	path OutPix = PalettiCache / path(std::to_string(image_id) + ".png");
	pixWrite(OutPix.string().c_str(), pixc, 1);
	pixDestroy(&pixc);
	pixDestroy(&pixs);
	sqlite3_close(db);

	return 0;
}

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
	if (pixs == NULL) {
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
	if (pixc == NULL) {
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
