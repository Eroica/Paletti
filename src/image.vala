using Gdk;
using Gtk;

namespace Leptonica {
	public errordomain Exception {
		UNSUPPORTED,
		FAILURE,
		UNINITIALIZED
	}
}

namespace Paletti {
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

	public interface IPosterizedImage : Image {
		public abstract bool is_black_white { get; set; }
		public abstract void save () throws Leptonica.Exception;
		public abstract void copy () throws Leptonica.Exception;
		public abstract void load_from (string filename) throws Leptonica.Exception;
		public abstract Colors posterize (int color_count) throws Leptonica.Exception;
	}

	public class NullImage : Image, IPosterizedImage {
		public bool is_black_white { get; set; }

		public NullImage () {
			set_from_icon_name ("gtk-missing-image", IconSize.DND);
			get_style_context ().add_class ("preview-image");
		}

		public void save () throws Leptonica.Exception {
			throw new Leptonica.Exception.UNINITIALIZED ("First load an image into Paletti.");
		}

		public void copy () throws Leptonica.Exception {
			throw new Leptonica.Exception.UNINITIALIZED ("First load an image into Paletti.");
		}

		public void load_from (string filename) throws Leptonica.Exception {
			throw new Leptonica.Exception.UNINITIALIZED ("First load an image into Paletti.");
		}

		public Colors posterize (int color_count) throws Leptonica.Exception {
			throw new Leptonica.Exception.UNINITIALIZED ("First load an image into Paletti.");
		}
	}

	public class PosterizedImage : Image, IPosterizedImage {
		private Leptonica.PIX src;
		public bool is_black_white { get; set; }

		public PosterizedImage.from_file (string filename,
		                                  bool is_black_white = false) throws Leptonica.Exception {
			get_style_context ().add_class ("preview-image");
			this.is_black_white = is_black_white;
			src = new Leptonica.PIX.from_filename (filename);
			if (src == null) {
				throw new Leptonica.Exception.UNSUPPORTED("Could not read this image.");
			}
		}

		public void save () throws Leptonica.Exception {
			var dialog = new SaveFileDialog ();
			dialog.response.connect ((dialog, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					var file_dialog = dialog as FileChooserDialog;
					var ok = Leptonica.pix_write (
						Filename.from_uri (file_dialog.get_file ().get_uri ()),
						src,
						3 // PNG
					);
					if (ok != 0) {
						throw new Leptonica.Exception.FAILURE ("An error occurred while saving the image.");
					}
				}
				dialog.destroy ();
			});
			dialog.show ();
		}

		public void copy () throws Leptonica.Exception {
			Clipboard.get_for_display (
				get_window ().get_display (), Gdk.SELECTION_CLIPBOARD
			).set_image (pixbuf);
		}

		public void load_from (string filename) {
			src = new Leptonica.PIX.from_filename (filename);
			if (src == null) {
				throw new Leptonica.Exception.UNSUPPORTED("Could not read this image");
			}
		}

		public Colors posterize (int color_count) throws Leptonica.Exception {
			var count = int.min (color_count, MAX_COLORS);
			Leptonica.PIX tmp;
			if (is_black_white) {
				tmp = Leptonica.pixAddMinimalGrayColormap8 (Leptonica.pixRemoveColormap (
					Leptonica.pixMedianCutQuantGeneral (src, 0, 8, count)
				));
			} else {
				tmp = Leptonica.pixMedianCutQuantGeneral (src, 0, 8, count);
			}
			save_cached_image (tmp);
			load_image ();
			return new Colors (tmp.colormap.colors);
		}

		private void load_image () {
			var dimensions = Allocation ();
			(parent as Stack).get_allocation (out dimensions);
			var ratio = (double) src.width / src.height;
			var width = (double) dimensions.width;
			var height = (double) dimensions.height;

			if (ratio >= 1) {
				width = (double) dimensions.height/src.height * src.width;
			} else {
				height = (double) dimensions.width/src.width * src.height;
			}

			var tmp = new Pixbuf (Colorspace.RGB, false, 8, dimensions.width, dimensions.height);
			(new Pixbuf.from_file (get_cached_image ()))
				.scale (
					tmp,
					0, 0,
					dimensions.width, dimensions.height,
					-(int) (width - dimensions.width) / 2, -(int) (height - dimensions.height) / 2,
					width/src.width, height/src.height,
					InterpType.BILINEAR
				);
			set_from_pixbuf (tmp);
		}
	}
}

