namespace Paletti {
	public string save_cached_image (Leptonica.PIX pix) {
		var paletti_cache_dir = Path.build_filename (
			Environment.get_user_cache_dir (),
			"Paletti"
		);
		DirUtils.create_with_parents (paletti_cache_dir, 755);
		var dest_path = Path.build_filename (paletti_cache_dir, "cache.png");
		Leptonica.pixWrite (dest_path, pix, 3);
		return dest_path;
	}
}

