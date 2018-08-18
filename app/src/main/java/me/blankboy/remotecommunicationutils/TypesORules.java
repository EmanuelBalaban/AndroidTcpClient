package me.blankboy.remotecommunicationutils;

// All possible queue rules.
public enum TypesORules {
    // TypesOPriority.VeryHigh will be processed next in queue.
    // TypesOPriority.High will be processed if no other TypesOPriority.Normal is in process already.
    // TypesOPriority.Normal will be processed if there is no higher priority package.
    // But everything is processed depending on its position in Outgoing Queue, from start to end.
    Normal,

    // Same priority rules as Normal.
    // Outgoing Queue is processed from end to start.
    Reverse
}
