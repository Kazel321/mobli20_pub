package com.example.lab20_lukyanov;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONObject;

import java.sql.Timestamp;

public class Tile {

    public int scale;
    public int x;
    public int y;
    public Bitmap bmp;
    public Timestamp time;

    public Tile()
    {

    }

    public Tile(int x, int y, int scale, Bitmap bmp, Timestamp time)
    {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.bmp = bmp;
        this.time = time;
    }

    public Tile(int x, int y, int scale, Timestamp time, Activity ctx, MapView map) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.time = time;
        this.bmp = null;

        ApiHelper req = new ApiHelper(ctx) {
            @Override
            public void on_ready(String res) {
                //Log.println(Log.ASSERT, "in", "/raster/" + String.format("%d/%d-%d", scale, x, y));
                try {
                    JSONObject obj = new JSONObject(res);
                    String b64 = obj.getString("data");
                    byte[] jpeg = Base64.decode(b64, Base64.DEFAULT);
                    bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                    g.db.addTile(new Tile(x, y, scale, bmp, time));
                    map.pendling = true;
                    map.invalidate();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        };
        req.send("/raster/" + String.format("%d/%d-%d", scale, x, y));
    }
}
