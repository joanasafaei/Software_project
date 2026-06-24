package com.parking.manager.service;

public class OperatorPerformance {
    public String operatorUsername;
    public int transactionsCount;

    public OperatorPerformance(String op, int count) {
        this.operatorUsername = op;
        this.transactionsCount = count;
    }
}
