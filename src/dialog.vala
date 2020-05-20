using Gdk;
using Gtk;

namespace Paletti {
	[GtkTemplate (ui = "/com/moebots/Paletti/ui/open_file_dialog.ui")]
	public class OpenFileDialog : FileChooserDialog {
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/save_file_dialog.ui")]
	public class SaveFileDialog : FileChooserDialog {
		public SaveFileDialog (INotification notification, CachedPix pix) {
			this.response.connect ((self, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					try {
						var filename = File.new_for_path (
							get_file ().get_path () + @".$(pix.format)"
						);
						pix.copy (filename);
						notification.display (
							@"Copied image to $(filename.get_path ())",
							NotificationType.INFO
						);
					} catch (Error e) {
						notification.display (e.message);
					}
				}
				self.destroy ();
			});
		}
	}

	[GtkTemplate (ui = "/com/moebots/Paletti/ui/export_palette_dialog.ui")]
	public class ExportPaletteDialog : FileChooserDialog {
		public ExportPaletteDialog (ExportColorPaletteWindow palette) {
			this.response.connect ((self, response_id) => {
				if (response_id == ResponseType.ACCEPT) {
					try {
						palette.export (get_file ().get_path ());
					} catch (Error e) {
						stderr.printf (e.message);
					}
				}
				self.destroy ();
			});
		}
	}

	public void AboutDialog (Window parent) {
		try {
			show_about_dialog (
				parent,
				"program_name", "Paletti",
				"logo", new Pixbuf.from_resource ("/com/moebots/Paletti/resources/com.moebots.Paletti.png"),
				"title", "About Paletti",
				"authors", new string[] {"Eroica"},
				"copyright", "Copyright © 2020 Eroica",
				"version", "2.0",
				"license", (string) resources_lookup_data (
					"/com/moebots/Paletti/resources/LICENSE",
					ResourceLookupFlags.NONE
				).get_data (),
				"website", "https://github.com/Eroica/Paletti",
				"website_label", "Paletti GitHub Repository",
				null
			);
		} catch (Error e) {
			print (
"""Paletti v2.0, (c) 2020 Eroica

Paletti uses Leptonica (http://leptonica.org)

/*====================================================================*
 -  Copyright (C) 2001 Leptonica.  All rights reserved.
 -
 -  Redistribution and use in source and binary forms, with or without
 -  modification, are permitted provided that the following conditions
 -  are met:
 -  1. Redistributions of source code must retain the above copyright
 -     notice, this list of conditions and the following disclaimer.
 -  2. Redistributions in binary form must reproduce the above
 -     copyright notice, this list of conditions and the following
 -     disclaimer in the documentation and/or other materials
 -     provided with the distribution.
 -
 -  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 -  ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 -  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 -  A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL ANY
 -  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 -  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 -  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 -  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 -  OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 -  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 -  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *====================================================================*/
"""
			);
		}
	}
}
