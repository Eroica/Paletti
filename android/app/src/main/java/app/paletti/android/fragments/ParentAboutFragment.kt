package app.paletti.android.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import app.paletti.android.R
import app.paletti.android.databinding.FragmentAboutBinding
import com.google.android.material.transition.MaterialFadeThrough

class ParentAboutFragment : Fragment(R.layout.fragment_about) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FragmentAboutBinding.bind(view).apply {
            licenseReportText.text = resources.openRawResource(R.raw.report)
                .bufferedReader()
                .use { it.readText() }
            licenseApache.text = resources.openRawResource(R.raw.apache)
                .bufferedReader()
                .use { it.readText() }
            licenseKodein.text = resources.openRawResource(R.raw.license_kodein)
                .bufferedReader()
                .use { it.readText() }
        }
    }
}
