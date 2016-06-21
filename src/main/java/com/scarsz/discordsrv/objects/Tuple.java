package com.scarsz.discordsrv.objects;

public class Tuple<ChannelName, ChannelId> {

    private ChannelName channelname;
    private ChannelId channelid;

    public Tuple(ChannelName var1, ChannelId var2) {
        channelname = var1;
        channelid = var2;
    }

    public ChannelName channelName() {
        return channelname;
    }
    public ChannelId channelId() {
        return channelid;
    }

}