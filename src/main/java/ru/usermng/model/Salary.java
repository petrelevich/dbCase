package ru.usermng.model;


public class Salary {
    private final long id;
    private final int val;

    public Salary(long id, int value) {
        this.id = id;
        this.val = value;
    }

    public long getId() {
        return id;
    }

    public int getVal() {
        return val;
    }

    @Override
    public String toString() {
        return "Salary{" +
                "id=" + id +
                ", val=" + val +
                '}';
    }
}
