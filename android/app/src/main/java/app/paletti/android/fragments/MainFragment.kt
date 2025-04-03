package app.paletti.android.fragments

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.transition.TransitionInflater
import androidx.work.WorkInfo
import app.paletti.android.ImageViewModel
import app.paletti.android.R
import app.paletti.android.databinding.FragmentMainBinding
import com.google.android.material.transition.MaterialFadeThrough

class MainFragment : Fragment(R.layout.fragment_main) {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ImageViewModel by activityViewModels()

    private val selectImageResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.new(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.fragment_main_enter)
        exitTransition = MaterialFadeThrough()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMainBinding.bind(view)
        binding.viewModel = viewModel
        binding.executePendingBindings()
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

        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_open_image -> selectImageResult.launch("image/*")
                    else -> return false
                }

                return true
            }
        }, viewLifecycleOwner)
    }

    override fun onDestroyView() {
        (activity as AppCompatActivity).setSupportActionBar(null)
        binding.viewModel = null
        _binding = null
        super.onDestroyView()
    }
}
