package com.ducdiep.playmusic.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.models.topsong.Song
import kotlinx.android.synthetic.main.top_song_item_horizontal.view.*

class TopSongHoriAdapter(var context:Context, var listSong:List<Song>):
    RecyclerView.Adapter<TopSongHoriAdapter.SongViewHolder>() {
    var onClick:((Song)->Unit)? = null

    fun setOnClickItem(callBack:(Song)->Unit){
        onClick = callBack
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SongViewHolder {
        var view =LayoutInflater.from(context).inflate(R.layout.top_song_item_horizontal,parent,false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        var song = listSong[position]
        holder.tvName.text = "${song.position}.${song.name}"
        holder.tvName.isSelected = true
        var linkImage = song.thumbnail.replaceFirst("w94_r1x1_jpeg/","")
        Glide.with(context).load("$linkImage").into(holder.image)
        holder.itemView.setOnClickListener {
            onClick?.invoke(song)
        }
    }

    override fun getItemCount(): Int {
        return listSong.size
    }
    class SongViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView) {
        var tvName = itemView.findViewById<TextView>(R.id.tv_top_song_item_horizontal_name)
        var image = itemView.findViewById<ImageView>(R.id.img_top_song_horizontal_item)
    }
}