/* 
 * Copyright (c) 2019 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.net.social.activitypub;

import org.andstatus.app.context.TestSuite;
import org.andstatus.app.net.http.HttpConnectionMock;
import org.andstatus.app.net.social.AActivity;
import org.andstatus.app.net.social.AObjectType;
import org.andstatus.app.net.social.ActivityType;
import org.andstatus.app.net.social.Actor;
import org.andstatus.app.net.social.ActorEndpointType;
import org.andstatus.app.net.social.Connection;
import org.andstatus.app.net.social.Note;
import org.andstatus.app.net.social.TimelinePosition;
import org.andstatus.app.origin.OriginConnectionData;
import org.andstatus.app.util.TriState;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static org.andstatus.app.context.DemoData.demoData;
import static org.andstatus.app.net.social.activitypub.VerifyCredentialsActivityPubTest.ACTOR_OID;
import static org.andstatus.app.net.social.activitypub.VerifyCredentialsActivityPubTest.UNIQUE_NAME_IN_ORIGIN;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConnectionActivityPubTest {
    private Connection connection;
    private HttpConnectionMock httpConnection;

    String pawooActorOid = "https://pawoo.net/users/pawooAndStatusTester";
    String pawooNoteOid = "https://pawoo.net/users/pawooAndStatusTester/statuses/101727836012435643";

    @Before
    public void setUp() throws Exception {
        TestSuite.initializeWithAccounts(this);

        TestSuite.setHttpConnectionMockClass(HttpConnectionMock.class);
        OriginConnectionData connectionData = OriginConnectionData.fromMyAccount(
                demoData.getMyAccount(demoData.activityPubTestAccountName), TriState.UNKNOWN);
        connection = connectionData.newConnection();
        httpConnection = (HttpConnectionMock) connection.getHttp();
        TestSuite.setHttpConnectionMockClass(null);
    }

    @Test
    public void getTimeline() throws IOException {
        String sinceId = "";
        httpConnection.addResponse(org.andstatus.app.tests.R.raw.activitypub_inbox_pleroma);
        Actor actorForTimeline = Actor.fromOid(connection.getData().getOrigin(), ACTOR_OID)
                .withUniqueNameInOrigin(UNIQUE_NAME_IN_ORIGIN);
        actorForTimeline.endpoints.add(ActorEndpointType.API_INBOX, "https://pleroma.site/users/AndStatus/inbox");
        List<AActivity> timeline = connection.getTimeline(Connection.ApiRoutineEnum.HOME_TIMELINE,
                new TimelinePosition(sinceId), TimelinePosition.EMPTY, 20, actorForTimeline);
        assertNotNull("timeline returned", timeline);
        assertEquals("Number of items in the Timeline", 5, timeline.size());

        AActivity activity = timeline.get(4);
        assertEquals("Creating a Note " + activity, AObjectType.NOTE, activity.getObjectType());
        Note note = activity.getNote();
        assertEquals("Note oid " + note, "https://pleroma.site/objects/34ab2ec5-4307-4e0b-94d6-a789d4da1240", note.oid);
        assertEquals("Conversation oid " + note,"https://pleroma.site/contexts/c62ba280-2a11-473e-8bd1-9435e9dc83ae", note.conversationOid);
        assertEquals("Note name " + note, "", note.getName());
        assertThat("Note body " + note, note.getContent(), startsWith("We could successfully create an account"));
        assertEquals("Activity updated at " + TestSuite.utcTime(activity.getUpdatedDate()),
                TestSuite.utcTime(2019, Calendar.FEBRUARY, 10, 17, 37, 25).toString(),
                TestSuite.utcTime(activity.getUpdatedDate()).toString());
        assertEquals("Note updated at " + TestSuite.utcTime(note.getUpdatedDate()),
                TestSuite.utcTime(2019, Calendar.FEBRUARY, 10, 17, 37, 25).toString(),
                TestSuite.utcTime(note.getUpdatedDate()).toString());
        Actor actor = activity.getActor();
        assertEquals("Actor's oid " + activity, "https://pleroma.site/users/ActivityPubTester", actor.oid);
        assertEquals("Actor's Webfinger " + activity, "", actor.getWebFingerId());

        assertEquals("Actor is an Author", actor, activity.getAuthor());
        assertEquals("Should be Create " + activity, ActivityType.CREATE, activity.type);
        assertEquals("Favorited by me " + activity, TriState.UNKNOWN, activity.getNote().getFavoritedBy(activity.accountActor));

        activity = timeline.get(3);
        assertEquals("Is not FOLLOW " + activity, ActivityType.FOLLOW, activity.type);
        assertEquals("Actor", "https://pleroma.site/users/ActivityPubTester", activity.getActor().oid);
        assertEquals("Actor followed by me", TriState.UNKNOWN, activity.getActor().followedByMe);
        assertEquals("Activity Object", AObjectType.ACTOR, activity.getObjectType());
        Actor objActor = activity.getObjActor();
        assertEquals("objActor followed", "https://pleroma.site/users/AndStatus", objActor.oid);
        assertEquals("Actor followed by me", TriState.UNKNOWN, objActor.followedByMe);

        for (int ind = 0; ind < 3; ind++) {
            activity = timeline.get(ind);
            assertEquals("Is not UPDATE " + activity, ActivityType.UPDATE, activity.type);
            assertEquals("Actor", AObjectType.ACTOR, activity.getObjectType());
            objActor = activity.getObjActor();
            assertEquals("Following", TriState.UNKNOWN, objActor.followedByMe);
            assertEquals("Url of objActor", "https://pleroma.site/users/AndStatus", objActor.getProfileUrl());
            assertEquals("WebFinger ID", "andstatus@pleroma.site", objActor.getWebFingerId());
        }
    }

    @Test
    public void getNotesByActor() throws IOException {
        String ACTOR_OID2 = "https://pleroma.site/users/kaniini";
        String sinceId = "";
        httpConnection.addResponse(org.andstatus.app.tests.R.raw.activitypub_outbox_pleroma);
        Actor actorForTimeline = Actor.fromOid(connection.getData().getOrigin(), ACTOR_OID2)
                .withUniqueNameInOrigin(UNIQUE_NAME_IN_ORIGIN);
        actorForTimeline.endpoints.add(ActorEndpointType.API_OUTBOX, ACTOR_OID2 + "/outbox");
        List<AActivity> timeline = connection.getTimeline(Connection.ApiRoutineEnum.ACTOR_TIMELINE,
                new TimelinePosition(sinceId), TimelinePosition.EMPTY, 20, actorForTimeline);
        assertNotNull("timeline returned", timeline);
        assertEquals("Number of items in the Timeline", 10, timeline.size());

        AActivity activity = timeline.get(2);
        assertEquals("Announcing " + activity, ActivityType.ANNOUNCE, activity.type);
        assertEquals("Announcing a Note " + activity, AObjectType.NOTE, activity.getObjectType());
        Note note = activity.getNote();
        assertEquals("Note oid " + note, "https://lgbtq.cool/users/abby/statuses/101702144808655868", note.oid);
        Actor actor = activity.getActor();
        assertEquals("Actor's oid " + activity, ACTOR_OID2, actor.oid);

        assertEquals("Author is unknown", Actor.EMPTY, activity.getAuthor());
    }

    @Test
    public void noteFromPawooNet() throws IOException {
        httpConnection.addResponse(org.andstatus.app.tests.R.raw.activitypub_note_from_pawoo_net_pleroma);
        AActivity activity8 = connection.getNote(pawooNoteOid);
        assertEquals("Updating " + activity8, ActivityType.UPDATE, activity8.type);
        assertEquals("Acting on a Note " + activity8, AObjectType.NOTE, activity8.getObjectType());
        Note note8 = activity8.getNote();
        assertEquals("Note oid " + note8, pawooNoteOid, note8.oid);
        Actor author = activity8.getAuthor();
        assertEquals("Author's oid " + activity8, pawooActorOid, author.oid);
        assertEquals("Actor is author", author, activity8.getActor());
        assertThat("Note body " + note8, note8.getContent(),
                containsString("how two attached images may look like"));
        assertEquals("Note updated at " + TestSuite.utcTime(note8.getUpdatedDate()),
                TestSuite.utcTime(2019, Calendar.MARCH, 10, 18, 46, 31).toString(),
                TestSuite.utcTime(note8.getUpdatedDate()).toString());
    }

    @Test
    public void getTimeline2() throws IOException {
        String sinceId = "";
        httpConnection.addResponse(org.andstatus.app.tests.R.raw.activitypub_inbox_pleroma_2);
        Actor actorForTimeline = Actor.fromOid(connection.getData().getOrigin(), ACTOR_OID)
                .withUniqueNameInOrigin(UNIQUE_NAME_IN_ORIGIN);
        actorForTimeline.endpoints.add(ActorEndpointType.API_INBOX, "https://pleroma.site/users/AndStatus/inbox");
        List<AActivity> timeline = connection.getTimeline(Connection.ApiRoutineEnum.HOME_TIMELINE,
                new TimelinePosition(sinceId), TimelinePosition.EMPTY, 20, actorForTimeline);
        assertNotNull("timeline returned", timeline);
        assertEquals("Number of items in the Timeline", 10, timeline.size());

        AActivity activity8 = timeline.get(8);
        assertEquals("Creating " + activity8, ActivityType.CREATE, activity8.type);
        assertEquals("Acting on a Note " + activity8, AObjectType.NOTE, activity8.getObjectType());
        Note note8 = activity8.getNote();
        assertEquals("Note oid " + note8, pawooNoteOid, note8.oid);
        Actor author = activity8.getAuthor();
        assertEquals("Author's oid " + activity8, pawooActorOid, author.oid);
        assertEquals("Actor is author", author, activity8.getActor());
        assertThat("Note body " + note8, note8.getContent(),
                containsString("how two attached images may look like"));
        assertEquals("Note updated at " + TestSuite.utcTime(note8.getUpdatedDate()),
                TestSuite.utcTime(2019, Calendar.MARCH, 10, 18, 46, 31).toString(),
                TestSuite.utcTime(note8.getUpdatedDate()).toString());

        AActivity activity9 = timeline.get(9);
        assertEquals("Creating a Note " + activity9, AObjectType.NOTE, activity9.getObjectType());
        Note note9 = activity9.getNote();
        assertEquals("Activity oid " + activity9,
                "https://pleroma.site/activities/0f74296c-0f8c-43e2-a250-692f3e61c9c3",
                activity9.getTimelinePosition().getPosition());
        assertEquals("Note oid " + note9, "https://pleroma.site/objects/78bcd5dd-c1ee-4ac1-b2e0-206a508e60e9", note9.oid);
        assertEquals("Conversation oid " + note9,"https://pleroma.site/contexts/cebf1c4d-f7f2-46a5-8025-fd8bd9cde1ab", note9.conversationOid);
        assertEquals("Note name " + note9, "", note9.getName());
        assertThat("Note body " + note9, note9.getContent(),
                is("@pawooandstatustester@pawoo.net We are implementing conversation retrieval via #ActivityPub"));
        assertEquals("Activity updated at " + TestSuite.utcTime(activity9.getUpdatedDate()),
                TestSuite.utcTime(2019, Calendar.MARCH, 15, 4, 38, 48).toString(),
                TestSuite.utcTime(activity9.getUpdatedDate()).toString());
        assertEquals("Note updated at " + TestSuite.utcTime(note9.getUpdatedDate()),
                TestSuite.utcTime(2019, Calendar.MARCH, 15, 4, 38, 48).toString(),
                TestSuite.utcTime(note9.getUpdatedDate()).toString());
        Actor actor9 = activity9.getActor();
        assertEquals("Actor's oid " + activity9, ACTOR_OID, actor9.oid);
        assertEquals("Actor's Webfinger " + activity9, "", actor9.getWebFingerId());

        assertEquals("Actor is an Author", actor9, activity9.getAuthor());
        assertEquals("Should be Create " + activity9, ActivityType.CREATE, activity9.type);
        assertEquals("Favorited by me " + activity9, TriState.UNKNOWN, activity9.getNote().getFavoritedBy(activity9.accountActor));
    }
}
