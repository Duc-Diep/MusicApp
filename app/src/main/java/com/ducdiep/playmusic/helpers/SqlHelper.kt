package com.ducdiep.playmusic.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.ducdiep.playmusic.models.songoffline.SongOffline
import com.ducdiep.playmusic.models.topsong.Song
import java.util.ArrayList

const val DB_NAME = "FavouriteMusic.db"
const val DB_TABLE_ON = "MusicOnline"
const val DB_TABLE_OFF = "MusicOffline"
const val DB_VERSION = 1
class SqlHelper(context: Context):SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    lateinit var sqlIteDatabase:SQLiteDatabase
    lateinit var contentValue: ContentValues
    override fun onCreate(db: SQLiteDatabase) {
        val queryCreateTableOnline = "CREATE TABLE $DB_TABLE_ON(code Text PRIMARY KEY)"
        val queryCreateTableOffline = "CREATE TABLE $DB_TABLE_OFF(uri Text PRIMARY KEY)"
        db.execSQL(queryCreateTableOnline)
        db.execSQL(queryCreateTableOffline)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (newVersion!=oldVersion){
            db.execSQL("DROP TABLE IF EXISTS $DB_TABLE_ON")
            db.execSQL("DROP TABLE IF EXISTS $DB_TABLE_OFF")
            onCreate(db)
        }
    }
    fun addSongOffline(songOffline: SongOffline) {
        sqlIteDatabase = writableDatabase
        contentValue = ContentValues()
        contentValue.put("uri", songOffline.resource)
        sqlIteDatabase.insert(DB_TABLE_OFF, null, contentValue)
    }
    fun addSongOnline(song: Song) {
        sqlIteDatabase = writableDatabase
        contentValue = ContentValues()
        contentValue.put("code", song.code)
        sqlIteDatabase.insert(DB_TABLE_ON, null, contentValue)
    }

    fun getAllCode():List<String>{
        val list: MutableList<String> = ArrayList()
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
            val code = cursor.getString(cursor.getColumnIndex("code"))
            list.add(code)
        }
        return list
    }
    fun getAllUrl():List<String>{
        val list: MutableList<String> = ArrayList()
        sqlIteDatabase = readableDatabase
        val cursor: Cursor = sqlIteDatabase.query(
            false,
            DB_TABLE_OFF,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val uri = cursor.getString(cursor.getColumnIndex("uri"))
            list.add(uri)
        }
        return list
    }

}