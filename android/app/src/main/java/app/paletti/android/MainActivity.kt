package app.paletti.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.commit
import androidx.work.WorkManager
import app.paletti.android.fragments.MainFragment
import app.paletti.android.fragments.ParentAboutFragment
import org.kodein.di.conf.DIGlobalAware
import org.kodein.di.instance

class MainActivity : AppCompatActivity(), DIGlobalAware {
    private val WorkManager: WorkManager by instance()
    private val viewModel: ImageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkManager.pruneWork()
        if (savedInstanceState == null) {
            findViewById<ViewGroup>(android.R.id.content).doOnPreDraw {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    add(android.R.id.content, MainFragment(), null)
                }
            }
        }
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { viewModel.new(it) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(android.R.id.content, ParentAboutFragment(), null)
                    addToBackStack(null)
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
