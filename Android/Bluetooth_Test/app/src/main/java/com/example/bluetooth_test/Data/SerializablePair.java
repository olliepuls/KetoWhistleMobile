package com.example.bluetooth_test.Data;

import android.util.Pair;

import java.io.Serializable;

public class SerializablePair<F extends Serializable, S extends Serializable>
        extends Pair<F, S> implements Serializable {
    /**
     * Constructor for a Pair.
     *
     * @param first  the first object in the Pair
     * @param second the second object in the pair
     */
    public SerializablePair(F first, S second) {
        super(first, second);
    }
}
