namespace Paletti {
	public class RGB : Object, Gdk.Paintable {
		public int r;
		public int g;
		public int b;
		public string hex {
			owned get { return this.to_string (); }
		}

		public Gdk.Paintable paint {
			get { return this; }
		}

		public RGB (int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public RGB.from_hex (string hex_string) {
			var hex = int.parse (hex_string);
			this.r = (hex >> 16) & 0xFF;
			this.g = (hex >> 8) & 0xFF;
			this.b = hex & 0xFF;
		}

		public string to_string () {
			return "#%02x%02x%02x".printf (r, g, b);
		}

		private float[] border_size = {2f, 2f, 2f, 2f};
		private Gdk.RGBA[] border_color = {{0f, 0f, 0f, 0.15f}, {0f, 0f, 0f, 0.15f}, {0f, 0f, 0f, 0.15f}, {0f, 0f, 0f, 0.15f}};

		public void snapshot (Gdk.Snapshot snapshot, double width, double height) {
			var gtk_snapshot = (Gtk.Snapshot) snapshot;
			gtk_snapshot.push_rounded_clip (
				(!) Gsk.RoundedRect ().init_from_rect ({{0f, 0f}, {(float) width, (float) height}}, 5f)
			);
			gtk_snapshot.append_color ({r/255f, g/255f, b/255f, 1f}, {{0f, 0f}, {(float) width, (float) height}});
			gtk_snapshot.append_border (
				(!) Gsk.RoundedRect ().init_from_rect ({{0f, 0f}, {(float) width, (float) height}}, 5f),
				border_size,
				border_color
			);
			gtk_snapshot.pop ();
		}
	}
}
