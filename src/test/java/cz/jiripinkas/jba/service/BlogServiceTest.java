package cz.jiripinkas.jba.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class BlogServiceTest {

	private BlogService blogService;

	@BeforeEach
	void setUp() throws Exception {
		blogService = new BlogService();
	}

	@Test
	void testGetLastIndexDateMinutes() {
		blogService.setLastIndexedDateFinish(new Date());
		assertEquals(blogService.getLastIndexDateMinutes(), 0);
	}

	@Test
	void testGetLastIndexDateMinutesEmptyDateFinish() {
		assertEquals(blogService.getLastIndexDateMinutes(), 0);
	}
}
