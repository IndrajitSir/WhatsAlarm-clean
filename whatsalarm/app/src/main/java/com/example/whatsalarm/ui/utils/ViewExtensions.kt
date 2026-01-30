package com.example.whatsalarm.ui.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.animation.AnimationUtils
import com.example.whatsalarm.R

fun View.animateClick() {
    val down = AnimationUtils.loadAnimation(context, R.anim.scale_click)
    val up = AnimationUtils.loadAnimation(context, R.anim.scale_release)

    startAnimation(down)
    postDelayed({ startAnimation(up) }, 120)
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
