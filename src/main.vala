using Gdk;
using Gtk;

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
			win = new Paletti.Window (app);
		}
		win.present ();
	});

	return app.run (args);
}

