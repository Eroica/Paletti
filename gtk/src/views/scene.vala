using Gdk;
using Gtk;

namespace Paletti {
	public interface IScene : Object {
		public abstract void on_shortcut (uint keyval, uint keycode, ModifierType state);
	}

	[GtkTemplate (ui = "/app/paletti/gtk/ui/scene-initial.ui")]
	public class InitialScene : Box, IScene {
		private INotification notification;

		public InitialScene (INotification notification, bool is_first_run) {
			this.notification = notification;
			if (is_first_run) {
				this.notification.display (
					"Press <b>Ctrl+?</b> for a list of keyboard shortcuts.",
					NotificationType.INFO
				);
			}
		}

		public void on_shortcut (uint keyval, uint keycode, ModifierType state) {
			if ((keyval == Key.s && (state & ModifierType.CONTROL_MASK) > 0)
				|| (keyval == Key.c && (state & ModifierType.CONTROL_MASK) > 0)
				|| (keyval == Key.e && (state & ModifierType.CONTROL_MASK) > 0)) {
				notification.display ("First load an image into Paletti.");
			}
		}
	}

	[GtkTemplate (ui = "/app/paletti/gtk/ui/scene-image.ui")]
	public class ImageScene : Box, IScene {
		[GtkChild]
		private unowned Picture image;

		private IControl control;
		private INotification notification;
		private ViewModel view_model;

		public ImageScene (IControl control, INotification notification, ViewModel view_model) {
			this.control = control;
			this.notification = notification;
			this.view_model = view_model;
			this.view_model.notify["pix"].connect(() => on_load_image ());
		}

		public void on_shortcut (uint keyval, uint keycode, ModifierType state) {
			if (keyval == Key.s && (state & ModifierType.CONTROL_MASK) > 0) {
				control.on_save_dialog ();
			} else if (keyval == Key.c && (state & ModifierType.CONTROL_MASK) > 0) {
				var paint_val = Value (typeof (Texture));
				paint_val.set_object (image.paintable);
				get_clipboard ().set_value (paint_val);
			}
		}

		private void on_load_image () {
			load_image.begin ();
		}

		public async void load_image () {
			try {
				var parent = (!) get_parent ();
				var width = parent.get_width ();
				var height = parent.get_height ();
				unowned var pix = ((!) view_model.pix);

				var tmp = yield new Pixbuf.from_stream_async (
					File.new_for_path (pix.path).read (), null
				);

				var dest = new Pixbuf (Colorspace.RGB, false, 8, width, height);
				tmp.scale (
					dest,
					0, 0,
					width, height,
					0, 0,
					(float) width/pix.width, (float) height/pix.height,
					InterpType.BILINEAR
				);
				image.set_pixbuf (dest);
			} catch (Error e) {
				notification.display (e.message);
			}
		}
	}
}
