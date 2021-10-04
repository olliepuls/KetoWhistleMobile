package com.example.bluetooth_test;

public interface Procedure{
        void perform();

        static Procedure empty = () -> {};
}