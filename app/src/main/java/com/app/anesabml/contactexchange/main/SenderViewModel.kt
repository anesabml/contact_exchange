package com.app.anesabml.contactexchange.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.databinding.ObservableField
import com.app.anesabml.contactexchange.models.Contact
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.util.*

class SenderViewModel(application: Application) : AndroidViewModel(application) {

    var repository = ReceiverRepository(getApplication<Application>().contentResolver)
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


}