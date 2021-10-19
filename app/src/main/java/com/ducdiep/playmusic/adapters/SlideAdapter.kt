package com.ducdiep.playmusic.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.config.URL_THUMB
import com.ducdiep.playmusic.models.topsong.Song
import kotlinx.android.synthetic.main.slide_item.view.*

class SlideAdapter(var context: Context,var listSongOnline: List<Song>) : RecyclerView.Adapter<SlideAdapter.SlideViewHolder>(){
    var onClick:((Song)->Unit)? = null

    fun setOnClickItem(callBack:(Song)->Unit){
        onClick = callBack
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SlideViewHolder {
        var view = LayoutInflater.from(context).inflate(R.layout.slide_item,parent,false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        var song = listSongOnline[position]
        var linkImage = song.thumbnail.replaceFirst("w94_r1x1_jpeg/","")
            Glide.with(context).load("$linkImage").into(holder.image)
        holder.itemView.setOnClickListener {
            onClick?.invoke(song)
        }
    }

    override fun getItemCount(): Int {
        return listSongOnline.size
    }
    class SlideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image = itemView.findViewById<ImageView>(R.id.image_slide)
    }

}