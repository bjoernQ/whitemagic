package de.mobilej.ktest

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import de.mobilej.ktest.databinding.ActivityMainBinding
import de.mobilej.whitemagic.findCallbackFor

class MainActivity : AppCompatActivity(), MainViewModel.ActionCallbacks, InputDialogFragment.OnResult {

    private lateinit var vm: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        vm = ViewModelProviders.of(this).get(MainViewModel::class.java)

        vm.attach(this)
        vm.attachCallbacks(this)

        binding.setVariable(BR.vm, vm)
        binding.executePendingBindings()

        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun getActivity(): AppCompatActivity {
        return this
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_dont_crash) {
            startActivity(Intent(this, DontCrashActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showInputDialog(): String {
        val dlg = InputDialogFragment()
        dlg.show(supportFragmentManager, "TAG")
        return "TAG_FOR_INPUT"
    }

    override fun onResult(result: String) {
        findCallbackFor(vm, "TAG_FOR_INPUT")?.onResult(result)
    }

    override fun showWebsiteStats(websiteUrl: String): String {
        val intent = Intent(this, SiteDetailsActivity::class.java)
        intent.putExtra(SiteDetailsActivity.EXTRA_URL, websiteUrl)
        startActivityForResult(intent, 42)
        return "TAG_FOR_SHOW_WEBSITE_STATS"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 42) {
            val callback = findCallbackFor(vm, "TAG_FOR_SHOW_WEBSITE_STATS")
            if (resultCode == Activity.RESULT_OK) {
                callback?.onResult(true)
            } else {
                callback?.onResult(false)
            }
        }
    }
}