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

	// Simple check: if a cached image exists, then Paletti has been used before
	private bool is_first_run () {
		return !File.new_build_filename (Path.build_filename (
			Environment.get_user_cache_dir (),
			"Paletti", "cache.png"
		)).query_exists ();
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/window.ui")]
	public class Window : ApplicationWindow {
		[GtkChild]
		private HeaderBar header_bar;

		[GtkChild]
		private Adjustment colors_range;

		[GtkChild]
		private Switch mono_switch;

		[GtkChild]
		private Box box;

		[GtkChild]
		private Stack stack;

		[GtkChild]
		private Overlay overlay;

		private IPosterizedImage image;

		private ColorPalette color_palette;

		private Notification notification;

		public Window (Gtk.Application app) {
			Object (application: app);
			drag_dest_set (this, DestDefaults.ALL, targets, DragAction.COPY);
			color_palette = new ColorPalette (DEFAULT_COLORS, MAX_COLORS);
			box.add (color_palette);
			notification = new Notification ();
			overlay.add_overlay (notification);
			image = new NullImage ();
			stack.add_named (image, "NULLIMAGE");
			show_all();

			if (is_first_run ()) {
				notification.display ("Press <b>Ctrl+?</b> for a list of keyboard shortcuts.", NotificationType.INFO);
			}
		}

		private void load_file (string filename) {
			try {
				image.load_from (filename);
			} catch (Leptonica.Exception e) {
				if (e is Leptonica.Exception.UNINITIALIZED) {
					stack.remove (image);
					image = new PosterizedImage.from_file (filename, image.is_black_white);
					image.show ();
					stack.add_named (image, "PreviewImage");
					stack.visible_child_name = "PreviewImage";
				} else {
					notification.display (e.message);
				}
			}
			var colors = image.posterize ((int) colors_range.value);
			color_palette.set_tile_colors (colors);
			header_bar.subtitle = filename;
		}

		private void show_open_dialog () {
			var dialog = new OpenFileDialog ();
			dialog.response.connect ((dialog, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					var file_dialog = dialog as FileChooserDialog;
					load_file (Filename.from_uri (file_dialog.get_file ().get_uri ()));
				}
				dialog.destroy ();
			});
			dialog.show ();
		}

		[GtkCallback]
		private bool on_key_release (EventKey event) {
			try {
				if (event.keyval == Key.o && event.state == ModifierType.CONTROL_MASK) {
					show_open_dialog ();
				} else if (event.keyval == Key.s && event.state == ModifierType.CONTROL_MASK) {
					image.save ();
				} else if (event.keyval == Key.c && event.state == ModifierType.CONTROL_MASK) {
					image.copy ();
				} else if (event.keyval == Key.x) {
					mono_switch.activate ();
				}
			} catch (Leptonica.Exception e) {
				notification.display (e.message);
			}
			return true;
		}

		[GtkCallback]
		private bool on_click (EventButton event) {
			show_open_dialog ();
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
			try {
				var colors = image.posterize ((int) colors_range.value);
				color_palette.set_tile_colors (colors);
			} catch (Leptonica.Exception e) {
				if (!(e is Leptonica.Exception.UNINITIALIZED)) {
					notification.display (e.message);
				}
			}
		}

		[GtkCallback]
		private void on_drag_data_received (DragContext drag_context,
		                                    int x, int y, SelectionData data,
		                                    uint info, uint time) {
			load_file (Filename.from_uri (data.get_uris ()[0]));
			Gtk.drag_finish (drag_context, true, false, time);
		}

		[GtkCallback]
		private bool on_mono_set (bool state) {
			image.is_black_white = state;
			try {
				var colors = image.posterize ((int) colors_range.value);
				color_palette.set_tile_colors (colors);
			} catch (Leptonica.Exception e) {
				if (!(e is Leptonica.Exception.UNINITIALIZED)) {
					notification.display (e.message);
				}
			}
			return false;
		}
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/color_palette.ui")]
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

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/color_tile.ui")]
	private class ColorTile : EventBox {
		private RGB? _color;
		public RGB? color {
			get { return _color; }
			set {
				_color = value;
				tooltip_text = _color.to_string ();
				try {
					var css = new CssProvider ();
					css.load_from_data (@".$(name) { background-color: rgba($(color.r), $(color.g), $(color.b), 1.0); }");
					get_style_context ().add_class (name);
					get_style_context ().add_provider (css, STYLE_PROVIDER_PRIORITY_USER);
				} catch (Error e) {
					stderr.printf ("%s\n", e.message);
				}
			}
		}

		public ColorTile (int index) {
			name = @"Tile$index";
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

