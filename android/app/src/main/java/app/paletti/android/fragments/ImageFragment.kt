package app.paletti.android.fragments

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.transition.TransitionInflater
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.work.WorkInfo
import app.paletti.android.FilePaths
import app.paletti.android.ImageViewModel
import app.paletti.android.ProviderData
import app.paletti.android.R
import app.paletti.android.databinding.FragmentImageBinding
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.createBitmap

class ImageFragment : Fragment(), DIGlobalAware {
    companion object {
        const val IS_CENTER_CROP = "IS_CENTER_CROP"
    }

    private val Paths: FilePaths by instance()

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ImageViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionInflater.from(context)
            .inflateTransition(R.transition.fragment_image_palette_enter)
        postponeEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_image, container, false)
        binding.fragment = this
        binding.viewModel = viewModel
        binding.executePendingBindings()
        viewModel.imageId.set(binding.image.id)
        viewModel.workState.observe(viewLifecycleOwner) {
            if (it == WorkInfo.State.SUCCEEDED) {
                binding.image.setImageBitmap(BitmapFactory.decodeFile(Paths.outImage.toString()))
            }
        }

        if (savedInstanceState != null) {
            binding.image.setImageBitmap(BitmapFactory.decodeFile(Paths.outImage.toString()))
            viewModel.readColors()
        }

        setupMenu(savedInstanceState)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startPostponedEnterTransition()
        binding.image.startAnimation(AnimationUtils.loadAnimation(context, R.anim.image_enter))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_CENTER_CROP, binding.image.scaleType == ImageView.ScaleType.CENTER_CROP)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.fragment = null
        binding.viewModel = null
        _binding = null
        super.onDestroyView()
    }

    private fun shareImage(image: File) {
        val contentUri = FileProvider.getUriForFile(requireContext(), ProviderData.provider, image)
        startActivity(Intent.createChooser(Intent().apply {
            setAction(Intent.ACTION_SEND)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(contentUri, context?.contentResolver?.getType(contentUri))
            putExtra(Intent.EXTRA_STREAM, contentUri)
        }, getString(R.string.intent_share_to_application)))
    }

    private fun sharePalette() {
        val bitmap = createBitmap(viewModel.count.get().toInt() * 48, 96)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        viewModel.colors.forEachIndexed { i, color ->
            paint.color = color
            canvas.drawRect(i * 48f, 0f, (i + 1) * 48f, 96f, paint)
        }

        try {
            FileOutputStream(Paths.palette).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                shareImage(Paths.palette)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            bitmap.recycle()
        }
    }

    private fun saveImage() {
        context?.contentResolver?.let { resolver ->
            ContentValues().apply {
                val name = System.currentTimeMillis().toString() + ".bmp"
                val directory = Environment.DIRECTORY_PICTURES + File.separator + "Paletti"
                val path = directory + File.separator + name
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/bmp")
                put(MediaStore.MediaColumns.RELATIVE_PATH, directory)
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, this)?.let {
                    resolver.openOutputStream(it)?.use {
                        Paths.outImage.inputStream().copyTo(it)
                        Toast.makeText(
                            context, getString(R.string.toast_saved_image, path), Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupMenu(savedInstanceState: Bundle?) {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_image, menu)

                if (savedInstanceState?.getBoolean(IS_CENTER_CROP) == false) {
                    menu.findItem(R.id.action_crop_image).isChecked = false
                    viewModel.isImageZoom.set(false)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_crop_image -> {
                        menuItem.isChecked = !menuItem.isChecked
                        viewModel.isImageZoom.set(!viewModel.isImageZoom.get())
                    }
                    R.id.action_export_image -> shareImage(Paths.outImage)
                    R.id.action_export_palette -> sharePalette()
                    R.id.action_save_image -> saveImage()
                    else -> return false
                }

                return true
            }
        }, viewLifecycleOwner)
    }
}
