package com.scarsz.discordsrv.objects;

public class Tuple<A, B> {
    private A a;
    private B b;

    public Tuple(A var1, B var2) {
        a = var1;
        b = var2;
    }

    public A a() {
        return a;
    }

    public B b() {
        return b;
    }
}