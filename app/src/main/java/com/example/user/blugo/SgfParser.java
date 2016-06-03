package com.example.user.blugo;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

/**
 * Created by user on 2016-06-04.
 */
public class SgfParser {
    public enum ItemType {
        BOARD_SIZE, /* Type Integer */
        KOMI, /* Type Float */
        WHITE_PUT, /* Type Point */
        WHITE_PASS,
        BLACK_PUT,
        BLACK_PASS,
        UNKNOWN,
    }
    public class ParsedItem {
        public ItemType type = ItemType.UNKNOWN;
        public Object content = null;
    }

    public static ArrayList<ParsedItem> parse(String sgf_string) {
        ArrayList<ParsedItem> parsed_item = new ArrayList<>();

        /* Find ';' */

        return parsed_item;
    }
}
