package com.hou.courierdriver.util

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.hou.courierdriver.R
import com.hou.courierdriver.models.ParcelMarker
import kotlinx.android.synthetic.main.view_details.view.*

class DetailsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_details, this)
    }

    fun onSaveButtonClicked(onClick: () -> Unit) = saveMarker.setOnClickListener {
        onClick()
        hide()
    }

    fun onRemoveButtonClicked(onClick: () -> Unit) = removeMarker.setOnClickListener {
        onClick()
        hide()
    }

    fun render(parcelMarker: ParcelMarker) {
        title.setText(parcelMarker.title)
        information.setText(parcelMarker.information)
        status.isChecked = parcelMarker.delivered!!
        show()
    }

    fun clear() {
        title.setText("")
        information.setText("")
        status.isChecked = false
        show()
    }
}