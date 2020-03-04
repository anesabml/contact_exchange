package com.app.anesabml.contactexchange

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import com.app.anesabml.contactexchange.models.Contact

class NfcManager(
        private val nfcActivity: NfcActivity
) :
        NfcAdapter.CreateNdefMessageCallback,
        NfcAdapter.OnNdefPushCompleteCallback {

    companion object {
        const val MIME_TEXT_PLAIN = "application/vnd.com.app.anesabml.contactexchange"
    }

    override fun createNdefMessage(event: NfcEvent): NdefMessage {
        // creating outcoming NFC message with a helper method
        // you could as well create it manually and will surely need, if Android version is too low
        val outContact = nfcActivity.getOutcomingMessage()

        with(outContact) {
            val msg = NdefMessage(
                    arrayOf(
                            NdefRecord.createMime(MIME_TEXT_PLAIN, this.id.toString().toByteArray()),
                            NdefRecord.createMime(MIME_TEXT_PLAIN, this.name?.toByteArray()),
                            NdefRecord.createMime(MIME_TEXT_PLAIN, this.numbers[0]?.toByteArray())
                    )
            )
            return msg
        }
    }

    override fun onNdefPushComplete(event: NfcEvent) {
        // onNdefPushComplete() is called on the Binder thread, so remember to explicitly notify
        // your view on the UI thread
        nfcActivity.signalResult()
    }


    /*
    * Callback to be implemented by a Sender activity
    * */
    interface NfcActivity {
        fun getOutcomingMessage(): Contact

        fun signalResult()
    }
}
