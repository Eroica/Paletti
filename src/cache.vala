namespace Paletti {
	public void save_cached_image (Leptonica.PIX pix) {
		var paletti_cache_dir = Path.build_filename (
			Environment.get_user_cache_dir (),
			"Paletti"
		);
		DirUtils.create_with_parents (paletti_cache_dir, 755);
		var dest_path = Path.build_filename (paletti_cache_dir, "cache.png");
		Leptonica.pix_write (dest_path, pix, pix.input_format);
	}

	public string get_cached_image () {
		return Path.build_filename (
			Environment.get_user_cache_dir (),
			"Paletti",
			"cache.png"
		);
	}
}

