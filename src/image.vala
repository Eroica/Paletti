namespace Paletti {
	using Leptonica;

	errordomain FileTypeError {
		UNSUPPORTED
	}

	public class RGB {
		public int r;
		public int g;
		public int b;

		public RGB (int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public string to_string () {
			return "#%02x%02x%02x".printf (r, g, b);
		}
	}

	public class Colors {
		private RGB[] colors;
		public int size {
			get { return colors.length; }
		}

		public Colors (RGB[] colors) {
			this.colors = colors;
		}

		public RGB get (int index) {
			return colors[index];
		}
	}

	class PosterizedImage {
		private PIX pix;

		public int count {
			get { return pix.colormap.size; }
		}

		public RGB[] colors {
			owned get { return pix.colormap.colors; }
		}

		public PosterizedImage.from_file (string filename) throws FileTypeError {
			var src = new PIX.from_filename (filename);
			if (src == null) {
				throw new FileTypeError.UNSUPPORTED("Could not read this image");
			}
			this.pix = pixMedianCutQuantGeneral (src, 0, 8, MAX_COLORS);
		}
	}
}
