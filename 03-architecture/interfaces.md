# Спецификация интерфейсов между слоями

Для обеспечения слабой связности и тестируемости слои взаимодействуют через интерфейсы.

## 1. Интерфейсы уровня Mediator (бизнес-логика)

### IUserService

public interface IUserService {
    User registerUser(RegistrationRequest request);
    User authenticate(String email, String password);
    User getUserById(Long id);
    List<User> getAllUsers();
    void deleteUser(Long id);
}