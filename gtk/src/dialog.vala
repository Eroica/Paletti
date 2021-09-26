using Gdk;
using Gtk;

namespace Paletti {
	public void AboutDialog (Window parent) {
		try {
			show_about_dialog (
				parent,
				"program_name", "Paletti",
				"logo", Texture.from_resource ("/app/paletti/gtk/resources/app.paletti.gtk.png"),
				"title", "About Paletti",
				"authors", new string[] {"Eroica"},
				"copyright", "Copyright © 2020-2021 Eroica",
				"version", "2021.09",
				"license", (string) resources_lookup_data (
					"/app/paletti/gtk/resources/LICENSE",
					ResourceLookupFlags.NONE
				).get_data (),
				"license_type", License.CUSTOM,
				"website", "https://paletti.app",
				"website_label", "Paletti’s website",
				null
			);
		} catch (Error e) {
			stderr.printf (e.message);
		}
	}
}
