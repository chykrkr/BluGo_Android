package com.example.user.blugo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

public class GoBoardActivity extends AppCompatActivity {
    private NumberPicker np1, np2, np_color;
    private GoBoardView gv;
    private GoControlSingle single_game = new GoControlSingle();
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_board);

        np1 = (NumberPicker) findViewById(R.id.numberPicker);
        np2 = (NumberPicker) findViewById(R.id.numberPicker2);
        np_color = (NumberPicker) findViewById(R.id.num_color);

        gv = (GoBoardView) findViewById(R.id.go_board_view);

        gv.setGo_control(single_game);

        gv.setFocusable(true);
    }

    public void aaaa(View view)
    {
        /* board clear */
        single_game.new_game();
    }
}
