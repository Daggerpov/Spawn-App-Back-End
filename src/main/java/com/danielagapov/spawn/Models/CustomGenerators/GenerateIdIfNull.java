package com.danielagapov.spawn.Models.CustomGenerators;

import com.danielagapov.spawn.Models.User;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.UUID;

public class GenerateIdIfNull implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object o) throws HibernateException {
        User user = (User) o;
        return user.getId() == null ? UUID.randomUUID() : user.getId();
    }

}
