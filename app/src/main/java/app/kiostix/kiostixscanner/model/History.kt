package app.kiostix.kiostixscanner.model

import io.realm.RealmObject

open class History: RealmObject() {

    var device_name: String? = null
    var transaction_data: String? = null
    var created_at: String? = null

}