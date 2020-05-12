using Gtk;

namespace Paletti {
	[GtkTemplate (ui = "/com/moebots/Paletti/ui/open_file_dialog.ui")]
	public class OpenFileDialog : FileChooserDialog {
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/save_file_dialog.ui")]
	public class SaveFileDialog : FileChooserDialog {
		public SaveFileDialog (INotification notification, CachedPix pix) {
			response.connect ((self, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					try {
						pix.copy (get_file ());
						notification.display (
							@"Copied image to $(get_file ().get_path ())",
							NotificationType.INFO
						);
					} catch (Error e) {
						notification.display (e.message);
					}
				}
				self.destroy ();
			});
		}
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/export_palette_dialog.ui")]
	public class ExportPaletteDialog : FileChooserDialog {
		public ExportPaletteDialog (ExportColorPaletteWindow palette) {
			response.connect ((self, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					try {
						palette.export (get_file ().get_path ());
					} catch (Error e) {
						stderr.printf (e.message);
					}
				}
				self.destroy ();
			});
		}
	}
}
