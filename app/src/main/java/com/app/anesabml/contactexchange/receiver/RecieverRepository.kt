package com.app.anesabml.contactexchange.receiver

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract
import com.app.anesabml.contactexchange.uimodels.Contact
import io.reactivex.Maybe

class ReceiverRepository(var contentResolver: ContentResolver) {

    fun insertContact(contact: Contact) : Maybe<Uri> {
        val values = ContentValues()
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        values.put(ContactsContract.Data.RAW_CONTACT_ID, contact.id)
        values.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.name)
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.numbers.first())
        values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)

        val dataUri = contentResolver.insert(ContactsContract.Data.CONTENT_URI, values)
        return Maybe.just(dataUri)
    }
}