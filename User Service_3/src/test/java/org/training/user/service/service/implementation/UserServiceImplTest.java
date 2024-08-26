package org.training.user.service.service.implementation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.training.user.service.exception.EmptyFields;
import org.training.user.service.exception.ResourceConflictException;
import org.training.user.service.exception.ResourceNotFound;
import org.training.user.service.external.AccountService;
import org.training.user.service.model.Status;
import org.training.user.service.model.dto.CreateUser;
import org.training.user.service.model.dto.UserDto;
import org.training.user.service.model.dto.UserUpdate;
import org.training.user.service.model.dto.UserUpdateStatus;
import org.training.user.service.model.dto.response.Response;
import org.training.user.service.model.entity.User;
import org.training.user.service.model.entity.UserProfile;
import org.training.user.service.model.external.Account;
import org.training.user.service.repository.UserRepository;
import org.training.user.service.service.implementation.UserServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, accountService);
    }

    // Test cases go here...
    @Test
    void createUser_ShouldCreateUser_WhenDataIsValid() {
        CreateUser createUserDto = new CreateUser();
        createUserDto.setEmailId("test@example.com");
        createUserDto.setFirstName("John");
        createUserDto.setLastName("Doe");
        createUserDto.setContactNumber("1234567890");

        when(userRepository.findByEmailId(createUserDto.getEmailId())).thenReturn(Optional.empty());

        Response response = userService.createUser(createUserDto);

        assertEquals("User created successfully", response.getResponseMessage());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_ShouldThrowResourceConflictException_WhenEmailAlreadyExists() {
        CreateUser createUserDto = new CreateUser();
        createUserDto.setEmailId("test@example.com");

        when(userRepository.findByEmailId(createUserDto.getEmailId())).thenReturn(Optional.of(new User()));

        assertThrows(ResourceConflictException.class, () -> userService.createUser(createUserDto));
    }
    
    @Test
    void readAllUsers_ShouldReturnListOfUsers() {
        User user = new User();
        user.setUserId(1L);
        user.setEmailId("test@example.com");
        user.setIdentificationNumber(UUID.randomUUID().toString());

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> userDtos = userService.readAllUsers();

        assertFalse(userDtos.isEmpty());
        assertEquals(1, userDtos.size());
        assertEquals("test@example.com", userDtos.get(0).getEmailId());
    }

    @Test
    void updateUserStatus_ShouldUpdateStatus_WhenUserExists() {
        User user = new User();
        user.setUserId(1L);

        UserUpdateStatus userUpdateStatus = new UserUpdateStatus();
        userUpdateStatus.setStatus(Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Response response = userService.updateUserStatus(1L, userUpdateStatus);

        assertEquals("User updated successfully", response.getResponseMessage());
        assertEquals(Status.ACTIVE, user.getStatus());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateUserStatus_ShouldThrowResourceNotFound_WhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UserUpdateStatus userUpdateStatus = new UserUpdateStatus();
        assertThrows(ResourceNotFound.class, () -> userService.updateUserStatus(1L, userUpdateStatus));
    }

    @Test
    void updateUserStatus_ShouldThrowEmptyFieldsException_WhenFieldsAreEmpty() {
        User user = new User();
        user.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateStatus userUpdateStatus = new UserUpdateStatus();
        assertThrows(EmptyFields.class, () -> userService.updateUserStatus(1L, userUpdateStatus));
    }
    
    @Test
    void readUserById_ShouldReturnUserDto_WhenUserExists() {
        User user = new User();
        user.setUserId(1L);
        user.setEmailId("test@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto userDto = userService.readUserById(1L);

        assertNotNull(userDto);
        assertEquals("test@example.com", userDto.getEmailId());
    }

    @Test
    void readUserById_ShouldThrowResourceNotFound_WhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFound.class, () -> userService.readUserById(1L));
    }
    
    @Test
    void updateUser_ShouldUpdateUser_WhenUserExists() {
        User user = new User();
        user.setUserId(1L);

        UserUpdate userUpdate = new UserUpdate();
        userUpdate.setContactNo("1234567890");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Response response = userService.updateUser(1L, userUpdate);

        assertEquals("user updated successfully", response.getResponseMessage());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateUser_ShouldThrowResourceNotFound_WhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UserUpdate userUpdate = new UserUpdate();
        assertThrows(ResourceNotFound.class, () -> userService.updateUser(1L, userUpdate));
    }
    
    @Test
    void readUserByAccountId_ShouldReturnUserDto_WhenAccountExists() {
        Account account = new Account();
        account.setUserId(1L);

        User user = new User();
        user.setUserId(1L);

        when(accountService.readByAccountNumber("acc123")).thenReturn(new ResponseEntity<>(account, HttpStatus.OK));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto userDto = userService.readUserByAccountId("acc123");

        assertNotNull(userDto);
        assertEquals(1L, userDto.getUserId());
    }

    @Test
    void readUserByAccountId_ShouldThrowResourceNotFound_WhenAccountDoesNotExist() {
        when(accountService.readByAccountNumber("acc123")).thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        assertThrows(ResourceNotFound.class, () -> userService.readUserByAccountId("acc123"));
    }

    @Test
    void readUserByAccountId_ShouldThrowResourceNotFound_WhenUserDoesNotExist() {
        Account account = new Account();
        account.setUserId(1L);

        when(accountService.readByAccountNumber("acc123")).thenReturn(new ResponseEntity<>(account, HttpStatus.OK));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFound.class, () -> userService.readUserByAccountId("acc123"));
    }





}
