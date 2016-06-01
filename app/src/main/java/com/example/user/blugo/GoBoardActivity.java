package com.example.user.blugo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

public class GoBoardActivity extends AppCompatActivity {
    private NumberPicker np1, np2, np_color;
    private GoBoardView gv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_board);

        np1 = (NumberPicker) findViewById(R.id.numberPicker);
        np2 = (NumberPicker) findViewById(R.id.numberPicker2);
        np_color = (NumberPicker) findViewById(R.id.num_color);

        gv = (GoBoardView) findViewById(R.id.go_board_view);

        np1.setMinValue(1);
        np2.setMinValue(1);
        np_color.setMinValue(GoBoardView.EMPTY);

        np1.setMaxValue(gv.getBoardSize());
        np2.setMaxValue(gv.getBoardSize());
        np_color.setMaxValue(GoBoardView.WHITE_STONE);

        gv.setFocusable(true);
    }

    public void aaaa(View view)
    {
        gv.setCurrent_turn(np_color.getValue());

        gv.invalidate();
    }
}
