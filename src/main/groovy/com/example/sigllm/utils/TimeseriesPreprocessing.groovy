package com.example.sigllm.utils

import groovy.transform.CompileStatic
import groovy.lang.Tuple4
import java.util.Arrays

/** Mirror of timeseries_preprocessing.py */
@CompileStatic
class TimeseriesPreprocessing {
    static Tuple4<List<double[]>, int[], Integer, Integer> rollingWindowSequences(double[] X, int windowSize=500, int stepSize=100) {
        int[] index = (0..<X.length).toArray()
        List<double[]> outX = []
        List<Integer> XIndex = []
        int start = 0
        int maxStart = X.length - windowSize + 1
        while (start < maxStart) {
            int end = start + windowSize
            double[] slice = Arrays.copyOfRange(X, start, end)
            outX.add(slice)
            XIndex.add(index[start])
            start += stepSize
        }
        int[] firstIndex = XIndex as int[]
        return new Tuple4(outX, firstIndex, windowSize, stepSize)
    }
}
