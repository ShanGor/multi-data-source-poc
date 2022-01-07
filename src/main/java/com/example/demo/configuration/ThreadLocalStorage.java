package com.example.demo.configuration;

public class ThreadLocalStorage {

    private static ThreadLocal<String> localThread = new ThreadLocal<>();

    public static void setDBName(String tenantName) {
        localThread.set(tenantName);
    }

    public static String getDBName() {
        return localThread.get();
    }

}
