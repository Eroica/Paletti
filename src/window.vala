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
		private Image image;

		[GtkChild]
		private Box box;

		[GtkChild]
		private Stack stack;

		[GtkChild]
		private Revealer revealer;

		private ColorPalette color_palette;

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

		[GtkCallback]
		private bool on_click (EventButton event) {
			// TODO File browser
			return true;
		}

		[GtkCallback]
		private bool on_mouse_scroll (EventScroll event) {
			if (event.direction == ScrollDirection.UP) {
				color_palette.increase_palette ();
			} else if (event.direction == ScrollDirection.DOWN) {
				color_palette.decrease_palette ();
			}
			return true;
		}

		[GtkCallback]
		private void on_drag_data_received (DragContext drag_context,
		                                    int x, int y, SelectionData data,
		                                    uint info, uint time) {
			try {
				var filename = Filename.from_uri (data.get_uris ()[0]);
				var posterized_image = new PosterizedImage.from_file (filename);
				color_palette.colors = new Colors (posterized_image.colors);

				var stack_dimensions = Allocation ();
				stack.get_allocation (out stack_dimensions);
				image.set_from_pixbuf (new Pixbuf.from_file_at_scale (
					filename,
					stack_dimensions.width,
					stack_dimensions.height,
					true
				));
				stack.set_visible_child_name("PreviewImage");
				header_bar.subtitle = filename;
			} catch (FileTypeError e) {
				show_notification ();
			} catch (Error e) {
				show_notification ();
			} finally {
				Gtk.drag_finish (drag_context, true, false, time);
			}
		}
	}

	[GtkTemplate (ui = "/com/moebots/paletti/ui/color_palette.ui")]
	public class ColorPalette : Box {
		private int min_colors;
		private int max_colors;
		private int current_count;
		public Colors colors { get; set; }

		public ColorPalette (int min_colors, int max_colors) {
			this.min_colors = min_colors;
			this.max_colors = max_colors;
			this.current_count = int.max(min_colors, DEFAULT_COLORS);

			for (int i=0; i < current_count; i++) {
				add (new ColorTile (this, i));
			}
			show_all ();
		}

		public void increase_palette () {
			if (current_count >= max_colors || colors == null) {
				return;
			}
			add (new ColorTile.with_color (this, current_count, colors[current_count]));
			current_count++;
			show_all ();
		}

		public void decrease_palette () {
			if (current_count <= min_colors || colors == null) {
				return;
			}
			get_children ().last ().foreach ((element) => remove (element));
			current_count--;
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

		public ColorTile (ColorPalette color_palette, int index) {
			set_name (@"Tile$index");
			color_palette.notify["colors"].connect ((s, p) => {
				color = color_palette.colors[index];
			});
		}

		public ColorTile.with_color (ColorPalette color_palette, int index, RGB color) {
			this (color_palette, index);
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

