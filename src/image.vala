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
		public abstract void on_copy () throws Exception;
		public abstract void on_save () throws Exception;
		public abstract void on_load (string filename) throws Leptonica.Exception;
		public abstract void posterize (int colors_count, bool is_black_white);
	}

	public interface IColorPalette : Object {
		public abstract Colors? colors { get; set; }

		public abstract void export () throws Exception;
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/posterized-image.ui")]
	public class PosterizedImage : Image, IPosterizedImage {
		private Leptonica.PIX? src;
		private INotification notification;
		private IColorPalette color_palette;
		public string filename { get; private set; }

		public PosterizedImage (IColorPalette color_palette,
		                        INotification notification) {
			this.color_palette = color_palette;
			this.notification = notification;
		}

		public void on_copy () throws Exception {
			if (src == null) {
				throw new Exception.UNINITIALIZED ("First load an image into Paletti.");
			}
			Clipboard.get_for_display (
				get_window ().get_display (), Gdk.SELECTION_CLIPBOARD
			).set_image (pixbuf);
		}

		public void on_save () throws Exception {
			if (src == null) {
				throw new Exception.UNINITIALIZED ("First load an image into Paletti.");
			}
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

		public void on_load (string filename) throws Leptonica.Exception {
			var tmp = new Leptonica.PIX.from_filename (filename);
			// The second check might be a little bit wasteful, but is there to
			// check whether the image is analyzable by Leptonica. To be exact,
			// `pix != null' can never happen because PosterziedPix would have
			// already raised a FAILURE exception. This exception prevents
			// `src' from being assigned to the un-analyzable image.
			if (tmp == null || new PosterizedPix (tmp, MAX_COLORS).pix == null) {
				throw new Leptonica.Exception.UNSUPPORTED ("Could not read this image");
			}
			this.filename = filename;
			src = tmp.clone ();
		}

		public void posterize (int colors_count, bool is_black_white) {
			if (src == null) {
				return;
			}
			try {
				IPix tmp;
				if (is_black_white) {
					tmp = new CachedPix (new BlackWhitePix (new PosterizedPix (src, colors_count)));
				} else {
					tmp = new CachedPix (new PosterizedPix (src, colors_count));
				}
				load_image ();
				color_palette.colors = tmp.colors;
			} catch (Leptonica.Exception e) {
				notification.display (e.message);
			}
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
