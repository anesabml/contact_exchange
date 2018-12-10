package com.app.anesabml.contactexchange.receiver

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.databinding.ObservableField
import android.net.Uri
import com.app.anesabml.contactexchange.uimodels.Contact
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableMaybeObserver
import io.reactivex.schedulers.Schedulers

class ReceiverViewModel(application: Application) : AndroidViewModel(application) {

    var repository = ReceiverRepository(getApplication<Application>().contentResolver)
    val isLoading = ObservableField<Boolean>(false)
    var isContactSaved = MutableLiveData<Boolean>()
    private val compositeDisposable = CompositeDisposable()

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