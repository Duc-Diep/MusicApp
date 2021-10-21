package com.ducdiep.playmusic.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.songresponse.Song
import java.util.ArrayList

const val DB_NAME = "FavouriteMusic.db"
const val DB_TABLE_ON = "MusicOnline"
const val DB_TABLE_OFF = "MusicOffline"
const val DB_VERSION = 1
class SqlHelper(context: Context):SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    lateinit var sqlIteDatabase:SQLiteDatabase
    lateinit var contentValue: ContentValues
    override fun onCreate(db: SQLiteDatabase) {
        val queryCreateTableOnline = "CREATE TABLE $DB_TABLE_ON(id Text PRIMARY KEY, code Text, artist Text, duration INTEGER, name Text, thumb Text, type Text )"
//        val queryCreateTableOffline = "CREATE TABLE $DB_TABLE_OFF(uri Text PRIMARY KEY)"
        db.execSQL(queryCreateTableOnline)
//        db.execSQL(queryCreateTableOffline)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (newVersion!=oldVersion){
            db.execSQL("DROP TABLE IF EXISTS $DB_TABLE_ON")
//            db.execSQL("DROP TABLE IF EXISTS $DB_TABLE_OFF")
            onCreate(db)
        }
    }
//    fun addSongOffline(songOffline: SongOffline) {
//        sqlIteDatabase = writableDatabase
//        contentValue = ContentValues()
//        contentValue.put("uri", songOffline.resource)
//        sqlIteDatabase.insert(DB_TABLE_OFF, null, contentValue)
//    }
    fun addSong(song: Song) {
        sqlIteDatabase = writableDatabase
        contentValue = ContentValues()
        contentValue.put("id", song.id)
        contentValue.put("code", song.code)
        contentValue.put("artist", song.artists_names)
        contentValue.put("duration", song.duration)
        contentValue.put("name", song.name)
        contentValue.put("thumb", song.thumbnail)
        contentValue.put("type", song.type)
        sqlIteDatabase.insert(DB_TABLE_ON, null, contentValue)
    }
    fun removeSong(id:String){
        sqlIteDatabase = writableDatabase
        sqlIteDatabase.delete(DB_TABLE_ON,"id = ?", arrayOf(id))
    }
    fun checkExists(id:String): Boolean {
        sqlIteDatabase = readableDatabase
        val cursor: Cursor = sqlIteDatabase.rawQuery(
            "SELECT * FROM $DB_TABLE_ON where id = ?",
            arrayOf(id)
        )
        return cursor.count == 1
    }
    fun removeOrAddSong(song:Song):Boolean{
        if (checkExists(song.id)){
            removeSong(song.id)
            return true
        }else{
            addSong(song)
            return false
        }
    }

    fun getAllSong():List<Song>{
        val list: MutableList<Song> = ArrayList()
        sqlIteDatabase = readableDatabase
        val cursor: Cursor = sqlIteDatabase.query(
            false,
            DB_TABLE_ON,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val artists = cursor.getString(cursor.getColumnIndex("artist"))
            val code = cursor.getString(cursor.getColumnIndex("code"))
            val duration = cursor.getInt(cursor.getColumnIndex("duration"))
            val id = cursor.getString(cursor.getColumnIndex("id"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            val thumb = cursor.getString(cursor.getColumnIndex("thumb"))
            val type = cursor.getString(cursor.getColumnIndex("type"))

            list.add(Song(artists, code, duration, id, name, 0,thumb, type))
        }
        return list
    }
//    fun getAllUrl():List<String>{
//        val list: MutableList<String> = ArrayList()
//        sqlIteDatabase = readableDatabase
//        val cursor: Cursor = sqlIteDatabase.query(
//            false,
//            DB_TABLE_OFF,
//            null,
//            null,
//            null,
//            null,
//            null,
//            null,
//            null
//        )
//        while (cursor.moveToNext()) {
//            val uri = cursor.getString(cursor.getColumnIndex("uri"))
//            list.add(uri)
//        }
//        return list
//    }

}