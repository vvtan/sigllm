package com.example.sigllm.prompter

import java.util.regex.Pattern

class AnomalyUtils {

    static String cleanResponse(String text) {
        def clean = text.trim().toLowerCase().replaceAll(',+', ',')
        if (clean.contains('no anomalies') || clean.contains('no anomaly')) {
            return ''
        }
        return clean
    }

    static String parseListResponse(String text) {
        def clean = cleanResponse(text)
        def matcher = Pattern.compile(/\[([\d\s,]+)\]/).matcher(clean)
        if (matcher.find()) {
            def values = matcher.group(1)
                .split(',')
                .collect { it.trim() }
                .findAll { it }
            return values.join(',')
        }
        return ''
    }

    static List<Integer> parseIntervalResponse(String text) {
        def clean = cleanResponse(text)
        def matcher = Pattern.compile(/\[([\d\s,]+)\]/).matcher(clean)
        def values = []
        while (matcher.find()) {
            def pair = matcher.group(1)
            def nums = pair.split(',').collect { it.trim() }.findAll { it }
            if (nums.size() == 2) {
                int start = nums[0] as int
                int end = nums[1] as int
                values.addAll((start..end))
            }
        }
        return values
    }

    static List<List<String>> parseAnomalyResponse(List<List<String>> X, boolean interval=false) {
        def method = interval ? this.&parseIntervalResponse : this.&parseListResponse
        X.collect { list ->
            list.collect { method(it)?.toString() ?: '' }
        }
    }

    static List<List<int[]>> val2idx(List<List<List<Integer>>> y, List<List<Integer>> X) {
        List<List<int[]>> idxList = []
        for (int i=0; i<y.size(); i++) {
            def seq = X[i]
            def anomaliesList = y[i]
            List<int[]> winList = []
            anomaliesList.each { anomalies ->
                int[] indices = anomalies.collectMany { val ->
                    seq.collectIndexed { idx, v -> v == val ? idx : null }
                }.findAll { it != null } as int[]
                winList << indices
            }
            idxList << winList
        }
        return idxList
    }

    static List<int[]> findAnomaliesInWindows(List<List<int[]>> y, double alpha=0.5) {
        List<int[]> idxList = []
        y.each { samples ->
            int minVote = Math.ceil(alpha * samples.size()) as int
            def flattened = samples.collectMany { it as List }
            def counts = [:]
            flattened.each { counts[it] = (counts[it] ?: 0) + 1 }
            def finalList = counts.findAll { it.value >= minVote }.keySet().collect { it as int }
            idxList << finalList as int[]
        }
        return idxList
    }

    static int[] mergeAnomalousSequences(List<int[]> y, int[] firstIndex, int windowSize, int stepSize, double beta=0.5) {
        def shifted = []
        for (int i=0; i<y.size(); i++) {
            shifted << y[i].collect { it + firstIndex[i] }
        }
        int minVote = Math.ceil(beta * windowSize / stepSize) as int
        def flattened = shifted.collectMany { it }
        def counts = [:]
        flattened.each { counts[it] = (counts[it] ?: 0) + 1 }
        def finalList = counts.findAll { it.value >= minVote }.keySet().collect { it as int }
        finalList.sort()
        return finalList as int[]
    }

    static List<Tuple3<Long,Long,Integer>> formatAnomalies(int[] y, long[] timestamp, int paddingSize=50) {
        if (y.length == 0) {
            return []
        }
        long start = timestamp[0]
        long end = timestamp[timestamp.length-1]
        long interval = timestamp[1] - timestamp[0]
        def intervals = []
        y.each { idx ->
            long ts = timestamp[idx]
            long s = Math.max(start, ts - paddingSize * interval)
            long e = Math.min(end, ts + paddingSize * interval)
            intervals << [s, e]
        }
        intervals.sort { a, b -> a[0] <=> b[0] }
        def merged = []
        intervals.each { cur ->
            if (!merged) { merged << cur; return }
            def prev = merged[-1]
            if (cur[0] <= prev[1]) {
                prev[1] = Math.max(prev[1], cur[1])
            } else {
                merged << cur
            }
        }
        merged.collect { new Tuple3(it[0] as Long, it[1] as Long, 0) }
    }
}
