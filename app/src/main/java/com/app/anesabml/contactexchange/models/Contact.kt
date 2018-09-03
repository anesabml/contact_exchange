package com.app.anesabml.contactexchange.models

import android.databinding.BindingAdapter
import android.widget.ImageView
import android.widget.TextView
import com.app.anesabml.contactexchange.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions


class Contact(var image: String?,
              var name: String?,
              var numbers: List<String?>) {

    companion object {
        @BindingAdapter("bind:image")
        @JvmStatic
        fun loadImage(view: ImageView, image: String?) {
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