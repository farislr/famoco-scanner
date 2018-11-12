package app.kiostix.kiostixscanner.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import app.kiostix.kiostixscanner.R
import kotlinx.android.synthetic.main.dialog_send_email.view.*

class AnyDialogs: DialogFragment() {

    private lateinit var mListener: EmailDialogListener

    interface EmailDialogListener {
        fun onSendClick(dialog: DialogFragment, emailInput: String)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            mListener = context as EmailDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() +
                    " must implement NoticeDialogListener"))
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val view = it.layoutInflater.inflate(R.layout.dialog_send_email, null)
            builder.setView(view)
                    .setPositiveButton("Send"
                    ) { dialog, id ->
                        val email = view.SendEmailInput.text.toString()
                        mListener.onSendClick(this, email)
                    }
                    .setNegativeButton("Cancel"
                    ) { dialog, id ->
                        dialog.cancel()
                    }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}