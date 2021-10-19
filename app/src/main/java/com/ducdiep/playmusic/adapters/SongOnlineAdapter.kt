package com.ducdiep.playmusic.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.config.URL_THUMB
import com.ducdiep.playmusic.models.topsong.Song

class SongOnlineAdapter(var context: Context,var listSong:List<Song>):RecyclerView.Adapter<SongOnlineAdapter.SongviewHolder>() {
    var onClick:((Song)->Unit)?=null

    fun setOnClickItem(callBack:(Song)->Unit){
        onClick = callBack
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SongviewHolder {
        var view = LayoutInflater.from(context).inflate(R.layout.song_online_item,parent,false)
        return SongviewHolder(view)
    }

    override fun onBindViewHolder(holder: SongviewHolder, position: Int) {
        var song = listSong[position]
        Glide.with(context).load("${song.thumbnail}").into(holder.image)
        holder.tvName.text = song.name
        holder.tvName.setTextColor(Color.WHITE)
        holder.tvArtist.text = song.artists_names
        holder.tvArtist.setTextColor(Color.WHITE)
        holder.itemView.setOnClickListener {
            onClick?.invoke(song)
        }
    }

    override fun getItemCount(): Int {
        return listSong.size
    }
    class SongviewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image = itemView.findViewById<ImageView>(R.id.img_song_online_item)
        var tvName = itemView.findViewById<TextView>(R.id.tv_song_online_name_item)
        var tvArtist = itemView.findViewById<TextView>(R.id.tv_song_online_artist_item)
    }
}