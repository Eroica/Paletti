namespace Paletti {
	public class Application : Adw.Application {
		public Application () {
			Object (application_id: "app.paletti.gtk", flags: ApplicationFlags.DEFAULT_FLAGS);
		}

		construct {
			ActionEntry[] action_entries = {
				{ "about", this.on_about_action },
				{ "quit", this.quit }
			};
			this.add_action_entries (action_entries, this);
			this.set_accels_for_action ("app.quit", {"<primary>q"});
		}

		private void on_about_action () {
			var about = new Adw.AboutWindow () {
				transient_for = this.active_window,
				application_name = "Paletti",
				application_icon = "app.paletti.gtk",
				developer_name = "Eroica",
				version = "v2024.04a",
				developers = { "Eroica" },
				copyright = "© 2020-2024 Eroica",
				issue_url = "https://github.com/Eroica/Paletti/issues",
				license_type = Gtk.License.CUSTOM,
				website = "https://paletti.app"
			};

			about.present ();
		}
	}
}
