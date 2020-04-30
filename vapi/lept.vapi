[SimpleType]
[CCode (cname = "l_int32", has_type_id = false)]
public struct l_int32 : int {
}

[SimpleType]
[CCode (cname = "l_ok", has_type_id = false)]
public struct l_ok : int {
}


[CCode (cheader_filename = "allheaders.h")]
namespace Leptonica {
	[Compact]
	[CCode (cname = "PIX", free_function = "", destroy_function = "", has_type_id = false)]
	public class PIX {
		[CCode (cname = "pixGetWidth")]
		public int get_width ();

		[CCode (cname = "pixGetColormap")]
		public PixColormap get_colormap ();

		[CCode (cname = "pixGetInputFormat")]
		public int get_input_format ();

		[CCode (cname = "pixWriteStream", instance_pos = 0.5)]
		public int write_stream (out GLib.FileStream fp, int format);
	}

	[Compact]
	[CCode (cname = "PIXCMAP", free_function = "", destroy_function = "", has_type_id = false)]
	public class PixColormap {
		[CCode (cname = "pixcmapGetCount")]
		public int get_count ();

		[CCode (cname = "pixcmapGetColor")]
		public int get_color (int index, out int r, out int g, out int b);

		[CCode (cname = "pixcmapToArrays")]
		public int get_arrays ([CCode (array_length = false)] out int[] prmap,
		                       [CCode (array_length = false)] out int[] pgmap,
		                       [CCode (array_length = false)] out int[] pbmap,
		                       [CCode (array_length = false)] out int[] pamap);
	}

	[CCode (cname = "pixRead")]
	PIX pixRead (string filename);

	[CCode (cname = "pixWrite")]
	void pixWrite (string filename, PIX pix, int format);

	[CCode (cname = "pixMedianCutQuantGeneral")]
	PIX pixMedianCutQuantGeneral (PIX pix,
	                              l_int32  ditherflag,
	                              l_int32  outdepth,
	                              l_int32  maxcolors,
	                              l_int32  sigbits = 0,
	                              l_int32  maxsub = 0,
	                              l_int32  checkbw = 0);
}

