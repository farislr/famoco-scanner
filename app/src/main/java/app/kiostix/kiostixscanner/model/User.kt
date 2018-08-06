package app.kiostix.kiostixscanner.model

import io.realm.RealmObject

class User: RealmObject() {

    var id = Int
    var fullname = String

}