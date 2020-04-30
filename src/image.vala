namespace Paletti {
	using Leptonica;

	errordomain FileTypeError {
		UNSUPPORTED
	}

	class PosterizedImage {
		private PIX pix;

		public PosterizedImage.from_file (string filename) throws FileTypeError {
			var src = new PIX.from_filename (filename);
			if (src == null) {
				throw new FileTypeError.UNSUPPORTED("Could not read this image");
			}
			this.pix = pixMedianCutQuantGeneral (src, 0, 8, MAX_COLORS);
		}

		public RGB[] get_colors () {
			var count = pix.get_colormap ().get_count ();
			var r = new int[count];
			var g = new int[count];
			var b = new int[count];
			var a = new int[count];
			pix.get_colormap ().get_arrays (out r, out g, out b, out a);

			RGB[] colors = {};
			for (int i=0; i < count; i++) {
				colors += RGB (r[i], g[i], b[i]);
			}
			return colors;
		}
	}
}
