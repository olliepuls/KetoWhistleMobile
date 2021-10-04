package com.example.bluetooth_test.Data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Random;

public class DataSetInfo implements Serializable {
        public final boolean left;
        public final int color;
        public final String label;

        public DataSetInfo(String label){
            this.label = label;
            this.color = new Random().nextInt();
            this.left = true;
        }

        public DataSetInfo(String label, int color){
            this.label = label;
            this.color = color;
            this.left = true;
        }

        public DataSetInfo(String label, boolean left){
            this.label = label;
            this.color = new Random().nextInt();
            this.left = true;
        }

        public DataSetInfo(String label, int color, boolean left){
            this.label = label;
            this.color = color;
            this.left = left;
        }

        public static DataSetInfo findDSIByLabel(String label, Collection<DataSetInfo> set) {
            for (DataSetInfo dsi:set) {
                if(dsi.label.equalsIgnoreCase(label)) return dsi;
            }
            return null;
        }

    }