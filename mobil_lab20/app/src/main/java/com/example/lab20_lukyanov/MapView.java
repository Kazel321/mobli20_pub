package com.example.lab20_lukyanov;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

public class MapView extends SurfaceView {

    public Activity ctx;
    public boolean pendling;

    public ArrayList<Tile> tiles = new ArrayList<>();
    public Timestamp cacheTime;
    public int[][] layerProp;
    Paint p;

    float lat0, lon0;
    float lat1, lon1;

    public void update_viewport()
    {
        lat0 = -offset_x * dpp[current_level_index] - 180.0f;
        lon0 = 90.0f + offset_y * dpp[current_level_index];
        lat1 = lat0 + (float)width * dpp[current_level_index];
        lon1 = lon0 - (float)height * dpp[current_level_index];
    }

    /*Tile getTile(int x, int y, int scale)
    {
        for (int i = 0; i < tiles.size(); i++)
        {
            Tile t = tiles.get(i);
            if (t.x == x && t.y == y && t.scale == scale) return t;
        }

        Timestamp currentTime = new Timestamp(System.currentTimeMillis());


        Tile nt = new Tile(x, y, scale, currentTime, ctx, this);
        tiles.add(nt);
        return nt;
    }*/

    Tile getTile(int x, int y, int scale)
    {

        for (int i = 0; i < tiles.size(); i++)
        {
            Tile t = tiles.get(i);
            if (t.x == x && t.y == y && t.scale == scale)
            {
                Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                Long timeDif = t.time.getTime() + cacheTime.getTime();
                if (!currentTime.after(new Timestamp(timeDif)))
                {
                    return t;
                }
            }
        }

        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        Tile nt = new Tile(x, y, scale, currentTime, ctx, this);
        tiles.add(nt);
        return nt;
    }

    float last_x;
    float last_y;

    public int current_level_index = 0;

    int[] levels = new int[] {16, 8, 4, 2, 1};
    int[] x_tiles = new int[] {54, 108, 216, 432, 864};
    int[] y_tiles = new int[] {27, 54, 108, 216, 432};
    float[] dpp = new float[] {
            360.0f / (86400 / 16),
            360.0f / (86400 / 8),
            360.0f / (86400 / 4),
            360.0f / (86400 / 2),
            360.0f / (86400 / 1)
    };
    String[] layers = new String[] {"/coastline/", "/river/", "/road/", "/railroad/"};

    int tile_width = 100;
    int tile_height = 100;

    float offset_x = 0.0f;
    float offset_y = 0.0f;

