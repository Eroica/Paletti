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
		public Colors colors { get; protected set; }
		public Leptonica.PIX pix { get; protected owned set; }
		public int width { get { return pix.width; } }
		public int height { get { return pix.height; } }
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
			pix = Leptonica.pixMedianCutQuantGeneral (src, 0, 8, colors_count);
			if (pix == null) {
				throw new Leptonica.Exception.FAILURE ("Could not run quantization on this image.");
			}
			colors = new Colors (pix.colormap.colors);
		}
	}

	public class BlackWhitePix : IPix {
		public BlackWhitePix (IPix src) throws Leptonica.Exception {
			pix = Leptonica.pixAddMinimalGrayColormap8 (
				Leptonica.pixRemoveColormap (src.pix)
			);
			if (pix == null) {
				throw new Leptonica.Exception.FAILURE ("Could not run quantization on this image.");
			}
			colors = new Colors (pix.colormap.colors);
		}
	}

	public class CachedPix : IPix {
		public string path = Path.build_filename (
			Environment.get_user_cache_dir (), "Paletti", "cache.png"
		);

		public CachedPix (IPix src) {
			pix = src.pix.clone ();
			var paletti_cache_dir = Path.build_filename (
				Environment.get_user_cache_dir (),
				"Paletti"
			);
			DirUtils.create_with_parents (paletti_cache_dir, 755);
			var dest_path = Path.build_filename (paletti_cache_dir, "cache.png");
			Leptonica.pix_write (dest_path, pix, pix.input_format);
			colors = src.colors;
		}

		public void copy (File destination) throws Error {
			File.new_for_path (path).copy (
				destination, FileCopyFlags.OVERWRITE
			);
		}
	}

	public interface IViewModel : Object {
		public abstract CachedPix? pix { get; protected set; }

		public abstract void on_shortcut (EventKey event) throws Exception;
		public abstract void on_colors_range_change (int count) throws Leptonica.Exception;
		public abstract void on_load (string filename, int count) throws Leptonica.Exception;
		public abstract void on_set_black_white (bool is_black_white) throws Leptonica.Exception;
	}

	public class ImageViewModel : IViewModel, Object {
		public CachedPix? pix { get; protected set; }
		protected bool is_black_white { get; set; }
		private Leptonica.PIX? src { get; owned set; }

		public void on_load (string filename, int count) throws Leptonica.Exception {
			this.src = new Leptonica.PIX.from_filename (filename);
			on_colors_range_change (count);
		}

		public void on_colors_range_change (int count) throws Leptonica.Exception {
			if (src == null) {
				return;
			}
			if (is_black_white) {
				pix = new CachedPix (new BlackWhitePix (new PosterizedPix (src, count)));
			} else {
				pix = new CachedPix (new PosterizedPix (src, count));
			}
		}

		public void on_set_black_white (bool is_black_white) throws Leptonica.Exception {
			this.is_black_white = is_black_white;
			if (src != null) {
				on_colors_range_change (pix.colors.size);
			}
		}
		public void on_shortcut (EventKey event) throws Exception {
			if (src == null) {
				if (event.state == ModifierType.CONTROL_MASK
					&& (event.keyval == Key.s || event.keyval == Key.c || event.keyval == Key.e)) {
					throw new Exception.UNINITIALIZED ("First load an image into Paletti.");
				}
			}
		}
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/posterized-image.ui")]
	public class PosterizedImage : Image {
		public PosterizedImage (IViewModel view_model) {
			view_model.notify["pix"].connect (() => {
				this.load (view_model.pix);
			});
		}

		private void load (CachedPix pix) {
			try {
				var tmp = new Pixbuf.from_file (pix.path);
				var parent = parent as Stack;
				var dimensions = Allocation ();
				parent.get_allocation (out dimensions);
				var ratio = (double) pix.width / pix.height;
				var target_width = (double) dimensions.width;
				var target_height = (double) dimensions.height;

				if (ratio >= 1) {
					target_width = (double) dimensions.height/pix.height * pix.width;
				} else {
					target_height = (double) dimensions.width/pix.width * pix.height;
				}
				var dest = new Pixbuf (Colorspace.RGB, false, 8, dimensions.width, dimensions.height);
				tmp.scale (
					dest, 0, 0, dimensions.width, dimensions.height,
					-(int) (target_width - dimensions.width) / 2,
					-(int) (target_height - dimensions.height) / 2,
					target_width/pix.width, target_height/pix.height,
					InterpType.BILINEAR
				);
				set_from_pixbuf (dest);
			} catch (Error e) {
				set_from_icon_name ("gtk-missing-image", IconSize.DND);
			}
		}
	}
}
