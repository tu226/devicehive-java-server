package com.devicehive.service;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
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
 * #L%
 */

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.exceptions.InvalidPrincipalException;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.Network;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.rpc.ListUserRequest;
import com.devicehive.model.rpc.ListUserResponse;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.service.helpers.PasswordProcessor;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * This class serves all requests to database from controller.
 */
@Component
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String PASSWORD_REGEXP = "^.{6,128}$";

    private final PasswordProcessor passwordService;
    private final NetworkDao networkDao;
    private final UserDao userDao;
    private final TimestampService timestampService;
    private final ConfigurationService configurationService;
    private final HiveValidator hiveValidator;
    private final RpcClient rpcClient;

    private NetworkService networkService;

    @Autowired
    public UserService(PasswordProcessor passwordService,
                       NetworkDao networkDao,
                       UserDao userDao,
                       TimestampService timestampService,
                       ConfigurationService configurationService,
                       HiveValidator hiveValidator,
                       RpcClient rpcClient) {
        this.passwordService = passwordService;
        this.networkDao = networkDao;
        this.userDao = userDao;
        this.timestampService = timestampService;
        this.configurationService = configurationService;
        this.hiveValidator = hiveValidator;
        this.rpcClient = rpcClient;
    }

    @Autowired
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    /**
     * Tries to authenticate with given credentials
     *
     * @return User object if authentication is successful or null if not
     */
    @Transactional(noRollbackFor = ActionNotAllowedException.class)
    public UserVO authenticate(String login, String password) {
        Optional<UserVO> userOpt = userDao.findByName(login);
        if (!userOpt.isPresent()) {
            return null;
        }
        return checkPassword(userOpt.get(), password)
                .orElseThrow(() -> new ActionNotAllowedException(String.format(Messages.INCORRECT_CREDENTIALS, login)));
    }

    @Transactional(noRollbackFor = InvalidPrincipalException.class)
    public UserVO getActiveUser(String login, String password) {
        Optional<UserVO> userOpt = userDao.findByName(login);
        if (!userOpt.isPresent()) {
            logger.error("Can't find user with login {} and password {}", login, password);
            throw new InvalidPrincipalException(String.format(Messages.USER_LOGIN_NOT_FOUND, login));
        } else if (userOpt.get().getStatus() != UserStatus.ACTIVE) {
            logger.error("User with login {} is not active", login);
            throw new InvalidPrincipalException(Messages.USER_NOT_ACTIVE);
        }
        return checkPassword(userOpt.get(), password)
                .orElseThrow(() -> new InvalidPrincipalException(String.format(Messages.INCORRECT_CREDENTIALS, login)));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserVO updateUser(@NotNull Long id, UserUpdate userToUpdate, UserVO curUser) {
        UserVO existing = userDao.find(id);

        if (existing == null) {
            logger.error("Can't update user with id {}: user not found", id);
            throw new NoSuchElementException(String.format(Messages.USER_NOT_FOUND, id));
        }

        if (userToUpdate == null) {
            return existing;
        }

        final boolean isClient = UserRole.CLIENT.equals(curUser.getRole());
        if (isClient) {
            if (userToUpdate.getLogin().isPresent() ||
                    userToUpdate.getStatus().isPresent() ||
                    userToUpdate.getRole().isPresent()) {
                logger.error("Can't update user with id {}: users with the 'client' role not allowed to change their " +
                        "login, status or role", id);
                throw new HiveException(Messages.ADMIN_PERMISSIONS_REQUIRED, FORBIDDEN.getStatusCode());
            }
        }

        if (userToUpdate.getLogin().isPresent()) {
            final String newLogin = StringUtils.trim(userToUpdate.getLogin().orElse(null));
            Optional<UserVO> withSuchLogin = userDao.findByName(newLogin);

            if (withSuchLogin.isPresent() && !withSuchLogin.get().getId().equals(id)) {
                throw new ActionNotAllowedException(Messages.DUPLICATE_LOGIN);
            }
            existing.setLogin(newLogin);
        }

        final Optional<String> newPassword = userToUpdate.getPassword();
        if (newPassword.isPresent() && StringUtils.isNotEmpty(newPassword.get())) {
            final String password = newPassword.get();
            if (StringUtils.isEmpty(password) || !password.matches(PASSWORD_REGEXP)) {
                logger.error("Can't update user with id {}: password required", id);
                throw new IllegalParametersException(Messages.PASSWORD_VALIDATION_FAILED);
            }
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            existing.setPasswordSalt(salt);
            existing.setPasswordHash(hash);
        }

        if (userToUpdate.getRoleEnum() != null) {
            existing.setRole(userToUpdate.getRoleEnum());
        }

        if (userToUpdate.getStatusEnum() != null) {
            existing.setStatus(userToUpdate.getStatusEnum());
        }

        existing.setData(userToUpdate.getData().orElse(null));
        
        if (userToUpdate.getIntroReviewed().isPresent()) {
            existing.setIntroReviewed(userToUpdate.getIntroReviewed().get());
        }

        hiveValidator.validate(existing);
        return userDao.merge(existing);
    }

    /**
     * Allows user access to given network
     *
     * @param userId id of user
     * @param networkId id of network
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void assignNetwork(@NotNull long userId, @NotNull long networkId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't assign network with id {}: user {} not found", networkId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        NetworkWithUsersAndDevicesVO existingNetwork = networkDao.findWithUsers(networkId).orElse(null);
        if (Objects.isNull(existingNetwork)) {
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
        }
            
        networkDao.assignToNetwork(existingNetwork, existingUser);
    }

    /**
     * Revokes user access to given network
     *
     * @param userId id of user
     * @param networkId id of network
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void unassignNetwork(@NotNull long userId, @NotNull long networkId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't unassign network with id {}: user {} not found", networkId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        NetworkVO existingNetwork = networkDao.find(networkId);
        if (existingNetwork == null) {
            logger.error("Can't unassign user with id {}: network {} not found", userId, networkId);
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
        }
        userDao.unassignNetwork(existingUser, networkId);
    }

    public CompletableFuture<List<UserVO>> list(ListUserRequest request) {
        return list(request.getLogin(), request.getLoginPattern(), request.getRole(), request.getStatus(), request.getSortField(),
                request.getSortOrder(), request.getTake(), request.getSkip());
    }

    public CompletableFuture<List<UserVO>> list(String login, String loginPattern, Integer role, Integer status, String sortField,
            String sortOrder, Integer take, Integer skip) {
        ListUserRequest request = new ListUserRequest();
        request.setLogin(login);
        request.setLoginPattern(loginPattern);
        request.setRole(role);
        request.setStatus(status);
        request.setSortField(sortField);
        request.setSortOrder(sortOrder);
        request.setTake(take);
        request.setSkip(skip);

        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(request)
                .build(), new ResponseConsumer(future));

        return future.thenApply(r -> ((ListUserResponse) r.getBody()).getUsers());
    }

    /**
     * Retrieves user by id (no networks fetched in this case)
     *
     * @param id user id
     * @return User model without networks, or null if there is no such user
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserVO findById(@NotNull long id) {
        return userDao.find(id);
    }

    /**
     * Retrieves user with networks by id, if there is no networks user hass
     * access to networks will be represented by empty set
     *
     * @param id user id
     * @return User model with networks, or null, if there is no such user
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserWithNetworkVO findUserWithNetworks(@NotNull long id) {
        return userDao.getWithNetworksById(id);

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserVO createUser(@NotNull UserVO user, String password) {
        hiveValidator.validate(user);
        if (user.getId() != null) {
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        final String userLogin = StringUtils.trim(user.getLogin());
        user.setLogin(userLogin);
        Optional<UserVO> existing = userDao.findByName(user.getLogin());
        if (existing.isPresent()) {
            throw new ActionNotAllowedException(Messages.DUPLICATE_LOGIN);
        }
        if (StringUtils.isNotEmpty(password) && password.matches(PASSWORD_REGEXP)) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            user.setPasswordSalt(salt);
            user.setPasswordHash(hash);
        } else {
            throw new IllegalParametersException(Messages.PASSWORD_VALIDATION_FAILED);
        }
        user.setLoginAttempts(Constants.INITIAL_LOGIN_ATTEMPTS);
        if (user.getIntroReviewed() == null) {
            user.setIntroReviewed(false);
        }
        userDao.persist(user);
        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserWithNetworkVO createUserWithNetwork(UserVO convertTo, String password) {
        hiveValidator.validate(convertTo);
        UserVO createdUser = createUser(convertTo, password);
        NetworkVO createdNetwork = networkService.createOrUpdateNetworkByUser(createdUser);
        UserWithNetworkVO result = UserWithNetworkVO.fromUserVO(createdUser);
        result.getNetworks().add(createdNetwork);
        return result;
    }

    /**
     * Deletes user by id. deletion is cascade
     *
     * @param id user id
     * @return true in case of success, false otherwise
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean deleteUser(long id) {
        int result = userDao.deleteById(id);
        return result > 0;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToDevice(UserVO user, String deviceId) {
        if (!user.isAdmin()) {
            long count = userDao.hasAccessToDevice(user, deviceId);
            return count > 0;
        }
        return true;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToNetwork(UserVO user, NetworkVO network) {
        if (!user.isAdmin()) {
            long count = userDao.hasAccessToNetwork(user, network);
            return count > 0;
        }
        return true;
    }

    @Transactional
    public UserVO refreshUserLoginData(UserVO user) {
        hiveValidator.validate(user);
        final long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
        return updateStatisticOnSuccessfulLogin(user, loginTimeout);
    }

    private Optional<UserVO> checkPassword(UserVO user, String password) {
        boolean validPassword = passwordService.checkPassword(password, user.getPasswordSalt(), user.getPasswordHash());

        long loginTimeout = configurationService.getLong(Constants.LAST_LOGIN_TIMEOUT, Constants.LAST_LOGIN_TIMEOUT_DEFAULT);
        boolean mustUpdateLoginStatistic = user.getLoginAttempts() != 0
                || user.getLastLogin() == null
                || timestampService.getTimestamp() - user.getLastLogin().getTime() > loginTimeout;

        if (validPassword && mustUpdateLoginStatistic) {
            UserVO user1 = updateStatisticOnSuccessfulLogin(user, loginTimeout);
            return of(user1);
        } else if (!validPassword) {
            user.setLoginAttempts(user.getLoginAttempts() + 1);
            if (user.getLoginAttempts()
                    >= configurationService.getInt(Constants.MAX_LOGIN_ATTEMPTS, Constants.MAX_LOGIN_ATTEMPTS_DEFAULT)) {
                user.setStatus(UserStatus.LOCKED_OUT);
                user.setLoginAttempts(0);
            }
            userDao.merge(user);
            return empty();
        }
        return of(user);
    }

    private UserVO updateStatisticOnSuccessfulLogin(UserVO user, long loginTimeout) {
        boolean update = false;
        if (user.getLoginAttempts() != 0) {
            update = true;
            user.setLoginAttempts(0);
        }
        if (user.getLastLogin() == null || timestampService.getTimestamp() - user.getLastLogin().getTime() > loginTimeout) {
            update = true;
            user.setLastLogin(timestampService.getDate());
        }
        return update ? userDao.merge(user) : user;
    }
}
