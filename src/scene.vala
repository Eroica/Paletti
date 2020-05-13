using Gdk;
using Gtk;

namespace Paletti {
	[GtkTemplate (ui = "/com/moebots/Paletti/ui/scene-initial.ui")]
	public class InitialScene : EventBox, IScene, LinkBehavior {
		[GtkChild]
		private Label dropzone;

		private IControl controller;
		private INavigation navigation;
		private INotification notification;

		public InitialScene (INotification notification,
		                     INavigation navigation, IControl controller,
		                     bool is_first_run) {
			this.controller = controller;
			this.navigation = navigation;
			this.notification = notification;

			if (is_first_run) {
				this.notification.display (
					"Press <b>Ctrl+?</b> for a list of keyboard shortcuts.",
					NotificationType.INFO
				);
			}
		}

		public void on_load (string filename) throws Leptonica.Exception {
			var pix = new Leptonica.PIX.from_filename (filename);
			if (pix == null) {
				throw new Leptonica.Exception.UNSUPPORTED ("Could not read this image.");
			}
			var scene = new PosterizeScene (pix, notification, controller);
			var dimensions = Allocation ();
			dropzone.get_allocation (out dimensions);
			scene.posterize (controller.mono_switch.state, dimensions);
			navigation.next (scene);
		}

		public void on_shortcut (EventKey event) {
			if ((event.keyval == Key.s && event.state == ModifierType.CONTROL_MASK)
				|| (event.keyval == Key.c && event.state == ModifierType.CONTROL_MASK)
				|| (event.keyval == Key.e && event.state == ModifierType.CONTROL_MASK)) {
			notification.display ("First load an image into Paletti.");
			}
		}

		[GtkCallback]
		private bool on_enter_dropzone (EventCrossing event) {
			dropzone.set_state_flags (StateFlags.PRELIGHT, false);
			cursor_to_pointer ();
			return true;
		}

		[GtkCallback]
		private bool on_leave_dropzone (EventCrossing event) {
			dropzone.unset_state_flags (StateFlags.PRELIGHT);
			cursor_to_default ();
			return true;
		}
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/scene-posterize.ui")]
	public class PosterizeScene : EventBox, IScene, LinkBehavior {
		[GtkChild]
		private Image image;

		public Leptonica.PIX src;
		private IControl controller;
		private CachedPix cached_pix;
		private INotification notification;

		public PosterizeScene (Leptonica.PIX src, INotification notification,
		                       IControl controller) {
			this.src = src.clone ();
			this.notification = notification;
			this.controller = controller;
			this.show_all ();

			controller.colors_range.value_changed.connect (() => {
				var dimensions = Allocation ();
				image.get_allocation (out dimensions);
				this.posterize (controller.mono_switch.state, dimensions);
			});
			controller.mono_switch.state_set.connect ((is_black_white) => {
				var dimensions = Allocation ();
				image.get_allocation (out dimensions);
				this.posterize (is_black_white, dimensions);
				return false;
			});
		}

		public void on_load (string filename) throws Leptonica.Exception{
			var tmp = new Leptonica.PIX.from_filename (filename);
			if (tmp == null) {
				throw new Leptonica.Exception.UNSUPPORTED ("Could not read this image.");
			}
			src = tmp.clone ();
			var dimensions = Allocation ();
			image.get_allocation (out dimensions);
			posterize (controller.mono_switch.state, dimensions);
		}

		public void on_shortcut (EventKey event) {
			if (event.keyval == Key.s && event.state == ModifierType.CONTROL_MASK) {
				var dialog = new SaveFileDialog (notification, cached_pix);
				dialog.show ();
			} else if (event.keyval == Key.c && event.state == ModifierType.CONTROL_MASK) {
				Clipboard.get_for_display (
					get_window ().get_display (), Gdk.SELECTION_CLIPBOARD
				).set_image (image.pixbuf);
			} else if (event.keyval == Key.e && event.state == ModifierType.CONTROL_MASK) {
				controller.color_palette.export ();
			}
		}

		public void posterize (bool is_black_white, Allocation dimensions) {
			try {
				if (is_black_white) {
					cached_pix = new CachedPix (new BlackWhitePix (
						new PosterizedPix (src, (int) controller.colors_range.value)
					));
				} else {
					cached_pix = new CachedPix (new PosterizedPix (
						src, (int) controller.colors_range.value)
					);
				}
				controller.color_palette.colors = cached_pix.colors;

				var tmp = new Pixbuf.from_file (cached_pix.path);
				var ratio = (double) cached_pix.width / cached_pix.height;
				var target_width = (double) dimensions.width;
				var target_height = (double) dimensions.height;
				if (ratio >= 1) {
					target_width = (double) dimensions.height/cached_pix.height * cached_pix.width;
				} else {
					target_height = (double) dimensions.width/cached_pix.width * cached_pix.height;
				}
				var dest = new Pixbuf (Colorspace.RGB, false, 8, dimensions.width, dimensions.height);
				tmp.scale (
					dest, 0, 0, dimensions.width, dimensions.height,
					-(int) (target_width - dimensions.width) / 2,
					-(int) (target_height - dimensions.height) / 2,
					target_width/cached_pix.width, target_height/cached_pix.height,
					InterpType.BILINEAR
				);
				image.pixbuf = dest;
			} catch (Error e) {
				notification.display (e.message);
			}
		}

		[GtkCallback]
		private bool on_enter_image (EventCrossing event) {
			image.set_state_flags (StateFlags.PRELIGHT, false);
			cursor_to_pointer ();
			return true;
		}

		[GtkCallback]
		private bool on_leave_image (EventCrossing event) {
			image.unset_state_flags (StateFlags.PRELIGHT);
			cursor_to_default ();
			return true;
		}
	}
}
