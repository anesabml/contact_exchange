package com.app.anesabsml.nouveauleader

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import com.app.anesabml.contactexchange.models.Contact
import com.app.anesabml.contactexchange.databinding.ContactItemBinding
import com.app.anesabml.contactexchange.main.RecyclerViewClickListener


class ContactAdapter(private var mContacts: ArrayList<Contact>,
                     private var listener: RecyclerViewClickListener)
    : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>(),
        Filterable {

    private var mContactFilterList = mContacts
    private var mValueFilter = ValueFilter()

    inner class ContactViewHolder(private val binding: ContactItemBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        init {
            binding.root.setOnClickListener(this)
        }
        fun bind(currentContact: Contact) {
            binding.contact = currentContact
            binding.executePendingBindings()
        }
        override fun onClick(v: View?) {
            listener.recyclerViewListClicked(v, adapterPosition)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, p: Int): ContactViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ContactItemBinding
                .inflate(layoutInflater, parent, false)

        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holderContact: ContactViewHolder, p: Int) {
        holderContact.bind(mContacts[p])
    }

    override fun getItemCount(): Int =
            mContacts.size


    override fun getFilter(): Filter {
        return mValueFilter
    }

    fun replaceData(arrayList: java.util.ArrayList<Contact>) {
        mContacts = arrayList
        mContactFilterList = arrayList
        notifyDataSetChanged()
    }

    inner class ValueFilter : Filter()  {

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

}