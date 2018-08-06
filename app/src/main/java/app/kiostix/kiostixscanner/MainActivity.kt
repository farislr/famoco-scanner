package app.kiostix.kiostixscanner

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import app.kiostix.kiostixscanner.model.User
import com.zebra.adc.decoder.BarCodeReader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.sync_action_layout.*
import kotlinx.android.synthetic.main.scanning_layout.*


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DownloadCard.setOnClickListener {

        }

    }
}
