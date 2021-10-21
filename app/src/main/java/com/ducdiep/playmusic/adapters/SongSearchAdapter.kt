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
import com.ducdiep.playmusic.config.URL_THUMB
import com.ducdiep.playmusic.models.search.SongSearch

class SongSearchAdapter(var context: Context, var listSongSearch:List<SongSearch>):RecyclerView.Adapter<SongSearchAdapter.SongViewHolder>() {
    var onClick: ((SongSearch)->Unit)?=null

    fun setOnClickItem(callBack:(SongSearch)->Unit){
        onClick = callBack
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SongViewHolder {
        var view = LayoutInflater.from(context).inflate(R.layout.song_online_item,parent,false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        var song = listSongSearch[position]
        Glide.with(context).load("$URL_THUMB${song.thumb}").into(holder.image)
        holder.tvName.text = song.name
        holder.tvArtist.text = song.artist
        holder.itemView.setOnClickListener {
            onClick?.invoke(song)
        }
    }

    override fun getItemCount(): Int {
        return listSongSearch.size
    }
    class SongViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView) {
        var image = itemView.findViewById<ImageView>(R.id.img_song_online_item)
        var tvName = itemView.findViewById<TextView>(R.id.tv_song_online_name_item)
        var tvArtist = itemView.findViewById<TextView>(R.id.tv_song_online_artist_item)
    }
}