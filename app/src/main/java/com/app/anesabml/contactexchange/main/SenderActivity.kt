package com.app.anesabml.contactexchange.main

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.anesabml.contactexchange.NfcManager
import com.app.anesabml.contactexchange.R
import com.app.anesabml.contactexchange.databinding.ActivitySenderBinding
import com.app.anesabml.contactexchange.models.Contact
import com.app.anesabsml.nouveauleader.ContactAdapter
import com.google.android.material.snackbar.Snackbar


class SenderActivity : AppCompatActivity(),
        RecyclerViewClickListener,
        NfcManager.NfcActivity {

    companion object {

        private val PERMISSIONS_REQUEST_READ_CONTACTS: Int = 1

    }

    private lateinit var mBinding: ActivitySenderBinding
    private val mViewModel: SenderViewModel by viewModels()

    private var mAdapter = ContactAdapter(arrayListOf(), this)
    private var mContactPosition: Int = -1
    private var mNumberPosition: Int = -1

    private var mNfcAdapter: NfcAdapter? = null
    private lateinit var nfcManager: NfcManager
    private var mSendingDialog: AlertDialog? = null
    private var mNewContact: Contact? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_sender)

        checkContactPermission()

        mBinding.contactsList.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mBinding.contactsList.adapter = mAdapter
        mViewModel.contactList.observe(this, Observer { it ->
            it?.let { mAdapter.replaceData(it) }
        })

        mBinding.search.isActivated = true
        mBinding.search.queryHint = getString(R.string.search_hint)
        mBinding.search.onActionViewExpanded()
        mBinding.search.isIconified = false
        mBinding.search.clearFocus()

        mBinding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                mAdapter.filter.filter(newText)
                return false
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBinding.contactsList.setOnScrollChangeListener { _, _, _, _, _ ->
                mBinding.searchHolder.isSelected = mBinding.contactsList.canScrollVertically(-1)
            }
        }

        nfcManager = NfcManager(this)
        //Check if NFC is available on device
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (mNfcAdapter != null) {
            //This will refer back to createNdefMessage for what it will send
            mNfcAdapter!!.setNdefPushMessageCallback(nfcManager, this)
            //This will be called if the message is sent successfully
            mNfcAdapter!!.setOnNdefPushCompleteCallback(nfcManager, this);
        }

        mViewModel.isContactSaved.observe(this, Observer {
            if (it!!) {
                Snackbar.make(mBinding.root, R.string.contact_saved_successfully, Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(mBinding.root, R.string.contact_saved_error, Snackbar.LENGTH_LONG).show()
            }
        })

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, so get the contact list
                    mViewModel.getContactsList()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

        }
    }

    override fun recyclerViewListClicked(v: View?, position: Int) {
        if (!mNfcAdapter?.isEnabled!!) {
            showEnableNfcDialog()
            return
        }
        mContactPosition = position
        showNumbersDialog()
    }

    override fun getOutcomingMessage(): Contact {
        val contactToSend = mViewModel.contactList.value?.get(mContactPosition)
        contactToSend?.numbers = arrayListOf(contactToSend?.numbers?.get(mNumberPosition))
        return contactToSend!!
    }

    override fun signalResult() {
        // this will be triggered when NFC message is sent to a device.
        // should be triggered on UI thread. We specify it explicitly
        // cause onNdefPushComplete is called from the Binder thread
        runOnUiThread {
            Toast.makeText(this, R.string.message_beaming_complete, Toast.LENGTH_SHORT).show()
            mSendingDialog?.dismiss()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
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

    private fun checkContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    PERMISSIONS_REQUEST_READ_CONTACTS)
        } else {
            mViewModel.getContactsList()
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

    private fun showNumbersDialog() {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                mViewModel.contactList.value!![mContactPosition].numbers)

        val builder = AlertDialog.Builder(this, R.style.Dialog_Light)
        builder.setTitle(mViewModel.contactList.value!![mContactPosition].name)
        builder.setAdapter(adapter) { _, which ->
            mNumberPosition = which
            showSendingDialog()
        }
        builder.show()
    }

    private fun showSendingDialog() {
        val builder = AlertDialog.Builder(this, R.style.Dialog_Light)
        builder.setTitle(getString(R.string.sending) + " " + mViewModel.contactList.value?.get(mContactPosition)?.name)
        builder.setMessage(R.string.please_place_the_devices_close)
        mSendingDialog = builder.show()
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
                this?.addDataType(NfcManager.MIME_TEXT_PLAIN)
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
     * Parses the NDEF Message from the intent and prints to Contact
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

            showSaveContactDialog(newContact)
        }
    }

    private fun showSaveContactDialog(contact: Contact) {
        val builder = AlertDialog.Builder(this, R.style.Dialog_Light)
        builder.setTitle(R.string.new_contact)
        builder.setMessage(getString(R.string.save) + " ${contact.name}")
        builder.setPositiveButton(R.string.save) { d, _ ->
            // Send the intent to contacts App
            val intent = Intent(Intent.ACTION_INSERT)
            intent.type = ContactsContract.Contacts.CONTENT_TYPE
            intent.putExtra(ContactsContract.Intents.Insert.NAME, contact.name)
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, contact.numbers?.get(0))
            startActivity(intent)

            d.dismiss()
        }
        builder.setNegativeButton(R.string.cancel) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        builder.show()
    }
}

interface RecyclerViewClickListener {
    fun recyclerViewListClicked(v: View?, position: Int)
}