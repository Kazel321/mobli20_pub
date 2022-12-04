package com.example.lab20_lukyanov;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import java.sql.Timestamp;

public class MainActivity extends AppCompatActivity {

    MapView mapView;
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        g.db = new DB(this, "tile.db", null, 1);

        float[] lastposition = g.db.getLastPosition();

        mapView = findViewById(R.id.mapView2);

        if (lastposition != null) {
            mapView.offset_x = lastposition[0];
            mapView.offset_y = lastposition[1];
            mapView.current_level_index = (int) lastposition[2];
        }

        g.db.deleteOldTiles();
        g.db.getAllTiles(mapView.tiles);
        mapView.update_viewport();
        mapView.layerProp = g.db.getLayerProperties();
        mapView.cacheTime = new Timestamp(g.db.getCacheTime());
        mapView.ctx = this;
        mapView.pendling = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        mapView.tiles.clear();
        g.db.deleteOldTiles();
        g.db.getAllTiles(mapView.tiles);
        mapView.layerProp = g.db.getLayerProperties();
        mapView.cacheTime = new Timestamp(g.db.getCacheTime());
        mapView.invalidate();
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void on_settings_start(View v)
    {
        i = new Intent(this, SettingsActivity.class);
        startActivityForResult(i, 123);
    }

    public void on_scale_up(View v)
    {
        mapView.next();
    }

    public void on_scale_down(View v)
    {
        mapView.prev();
    }
}