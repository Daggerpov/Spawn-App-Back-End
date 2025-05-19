package com.danielagapov.spawn.Services.OAuth;


import com.danielagapov.spawn.Enums.OAuthProvider;

/**
 * Strategy interface for different OAuth providers
 */
public interface OAuthStrategy {

    OAuthProvider getOAuthProvider();

    String verifyIdToken(String idToken);
}
