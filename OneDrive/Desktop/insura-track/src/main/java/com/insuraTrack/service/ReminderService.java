package com.insuraTrack.service;

import com.insuraTrack.model.Reminder;
import java.util.List;

public interface ReminderService {
    Reminder create(Reminder reminder);
    Reminder getById(String id);
    List<Reminder> getAll();
    List<Reminder> getByPolicy(String policyId);
    List<Reminder> getPendingReminders();
    Reminder markAsSent(String id);
    Reminder dismiss(String id);
    void softDelete(String id, String deletedByEmail);
    void processScheduledReminders();


    // Add these methods to your ReminderService interface + impl:

    List<Reminder> getActive();       // dismissed=false, deleted=false
    List<Reminder> getDismissed();    // dismissed=true, deleted=false
    List<Reminder> getMyActive();     // current user, dismissed=false
    List<Reminder> getMyDismissed(); // current user, dismissed=true
    Reminder restore(String id);      // dismissed=false
    void dismissAllByPolicy(String policyId);
    void generateAll();               // generate reminders for all active policies
    void generateForPolicy(String policyId);
}