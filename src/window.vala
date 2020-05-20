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

	public interface IControl : Object {
		public abstract ColorPalette color_palette { get; protected set; }

		public abstract Switch get_mono_switch ();
		public abstract Adjustment get_colors_range ();
	}

	public interface IScene : Widget {
		public abstract void on_load (string filename) throws Leptonica.Exception;
		public abstract void on_shortcut (EventKey event);
	}

	public interface INavigation : Object {
		public abstract void next (IScene scene);
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/window.ui")]
	public class Window : ApplicationWindow, IControl, LinkBehavior, INavigation {
		[GtkChild]
		private Adjustment colors_range;

		[GtkChild]
		private Switch mono_switch;

		[GtkChild]
		private HeaderBar header_bar;

		[GtkChild]
		private Box box;

		[GtkChild]
		private Overlay overlay;

		[GtkChild]
		private Stack stack;

		public ColorPalette color_palette { get; protected set; }
		private IScene scene;
		private Notification notification;

		public Window (Gtk.Application app, bool is_first_run) {
			Object (application: app);
			drag_dest_set (this, DestDefaults.ALL, targets, DragAction.COPY);
			this.notification = new Notification ();
			this.overlay.add_overlay (notification);
			this.color_palette = new ColorPalette ();
			this.box.add (color_palette);
			this.scene = new InitialScene (notification, this, this, is_first_run);
			this.stack.add_named (scene, "Dropzone");
			this.show_all();
		}

		public Adjustment get_colors_range () {
			return colors_range;
		}

		public Switch get_mono_switch () {
			return mono_switch;
		}

		public void next (IScene scene) {
			stack.add_named (scene, "PosterizedImage");
			stack.set_visible_child_name ("PosterizedImage");
			this.scene = scene;
		}

		public void open_file_dialog () {
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

		private void load (string filename) {
			try {
				scene.on_load (filename);
				header_bar.subtitle = filename;
			} catch (Error e) {
				notification.display (e.message);
			}
		}

		[GtkCallback]
		private bool on_click (EventButton event) {
			open_file_dialog ();
			return true;
		}

		[GtkCallback]
		private bool on_key_release (EventKey event) {
			if (event.keyval == Key.o && event.state == ModifierType.CONTROL_MASK) {
				open_file_dialog ();
			} else if (event.keyval == Key.x) {
				mono_switch.activate ();
			} else if (event.keyval == Key.F1) {
				AboutDialog (this);
			} else {
				scene.on_shortcut (event);
			}
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
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/color_palette.ui")]
	public class ColorPalette : Box {
		public Colors colors {
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
		private Colors _colors;

		public ColorPalette () {
			for (int i=0; i < DEFAULT_COLORS; i++) {
				this.add (new ColorTile (i));
			}
			this.show_all ();
		}

		public ColorPalette.with_colors (Colors colors) {
			this.colors = colors;
			this.show_all ();
		}

		public void export () {
			var dialog = new ExportPaletteDialog (
				new ExportColorPaletteWindow (
					colors, get_allocated_width (), get_allocated_height ()
				)
			);
			dialog.show ();
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
					css.load_from_data (@".$(name) { background-color: $color; }", -1);
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
