package app.paletti.android

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.transition.TransitionInflater
import android.view.*
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.work.WorkInfo
import app.paletti.android.databinding.FragmentImageBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class ImageFragment : Fragment() {
    private lateinit var binding: FragmentImageBinding
    private val viewModel: ImageViewModel by activityViewModels()
    @Inject lateinit var filePaths: FilePaths

    // This field is being called from the XML directly.
    val selectImageResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.new(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.fragment_image_palette_enter)
        postponeEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_image, container, false)
        binding.fragment = this
        binding.viewModel = viewModel
        viewModel.imageId.set(binding.image.id)
        viewModel.workState.observe(viewLifecycleOwner) {
            if (it == WorkInfo.State.SUCCEEDED) {
                binding.image.setImageBitmap(BitmapFactory.decodeFile(filePaths.outImage.toString()))
            }
        }
        binding.executePendingBindings()
        binding.image.startAnimation(AnimationUtils.loadAnimation(context, R.anim.image_enter))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startPostponedEnterTransition()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_image, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_export_image -> shareImage(filePaths.outImage)
            R.id.action_export_palette -> sharePalette()
            R.id.action_save_image -> saveImage()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun shareImage(image: File) {
        val contentUri = FileProvider.getUriForFile(requireContext(), ProviderData.provider, image)
        startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(contentUri, context?.contentResolver?.getType(contentUri))
            putExtra(Intent.EXTRA_STREAM, contentUri)
        }, getString(R.string.intent_share_to_application)))
    }

    private fun sharePalette() {
        val bitmap = Bitmap.createBitmap(viewModel.count.get().toInt() * 48, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        viewModel.colors.forEachIndexed { i, color ->
            paint.color = color
            canvas.drawRect(i * 48f, 0f, (i + 1) * 48f, 96f, paint)
        }

        try {
            val stream = FileOutputStream(filePaths.palette)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        shareImage(filePaths.palette)
    }

    private fun saveImage() {
        context?.contentResolver?.let { resolver ->
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis().toString() + ".bmp")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/bmp")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures" + File.separator + "Paletti")
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, this)?.let {
                    resolver.openOutputStream(it).use {
                        it?.let {
                            filePaths.outImage.inputStream().copyTo(it)
                        }
                    }
                }
            }
        }
    }
}
