using Gdk;
using Gtk;

namespace Paletti {
	enum Target {
		FILE
	}

	private const Gtk.TargetEntry[] targets = {
		{"text/uri-list", 0, Target.FILE}
	};

	private const int MAX_COLORS = 32;
	private const int DEFAULT_COLORS = 6;

	[GtkTemplate (ui = "/com/moebots/paletti/ui/window.ui")]
	public class Window : ApplicationWindow {
		[GtkChild]
		private HeaderBar header_bar;

		[GtkChild]
		private Adjustment colors_range;

		[GtkChild]
		private Button copy_button;

		[GtkChild]
		private Image image;

		[GtkChild]
		private Box box;

		[GtkChild]
		private Stack stack;

		[GtkChild]
		private Revealer revealer;

		private ColorPalette color_palette;

		private IPosterizedImage pix = new PosterizedImage ();

		public Window (Gtk.Application app) {
			Object (application: app);
			drag_dest_set (this, DestDefaults.ALL, targets, DragAction.COPY);
			color_palette = new ColorPalette (DEFAULT_COLORS, MAX_COLORS);
			box.add (color_palette);
			show_all();
		}

		private void show_notification () {
			revealer.reveal_child = true;
			Timeout.add (3600, () => {
				revealer.reveal_child = false;
				return false;
			});
		}

		private void load_image (string filename) {
			var stack_dimensions = Allocation ();
			stack.get_allocation (out stack_dimensions);
			try {
				image.set_from_pixbuf (new Pixbuf.from_file_at_scale (
					filename,
					stack_dimensions.width,
					stack_dimensions.height,
					true
				));
			} catch (Error e) {
				show_notification ();
			}
		}

		[GtkCallback]
		private bool on_click (EventButton event) {
			// TODO File browser
			return true;
		}

		[GtkCallback]
		private bool on_mouse_scroll (EventScroll event) {
			if (event.direction == ScrollDirection.UP) {
				colors_range.value++;
			} else if (event.direction == ScrollDirection.DOWN) {
				colors_range.value--;
			}
			return true;
		}

		[GtkCallback]
		private void on_colors_range_value_changed () {
			color_palette.adjust_tiles_to ((int) colors_range.value);
			var colors = pix.posterize ((int) colors_range.value);
			if (colors != null) {
				load_image (load_cached_image ());
				color_palette.set_tile_colors (colors);
			}
		}

		[GtkCallback]
		private void on_drag_data_received (DragContext drag_context,
		                                    int x, int y, SelectionData data,
		                                    uint info, uint time) {
			try {
				var filename = Filename.from_uri (data.get_uris ()[0]);
				pix.load_from_file (filename);
				var colors = pix.posterize ((int) colors_range.value);
				load_image (load_cached_image ());
				color_palette.set_tile_colors (colors);
				stack.set_visible_child_name("PreviewImage");
				copy_button.sensitive = true;
				header_bar.subtitle = filename;
			} catch (Leptonica.Exception e) {
				show_notification ();
			} catch (Error e) {
				show_notification ();
			} finally {
				Gtk.drag_finish (drag_context, true, false, time);
			}
		}

		[GtkCallback]
		private bool on_mono_set (bool state) {
			pix.is_black_white = state;
			var colors = pix.posterize ((int) colors_range.value);
			if (colors != null) {
				load_image (load_cached_image ());
				color_palette.set_tile_colors (colors);
			}
			return false;
		}

		[GtkCallback]
		private void on_copy_click () {
			Clipboard.get_for_display (
				get_window ().get_display (),
				Gdk.SELECTION_CLIPBOARD
			).set_image (image.pixbuf);
		}
	}

	[GtkTemplate (ui = "/com/moebots/paletti/ui/color_palette.ui")]
	public class ColorPalette : Box {
		private int min_colors;
		private int max_colors;
		private int current_count;

		public ColorPalette (int min_colors, int max_colors) {
			this.min_colors = min_colors;
			this.max_colors = max_colors;
			this.current_count = int.max (min_colors, DEFAULT_COLORS);

			for (int i=0; i < current_count; i++) {
				add (new ColorTile (i));
			}

			show_all ();
		}

		public void adjust_tiles_to (int size) {
			var child_count = (int) get_children ().length ();
			if (child_count > size) {
				for (int i=child_count; i > size; i--) {
					remove (get_children ().nth_data (i - 1));
				}
			} else if (child_count < size) {
				for (int i=child_count; i < size; i++) {
					var tile = new ColorTile (i);
					add (tile);
					tile.show ();
				}
			}
		}

		public void set_tile_colors (Colors colors) {
			if (colors.size < get_children ().length ()) {
				adjust_tiles_to (colors.size);
			}

			int i = 0;
			get_children ().foreach ((it) => {
				var tile = it as ColorTile;
				if (tile != null) {
					tile.color = colors[i];
				}
				i++;
			});
		}
	}

	[GtkTemplate (ui = "/com/moebots/paletti/ui/color_tile.ui")]
	private class ColorTile : EventBox {
		private RGB? _color;
		public RGB? color {
			get { return _color; }
			set {
				_color = value;
				try {
					var css = new CssProvider ();
					css.load_from_data (@".$(get_name ()) { background-color: rgba($(color.r), $(color.g), $(color.b), 1.0); }");
					get_style_context ().add_class (get_name ());
					get_style_context ().add_provider (css, STYLE_PROVIDER_PRIORITY_USER);
				} catch (Error e) {
					stderr.printf ("%s\n", e.message);
				}
			}
		}

		public ColorTile (int index) {
			set_name (@"Tile$index");
		}

		public ColorTile.with_color (int index, RGB color) {
			this (index);
			this.color = color;
		}

		[GtkCallback]
		private bool on_click (EventButton event) {
			if (color != null) {
				var clipboard = Clipboard.get_for_display (
					get_window ().get_display (),
					Gdk.SELECTION_CLIPBOARD
				);
				clipboard.set_text (color.to_string (), -1);
			}
			return true;
		}

		[GtkCallback]
		private bool on_enter (EventCrossing event) {
			if (color != null) {
				set_state_flags (StateFlags.PRELIGHT, false);
				var window = get_window ();
				window.set_cursor (new Cursor.from_name (window.get_display (), "pointer"));
			}
			return true;
		}

		[GtkCallback]
		private bool on_leave (EventCrossing event) {
			if (color != null) {
				unset_state_flags (StateFlags.PRELIGHT);
				var window = get_window ();
				window.set_cursor (new Cursor.from_name (window.get_display (), "default"));
			}
			return true;
		}
	}
}

