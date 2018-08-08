package app.kiostix.kiostixscanner

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Switch
import android.widget.Toast
import app.kiostix.kiostixscanner.api.ApiClient
import app.kiostix.kiostixscanner.auth.LoginActivity
import app.kiostix.kiostixscanner.model.User
import com.zebra.adc.decoder.BarCodeReader
import io.realm.Realm
import io.realm.kotlin.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.sync_action_layout.*
import kotlinx.android.synthetic.main.scanning_layout.*
import org.jetbrains.anko.doAsync
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection


class MainActivity : AppCompatActivity() {

    private val realm: Realm? = Realm.getDefaultInstance()
    private val apiClient = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkSession()) {
            val toLoginActivity = Intent(this, LoginActivity::class.java)
            toLoginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            toLoginActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            toLoginActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(toLoginActivity)
        }

        DownloadCard.setOnClickListener {
            doAsync {
                downloadFile()
            }
        }

    }

    private fun checkSession():Boolean {
        var pass = false
        val user = realm?.where<User>()?.findFirst()
        if (user != null) {
            pass = true
        }

        return pass
    }

    private fun downloadFile() {
        val url = URL(apiClient.cdnUrl)
        val connection = url.openConnection()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.LogoutItem -> {
                realm?.executeTransaction {
                    realm.delete<User>()
                }
                finish()
//                val toLoginActivity = Intent(this, LoginActivity::class.java)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
