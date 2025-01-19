package unibo.JVS.Streamer;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedList;
import java.util.List;

public class StreamStatus implements Parcelable {

    private long currentTime;
    private int streamingStatus;
    private int recordingStatus;
    private int sentVideoPackets;
    private int sentAudioPackets;
    private int numberOfReconnection;
    private int currentFramerate;
    private int currentBitrate;

    private String address;
    private List<String> clients;

    /* default constructor */
    StreamStatus() {
        this.currentTime = -1;
        this.streamingStatus = -1;
        this.recordingStatus = -1;
        this.sentVideoPackets = -1;
        this.sentAudioPackets = -1;
        this.numberOfReconnection = -1;
        this.currentFramerate = -1;
        this.currentBitrate = -1;
        this.address = "";
        this.clients = new LinkedList<>();
    }

    protected StreamStatus(Parcel in) {
        currentTime = in.readLong();
        streamingStatus = in.readInt();
        recordingStatus = in.readInt();
        sentVideoPackets = in.readInt();
        sentAudioPackets = in.readInt();
        numberOfReconnection = in.readInt();
        currentFramerate = in.readInt();
        currentBitrate = in.readInt();
        address = in.readString();
        clients = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(currentTime);
        dest.writeInt(streamingStatus);
        dest.writeInt(recordingStatus);
        dest.writeInt(sentVideoPackets);
        dest.writeInt(sentAudioPackets);
        dest.writeInt(numberOfReconnection);
        dest.writeInt(currentFramerate);
        dest.writeInt(currentBitrate);
        dest.writeString(address);
        dest.writeStringList(clients);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<StreamStatus> CREATOR = new Creator<StreamStatus>() {
        @Override
        public StreamStatus createFromParcel(Parcel in) {
            return new StreamStatus(in);
        }

        @Override
        public StreamStatus[] newArray(int size) {
            return new StreamStatus[size];
        }
    };

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public int isStreamingStatus() {
        return streamingStatus;
    }

    public void setStreamingStatus(int streamingStatus) {
        this.streamingStatus = streamingStatus;
    }

    public int isRecordingStatus() {
        return recordingStatus;
    }

    public void setRecordingStatus(int recordingStatus) {
        this.recordingStatus = recordingStatus;
    }

    public int getSentVideoPackets() {
        return sentVideoPackets;
    }

    public void setSentVideoPackets(int sentVideoPackets) {
        this.sentVideoPackets = sentVideoPackets;
    }

    public int getSentAudioPackets() {
        return sentAudioPackets;
    }

    public void setSentAudioPackets(int sentAudioPackets) {
        this.sentAudioPackets = sentAudioPackets;
    }

    public int getNumberOfReconnection() {
        return numberOfReconnection;
    }

    public void setNumberOfReconnection(int numberOfReconnection) {
        this.numberOfReconnection = numberOfReconnection;
    }

    public double getCurrentFramerate() {
        return currentFramerate;
    }

    public void setCurrentFramerate(int currentFramerate) {
        this.currentFramerate = currentFramerate;
    }

    public long getCurrentBitrate() {
        return currentBitrate;
    }

    public void setCurrentBitrate(int currentBitrate) {
        this.currentBitrate = currentBitrate;
    }

    public List<String> getClients() {
        return clients;
    }

    public void setClients(List<String> clients) {
        this.clients = clients;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "StreamStatus{" +
                "currentTime=" + currentTime +
                ", streamingStatus=" + streamingStatus +
                ", recordingStatus=" + recordingStatus +
                ", sentVideoPackets=" + sentVideoPackets +
                ", sentAudioPackets=" + sentAudioPackets +
                ", numberOfReconnection=" + numberOfReconnection +
                ", currentFramerate=" + currentFramerate +
                ", currentBitrate=" + currentBitrate +
                ", clients=" + clients +
                '}';
    }
}
