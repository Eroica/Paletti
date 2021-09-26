using Gdk;
using Gtk;

// Simple check: if a cached image exists, then Paletti has been used before
private bool is_first_run () {
	return !File.new_for_path (Path.build_filename (
		Environment.get_user_cache_dir (), "Paletti"
	)).query_exists ();
}

int main (string[] args) {
	var app = new Gtk.Application ("app.paletti.gtk", ApplicationFlags.FLAGS_NONE);
	var view_model = new Paletti.ViewModel();
	app.activate.connect (() => {
		CssProvider css_provider = new CssProvider ();
		css_provider.load_from_resource ("/app/paletti/gtk/resources/style.css");
		StyleContext.add_provider_for_display (
			(!) Display.get_default (),
			css_provider,
			STYLE_PROVIDER_PRIORITY_APPLICATION
		);

		var win = app.active_window;
		win = new Paletti.Window (app, view_model, is_first_run ());
		win.present ();
	});

	var quit_action = new SimpleAction ("quit", null);
	quit_action.activate.connect (app.quit);
	app.set_accels_for_action ("app.quit", { "<Ctrl>q" });
	app.add_action (quit_action);

	return app.run (args);
}
