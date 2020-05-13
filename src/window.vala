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
			var window = get_window ();
			window.set_cursor (new Cursor.from_name (window.get_display (), "pointer"));
		}

		public void cursor_to_default () {
			var window = get_window ();
			window.set_cursor (new Cursor.from_name (window.get_display (), "default"));
		}
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/window.ui")]
	public class Window : ApplicationWindow, LinkBehavior {
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
		private Label dropzone;

		[GtkChild]
		private Overlay overlay;

		private IViewModel view_model;
		private PosterizedImage image;
		private Notification notification;
		private ColorPalette color_palette;

		public Window (Gtk.Application app, IViewModel view_model) {
			Object (application: app);
			drag_dest_set (this, DestDefaults.ALL, targets, DragAction.COPY);
			this.view_model = view_model;
			this.color_palette = new ColorPalette (view_model);
			this.box.add (color_palette);
			this.notification = new Notification ();
			this.overlay.add_overlay (notification);
			this.image = new PosterizedImage (view_model);
			this.stack.add_named (image, "PreviewImage");
			this.show_all();

			if (is_first_run ()) {
				this.notification.display (
					"Press <b>Ctrl+?</b> for a list of keyboard shortcuts.",
					NotificationType.INFO
				);
			}
		}

		private void load (string filename) {
			try {
				view_model.on_load (filename, (int) colors_range.value);
				stack.set_visible_child_name ("PreviewImage");
				header_bar.subtitle = filename;
			} catch (Leptonica.Exception e) {
				notification.display (e.message);
			}
		}

		private void show_open_dialog () {
			var dialog = new OpenFileDialog ();
			dialog.response.connect ((dialog, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					var file_dialog = dialog as FileChooserDialog;
					load (file_dialog.get_file ().get_path ());
				}
				dialog.destroy ();
			});
			dialog.show ();
		}

		[GtkCallback]
		private bool on_key_release (EventKey event) {
			try {
				view_model.on_shortcut (event);
				if (event.keyval == Key.o && event.state == ModifierType.CONTROL_MASK) {
					show_open_dialog ();
				} else if (event.keyval == Key.s && event.state == ModifierType.CONTROL_MASK) {
					var dialog = new SaveFileDialog (notification, view_model.pix);
					dialog.show ();
				} else if (event.keyval == Key.c && event.state == ModifierType.CONTROL_MASK) {
					Clipboard.get_for_display (
						get_window ().get_display (), Gdk.SELECTION_CLIPBOARD
					).set_image (image.pixbuf);
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
		private void on_drag_data_received (DragContext drag_context,
		                                    int x, int y, SelectionData data,
		                                    uint info, uint time) {
			try {
				load (Filename.from_uri (data.get_uris ()[0]));
			} catch (Error e) {
				notification.display (e.message);
			} finally {
				Gtk.drag_finish (drag_context, true, false, time);
			}
		}

		[GtkCallback]
		private bool on_mono_set (bool state) {
			try {
				view_model.on_set_black_white (state);
			} catch (Leptonica.Exception e) {
				notification.display (e.message);
			}
			return false;
		}

		[GtkCallback]
		private void on_colors_range_value_changed () {
			color_palette.adjust_tiles_to ((uint) colors_range.value);
			try {
				view_model.on_colors_range_change ((int) colors_range.value);
			} catch (Leptonica.Exception e) {
				notification.display (e.message);
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

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/color_palette.ui")]
	public class ColorPalette : Box {
		private Colors _colors;
		private Colors colors {
			get { return _colors; }
			set {
				_colors = value;
				adjust_tiles_to (_colors.size);
				int i = 0;
				get_children ().foreach ((it) => {
					var tile = it as ColorTile;
					if (tile != null) {
						tile.color = colors[i++];
					}
				});
			}
		}

		public ColorPalette (IViewModel view_model) {
			for (int i=0; i < DEFAULT_COLORS; i++) {
				this.add (new ColorTile (i));
			}
			this.show_all ();
			view_model.notify["pix"].connect (() => {
				this.colors = view_model.pix.colors;
			});
		}

		public ColorPalette.with_colors (Colors colors) {
			this.colors = colors;
			this.show_all ();
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
			var dialog = new ExportPaletteDialog (
				new ExportColorPaletteWindow (
					colors, get_allocated_width (), get_allocated_height ()
				)
			);
			dialog.show ();
		}
	}

	public class ExportColorPaletteWindow : OffscreenWindow {
		public ExportColorPaletteWindow (Colors colors, int width, int height) {
			this.set_size_request (width, height);
			var palette = new ColorPalette.with_colors (colors);
			this.add (palette);
			this.show_all ();
			var context = new Cairo.Context (get_surface ());
			this.draw (context);
		}

		public void export (string filename) throws Error {
			get_pixbuf ().save (@"$filename.png", "png");
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
				set_state_flags (StateFlags.PRELIGHT, false);
				cursor_to_pointer ();
			}
			return true;
		}

		[GtkCallback]
		private bool on_leave (EventCrossing event) {
			if (color != null) {
				unset_state_flags (StateFlags.PRELIGHT);
				cursor_to_default ();
			}
			return true;
		}
	}
}
