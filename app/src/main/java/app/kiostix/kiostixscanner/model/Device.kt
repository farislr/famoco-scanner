package app.kiostix.kiostixscanner.model

import io.realm.RealmObject

open class Device: RealmObject() {
    var deviceId: String? = null
    var deviceName: String? = null
}