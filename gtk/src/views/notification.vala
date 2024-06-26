using Gtk;

namespace Paletti {
	public enum NotificationType {
		INFO,
		ERROR;
	}

	public interface INotification : Object {
		public abstract void display (string message, NotificationType type = NotificationType.ERROR);
	}

	[GtkTemplate (ui = "/app/paletti/gtk/ui/views-notification.ui")]
	public class Notification : Box, INotification {
		[GtkChild]
		private unowned Revealer revealer;

		[GtkChild]
		private unowned Label label;

		public void display (string notification, NotificationType type = NotificationType.ERROR) {
			if (type == NotificationType.INFO) {
				this.label.add_css_class ("info");
			}
			this.label.label = notification;
			this.revealer.reveal_child = true;
			Timeout.add (3600, () => {
				this.revealer.reveal_child = false;
				return false;
			});
			Timeout.add (3850, () => {
				this.label.remove_css_class ("info");
				return false;
			});
		}
	}
}
