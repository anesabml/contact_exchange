package com.app.anesabml.contactexchange

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.app.anesabml.contactexchange.OutcomingNfcManager.Companion.MIME_TEXT_PLAIN
import com.app.anesabml.contactexchange.databinding.ActivityReceiverBinding
import com.app.anesabml.contactexchange.main.SenderActivity
import com.app.anesabml.contactexchange.receiver.ReceiverViewModel
import com.app.anesabml.contactexchange.uimodels.Contact
import android.support.v4.content.ContextCompat.startActivity
import android.provider.ContactsContract




class ReceiverActivity : AppCompatActivity() {

    val PERMISSIONS_REQUEST_WRITE_CONTACTS: Int = 1

    private lateinit var mBinding: ActivityReceiverBinding
    private lateinit var mViewModel: ReceiverViewModel

    private var mNfcAdapter: NfcAdapter? = null
    private var mNewContact: Contact? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_receiver)

        mViewModel = ViewModelProviders.of(this).get(ReceiverViewModel::class.java)

        //Check if NFC is available on device
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (!mNfcAdapter?.isEnabled!!) {
            showEnableNfcDialog()
            return
        }

    }

    private fun showEnableNfcDialog() {
        val builder = AlertDialog.Builder(this, R.style.Dialog_Light)
        builder.setMessage(R.string.enable_nfc_msg)
        builder.setPositiveButton(R.string.enable) { _, _ ->
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }
        builder.setNegativeButton(R.string.cancel) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        builder.show()
    }

    override fun onNewIntent(intent: Intent?) {
        processIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // foreground dispatch should be enabled here, as onResume is the guaranteed place where app
        // is in the foreground
        enableForegroundDispatch(this, mNfcAdapter)
        processIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        disableForegroundDispatch(this, mNfcAdapter)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_WRITE_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, so get the contact list
                    mNewContact?.let { mViewModel.saveContact(it) }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

        }
    }

    private fun enableForegroundDispatch(activity: AppCompatActivity, adapter: NfcAdapter?) {
        // here we are setting up receiving activity for a foreground dispatch
        // thus if activity is already started it will take precedence over any other activity or app
        // with the same intent filters
        val intent = Intent(activity.applicationContext, activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)

        val filters = arrayOfNulls<IntentFilter>(1)
        val techList = arrayOf<Array<String>>()

        filters[0] = IntentFilter()
        with(filters[0]) {
            this?.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
            this?.addCategory(Intent.CATEGORY_DEFAULT)
            try {
                this?.addDataType(MIME_TEXT_PLAIN)
            } catch (ex: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("Check your MIME type")
            }
        }

        adapter?.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    private fun disableForegroundDispatch(activity: AppCompatActivity, adapter: NfcAdapter?) {
        adapter?.disableForegroundDispatch(activity)
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    private fun processIntent(intent: Intent?) {
        val action = intent?.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            // only one message sent during the beam
            val msg = parcelables?.get(0) as NdefMessage
            val records = msg.records
            // record 0 contains the MIME type, record 1 is the AAR, if present
            Log.d("Ndf msg", records[0].payload.toString())

            val newContact = Contact(
                    String(records[0].payload).toInt(),
                    String(records[1].payload),
                    arrayListOf(String(records[2].payload)))

            val builder = AlertDialog.Builder(this, R.style.Dialog_Light)
            builder.setTitle(R.string.new_contact)
            builder.setMessage(getString(R.string.save) + " ${newContact.name}")
            builder.setPositiveButton(R.string.save) { _, _ ->
                mNewContact = newContact
                //checkContactPermission()
                addAsContactConfirmed()
            }
            builder.setNegativeButton(R.string.cancel) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            builder.show()
        }
    }

    private fun checkContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_CONTACTS),
                    PERMISSIONS_REQUEST_WRITE_CONTACTS)
        } else {
            mNewContact?.let { mViewModel.saveContact(it) }
        }
    }

    fun addAsContactConfirmed() {

        val intent = Intent(Intent.ACTION_INSERT)
        intent.type = ContactsContract.Contacts.CONTENT_TYPE

        intent.putExtra(ContactsContract.Intents.Insert.NAME, mNewContact?.name)
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, mNewContact?.numbers?.get(0))

        startActivity(intent)

    }

}
