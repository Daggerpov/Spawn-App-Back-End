package com.danielagapov.spawn.Services.UserDetails;

import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserInfo;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserInfoService implements UserDetailsService {
    private final IUserRepository repository;

    public UserInfoService(IUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repository.findByUsername(username);
        if (user == null) { throw new UsernameNotFoundException(username); }

        return new UserInfo(username);
    }
}
