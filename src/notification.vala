using Gtk;

namespace Paletti {
	public enum NotificationType {
		INFO,
		ERROR;
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/notification.ui")]
	public class Notification : Revealer {
		[GtkChild]
		private Label label;

		public void show (string notification, NotificationType type = NotificationType.ERROR) {
			if (type == NotificationType.INFO) {
				label.get_style_context ().add_class ("info");
			}
			label.label = notification;
			reveal_child = true;
			Timeout.add (3600, () => {
				reveal_child = false;
				return false;
			});
			Timeout.add (3850, () => {
				label.get_style_context ().remove_class ("info");
				return false;
			});
		}
	}
}