    int width, height;


    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);

        p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        //p.setColor(Color.RED);
        p.setStrokeWidth(5);

        for (int i = 0; i < isCreatePoints.length; i++)
        {
            isCreatePoints[i] = false;
            apiRes[i] = "";
        }

        setWillNotDraw(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int act = event.getAction();
        switch (act)
        {
            case MotionEvent.ACTION_DOWN:
                last_x = event.getX();
                last_y = event.getY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();

                float dx = x - last_x;
                float dy = y - last_y;

                offset_x += dx;
                offset_y += dy;

                update_viewport();
                pendling = true;

                invalidate();

                last_x = x;
                last_y = y;
                return true;

            case MotionEvent.ACTION_UP:
                for (int i = 0; i < isCreatePoints.length; i++)
                {
                    isCreatePoints[i] = false;
                }
                g.db.saveLastPosition(offset_x, offset_y, current_level_index);
                invalidate();
                return true;
        }
        return false;
        //return super.onTouchEvent(event);
    }

    Float map(Float x, Float x0, Float x1, Float a, Float b)
    {
        float t = (x - x0) / (x1 - x0);
        return a + (b - a) * t;
    }

    boolean rect_intersects_rect(
            float ax0, float ay0, float ax1, float ay1,
            float bx0, float by0, float bx1, float by1)
    {
        if (ax1 < bx0) return false;
        if (ax0 > bx1) return false;
        if (ay1 < by0) return false;
        if (ay0 > by1) return false;
        return true;
    }

    float layer_x0;
    float layer_y0;
    float layer_x1;
    float layer_y1;

    String http = "";

    String[] apiRes = new String[4];

    boolean[] isCreatePoints = new boolean[4];

    ArrayList<Float[]> coord;
    ArrayList<Points> layerPoints;
    ArrayList<Points>[] onLayerPoints = new ArrayList[4];
    @Override
    protected void onDraw(Canvas canvas) {
        //if (!pendling) return;

        width = canvas.getWidth();
        height = canvas.getHeight();

        canvas.drawColor(Color.WHITE);

        int screen_x0 = 0;
        int screen_y0 = 0;
        int screen_x1 = width - 1;
        int screen_y1 = height - 1;

        int w = x_tiles[current_level_index];
        int h = y_tiles[current_level_index];

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
            {
                int x0 = x * tile_width + (int) offset_x;
                int y0 = y * tile_height + (int) offset_y;
                int x1 = x0 + tile_width;
                int y1 = y0 + tile_height;
                if (rect_intersects_rect(screen_x0, screen_y0, screen_x1, screen_y1, x0, y0, x1, y1) == false) continue;
                //Log.println(Log.ASSERT, "" + current_level_index, "" + current_level_index);

                Tile t = getTile(x, y, levels[current_level_index]);
                if (t.bmp != null) canvas.drawBitmap(t.bmp, x0, y0, p);

            }

        for (int i = 0; i < layerProp.length; i++)
        {
            if (layerProp[i][1] == 1)
            {
                if (!isCreatePoints[i])
                {
                    http = layers[i] + levels[current_level_index] + "?lat0=" + lat0 + "&lon0=" + lon0 + "&lat1=" + lat1 + "&lon1=" + lon1;

                    //canvas.drawText("" + levels[current_level_index], width/2, height/2, p);

                    int asd = i;
                    ApiHelper req = new ApiHelper(ctx, canvas) {
                        @Override
                        public void on_ready(String res) {
                            if (!res.equals("[]")) {
                                isCreatePoints[asd] = true;
                                //apiRes[asd] = res;

                                layerPoints = new ArrayList<>();
                                JSONArray jsonArray = null;
                                try {
                                    jsonArray = new JSONArray(res);
                                }
                                catch (Exception ex)
                                {
                                    ex.printStackTrace();
                                }
                                for (int j = 0; j < jsonArray.length(); j++)
                                {
                                    JSONArray jsonArray1 = null;
                                    try {
                                        jsonArray1 = new JSONArray(jsonArray.getString(j));
                                    }
                                    catch (Exception ex)
                                    {
                                        ex.printStackTrace();
                                    }

                                    Points points = new Points(j);
                                    points.points = new ArrayList<>();
                                    for (int k = 0; k < jsonArray1.length(); k++)
                                    {
                                        try {
                                            JSONObject obj = jsonArray1.getJSONObject(k);
                                            //Log.println(Log.ASSERT, "obj", obj.toString());

                                            float x = Float.parseFloat(obj.getString("x"));
                                            float y = Float.parseFloat(obj.getString("y"));

                                            PointF point = new PointF(x, y);
                                            points.pointsAdd(point);
                                        }
                                        catch (Exception ex)
                                        {
                                            ex.printStackTrace();
                                        }
                                    }
                                    layerPoints.add(points);
                                }

                                onLayerPoints[asd] = layerPoints;
                                invalidate();
                            }
                        }
                    };
                    //Log.println(Log.ASSERT, "http", http);
                    req.send(http);
                }
                else
                {
                    p.setColor(layerProp[i][0]);
                    layerPoints = onLayerPoints[i];
                    for (int j = 0; j < layerPoints.size(); j++)
                    {
                        Points points = layerPoints.get(j);
                        for (int k = 0; k < points.points.size(); k++)
                        {
                            PointF pnt = points.getPoint(k);
                            layer_x1 = map(pnt.x, lat0, lat1, 0.0f, (float) width);
                            layer_y1 = map(pnt.y, lon0, lon1, 0.0f, (float) height);
                            //Log.println(Log.ASSERT, "laers", "layer_x1: " + layer_x1 + " layer_y1: " + layer_y1 + " layer_x0: " + layer_x0 + " layer_y0: " + layer_y0);

                            if (k != 0) {
                                Log.println(Log.ASSERT, "draw", "draw");
                                canvas.drawLine(layer_x0, layer_y0, layer_x1, layer_y1, p);
                                //canvas.drawText("" + levels[current_level_index], width / 2, height / 2, p);
                            }
                            layer_x0 = layer_x1;
                            layer_y0 = layer_y1;
                        }
                    }
                }
            }
        }
        //Log.println(Log.ASSERT, "draw", "draw");
        super.onDraw(canvas);
        pendling = false;
    }
/*
    class LayerPoints
    {
        ArrayList<Points> layerPoints = new ArrayList<>();
        public void layerPointsAdd(Points layerPoints)
        {
            this.layerPoints.add(layerPoints);
        }
    }*/

    class Points
    {
        int index;
        public ArrayList<PointF> points = new ArrayList<>();

        public Points(int index)
        {
            this.index = index;
        }

        public void pointsAdd(PointF point)
        {
            points.add(point);
        }

        public PointF getPoint(int index)
        {
            return points.get(index);
        }
    }

    public void prev()
    {
        if (current_level_index == 0) return;
        current_level_index--;

        offset_x += (float) width / 2;
        offset_y += (float) height / 2;

        offset_x /= 2;
        offset_y /= 2;

        pendling = true;
        invalidate();
    }

    public void next()
    {
        if (current_level_index == levels.length-1) return;
        current_level_index++;

        offset_x *= 2;
        offset_y *= 2;

        offset_x -= (float) width / 2;
        offset_y -= (float) height / 2;

        pendling = true;
        invalidate();
    }
}
