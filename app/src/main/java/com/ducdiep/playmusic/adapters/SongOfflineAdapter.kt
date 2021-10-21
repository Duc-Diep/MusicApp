package com.ducdiep.playmusic.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.models.songoffline.SongOffline

class SongOfflineAdapter(var context: Context, var listSongOffline: ArrayList<SongOffline>) :
    RecyclerView.Adapter<SongOfflineAdapter.SongViewHolder>() {
    var onClick: ((SongOffline) -> Unit)? = null

    fun setOnClickItem(callBack:(SongOffline)->Unit){
        onClick = callBack
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        var view = LayoutInflater.from(context).inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        var song = listSongOffline[position]
        holder.tvSongName.text = song.name
        holder.tvSongArtist.text = song.artist
        holder.imgSong.setImageBitmap(song.imageBitmap)
        holder.itemView.setOnClickListener {
            onClick?.invoke(song)
        }
    }

    override fun getItemCount(): Int {
        return listSongOffline.size
    }

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvSongName = itemView.findViewById<TextView>(R.id.tv_song_item_name)
        var tvSongArtist = itemView.findViewById<TextView>(R.id.tv_song_item_artist)
        var imgSong = itemView.findViewById<ImageView>(R.id.img_song_item)
    }

}