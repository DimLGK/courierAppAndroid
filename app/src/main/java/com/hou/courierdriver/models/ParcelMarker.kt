package com.hou.courierdriver.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ParcelMarker(
    var uid: String? = "",
    var title: String? = "",
    var information: String? = "",
    var delivered: Boolean? = false,
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0
)
