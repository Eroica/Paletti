package app.paletti.android

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.transition.TransitionInflater
import androidx.work.WorkInfo
import app.paletti.android.databinding.FragmentMainBinding
import com.google.android.material.transition.MaterialFadeThrough

class MainFragment : Fragment(R.layout.fragment_main) {
    private lateinit var binding: FragmentMainBinding
    private val viewModel: ImageViewModel by activityViewModels()

    private val selectImageResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.new(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.fragment_main_enter)
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_main, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)
        binding.executePendingBindings()
        binding.viewModel = viewModel
        (activity as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }

        if (savedInstanceState == null && childFragmentManager.findFragmentById(binding.container.id) == null) {
            childFragmentManager.beginTransaction()
                .add(binding.container.id, InitialFragment())
                .commit()
        }

        if (viewModel.imageId.get() == -1) {
            viewModel.workState.observe(viewLifecycleOwner) {
                if (it == WorkInfo.State.SUCCEEDED) {
                    childFragmentManager.beginTransaction()
                        .replace(binding.container.id, ImageFragment())
                        .commit()
                    viewModel.workState.removeObservers(viewLifecycleOwner)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_open_image -> selectImageResult.launch("image/*")
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
