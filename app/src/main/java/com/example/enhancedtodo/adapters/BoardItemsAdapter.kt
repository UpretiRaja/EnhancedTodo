package com.example.enhancedtodo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.enhancedtodo.R
import com.example.enhancedtodo.models.Board

open class BoardItemsAdapter(
    private val context: Context,
    private val list: ArrayList<Board>
) :
    RecyclerView.Adapter<ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item_board, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {
            Glide
                .with(context)
                .load(model.image)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(holder.itemView.findViewById(R.id.iv_board_image_Main))

            holder.itemView.findViewById<TextView>(R.id.tv_name).text = model.name
            holder.itemView.findViewById<TextView>(R.id.tv_created_by).text =
                "Created by: ${model.createdBy}"

            holder.itemView.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick(position, model)
                }
            }

        }
    }

    interface OnClickListener {
        fun onClick(position: Int, model: Board)
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    private class MyViewHolder(view: View) : ViewHolder(view)

}
