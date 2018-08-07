package app.kiostix.kiostixscanner.model

import io.realm.RealmObject

open class User: RealmObject() {

    var fullname: String? = null
    var token: String? = null

}