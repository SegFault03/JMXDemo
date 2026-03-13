package com.niladri.beans;

import java.util.ArrayList;
import java.util.List;

public class ProxyMetrics implements ProxyMetricsMBean {
    private int counter1;
    private int counter2;
    private boolean flagState;
    private String propertyName;
    private List<String> arrayOfStrings;

    public ProxyMetrics() {
        counter1 = 0;
        counter2 = 0;
        flagState = false;
        propertyName = "InitialPropertyName";
        arrayOfStrings = new ArrayList<>();
        arrayOfStrings.add("Hello");
        arrayOfStrings.add("World");
    }

    @Override
    public void incrementCounter1() {
        counter1++;
    }

    @Override
    public int getCounter1() {
        return counter1;
    }

    @Override
    public void setCounter1(int value) {
        counter1 = value;
    }

    @Override
    public void incrementCounter2() {
        counter2++;
    }

    @Override
    public int getCounter2() {
        return counter2;
    }

    @Override
    public void setCounter2(int value) {
        counter2 = value;
    }

    @Override
    public boolean getFlagState() {
        return flagState;
    }

    @Override
    public void setFlagState(boolean value) {

    }

    @Override
    public List<String> getArrayOfStrings() {
        return arrayOfStrings;
    }

    @Override
    public void setArrayOfStrings(List<String> arrayOfStrings) {

    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public void setPropertyName(String value) {

    }

    @Override
    public void printCounters() {
        System.out.printf("counter1: %d, counter2: %d\n", counter1, counter2);
    }
}
