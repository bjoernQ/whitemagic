package de.mobilej.ktest

import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.mobilej.ktest.databinding.ActivityDontCrashBinding

class DontCrashActivity : AppCompatActivity() {

    private lateinit var vm: DontCrashViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityDontCrashBinding>(this, R.layout.activity_dont_crash)
        vm = ViewModelProviders.of(this).get(DontCrashViewModel::class.java)

        vm.attach(this)

        binding.setVariable(BR.vm, vm)
        binding.executePendingBindings()

        if (savedInstanceState == null) {
            vm.init()
        }
    }

    override fun onResume() {
        super.onResume()
        finish()
    }
}