using Gdk;
using Gtk;

namespace Leptonica {
	public errordomain Exception {
		UNSUPPORTED,
		FAILURE,
	}
}

namespace Paletti {
	public errordomain Exception {
		UNINITIALIZED
	}

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

	public abstract class IPix : Object {
		public Colors colors { get; protected set; }
		public Leptonica.PIX pix { get; protected owned set; }
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
		public CachedPix (IPix src) {
			pix = src.pix.clone ();
			save_cached_image (src.pix);
			colors = src.colors;
		}
	}

	public interface IPosterizedImage : Object {
		public signal void change (Colors colors);

		public abstract void on_copy () throws Exception;
		public abstract void on_save () throws Exception;
		public abstract void on_load (string filename) throws Leptonica.Exception;
		public abstract void posterize (int colors_count, bool is_black_white);
	}

	public class NullImage : IPosterizedImage, Object {
		public void on_copy () throws Exception {
			throw new Exception.UNINITIALIZED ("First load an image into Paletti.");
		}

		public void on_save () throws Exception {
			throw new Exception.UNINITIALIZED ("First load an image into Paletti.");
		}

		public void on_load (string filename) throws Leptonica.Exception {}
		public void posterize (int colors_count, bool is_black_white) {}
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/posterized-image.ui")]
	public class PosterizedImage : Image, IPosterizedImage {
		private Leptonica.PIX src;
		private INotification notification;

		public PosterizedImage (INotification notification) {
			this.notification = notification;
		}

		public void on_copy () throws Exception {
			Clipboard.get_for_display (
				get_window ().get_display (), Gdk.SELECTION_CLIPBOARD
			).set_image (pixbuf);
		}

		public void on_save () throws Exception {
			show_save_dialog ();
		}

		public void on_load (string filename) throws Leptonica.Exception {
			src = new Leptonica.PIX.from_filename (filename);
			if (src == null) {
				throw new Leptonica.Exception.UNSUPPORTED ("Could not read this image");
			}
		}

		public void posterize (int colors_count, bool is_black_white) {
			try {
				IPix tmp;
				if (is_black_white) {
					tmp = new CachedPix (new BlackWhitePix (new PosterizedPix (src, colors_count)));
				} else {
					tmp = new CachedPix (new PosterizedPix (src, colors_count));
				}
				load_image ();
				change (tmp.colors);
			} catch (Leptonica.Exception e) {
				notification.display (e.message);
			}
		}

		private void show_save_dialog () {
			var dialog = new SaveFileDialog ();
			dialog.response.connect ((dialog, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					try {
						var file_dialog = dialog as FileChooserDialog;
						var file = file_dialog.get_file ();
						File.new_for_path (get_cached_image ()).copy (
							file, FileCopyFlags.OVERWRITE
						);
						notification.display (
							@"Copied image to $(file.get_path ())",
							NotificationType.INFO
						);
					} catch (Error e) {
						notification.display (e.message);
					}
				}
				dialog.destroy ();
			});
			dialog.show ();
		}

		private void load_image () {
			try {
				var tmp = new Pixbuf.from_file (get_cached_image ());
				var parent = parent as Stack;
				var dimensions = Allocation ();
				parent.get_allocation (out dimensions);
				var ratio = (double) src.width / src.height;
				var target_width = (double) dimensions.width;
				var target_height = (double) dimensions.height;

				if (ratio >= 1) {
					target_width = (double) dimensions.height/src.height * src.width;
				} else {
					target_height = (double) dimensions.width/src.width * src.height;
				}
				var dest = new Pixbuf (Colorspace.RGB, false, 8, dimensions.width, dimensions.height);
				tmp.scale (
					dest, 0, 0, dimensions.width, dimensions.height,
					-(int) (target_width - dimensions.width) / 2,
					-(int) (target_height - dimensions.height) / 2,
					target_width/src.width, target_height/src.height,
					InterpType.BILINEAR
				);
				set_from_pixbuf (dest);
			} catch (Error e) {
				set_from_icon_name ("gtk-missing-image", IconSize.DND);
			}
		}
	}
}
