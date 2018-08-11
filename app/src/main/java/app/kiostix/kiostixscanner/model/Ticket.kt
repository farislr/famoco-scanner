package app.kiostix.kiostixscanner.model

import io.realm.RealmObject

open class Ticket: RealmObject() {

    var event_name: String? = null
    var schedule_data: String? = null
}