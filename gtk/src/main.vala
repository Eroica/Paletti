using Gdk;

/* Simple check: if the cache directory exists, then Paletti has been used before */
private bool is_first_run () {
	return !File.new_for_path (Path.build_filename (
		Environment.get_user_cache_dir (), "Paletti"
	)).query_exists ();
}

int main (string[] args) {
	var app = new Paletti.Application ();
	var view_model = new Paletti.ViewModel ();

	app.activate.connect (() => {
		var win = app.active_window;
		if (win == null) {
			win = new Paletti.Window (app, view_model, is_first_run ());
		}
		win.present ();
	});

	return app.run (args);
}
