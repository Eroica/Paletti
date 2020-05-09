using Gtk;

namespace Paletti {
	[GtkTemplate (ui = "/com/moebots/Paletti/ui/open_file_dialog.ui")]
	public class OpenFileDialog : FileChooserDialog {
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/save_file_dialog.ui")]
	public class SaveFileDialog : FileChooserDialog {
	}
}
