package com.danielagapov.spawn.Services.BetaAccessSignUp;

import com.danielagapov.spawn.DTOs.BetaAccessSignUpDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.BetaAccessSignUpMapper;
import com.danielagapov.spawn.Models.BetaAccessSignUp;
import com.danielagapov.spawn.Repositories.IBetaAccessSignUpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BetaAccessSignUpService implements IBetaAccessSignUpService {
    private final IBetaAccessSignUpRepository repository;
    private final ILogger logger;

    @Autowired
    public BetaAccessSignUpService(IBetaAccessSignUpRepository repository, ILogger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    /**
     * This would likely be used for internal use, just to check who's signed up
     * @return all beta access sign up record DTOs
     */
    @Override
    public List<BetaAccessSignUpDTO> getAllBetaAccessSignUpRecords() {
        try {
            return BetaAccessSignUpMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BasesNotFoundException(EntityType.FriendTag);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    /**
     * @return Strings of all emails of beta access sign up records
     */
    @Override
    public List<String> getAllEmails() {
        try {
            return getAllBetaAccessSignUpRecords().stream()
                    .map(BetaAccessSignUpDTO::getEmail)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    /**
     * This is meant for saving a record to the database upon signing up through our site
     * @param dto constructed in the front-end of our site via a form input
     * @return back the dto after persisting to database, if successful. Throws otherwise.
     */
    @Override
    public BetaAccessSignUpDTO signUp(BetaAccessSignUpDTO dto) {
        try {
            BetaAccessSignUp entity = BetaAccessSignUpMapper.toEntity(dto);
            entity = repository.save(entity);
            return BetaAccessSignUpMapper.toDTO(entity);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save beta access sign up record: " + e.getMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }
}
