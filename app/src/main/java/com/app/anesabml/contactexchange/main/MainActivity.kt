package com.app.anesabml.contactexchange.main

import android.Manifest
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NdefRecord.createMime
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import com.app.anesabml.contactexchange.R
import com.app.anesabml.contactexchange.databinding.ActivityMainBinding
import com.app.anesabml.contactexchange.uimodels.Contact
import com.app.anesabsml.nouveauleader.ContactAdapter


class MainActivity : AppCompatActivity(),
        RecyclerViewClickListener,
        NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {

    companion object {
        const val PERMISSIONS_REQUEST_READ_CONTACTS: Int = 1
    }

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mViewModel: MainViewModel

    private var mAdapter = ContactAdapter(arrayListOf(), this)
    private var mContactPosition: Int = -1
    private var mNumberPosition: Int = -1

    private var mNfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        checkContactPermission()

        mBinding.contactsList.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mBinding.contactsList.adapter = mAdapter
        mViewModel.contactList.observe(this, Observer {
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

        //Check if NFC is available on device
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (mNfcAdapter != null) {
            //This will refer back to createNdefMessage for what it will send
            mNfcAdapter!!.setNdefPushMessageCallback(this, this)

            //This will be called if the message is sent successfully
            mNfcAdapter!!.setOnNdefPushCompleteCallback(this, this);
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

    override fun createNdefMessage(event: NfcEvent?): NdefMessage {
        val contact = mViewModel.contactList.value!![mContactPosition]
        val msg = NdefMessage(
                arrayOf(
                        createMime("application/vnd.com.app.anesabml.contactexchange", contact.name?.toByteArray()),
                        createMime("application/vnd.com.app.anesabml.contactexchange", contact.numbers[mNumberPosition]?.toByteArray())
                )
        )
        NdefRecord.createApplicationRecord("com.app.anesabml.contactexchange")
        return msg

    }

    override fun onResume() {
        super.onResume()
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            processIntent(intent)
        }
    }

    override fun onNdefPushComplete(event: NfcEvent?) {
        AlertDialog.Builder(this)
                .setMessage(R.string.contact_sent_successfully)
                .show()
    }

    override fun onNewIntent(intent: Intent?) {
        // onResume gets called after this to handle the intent
        setIntent(intent)
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

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    private fun processIntent(intent: Intent?) {
        val rawMsgs = intent?.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES)
        // only one message sent during the beam
        val msg = rawMsgs?.get(0) as NdefMessage
        // record 0 contains the MIME type, record 1 is the AAR, if present
        Log.d("Ndf msg", msg.records[0].payload.toString())

        val newContact = Contact(
                msg.records[0].payload.toString(),
                arrayListOf(msg.records[1].payload.toString()))

        mViewModel.saveContact(newContact)
    }

    private fun showNumbersDialog() {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                mViewModel.contactList.value!![mContactPosition].numbers)

        val builder = AlertDialog.Builder(this, R.style.Dialog_Light)
        builder.setAdapter(adapter) { _, which ->
            mNumberPosition = which
        }
        builder.show()
    }
}

interface RecyclerViewClickListener {
    fun recyclerViewListClicked(v: View?, position: Int)
}