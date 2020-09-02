package com.omarea.library.calculator

/*
// JAVA
public class Flags {
    private int flags;
    public Flags(int flags) {
        this.flags = flags;
    }

    public int addFlag(int flag) {
        this.flags |= flag;
        return this.flags;
    }

    public int removeFlag(int flag) {
        this.flags &= ~flag;
        return this.flags;
    }
}
*/

class Flags(private var flags: Int) {
    fun addFlag(flag: Int): Int {
        flags = flags or flag
        return flags
    }

    fun removeFlag(flag: Int): Int {
        flags = flags and flag.inv()
        return flags
    }
}