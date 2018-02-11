package de.mobilej.ktest

import android.databinding.BindingAdapter
import android.view.View

object BooleanVisibilityBindingAdapter {
    @JvmStatic
    @BindingAdapter("android:visibility")
    fun setVisibility(view: View, visible: Boolean) {
        when (visible) {
            true -> view.visibility = View.VISIBLE
            else -> view.visibility = View.GONE
        }
    }
}
