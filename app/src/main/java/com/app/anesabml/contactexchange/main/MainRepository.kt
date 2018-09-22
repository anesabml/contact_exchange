package com.app.anesabml.contactexchange.main

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Data
import com.app.anesabml.contactexchange.uimodels.Contact
import io.reactivex.Maybe
import io.reactivex.Observable


class MainRepository(var contentResolver: ContentResolver) {

    companion object {
        const val TAG = "MainRepository"
    }

    fun getContacts(): Observable<ArrayList<Contact>> {

        val contacts = ArrayList<Contact>()

        // Get cursor that include contacts order by name
        val sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, sortOrder)

        // Get contact row info
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            val image = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))
            val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

            // Getting the numbers
            val hasNumber = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
            if (hasNumber > 0) {
                val phoneCursor = contentResolver.query(Phone.CONTENT_URI,
                        null,
                        Phone.CONTACT_ID + " =?",
                        arrayOf(id), null)

                val numbersList = ArrayList<String?>()
                while (phoneCursor.count > 0 && phoneCursor.moveToNext()) {
                    val number = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.NUMBER))
                    numbersList.add(number.replace("\\s".toRegex(), ""))
                }
                // Adding a new contact to the list
                contacts.add(Contact(image, name, numbersList.distinct()))
                phoneCursor.close()
            }
        }
        cursor.close()

        return Observable.just(contacts)
    }

    fun insertContact(contact: Contact) : Maybe<Uri> {
        val values = ContentValues()
        values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
        values.put(Phone.DISPLAY_NAME, contact.name)
        values.put(Phone.PHOTO_URI, contact.image)
        values.put(Phone.NUMBER, contact.numbers.first())
        values.put(Phone.TYPE, Phone.TYPE_MOBILE)

        val dataUri = contentResolver.insert(Data.CONTENT_URI, values)
        return Maybe.just(dataUri)
    }
}