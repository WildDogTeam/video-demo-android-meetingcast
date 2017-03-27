package com.wilddog.video.meetingcast.util;

import com.wilddog.video.Conference;
import com.wilddog.video.Conversation;
import com.wilddog.video.IncomingInvite;
import com.wilddog.video.Participant;

import java.util.Iterator;
import java.util.Set;

/**
 * Created by Administrator on 2016/12/29.
 */
public class AdjustNullTool {
    public static String adjustConversation(Conversation conversation){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("conversation ID :"+conversation.getId()+" ");
        stringBuffer.append("localParticipantID:"+conversation.getLocalParticipant().getParticipantId()+" ");
        stringBuffer.append("status:"+conversation.getStatus()+" ");
        stringBuffer.append("RemoteParticipantID:"+conversation.getParticipant().getParticipantId()+" ");
        return stringBuffer.toString();
    }

    public static String adjustParticipant(Participant participant){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("ParticipantID:"+participant.getParticipantId()+" ");
        return stringBuffer.toString();
    }

    public static String adjustIncomingInvite(IncomingInvite incomingInvite){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("ConversationId : "+incomingInvite.getConversationId()+" ");
        stringBuffer.append("Status : "+incomingInvite.getStatus()+" ");
        stringBuffer.append("FromPariticpantId : "+incomingInvite.getFromPariticpantId()+" ");
        stringBuffer.append("UserData : "+incomingInvite.getUserData()+" ");
        return stringBuffer.toString();
    }

    public static String adjustConference(Conference conference){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("conference LocalParticipentId :"+conference.getLocalParticipant().getParticipantId()+" ");
        stringBuffer.append("conference status :"+conference.getStatus()+" ");
        stringBuffer.append("conference ID :"+conference.getId()+" ");
        stringBuffer.append("participantSet :"+participantSetToString(conference.getParticipants())+" ");
        return stringBuffer.toString();
    }
    private static String participantSetToString(Set<Participant> set){
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<Participant> it = set.iterator();
        while (it.hasNext()){
           Participant participant = it.next();
           stringBuffer.append(adjustParticipant(participant)) ;
        }
        return stringBuffer.toString();
    }
}
