package com.xbjornsen.emftracker.data

import com.xbjornsen.emftracker.data.models.FftPeak
import com.xbjornsen.emftracker.data.models.FftResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FftAnalyzer {

    private const val N = 256
    private const val MAINS_THRESHOLD = 0.10f  // lowered — demeaning makes AC peaks dominant
    private const val MAINS_WINDOW = 3          // ±3 bins to handle slight sample-rate error
    private const val PEAK_THRESHOLD = 0.15f    // min normalised mag to report a peak
    private const val PEAK_MIN_SEP_HZ = 5f   // merge peaks closer than this

    private val HANN = FloatArray(N) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (N - 1)))).toFloat()
    }

    fun analyze(samples: FloatArray, sampleRateHz: Float): FftResult {
        require(samples.size == N)

        // Remove DC offset so AC oscillations aren't swamped by the Earth's ~50 µT background.
        // Without this, bin 0 dominates by 100:1 and 50/60 Hz peaks are invisible after normalisation.
        val mean = samples.average().toFloat()
        val re = FloatArray(N) { i -> (samples[i] - mean) * HANN[i] }
        val im = FloatArray(N)

        fft(re, im)

        // Magnitude spectrum — skip bin 0 (DC residual after demeaning)
        val mags = FloatArray(N / 2) { k ->
            sqrt(re[k] * re[k] + im[k] * im[k]) / N
        }

        // Normalise against the strongest AC bin (skip DC bin 0)
        val maxMag = (1 until N / 2).maxOf { mags[it] }.coerceAtLeast(1e-9f)
        val normalised = mags.map { it / maxMag }

        // Dominant bin (skip DC bin 0)
        val dominantBin = (1 until N / 2).maxByOrNull { mags[it] } ?: 1
        val dominantHz = dominantBin * sampleRateHz / N

        val has50Hz = checkMains(normalised, 50f, sampleRateHz)
        val has60Hz = checkMains(normalised, 60f, sampleRateHz)
        val peaks   = extractPeaks(normalised, sampleRateHz)

        return FftResult(
            dominantFrequencyHz = dominantHz,
            has50Hz = has50Hz,
            has60Hz = has60Hz,
            bins = normalised,
            sampleRateHz = sampleRateHz,
            peaks = peaks
        )
    }

    private fun extractPeaks(normalised: List<Float>, sampleRateHz: Float): List<FftPeak> {
        val hzPerBin = sampleRateHz / N
        val minBinSep = (PEAK_MIN_SEP_HZ / hzPerBin).toInt().coerceAtLeast(1)
        val peaks = mutableListOf<FftPeak>()
        // Skip DC (bin 0) and last bin
        for (i in 2 until normalised.size - 1) {
            val mag = normalised[i]
            if (mag >= PEAK_THRESHOLD
                && mag > normalised[i - 1]
                && mag > normalised[i + 1]
            ) {
                val hz = i * hzPerBin
                // Merge with nearby already-found peak
                val last = peaks.lastOrNull()
                if (last != null && hz - last.frequencyHz < PEAK_MIN_SEP_HZ) {
                    if (mag > last.normalizedMagnitude)
                        peaks[peaks.lastIndex] = FftPeak(hz, mag)
                } else {
                    peaks.add(FftPeak(hz, mag))
                }
            }
        }
        return peaks.sortedByDescending { it.normalizedMagnitude }.take(8)
    }

    private fun checkMains(normalised: List<Float>, targetHz: Float, sampleRateHz: Float): Boolean {
        val targetBin = (targetHz * N / sampleRateHz).toInt()
        if (targetBin < 1 || targetBin >= N / 2) return false
        val lo = (targetBin - MAINS_WINDOW).coerceAtLeast(1)
        val hi = (targetBin + MAINS_WINDOW).coerceAtMost(N / 2 - 1)
        return (lo..hi).any { normalised[it] > MAINS_THRESHOLD }
    }

    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { swap(re, i, j); swap(im, i, j) }
        }
        // Cooley-Tukey iterative DIT
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang).toFloat()
            val wIm = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f; var curIm = 0f
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]; val uIm = im[i + k]
                    val tRe = curRe * re[i + k + len / 2] - curIm * im[i + k + len / 2]
                    val tIm = curRe * im[i + k + len / 2] + curIm * re[i + k + len / 2]
                    re[i + k] = uRe + tRe; im[i + k] = uIm + tIm
                    re[i + k + len / 2] = uRe - tRe; im[i + k + len / 2] = uIm - tIm
                    val newCurRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = newCurRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun swap(arr: FloatArray, i: Int, j: Int) {
        val tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp
    }
}
