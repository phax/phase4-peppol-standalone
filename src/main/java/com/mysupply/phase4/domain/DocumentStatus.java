package com.mysupply.phase4.domain;

public enum DocumentStatus {

    /// The document has been received, and stored in the database.
    Created,

//    /// VAX has retrieved/requested the document, but not yet confirmed it.
//    /// Some time the confirmation message failed to reach Phase4 (so the document must go back to Created state).
//    Retrieved,

    /// VAX has takeover.
    Confirmed,

}
