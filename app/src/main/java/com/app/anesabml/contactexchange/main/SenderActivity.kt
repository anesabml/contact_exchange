package com.app.anesabml.contactexchange.main

import android.Manifest
import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.app.anesabml.contactexchange.OutcomingNfcManager
import com.app.anesabml.contactexchange.R
import com.app.anesabml.contactexchange.databinding.ActivitySenderBinding
import com.app.anesabml.contactexchange.uimodels.Contact
import com.app.anesabsml.nouveauleader.ContactAdapter


class SenderActivity : AppCompatActivity(),
        RecyclerViewClickListener,
        OutcomingNfcManager.NfcActivity {


    val PERMISSIONS_REQUEST_READ_CONTACTS: Int = 1


    private lateinit var mBinding: ActivitySenderBinding
    private lateinit var mViewModel: SenderViewModel

    private var mAdapter = ContactAdapter(arrayListOf(), this)
    private var mContactPosition: Int = -1
    private var mNumberPosition: Int = -1

    private var mNfcAdapter: NfcAdapter? = null

    private lateinit var outcomingNfcCallback: OutcomingNfcManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_sender)

        mViewModel = ViewModelProviders.of(this).get(SenderViewModel::class.java)

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBinding.contactsList.setOnScrollChangeListener { _, _, _, _, _ ->
                mBinding.searchHolder.isSelected = mBinding.contactsList.canScrollVertically(-1)
            }
        }

        outcomingNfcCallback = OutcomingNfcManager(this)
        //Check if NFC is available on device
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (mNfcAdapter != null) {
            //This will refer back to createNdefMessage for what it will send
            mNfcAdapter!!.setNdefPushMessageCallback(outcomingNfcCallback, this)
            //This will be called if the message is sent successfully
            mNfcAdapter!!.setOnNdefPushCompleteCallback(outcomingNfcCallback, this);
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

    override fun onNewIntent(intent: Intent?) {
        // onResume gets called after this to handle the intent
        this.intent = intent
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
        builder.setAdapter(adapter) { _, which ->
            mNumberPosition = which
        }
        builder.show()
    }

    override fun getOutcomingMessage(): Contact {
        return mViewModel.contactList.value!![mContactPosition]
    }

    override fun signalResult() {
        // this will be triggered when NFC message is sent to a device.
        // should be triggered on UI thread. We specify it explicitly
        // cause onNdefPushComplete is called from the Binder thread
        runOnUiThread {
            Toast.makeText(this, R.string.message_beaming_complete, Toast.LENGTH_SHORT).show()
        }
    }
}

interface RecyclerViewClickListener {
    fun recyclerViewListClicked(v: View?, position: Int)
}