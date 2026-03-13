package com.niladri.beans;

import java.util.List;

public interface ProxyMetricsMBean {
    void incrementCounter1();
    int getCounter1();
    void setCounter1(int value);

    void incrementCounter2();
    int getCounter2();
    void setCounter2(int value);

    boolean getFlagState();
    void setFlagState(boolean value);

    List<String> getArrayOfStrings();
    void setArrayOfStrings(List<String> arrayOfStrings);

    String getPropertyName();
    void setPropertyName(String value);

    NestedBean getNestedBean();

    void printCounters();
}
