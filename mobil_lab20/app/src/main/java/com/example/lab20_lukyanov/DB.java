package com.example.lab20_lukyanov;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;

public class DB extends SQLiteOpenHelper {
    public DB(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    String sql;

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE Tile (x INT, y INT, scale INT, data TEXT, time TIMESTAMP);";
        db.execSQL(sql);
        sql = "CREATE TABLE LastPosition (offset_x FLOAT, offset_y FLOAT, scale INT);";
        db.execSQL(sql);
        sql = "CREATE TABLE CacheTime (time TIMESTAMP, unit TEXT);";
        db.execSQL(sql);
        sql = "INSERT INTO CacheTime VALUES (93720368, 'd');";
        db.execSQL(sql);
        sql = "CREATE TABLE APIEndPoint (endpoint TEXT);";
        db.execSQL(sql);
        sql = "CREATE TABLE Layer (layer INT, name TEXT, color INT, checked INT);";
        db.execSQL(sql);
        sql = "INSERT INTO Layer VALUES (0, 'coastline', " + Color.BLACK + ", 0);";
        db.execSQL(sql);
        sql = "INSERT INTO Layer VALUES (1, 'river', " + Color.BLACK + ", 0);";
        db.execSQL(sql);
        sql = "INSERT INTO Layer VALUES (2, 'road', " + Color.BLACK + ", 0);";
        db.execSQL(sql);
        sql = "INSERT INTO Layer VALUES (3, 'railroad', " + Color.BLACK + ", 0);";
        db.execSQL(sql);
    }

    public void addTile(Tile tile)
    {
        SQLiteDatabase db = getWritableDatabase();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        tile.bmp.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        String image = Base64.encodeToString(bytes, Base64.DEFAULT);
        String sql = "INSERT INTO Tile VALUES (" + tile.x + ", " + tile.y + ", " + tile.scale + ", '" + image + "', " + tile.time.getTime() + ");";
        db.execSQL(sql);
    }

    public Tile getTile(int x, int y, int scale)
    {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM Tile WHERE x = " + x + " AND y = " + y + " AND scale = " + scale + ";";
        Cursor cur = db.rawQuery(sql, null);
        if (cur.moveToFirst())
        {
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            Timestamp saveTime = new Timestamp(cur.getLong(4));
            Timestamp ts2 = new Timestamp(g.db.getCacheTime());
            Long timeDif = saveTime.getTime() + g.db.getCacheTime();
            if (ts.after(new Timestamp(timeDif)))
            {
                SQLiteDatabase dbWrite = getWritableDatabase();
                String sql2 = "DELETE FROM Tile WHERE x = " + x + " AND y = " + y + " AND scale = " + scale + ";";
                dbWrite.execSQL(sql2);
                return null;
            }
            Tile tile = new Tile();
            tile.x = cur.getInt(0);
            tile.y = cur.getInt(1);
            tile.scale = cur.getInt(2);
            String b64 = cur.getString(3);
            byte[] jpeg = Base64.decode(b64, Base64.DEFAULT);
            tile.bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
            tile.time = ts;
            return tile;
        }
        return null;
    }

    public void deleteTile(int x, int y, int scale)
    {
        SQLiteDatabase db = getWritableDatabase();
        sql = "DELETE FROM Tile WHERE x = " + x + " AND y = " + y + " AND scale = " + scale + ";";
        db.execSQL(sql);
    }

    public void deleteALlTile()
    {
        SQLiteDatabase db = getWritableDatabase();
        sql = "DELETE FROM Tile;";
        db.execSQL(sql);
    }

    public void getAllTiles(ArrayList<Tile> lst)
    {
        SQLiteDatabase db = getReadableDatabase();
        sql = "SELECT * FROM Tile;";
        Cursor cur = db.rawQuery(sql, null);
        if (cur.moveToFirst())
        {
            do {
                Tile tile = new Tile();
                tile.x = cur.getInt(0);
                tile.y = cur.getInt(1);
                tile.scale = cur.getInt(2);
                String b64 = cur.getString(3);
                byte[] jpeg = Base64.decode(b64, Base64.DEFAULT);
                tile.bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                tile.time = new Timestamp(cur.getLong(4));
                lst.add(tile);
            } while (cur.moveToNext());
        }
    }

