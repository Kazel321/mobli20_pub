package com.example.lab20_lukyanov;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.sql.Timestamp;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    EditText txtAPI;
    EditText txtCacheTime;

    CheckBox chkCoast;
    CheckBox chkRiver;
    CheckBox chkRoad;
    CheckBox chkRail;

    Spinner spnCoastline;
    Spinner spnRiver;
    Spinner spnRoad;
    Spinner spnRailroad;

    ArrayAdapter<IntColor> colors;

    int[][] layers;

    //String[] colorNames = new String[] {"WHITE", "BLACK", "RED", "BLUE", "GREEN", "YELLOW", "MAGENTA"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        txtAPI = findViewById(R.id.txtAPI);
        txtCacheTime = findViewById(R.id.txtCacheTime);

        chkCoast = findViewById(R.id.chkCoastline);
        chkRiver = findViewById(R.id.chkRiver);
        chkRoad = findViewById(R.id.chkRoad);
        chkRail = findViewById(R.id.chkRailroad);

        spnCoastline = findViewById(R.id.spnCoastline);
        spnRiver = findViewById(R.id.spnRiver);
        spnRoad = findViewById(R.id.spnRoad);
        spnRailroad = findViewById(R.id.spnRailroad);

        colors = new ArrayAdapter<IntColor>(this, android.R.layout.simple_list_item_1);
        colors.add(new IntColor(Color.WHITE, "WHITE"));
        colors.add(new IntColor(Color.BLACK, "BLACK"));
        colors.add(new IntColor(Color.RED, "RED"));
        colors.add(new IntColor(Color.BLUE, "BLUE"));
        colors.add(new IntColor(Color.GREEN, "GREEN"));
        colors.add(new IntColor(Color.YELLOW, "YELLOW"));
        colors.add(new IntColor(Color.MAGENTA, "MAGENTA"));

        spnCoastline.setAdapter(colors);
        spnRiver.setAdapter(colors);
        spnRoad.setAdapter(colors);
        spnRailroad.setAdapter(colors);

        layers = g.db.getLayerProperties();
        setSelected(spnCoastline, chkCoast);
        setSelected(spnRiver, chkRiver);
        setSelected(spnRoad, chkRoad);
        setSelected(spnRailroad, chkRail);


        String api_endpoint = g.db.getEndPoint();
        if (api_endpoint != null) txtAPI.setText(api_endpoint);

        Long timeCount = g.db.getCacheTime();
        char unit = g.db.getCacheTimeUnit();
        Long time = null;
        switch (unit) {
            case 'd':
                time = Long.valueOf((long) (timeCount / 8.64e+7));
                break;
            case 'h':
                time = Long.valueOf((long) (timeCount / 3.6e+6));
                break;
            case 'm':
                time = Long.valueOf((long) (timeCount / 60000));
                break;
            case 's':
                time = Long.valueOf((long) (timeCount / 1000));
                break;
        }
        if (time != null) txtCacheTime.setText("" + time + Character.toString(unit));
    }

    int findIndexIntColor(int color)
    {
        for (int i = 0; i < colors.getCount(); i++)
        {
            IntColor intColor = colors.getItem(i);
            if (color == intColor.color) return i;
        }
        return -1;
    }

    void setSelected(Spinner spn, CheckBox chk)
    {
        int spnInd = Integer.parseInt((String) spn.getTag());
        int color = layers[spnInd][0];
        int checked = layers[spnInd][1];
        if (checked == 1) chk.setChecked(true);
        else chk.setChecked(false);
        spn.setSelection(findIndexIntColor(color));
    }

    class IntColor
    {
        int color;
        String name;

        public IntColor(int color, String name)
        {
            this.color = color;
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }

    }


    public void saveAPI(View v)
    {
        String api_endpoint = txtAPI.getText().toString();
        g.db.saveEndPoint(api_endpoint);
    }

    public void saveCacheTime(View v)
    {
        String cacheTime = txtCacheTime.getText().toString();
        int timeCount;
        try
        {
            timeCount = Integer.parseInt(cacheTime.substring(0, cacheTime.length() - 1));
        }
        catch (Exception e)
        {
            Toast.makeText(this, "Invalid format", Toast.LENGTH_SHORT).show();
            return;
        }
        char unit = cacheTime.charAt(cacheTime.length() - 1);
        Long time = null;
        switch (unit)
        {
            case 'd': time = Long.valueOf((long) (timeCount * 8.64e+7));
                break;
            case 'h': time = Long.valueOf((long) (timeCount * 3.6e+6));
                break;
            case 'm': time = Long.valueOf((long) (timeCount * 60000));
                break;
            case 's': time = Long.valueOf((long) (timeCount * 1000));
                break;
            default: Toast.makeText(this, "Invalid unit format", Toast.LENGTH_SHORT).show();
                return;
        }
        g.db.saveCacheTime(new Timestamp(time), unit);
    }

    public void onClear(View v)
    {
        g.db.deleteALlTile();
    }


    public void saveLayer(View v)
    {
        IntColor intColor;
        if (chkCoast.isChecked())
        {
            intColor = (IntColor) spnCoastline.getSelectedItem();
            g.db.saveLayer(0, intColor.color, 1);
        }
        else
        {
            intColor = (IntColor) spnCoastline.getSelectedItem();
            g.db.saveLayer(0, intColor.color, 0);
        }
        if (chkRiver.isChecked())
        {
            intColor = (IntColor) spnRiver.getSelectedItem();
            g.db.saveLayer(1, intColor.color, 1);
        }
        else
        {
            intColor = (IntColor) spnRiver.getSelectedItem();
            g.db.saveLayer(1, intColor.color, 0);
        }
        if (chkRoad.isChecked())
        {
            intColor = (IntColor) spnRoad.getSelectedItem();
            g.db.saveLayer(2, intColor.color, 1);
        }
        else
        {
            intColor = (IntColor) spnRoad.getSelectedItem();
            g.db.saveLayer(2, intColor.color, 0);
        }
        if (chkRail.isChecked())
        {
            intColor = (IntColor) spnRailroad.getSelectedItem();
            g.db.saveLayer(3, intColor.color, 1);
        }
        else
        {
            intColor = (IntColor) spnRailroad.getSelectedItem();
            g.db.saveLayer(3, intColor.color, 0);
        }
    }


    public void onReturn(View v)
    {
        finish();
    }
}