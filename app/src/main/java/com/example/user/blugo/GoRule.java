package com.example.user.blugo;

import java.util.ArrayList;

/**
 * Created by user on 2016-06-02.
 */
public abstract class GoRule {
    protected class BoardState {
	public ArrayList<GoControl.BoardPos> stone_pos;
	public ArrayList white_links = new ArrayList();
	public ArrayList black_links = new ArrayList();
	public GoControl.BoardPos ko_pos = null;
	public int turn = GoControl.BoardPos.BLACK_STONE;

	BoardState()
	{
	    this(new ArrayList<GoControl.BoardPos>(), new ArrayList(), new ArrayList(), null, GoControl.BoardPos.BLACK_STONE);
	}

	BoardState(ArrayList<GoControl.BoardPos> stone_pos, ArrayList white_links, ArrayList black_links, GoControl.BoardPos ko_pos, int turn)
	{
	    this.stone_pos = stone_pos;
	    this.white_links = white_links;
	    this.black_links = black_links;
	    this.ko_pos = ko_pos;
	    this.turn = turn;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
	    BoardState state = new BoardState(null, null, null, null, GoControl.BoardPos.BLACK_STONE);
	    int i, j;

	    state.stone_pos = (ArrayList<GoControl.BoardPos>) stone_pos.clone();
	    for (i = 0 ; i < stone_pos.size() ; i++) {
		state.stone_pos.set(i, (GoControl.BoardPos) stone_pos.get(i).clone());
	    }

	    state.white_links = (ArrayList) white_links.clone();
	    for (i = 0 ; i < white_links.size() ; i++) {
		ArrayList<GoControl.BoardPos> link = (ArrayList<GoControl.BoardPos>) ((ArrayList<GoControl.BoardPos>) white_links.get(i)).clone();

		for (j = 0 ; j < link.size() ; j++) {
		    link.set(j, (GoControl.BoardPos) link.get(j).clone());
		}

		state.white_links.set(i, link);
	    }

	    state.black_links = (ArrayList) black_links.clone();
	    for (i = 0 ; i < black_links.size() ; i++) {
		ArrayList<GoControl.BoardPos> link = (ArrayList<GoControl.BoardPos>) ((ArrayList<GoControl.BoardPos>) black_links.get(i)).clone();

		for (j = 0 ; j < link.size() ; j++) {
		    link.set(j, (GoControl.BoardPos) link.get(j).clone());
		}

		state.black_links.set(i, link);
	    }

	    state.ko_pos = (ko_pos == null)? null : (GoControl.BoardPos) ko_pos.clone();

	    return state;
	}
    }
    public abstract ArrayList<GoControl.BoardPos> get_stones();
    public abstract boolean putStoneAt(int x, int y, int stone_color, int board_size);
}
