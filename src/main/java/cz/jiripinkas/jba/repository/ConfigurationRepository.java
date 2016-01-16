package cz.jiripinkas.jba.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cz.jiripinkas.jba.entity.Configuration;

public interface ConfigurationRepository extends JpaRepository<Configuration, Integer> {
	
	@Transactional
	@Modifying
	@Query("update Configuration c set c.icon = ?1")
	void saveIcon(byte[] icon);

	@Transactional
	@Modifying
	@Query("update Configuration c set c.favicon = ?1")
	void saveFavicon(byte[] icon);

	@Transactional
	@Modifying
	@Query("update Configuration c set c.appleTouchIcon = ?1")
	void saveAppleTouchIcon(byte[] icon);

}
