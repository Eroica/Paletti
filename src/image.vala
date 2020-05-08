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
	public struct RGB {
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
		public signal void change (Colors colors);

		public abstract void copy () throws Leptonica.Exception;
		public abstract void save () throws Leptonica.Exception;
		public abstract void load (string filename) throws Leptonica.Exception;
	}

	public class PosterizedPix {
		public Colors colors { get; private set; }

		public PosterizedPix (Leptonica.PIX src,
		                      int colors_count,
		                      bool is_black_white) throws Leptonica.Exception {
			Leptonica.PIX tmp;
			if (is_black_white) {
				tmp = Leptonica.pixAddMinimalGrayColormap8 (Leptonica.pixRemoveColormap (
					Leptonica.pixMedianCutQuantGeneral (src, 0, 8, colors_count)
				));
			} else {
				tmp = Leptonica.pixMedianCutQuantGeneral (src, 0, 8, colors_count);
			}
			if (tmp == null) {
				throw new Leptonica.Exception.FAILURE ("Could not run quantization on this image.");
			}
			save_cached_image (tmp);
			this.colors = new Colors (tmp.colormap.colors);
		}
	}

	public class NullImage : Image, IPosterizedImage {
		public NullImage () {
			set_from_icon_name ("gtk-missing-image", IconSize.DND);
			get_style_context ().add_class ("preview-image");
		}

		public void copy () throws Leptonica.Exception {
			throw new Leptonica.Exception.UNINITIALIZED ("First load an image into Paletti.");
		}

		public void save () throws Leptonica.Exception {
			throw new Leptonica.Exception.UNINITIALIZED ("First load an image into Paletti.");
		}

		public void load (string filename) throws Leptonica.Exception {
			throw new Leptonica.Exception.UNINITIALIZED ("First load an image into Paletti.");
		}
	}

	public class PosterizedImage : Image, IPosterizedImage {
		private Leptonica.PIX src;
		private PosterizedPix pix;
		private Notification notification;

		public PosterizedImage (string filename, Notification notification,
		                        IPosterize s) {
			get_style_context ().add_class ("preview-image");
			this.notification = notification;
			s.posterize.connect ((colors_count, is_black_white, dimensions) => {
				try {
					posterize (colors_count, is_black_white);
					setup (dimensions);
				} catch (Leptonica.Exception e) {
					if (!(e is Leptonica.Exception.UNINITIALIZED)) {
						notification.display (e.message);
					}
				} catch (Error e) {
					notification.display (e.message);
				}
			});
			show ();
		}

		public void copy () throws Leptonica.Exception {
			Clipboard.get_for_display (
				get_window ().get_display (), Gdk.SELECTION_CLIPBOARD
			).set_image (pixbuf);
		}

		public void save () throws Leptonica.Exception {
			var dialog = new SaveFileDialog ();
			dialog.response.connect ((dialog, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					try {
						var file_dialog = dialog as FileChooserDialog;
						File.new_for_path (get_cached_image ()).copy (
							file_dialog.get_file (), FileCopyFlags.OVERWRITE
						);
					} catch (Error e) {
						notification.display (e.message);
					}
				}
				dialog.destroy ();
			});
			dialog.show ();
		}

		public void load (string filename) throws Leptonica.Exception {
			src = new Leptonica.PIX.from_filename (filename);
			if (src == null) {
				throw new Leptonica.Exception.UNSUPPORTED ("Could not read this image");
			}
		}

		private void posterize (int colors_count,
		                        bool is_black_white) throws Leptonica.Exception {
			pix = new PosterizedPix (src, colors_count, is_black_white);
			change (pix.colors);
		}

		private void setup (Allocation dimensions) throws Error {
			var ratio = (double) src.width / src.height;
			var target_width = (double) dimensions.width;
			var target_height = (double) dimensions.height;

			if (ratio >= 1) {
				target_width = (double) dimensions.height/src.height * src.width;
			} else {
				target_height = (double) dimensions.width/src.width * src.height;
			}

			var dest = new Pixbuf (Colorspace.RGB, false, 8, dimensions.width, dimensions.height);
			var tmp = new Pixbuf.from_file (get_cached_image ());
			tmp.scale (
				dest,
				0, 0,
				dimensions.width, dimensions.height,
				-(int) (target_width - dimensions.width) / 2,
				-(int) (target_height - dimensions.height) / 2,
				target_width/src.width, target_height/src.height,
				InterpType.BILINEAR
			);
			set_from_pixbuf (dest);
		}
	}
}

