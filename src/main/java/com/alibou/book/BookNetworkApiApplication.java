package com.alibou.book;

import com.alibou.book.role.Role;
import com.alibou.book.role.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
@SpringBootApplication
public class BookNetworkApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookNetworkApiApplication.class, args);
	}

//	@Bean
//	public CommandLineRunner runner(RoleRepository roleRepository) {
//		return args -> {
//			if (roleRepository.findByName("ADMIN").isEmpty()) {
//				roleRepository.save(Role.builder().name("ADMIN").build());
//			}
//		};
//	}






	@Bean
	public CommandLineRunner runner(RoleRepository roleRepository) {
		return args -> {
//			createRoleIfNotExists(roleRepository, "FARMER");
			createRoleIfNotExists(roleRepository, "ADMIN");
			createRoleIfNotExists(roleRepository, "USER");
		};
	}

	private void createRoleIfNotExists(RoleRepository roleRepository, String roleName) {
		if (roleRepository.findByName(roleName).isEmpty()) {
			roleRepository.save(Role.builder().name(roleName).build());
		}
	}
}
