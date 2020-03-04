package com.app.anesabml.contactexchange.models

import androidx.databinding.BindingAdapter
import android.widget.ImageView
import android.widget.TextView
import com.app.anesabml.contactexchange.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions


class Contact(var image: String?,
              var name: String?,
              var numbers: List<String?>) {

    var id: Int = 0

    constructor(name: String?, numbers: List<String?>) : this(null, name, numbers)

    constructor(id: Int, name: String?, numbers: List<String?>): this(null, name, numbers) {
        this.id = id
    }

    companion object {
        @BindingAdapter("bind:image", "bind:name")
        @JvmStatic
        fun loadImage(view: ImageView, image: String?, name: String?) {
            val requestOptions = RequestOptions()
            requestOptions.placeholder(R.drawable.ic_person)
            requestOptions.error(R.drawable.ic_person)

            Glide.with(view.context)
                    .setDefaultRequestOptions(requestOptions)
                    .load(image)
                    .into(view)
        }

        @BindingAdapter("bind:numbers")
        @JvmStatic
        fun showNumbers(view: TextView, numbers: List<String?>) {
            view.text = ""
            for (t in numbers) {
                view.append("$t\n")
            }
        }
    }
}