package service;

import org.junit.jupiter.api.Test;
import repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    public UserServiceTest() {
        super();
    }

    @Test
    void testUserName() {

        UserRepository mockRepo = mock(UserRepository.class);

        when(mockRepo.getUserName())
                .thenReturn("Aryan");

        UserService service = new UserService(mockRepo);

        String result = service.fetchName();

        assertEquals("Aryan", result);
    }


}
