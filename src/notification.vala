using Gtk;

namespace Paletti {
	public enum NotificationType {
		INFO,
		ERROR;
	}

	public interface INotification : Object {
		public abstract void display (string message, NotificationType type = NotificationType.ERROR);
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/notification.ui")]
	public class Notification : Revealer, INotification {
		[GtkChild]
		private Label label;

		public void display (string notification, NotificationType type = NotificationType.ERROR) {
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
