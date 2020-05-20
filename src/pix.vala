using Gdk;
using Gtk;

namespace Leptonica {
	public errordomain Exception {
		UNSUPPORTED,
		FAILURE,
	}

	public enum Format {
		UNKNOWN, BMP, JFIF_JPEG, PNG, TIFF, TIFF_PACKBITS, TIFF_RLE, TIFF_G3,
		TIFF_G4, TIFF_LZW, TIFF_ZIP, PNM, PS, GIF, JP2, WEBP, LPDF, TIFF_JPEG,
		DEFAULT, SPIX;

		public string to_string () {
			switch (this) {
				case BMP: return "bmp";
				case JFIF_JPEG: return "jpg";
				case PNG: return "png";
				case TIFF: return "tiff";
				case TIFF_PACKBITS: return "tiff";
				case TIFF_RLE: return "tiff";
				case TIFF_G3: return "tiff";
				case TIFF_G4: return "tiff";
				case TIFF_LZW: return "tiff";
				case TIFF_ZIP: return "tiff";
				case PNM: return "pnm";
				case PS: return "ps";
				case GIF: return "gif";
				case JP2: return "jp2";
				case WEBP: return "webp";
				case LPDF: return "lpdf";
				case TIFF_JPEG: return "tiff";
				default: return "";
			}
		}

		public int to_int () {
			switch (this) {
				case BMP: return 1;
				case JFIF_JPEG: return 2;
				case PNG: return 3;
				case TIFF: return 4;
				case TIFF_PACKBITS: return 5;
				case TIFF_RLE: return 6;
				case TIFF_G3: return 7;
				case TIFF_G4: return 8;
				case TIFF_LZW: return 9;
				case TIFF_ZIP: return 10;
				case PNM: return 11;
				case PS: return 12;
				case GIF: return 13;
				case JP2: return 14;
				case WEBP: return 15;
				case LPDF: return 16;
				case TIFF_JPEG: return 17;
				default: return 0;
			}
		}
	}
}

namespace Paletti {
	public errordomain Exception {
		UNINITIALIZED
	}

	public abstract class IPix : Object {
		public int width { get { return pix.width; } }
		public int height { get { return pix.height; } }
		public double ratio { get { return width/height; } }
		public Colors colors { get; protected set; }
		public Leptonica.PIX pix { get; protected owned set; }


		public Leptonica.Format format {
			get {
				switch (pix.input_format) {
					case 1: return Leptonica.Format.BMP;
					case 2: return Leptonica.Format.JFIF_JPEG;
					case 3: return Leptonica.Format.PNG;
					case 4: return Leptonica.Format.TIFF;
					case 5: return Leptonica.Format.TIFF_PACKBITS;
					case 6: return Leptonica.Format.TIFF_RLE;
					case 7: return Leptonica.Format.TIFF_G3;
					case 8: return Leptonica.Format.TIFF_G4;
					case 9: return Leptonica.Format.TIFF_LZW;
					case 10: return Leptonica.Format.TIFF_ZIP;
					case 11: return Leptonica.Format.PNM;
					case 12: return Leptonica.Format.PS;
					case 13: return Leptonica.Format.GIF;
					case 14: return Leptonica.Format.JP2;
					case 15: return Leptonica.Format.WEBP;
					case 16: return Leptonica.Format.LPDF;
					case 17: return Leptonica.Format.TIFF_JPEG;
					default: return Leptonica.Format.UNKNOWN;
				}
			}
		}
	}

	public class PosterizedPix : IPix {
		public PosterizedPix (Leptonica.PIX src, int colors_count) throws Leptonica.Exception {
			this.pix = Leptonica.pixMedianCutQuantGeneral (src, 0, 8, colors_count);
			if (this.pix == null) {
				throw new Leptonica.Exception.FAILURE ("Could not run quantization on this image.");
			}
			this.colors = new Colors (pix.colormap.colors);
		}
	}

	public class BlackWhitePix : IPix {
		public BlackWhitePix (IPix src) throws Leptonica.Exception {
			this.pix = Leptonica.pixAddMinimalGrayColormap8 (
				Leptonica.pixRemoveColormap (src.pix)
			);
			if (this.pix == null) {
				throw new Leptonica.Exception.FAILURE ("Could not run quantization on this image.");
			}
			this.colors = new Colors (pix.colormap.colors);
		}
	}

	public class CachedPix : IPix {
		public string path { get; private set; }

		public CachedPix (IPix src) {
			this.pix = src.pix.clone ();
			var paletti_cache_dir = Path.build_filename (
				Environment.get_user_cache_dir (),
				"Paletti"
			);
			DirUtils.create_with_parents (paletti_cache_dir, 755);
			var dest_path = Path.build_filename (paletti_cache_dir, @"cache.$(src.format)");
			this.pix.write (dest_path, pix.input_format);
			this.colors = src.colors;
			this.path = Path.build_filename (
				Environment.get_user_cache_dir (), "Paletti",
				@"cache.$(src.format)"
			);
		}

		public void copy (File destination) throws Error {
			File.new_for_path (path).copy (
				destination, FileCopyFlags.OVERWRITE
			);
		}
	}
}
