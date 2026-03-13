package com.niladri.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NestedBean implements Serializable {
    List<String> list = new ArrayList<String>();
    int i = 5;
    NestedBean() {
        list.add("a");
        list.add("b");
    }

    @Override
    public String toString() {
        return "NestedBean{" +
                "list=" + list +
                ", i=" + i +
                '}';
    }

    public List<String> getList() {
        return list;
    }

    public int getI() {
        return i;
    }
}
