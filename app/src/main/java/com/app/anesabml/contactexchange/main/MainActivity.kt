package com.app.anesabml.contactexchange.main

import android.Manifest
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import com.app.anesabml.contactexchange.databinding.ActivityMainBinding
import com.app.anesabsml.nouveauleader.ContactAdapter
import com.app.anesabml.contactexchange.utility.CustomDividerItem
import com.app.anesabml.contactexchange.R
import android.view.View
import android.content.Intent
import android.nfc.*
import android.provider.Settings
import android.nfc.NdefRecord.createMime
import android.nfc.NdefRecord
import android.nfc.NdefMessage
import android.databinding.adapters.TextViewBindingAdapter.setText
import android.nfc.NfcAdapter
import android.os.Parcelable
import android.util.Log
import android.widget.TextView






class MainActivity : AppCompatActivity(),
        View.OnClickListener,
        NfcAdapter.CreateNdefMessageCallback {

    companion object {
        const val PERMISSIONS_REQUEST_READ_CONTACTS: Int = 1
    }

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mViewModel: MainViewModel

    private lateinit var mAdapter: ContactAdapter

    private lateinit var mNfcAdapter: NfcAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setSupportActionBar(mBinding.toolbar)

        mViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        checkContactPermission()

        mBinding.contactsList.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        mBinding.contactsList.addItemDecoration(
                CustomDividerItem(this, LinearLayoutManager.VERTICAL, 72))

        mViewModel.contactList.observe(this, Observer {
            mAdapter = ContactAdapter(it!!, this)
            mBinding.contactsList.adapter = mAdapter
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

    private fun checkContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_CONTACTS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_CONTACTS),
                        PERMISSIONS_REQUEST_READ_CONTACTS)
            }
        } else {
            mViewModel.getContactsList()
        }
    }

    override fun onClick(v: View?) {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        mNfcAdapter.setNdefPushMessageCallback(this, this)
        if (!mNfcAdapter.isEnabled) {
            showEnableLocationDialog()
        }
    }

    override fun createNdefMessage(event: NfcEvent?): NdefMessage {
        val text = "Beam me up, Android!\n\n" +
                "Beam Time: " + System.currentTimeMillis()
        val msg = NdefMessage(
                arrayOf(createMime(
                        "application/vnd.com.app.anesabml.contactexchange", text.toByteArray()))
                /**
                 * The Android Application Record (AAR) is commented out. When a device
                 * receives a push with an AAR in it, the application specified in the AAR
                 * is guaranteed to run. The AAR overrides the tag dispatch system.
                 * You can add it back in to guarantee that this
                 * activity starts when receiving a beamed message. For now, this code
                 * uses the tag dispatch system.
                 *///,NdefRecord.createApplicationRecord("com.example.android.beam")
        )
        NdefRecord.createApplicationRecord("com.app.anesabml.contactexchange")
        return msg

    }

    private fun showEnableLocationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.enable_nfc_msg)
        builder.setPositiveButton(R.string.enable, { _, _ ->
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        })
        builder.setNegativeButton(R.string.cancel, { dialogInterface, _ ->
            dialogInterface.dismiss()
        })
        builder.show()
    }

    override fun onResume() {
        super.onResume()
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            processIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        // onResume gets called after this to handle the intent
        setIntent(intent)
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
    }

}
