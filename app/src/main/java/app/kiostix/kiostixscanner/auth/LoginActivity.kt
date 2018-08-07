package app.kiostix.kiostixscanner.auth

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import app.kiostix.kiostixscanner.R
import android.widget.Toast



class LoginActivity : AppCompatActivity() {
    private var exit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    override fun onBackPressed() {
        if (exit) {
            finish() // finish activity
        } else {
            Toast.makeText(this, "Press Back again to Exit.",
                    Toast.LENGTH_SHORT).show()
            exit = true
            Handler().postDelayed({ exit = false }, 3 * 1000)
        }
    }
}
