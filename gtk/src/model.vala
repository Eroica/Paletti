namespace Paletti {
	public interface IViewModel {
		public abstract void load(string path) throws Leptonica.Exception;
		public abstract void save(File destination) throws Error;
	}

	public class PosterizedPix {
		public string path;
		public RGB[] colors;
		public int width;
		public int height;

		public PosterizedPix (string path, int width, int height, RGB[] colors) {
			this.path = path;
			this.width = width;
			this.height = height;
			this.colors = colors;
		}
	}

	public class ViewModel : Object, IViewModel {
		public int count { get; set; default = DEFAULT_COLORS; }
		public bool is_black_white { get; set; default = false; }
		public string notification { get; private set; }
		public PosterizedPix? pix { get; private set; }

		private string? source_path;
		private int64 time;

		private string paletti_cache_dir = Path.build_filename (
			Environment.get_user_cache_dir (),
			"Paletti"
		);

		public ViewModel () {
			DirUtils.create_with_parents (this.paletti_cache_dir, 0755);
			this.notify["count"].connect (() => debounced_posterize (count, is_black_white));
			this.notify["is-black-white"].connect (() => debounced_posterize (count, is_black_white));
		}

		public void load (string path) throws Leptonica.Exception {
			source_path = path;
			posterize.begin (count, is_black_white, (obj, res) => {
				posterize.end (res);
			});
		}

		public void save (File destination) throws Error {
			File.new_for_path (((!) pix).path).copy (
				destination, FileCopyFlags.OVERWRITE
			);
		}

		private void debounced_posterize (int count, bool is_black_white) {
			/* Sliding the color slider rapidly will make many calls to this
			 * method. To improve performance, do not call posterize on every
			 * tick. It is only called if at least 100 milliseconds have passed
			 * between each call. */
			time = get_monotonic_time ();
			Timeout.add (100, () => {
				var delta = get_monotonic_time ();
				/* Difference in MICROseconds */
				if (delta - time >= 100000) {
					posterize.begin (count, is_black_white, (obj, res) => {
						posterize.end (res);
					});
				}
				return false;
			});
		}

		private async void posterize (int count, bool is_black_white) {
			if (source_path != null) {
				SourceFunc callback = posterize.callback;
				PosterizedPix[] output = new PosterizedPix[1];

				ThreadFunc<bool> run = () => {
					Leptonica.PosterizeJob job = {
						count, is_black_white, Path.build_filename ((!) source_path),
						Path.build_filename (paletti_cache_dir)
					};
					try {
						var result = Leptonica.posterize (job);
						output[0] = new PosterizedPix (
							result.destination, result.width, result.height, result.colors
						);
						Idle.add ((owned) callback);
					} catch (Leptonica.Exception e) {
						notification = e.message;
					}

					return true;
				};

				new Thread<bool>("paletti-posterize", (owned) run);
				yield;
				pix = output[0];
			}
		}
	}
}
