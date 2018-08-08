package app.kiostix.kiostixscanner.model

import io.realm.RealmObject

open class Ticket: RealmObject() {

    var eventName: String? = null
    var scheduleData: String? = null
}