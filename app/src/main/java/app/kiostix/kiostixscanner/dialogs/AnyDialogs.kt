package app.kiostix.kiostixscanner.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.view.LayoutInflaterCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import app.kiostix.kiostixscanner.MainActivity
import app.kiostix.kiostixscanner.R
import org.jetbrains.anko.support.v4.toast

class AnyDialogs: DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setView(it.layoutInflater.inflate(R.layout.dialog_send_email, null))
                    .setPositiveButton("Send"
                    ) { dialog, id ->
                        toast("Sending ...")
                        MainActivity().sendEmail()
                    }
                    .setNegativeButton("Cancel"
                    ) { dialog, id ->
                        getDialog().cancel()
                    }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}