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
	[CCode (cname = "PIX", free_function = "pixDestroy", free_function_address_of = true, has_type_id = false)]
	[Compact]
	public class PIX {
		public PixColormap? colormap {
			[CCode (cname = "pixGetColormap")] get;
		}

		public int width {
			[CCode (cname = "pixGetWidth")] get;
		}

		public int height {
			[CCode (cname = "pixGetHeight")] get;
		}

		public int depth {
			[CCode (cname = "pixGetDepth")] get;
		}

		public int input_format {
			[CCode (cname = "pixGetInputFormat")] get;
		}

		public int ref_count {
			[CCode (cname = "pixGetRefcount")] get;
		}

		[CCode (cname = "pixRead")]
		public PIX.from_filename (string filename);

		// TODO
		[CCode (cname = "pixWrite", instance_pos = 0.5)]
		public void write (string filename, int format);

		[CCode (cname = "pixWriteStream", instance_pos = 0.5)]
		public int write_stream (out GLib.FileStream fp, int format);

		[CCode (cname = "pixClone")]
		public PIX clone ();
	}

	[Compact]
	[CCode (cname = "PIXCMAP", has_type_id = false)]
	public class PixColormap {
		public int size {
			[CCode (cname = "pixcmapGetCount")] get;
		}

		public Paletti.RGB get (int index) throws Exception {
			int r, g, b;
			var ok = get_color (index, out r, out g, out b);
			if (ok != 0) {
				throw new Exception.FAILURE("Could not get color");
			}
			return Paletti.RGB (r, g, b);
		}

		public Paletti.RGB[] colors {
			owned get {
				var count = size;
				var r = new int[count];
				var g = new int[count];
				var b = new int[count];
				var a = new int[count];
				var ok = get_arrays (out r, out g, out b, out a);
				if (ok != 0) {
					return new Paletti.RGB[0];
				}

				Paletti.RGB[] colors = new Paletti.RGB[count];
				for (int i=0; i < count; i++) {
					colors[i] = Paletti.RGB (r[i], g[i], b[i]);
				}
				return colors;
			}
		}

		[CCode (cname = "pixcmapGetColor")]
		private int get_color (int index, out int r, out int g, out int b);

		[CCode (cname = "pixcmapToArrays")]
		private int get_arrays ([CCode (array_length = false)] out int[] prmap,
		                        [CCode (array_length = false)] out int[] pgmap,
		                        [CCode (array_length = false)] out int[] pbmap,
		                        [CCode (array_length = false)] out int[] pamap);
	}

	[CCode (cname = "pixMedianCutQuantGeneral")]
	PIX pixMedianCutQuantGeneral (PIX pix,
	                              l_int32  ditherflag,
	                              l_int32  outdepth,
	                              l_int32  maxcolors,
	                              l_int32  sigbits = 0,
	                              l_int32  maxsub = 0,
	                              l_int32  checkbw = 0);

	[CCode (cname = "pixWrite")]
	int pix_write (string filename, PIX pix, int format);

	[CCode (cname = "pixQuantizeIfFewColors")]
	int pixQuantizeIfFewColors (PIX pix,
	                            l_int32 maxcolors,
	                            l_int32 mingraycolors,
	                            l_int32 octlevel,
	                            out PIX ppixd);

	[CCode (cname = "pixConvertRGBToLuminance")]
	PIX pixConvertRGBToLuminance (PIX pix);

	[CCode (cname = "pixAddGrayColormap8")]
	int pixAddGrayColormap8 (PIX pix);

	[CCode (cname = "pixAddMinimalGrayColormap8")]
	PIX pixAddMinimalGrayColormap8 (PIX pix);

	[CCode (cname = "pixRemoveColormap")]
	PIX pixRemoveColormap (PIX pix, int type = 1);
}
