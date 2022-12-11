package com.beautybarber.app.ui.activities.streaming

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.beautybarber.app.databinding.LiveMsgLayoutBinding
import com.beautybarber.app.models.Message

class LiveMsgAdapter ():
    RecyclerView.Adapter<LiveMsgAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LiveMsgLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }


    override fun onBindViewHolder(holder: LiveMsgAdapter.ViewHolder, position: Int) {
        val model=messages[position]
        holder.binding.apply {
            nameText.text=model.name
            messageText.text=model.message
        }
    }

    override fun getItemCount(): Int = messages.size

//    interface OnItemClickListener{
//        fun onItemClick(article: Article)
//    }

    inner class ViewHolder(val binding: LiveMsgLayoutBinding): RecyclerView.ViewHolder(binding.root)

    private val differCallback=object : DiffUtil.ItemCallback<Message>(){
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.timeInMillis==newItem.timeInMillis
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem==newItem
    }
    private val differ=AsyncListDiffer(this,differCallback)
    var messages:List<Message>
        get() = differ.currentList
        set(value){differ.submitList(value)}
}