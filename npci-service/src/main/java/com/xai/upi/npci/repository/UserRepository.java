    package com.xai.upi.npci.repository;

    import com.xai.upi.npci.model.User;
    import org.springframework.data.mongodb.repository.MongoRepository;

    import java.util.List;
    import java.util.Optional;

    public interface UserRepository extends MongoRepository<User, String> {
        Optional<User> findByUpiId(String upiId);
        Optional<User> findByPhone(String phone);
        List<User> findAllByUpiId(String upiId);
        Optional<User> findByEmail(String email);
        Optional<User> findByUserId(String userId);
        Optional<User> findByUpiIdOrPhoneOrBankAccountId(String upiId, String phone,String bankAccountId);
    }
