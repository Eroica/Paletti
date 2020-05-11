using Gdk;
using Gtk;

namespace Paletti {
	private const int DEFAULT_COLORS = 6;
	private const int MAX_COLORS = 32;

	enum Target {
		FILE
	}

	private const TargetEntry[] targets = {
		{"text/uri-list", 0, Target.FILE}
	};

	// Simple check: if a cached image exists, then Paletti has been used before
	private bool is_first_run () {
		return !File.new_build_filename (Path.build_filename (
			Environment.get_user_cache_dir (),
			"Paletti", "cache.png"
		)).query_exists ();
	}

	public interface LinkBehavior : Widget {
		public void cursor_to_pointer () {
			set_state_flags (StateFlags.PRELIGHT, false);
			var window = get_window ();
			window.set_cursor (new Cursor.from_name (window.get_display (), "pointer"));
		}

		public void cursor_to_default () {
			unset_state_flags (StateFlags.PRELIGHT);
			var window = get_window ();
			window.set_cursor (new Cursor.from_name (window.get_display (), "default"));
		}
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

		private PosterizedImage image;
		private Notification notification;
		private ColorPalette color_palette;

		public Window (Gtk.Application app) {
			Object (application: app);
			drag_dest_set (this, DestDefaults.ALL, targets, DragAction.COPY);
			this.color_palette = new ColorPalette (DEFAULT_COLORS, MAX_COLORS);
			this.box.add (color_palette);
			this.notification = new Notification ();
			this.overlay.add_overlay (notification);
			this.image = new PosterizedImage (this.color_palette, this.notification);
			this.stack.add_named (image, "PreviewImage");
			show_all();

			this.image.notify["filename"].connect ((s, p) => {
				this.header_bar.subtitle = this.image.filename;
				this.stack.visible_child_name = "PreviewImage";
			});

			if (is_first_run ()) {
				notification.display (
					"Press <b>Ctrl+?</b> for a list of keyboard shortcuts.",
					NotificationType.INFO
				);
			}
		}

		private void show_open_dialog () {
			var dialog = new OpenFileDialog ();
			dialog.response.connect ((dialog, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					try {
						var file_dialog = dialog as FileChooserDialog;
						image.on_load (file_dialog.get_file ().get_path ());
						image.posterize ((int) colors_range.value, mono_switch.state);
					} catch (Leptonica.Exception e) {
						notification.display (e.message);
					}
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
					image.on_save ();
				} else if (event.keyval == Key.c && event.state == ModifierType.CONTROL_MASK) {
					image.on_copy ();
				} else if (event.keyval == Key.e && event.state == ModifierType.CONTROL_MASK) {
					color_palette.export ();
				} else if (event.keyval == Key.x) {
					mono_switch.activate ();
				}
			} catch (Exception e) {
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
			color_palette.adjust_tiles_to ((uint) colors_range.value);
			image.posterize ((int) colors_range.value, mono_switch.state);
		}

		[GtkCallback]
		private void on_drag_data_received (DragContext drag_context,
		                                    int x, int y, SelectionData data,
		                                    uint info, uint time) {
			try {
				image.on_load (Filename.from_uri (data.get_uris ()[0]));
				image.posterize ((int) colors_range.value, mono_switch.state);
			} catch (Error e) {
				notification.display (e.message);
			} finally {
				Gtk.drag_finish (drag_context, true, false, time);
			}
		}

		[GtkCallback]
		private bool on_mono_set (bool state) {
			image.posterize ((int) colors_range.value, state);
			return false;
		}
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/color_palette.ui")]
	public class ColorPalette : Box, IColorPalette {
		private Colors? _colors;
		public Colors? colors {
			get { return _colors; }
			set {
				_colors = value;
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

		public void adjust_tiles_to (uint size) {
			var child_count = get_children ().length ();
			if (child_count > size) {
				for (uint i=child_count; i > size; i--) {
					remove (get_children ().nth_data (i - 1));
				}
			} else if (child_count < size) {
				for (uint i=child_count; i < size; i++) {
					var tile = new ColorTile (i);
					add (tile);
					tile.show ();
				}
			}
		}

		public void export () throws Exception {
			if (colors == null) {
				throw new Exception.UNINITIALIZED ("First load an image into Paletti.");
			}
			var dialog = new SaveFileDialog ();
			dialog.response.connect ((dialog, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					try {
						var file_dialog = dialog as FileChooserDialog;
						var window = new ExportColorPaletteWindow (
							colors, get_allocated_width (), get_allocated_height ()
						);
						window.export (file_dialog.get_file ().get_path ());
					} catch (Error e) {
						stderr.printf (e.message);
					}
				}
				dialog.destroy ();
			});
			dialog.show ();
		}
	}

	public class ExportColorPaletteWindow : OffscreenWindow {
		public ExportColorPaletteWindow (Colors colors, int width, int height) {
			set_size_request (width, height);
			var palette = new ColorPalette (DEFAULT_COLORS, MAX_COLORS);
			palette.colors = colors;
			palette.show_all ();
			add (palette);
			show_all ();
			var context = new Cairo.Context (get_surface ());
			draw (context);
		}

		public void export (string filename) throws Error {
			get_pixbuf ().save (filename, "png");
		}
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/color_tile.ui")]
	private class ColorTile : EventBox, LinkBehavior {
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

		public ColorTile (uint index) {
			name = @"Tile$index";
		}

		public ColorTile.with_color (uint index, RGB color) {
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
				cursor_to_pointer ();
			}
			return true;
		}

		[GtkCallback]
		private bool on_leave (EventCrossing event) {
			if (color != null) {
				cursor_to_default ();
			}
			return true;
		}
	}
}
