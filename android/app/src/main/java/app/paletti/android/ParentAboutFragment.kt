package app.paletti.android

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import app.paletti.android.databinding.FragmentAboutBinding
import com.google.android.material.transition.MaterialFadeThrough

class ParentAboutFragment : Fragment(R.layout.fragment_about) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FragmentAboutBinding.bind(view)
    }
}
