package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

class GoogleCalendarServiceTest {

    private EventRepository eventRepository;
    private UserRepository userRepository;
    private GoogleCalendarService service;

    @BeforeEach
    void setUp() {
        eventRepository = Mockito.mock(EventRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        service = new GoogleCalendarService(eventRepository, userRepository);
    }

    @Test
    void buildCalendarClient_userNotFound_throws() {
        User user = new User();
        user.setId(999L);

        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(RuntimeException.class, () -> service.buildCalendarClient(user));
    }

    @Test
    void pushEventsToGoogle_countsSuccessfulPushes() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setGoogleAccessToken("token");

        Event e1 = new Event();
        e1.setId(1L);
        e1.setUser(user);

        Event e2 = new Event();
        e2.setId(2L);
        e2.setUser(user);

        GoogleCalendarService spySvc = spy(service);
        // stub out the heavy pushEventToGoogle to avoid external calls
        doNothing().when(spySvc).pushEventToGoogle(e1);
        doNothing().when(spySvc).pushEventToGoogle(e2);

        int count = spySvc.pushEventsToGoogle(List.of(e1, e2), user);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void convertToGoogleEvent_mapsFields() throws Exception {
        // prepare service with timezone
        Field tz = GoogleCalendarService.class.getDeclaredField("defaultTimezone");
        tz.setAccessible(true);
        tz.set(service, "UTC");

        // set client id/secret to avoid NPE in GoogleCredential builder
        Field cid = GoogleCalendarService.class.getDeclaredField("clientId");
        cid.setAccessible(true);
        cid.set(service, "cid");
        Field csec = GoogleCalendarService.class.getDeclaredField("clientSecret");
        csec.setAccessible(true);
        csec.set(service, "csecret");

        // build an event with details
        com.example.backend.model.Event ev = new com.example.backend.model.Event();
        ev.setId(99L);
        ev.setSummary("TestEvt");
        ev.setStartTime(LocalDateTime.now());
        ev.setEndTime(LocalDateTime.now().plusHours(1));

        com.example.backend.model.Location loc = new com.example.backend.model.Location();
        loc.setAddress("1 Rue de Test, Paris, France");
        ev.setLocation(loc);

        com.example.backend.model.Task task = new com.example.backend.model.Task();
        task.setTitle("DoIt");
        ev.setTask(task);

        // set category to FOCUS
        ev.setCategory(com.example.backend.model.ActivityCategory.FOCUS);

        // invoke private convertToGoogleEvent
        Method conv = GoogleCalendarService.class.getDeclaredMethod("convertToGoogleEvent", com.example.backend.model.Event.class);
        conv.setAccessible(true);
        com.google.api.services.calendar.model.Event g = (com.google.api.services.calendar.model.Event) conv.invoke(service, ev);

        assertThat(g.getLocation()).isEqualTo("1 Rue de Test, Paris, France");
        assertThat(g.getDescription()).contains("Tâche : DoIt").contains("EDT-ID:99");
        assertThat(g.getTransparency()).isEqualTo("opaque");
        assertThat(g.getColorId()).isEqualTo("9");
    }

    @Test
    void refreshAccessToken_noRefreshToken_returnsFalse() {
        com.example.backend.model.User user = new com.example.backend.model.User();
        user.setId(5L);
        user.setGoogleRefreshToken(null);

        boolean ok = service.refreshAccessToken(user);
        assertThat(ok).isFalse();
    }

    @Test
    void deleteEventFromGoogle_ignoresWhenNoTokenOrNoGoogleId() {
        com.example.backend.model.User user = new com.example.backend.model.User();
        user.setId(7L);
        user.setGoogleAccessToken(null);

        com.example.backend.model.Event ev = new com.example.backend.model.Event();
        ev.setUser(user);
        ev.setSummary("s");

        // no token -> ignored
        service.deleteEventFromGoogle(ev);
        verify(eventRepository, org.mockito.Mockito.never()).save(any());

        // token but no googleId -> ignored
        user.setGoogleAccessToken("tok");
        ev.setGoogleEventId(null);
        service.deleteEventFromGoogle(ev);
        verify(eventRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void pushEventToGoogle_ignoresWhenNoToken() {
        com.example.backend.model.User user = new com.example.backend.model.User();
        user.setId(8L);
        user.setGoogleAccessToken(null);

        com.example.backend.model.Event ev = new com.example.backend.model.Event();
        ev.setUser(user);
        ev.setSummary("s");

        service.pushEventToGoogle(ev);
        verify(eventRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void executeWithRetry_success() throws Throwable {
        // prepare user and repository
        com.example.backend.model.User user = new com.example.backend.model.User();
        user.setId(42L);
        user.setGoogleAccessToken("tok");
        when(userRepository.findById(42L)).thenReturn(java.util.Optional.of(user));

        // find private CalendarOperation interface
        Class<?> opIface = Arrays.stream(GoogleCalendarService.class.getDeclaredClasses())
            .filter(Class::isInterface)
            .filter(c -> "CalendarOperation".equals(c.getSimpleName()))
            .findFirst()
            .orElse(null);
        assertThat(opIface).isNotNull();
        for (Class<?> c : GoogleCalendarService.class.getDeclaredClasses()) {
            if (c.isInterface() && c.getSimpleName().equals("CalendarOperation")) {
                opIface = c;
                break;
            }
        }
        assertThat(opIface).isNotNull();

        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("execute")) {
                return "OK";
            }
            return null;
        };

        Object opProxy = Proxy.newProxyInstance(opIface.getClassLoader(), new Class[]{opIface}, handler);

        // ensure clientId/secret set to avoid NPE in GoogleCredential builder
        Field cid = GoogleCalendarService.class.getDeclaredField("clientId");
        cid.setAccessible(true);
        cid.set(service, "cid");
        Field csec = GoogleCalendarService.class.getDeclaredField("clientSecret");
        csec.setAccessible(true);
        csec.set(service, "csecret");

        Method exec = GoogleCalendarService.class.getDeclaredMethod("executeWithRetry", com.example.backend.model.User.class, opIface);
        exec.setAccessible(true);

        Object res = exec.invoke(service, user, opProxy);
        assertThat(res).isEqualTo("OK");
    }

    @Test
    void executeWithRetry_retriesOn401_and_refreshes() throws Throwable {
        com.example.backend.model.User user = new com.example.backend.model.User();
        user.setId(43L);
        user.setGoogleAccessToken("tok");
        when(userRepository.findById(43L)).thenReturn(java.util.Optional.of(user));

        // set client id/secret to avoid NPE in GoogleCredential builder
        Field cid = GoogleCalendarService.class.getDeclaredField("clientId");
        cid.setAccessible(true);
        cid.set(service, "cid");
        Field csec = GoogleCalendarService.class.getDeclaredField("clientSecret");
        csec.setAccessible(true);
        csec.set(service, "csecret");

        // spy the service so we can stub refreshAccessToken
        GoogleCalendarService spySvc = spy(service);
        doReturn(true).when(spySvc).refreshAccessToken(user);

        // find private CalendarOperation interface
        Class<?> opIface = Arrays.stream(GoogleCalendarService.class.getDeclaredClasses())
            .filter(Class::isInterface)
            .filter(c -> "CalendarOperation".equals(c.getSimpleName()))
            .findFirst()
            .orElse(null);
        assertThat(opIface).isNotNull();
        for (Class<?> c : GoogleCalendarService.class.getDeclaredClasses()) {
            if (c.isInterface() && c.getSimpleName().equals("CalendarOperation")) {
                opIface = c;
                break;
            }
        }
        assertThat(opIface).isNotNull();

        AtomicInteger calls = new AtomicInteger(0);
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("execute")) {
                if (calls.getAndIncrement() == 0) {
                    throw new java.io.IOException("401 Unauthorized");
                }
                return "RETRIED";
            }
            return null;
        };

        Object opProxy = Proxy.newProxyInstance(opIface.getClassLoader(), new Class[]{opIface}, handler);

        Method exec = GoogleCalendarService.class.getDeclaredMethod("executeWithRetry", com.example.backend.model.User.class, opIface);
        exec.setAccessible(true);

        Object res = exec.invoke(spySvc, user, opProxy);
        assertThat(res).isEqualTo("RETRIED");
        verify(spySvc).refreshAccessToken(user);
    }

    @Test
    void pushEventsToGoogle_partialFailures_countsOnlySuccesses() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setGoogleAccessToken("token");

        Event e1 = new Event();
        e1.setId(1L);
        e1.setUser(user);

        Event e2 = new Event();
        e2.setId(2L);
        e2.setUser(user);

        GoogleCalendarService spySvc = spy(service);
        // e1 succeeds, e2 throws
        doNothing().when(spySvc).pushEventToGoogle(e1);
        doThrow(new RuntimeException("boom")).when(spySvc).pushEventToGoogle(e2);

        int count = spySvc.pushEventsToGoogle(List.of(e1, e2), user);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void pushEventsToGoogle_emptyList_returnsZero() {
        User user = new User();
        user.setId(99L);
        int count = service.pushEventsToGoogle(List.of(), user);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void convertToGoogleEvent_handlesMissingFields() throws Exception {
        // prepare service with timezone
        Field tz = GoogleCalendarService.class.getDeclaredField("defaultTimezone");
        tz.setAccessible(true);
        tz.set(service, "UTC");

        // set client id/secret to avoid NPE in GoogleCredential builder
        Field cid = GoogleCalendarService.class.getDeclaredField("clientId");
        cid.setAccessible(true);
        cid.set(service, "cid");
        Field csec = GoogleCalendarService.class.getDeclaredField("clientSecret");
        csec.setAccessible(true);
        csec.set(service, "csecret");

        com.example.backend.model.Event ev = new com.example.backend.model.Event();
        ev.setSummary("NoExtras");
        ev.setStartTime(LocalDateTime.now());
        ev.setEndTime(LocalDateTime.now().plusHours(1));
        // no task, no location, no category, no id

        Method conv = GoogleCalendarService.class.getDeclaredMethod("convertToGoogleEvent", com.example.backend.model.Event.class);
        conv.setAccessible(true);
        com.google.api.services.calendar.model.Event g = (com.google.api.services.calendar.model.Event) conv.invoke(service, ev);

        assertThat(g.getLocation()).isNull();
        assertThat(g.getDescription()).isNullOrEmpty();
    }

    @Test
    void deleteEventFromGoogle_whenRemoteFails_throwsGoogleApiException() throws Exception {
        com.example.backend.model.User user = new com.example.backend.model.User();
        user.setId(55L);
        user.setGoogleAccessToken("token");

        com.example.backend.model.Event ev = new com.example.backend.model.Event();
        ev.setUser(user);
        ev.setGoogleEventId("someId");
        ev.setSummary("toDel");

        when(userRepository.findById(55L)).thenReturn(java.util.Optional.of(user));

        // ensure client id/secret to avoid NPE in GoogleCredential.Builder
        Field cid = GoogleCalendarService.class.getDeclaredField("clientId");
        cid.setAccessible(true);
        cid.set(service, "cid");
        Field csec = GoogleCalendarService.class.getDeclaredField("clientSecret");
        csec.setAccessible(true);
        csec.set(service, "csecret");

        assertThrows(com.example.backend.exception.GoogleApiException.class, () -> service.deleteEventFromGoogle(ev));
        // when deletion fails, local event should not be saved with null googleId
        verify(eventRepository, org.mockito.Mockito.never()).save(any());
    }
}
