using Gdk;
using Gtk;

// Simple check: if a cached image exists, then Paletti has been used before
private bool is_first_run () {
	return !File.new_for_path (Path.build_filename (
		Environment.get_user_cache_dir (), "Paletti"
	)).query_exists ();
}

int main (string[] args) {
	var app = new Gtk.Application ("com.moebots.Paletti", ApplicationFlags.FLAGS_NONE);
	app.activate.connect (() => {
		CssProvider css_provider = new CssProvider ();
		css_provider.load_from_resource ("/com/moebots/Paletti/resources/style.css");
		StyleContext.add_provider_for_screen (
			Screen.get_default (),
			css_provider,
			STYLE_PROVIDER_PRIORITY_USER
		);

		var win = app.active_window;
		if (win == null) {
			win = new Paletti.Window (app, is_first_run ());
		}
		win.present ();
	});

	return app.run (args);
}
