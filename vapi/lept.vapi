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

		public int input_format {
			[CCode (cname = "pixGetInputFormat")] get;
		}

		[CCode (cname = "pixRead")]
		public PIX.from_filename (string filename);

		[CCode (cname = "pixWrite", instance_pos = 0.5)]
		void write (string filename, int format);

		[CCode (cname = "pixWriteStream", instance_pos = 0.5)]
		public int write_stream (out GLib.FileStream fp, int format);
	}

	[Compact]
	[CCode (cname = "PIXCMAP", has_type_id = false)]
	public class PixColormap {
		public int size {
			[CCode (cname = "pixcmapGetCount")] get;
		}

		public Paletti.RGB get (int index) {
			int r, g, b;
			var _ = get_color (index, out r, out g, out b);
			return new Paletti.RGB (r, g, b);
		}

		public Paletti.RGB[] colors {
			owned get {
				var count = size;
				var r = new int[count];
				var g = new int[count];
				var b = new int[count];
				var a = new int[count];
				get_arrays (out r, out g, out b, out a);

				Paletti.RGB[] colors = new Paletti.RGB[count];
				for (int i=0; i < count; i++) {
					colors[i] = new Paletti.RGB (r[i], g[i], b[i]);
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
}

