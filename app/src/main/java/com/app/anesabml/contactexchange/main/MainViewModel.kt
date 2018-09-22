package com.app.anesabml.contactexchange.main

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.databinding.ObservableField
import android.net.Uri
import com.app.anesabml.contactexchange.uimodels.Contact
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableMaybeObserver
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var repository = MainRepository(getApplication<Application>().contentResolver)
    val isLoading = ObservableField<Boolean>(false)
    var contactList = MutableLiveData<ArrayList<Contact>>()
    var errorText: String? = null
    var isContactSaved = MutableLiveData<Boolean>()
    private val compositeDisposable = CompositeDisposable()

    fun getContactsList() {
        isLoading.set(true)
        compositeDisposable.add(repository.getContacts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<ArrayList<Contact>>() {
                    override fun onComplete() {
                        isLoading.set(false)
                    }

                    override fun onNext(t: ArrayList<Contact>) {
                        contactList.value = t
                    }

                    override fun onError(e: Throwable) {
                        errorText = e.message
                    }
                }))
    }

    fun saveContact(contact: Contact) {
        isLoading.set(true)
        compositeDisposable.add(repository.insertContact(contact)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableMaybeObserver<Uri>() {
                    override fun onSuccess(t: Uri) {
                        isContactSaved.value = true
                    }

                    override fun onComplete() {
                        isLoading.set(false)
                    }

                    override fun onError(e: Throwable) {
                        isContactSaved.value = false
                    }
                }))
    }
}