    public void deleteOldTiles()
    {
        SQLiteDatabase db = getWritableDatabase();
        sql = "SELECT * FROM Tiles;";
        Cursor cur = db.rawQuery(sql, null);
        if (cur.moveToFirst())
        {
            ArrayList<Tile> tiles = new ArrayList<>();
            do
            {
                Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                Timestamp savedTime = new Timestamp(cur.getLong(4));
                Long timeDif = savedTime.getTime() + getCacheTime();
                if (currentTime.after(new Timestamp(timeDif)))
                {
                    Tile tile = new Tile();
                    tile.x = cur.getInt(0);
                    tile.y = cur.getInt(1);
                    tile.scale = cur.getInt(2);
                    tiles.add(tile);
                }
            } while (cur.moveToNext());
            for (int i = 0; i < tiles.size(); i++)
            {
                Tile t = tiles.get(i);
                g.db.deleteTile(t.x, t.y, t.scale);
            }
        }
    }

    public void saveLastPosition(Float offset_x, Float offset_y, int scale)
    {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "DELETE FROM LastPosition;";
        db.execSQL(sql);
        sql = "INSERT INTO LastPosition VALUES (" + offset_x + ", " + offset_y + ", " + scale + ");";
        db.execSQL(sql);
    }

    public float[] getLastPosition()
    {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM LastPosition;";
        Cursor cur = db.rawQuery(sql, null);
        if (cur.moveToFirst())
        {
            float[] last = new float[3];
            last[0] = cur.getFloat(0);
            last[1] = cur.getFloat(1);
            last[2] = (float)cur.getInt(2);
            return last;
        }
        return null;
    }

    public void saveCacheTime(Timestamp time, char unit)
    {
        SQLiteDatabase db = getWritableDatabase();
        sql = "DELETE FROM CacheTime;";
        db.execSQL(sql);
        sql = "INSERT INTO CacheTime VALUES (" + time.getTime() + ", '" + unit + "');";
        db.execSQL(sql);
    }

    public Long getCacheTime()
    {
        SQLiteDatabase db = getReadableDatabase();
        sql = "SELECT * FROM CacheTime;";
        Cursor cur = db.rawQuery(sql, null);
        if (cur.moveToFirst()) return cur.getLong(0);
        return null;
    }

    public char getCacheTimeUnit()
    {
        char unit = 0;
        SQLiteDatabase db = getReadableDatabase();
        sql = "SELECT * FROM CacheTime;";
        Cursor cur = db.rawQuery(sql, null);
        if (cur.moveToFirst())
        {
            unit = cur.getString(1).charAt(0);
        }
        return unit;
    }

    public void saveLayer(int layer, int color, int checked)
    {
        SQLiteDatabase db = getWritableDatabase();
        sql = "UPDATE Layer SET color = " + color + ", checked = " + checked + " WHERE layer = " + layer + ";";
        db.execSQL(sql);
    }

    public int[][] getLayerProperties()
    {
        SQLiteDatabase db = getReadableDatabase();
        sql = "SELECT * FROM Layer;";
        Cursor cur = db.rawQuery(sql, null);
        if (cur.moveToFirst())
        {
            int[][] layers = new int[4][4];
            for (int i = 0; i < 4; i++) {
                layers[i][0] = cur.getInt(2);
                layers[i][1] = cur.getInt(3);
                cur.moveToNext();
            }
            return layers;
        }
        return null;
    }

    public void saveEndPoint(String endpoint)
    {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "DELETE FROM APIEndPoint;";
        db.execSQL(sql);
        sql = "INSERT INTO APIEndPoint VALUES ('" + endpoint + "');";
        db.execSQL(sql);
    }

    public String getEndPoint()
    {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM APIEndPoint;";
        Cursor cur = db.rawQuery(sql, null);
        if (cur.moveToFirst())
        {
            String endpoint = cur.getString(0);
            return endpoint;
        }
        else return null;
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
