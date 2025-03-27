package app.paletti.android.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.work.WorkInfo
import app.paletti.android.ImageViewModel
import app.paletti.android.R
import app.paletti.android.databinding.FragmentInitialBinding

class InitialFragment : Fragment() {
    private lateinit var binding: FragmentInitialBinding
    private val viewModel: ImageViewModel by activityViewModels()

    /* This field is being called from the XML directly */
    val selectImageResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.new(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_initial, container, false)
        binding.lifecycleOwner = this
        binding.fragment = this
        binding.viewModel = viewModel
        binding.executePendingBindings()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.workState.observe(viewLifecycleOwner) {
            if (it == WorkInfo.State.FAILED) {
                Toast.makeText(context, getString(R.string.toast_error_load), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
