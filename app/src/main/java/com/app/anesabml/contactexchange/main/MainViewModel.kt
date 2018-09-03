package com.app.anesabml.contactexchange.main

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.app.anesabml.contactexchange.models.Contact
import java.util.ArrayList

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var repository = MainRepository(getApplication<Application>().contentResolver)
    var contactList = MutableLiveData<ArrayList<Contact>>()

    fun getContactsList() {
        contactList.value = repository.getContacts()

    }
}