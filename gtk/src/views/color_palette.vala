using Gdk;
using Gtk;

namespace Paletti {
	[GtkTemplate (ui = "/app/paletti/gtk/ui/views-color-palette.ui")]
	public class ColorPalette : Widget {
		private Widget[] widgets = {};
		private unowned ViewModel view_model;

		public ColorPalette (ViewModel view_model) {
			this.view_model = view_model;

			for (short i = 0; i < DEFAULT_COLORS; i++) {
				var l = new Label ("");
				widgets += l;
				l.set_parent (this);
			}
		}

		~ColorPalette () {
			foreach (var child in widgets) {
				child.unparent ();
			}
		}

		public override void size_allocate (int _width, int _height, int _baseline) {
			for (var i = 0; i < widgets.length; i++) {
				Gtk.Requisition size;
				Gtk.Requisition natural_size;
				widgets[i].get_preferred_size (out size, out natural_size);

				int width = natural_size.width;
				int height = natural_size.height;

				var allocation = Gtk.Allocation () {
					x = get_width () / widgets.length * i,
					y = 0,
					height = 80,
					width = get_width () / widgets.length
				};

				widgets[i].allocate_size (allocation, -1);
			}
		}

		private void on_change_image () {
			view_model.notify["count"].disconnect (on_change_count);
		}

		private void on_change_count () {
			// store.remove_all ();
			// for (short i = 0; i < view_model.count; i++) {
			// 	this.store.append (new RGB (50, 50, 50));
			// 	// if (Adw.StyleManager.get_default ().dark) {
			// 	// 	this.store.append (new RGB (50, 50, 50));
			// 	// } else {
			// 	// 	this.store.append (new RGB (255, 255, 255));
			// 	// }
			// }
		}
	}
}
