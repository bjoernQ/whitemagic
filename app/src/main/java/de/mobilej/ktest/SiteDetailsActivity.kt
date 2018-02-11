package de.mobilej.ktest

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.mobilej.ktest.databinding.ActivitySiteDetailsBinding

class SiteDetailsActivity : AppCompatActivity(), SiteDetailsViewModel.ActionCallbacks {

    companion object {
        const val EXTRA_URL = "extra_url"
    }

    private lateinit var vm: SiteDetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_site_details)

        val binding = DataBindingUtil.setContentView<ActivitySiteDetailsBinding>(this, R.layout.activity_site_details)
        vm = ViewModelProviders.of(this).get(SiteDetailsViewModel::class.java)

        vm.attach(this)
        vm.setActionCallbacks(this)

        binding.setVariable(BR.vm, vm)
        binding.executePendingBindings()

        vm.setSiteUrl(intent.getStringExtra(EXTRA_URL))
    }

    override fun exitWithResult(result: Boolean) {
        setResult(when (result) {
            true -> Activity.RESULT_OK
            else -> Activity.RESULT_CANCELED
        })
        finish()
    }
}