package com.ducdiep.playmusic.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ducdiep.playmusic.R
import com.ducdiep.playmusic.models.Song

class SongAdapter(var context: Context, var listSong: ArrayList<Song>) :
    RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
    var onClick: ((Song) -> Unit)? = null

    fun setOnClickItem(callBack:(Song)->Unit){
        onClick = callBack
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        var view = LayoutInflater.from(context).inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        var song = listSong[position]
        holder.tvSongName.text = song.name
        holder.tvSongArtist.text = song.artist
        holder.imgSong.setImageBitmap(song.imageBitmap)
        holder.imgPlay.setOnClickListener {
            onClick?.invoke(song)
        }
    }

    override fun getItemCount(): Int {
        return listSong.size
    }

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvSongName = itemView.findViewById<TextView>(R.id.tv_song_item_name)
        var tvSongArtist = itemView.findViewById<TextView>(R.id.tv_song_item_artist)
        var imgPlay = itemView.findViewById<ImageView>(R.id.btn_play_item)
        var imgSong = itemView.findViewById<ImageView>(R.id.img_song_item)
    }

}