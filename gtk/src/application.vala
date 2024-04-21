namespace Paletti {
	public class Application : Gtk.Application {
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
			string[] authors = { "Eroica" };
			var license = (string) resources_lookup_data (
				"/app/paletti/gtk/resources/LICENSE",
				ResourceLookupFlags.NONE
			).get_data ();

			Gtk.show_about_dialog (
				this.active_window,
				program_name: "Paletti",
				logo_icon_name: "app.paletti.gtk",
                version: "v2024.04b",
                copyright: "© 2020-2024 Eroica",
                authors: authors,
                website: "https://paletti.app",
                license: license,
                license_type: Gtk.License.CUSTOM
			);
		}
	}
}
