package com.hou.courierdriver.util

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.hou.courierdriver.models.ParcelMarker

fun View.show() {
    if (visibility != VISIBLE) visibility = VISIBLE
}

fun View.hide() {
    if (visibility == VISIBLE) visibility = GONE
}

fun View.closeKeyboard(context: Context) {
    clearFocus()
    context.hideKeyboard(this)
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun HashMap<String, Object>.toParcelMarker() = ParcelMarker(
    uid = this["uid"]!! as String,
    title = this["title"]!! as String,
    information = this["information"]!! as String,
    delivered = this["delivered"]!! as Boolean,
    latitude = this["latitude"] as Double,
    longitude = this["longitude"] as Double
)

fun EditText.onImeSearch(onImeSearchClicked: (String) -> Unit) {
    this.setOnEditorActionListener(object : TextView.OnEditorActionListener {
        override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onImeSearchClicked(v?.text.toString())
                return true
            }
            return false
        }
    })
}