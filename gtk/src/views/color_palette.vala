using Gdk;
using Gtk;

namespace Paletti {
	[GtkTemplate (ui = "/app/paletti/gtk/ui/views-color-palette.ui")]
	public class ColorPalette : Adw.Bin {
		[GtkChild]
		private unowned ListView colors_list;

		private unowned ViewModel view_model;
		private GLib.ListStore store = new GLib.ListStore (typeof (RGB));

		public ColorPalette (ViewModel view_model) {
			this.view_model = view_model;

			this.colors_list.factory = new BuilderListItemFactory.from_resource (null, "/app/paletti/gtk/ui/color-tile-placeholder.ui");
			this.colors_list.model = new NoSelection (store);
			this.colors_list.activate.connect ((position) => {
				if (this.view_model.pix != null) {
					get_clipboard ().set_text (((!) this.view_model.pix).colors[position].to_string ());
				}
			});

			this.view_model.notify["pix"].connect (on_change_image);
			this.view_model.notify["pix"].connect (() => {
				this.store.remove_all ();
				foreach (RGB color in ((!) this.view_model.pix).colors) {
					this.store.append (color);
				}
			});

			for (short i = 0; i < DEFAULT_COLORS; i++) {
				this.store.append (new RGB (255, 255, 255));
			}
			this.view_model.notify["count"].connect (on_change_count);
		}

		private void on_change_image () {
			view_model.notify["count"].disconnect (on_change_count);
		}

		private void on_change_count () {
			store.remove_all ();
			for (short i = 0; i < view_model.count; i++) {
				store.append (new RGB (255, 255, 255));
			}
		}
	}
}
