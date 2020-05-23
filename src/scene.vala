using Gdk;
using Gtk;

namespace Paletti {
	[GtkTemplate (ui = "/com/moebots/Paletti/ui/scene-initial.ui")]
	public class InitialScene : EventBox, IScene, LinkBehavior {
		[GtkChild]
		private Box dropzone;

		private IControl controller;
		private INavigation navigation;
		private INotification notification;
		private ulong adjust_palette_signal;

		public InitialScene (INotification notification,
		                     INavigation navigation, IControl controller,
		                     bool is_first_run) {
			this.controller = controller;
			this.navigation = navigation;
			this.notification = notification;
			adjust_palette_signal = controller.get_colors_range ().value_changed.connect (() => {
				controller.color_palette.adjust_tiles_to ((int) controller.get_colors_range ().value);
			});
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
			scene.posterize (controller.get_mono_switch ().state);
			controller.get_colors_range ().disconnect (adjust_palette_signal);
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
		private int64 time;
		private IControl controller;
		private CachedPix cached_pix;
		private INotification notification;

		public PosterizeScene (Leptonica.PIX src, INotification notification,
		                       IControl controller) {
			this.src = src.clone ();
			this.notification = notification;
			this.controller = controller;
			this.show_all ();

			controller.get_colors_range ().value_changed.connect (() => {
				this.posterize (controller.get_mono_switch ().state);
			});
			controller.get_mono_switch ().state_set.connect ((is_black_white) => {
				this.posterize (is_black_white);
				return false;
			});
		}

		public void on_load (string filename) throws Leptonica.Exception{
			var tmp = new Leptonica.PIX.from_filename (filename);
			if (tmp == null) {
				throw new Leptonica.Exception.UNSUPPORTED ("Could not read this image.");
			}
			src = tmp.clone ();
			posterize (controller.get_mono_switch ().state);
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

		public void posterize (bool is_black_white) {
			// Sliding the color slider rapidly will make many calls to this
			// method. To improve performance, do not call posterize on every
			// tick. It is only called if at least 100 milliseconds have passed
			// between each call.
			time = get_monotonic_time ();
			Timeout.add (100, () => {
				var delta = get_monotonic_time ();
				// Difference in MICROseconds
				if (delta - time >= 100000) {
					debounced_posterize.begin (is_black_white);
				}
				return false;
			});
		}

		private async void debounced_posterize (bool is_black_white) {
			var result = new Thread<bool> (null, () => {
				try {
					lock (cached_pix) {
						if (is_black_white) {
							cached_pix = new CachedPix (new BlackWhitePix (
								new PosterizedPix (src, (int) controller.get_colors_range ().value)
							));
						} else {
							cached_pix = new CachedPix (new PosterizedPix (
								src, (int) controller.get_colors_range ().value)
							);
						}
					}
				} catch {
					return false;
				}
				Idle.add (debounced_posterize.callback);
				return true;
			});
			yield;

			if (!result.join ()) {
				notification.display ("Could not run quantization on this image.");
			} else {
				try {
					var tmp = yield new Pixbuf.from_stream_async (
						File.new_for_path (cached_pix.path).read (), null
					);
					var target_width = (double) image.get_allocated_width ();
					var target_height = (double) image.get_allocated_height ();
					if (target_width / target_height >= 1) {
						target_height = target_width/cached_pix.width * cached_pix.height;
					} else {
						target_width = target_height/cached_pix.height * cached_pix.width;
					}
					var dest = new Pixbuf (Colorspace.RGB, false, 8, image.get_allocated_width (), image.get_allocated_height ());
					tmp.scale (
						dest, 0, 0, image.get_allocated_width (), image.get_allocated_height (),
						-(int) (target_width - image.get_allocated_width ()) / 2,
						-(int) (target_height - image.get_allocated_height ()) / 2,
						target_width/cached_pix.width, target_height/cached_pix.height,
						InterpType.BILINEAR
					);
					image.pixbuf = dest;
					controller.color_palette.colors = cached_pix.colors;
				} catch (Error e) {
					notification.display (e.message);
				}
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
