package com.app.anesabml.contactexchange.main

import android.content.ContentResolver
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.util.Log
import com.app.anesabml.contactexchange.models.Contact
import android.R.attr.phoneNumber
import java.util.Collections.replaceAll


class MainRepository(var contentResolver: ContentResolver) {

    companion object {
        const val TAG = "MainRepository"
    }

    fun getContacts(): ArrayList<Contact> {

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

        return contacts
    }
}