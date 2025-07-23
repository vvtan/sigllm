package com.example.sigllm.utils

import groovy.transform.CompileStatic
import java.util.regex.Pattern

/** Utility methods mirroring anomalies.py from the Python implementation. */
@CompileStatic
class AnomalyUtils {
    private static final Pattern PATTERN = Pattern.compile(/\[([\d\s,]+)\]/)

    static String cleanResponse(String text) {
        text = text.trim().toLowerCase()
        text = text.replaceAll(',+', ',')
        if (text.contains('no anomalies') || text.contains('no anomaly')) {
            return ''
        }
        return text
    }

    static String parseListResponse(String text) {
        String clean = cleanResponse(text)
        def matcher = PATTERN.matcher(clean)
        if (matcher.find()) {
            String values = matcher.group(1)
            def nums = values.split(',').collect { it.trim() }.findAll { it }
            return nums.join(',')
        }
        return ''
    }

    static List<Integer> parseIntervalResponse(String text) {
        String clean = cleanResponse(text)
        def matcher = PATTERN.matcher(clean)
        if (!matcher.find()) {
            return []
        }
        matcher.reset()
        List<Integer> values = []
        while (matcher.find()) {
            String group = matcher.group()
            try {
                def list = Eval.me(group)
                if (list instanceof List && list.size() == 2) {
                    int start = list[0] as int
                    int end = list[1] as int
                    for(int i=start; i<=end; i++) {
                        values.add(i)
                    }
                }
            } catch(Exception ignore) {
            }
        }
        return values
    }

    static List<List> parseAnomalyResponse(List<List<String>> X, boolean interval=false) {
        def method = interval ? { String t -> parseIntervalResponse(t) } : { String t -> parseListResponse(t) }
        List<List> result = []
        X.each { respList ->
            result << respList.collect { method(it) }
        }
        return result
    }

    static List<List<int[]>> val2idx(List<List<List<Integer>>> y, List<List<Integer>> X) {
        List<List<int[]>> idxList = []
        for(int i=0; i<y.size(); i++) {
            def anomaliesList = y[i]
            def seq = X[i]
            List<int[]> idxWinList = []
            anomaliesList.each { anomalies ->
                Set<Integer> indices = []
                anomalies.each { val ->
                    for(int j=0; j<seq.size(); j++) {
                        if(seq[j] == val) indices.add(j)
                    }
                }
                int[] idx = indices as int[]
                idxWinList.add(idx)
            }
            idxList.add(idxWinList)
        }
        return idxList
    }

    static List<int[]> findAnomaliesInWindows(List<List<int[]>> y, double alpha=0.5) {
        List<int[]> result = []
        y.each { samples ->
            int minVote = (int)Math.ceil(alpha * samples.size())
            List<Integer> flat = []
            samples.each { arr -> arr.each { flat.add(it) } }
            Map<Integer,Integer> counts = [:]
            flat.each { counts[it] = (counts[it] ?: 0) + 1 }
            def finals = counts.findAll { k,v -> v >= minVote }.keySet().sort()
            result.add(finals as int[])
        }
        return result
    }

    static int[] mergeAnomalousSequences(List<int[]> y, int[] firstIndex, int windowSize, int stepSize, double beta=0.5) {
        List<Integer> anomalies = []
        for(int i=0;i<y.size();i++) {
            int[] arr = y[i]
            int start = firstIndex[i]
            arr.each { anomalies.add(it + start) }
        }
        int minVote = (int)Math.ceil(beta * windowSize / stepSize)
        Map<Integer,Integer> counts = [:]
        anomalies.each { counts[it] = (counts[it] ?: 0) + 1 }
        def finals = counts.findAll { k,v -> v >= minVote }.keySet().sort()
        return finals as int[]
    }

    static List<List<Number>> formatAnomalies(int[] y, double[] timestamp, int paddingSize=50) {
        if(y.length == 0) return []
        double[] times = y.collect { timestamp[it] } as double[]
        double start = timestamp[0]
        double end = timestamp[timestamp.length-1]
        double interval = timestamp[1] - timestamp[0]
        List<List<Double>> intervals = []
        times.each { t ->
            intervals.add([Math.max(start, t - paddingSize*interval), Math.min(end, t + paddingSize*interval)])
        }
        if(intervals.isEmpty()) return []
        intervals.sort { a,b -> a[0] <=> b[0] }
        List<List<Double>> merged = [intervals[0]]
        for(int i=1;i<intervals.size();i++) {
            def prev = merged[-1]
            def curr = intervals[i]
            if(curr[0] <= prev[1]) {
                prev[1] = Math.max(prev[1], curr[1])
                merged[-1] = prev
            } else {
                merged.add(curr)
            }
        }
        return merged.collect { [it[0], it[1], 0] }
    }
}
