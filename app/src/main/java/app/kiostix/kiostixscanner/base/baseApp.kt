package app.kiostix.kiostixscanner.base

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class baseApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)

        val config = RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build()

        Realm.setDefaultConfiguration(config)
    }

}