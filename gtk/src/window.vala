using Gdk;
using Gtk;

namespace Paletti {
	private const int DEFAULT_COLORS = 6;
	private const int MAX_COLORS = 32;

	public interface IControl : Object {
		public abstract void on_save_dialog ();
	}

	[GtkTemplate (ui = "/app/paletti/gtk/ui/window.ui")]
	public class Window : ApplicationWindow, IControl {
		[GtkChild]
		private unowned Adjustment colors_range;

		[GtkChild]
		private unowned Switch mono_switch;

		[GtkChild]
		private unowned Overlay overlay;

		[GtkChild]
		private unowned Stack stack;

		[GtkChild]
		private unowned ListView colors_list;

		private unowned ViewModel view_model;
		private IScene scene;
		private Notification notification;
		private GLib.ListStore store = new GLib.ListStore (typeof (RGB));

		public Window (Gtk.Application app, ViewModel view_model, bool is_first_run) {
			Object (application: app, title: "Paletti");
			X11.Display.set_program_class((!) Display.get_default (), "Paletti");
			this.view_model = view_model;

			this.notification = new Notification ();
			this.overlay.add_overlay (notification);

			this.scene = new InitialScene (this.notification, is_first_run);
			this.stack.add_named ((Box) this.scene, "Dropzone");
			this.stack.set_visible_child_name ("Dropzone");

			var drop_target = new Gtk.DropTarget (typeof(File), Gdk.DragAction.COPY);
			drop_target.on_drop.connect ((value, x, y) => {
				try {
					var f = (File) value;
					this.view_model.load (f.get_path () ?? "");
				} catch (Leptonica.Exception e) {
					this.notification.display (e.message);
				}
				return true;
			});
			this.stack.add_controller (drop_target);

			this.colors_list.factory = new BuilderListItemFactory.from_resource (null, "/app/paletti/gtk/ui/color_tile_placeholder.ui");
			this.colors_list.model = new NoSelection (store);
			this.colors_list.activate.connect ((position) => {
				if (this.view_model.pix != null) {
					get_clipboard ().set_text (((!) this.view_model.pix).colors[position].to_string ());
				}
			});

			this.view_model.bind_property (
				"is-black-white", this.mono_switch, "state",
				BindingFlags.BIDIRECTIONAL | BindingFlags.SYNC_CREATE
			);
			this.view_model.bind_property (
				"count", this.colors_range, "value",
				BindingFlags.BIDIRECTIONAL | BindingFlags.SYNC_CREATE,
				(binding, srcval, ref targetval) => {
					var src = (int) srcval;
					targetval.set_double ((double) src);
					return true;
				},
				(binding, srcval, ref targetval) => {
					var src = (double) srcval;
					targetval.set_int ((int) src);
					return true;
				}
			);
			this.view_model.notify["notification"].connect (() => {
				this.notification.display (this.view_model.notification);
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

		[GtkCallback]
		void on_key_released (uint keyval, uint keycode, ModifierType state) {
			if (keyval == Key.x) {
				mono_switch.activate ();
			} else if (keyval == Key.Up) {
				colors_range.value++;
			} else if (keyval == Key.Down) {
				colors_range.value--;
			} else if (keyval == Key.F1) {
				AboutDialog (this);
			} else if (keyval == Key.o && (state & ModifierType.CONTROL_MASK) > 0) {
				open_file_dialog ();
			} else if (keyval == Key.v && (state & ModifierType.CONTROL_MASK) > 0) {
				var clipboard = get_clipboard ();
				clipboard.read_texture_async.begin (null, (obj, res) => {
					try {
						var texture = (!) clipboard.read_texture_async.end (res);
						var tmp_image = Path.build_filename (
							Environment.get_user_cache_dir (), "Paletti", "tmp.png"
						);
						texture.save_to_png (tmp_image);
						view_model.load (tmp_image);
					} catch (Error e) {
						notification.display (e.message);
					}
				});
			} else {
				scene.on_shortcut (keyval, keycode, state);
			}
		}

		[GtkCallback]
		void on_droparea_click (int n_press, double x, double y) {
			open_file_dialog ();
		}

		public void on_save_dialog () {
			var dialog = new FileChooserDialog (
				"Save Paletti image", this, FileChooserAction.SAVE,
				"Cancel", ResponseType.CANCEL,
				"Save", ResponseType.OK
			);
			dialog.response.connect ((response) => {
				if (response == ResponseType.OK) {
					try {
						view_model.save (dialog.get_file ());
					} catch (Error e) {
						notification.display (e.message);
					}
				}
				dialog.destroy ();
			});
			dialog.show ();
		}

		public void open_file_dialog () {
			var dialog = new FileChooserDialog (
				"Select an image", this, FileChooserAction.OPEN,
				"Cancel", ResponseType.CANCEL,
				"Open", ResponseType.OK
			);
			dialog.response.connect ((response) => {
				if (response == ResponseType.OK) {
					try {
						view_model.load (dialog.get_file ().get_path () ?? "");
					} catch (Leptonica.Exception e) {
						notification.display (e.message);
					}
				}
				dialog.destroy ();
			});
			dialog.show ();
		}

		private void on_change_count () {
			store.remove_all ();
			for (short i = 0; i < view_model.count; i++) {
				store.append (new RGB (255, 255, 255));
			}
		}

		private void on_change_image () {
			var scene = new ImageScene (this, notification, view_model);
			this.stack.add_named (scene, "Image");
			this.scene = scene;
			this.stack.set_visible_child_name ("Image");
			scene.load_image.begin ();
			view_model.notify["pix"].disconnect (on_change_image);
			view_model.notify["count"].disconnect (on_change_count);
		}
	}
}
