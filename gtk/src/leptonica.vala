namespace Leptonica {
	public errordomain Exception {
		UNSUPPORTED,
		FAILURE,
	}

	public enum Format {
		UNKNOWN, BMP, JFIF_JPEG, PNG, TIFF, TIFF_PACKBITS, TIFF_RLE, TIFF_G3,
		TIFF_G4, TIFF_LZW, TIFF_ZIP, PNM, PS, GIF, JP2, WEBP, LPDF, TIFF_JPEG,
		DEFAULT, SPIX;

		public string to_string () {
			switch (this) {
				case BMP: return "bmp";
				case JFIF_JPEG: return "jpg";
				case PNG: return "png";
				case TIFF: return "tiff";
				case TIFF_PACKBITS: return "tiff";
				case TIFF_RLE: return "tiff";
				case TIFF_G3: return "tiff";
				case TIFF_G4: return "tiff";
				case TIFF_LZW: return "tiff";
				case TIFF_ZIP: return "tiff";
				case PNM: return "pnm";
				case PS: return "ps";
				case GIF: return "gif";
				case JP2: return "jp2";
				case WEBP: return "webp";
				case LPDF: return "lpdf";
				case TIFF_JPEG: return "tiff";
				default: return "";
			}
		}

		public int to_int () {
			switch (this) {
				case BMP: return 1;
				case JFIF_JPEG: return 2;
				case PNG: return 3;
				case TIFF: return 4;
				case TIFF_PACKBITS: return 5;
				case TIFF_RLE: return 6;
				case TIFF_G3: return 7;
				case TIFF_G4: return 8;
				case TIFF_LZW: return 9;
				case TIFF_ZIP: return 10;
				case PNM: return 11;
				case PS: return 12;
				case GIF: return 13;
				case JP2: return 14;
				case WEBP: return 15;
				case LPDF: return 16;
				case TIFF_JPEG: return 17;
				default: return 0;
			}
		}
	}

	public Format extension (int code) {
		switch (code) {
			case 1: return Leptonica.Format.BMP;
			case 2: return Leptonica.Format.JFIF_JPEG;
			case 3: return Leptonica.Format.PNG;
			case 4: return Leptonica.Format.TIFF;
			case 5: return Leptonica.Format.TIFF_PACKBITS;
			case 6: return Leptonica.Format.TIFF_RLE;
			case 7: return Leptonica.Format.TIFF_G3;
			case 8: return Leptonica.Format.TIFF_G4;
			case 9: return Leptonica.Format.TIFF_LZW;
			case 10: return Leptonica.Format.TIFF_ZIP;
			case 11: return Leptonica.Format.PNM;
			case 12: return Leptonica.Format.PS;
			case 13: return Leptonica.Format.GIF;
			case 14: return Leptonica.Format.JP2;
			case 15: return Leptonica.Format.WEBP;
			case 16: return Leptonica.Format.LPDF;
			case 17: return Leptonica.Format.TIFF_JPEG;
			default: return Leptonica.Format.UNKNOWN;
		}
	}

	public struct PosterizeJob {
		int count;
		bool is_black_white;
		string source_path;
		string cache_dir;
	}

	public struct PosterizeSuccess {
		string destination;
		int width;
		int height;
		Paletti.RGB[] colors;
	}

	public PosterizeSuccess posterize (PosterizeJob job) throws Leptonica.Exception {
		PIX? source = new Leptonica.PIX.from_filename (job.source_path);
		if (source == null) {
			throw new Leptonica.Exception.UNSUPPORTED ("Could not read this image.");
		}

		PIX? tmp;
		if (job.is_black_white) {
			Leptonica.PIX? tmp2 = Leptonica.pixConvertRGBToLuminance ((!) source);
			Leptonica.PIX? tmp3 = Leptonica.pixConvert8To32 ((!) tmp2);
			tmp = Leptonica.pixMedianCutQuantGeneral ((!) tmp3, 0, 8, job.count);
		} else {
			tmp = Leptonica.pixMedianCutQuantGeneral ((!) source, 0, 8, job.count);
		}
		if (tmp == null) {
			throw new Leptonica.Exception.FAILURE ("Could not run quantization on this image.");
		}
		var destination = Path.build_filename (job.cache_dir, @"cache.$(extension (((!) tmp).input_format))");
		((!) tmp).write (destination, ((!) tmp).input_format);

		Paletti.RGB[] colors = {};
		foreach (Paletti.RGB color in ((!) ((!) tmp).colormap).colors) {
			if (job.is_black_white && (color.r != color.g || color.r != color.b)) {
				continue;
			} else {
				colors += new Paletti.RGB (color.r, color.g, color.b);
			}
		}

		return {destination, ((!) tmp).width, ((!) tmp).height, colors};
	}
}
