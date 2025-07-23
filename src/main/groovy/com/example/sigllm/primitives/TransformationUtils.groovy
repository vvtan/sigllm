package com.example.sigllm.primitives

import java.util.regex.Pattern

class TransformationUtils {
    static List<String> formatAsString(List<List<Number>> X, String sep=',', boolean space=false, boolean single=false) {
        def asString = { List<Number> arr ->
            String text = arr.collect{ it.toString() }.join(sep)
            if (space) {
                text = text.toCharArray().join(' ')
            }
            return text
        }
        if (single) {
            return [asString(X[0])]
        } else {
            return X.collect { asString(it) }
        }
    }

    static List<List<Double>> formatAsInteger(List<List<String>> X, String sep=',', Integer trunc=null, String errors='ignore') {
        def result = []
        X.each { lst ->
            if (!(lst instanceof List)) {
                throw new IllegalArgumentException('Input is not a list of lists.')
            }
            def sample = []
            lst.each { text ->
                if (!text) {
                    sample << []
                } else {
                    sample << fromStringToInteger(text, sep, trunc, errors)
                }
            }
            result << sample
        }
        return result
    }

    static List<Double> fromStringToInteger(String text, String sep=',', Integer trunc=null, String errors='ignore') {
        String nospace = text.replaceAll(/\s+/, '')
        String rule = "[^0-9|${sep}]"
        if (errors == 'raise') {
            def m = nospace =~ rule
            if (m.find()) {
                throw new IllegalArgumentException("Encountered a non-digit value ${m.group()}")
            }
        }
        def values = nospace.split(Pattern.quote(sep)).findAll{ it }
        List clean
        switch(errors) {
            case 'ignore':
                clean = values.findAll{ !(it =~ rule) }
                break
            case 'filter':
                clean = values.collect{ it.replaceAll(rule, '') }
                break
            case 'coerce':
                clean = values.collect{ (it =~ rule).find() ? 'NaN' : it }
                break
            default:
                throw new IllegalArgumentException("Unknown errors strategy ${errors}.")
        }
        clean = clean.collect{ it=='NaN' ? Double.NaN : Double.valueOf(it) }
        if (trunc != null) {
            clean = clean.take(trunc)
        }
        return clean
    }

    static class Float2Scalar {
        int decimal = 2
        boolean rescale = true
        Double minimum

        Float2Scalar(int decimal=2, boolean rescale=true) {
            this.decimal = decimal
            this.rescale = rescale
        }

        void fit(List<Double> X) {
            this.minimum = X.min()
        }

        Tuple3<List<Integer>, Double, Integer> transform(List<Double> X) {
            List<Double> values = X
            if (rescale) {
                values = X.collect{ it - minimum }
            }
            List<Integer> out = []
            values.each { v ->
                int sign = v >= 0 ? 1 : -1
                double absVal = Math.abs(v)
                int val = (int)(sign * (absVal * Math.pow(10, decimal)))
                out << val
            }
            return new Tuple3(out, minimum, decimal)
        }
    }

    static class Scalar2Float {
        List<Double> transform(List<Integer> X, Double minimum=0, int decimal=2) {
            return X.collect { it * Math.pow(10, -decimal) + minimum }
        }
    }
}
