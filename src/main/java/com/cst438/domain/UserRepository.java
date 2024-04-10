package com.cst438.domain;

import org.springframework.data.repository.CrudRepository;
import java.util.List;
import com.cst438.domain.User;

public interface UserRepository extends CrudRepository<User, Integer>{

	List<User> findAllByOrderByIdAsc();

	User findByEmail(String email);
}
