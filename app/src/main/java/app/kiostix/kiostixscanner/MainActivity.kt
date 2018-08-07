package app.kiostix.kiostixscanner

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import app.kiostix.kiostixscanner.auth.LoginActivity
import app.kiostix.kiostixscanner.model.User
import com.zebra.adc.decoder.BarCodeReader
import io.realm.Realm
import io.realm.kotlin.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.sync_action_layout.*
import kotlinx.android.synthetic.main.scanning_layout.*


class MainActivity : AppCompatActivity() {

    private val realm: Realm? = Realm.getDefaultInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkSession()) {
            val toLoginActivity = Intent(this, LoginActivity::class.java)
            toLoginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            toLoginActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            toLoginActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(toLoginActivity)
        }

    }

    fun checkSession():Boolean {
        val pass = false
        val user = realm?.where<User>()?.findFirst()

        return pass
    }


}
