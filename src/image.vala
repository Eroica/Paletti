namespace Leptonica {
	public errordomain Exception {
		UNSUPPORTED,
		FAILURE
	}
}

namespace Paletti {
	using Leptonica;

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

	public interface IPosterizedImage : Object {
		public abstract bool is_black_white { get; set; }
		public abstract Colors? posterize (int color_count);
		public abstract void load_from_file (string filename) throws Exception;
	}

	public class PosterizedImage : Object, IPosterizedImage {
		private PIX? src;
		public bool is_black_white { get; set; }

		public PosterizedImage.from_file (string filename) throws Exception {
			load_from_file (filename);
		}

		public void load_from_file (string filename) throws Exception {
			this.src = new PIX.from_filename (filename);
			if (src == null) {
				throw new Exception.UNSUPPORTED("Could not read this image");
			}
		}

		public Colors? posterize (int color_count) {
			if (src == null) {
				return null;
			}
			var count = int.min (color_count, MAX_COLORS);

			PIX tmp;
			if (is_black_white) {
				tmp = pixAddMinimalGrayColormap8 (pixRemoveColormap (
					pixMedianCutQuantGeneral (src, 0, 8, count)
				));
			} else {
				tmp = pixMedianCutQuantGeneral (src, 0, 8, count);
			}
			save_cached_image (tmp);
			return new Colors (tmp.colormap.colors);
		}
	}
}

