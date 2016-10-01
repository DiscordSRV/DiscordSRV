package com.scarsz.discordsrv.objects;

public class ChannelInfo<ChannelName, ChannelId> {

    private ChannelName channelname;
    private ChannelId channelid;

    public ChannelInfo(ChannelName name, ChannelId id) {
        channelname = name;
        channelid = id;
    }

    public ChannelName channelName() throws NullPointerException {
        return channelname;
    }
    public ChannelId channelId() throws NullPointerException {
        return channelid;
    }

}