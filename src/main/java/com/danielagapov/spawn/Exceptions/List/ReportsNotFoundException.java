package com.danielagapov.spawn.Exceptions.List;

import com.danielagapov.spawn.Enums.EntityType;

import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;

public class ReportsNotFoundException extends BasesNotFoundException{
    public ReportsNotFoundException() {
        super(EntityType.ReportedContent);
    }
}
