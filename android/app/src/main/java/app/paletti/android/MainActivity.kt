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
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.commit
import androidx.work.WorkManager

class MainActivity : AppCompatActivity() {
    private val viewModel: ImageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkManager.getInstance(this).pruneWork()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
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
