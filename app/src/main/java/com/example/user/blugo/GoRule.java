package com.example.user.blugo;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by user on 2016-06-02.
 */
public abstract class GoRule {
    protected class BoardState {
	public ArrayList<GoControl.GoAction> stone_pos;
	public ArrayList white_links = new ArrayList();
	public ArrayList black_links = new ArrayList();
	public Point ko_pos = null;
	public GoControl.Player next_turn = GoControl.Player.BLACK;

	BoardState()
	{
	    this(new ArrayList<GoControl.GoAction>(), new ArrayList(), new ArrayList(), null, GoControl.Player.BLACK);
	}

	BoardState(ArrayList<GoControl.GoAction> stone_pos, ArrayList white_links, ArrayList black_links, Point ko_pos, GoControl.Player next_turn)
	{
	    this.stone_pos = stone_pos;
	    this.white_links = white_links;
	    this.black_links = black_links;
	    this.ko_pos = ko_pos;
	    this.next_turn = next_turn;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
	    BoardState state = new BoardState(null, null, null, null, GoControl.Player.BLACK);
	    int i, j;

	    state.stone_pos = (ArrayList<GoControl.GoAction>) stone_pos.clone();
	    for (i = 0 ; i < stone_pos.size() ; i++) {
		state.stone_pos.set(i, (GoControl.GoAction) stone_pos.get(i).clone());
	    }

	    state.white_links = (ArrayList) white_links.clone();
	    for (i = 0 ; i < white_links.size() ; i++) {
		ArrayList<GoControl.GoAction> link = (ArrayList<GoControl.GoAction>) ((ArrayList<GoControl.GoAction>) white_links.get(i)).clone();

		for (j = 0 ; j < link.size() ; j++) {
		    link.set(j, (GoControl.GoAction) link.get(j).clone());
		}

		state.white_links.set(i, link);
	    }

	    state.black_links = (ArrayList) black_links.clone();
	    for (i = 0 ; i < black_links.size() ; i++) {
		ArrayList<GoControl.GoAction> link = (ArrayList<GoControl.GoAction>) ((ArrayList<GoControl.GoAction>) black_links.get(i)).clone();

		for (j = 0 ; j < link.size() ; j++) {
		    link.set(j, (GoControl.GoAction) link.get(j).clone());
		}

		state.black_links.set(i, link);
	    }

	    state.ko_pos = (ko_pos == null)? null : new Point(ko_pos.x, ko_pos.y);

	    return state;
	}
    }
    public abstract ArrayList<GoControl.GoAction> get_stones();
    public abstract ArrayList<GoControl.GoAction> get_action_history();
    public abstract ArrayList<BoardState> getTimeline();
    public abstract boolean putStoneAt(int x, int y, GoControl.Player stone_color, GoControl.Player next_turn, int board_size);
    public abstract void pass(GoControl.Player next_turn);
    public abstract boolean undo();
}
