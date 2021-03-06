package com.luck.picture.lib.instagram.process

import android.media.*
import android.util.Log
import java.io.FileDescriptor
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

object VideoClipUtils {
    private val TAG = VideoClipUtils::class.java.simpleName
    private const val DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024

    /**
     * @param srcPath the path of source video file.
     * @param dstPath the path of destination video file.
     * @param startMs starting time in milliseconds for trimming. Set to
     * negative if starting from beginning.
     * @param endMs end time for trimming in milliseconds. Set to negative if
     * no trimming at the end.
     * @param useAudio true if keep the audio track from the source.
     * @param useVideo true if keep the video track from the source.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun genVideoUsingMuxer(srcPath: FileDescriptor?, dstPath: String?,
                           startMs: Long, endMs: Long, useAudio: Boolean, useVideo: Boolean): Boolean {
        // Set up MediaExtractor to read from the source.
        val extractor = MediaExtractor()
        extractor.setDataSource(srcPath!!)
        val trackCount = extractor.trackCount
        // Set up MediaMuxer for the destination.
        val muxer: MediaMuxer
        muxer = MediaMuxer(dstPath!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // Set up the tracks and retrieve the max buffer size for selected
        // tracks.
        val indexMap = HashMap<Int, Int>(trackCount)
        var bufferSize = -1
        try {
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                var selectCurrentTrack = false
                if (mime.startsWith("audio/") && useAudio) {
                    selectCurrentTrack = true
                } else if (mime.startsWith("video/") && useVideo) {
                    selectCurrentTrack = true
                }
                if (selectCurrentTrack) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        bufferSize = if (newSize > bufferSize) newSize else bufferSize
                    }
                }
            }
            if (bufferSize < 0) {
                bufferSize = DEFAULT_BUFFER_SIZE
            }
            // Set up the orientation and starting time for extractor.
            val retrieverSrc = MediaMetadataRetriever()
            retrieverSrc.setDataSource(srcPath)
            val degreesString = retrieverSrc.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            if (degreesString != null) {
                val degrees = degreesString.toInt()
                if (degrees >= 0) {
                    muxer.setOrientationHint(degrees)
                }
            }
            if (startMs > 0) {
                extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }
            // Copy the samples from MediaExtractor to MediaMuxer. We will loop
            // for copying each sample and stop when we get to the end of the source
            // file or exceed the end time of the trimming.
            val offset = 0
            var trackIndex = -1
            val dstBuf = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            muxer.start()
            while (true) {
                bufferInfo.offset = offset
                bufferInfo.size = extractor.readSampleData(dstBuf, offset)
                if (bufferInfo.size < 0) {
                    Log.d(TAG, "Saw input EOS.")
                    bufferInfo.size = 0
                    break
                } else {
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    if (endMs > 0 && bufferInfo.presentationTimeUs > endMs * 1000) {
                        Log.d(TAG, "The current sample is over the trim end time.")
                        break
                    } else {
                        bufferInfo.flags = extractor.sampleFlags
                        trackIndex = extractor.sampleTrackIndex
                        muxer.writeSampleData(indexMap[trackIndex]!!, dstBuf,
                                bufferInfo)
                        extractor.advance()
                    }
                }
            }
            muxer.stop()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            muxer.release()
        }
        return false
    } //    /**
    //     * ????????????
    //     * @param srcPath ??????????????????????????????
    //     * @param outPath ??????????????????????????????
    //     * @param startTimeMs ?????????????????????
    //     * @param endTimeMs ?????????????????????
    //     */
    //    public static boolean trimUsingMp4Parser(FileDescriptor srcPath, String outPath, double startTimeMs, double endTimeMs) throws IOException, IllegalArgumentException {
    //        if (srcPath == null || TextUtils.isEmpty(outPath)) {
    //            return false;
    //        }
    //        if (startTimeMs >= endTimeMs) {
    //            return false;
    //        }
    //        Movie movie = MovieCreator.build(new FileDataSourceViaHeapImpl(new FileInputStream(srcPath).getChannel()));
    //        List<Track> tracks = movie.getTracks();
    //        //????????????track
    //        movie.setTracks(new LinkedList<Track>());
    //        //??????????????????????????????
    //        double startTime = startTimeMs/1000;
    //        double endTime = endTimeMs/1000;
    //
    //        boolean timeCorrected = false;
    //        // Here we try to find a track that has sync samples. Since we can only
    //        // start decoding at such a sample we SHOULD make sure that the start of
    //        // the new fragment is exactly such a frame.
    //        for (Track track : tracks) {
    //            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
    //                if (timeCorrected) {
    //                    // This exception here could be a false positive in case we
    //                    // have multiple tracks with sync samples at exactly the
    //                    // same positions. E.g. a single movie containing multiple
    //                    // qualities of the same video (Microsoft Smooth Streaming
    //                    // file)
    //                    return false;
    //                }
    //                startTime = correctTimeToSyncSample(track, startTime, false);
    //                endTime = correctTimeToSyncSample(track, endTime, true);
    //                timeCorrected = true;
    //            }
    //        }
    //
    //        long currentSample;
    //        double currentTime;
    //        double lastTime;
    //        long startSample1;
    //        long endSample1;
    //        long delta;
    //
    //        for (Track track : tracks) {
    //            currentSample = 0;
    //            currentTime = 0;
    //            lastTime = -1;
    //            startSample1 = -1;
    //            endSample1 = -1;
    //
    //            //?????????????????????????????????????????????sample?????????sample?????????
    //            for (int i = 0; i < track.getSampleDurations().length; i++) {
    //                delta = track.getSampleDurations()[i];
    //                if (currentTime > lastTime && currentTime <= startTime) {
    //                    startSample1 = currentSample;
    //                }
    //                if (currentTime > lastTime && currentTime <= endTime) {
    //                    endSample1 = currentSample;
    //                }
    //                lastTime = currentTime;
    //                currentTime += (double)delta / (double)track.getTrackMetaData().getTimescale();
    //                currentSample++;
    //            }
    //            if (startSample1 <= 0 && endSample1 <= 0) {
    //                throw new RuntimeException("clip failed !!");
    //            }
    //            movie.addTrack(new CroppedTrack(track, startSample1, endSample1));// ???????????????track
    //        }
    //
    //        //????????????mp4
    //        Container out = new DefaultMp4Builder().build(movie);
    //        FileOutputStream fos = new FileOutputStream(outPath);
    //        FileChannel fco = fos.getChannel();
    //        out.writeContainer(fco);
    //        fco.close();
    //        fos.close();
    //        return true;
    //    }
    //    /**
    //     * ??????????????????
    //     * @param track
    //     * @param cutHere
    //     * @param next
    //     * @return
    //     */
    //    public static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
    //        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
    //        long currentSample = 0;
    //        double currentTime = 0;
    //        for (int i = 0; i < track.getSampleDurations().length; i++) {
    //            long delta = track.getSampleDurations()[i];
    //            int index = Arrays.binarySearch(track.getSyncSamples(), currentSample + 1);
    //            if (index >= 0) {
    //                timeOfSyncSamples[index] = currentTime;
    //            }
    //            currentTime += ((double)delta / (double)track.getTrackMetaData().getTimescale());
    //            currentSample++;
    //        }
    //        double previous = 0;
    //        for (double timeOfSyncSample : timeOfSyncSamples) {
    //            if (timeOfSyncSample > cutHere) {
    //                if (next) {
    //                    return timeOfSyncSample;
    //                } else {
    //                    return previous;
    //                }
    //            }
    //            previous = timeOfSyncSample;
    //        }
    //        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    //    }
}