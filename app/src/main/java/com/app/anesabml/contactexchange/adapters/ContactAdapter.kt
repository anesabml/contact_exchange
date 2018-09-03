package com.app.anesabsml.nouveauleader

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import com.app.anesabml.contactexchange.models.Contact
import com.app.anesabml.contactexchange.databinding.ContactItemBinding
import android.view.animation.AnimationUtils
import android.view.animation.Animation


class ContactAdapter(private var mContacts: ArrayList<Contact>, var listener: View.OnClickListener)
    : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>(),
        Filterable {


    // Allows to remember the last item shown on screen
    private var lastPosition = -1

    private val mContactFilterList = mContacts
    private var mValueFilter = ValueFilter()


    override fun onCreateViewHolder(parent: ViewGroup, p: Int): ContactViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ContactItemBinding
                .inflate(layoutInflater, parent, false)

        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holderContact: ContactViewHolder, p: Int) {
        holderContact.bind(mContacts[p])
        setAnimation(holderContact.itemView, p)
        holderContact.itemView.setOnClickListener(listener)
    }

    override fun onViewDetachedFromWindow(holder: ContactViewHolder) {
        holder.clearAnimation()
    }

    override fun getItemCount(): Int =
            mContacts.size


    override fun getFilter(): Filter {
        return mValueFilter
    }

    class ContactViewHolder(private val binding: ContactItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(currentContact: Contact) {
            binding.contact = currentContact
            binding.executePendingBindings()
        }

        fun clearAnimation() {
            binding.root.clearAnimation()
        }

    }

    inner class ValueFilter : Filter() {

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = Filter.FilterResults()

            // Check if the query is null
            if (constraint != null && constraint.isNotEmpty()) {
                val filterList = ArrayList<Contact>()
                for (i in 0 until mContactFilterList.size) {
                    // Check contact names
                    if (mContactFilterList[i].name?.toUpperCase()?.contains(constraint.toString().toUpperCase())!!) {
                        filterList.add(mContactFilterList[i])
                    }
                    // Check contact number
                    mContactFilterList[i].numbers.forEach {
                        if (it?.contains(constraint)!!)
                            filterList.add(mContactFilterList[i])
                    }
                }
                results.count = filterList.size
                results.values = filterList
            } else {
                results.count = mContactFilterList.size
                results.values = mContactFilterList
            }
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            mContacts = results?.values as ArrayList<Contact>
            notifyDataSetChanged()
        }

    }

    /**
     * The key method to apply the animation
     */
    private fun setAnimation(viewToAnimate: View, position: Int) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(viewToAnimate.context, android.R.anim.slide_in_left)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }
}