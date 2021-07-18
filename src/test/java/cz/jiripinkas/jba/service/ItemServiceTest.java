package cz.jiripinkas.jba.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

class ItemServiceTest {

	private ItemService itemService;

	@BeforeEach
	public void setUp() throws Exception {
		itemService = new ItemService();
	}

	@Test
	void testIsTooOldOrYoung() {
		assertFalse(itemService.isTooOldOrYoung(new Date()));
		Calendar calendar1 = new GregorianCalendar();
		calendar1.add(Calendar.MONTH, -5);
		assertTrue(itemService.isTooOldOrYoung(calendar1.getTime()));
		Calendar calendar2 = new GregorianCalendar();
		calendar2.add(Calendar.DATE, 1);
		calendar2.add(Calendar.MINUTE, 1);
		assertTrue(itemService.isTooOldOrYoung(calendar2.getTime()));
	}

}
